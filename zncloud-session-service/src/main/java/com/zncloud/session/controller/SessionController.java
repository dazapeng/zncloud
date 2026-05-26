package com.zncloud.session.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.session.model.entity.Session;
import com.zncloud.session.model.vo.RiskEventVO;
import com.zncloud.session.model.vo.SessionVO;
import com.zncloud.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * 创建会话
     */
    @PostMapping
    public ResponseEntity<Session> createSession(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String deviceId = (String) request.get("deviceId");
        Session session = sessionService.createSession(userId, deviceId);
        return ResponseEntity.ok(session);
    }

    /**
     * 开始会话
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<Session> startSession(@PathVariable String sessionId) {
        Session session = sessionService.startSession(sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * 结束会话
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Session> endSession(@PathVariable String sessionId) {
        Session session = sessionService.endSession(sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * 强制断开会话
     */
    @PostMapping("/{sessionId}/disconnect")
    public ResponseEntity<Session> disconnectSession(@PathVariable String sessionId,
                                                     @RequestBody(required = false) Map<String, String> request) {
        String reason = request != null ? request.getOrDefault("reason", "Manual disconnect") : "Manual disconnect";
        Session session = sessionService.disconnectSession(sessionId, reason);
        return ResponseEntity.ok(session);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionVO> getSession(@PathVariable String sessionId) {
        SessionVO vo = sessionService.getSessionVO(sessionId);
        if (vo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vo);
    }

    /**
     * 分页查询会话列表
     */
    @GetMapping
    public ResponseEntity<IPage<SessionVO>> listSessions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<SessionVO> page = sessionService.querySessions(userId, deviceId, status, pageNum, pageSize);
        return ResponseEntity.ok(page);
    }

    /**
     * 获取活跃会话数
     */
    @GetMapping("/active-count")
    public ResponseEntity<Map<String, Long>> getActiveCount() {
        long count = sessionService.getActiveSessionCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 风险事件分页查询
     */
    @GetMapping("/risk-events")
    public ResponseEntity<IPage<RiskEventVO>> listRiskEvents(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<RiskEventVO> page = sessionService.queryRiskEvents(pageNum, pageSize);
        return ResponseEntity.ok(page);
    }

    /**
     * 获取风险事件详情
     */
    @GetMapping("/risk-events/{checkLogId}")
    public ResponseEntity<RiskEventVO> getRiskEventDetail(@PathVariable Long checkLogId) {
        RiskEventVO vo = sessionService.getRiskEventDetail(checkLogId);
        if (vo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vo);
    }

    /**
     * 标记为误报
     */
    @PostMapping("/risk-events/{checkLogId}/false-positive")
    public ResponseEntity<Void> markAsFalsePositive(
            @PathVariable Long checkLogId,
            @RequestBody(required = false) Map<String, Object> request) {
        Long operatorId = request != null && request.containsKey("operatorId")
                ? Long.valueOf(request.get("operatorId").toString()) : 0L;
        String comment = request != null ? (String) request.getOrDefault("comment", "") : "";
        sessionService.markAsFalsePositive(checkLogId, operatorId, comment);
        return ResponseEntity.ok().build();
    }
}
