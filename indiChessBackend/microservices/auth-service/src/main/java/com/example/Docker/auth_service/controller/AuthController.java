package com.example.Docker.auth_service.controller;

import com.example.Docker.auth_service.model.AuthUser;
import com.example.Docker.auth_service.model.DTO.AuthUserDTO;
import com.example.Docker.auth_service.model.DTO.LoginRequestDTO;
import com.example.Docker.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthUserDTO> createUser(@RequestBody AuthUser user){
        System.out.println("User Created");
        return ResponseEntity.ok(authService.createUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> handleLogin(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response){
        System.out.println("Logging in...");
        Optional<?> loggedInUser = authService.handleLogin(loginRequestDTO, request, response);
        if(!loggedInUser.isPresent()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(loggedInUser.get());
        // add jwt to cookies
    }

    @PostMapping("/logout")
    public ResponseEntity<?> handleLogout(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Inside logout");
        return authService.handleLogout(request, response);
    }
}
