package com.ordercraft.service;

import com.ordercraft.dto.request.UserUpdateRequest;
import com.ordercraft.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUsers(String query, Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void toggleUserStatus(Long id);
}
