package com.ordercraft.service;

import com.ordercraft.dto.request.LoginRequest;
import com.ordercraft.dto.request.RegisterRequest;
import com.ordercraft.dto.response.JwtAuthResponse;
import com.ordercraft.dto.response.UserResponse;

public interface AuthService {
    JwtAuthResponse login(LoginRequest loginRequest);
    UserResponse register(RegisterRequest registerRequest);
}
