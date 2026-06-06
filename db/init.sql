-- ============================================================
--  铁路购票系统 - 数据库初始化脚本
--  数据库：MySQL 8.x
--  字符集：utf8mb4 / utf8mb4_unicode_ci
--  引擎：   InnoDB
--
--  用法：
--    mysql -u root -p < db/init.sql
--
--  注意：
--  1. 脚本会 DROP 现有 railway-real 库，请确认无重要数据
--    2. 雪花 ID 用 BIGINT（与 Java long 一致）
--    3. 微服务不建外键约束，user_id 等仅作业务关联
--    4. 初始数据：2 个示例车次 + 近 3 天各座位类型库存
--       用户需通过 service-user 的 /auth/register 注册
-- ============================================================

-- 1. 创建数据库
DROP DATABASE IF EXISTS `railway-real`;
CREATE DATABASE `railway-real` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `railway-real`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
--  2. 用户表
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL                                COMMENT '雪花ID',
  `username`    VARCHAR(50)  NOT NULL                                COMMENT '用户名',
  `password`    VARCHAR(100) NOT NULL                                COMMENT 'BCrypt 加密密码',
  `phone`       VARCHAR(20)  DEFAULT NULL                            COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL                            COMMENT '邮箱',
  `id_card`     VARCHAR(32)  DEFAULT NULL                            COMMENT '身份证号',
  `real_name`   VARCHAR(50)  DEFAULT NULL                            COMMENT '真实姓名',
  `user_type`   TINYINT      NOT NULL DEFAULT 0                      COMMENT '0-普通 1-管理员',
  `status`      TINYINT      NOT NULL DEFAULT 1                      COMMENT '0-禁用 1-正常',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
