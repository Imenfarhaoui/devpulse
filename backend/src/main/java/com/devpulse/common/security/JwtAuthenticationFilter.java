package com.devpulse.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.getClaimsFromToken(jwt);
                String email = claims.getSubject();
                String userIdStr = claims.get("userId", String.class);
                String globalRole = claims.get("globalRole", String.class);
                String orgIdStr = claims.get("orgId", String.class);
                String orgRole = claims.get("orgRole", String.class);

                // Allow X-Organization-Id header override if valid UUID
                String headerOrgId = request.getHeader("X-Organization-Id");
                UUID activeOrgId = null;
                if (StringUtils.hasText(headerOrgId)) {
                    try {
                        activeOrgId = UUID.fromString(headerOrgId);
                    } catch (Exception ignored) {}
                }
                if (activeOrgId == null && StringUtils.hasText(orgIdStr)) {
                    activeOrgId = UUID.fromString(orgIdStr);
                }

                if (activeOrgId != null) {
                    TenantContext.setCurrentOrgId(activeOrgId);
                }

                UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
                UserPrincipal principal = new UserPrincipal(
                        userId,
                        email,
                        "",
                        "",
                        "",
                        globalRole != null ? globalRole : "USER",
                        activeOrgId,
                        orgRole
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

