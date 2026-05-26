-- ============================================================
-- ZN Cloud Device Service - 数据库初始化脚本
-- 数据库: zncloud_db
-- ============================================================

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

-- 网吧表
CREATE TABLE IF NOT EXISTS `cafe` (
    `id`                VARCHAR(36)     NOT NULL COMMENT '网吧ID(UUID)',
    `name`              VARCHAR(100)    NOT NULL COMMENT '网吧名称',
    `province`          VARCHAR(50)     DEFAULT NULL COMMENT '省份',
    `city`              VARCHAR(50)     DEFAULT NULL COMMENT '城市',
    `district`          VARCHAR(50)     DEFAULT NULL COMMENT '区/县',
    `address`           VARCHAR(255)    DEFAULT NULL COMMENT '详细地址',
    `contact_phone`     VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      DEFAULT 0 COMMENT '逻辑删除(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_city` (`province`, `city`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网吧表';
