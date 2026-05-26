package com.zncloud.user.admin.controller;

import com.zncloud.user.admin.dto.CreateWebhookRequest;
import com.zncloud.user.admin.dto.UpdateWebhookRequest;
import com.zncloud.user.admin.dto.WebhookResponse;
import com.zncloud.user.admin.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook 订阅管理 API
 */
@RestController
@RequestMapping("/api/v1/admin/webhooks")
public class AdminWebhookController {

    @Autowired
    private WebhookService webhookService;

    /**
     * 创建 Webhook 订阅
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWebhook(
            @Valid @RequestBody CreateWebhookRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        WebhookResponse response = webhookService.createWebhook(request, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", response);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询 Webhook 列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listWebhooks() {
        List<WebhookResponse> webhooks = webhookService.listWebhooks();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", webhooks);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新 Webhook 配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWebhookRequest request) {
        WebhookResponse response = webhookService.updateWebhook(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", response);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除 Webhook 订阅
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Webhook 已删除");
        result.put("data", null);
        return ResponseEntity.ok(result);
    }
}
