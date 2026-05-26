-- ============================================================
-- ZN Cloud 数据库初始化脚本
-- 数据库: zncloud_db
-- 包含用户服务(user-service)和设备服务(device-service)的所有表
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `zncloud_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `zncloud_db`;

-- ============================================================
-- 用户服务相关表
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`         VARCHAR(20)     NOT NULL COMMENT '手机号',
    `password`      VARCHAR(255)    DEFAULT NULL COMMENT '密码（加密存储）',
    `nickname`      VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    `avatar`        VARCHAR(500)    DEFAULT NULL COMMENT '头像URL',
    `role`          VARCHAR(30)     NOT NULL DEFAULT 'USER' COMMENT '角色: USER/CAFE_ADMIN/OPERATOR/SUPER_ADMIN',
    `balance`       DECIMAL(12,2)   DEFAULT 0.00 COMMENT '账户余额(元)',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/BANNED/LOCKED',
    `wechat_openid` VARCHAR(64)     DEFAULT NULL COMMENT '微信OpenID',
    `last_login_at` DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    `created_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_wechat_openid` (`wechat_openid`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户登录日志表
CREATE TABLE IF NOT EXISTS `user_login_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `login_type`    VARCHAR(20)     NOT NULL DEFAULT 'PASSWORD' COMMENT '登录方式: PASSWORD/SMS/WECHAT',
    `ip_address`    VARCHAR(45)     DEFAULT NULL COMMENT '登录IP',
    `user_agent`    VARCHAR(500)    DEFAULT NULL COMMENT '客户端信息',
    `login_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

-- ============================================================
-- 设备服务相关表
-- ============================================================

-- 网吧表
CREATE TABLE IF NOT EXISTS `cafe` (
    `id`            VARCHAR(36)     NOT NULL COMMENT '网吧ID(UUID)',
    `name`          VARCHAR(100)    NOT NULL COMMENT '网吧名称',
    `province`      VARCHAR(50)     DEFAULT NULL COMMENT '省份',
    `city`          VARCHAR(50)     DEFAULT NULL COMMENT '城市',
    `district`      VARCHAR(50)     DEFAULT NULL COMMENT '区/县',
    `address`       VARCHAR(255)    DEFAULT NULL COMMENT '详细地址',
    `contact_phone` VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)      DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_city` (`province`, `city`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网吧表';

-- 设备表
CREATE TABLE IF NOT EXISTS `device` (
    `id`                VARCHAR(36)     NOT NULL COMMENT '设备ID(UUID)',
    `cafe_id`           VARCHAR(36)     DEFAULT NULL COMMENT '所属网吧ID',
    `cpu_info`          VARCHAR(255)    DEFAULT NULL COMMENT 'CPU信息',
    `gpu_info`          VARCHAR(255)    DEFAULT NULL COMMENT 'GPU信息',
    `memory_gb`         INT             DEFAULT NULL COMMENT '内存大小(GB)',
    `disk_gb`           INT             DEFAULT NULL COMMENT '磁盘大小(GB)',
    `os_version`        VARCHAR(100)    DEFAULT NULL COMMENT '操作系统版本',
    `mac_address`       VARCHAR(64)     DEFAULT NULL COMMENT 'MAC地址(唯一标识)',
    `public_ip`         VARCHAR(45)     DEFAULT NULL COMMENT '公网IP',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'REGISTERED' COMMENT '设备状态: REGISTERED/ONLINE/IN_USE/OFFLINE',
    `config_level`      VARCHAR(30)     DEFAULT NULL COMMENT '配置等级: ENTRY/MAINSTREAM/HIGH_PERFORMANCE',
    `price_per_hour`    DECIMAL(10,2)   DEFAULT 0.00 COMMENT '每小时价格(元)',
    `last_online_at`    DATETIME        DEFAULT NULL COMMENT '最后在线时间',
    `registered_at`     DATETIME        DEFAULT NULL COMMENT '注册时间',
    `total_online_hours` DOUBLE         DEFAULT 0.00 COMMENT '累计在线时长(小时)',
    `total_earnings`    DECIMAL(12,2)   DEFAULT 0.00 COMMENT '累计收益(元)',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_cafe_id` (`cafe_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_mac_address` (`mac_address`),
    INDEX `idx_config_level` (`config_level`),
    INDEX `idx_last_online_at` (`last_online_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

-- 设备配置变更记录表
CREATE TABLE IF NOT EXISTS `device_config_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `device_id`     VARCHAR(36)     NOT NULL COMMENT '设备ID',
    `field_name`    VARCHAR(50)     NOT NULL COMMENT '变更字段',
    `old_value`     VARCHAR(255)    DEFAULT NULL COMMENT '旧值',
    `new_value`     VARCHAR(255)    DEFAULT NULL COMMENT '新值',
    `operator_id`   VARCHAR(36)     DEFAULT NULL COMMENT '操作人ID',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_device_id` (`device_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备配置变更记录表';

-- 设备离线日志表
CREATE TABLE IF NOT EXISTS `device_offline_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `device_id`     VARCHAR(36)     NOT NULL COMMENT '设备ID',
    `last_online_at` DATETIME       DEFAULT NULL COMMENT '最后在线时间',
    `offline_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '离线检测时间',
    `duration_seconds` INT          DEFAULT 0 COMMENT '离线持续秒数',
    `handled`       TINYINT(1)      DEFAULT 0 COMMENT '是否已处理(0-未处理,1-已处理)',
    PRIMARY KEY (`id`),
    INDEX `idx_device_id` (`device_id`),
    INDEX `idx_offline_at` (`offline_at`),
    INDEX `idx_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备离线日志表';
