package com.zncloud.session.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("screen_recording")
public class ScreenRecording {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 会话ID */
    private String sessionId;

    /** 分段编号 */
    private Integer segmentNumber;

    /** MinIO对象路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 录制状态：RECORDING / COMPLETED / FAILED */
    private String status;

    /** 是否已合并 */
    @TableField("is_merged")
    private Boolean merged;

    /** 合并后的完整文件MinIO路径 */
    private String mergedFilePath;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
