package com.ordercraft.service.impl;

import com.ordercraft.dto.request.LoginRequest;
import com.ordercraft.dto.request.RegisterRequest;
import com.ordercraft.dto.response.JwtAuthResponse;
import com.ordercraft.dto.response.UserResponse;
import com.ordercraft.entity.Role;
import com.ordercraft.entity.RoleType;
import com.ordercraft.entity.User;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.repository.RoleRepository;
import com.ordercraft.repository.UserRepository;
import com.ordercraft.security.JwtTokenProvider;
import com.ordercraft.security.UserPrincipal;
import com.ordercraft.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public JwtAuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtAuthResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getFullName(),
                roles
        );
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BusinessRuleException("Username '" + registerRequest.getUsername() + "' is already taken.");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessRuleException("Email '" + registerRequest.getEmail() + "' is already in use.");
        }

        User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getFullName(),
                registerRequest.getPhone()
        );

        Set<Role> roles = new HashSet<>();
        if (registerRequest.getRoles() != null && !registerRequest.getRoles().isEmpty()) {
            for (String roleName : registerRequest.getRoles()) {
                RoleType type;
                try {
                    String cleanName = roleName.toUpperCase();
                    if (!cleanName.startsWith("ROLE_")) {
                        cleanName = "ROLE_" + cleanName;
                    }
                    type = RoleType.valueOf(cleanName);
                } catch (IllegalArgumentException e) {
                    type = RoleType.ROLE_EMPLOYEE;
                }
                Role role = roleRepository.findByName(type)
                        .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_EMPLOYEE, "Employee role")));
                roles.add(role);
            }
        } else {
            Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                    .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_EMPLOYEE, "Employee role")));
            roles.add(employeeRole);
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        UserResponse response = new UserResponse();
        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setFullName(savedUser.getFullName());
        response.setPhone(savedUser.getPhone());
        response.setActive(savedUser.isActive());
        response.setRoles(savedUser.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()));
        response.setCreatedAt(savedUser.getCreatedAt());

        return response;
    }
}
