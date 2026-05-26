-- ============================================================
-- ZN Cloud V2 数据库迁移脚本
-- 精细化运营管理后台所需表结构
-- ============================================================

-- 1. 设备表新增运营商线路字段
ALTER TABLE `device`
    ADD COLUMN `isp` VARCHAR(30) DEFAULT NULL COMMENT '运营商: 电信/联通/移动/多线' AFTER `public_ip`,
    ADD COLUMN `district` VARCHAR(50) DEFAULT NULL COMMENT '区/县（冗余字段，从网吧填充）' AFTER `city`,
    ADD INDEX `idx_isp` (`isp`),
    ADD INDEX `idx_province_city` (`province`, `city`);

-- 2. 运营通知表
CREATE TABLE IF NOT EXISTS `operation_notification` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `title`         VARCHAR(200)    NOT NULL COMMENT '通知标题',
    `content`       TEXT            NOT NULL COMMENT '通知内容',
    `type`          VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM' COMMENT '通知类型: SYSTEM/MAINTENANCE/PROMOTION/URGENT',
    `target_type`   VARCHAR(20)     DEFAULT NULL COMMENT '推送目标: ALL/PROVINCE/CITY/CAFE',
    `target_value`  VARCHAR(255)    DEFAULT NULL COMMENT '推送目标值',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/CANCELLED',
    `publisher_id`  BIGINT          DEFAULT NULL COMMENT '发布人用户ID',
    `published_at`  DATETIME        DEFAULT NULL COMMENT '发布时间',
    `created_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_status` (`status`),
    KEY `idx_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运营通知表';

-- 3. 批量调价记录表
CREATE TABLE IF NOT EXISTS `batch_price_change_log` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `operator_id`     BIGINT          NOT NULL COMMENT '操作人用户ID',
    `action_type`     VARCHAR(30)     NOT NULL COMMENT '操作类型: BY_REGION/BY_CONFIG/BY_ISP/BY_CUSTOM',
    `filter_criteria` JSON            NOT NULL COMMENT '筛选条件（JSON）',
    `adjustment_type` VARCHAR(20)     NOT NULL COMMENT '调价方式: FIXED/PERCENTAGE',
    `adjustment_value` DECIMAL(10,2)  NOT NULL COMMENT '调价数值',
    `affected_count`  INT             NOT NULL DEFAULT 0 COMMENT '影响设备数',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED' COMMENT '状态',
    `created_at`      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operator` (`operator_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量调价记录表';
