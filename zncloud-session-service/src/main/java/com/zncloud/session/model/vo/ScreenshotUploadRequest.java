package com.zncloud.session.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 截图上传请求
 */
@Data
public class ScreenshotUploadRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** Base64编码的图片数据 */
    @NotBlank(message = "图片数据不能为空")
    private String imageData;

    /** 文件名 */
    private String fileName;

    /** 文件大小 */
    private Integer fileSize;
}
