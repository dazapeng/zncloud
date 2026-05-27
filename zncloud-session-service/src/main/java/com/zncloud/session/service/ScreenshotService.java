package com.zncloud.session.service;

import com.zncloud.session.model.entity.SessionScreenshot;

/**
 * 截图管理服务接口
 */
public interface ScreenshotService {

    /**
     * 上传截图到MinIO
     *
     * @param sessionId 会话ID
     * @param imageData 图片二进制数据
     * @param fileName  文件名
     * @param fileSize  文件大小
     * @return 截图记录
     */
    SessionScreenshot uploadScreenshot(String sessionId, byte[] imageData, String fileName, int fileSize);

    /**
     * 获取截图预签名URL
     *
     * @param screenshotId 截图ID
     * @return 预签名URL
     */
    String getScreenshotUrl(Long screenshotId);

    /**
     * 获取截图的缩略图URL
     *
     * @param screenshotId 截图ID
     * @return 缩略图预签名URL
     */
    String getThumbnailUrl(Long screenshotId);

    /**
     * 清理过期截图（超过180天的标记截图）
     */
    void cleanupExpiredScreenshots();
}
