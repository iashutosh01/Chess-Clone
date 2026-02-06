package com.example.Docker.auth_service.service;

import com.example.Docker.auth_service.clients.UserServiceClient;
import com.example.Docker.auth_service.model.AuthUser;
import com.example.Docker.auth_service.model.DTO.AuthUserDTO;
import com.example.Docker.auth_service.model.DTO.LoginRequestDTO;
import com.example.Docker.auth_service.model.DTO.LoginResponseDTO;
import com.example.Docker.auth_service.model.DTO.UserDTO;
import com.example.Docker.auth_service.repo.AuthUserRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthUserRepo authUserRepo;
    private final UserServiceClient userServiceClient;

    public AuthUserDTO createUser(AuthUser user){
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        AuthUser createdUser = authUserRepo.save(user);
        AuthUserDTO returnedUserDTO = new AuthUserDTO(createdUser.getUserId(), createdUser.getUsername(), createdUser.getEmailId());
        // save the same user into user-service as well
        userServiceClient.createUser(new UserDTO(createdUser.getUserId(), createdUser.getUsername(), createdUser.getEmailId()));
        return returnedUserDTO;
    }

    public Optional<?> handleLogin(LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response) {
        AuthUser user = authUserRepo.findUserByUsername(loginRequestDTO.getUsername());
        if(user == null) return Optional.of(null);
        if(user.getPassword().equals(passwordEncoder.encode(loginRequestDTO.getPassword())) ) return Optional.of("Invalid Credentials");
        String jwt = jwtService.generateToken(user.getUsername(), user.getUserId());
        LoginResponseDTO loggedInUser = new LoginResponseDTO(user.getUsername(), jwt);

        System.out.println("Creating JWT cookie with value length: " + jwt.length());

        // Test with minimal cookie settings first
        ResponseCookie cookie = ResponseCookie.from("JWT", jwt)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(86400)
                .sameSite("Lax")
                // No domain setting
                .build();

        String cookieString = cookie.toString();
        System.out.println("Cookie string: " + cookieString);

        // Add to response
        response.addHeader(HttpHeaders.SET_COOKIE, cookieString);

        // Also test with a non-HttpOnly cookie
        ResponseCookie testCookie = ResponseCookie.from("TEST_COOKIE", "test123")
                .httpOnly(false)  // Accessible by JavaScript
                .secure(false)
                .path("/")
                .maxAge(3600)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, testCookie.toString());

        return Optional.of(loggedInUser);
    }

    public ResponseEntity<?> handleLogout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        Cookie jwtCookie = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT".equals(cookie.getName())) { // Assuming "JWT" is the cookie name
                    jwtCookie = cookie;
                    break;
                }
            }
        }
        if (jwtCookie != null) {
            jwtCookie.setValue(null);
            jwtCookie.setMaxAge(0);
            jwtCookie.setPath("/");
            response.addCookie(jwtCookie); // Add cookie back to response to delete it
            request.getSession().invalidate(); //optional
            // Step 4: Clear the authentication from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(null);
            }
            return ResponseEntity.ok("User logged out successfully");
        } else {
            return ResponseEntity.status(400).body("No JWT token found, user already logged out");
        }
    }

}
