package com.kitchenflow.kds.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kitchenflow")
public record KitchenFlowProperties(
        String jwtSecret,
        long accessTokenSeconds,
        long refreshTokenDays,
        boolean refreshCookieSecure,
        List<String> allowedOrigins,
        String bootstrapAdminUsername,
        String bootstrapAdminPassword,
        String bootstrapAdminDisplayName) {
}
