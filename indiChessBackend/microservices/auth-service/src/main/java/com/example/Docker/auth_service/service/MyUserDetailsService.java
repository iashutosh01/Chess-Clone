package com.example.Docker.auth_service.service;

import com.example.Docker.auth_service.model.AuthUser;
import com.example.Docker.auth_service.model.AuthUserPrincipal;
import com.example.Docker.auth_service.repo.AuthUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final AuthUserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser user = userRepo.getUserByUsername(username);
        return new AuthUserPrincipal(user);
    }
}
