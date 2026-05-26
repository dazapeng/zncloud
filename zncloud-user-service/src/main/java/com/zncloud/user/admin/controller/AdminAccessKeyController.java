package com.zncloud.user.admin.controller;

import com.zncloud.user.admin.dto.AccessKeyResponse;
import com.zncloud.user.admin.dto.CreateAccessKeyRequest;
import com.zncloud.user.admin.dto.CreateAccessKeyResponse;
import com.zncloud.user.admin.service.AccessKeyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AK/SK 密钥管理 API（仅超级管理员）
 */
@RestController
@RequestMapping("/api/v1/admin/access-keys")
public class AdminAccessKeyController {

    @Autowired
    private AccessKeyService accessKeyService;

    /**
     * 创建密钥对
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createKey(
            @Valid @RequestBody CreateAccessKeyRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        CreateAccessKeyResponse response = accessKeyService.createKey(request, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", response);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询密钥列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listKeys(
            @RequestHeader("X-User-Id") Long userId) {
        List<AccessKeyResponse> keys = accessKeyService.listKeys(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", keys);
        return ResponseEntity.ok(result);
    }

    /**
     * 吊销密钥
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> revokeKey(@PathVariable Long id) {
        accessKeyService.revokeKey(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "密钥已吊销");
        result.put("data", null);
        return ResponseEntity.ok(result);
    }

    /**
     * AK/SK 签名验证（供网关调用）
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifySignature(@RequestBody Map<String, String> request) {
        String keyId = request.get("keyId");
        String signature = request.get("signature");
        String stringToSign = request.get("stringToSign");

        boolean valid = accessKeyService.verifySignature(keyId, signature, stringToSign);

        Map<String, Object> data = new HashMap<>();
        data.put("valid", valid);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return ResponseEntity.ok(result);
    }
}
