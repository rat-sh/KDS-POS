package com.kitchenflow.kds.edge.security;

import com.kitchenflow.kds.common.security.AccessTokenVerifier;
import com.kitchenflow.kds.common.security.ActorDirectory;
import com.kitchenflow.kds.common.security.ApiActor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccessTokenFilter extends OncePerRequestFilter {
    private final AccessTokenVerifier tokens;
    private final ActorDirectory actors;
    public AccessTokenFilter(AccessTokenVerifier tokens, ActorDirectory actors) {
        this.tokens = tokens;
        this.actors = actors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            ApiActor tokenActor = tokens.verify(header.substring(7));
            ApiActor actor = tokenActor == null ? null : actors.findActiveActor(tokenActor.id());
            if (actor != null && actor.role().equals(tokenActor.role()) && actor.version() == tokenActor.version()) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        actor, null, List.of(new SimpleGrantedAuthority("ROLE_" + actor.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

}
