package com.kitchenflow.kds.auth.service;

import com.kitchenflow.kds.auth.repository.AuthRepository;
import com.kitchenflow.kds.common.config.KitchenFlowProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthBootstrap {
    @Bean
    ApplicationRunner bootstrapOutlet(AuthRepository repository, PasswordEncoder passwords, KitchenFlowProperties properties) {
        return args -> {
            if (repository.userCount() != 0) return;
            if (properties.bootstrapAdminUsername() == null || properties.bootstrapAdminUsername().isBlank()) {
                throw new IllegalStateException("No staff accounts exist. Set BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD to create the initial administrator.");
            }
            if (properties.bootstrapAdminPassword() == null || properties.bootstrapAdminPassword().length() < 12) {
                throw new IllegalStateException("Set BOOTSTRAP_ADMIN_PASSWORD to a unique password of at least 12 characters.");
            }
            repository.createBootstrapAdmin(properties.bootstrapAdminUsername().trim(),
                    properties.bootstrapAdminDisplayName(), passwords.encode(properties.bootstrapAdminPassword()));
        };
    }
}
