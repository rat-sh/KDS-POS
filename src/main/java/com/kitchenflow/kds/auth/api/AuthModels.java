package com.kitchenflow.kds.auth.api;

import java.util.List;

public final class AuthModels {
    private AuthModels() {}

    public record LoginRequest(String username, String password) {}
    public record UserView(long id, String username, String role, boolean active, List<Long> stationIds, long version) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserView user) {}
    public record LoginResult(LoginResponse response, String refreshToken) {}
}
