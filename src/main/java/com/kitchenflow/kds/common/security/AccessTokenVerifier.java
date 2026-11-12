package com.kitchenflow.kds.common.security;

public interface AccessTokenVerifier {
    ApiActor verify(String token);
}
