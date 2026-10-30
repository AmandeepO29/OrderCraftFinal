package com.ordercraft.service.impl;

import com.ordercraft.dto.request.UserUpdateRequest;
import com.ordercraft.dto.response.UserResponse;
import com.ordercraft.entity.Role;
import com.ordercraft.entity.RoleType;
import com.ordercraft.entity.User;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.RoleRepository;
import com.ordercraft.repository.UserRepository;
import com.ordercraft.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String query, Pageable pageable) {
        Page<User> users;
        if (StringUtils.hasText(query)) {
            users = userRepository.searchUsers(query.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (StringUtils.hasText(request.getEmail()) && !user.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
            if (userRepository.existsByEmail(request.getEmail().trim())) {
                throw new BusinessRuleException("Email '" + request.getEmail() + "' is already taken");
            }
            user.setEmail(request.getEmail().trim());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String rName : request.getRoles()) {
                try {
                    String clean = rName.toUpperCase();
                    if (!clean.startsWith("ROLE_")) clean = "ROLE_" + clean;
                    RoleType type = RoleType.valueOf(clean);
                    Role role = roleRepository.findByName(type)
                            .orElseGet(() -> roleRepository.save(new Role(type, type.name())));
                    roles.add(role);
                } catch (IllegalArgumentException ignored) {}
            }
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setPhone(user.getPhone());
        res.setActive(user.isActive());
        res.setCreatedAt(user.getCreatedAt());
        res.setRoles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()));
        return res;
    }
}
