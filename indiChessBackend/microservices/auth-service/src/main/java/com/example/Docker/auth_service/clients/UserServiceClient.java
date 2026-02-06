package com.example.Docker.auth_service.clients;

import com.example.Docker.auth_service.model.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "user-service", url = "http://USER-SERVICE/api/v1/user")
public interface UserServiceClient {

    @PostMapping("/")
    UserDTO createUser(UserDTO user);
}

