package com.flight.notification_service.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Minimal UserDetailsService — notification-service does not own a user table.
 * Identity is fully derived from the inbound JWT issued by auth-service.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new User(username, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
