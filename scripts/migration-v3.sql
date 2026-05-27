-- ============================================================
-- ZN Cloud V3 数据库迁移脚本
-- BillingRate 模型改造 + 费率管理相关表
-- ============================================================

-- 1. 费率配置表（支持按网吧维度配置）
CREATE TABLE IF NOT EXISTS `billing_rates` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `cafe_id`         VARCHAR(36)     NOT NULL COMMENT '网吧ID（空字符串表示全局配置）',
    `config_level`    VARCHAR(30)     NOT NULL COMMENT '配置等级: ENTRY/MAINSTREAM/HIGH_PERFORMANCE',
    `price_per_hour`  DECIMAL(10,2)   NOT NULL COMMENT '每小时价格(元)',
    `discount_start`  INT             DEFAULT NULL COMMENT '闲时折扣开始时间（分钟数，如 1320=22:00）',
    `discount_end`    INT             DEFAULT NULL COMMENT '闲时折扣结束时间（分钟数，如 360=06:00）',
    `discount_rate`   DECIMAL(4,2)    DEFAULT NULL COMMENT '闲时折扣率（如 0.80 表示打8折）',
    `effective_at`    DATETIME        NOT NULL COMMENT '生效时间',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/HISTORY',
    `created_by`      VARCHAR(36)     DEFAULT NULL COMMENT '创建人用户ID',
    `remark`          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    `created_at`      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cafe_level_effective` (`cafe_id`, `config_level`, `effective_at`),
    INDEX `idx_cafe_id` (`cafe_id`),
    INDEX `idx_config_level` (`config_level`),
    INDEX `idx_status` (`status`),
    INDEX `idx_effective_at` (`effective_at`),
    INDEX `idx_cafe_status` (`cafe_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费率配置表';

-- 2. 模拟硬件配置表（用于 Mock 数据）
CREATE TABLE IF NOT EXISTS `cafe_hardware_config` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `cafe_id`         VARCHAR(36)     NOT NULL COMMENT '网吧ID',
    `config_level`    VARCHAR(30)     NOT NULL COMMENT '配置等级',
    `cpu_info`        VARCHAR(255)    DEFAULT NULL COMMENT 'CPU信息',
    `gpu_info`        VARCHAR(255)    DEFAULT NULL COMMENT 'GPU信息',
    `memory_gb`       INT             DEFAULT NULL COMMENT '内存大小(GB)',
    `disk_gb`         INT             DEFAULT NULL COMMENT '磁盘大小(GB)',
    `os_version`      VARCHAR(100)    DEFAULT NULL COMMENT '操作系统版本',
    `device_count`    INT             NOT NULL DEFAULT 0 COMMENT '该配置设备数量',
    `created_at`      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cafe_config` (`cafe_id`, `config_level`),
    INDEX `idx_cafe_id` (`cafe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网吧硬件配置表（模拟数据）';

-- 3. 费率变更日志表
CREATE TABLE IF NOT EXISTS `rate_change_log` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `cafe_id`         VARCHAR(36)     DEFAULT NULL COMMENT '网吧ID（NULL表示全局变更）',
    `config_level`    VARCHAR(30)     NOT NULL COMMENT '配置等级',
    `old_price`       DECIMAL(10,2)   DEFAULT NULL COMMENT '旧价格',
    `new_price`       DECIMAL(10,2)   NOT NULL COMMENT '新价格',
    `change_type`     VARCHAR(20)     NOT NULL COMMENT '变更类型: SINGLE/BATCH',
    `operator_id`     VARCHAR(36)     DEFAULT NULL COMMENT '操作人用户ID',
    `remark`          VARCHAR(255)    DEFAULT NULL COMMENT '变更备注',
    `created_at`      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_cafe_id` (`cafe_id`),
    INDEX `idx_config_level` (`config_level`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费率变更日志表';

-- ============================================================
-- 插入默认全局费率（Mock 数据）
-- ============================================================
INSERT IGNORE INTO `billing_rates` (`cafe_id`, `config_level`, `price_per_hour`, `discount_start`, `discount_end`, `discount_rate`, `effective_at`, `status`, `remark`) VALUES
('', 'ENTRY',            3.00,  1320, 480, 0.70, NOW(), 'ACTIVE', '全局默认 - 入门配置'),
('', 'MAINSTREAM',       5.00,  1320, 480, 0.75, NOW(), 'ACTIVE', '全局默认 - 主流配置'),
('', 'HIGH_PERFORMANCE', 8.00,  1320, 480, 0.80, NOW(), 'ACTIVE', '全局默认 - 高性能配置');
