package com.kitchenflow.kds.edge.security;

import com.kitchenflow.kds.common.config.KitchenFlowProperties;
import com.kitchenflow.kds.common.security.AccessTokenVerifier;
import com.kitchenflow.kds.common.security.ActorDirectory;

import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(KitchenFlowProperties.class)
@EnableScheduling
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccessTokenVerifier tokens, ActorDirectory actors,
            ApiSecurityErrorHandler apiErrors, ApplicationRateLimitFilter rateLimits)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(apiErrors).accessDeniedHandler(apiErrors))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/board", "/api/v1/stream/board", "/api/v1/config/public").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new AccessTokenFilter(tokens, actors), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimits, AccessTokenFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<ApplicationRateLimitFilter> disableServletRateLimitRegistration(ApplicationRateLimitFilter filter) {
        FilterRegistrationBean<ApplicationRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(KitchenFlowProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Correlation-Id", "Idempotency-Key", "If-None-Match"));
        configuration.setExposedHeaders(Arrays.asList("X-Correlation-Id", "ETag", "Retry-After", "Idempotency-Replayed",
                "X-RateLimit-Limit", "X-RateLimit-Remaining"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration auth = new CorsConfiguration(configuration);
        auth.setAllowCredentials(true);
        source.registerCorsConfiguration("/api/v1/auth/**", auth);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
