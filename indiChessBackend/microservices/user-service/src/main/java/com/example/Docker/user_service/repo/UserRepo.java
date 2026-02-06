package com.example.Docker.user_service.repo;

import com.example.Docker.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    User getUserByUsername(String username);

    User getUserByEmailId(String email);

    User findUserByUsername(String username);

    User getUserByUserId(Long userId);
}
