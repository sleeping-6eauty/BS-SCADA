package com.example.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domain.Equipment;
import com.example.backend.domain.User;
import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.EquipmentAssignmentBatchRequest;
import com.example.backend.dto.EquipmentAssignmentRequest;
import com.example.backend.dto.EquipmentResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.UpdateUserRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<User> users = userService.listUsers(role, status);
        int from = Math.min(page * size, users.size());
        int to = Math.min(from + size, users.size());
        List<UserResponse> responses = users.subList(from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "User list retrieved", responses));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Validated @RequestBody SignupRequest request) {
        User user = userService.signup(request);
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(userId, request);
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null));
    }

    // GET /api/users/{userId}/equipments — 담당 설비 목록
    @GetMapping("/{userId}/equipments")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getUserEquipments(
            @PathVariable Long userId) {
        List<EquipmentResponse> responses = userService.listAssignedEquipments(userId)
                .stream()
                .map(this::toEquipmentResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Assigned equipments retrieved", responses));
    }

    // POST /api/users/{userId}/equipments — 설비 단건 할당
    @PostMapping("/{userId}/equipments")
    public ResponseEntity<ApiResponse<EquipmentResponse>> assignEquipment(
            @PathVariable Long userId,
            @Validated @RequestBody EquipmentAssignmentRequest request) {
        Equipment equipment = userService.assignEquipmentToUser(userId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment assigned", toEquipmentResponse(equipment)));
    }

    // POST /api/users/{userId}/equipments/batch — 담당 설비 전체 교체
    @PostMapping("/{userId}/equipments/batch")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> batchAssignEquipments(
            @PathVariable Long userId,
            @Validated @RequestBody EquipmentAssignmentBatchRequest request) {
        List<EquipmentResponse> responses = userService.assignEquipmentsToUser(userId, request)
                .stream()
                .map(this::toEquipmentResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipments assigned", responses));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }

    private EquipmentResponse toEquipmentResponse(Equipment e) {
        return new EquipmentResponse(
                e.getEquipmentId(), e.getEquipmentName(), e.getLineNo(), e.getZone(),
                e.getManufacturer(), e.getCycleTime(), e.getHealthScore(),
                e.getRemainingLife(), e.getReplacementDate());
    }
}
