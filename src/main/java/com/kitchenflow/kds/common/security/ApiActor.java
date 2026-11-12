package com.kitchenflow.kds.common.security;

public record ApiActor(long id, String username, String role, long version) {
}
