package com.kitchenflow.kds.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload for creating or updating an application user.
 * <p>
 * {@code stations} carries station <em>names</em> for KITCHEN users; the service
 * resolves names to IDs. For all other roles the list is ignored.
 */
public record UserRequest(@NotBlank @Size(max = 80) String username, @NotBlank String role,
        List<String> stations, Boolean active, String password) {}
