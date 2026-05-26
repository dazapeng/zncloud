package com.zncloud.user.controller;

import com.zncloud.user.model.dto.UpdateProfileRequest;
import com.zncloud.user.model.dto.UserProfileResponse;
import com.zncloud.user.model.entity.User;
import com.zncloud.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户信息（暂用 header 模拟，后续替换为 JWT 拦截器）
     */
    @GetMapping("/me")
    public ResponseEntity<AuthController.ApiResponse<UserProfileResponse>> getCurrentUser(
            @RequestHeader("X-User-Id") Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(AuthController.ApiResponse.success(UserProfileResponse.fromUser(user)));
    }

    /**
     * 更新当前用户资料
     */
    @PatchMapping("/me")
    public ResponseEntity<AuthController.ApiResponse<UserProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(AuthController.ApiResponse.success(response));
    }

    /**
     * 根据 ID 获取用户信息（管理员接口）
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuthController.ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(AuthController.ApiResponse.success(UserProfileResponse.fromUser(user)));
    }

    /**
     * 封禁用户（管理员接口）
     */
    @PostMapping("/{id}/ban")
    public ResponseEntity<AuthController.ApiResponse<Void>> banUser(@PathVariable Long id) {
        userService.banUser(id);
        return ResponseEntity.ok(AuthController.ApiResponse.success(null));
    }

    /**
     * 解封用户（管理员接口）
     */
    @PostMapping("/{id}/unban")
    public ResponseEntity<AuthController.ApiResponse<Void>> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.ok(AuthController.ApiResponse.success(null));
    }
}
