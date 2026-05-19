package com.example.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.backend.domain.Equipment;
import com.example.backend.domain.User;
import com.example.backend.dto.EquipmentAssignmentBatchRequest;
import com.example.backend.dto.EquipmentAssignmentRequest;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.UpdateUserRequest;
import com.example.backend.mapper.EquipmentMapper;
import com.example.backend.mapper.UserEquipmentMapper;
import com.example.backend.mapper.UserMapper;

import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final EquipmentMapper equipmentMapper;
    private final UserEquipmentMapper userEquipmentMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserMapper userMapper,
                       EquipmentMapper equipmentMapper,
                       UserEquipmentMapper userEquipmentMapper,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.equipmentMapper = equipmentMapper;
        this.userEquipmentMapper = userEquipmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(SignupRequest request) {
        User existing = userMapper.findByEmail(request.getEmail());

        if (existing != null) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();

        user.setName(request.getUsername());
        user.setEmail(request.getEmail());

        // User 클래스에는 password가 아니라 passwordHash가 있음
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setRole(normalizeRole(request.getRole()));
        user.setStatus("active");

        userMapper.insert(user);

        return user;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }

        String normalized = role.trim().toUpperCase();

        if ("ADMIN".equals(normalized) || "USER".equals(normalized)) {
            return normalized;
        }

        return "USER";
    }

    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    public User findByUsername(String username) {
        return findByEmail(username);
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public List<Equipment> listAssignedEquipments(Long userId) {
        return userEquipmentMapper.findEquipmentsByUserId(userId);
    }

    public Equipment assignEquipmentToUser(Long userId, EquipmentAssignmentRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Equipment equipment = equipmentMapper.findByEquipmentCode(request.getEquipmentId());
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }

        userEquipmentMapper.assignEquipmentToUser(userId, request.getEquipmentId());
        return equipment;
    }

    @Transactional
    public List<Equipment> assignEquipmentsToUser(Long userId, EquipmentAssignmentBatchRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (request.getEquipmentIds() == null) {
            throw new IllegalArgumentException("equipmentIds cannot be null");
        }

        List<Equipment> equipments = request.getEquipmentIds().stream()
                .map(equipmentMapper::findByEquipmentCode)
                .peek(equipment -> {
                    if (equipment == null) {
                        throw new IllegalArgumentException("Equipment not found");
                    }
                })
                .collect(Collectors.toList());

        userEquipmentMapper.deleteByUserId(userId);

        if (!request.getEquipmentIds().isEmpty()) {
            userEquipmentMapper.assignEquipmentsToUser(userId, request.getEquipmentIds());
        }

        return equipments;
    }

    public List<User> listUsers(String filterRole) {
        List<User> users = userMapper.findAll();

        if (filterRole == null || filterRole.isBlank()) {
            return users;
        }

        return users.stream()
                .filter(user ->
                        user.getRole() != null
                                && user.getRole().equalsIgnoreCase(filterRole)
                )
                .collect(Collectors.toList());
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User existing = userMapper.findById(id);

        if (existing == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            existing.setName(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            existing.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            existing.setRole(normalizeRole(request.getRole()));
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            existing.setStatus(request.getStatus());
        }

        userMapper.update(existing);

        return existing;
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}