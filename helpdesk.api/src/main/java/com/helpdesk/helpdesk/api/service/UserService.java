package com.helpdesk.helpdesk.api.service;

import com.helpdesk.helpdesk.api.dto.request.UserCreateRequest;
import com.helpdesk.helpdesk.api.dto.response.UserResponse;
import com.helpdesk.helpdesk.api.entity.User;
import com.helpdesk.helpdesk.api.exception.NotFoundException;
import com.helpdesk.helpdesk.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserCreateRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse getUser(Long id) {
        User user = findUserOrThrow(id);
        return toResponse(user);
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
