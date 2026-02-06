package com.example.Docker.auth_service.repo;

import com.example.Docker.auth_service.model.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepo extends JpaRepository<AuthUser, Long> {

    AuthUser getUserByUsername(String username);

    AuthUser getUserByEmailId(String email);

    AuthUser findUserByUsername(String username);

    AuthUser getUserByUserId(Long userId);
}
