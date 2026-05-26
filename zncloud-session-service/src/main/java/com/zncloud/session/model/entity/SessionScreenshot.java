package com.zncloud.session.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("session_screenshot")
public class SessionScreenshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** MinIO存储路径 */
    private String storagePath;

    /** 文件大小(字节) */
    private Integer fileSize;

    /** 缩略图路径 */
    private String thumbnailPath;

    /** 是否标记违规 */
    @TableField("is_flagged")
    private Boolean flagged;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
