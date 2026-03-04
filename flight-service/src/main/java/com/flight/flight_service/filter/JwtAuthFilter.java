package com.flight.flight_service.filter;

import com.flight.flight_service.service.JwtService;
import com.flight.flight_service.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter.
 * If a valid Bearer token is present → sets authentication in SecurityContext.
 * If no token or invalid token → proceeds unauthenticated (guest access allowed for search).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token — continue as anonymous (guest)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found, proceeding as anonymous");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt      = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT authenticated: user={}", username);
                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.warn("JWT processing error: {}", e.getMessage());
            // Do not block — let the request proceed; security config will reject if endpoint is protected
        }

        filterChain.doFilter(request, response);
    }
}
