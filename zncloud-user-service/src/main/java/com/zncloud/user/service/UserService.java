package com.zncloud.user.service;

import com.zncloud.user.model.dto.*;
import com.zncloud.user.model.entity.User;

public interface UserService {

    /**
     * 用户注册（短信验证码）
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录（短信验证码）
     */
    AuthResponse login(LoginRequest request);

    /**
     * 根据 ID 获取用户
     */
    User getUserById(Long id);

    /**
     * 更新用户资料
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 封禁用户
     */
    void banUser(Long userId);

    /**
     * 解封用户
     */
    void unbanUser(Long userId);

    /**
     * 刷新 token
     */
    AuthResponse refreshToken(String refreshToken);
}
