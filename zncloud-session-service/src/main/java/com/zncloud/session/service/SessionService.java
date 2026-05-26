package com.zncloud.session.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.session.model.entity.Session;
import com.zncloud.session.model.vo.ContentCheckLogVO;
import com.zncloud.session.model.vo.RiskEventVO;
import com.zncloud.session.model.vo.SessionVO;

import java.util.List;

/**
 * 会话服务接口
 */
public interface SessionService {

    /**
     * 创建会话
     */
    Session createSession(Long userId, String deviceId);

    /**
     * 开始会话
     */
    Session startSession(String sessionId);

    /**
     * 结束会话
     */
    Session endSession(String sessionId);

    /**
     * 强制断开会话（违规等）
     */
    Session disconnectSession(String sessionId, String reason);

    /**
     * 根据ID获取会话
     */
    Session getSessionById(String sessionId);

    /**
     * 获取会话详情VO
     */
    SessionVO getSessionVO(String sessionId);

    /**
     * 分页查询会话列表
     */
    IPage<SessionVO> querySessions(Long userId, String deviceId, String status,
                                   Integer pageNum, Integer pageSize);

    /**
     * 获取当前活跃会话数
     */
    long getActiveSessionCount();

    /**
     * 风险事件分页查询
     */
    IPage<RiskEventVO> queryRiskEvents(Integer pageNum, Integer pageSize);

    /**
     * 获取风险事件详情
     */
    RiskEventVO getRiskEventDetail(Long checkLogId);

    /**
     * 标记为误报
     */
    void markAsFalsePositive(Long checkLogId, Long operatorId, String comment);
}
