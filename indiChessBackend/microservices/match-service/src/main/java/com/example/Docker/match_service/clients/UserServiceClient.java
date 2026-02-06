package com.example.Docker.match_service.clients;


import com.example.Docker.match_service.model.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://USER-SERVICE/api/v1/user")
public interface UserServiceClient {

    // Method to get user details by userId
    @GetMapping("/{id}")
    UserDTO getUserById(@PathVariable("id") Long userId);
    @GetMapping("/{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username);
}

