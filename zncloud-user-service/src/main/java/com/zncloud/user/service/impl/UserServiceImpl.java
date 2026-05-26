package com.zncloud.user.service.impl;

import com.zncloud.common.util.JwtUtil;
import com.zncloud.user.mapper.UserMapper;
import com.zncloud.user.model.dto.*;
import com.zncloud.user.model.entity.User;
import com.zncloud.user.model.enums.UserRole;
import com.zncloud.user.model.enums.UserStatus;
import com.zncloud.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final long SMS_CODE_EXPIRE_SECONDS = 300;          // 5分钟
    private static final long LOGIN_LOCK_DURATION_MINUTES = 30;      // 锁定30分钟
    private static final int MAX_LOGIN_FAILURES = 5;                 // 5次锁定
    private static final long JWT_EXPIRE_MILLIS = 86400000L;         // 24小时
    private static final String REDIS_SMS_PREFIX = "sms:code:";
    private static final String REDIS_LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String REDIS_LOCK_PREFIX = "login:lock:";
    private static final String REDIS_REFRESH_PREFIX = "refresh:token:";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String phone = request.getPhone();
        String smsCode = request.getSmsCode();

        // 1. 校验短信验证码
        verifySmsCode(phone, smsCode);

        // 2. 检查手机号是否已注册
        User existingUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
        );
        if (existingUser != null) {
            throw new RuntimeException("该手机号已注册");
        }

        // 3. 创建新用户
        User user = new User();
        user.setPhone(phone);
        // 使用手机号后6位作为初始密码，进行 bcrypt 加密
        String rawPassword = phone.substring(phone.length() - 6);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + phone.substring(phone.length() - 4));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setBalance(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());

        userMapper.insert(user);

        // 4. 删除已使用的验证码
        redisTemplate.delete(REDIS_SMS_PREFIX + phone);

        // 5. 生成 token
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole().getCode());
        String refreshToken = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole().getCode());
        // 存储 refresh token (24小时有效期)
        redisTemplate.opsForValue().set(REDIS_REFRESH_PREFIX + refreshToken, String.valueOf(user.getId()),
                JWT_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);

        return new AuthResponse(token, refreshToken, JWT_EXPIRE_MILLIS / 1000,
                UserProfileResponse.fromUser(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String smsCode = request.getSmsCode();

        // 1. 检查是否被锁定
        checkLoginLock(phone);

        // 2. 查询用户
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
        );
        if (user == null) {
            throw new RuntimeException("手机号未注册");
        }

        // 3. 检查用户状态
        if (user.getStatus() == UserStatus.BANNED) {
            throw new RuntimeException("账号已被封禁");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new RuntimeException("账号已被锁定");
        }

        // 4. 校验短信验证码
        try {
            verifySmsCode(phone, smsCode);
        } catch (RuntimeException e) {
            // 验证码错误，记录失败次数
            recordLoginFailure(phone);
            throw e;
        }

        // 5. 登录成功，清除失败记录
        redisTemplate.delete(REDIS_LOGIN_FAIL_PREFIX + phone);
        redisTemplate.delete(REDIS_LOCK_PREFIX + phone);

        // 6. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 7. 删除已使用的验证码
        redisTemplate.delete(REDIS_SMS_PREFIX + phone);

        // 8. 生成 token
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole().getCode());
        String refreshToken = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole().getCode());
        redisTemplate.opsForValue().set(REDIS_REFRESH_PREFIX + refreshToken, String.valueOf(user.getId()),
                JWT_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);

        return new AuthResponse(token, refreshToken, JWT_EXPIRE_MILLIS / 1000,
                UserProfileResponse.fromUser(user));
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return UserProfileResponse.fromUser(user);
    }

    @Override
    @Transactional
    public void banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(UserStatus.BANNED);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // 校验 refreshToken 是否有效
        String userIdStr = redisTemplate.opsForValue().get(REDIS_REFRESH_PREFIX + refreshToken);
        if (userIdStr == null) {
            throw new RuntimeException("refreshToken 无效或已过期");
        }

        if (!jwtUtil.validateToken(refreshToken)) {
            redisTemplate.delete(REDIS_REFRESH_PREFIX + refreshToken);
            throw new RuntimeException("refreshToken 无效或已过期");
        }

        Long userId = Long.valueOf(userIdStr);
        User user = userMapper.selectById(userId);
        if (user == null) {
            redisTemplate.delete(REDIS_REFRESH_PREFIX + refreshToken);
            throw new RuntimeException("用户不存在");
        }

        // 删除旧的 refreshToken
        redisTemplate.delete(REDIS_REFRESH_PREFIX + refreshToken);

        // 生成新的 token
        String newToken = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole().getCode());
        String newRefreshToken = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole().getCode());
        redisTemplate.opsForValue().set(REDIS_REFRESH_PREFIX + newRefreshToken, String.valueOf(user.getId()),
                JWT_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);

        return new AuthResponse(newToken, newRefreshToken, JWT_EXPIRE_MILLIS / 1000,
                UserProfileResponse.fromUser(user));
    }

    // ===== 私有辅助方法 =====

    /**
     * 校验短信验证码
     */
    private void verifySmsCode(String phone, String inputCode) {
        String cachedCode = redisTemplate.opsForValue().get(REDIS_SMS_PREFIX + phone);
        if (cachedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(inputCode)) {
            throw new RuntimeException("验证码错误");
        }
    }

    /**
     * 检查登录锁定
     */
    private void checkLoginLock(String phone) {
        String lockKey = REDIS_LOCK_PREFIX + phone;
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            throw new RuntimeException("登录失败次数过多，账号已被锁定30分钟");
        }
    }

    /**
     * 记录登录失败次数
     */
    private void recordLoginFailure(String phone) {
        String failKey = REDIS_LOGIN_FAIL_PREFIX + phone;
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count == null) count = 1L;

        if (count == 1) {
            // 第一次失败，设置过期时间（重置周期）
            redisTemplate.expire(failKey, LOGIN_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        if (count >= MAX_LOGIN_FAILURES) {
            // 锁定账号
            String lockKey = REDIS_LOCK_PREFIX + phone;
            redisTemplate.opsForValue().set(lockKey, "locked", LOGIN_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            // 清除失败计数（锁定期间不再累计）
            redisTemplate.delete(failKey);
            throw new RuntimeException("登录失败次数过多，账号已被锁定30分钟");
        }
    }
}
