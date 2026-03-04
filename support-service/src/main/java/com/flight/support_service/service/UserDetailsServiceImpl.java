package com.flight.support_service.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Minimal UserDetailsService — support-service does not own a user table.
 * User identity is fully derived from the inbound JWT issued by auth-service.
 * This bean exists so that Spring Security's DaoAuthenticationProvider
 * can be wired up; the JWT filter bypasses password-based auth entirely.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // We don't store users locally; construct a minimal UserDetails
        // from the username claim. The real authority comes from extractRole()
        // in JwtAuthFilter, which sets it on the SecurityContext directly.
        return new org.springframework.security.core.userdetails.User(
                username,
                "",           // no password stored in this service
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
