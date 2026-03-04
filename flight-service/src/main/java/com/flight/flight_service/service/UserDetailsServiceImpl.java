package com.flight.flight_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Minimal UserDetailsService — flight-service delegates identity to auth-service via JWT.
 * Used only to satisfy DaoAuthenticationProvider requirements.
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // In a real scenario this would call auth-service or a shared DB.
        // For POC: construct a dummy principal from the JWT claims (username is already verified).
        log.debug("Loading user details for JWT principal: {}", username);
        return new User(username, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
