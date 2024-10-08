package com.quip.backend.service;

import com.quip.backend.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private List<User> users = new ArrayList<>();

    // Retrieve all users
    public List<User> getAllUsers() {
        return users;
    }

    // Retrieve a user by ID
    public User getUserById(Long id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Create a new user
    public User createUser(User user) {
        users.add(user);
        return user;
    }

    // Update an existing user
    public User updateUser(Long id, User updatedUser) {
        User user = getUserById(id);
        if (user != null) {
            user.setUsername(updatedUser.getUsername());
            user.setEmail(updatedUser.getEmail());
        }
        return user;
    }

    // Delete a user
    public void deleteUser(Long id) {
        users.removeIf(user -> user.getId().equals(id));
    }
}
