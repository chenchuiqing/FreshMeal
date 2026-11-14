CREATE TABLE `t_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `type` TINYINT NOT NULL DEFAULT 1,
    `threshold` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `total_count` INT NOT NULL DEFAULT 0,
    `received_count` INT NOT NULL DEFAULT 0,
    `used_count` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_status` (`status`, `end_time`)
) ENGINE=InnoDB COMMENT='优惠券模板表';

CREATE TABLE `t_user_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `coupon_id` BIGINT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `order_id` BIGINT DEFAULT NULL,
    `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `use_time` DATETIME DEFAULT NULL,
    `expire_time` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_coupon` (`user_id`, `status`),
    KEY `idx_coupon_template` (`coupon_id`)
) ENGINE=InnoDB COMMENT='用户优惠券表';

CREATE TABLE `t_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `rating` TINYINT NOT NULL DEFAULT 5,
    `content` VARCHAR(500) DEFAULT NULL,
    `images` VARCHAR(1000) DEFAULT NULL,
    `reply` VARCHAR(500) DEFAULT NULL,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_order_product` (`order_id`, `product_id`),
    KEY `idx_review_product` (`product_id`),
    KEY `idx_review_user` (`user_id`)
) ENGINE=InnoDB COMMENT='订单评价表';

CREATE TABLE `t_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB COMMENT='商品收藏表';

CREATE TABLE `t_browsing_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `view_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_history_user_product` (`user_id`, `product_id`),
    KEY `idx_history_user_time` (`user_id`, `view_time`)
) ENGINE=InnoDB COMMENT='商品浏览历史表';

CREATE TABLE `t_banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `image` VARCHAR(255) NOT NULL,
    `link` VARCHAR(255) DEFAULT NULL,
    `sort` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_banner_status_sort` (`status`, `sort`)
) ENGINE=InnoDB COMMENT='首页轮播图表';

CREATE TABLE `t_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` TINYINT NOT NULL DEFAULT 1,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notice_status_time` (`status`, `create_time`)
) ENGINE=InnoDB COMMENT='系统公告表';

