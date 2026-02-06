package com.example.Docker.auth_service.oauth;

import com.example.Docker.auth_service.clients.UserServiceClient;
import com.example.Docker.auth_service.model.AuthUser;
import com.example.Docker.auth_service.model.DTO.UserDTO;
import com.example.Docker.auth_service.repo.AuthUserRepo;
import com.example.Docker.auth_service.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AuthUserRepo authUserRepo;
    private final UserServiceClient userServiceClient;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        System.out.println(email);
        System.out.println(name);



        // Create or get user from the database (you should have a user service for this)
        AuthUser user = authUserRepo.getUserByEmailId(email);
        if (user == null) {
            // User doesn't exist, create the user
            user = new AuthUser();
            user.setEmailId(email);
            user.setUsername(name); // Set the user's name (or other data if needed)
            AuthUser createdUser = authUserRepo.save(user); // Save the new user to the database
            // save the same user into user-service as well
            userServiceClient.createUser(new UserDTO(createdUser.getUserId(), createdUser.getUsername(), createdUser.getEmailId()));
        }

        // Generate JWT token
        String jwt = jwtService.generateToken(name, user.getUserId());
        System.out.println("Inside oauth Success");
        System.out.println(jwt);

        // Store JWT in HTTP-only cookie
        Cookie jwtCookie = new Cookie("JWT", jwt);
        jwtCookie.setHttpOnly(true); // Prevents JavaScript from accessing the cookie
        jwtCookie.setPath("/"); // Make sure the cookie is accessible for the entire domain
        jwtCookie.setMaxAge(3600); // Optional: set cookie expiration (e.g., 1 hour)
        jwtCookie.setSecure(true); // Optional: set to true if using HTTPS
        response.addCookie(jwtCookie); // Add the cookie to the response
        response.sendRedirect("http://localhost:3000/home");
    }

}

