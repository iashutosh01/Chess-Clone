package com.example.Docker.user_service.service;
import com.example.Docker.user_service.model.DTO.UserDTO;
import com.example.Docker.user_service.model.User;
import com.example.Docker.user_service.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;

    public UserDTO getUserById(Long id) {
        User user = userRepo.getUserByUserId(id);
        if(user == null) {
            throw new RuntimeException("User not found");
        }
        UserDTO userDTO = new UserDTO(user.getUserId(), user.getUsername(), user.getEmailId());
        return userDTO;
    }

    public UserDTO getUserByUsername(String username){
        User user = userRepo.getUserByUsername(username);
        if(user == null) {
            throw new RuntimeException("User not found");
        }
        UserDTO userDTO = new UserDTO(user.getUserId(), user.getUsername(), user.getEmailId());
        return userDTO;
    }

    public UserDTO createUser(UserDTO user) {
        System.out.println("Inside User-Service userService "+user.getUserId() + " " + user.getUsername() + " " + user.getEmailId());
        User createdUser = new User(user.getUserId(), user.getUsername(), user.getEmailId());
        createdUser.setRating(250);

        User savedUser = userRepo.save(createdUser);
        return user;
    }
}
