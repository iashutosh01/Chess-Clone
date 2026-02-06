package com.example.Docker.match_service.model.DTO;

import lombok.Data;

@Data
public class UserDTO {
    Long userId;
    String username;
    String emailId;

    public UserDTO(Long userId, String username, String emailId) {
        this.userId = userId;
        this.username = username;
        this.emailId = emailId;
    }
}