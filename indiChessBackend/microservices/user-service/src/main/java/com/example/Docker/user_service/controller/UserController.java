package com.example.Docker.user_service.controller;

import com.example.Docker.user_service.model.DTO.UserDTO;
import com.example.Docker.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id){
        return userService.getUserById(id);
    }
    @GetMapping("{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username){
        return userService.getUserByUsername(username);
    }

    @GetMapping("/home")
    public ResponseEntity<String> redirectToHome(){
        System.out.println("Return to home");
        return ResponseEntity.ok("Home");
    }

    @PostMapping("/")
    public UserDTO createUser(UserDTO user){
        return userService.createUser(user);
    }

}
