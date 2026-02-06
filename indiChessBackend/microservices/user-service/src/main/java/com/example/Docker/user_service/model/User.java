package com.example.Docker.user_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UniqueElements;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User  {
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long userId;
    @Size(min = 4, max = 50,
            message = "Username must have characters between 4 and 50")
    @Column(name = "user_name", unique = true)
    String username;

    @Column(name = "email_id", unique = true)
    @Email
    String emailId;

    String pfpUrl;

    String country;

    Integer rating ;

    public User(Long userId, String username, String emailId) {
        this.userId = userId;
        this.username = username;
        this.emailId = emailId;
    }
}

