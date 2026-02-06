package com.example.Docker.auth_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long userId;

    @Column(name = "user_name", unique = true)
    String username;

    @Column(name = "email_id", unique = true)
    @Email
    String emailId;

    @Size(min=6,max=512)
    String password;

}