CREATE TABLE `t_refund` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `order_no` VARCHAR(32) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `previous_status` TINYINT NOT NULL DEFAULT 1,
    `admin_reply` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_order` (`order_id`),
    KEY `idx_refund_status` (`status`, `create_time`)
) ENGINE=InnoDB COMMENT='售后退款申请表';

INSERT INTO `t_coupon`
(`id`, `name`, `type`, `threshold`, `amount`, `start_time`, `end_time`,
 `total_count`, `received_count`, `used_count`, `status`)
VALUES
(1, '新客满减券', 1, 30.00, 5.00, CURRENT_TIMESTAMP - INTERVAL 1 DAY,
 CURRENT_TIMESTAMP + INTERVAL 30 DAY, 1000, 1, 0, 1),
(2, '周末八折券', 2, 50.00, 0.80, CURRENT_TIMESTAMP - INTERVAL 1 DAY,
 CURRENT_TIMESTAMP + INTERVAL 14 DAY, 500, 0, 0, 1);

INSERT INTO `t_user_coupon`
(`id`, `user_id`, `coupon_id`, `status`, `expire_time`)
VALUES
(1, 1, 1, 0, CURRENT_TIMESTAMP + INTERVAL 30 DAY);

INSERT INTO `t_banner`
(`id`, `title`, `image`, `link`, `sort`, `status`)
VALUES
(1, '新鲜备餐，准时送达', '/api/files/products/p101.svg', '/catalog', 10, 1),
(2, '高蛋白轻食周', '/api/files/products/p106.svg', '/catalog?healthTag=高蛋白', 20, 1);

INSERT INTO `t_notice`
(`id`, `title`, `content`, `type`, `status`)
VALUES
(1, '欢迎使用 FreshMeal 沙箱', '当前环境为演示沙箱，支付不会产生真实扣款。', 1, 1),
(2, '配送时段开放提醒', '每日 09:00 至 21:00 提供预约配送时段。', 2, 1);

INSERT INTO `t_order`
(`id`, `order_no`, `user_id`, `goods_amount`, `freight_amount`, `discount_amount`,
 `pay_amount`, `delivery_man_fee`, `receiver`, `receiver_phone`, `receiver_address`,
 `longitude`, `latitude`, `delivery_type`, `status`, `pay_status`, `pay_type`,
 `pay_time`, `remark`, `finish_time`)
VALUES
(9001, 'FMDEMO0001', 1, 28.00, 5.00, 0.00, 33.00, 0.00,
 'FreshMeal 用户', '13800000001', '上海市浦东新区世纪大道100号12层',
 121.493701, 31.240416, 1, 0, 0, 0, NULL, '演示待支付订单', NULL),
(9002, 'FMDEMO0002', 1, 34.00, 5.00, 5.00, 34.00, 0.00,
 'FreshMeal 用户', '13800000001', '上海市浦东新区世纪大道100号12层',
 121.493701, 31.240416, 1, 1, 1, 1, CURRENT_TIMESTAMP - INTERVAL 2 HOUR,
 '演示待接单订单', NULL),
(9003, 'FMDEMO0003', 1, 36.00, 5.00, 0.00, 41.00, 6.00,
 'FreshMeal 用户', '13800000001', '上海市浦东新区世纪大道100号12层',
 121.493701, 31.240416, 1, 5, 1, 1, CURRENT_TIMESTAMP - INTERVAL 1 DAY,
 '演示配送中订单', NULL),
(9004, 'FMDEMO0004', 1, 39.00, 5.00, 0.00, 44.00, 6.00,
 'FreshMeal 用户', '13800000001', '上海市浦东新区世纪大道100号12层',
 121.493701, 31.240416, 1, 6, 1, 2, CURRENT_TIMESTAMP - INTERVAL 2 DAY,
 '演示已完成订单', CURRENT_TIMESTAMP - INTERVAL 1 DAY),
(9005, 'FMDEMO0005', 1, 32.00, 5.00, 0.00, 37.00, 0.00,
 'FreshMeal 用户', '13800000001', '上海市浦东新区世纪大道100号12层',
 121.493701, 31.240416, 1, 7, 1, 1, CURRENT_TIMESTAMP - INTERVAL 3 DAY,
 '演示退款处理中订单', NULL);

INSERT INTO `t_order_item`
(`id`, `order_id`, `order_no`, `user_id`, `product_id`, `product_name`,
 `product_cover`, `spec_id`, `spec_name`, `price`, `quantity`, `subtotal`, `calories`)
VALUES
(9101, 9001, 'FMDEMO0001', 1, 101, '香煎鸡胸沙拉', '/api/files/products/p101.svg',
 1001, '标准份', 28.00, 1, 28.00, 352),
(9102, 9002, 'FMDEMO0002', 1, 106, '鸡胸藜麦碗', '/api/files/products/p106.svg',
 1011, '标准份', 34.00, 1, 34.00, 520),
(9103, 9003, 'FMDEMO0003', 1, 103, '照烧牛肉沙拉', '/api/files/products/p103.svg',
 1005, '标准份', 36.00, 1, 36.00, 486),
(9104, 9004, 'FMDEMO0004', 1, 104, '藜麦虾仁沙拉', '/api/files/products/p104.svg',
 1007, '标准份', 39.00, 1, 39.00, 388),
(9105, 9005, 'FMDEMO0005', 1, 102, '牛油果蛋沙拉', '/api/files/products/p102.svg',
 1003, '标准份', 32.00, 1, 32.00, 420);

INSERT INTO `t_payment`
(`id`, `pay_no`, `order_id`, `order_no`, `user_id`, `pay_type`, `amount`,
 `status`, `third_party_no`, `pay_time`, `expire_time`)
VALUES
(9201, 'PAYDEMO000001', 9002, 'FMDEMO0002', 1, 1, 34.00, 1,
 'MOCK-DEMO-0002', CURRENT_TIMESTAMP - INTERVAL 2 HOUR,
 CURRENT_TIMESTAMP + INTERVAL 13 HOUR),
(9202, 'PAYDEMO000002', 9003, 'FMDEMO0003', 1, 1, 41.00, 1,
 'MOCK-DEMO-0003', CURRENT_TIMESTAMP - INTERVAL 1 DAY,
 CURRENT_TIMESTAMP + INTERVAL 23 HOUR),
(9203, 'PAYDEMO000003', 9004, 'FMDEMO0004', 1, 2, 44.00, 1,
 'MOCK-DEMO-0004', CURRENT_TIMESTAMP - INTERVAL 2 DAY,
 CURRENT_TIMESTAMP + INTERVAL 46 HOUR),
(9204, 'PAYDEMO000004', 9005, 'FMDEMO0005', 1, 1, 37.00, 1,
 'MOCK-DEMO-0005', CURRENT_TIMESTAMP - INTERVAL 3 DAY,
 CURRENT_TIMESTAMP + INTERVAL 69 HOUR);

INSERT INTO `t_order_status_log`
(`order_id`, `order_no`, `from_status`, `to_status`, `operator_type`,
 `operator_id`, `operator_name`, `remark`)
VALUES
(9001, 'FMDEMO0001', -1, 0, 0, NULL, '系统', '演示订单创建'),
(9002, 'FMDEMO0002', -1, 0, 0, NULL, '系统', '演示订单创建'),
(9002, 'FMDEMO0002', 0, 1, 1, 1, 'FreshMeal 用户', '模拟支付成功'),
(9003, 'FMDEMO0003', -1, 0, 0, NULL, '系统', '演示订单创建'),
(9003, 'FMDEMO0003', 0, 1, 1, 1, 'FreshMeal 用户', '模拟支付成功'),
(9003, 'FMDEMO0003', 1, 2, 2, NULL, '商家', '商家接单'),
(9003, 'FMDEMO0003', 2, 4, 2, NULL, '商家', '出餐完成'),
(9003, 'FMDEMO0003', 4, 5, 3, 3, 'Rider 01', '配送员取货'),
(9004, 'FMDEMO0004', -1, 0, 0, NULL, '系统', '演示订单创建'),
(9004, 'FMDEMO0004', 0, 1, 1, 1, 'FreshMeal 用户', '模拟支付成功'),
(9004, 'FMDEMO0004', 1, 2, 2, NULL, '商家', '商家接单'),
(9004, 'FMDEMO0004', 2, 4, 2, NULL, '商家', '出餐完成'),
(9004, 'FMDEMO0004', 4, 5, 3, 3, 'Rider 01', '配送员取货'),
(9004, 'FMDEMO0004', 5, 6, 3, 3, 'Rider 01', '订单已送达'),
(9005, 'FMDEMO0005', -1, 0, 0, NULL, '系统', '演示订单创建'),
(9005, 'FMDEMO0005', 0, 1, 1, 1, 'FreshMeal 用户', '模拟支付成功'),
(9005, 'FMDEMO0005', 1, 7, 1, 1, 'FreshMeal 用户', '申请售后退款');

INSERT INTO `t_delivery_order`
(`id`, `delivery_no`, `order_id`, `order_no`, `delivery_man_id`,
 `delivery_man_name`, `delivery_man_phone`, `area_id`, `distance`, `freight`,
 `man_fee`, `status`, `assign_type`, `assign_time`, `pickup_time`,
 `pickup_address`, `receive_time`)
VALUES
(9301, 'DLVDEMO0003', 9003, 'FMDEMO0003', 3, 'Rider 01', '13800000003',
 1, 3.20, 11.40, 6.00, 3, 1, CURRENT_TIMESTAMP - INTERVAL 5 HOUR,
 CURRENT_TIMESTAMP - INTERVAL 1 HOUR, 'FreshMeal 中央厨房', NULL),
(9302, 'DLVDEMO0004', 9004, 'FMDEMO0004', 3, 'Rider 01', '13800000003',
 1, 3.20, 11.40, 6.00, 4, 1, CURRENT_TIMESTAMP - INTERVAL 2 DAY,
 CURRENT_TIMESTAMP - INTERVAL 2 DAY, 'FreshMeal 中央厨房',
 CURRENT_TIMESTAMP - INTERVAL 1 DAY);

INSERT INTO `t_refund`
(`id`, `order_id`, `order_no`, `user_id`, `amount`, `reason`, `status`,
 `previous_status`)
VALUES
(1, 9005, 'FMDEMO0005', 1, 37.00, '演示售后退款申请', 0, 1);

INSERT INTO `t_review`
(`id`, `user_id`, `order_id`, `product_id`, `rating`, `content`, `reply`)
VALUES
(1, 1, 9004, 104, 5, '虾仁很新鲜，配送也很准时。', '感谢您的认可，期待再次为您服务。');

INSERT INTO `t_favorite` (`user_id`, `product_id`)
VALUES (1, 101), (1, 106);

INSERT INTO `t_browsing_history` (`user_id`, `product_id`, `view_time`)
VALUES
(1, 101, CURRENT_TIMESTAMP - INTERVAL 10 MINUTE),
(1, 106, CURRENT_TIMESTAMP - INTERVAL 30 MINUTE),
(1, 104, CURRENT_TIMESTAMP - INTERVAL 2 HOUR);
