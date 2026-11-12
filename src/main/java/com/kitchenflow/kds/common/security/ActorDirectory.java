package com.kitchenflow.kds.common.security;

public interface ActorDirectory {
    ApiActor findActiveActor(long id);
}