--  3. 乘车人表
-- ============================================================
DROP TABLE IF EXISTS `passenger`;
CREATE TABLE `passenger` (
  `id`             BIGINT       NOT NULL                                COMMENT '雪花ID',
  `user_id`        BIGINT       NOT NULL                                COMMENT '所属用户ID',
  `name`           VARCHAR(50)  NOT NULL                                COMMENT '姓名',
  `id_card_type`   TINYINT      NOT NULL DEFAULT 1                      COMMENT '证件类型 1-身份证',
  `id_card_no`     VARCHAR(32)  NOT NULL                                COMMENT '证件号码',
  `phone`          VARCHAR(20)  DEFAULT NULL                            COMMENT '手机号',
  `passenger_type` TINYINT      NOT NULL DEFAULT 0                      COMMENT '0-成人 1-儿童 2-学生',
  `is_default`     TINYINT      NOT NULL DEFAULT 0                      COMMENT '0-否 1-默认乘车人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_id_card` (`id_card_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='乘车人表';

-- ============================================================
--  4. 车次表
-- ============================================================
DROP TABLE IF EXISTS `train`;
CREATE TABLE `train` (
  `id`            BIGINT       NOT NULL                                COMMENT '雪花ID',
  `train_no`      VARCHAR(20)  NOT NULL                                COMMENT '车次号 G1234',
  `train_type`    VARCHAR(10)  NOT NULL                                COMMENT 'G/D/K/T',
  `start_station` VARCHAR(50)  NOT NULL                                COMMENT '始发站',
  `end_station`   VARCHAR(50)  NOT NULL                                COMMENT '终点站',
  `start_time`    DATETIME     NOT NULL                                COMMENT '发车时间（当日）',
  `end_time`      DATETIME     NOT NULL                                COMMENT '到达时间（当日）',
  `run_days`      VARCHAR(20)  NOT NULL DEFAULT '1111111'             COMMENT '运行日 周一~周日 0/1',
  `status`        TINYINT      NOT NULL DEFAULT 1                      COMMENT '0-停运 1-正常',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_train_no` (`train_no`),
  KEY `idx_start_end` (`start_station`, `end_station`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车次表';

-- ============================================================
--  5. 座位库存表（按 车次 + 出发日期 + 座位类型 唯一）
-- ============================================================
DROP TABLE IF EXISTS `train_seat_stock`;
CREATE TABLE `train_seat_stock` (
  `id`        BIGINT      NOT NULL                                COMMENT '雪花ID',
  `train_no`  VARCHAR(20) NOT NULL                                COMMENT '车次号',
  `run_date`  DATE        NOT NULL                                COMMENT '出发日期',
  `seat_type` VARCHAR(20) NOT NULL                                COMMENT 'BUSINESS/FIRST/SECOND/STAND',
  `total`     INT         NOT NULL                                COMMENT '总票数',
  `remain`    INT         NOT NULL                                COMMENT '剩余票数',
  `version`   INT         NOT NULL DEFAULT 0                      COMMENT '乐观锁版本号',
  `create_time` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_train_date_seat` (`train_no`, `run_date`, `seat_type`),
  KEY `idx_run_date` (`run_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位库存表';

-- ============================================================
--  6. 库存流水表
-- ============================================================
DROP TABLE IF EXISTS `stock_flow`;
CREATE TABLE `stock_flow` (
  `id`          BIGINT       NOT NULL                                COMMENT '雪花ID',
  `order_no`    VARCHAR(32)  NOT NULL                                COMMENT '订单号',
  `train_no`    VARCHAR(20)  NOT NULL                                COMMENT '车次号',
  `run_date`    DATE         NOT NULL                                COMMENT '出发日期',
  `seat_type`   VARCHAR(20)  NOT NULL                                COMMENT '座位类型',
  `delta`       INT          NOT NULL                                COMMENT '正数=释放 负数=预占/扣减',
  `biz_type`    TINYINT      NOT NULL                                COMMENT '1-预占 2-确认 3-释放',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_no`),
  KEY `idx_train_date` (`train_no`, `run_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- ============================================================
--  7. 票表
-- ============================================================
DROP TABLE IF EXISTS `ticket`;
CREATE TABLE `ticket` (
  `id`             BIGINT       NOT NULL                                COMMENT '雪花ID',
  `ticket_no`      VARCHAR(32)  NOT NULL                                COMMENT '业务票号 T+雪花',
  `order_no`       VARCHAR(32)  NOT NULL                                COMMENT '订单号',
  `train_no`       VARCHAR(20)  NOT NULL                                COMMENT '车次号',
  `run_date`       DATE         NOT NULL                                COMMENT '出发日期',
  `seat_type`      VARCHAR(20)  NOT NULL                                COMMENT '座位类型',
  `carriage_no`    INT          DEFAULT NULL                            COMMENT '车厢号',
  `seat_no`        VARCHAR(10)  DEFAULT NULL                            COMMENT '座位号',
  `passenger_id`   BIGINT       NOT NULL                                COMMENT '乘车人ID',
  `passenger_name` VARCHAR(50)  NOT NULL                                COMMENT '乘车人姓名（冗余）',
  `id_card_no`     VARCHAR(32)  NOT NULL                                COMMENT '证件号（冗余）',
  `price`          DECIMAL(10,2) NOT NULL                              COMMENT '票价',
  `status`         TINYINT      NOT NULL DEFAULT 0                      COMMENT '0-待支付 1-已出票 2-已改签 3-已退',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_order` (`order_no`),
  KEY `idx_passenger` (`passenger_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票表';

-- ============================================================
--  8. 订单表
-- ============================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`          BIGINT        NOT NULL                                COMMENT '雪花ID',
  `order_no`    VARCHAR(32)   NOT NULL                                COMMENT '订单号',
  `user_id`     BIGINT        NOT NULL                                COMMENT '用户ID',
  `train_no`    VARCHAR(20)   NOT NULL                                COMMENT '车次号',
  `run_date`    DATE          NOT NULL                                COMMENT '出发日期',
  `seat_type`   VARCHAR(20)   NOT NULL                                COMMENT '座位类型',
  `num`         INT           NOT NULL                                COMMENT '购票数量',
  `amount`      DECIMAL(10,2) NOT NULL                                COMMENT '订单金额',
  `status`      TINYINT       NOT NULL DEFAULT 0                      COMMENT '0-待支付 1-已支付 2-已取消 3-已退款 4-已完成',
  `expire_time` DATETIME      NOT NULL                                COMMENT '支付截止时间',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
--  9. 支付流水表
-- ============================================================
DROP TABLE IF EXISTS `pay_record`;
CREATE TABLE `pay_record` (
  `id`          BIGINT        NOT NULL                                COMMENT '雪花ID',
  `pay_no`      VARCHAR(32)   NOT NULL                                COMMENT '支付单号',
  `order_no`    VARCHAR(32)   NOT NULL                                COMMENT '订单号',
  `amount`      DECIMAL(10,2) NOT NULL                                COMMENT '支付金额',
  `pay_channel` VARCHAR(20)   NOT NULL DEFAULT 'SIM'                  COMMENT 'ALIPAY/WECHAT/SIM',
  `status`      TINYINT       NOT NULL DEFAULT 0                      COMMENT '0-待支付 1-成功 2-失败 3-已关闭',
  `paid_time`   DATETIME      DEFAULT NULL                            COMMENT '支付成功时间',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no` (`pay_no`),
  KEY `idx_order` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ============================================================
--  10. 通知日志表（service-notification 阶段九新增）
-- ============================================================
DROP TABLE IF EXISTS `notification_log`;
CREATE TABLE `notification_log` (
  `id`          BIGINT       NOT NULL                                COMMENT '雪花ID',
  `order_no`    VARCHAR(32)  NOT NULL                                COMMENT '订单号',
  `type`        TINYINT      NOT NULL                                COMMENT '0-支付成功 1-订单取消 2-订单超时关闭',
  `channel`     VARCHAR(20)  NOT NULL DEFAULT 'LOG'                  COMMENT 'LOG/SMS/EMAIL',
  `receiver`    VARCHAR(100) DEFAULT NULL                            COMMENT '接收方（手机号/邮箱/demo-user）',
  `content`     VARCHAR(500) NOT NULL                                COMMENT '通知内容',
  `status`      TINYINT      NOT NULL DEFAULT 0                      COMMENT '0-成功 1-失败',
  `retry_count` INT          NOT NULL DEFAULT 0                      COMMENT '重试次数',
  `error_msg`   VARCHAR(500) DEFAULT NULL                            COMMENT '失败原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_no`),
  KEY `idx_type_create` (`type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送日志';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  10. 初始数据 - 示例车次
-- ============================================================
INSERT INTO `train` (`id`, `train_no`, `train_type`, `start_station`, `end_station`,
                     `start_time`, `end_time`, `run_days`, `status`) VALUES
(1, 'G1234', 'G', '北京南',  '上海虹桥', '2026-06-10 08:00:00', '2026-06-10 12:30:00', '1111111', 1),
(2, 'G5678', 'G', '上海虹桥', '北京南',  '2026-06-10 09:00:00', '2026-06-10 13:30:00', '1111111', 1),
(3, 'D2345', 'D', '北京南',  '杭州东',  '2026-06-10 19:00:00', '2026-06-11 07:00:00', '1111111', 1);

-- ============================================================
--  11. 初始数据 - 示例库存（G1234 近 3 天，3 种座位）
--      实际生产中由 service-train-stock 的 添加车次 接口写入
-- ============================================================
INSERT INTO `train_seat_stock` (`id`, `train_no`, `run_date`, `seat_type`, `total`, `remain`) VALUES
(1001, 'G1234', '2026-06-10', 'BUSINESS',  20,  20),
(1002, 'G1234', '2026-06-10', 'FIRST',     50,  50),
(1003, 'G1234', '2026-06-10', 'SECOND',   120, 120),
(1004, 'G1234', '2026-06-11', 'BUSINESS',  20,  20),
(1005, 'G1234', '2026-06-11', 'FIRST',     50,  50),
(1006, 'G1234', '2026-06-11', 'SECOND',   120, 120),
(1007, 'G1234', '2026-06-12', 'BUSINESS',  20,  20),
(1008, 'G1234', '2026-06-12', 'FIRST',     50,  50),
(1009, 'G1234', '2026-06-12', 'SECOND',   120, 120),
(1010, 'G5678', '2026-06-10', 'BUSINESS',  20,  20),
(1011, 'G5678', '2026-06-10', 'FIRST',     50,  50),
(1012, 'G5678', '2026-06-10', 'SECOND',   120, 120);

-- ============================================================
--  12. 初始数据 - 管理员用户（可选）
--      密码：admin123  （BCrypt 强度 10 加密）
--      首次启动后建议改密码或删除
--      生成新 hash 的方法（service-user 启动后调用注册接口或用以下 Java）：
--          new BCryptPasswordEncoder().encode("admin123")
--      也可以直接走注册接口：
--          POST /auth/register  {"username":"admin","password":"admin123"}
--          然后 SQL 手动：UPDATE user SET user_type=1 WHERE username='admin';
-- ============================================================
-- INSERT INTO `user` (`id`, `username`, `password`, `user_type`, `status`) VALUES
-- (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1, 1);
-- 上面的 hash 是 "password" 的标准测试值，**不是** admin123 的 hash；
-- 如需预置管理员，请先在 Java 中生成正确 hash 再取消注释执行。

-- ============================================================
--  13. 验证
-- ============================================================
SELECT '=== 表列表 ===' AS info;
SHOW TABLES;

SELECT '=== 车次 ===' AS info;
SELECT id, train_no, train_type, start_station, end_station, start_time FROM train;

SELECT '=== 库存 (G1234) ===' AS info;
SELECT train_no, run_date, seat_type, total, remain, version
FROM train_seat_stock WHERE train_no='G1234' ORDER BY run_date, seat_type;
