package com.example.buildPro.service;


import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Retrieve user from the database
        Optional<AuthUser> optionalAuthUser = authUserRepository.findByUsername(username);
        AuthUser authUser = optionalAuthUser.orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Return UserDetails implementation
        return authUser;
    }

//    public List<AuthUser> findUsersByIds(List<String> userIds) {
//        return authUserRepository.findAllById(userIds).stream().filter(user -> userIds.contains(user.getId())).collect(Collectors.toList());
//    }
}