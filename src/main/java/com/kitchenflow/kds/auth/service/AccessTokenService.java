package com.kitchenflow.kds.auth.service;

import com.kitchenflow.kds.common.config.KitchenFlowProperties;
import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.common.security.AccessTokenVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService implements AccessTokenVerifier {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final byte[] secret;
    private final long tokenSeconds;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AccessTokenService(KitchenFlowProperties properties, ObjectMapper mapper, Clock clock) {
        this.secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("KITCHENFLOW_JWT_SECRET must contain at least 32 characters.");
        }
        this.tokenSeconds = properties.accessTokenSeconds();
        this.mapper = mapper;
        this.clock = clock;
    }

    public String issue(ApiActor actor) {
        try {
            String header = ENCODER.encodeToString(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            long now = clock.instant().getEpochSecond();
            String payload = ENCODER.encodeToString(mapper.writeValueAsBytes(Map.of(
                    "sub", actor.username(), "uid", actor.id(), "role", actor.role(), "ver", actor.version(),
                    "iat", now, "exp", now + tokenSeconds)));
            String input = header + "." + payload;
            return input + "." + ENCODER.encodeToString(sign(input));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not issue access token.", exception);
        }
    }

    @Override
    public ApiActor verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;
            byte[] expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, DECODER.decode(parts[2]))) return null;
            Map<?, ?> claims = mapper.readValue(DECODER.decode(parts[1]), Map.class);
            if (!(claims.get("exp") instanceof Number expires) || expires.longValue() <= clock.instant().getEpochSecond()) return null;
            return new ApiActor(((Number) claims.get("uid")).longValue(), claims.get("sub").toString(),
                    claims.get("role").toString(), ((Number) claims.get("ver")).longValue());
        } catch (Exception exception) {
            return null;
        }
    }

    public long expiresInSeconds() {
        return tokenSeconds;
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
    }
}
