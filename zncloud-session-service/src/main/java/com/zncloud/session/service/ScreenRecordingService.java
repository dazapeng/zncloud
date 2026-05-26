package com.zncloud.session.service;

import com.zncloud.session.model.entity.ScreenRecording;

/**
 * 屏幕录制服务接口
 */
public interface ScreenRecordingService {

    /**
     * 完成录制 - 合并所有分段并更新状态
     *
     * @param sessionId 会话ID
     * @return 合并后的录制记录
     */
    ScreenRecording completeRecording(String sessionId);

    /**
     * 合并分段录像文件为一个完整文件并上传到MinIO
     *
     * @param sessionId 会话ID
     * @return 合并后的文件MinIO路径
     */
    String mergeSegments(String sessionId);

    /**
     * 获取合并后文件的下载URL
     *
     * @param sessionId 会话ID
     * @return 预签名下载URL
     */
    String getDownloadUrl(String sessionId);
}
