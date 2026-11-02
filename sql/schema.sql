-- ============================================================
-- 校园校车智能调度系统 数据库初始化脚本
-- MySQL 8.x  |  数据库：campus_bus
-- 执行方式：mysql -uroot -p < sql/schema.sql
-- 演示账号（密码均为 123456）：
--   管理员 admin / 学生 student / 司机 driver01
-- ============================================================

DROP DATABASE IF EXISTS campus_bus;
CREATE DATABASE campus_bus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_bus;

-- 1. 用户表
CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    role        VARCHAR(20)  NOT NULL COMMENT 'STUDENT / ADMIN / DRIVER',
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    phone       VARCHAR(20)  DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_role (role)
) ENGINE=InnoDB COMMENT='系统用户';

-- 2. 线路表
CREATE TABLE bus_route (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(80) NOT NULL COMMENT '线路名称',
    description VARCHAR(255) DEFAULT NULL,
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='校车线路';

-- 3. 站点表
CREATE TABLE bus_stop (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_id    BIGINT      NOT NULL,
    name        VARCHAR(80) NOT NULL,
    seq         INT         NOT NULL COMMENT '站点顺序，从1开始',
    latitude    DECIMAL(10,6) DEFAULT NULL,
    longitude   DECIMAL(10,6) DEFAULT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_route_seq (route_id, seq),
    INDEX idx_stop_route (route_id)
) ENGINE=InnoDB COMMENT='线路站点';

-- 4. 车辆表
CREATE TABLE bus_vehicle (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_no    VARCHAR(30) NOT NULL UNIQUE COMMENT '车牌/编号',
    model       VARCHAR(50) NOT NULL COMMENT '车型',
    capacity    INT         NOT NULL COMMENT '核定座位数',
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE / RUNNING / DISABLED',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_status (status)
) ENGINE=InnoDB COMMENT='校车车辆';

-- 5. 班次表
CREATE TABLE bus_shift (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_id    BIGINT      NOT NULL,
    vehicle_id  BIGINT      DEFAULT NULL,
    driver_id   BIGINT      DEFAULT NULL,
    shift_date  DATE        NOT NULL,
    depart_time TIME        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED / RUNNING / FINISHED / CANCELLED',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shift_date (shift_date),
    INDEX idx_shift_route (route_id),
    INDEX idx_shift_vehicle (vehicle_id),
    INDEX idx_shift_driver (driver_id)
) ENGINE=InnoDB COMMENT='校车班次';

-- 6. 预约表
CREATE TABLE bus_reservation (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    shift_id    BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED / CANCELLED',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancel_time DATETIME    DEFAULT NULL,
    INDEX idx_res_user (user_id),
    INDEX idx_res_shift (shift_id, status)
) ENGINE=InnoDB COMMENT='学生预约';

-- 7. 车辆位置表（模拟位置）
CREATE TABLE bus_vehicle_location (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id      BIGINT      NOT NULL,
    shift_id        BIGINT      NOT NULL,
    current_stop_id BIGINT      DEFAULT NULL,
    next_stop_id    BIGINT      DEFAULT NULL,
    progress        DOUBLE      NOT NULL DEFAULT 0 COMMENT '线路进度，0 到 站点数-1',
    run_status      VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING / DELAYED / ARRIVED',
    latitude        DECIMAL(10,6) DEFAULT NULL,
    longitude       DECIMAL(10,6) DEFAULT NULL,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_loc_shift (shift_id),
    INDEX idx_loc_vehicle (vehicle_id)
) ENGINE=InnoDB COMMENT='车辆模拟位置';

-- 8. 调度建议表
CREATE TABLE dispatch_suggestion (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id             BIGINT       NOT NULL,
    vehicle_id           BIGINT       DEFAULT NULL COMMENT '建议生成时的车辆',
    suggested_vehicle_id BIGINT       DEFAULT NULL COMMENT '推荐替换车辆',
    type                 VARCHAR(30)  NOT NULL COMMENT 'ADD_VEHICLE / LARGER_VEHICLE / REDUCE_CAPACITY / MERGE_SHIFT / REALLOCATE / REASSIGN_VEHICLE',
    reason               VARCHAR(500) NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / CONFIRMED / IGNORED',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handle_time          DATETIME     DEFAULT NULL,
    INDEX idx_sug_shift (shift_id, status),
    INDEX idx_sug_status (status)
) ENGINE=InnoDB COMMENT='调度建议';

-- 9. 司机任务表
CREATE TABLE driver_task (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    driver_id   BIGINT      NOT NULL,
    shift_id    BIGINT      NOT NULL,
    vehicle_id  BIGINT      DEFAULT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / RUNNING / FINISHED',
    start_time  DATETIME    DEFAULT NULL,
    end_time    DATETIME    DEFAULT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_shift (shift_id),
    INDEX idx_task_driver (driver_id, status)
) ENGINE=InnoDB COMMENT='司机任务';

-- ============================================================
-- 初始化演示数据
-- 密码均为 123456（BCrypt）
-- ============================================================
SET @pwd = '$2b$10$lqS/Xhl.UHb08xjTkbcNo.yIkIZejvVVQuedjEcn3pZ/AceuJ0J9.';

INSERT INTO sys_user (id, username, password, role, name, phone, status) VALUES
(1,  'admin',      @pwd, 'ADMIN',   '系统管理员', '13800000000', 1),
(2,  'student',    @pwd, 'STUDENT', '张小明',     '13800000001', 1),
(3,  'student02',  @pwd, 'STUDENT', '李小红',     '13800000002', 1),
(4,  'student03',  @pwd, 'STUDENT', '王小刚',     '13800000003', 1),
(5,  'student04',  @pwd, 'STUDENT', '赵小雨',     '13800000004', 1),
(6,  'student05',  @pwd, 'STUDENT', '陈小飞',     '13800000005', 1),
(7,  'student06',  @pwd, 'STUDENT', '刘小婷',     '13800000006', 1),
(8,  'student07',  @pwd, 'STUDENT', '杨小宇',     '13800000007', 1),
(9,  'student08',  @pwd, 'STUDENT', '周小欣',     '13800000008', 1),
(10, 'student09',  @pwd, 'STUDENT', '吴小龙',     '13800000009', 1),
(11, 'student10',  @pwd, 'STUDENT', '徐小雨',     '13800000010', 1),
(12, 'student11',  @pwd, 'STUDENT', '孙小宁',     '13800000011', 1),
(13, 'student12',  @pwd, 'STUDENT', '马小杰',     '13800000012', 1),
(14, 'student13',  @pwd, 'STUDENT', '朱小慧',     '13800000013', 1),
(15, 'student14',  @pwd, 'STUDENT', '胡小峰',     '13800000014', 1),
(16, 'student15',  @pwd, 'STUDENT', '郭小雪',     '13800000015', 1),
(17, 'driver01',   @pwd, 'DRIVER',  '王师傅',     '13900000001', 1),
(18, 'driver02',   @pwd, 'DRIVER',  '李师傅',     '13900000002', 1);

INSERT INTO bus_route (id, name, description, status) VALUES
(1, '东校区—西校区通勤线', '连接东西校区的日常通勤线路', 1),
(2, '南门—图书馆环线',     '环绕校园中心区域的短途线路', 1),
(3, '北区—体育馆线',       '服务北区宿舍与体育馆的线路', 1);

INSERT INTO bus_stop (route_id, name, seq, latitude, longitude) VALUES
(1, '东校区正门',       1, 30.512000, 114.421000),
(1, '图书馆',           2, 30.510500, 114.417500),
(1, '大学生活动中心',   3, 30.508200, 114.414000),
(1, '西校区南门',       4, 30.506000, 114.409500),
(1, '西校区教学楼',     5, 30.504500, 114.406000),
(2, '南门',             1, 30.514000, 114.418000),
(2, '行政楼',           2, 30.512500, 114.415000),
(2, '中心广场',         3, 30.511000, 114.412000),
(2, '图书馆',           4, 30.510500, 114.417500),
(2, '北门',             5, 30.516000, 114.420000),
(3, '北区宿舍',         1, 30.517000, 114.416000),
(3, '第三食堂',         2, 30.515500, 114.414500),
(3, '体育馆',           3, 30.513000, 114.412000),
(3, '实训中心',         4, 30.511000, 114.410000);

INSERT INTO bus_vehicle (id, plate_no, model, capacity, status) VALUES
(1, '校A·1001', '宇通大巴',  45, 'AVAILABLE'),
(2, '校A·1002', '金龙大巴',  30, 'AVAILABLE'),
(3, '校A·2001', '宇通中巴',  20, 'AVAILABLE'),
(4, '校A·2002', '金杯中巴',  12, 'AVAILABLE'),
(5, '校A·1003', '宇通大巴',  50, 'AVAILABLE'),
(6, '校A·2003', '金杯中巴',  20, 'DISABLED');

-- 今日班次
INSERT INTO bus_shift (id, route_id, vehicle_id, driver_id, shift_date, depart_time, status) VALUES
(1, 1, 1, 17, CURDATE(), '07:30:00', 'SCHEDULED'),
(2, 1, 4, 18, CURDATE(), '08:20:00', 'SCHEDULED'),
(3, 2, 3, 17, CURDATE(), '09:30:00', 'SCHEDULED'),
(4, 2, 4, 18, CURDATE(), '10:30:00', 'SCHEDULED'),
(5, 3, 6, 17, CURDATE(), '13:00:00', 'SCHEDULED'),
-- 明日班次
(6, 1, 2, 18, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '07:30:00', 'SCHEDULED'),
(7, 2, 5, 17, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', 'SCHEDULED'),
(8, 3, 3, 18, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00', 'SCHEDULED');

INSERT INTO driver_task (driver_id, shift_id, vehicle_id, status) VALUES
(17, 1, 1, 'PENDING'),
(18, 2, 4, 'PENDING'),
(17, 3, 3, 'PENDING'),
(18, 4, 4, 'PENDING'),
(17, 5, 6, 'PENDING'),
(18, 6, 2, 'PENDING'),
(17, 7, 5, 'PENDING'),
(18, 8, 3, 'PENDING');

-- 预约数据（用于演示预约率、调度建议）
-- 班次1：6/45 = 13%
INSERT INTO bus_reservation (user_id, shift_id, status) VALUES
(2, 1, 'RESERVED'), (3, 1, 'RESERVED'), (4, 1, 'RESERVED'),
(5, 1, 'RESERVED'), (6, 1, 'RESERVED'), (7, 1, 'RESERVED'),
-- 班次2：11/12 = 92%
(2, 2, 'RESERVED'), (3, 2, 'RESERVED'), (4, 2, 'RESERVED'),
(5, 2, 'RESERVED'), (6, 2, 'RESERVED'), (7, 2, 'RESERVED'),
(8, 2, 'RESERVED'), (9, 2, 'RESERVED'), (10, 2, 'RESERVED'),
(11, 2, 'RESERVED'), (12, 2, 'RESERVED'),
-- 班次3：2/20 = 10%
(2, 3, 'RESERVED'), (3, 3, 'RESERVED'),
-- 班次4：1/12 = 8%
(3, 4, 'RESERVED'),
-- 班次5：3/20，车辆已停用
(2, 5, 'RESERVED'), (3, 5, 'RESERVED'), (4, 5, 'RESERVED'),
-- 明日班次6：3/30
(5, 6, 'RESERVED'), (6, 6, 'RESERVED'), (7, 6, 'RESERVED'),
-- 明日班次7：5/50
(8, 7, 'RESERVED'), (9, 7, 'RESERVED'), (10, 7, 'RESERVED'),
(11, 7, 'RESERVED'), (12, 7, 'RESERVED');
