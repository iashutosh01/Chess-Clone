package com.example.Docker.auth_service.model.DTO;

public class AuthUserDTO {
    Long userId;
    String username;
    String emailId;

    public AuthUserDTO(Long userId, String username, String emailId) {
        this.userId = userId;
        this.username = username;
        this.emailId = emailId;
    }
}
