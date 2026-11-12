package com.kitchenflow.kds.auth.service;

import com.kitchenflow.kds.common.config.KitchenFlowProperties;
import com.kitchenflow.kds.common.error.ApiFailure;
import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.auth.repository.AuthRepository;
import com.kitchenflow.kds.auth.repository.AuthRepository.UserRow;
import com.kitchenflow.kds.auth.api.AuthModels.LoginRequest;
import com.kitchenflow.kds.auth.api.AuthModels.LoginResponse;
import com.kitchenflow.kds.auth.api.AuthModels.LoginResult;
import com.kitchenflow.kds.auth.api.AuthModels.UserView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Clock;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthRepository users;
    private final PasswordEncoder passwords;
    private final AccessTokenService accessTokens;
    private final KitchenFlowProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthRepository users, PasswordEncoder passwords, AccessTokenService accessTokens,
            KitchenFlowProperties properties, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        UserRow user = users.findByUsername(request.username().trim()).orElse(null);
        if (user == null || !user.active() || !passwords.matches(request.password(), user.passwordHash())) {
            throw new ApiFailure(401, "UNAUTHENTICATED", "Username or password is incorrect.");
        }
        return issueSession(user);
    }

    @Transactional
    public LoginResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiFailure(401, "UNAUTHENTICATED", "Sign in to continue.");
        }
        String hash = hash(rawToken);
        UserRow user = users.lockValidRefresh(hash).orElse(null);
        if (user == null || !user.active()) {
            throw new ApiFailure(401, "UNAUTHENTICATED", "Sign in to continue.");
        }
        users.rotateRefresh(hash);
        return issueSession(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            users.revokeRefresh(hash(rawToken));
        }
    }

    public UserView view(long userId) {
        UserView user = users.findUserView(userId);
        if (user == null) throw new ApiFailure(401, "UNAUTHENTICATED", "Sign in to continue.");
        return user;
    }

    private LoginResult issueSession(UserRow user) {
        String refresh = randomToken();
        Timestamp expiresAt = Timestamp.from(clock.instant().plusSeconds(properties.refreshTokenDays() * 24 * 60 * 60));
        users.insertRefresh(user.id(), hash(refresh), expiresAt);
        ApiActor actor = new ApiActor(user.id(), user.username(), user.role(), user.version());
        return new LoginResult(new LoginResponse(accessTokens.issue(actor), "Bearer", accessTokens.expiresInSeconds(), view(user.id())), refresh);
    }

    private String randomToken() {
        byte[] value = new byte[48];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token.", exception);
        }
    }

}
