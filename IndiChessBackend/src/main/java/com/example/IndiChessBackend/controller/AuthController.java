package com.example.IndiChessBackend.controller;

import com.example.IndiChessBackend.model.DTO.LoginDto;
import com.example.IndiChessBackend.model.DTO.LoginResponseDto;
import com.example.IndiChessBackend.model.User;
import com.example.IndiChessBackend.service.AuthService;
import com.example.IndiChessBackend.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@CrossOrigin(value = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authservice;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    @PostMapping("signup")
    public ResponseEntity<User> handleSignup(@RequestBody User user){
//        System.out.println(user);
        return new ResponseEntity<>(authservice.save(user), HttpStatus.CREATED);
    }

    @PostMapping("login")
    public ResponseEntity<?> handleLogin(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        @RequestBody LoginDto loginDto) throws IOException {


        Authentication authObject = authenticationManager.
                authenticate(new
                        UsernamePasswordAuthenticationToken
                        (loginDto.getUsername(), loginDto.getPassword()));
        if(authObject.isAuthenticated()) {
            String tk = jwtService.generateToken(loginDto.getUsername());
            System.out.println("Inside Auth controller");
            System.out.println(tk);


            Cookie jwtCookie = new Cookie("JWT", tk);
            jwtCookie.setHttpOnly(true); // Prevents JavaScript from accessing the cookie
            jwtCookie.setPath("/"); // Make sure the cookie is accessible for the entire domain
            jwtCookie.setMaxAge(3600); // Optional: set cookie expiration (e.g., 1 hour)
            jwtCookie.setSecure(true); // Optional: set to true if using HTTPS
            response.addCookie(jwtCookie);


            return ResponseEntity.ok("Signed in");
        }

        return new ResponseEntity<>(new LoginResponseDto(null, "Auth Failed"), HttpStatus.BAD_REQUEST);
    }

//    @GetMapping("home")
//    public String handleHome(){
//        System.out.println("Home");
//        return "Home";
//    }





}
