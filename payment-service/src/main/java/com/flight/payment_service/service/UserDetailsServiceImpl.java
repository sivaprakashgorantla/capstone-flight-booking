package com.flight.payment_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading principal for JWT user: {}", username);
        return new User(username, "",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
