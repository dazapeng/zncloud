package com.zncloud.user.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class CreateAccessKeyRequest {

    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 100, message = "密钥名称最长100个字符")
    private String name;

    @Size(max = 500, message = "权限列表过长")
    private String permissions;

    private LocalDateTime expiredAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
}
