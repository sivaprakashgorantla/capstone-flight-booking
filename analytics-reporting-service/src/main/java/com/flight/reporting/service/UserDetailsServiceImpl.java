package com.flight.reporting.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Minimal UserDetailsService — identity derived from inbound JWT.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new User(username, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
