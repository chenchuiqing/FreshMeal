CREATE TABLE `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费',
    `order_count` INT NOT NULL DEFAULT 0 COMMENT '累计订单数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE `t_admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `real_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '真实姓名',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `role` VARCHAR(20) NOT NULL DEFAULT 'operator' COMMENT '角色 super_admin/operator',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='管理员表';

CREATE TABLE `t_delivery_man` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配送员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `work_status` TINYINT NOT NULL DEFAULT 0 COMMENT '工作状态 0空闲 1配送中 2休息',
    `online_status` TINYINT NOT NULL DEFAULT 0 COMMENT '在线状态 0离线 1在线',
    `service_area_id` BIGINT DEFAULT NULL COMMENT '所属配送区域ID',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '当前位置经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '当前位置纬度',
    `max_order_num` INT NOT NULL DEFAULT 5 COMMENT '最大同时接单数',
    `today_order_count` INT NOT NULL DEFAULT 0 COMMENT '今日完成单量',
    `total_order_count` INT NOT NULL DEFAULT 0 COMMENT '累计完成单量',
    `total_income` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计配送收入',
    `rating` DECIMAL(3,2) NOT NULL DEFAULT 5.00 COMMENT '评分',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 0禁用 1正常',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_work_status` (`work_status`, `online_status`)
) ENGINE=InnoDB COMMENT='配送员表';

CREATE TABLE `t_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户',
    `receiver` VARCHAR(50) NOT NULL COMMENT '收货人',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `province` VARCHAR(30) NOT NULL COMMENT '省',
    `city` VARCHAR(30) NOT NULL COMMENT '市',
    `district` VARCHAR(30) NOT NULL COMMENT '区县',
    `detail` VARCHAR(200) NOT NULL COMMENT '详细地址',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `tag` VARCHAR(20) DEFAULT NULL COMMENT '地址标签',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='收货地址表';

CREATE TABLE `t_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID 0为一级分类',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='商品分类表';

CREATE TABLE `t_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `subtitle` VARCHAR(200) DEFAULT NULL COMMENT '副标题',
    `cover` VARCHAR(255) DEFAULT NULL COMMENT '主图',
    `detail` TEXT DEFAULT NULL COMMENT '图文详情',
    `product_type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型 1单品 2套餐',
    `price` DECIMAL(10,2) NOT NULL COMMENT '基础售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '划线原价',
    `unit` VARCHAR(20) NOT NULL DEFAULT '份' COMMENT '单位',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `sales_count` INT NOT NULL DEFAULT 0 COMMENT '销量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0下架 1上架',
    `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `calories` INT NOT NULL DEFAULT 0 COMMENT '热量kcal',
    `protein` DECIMAL(6,1) NOT NULL DEFAULT 0.0 COMMENT '蛋白质g',
    `fat` DECIMAL(6,1) NOT NULL DEFAULT 0.0 COMMENT '脂肪g',
    `carbohydrate` DECIMAL(6,1) NOT NULL DEFAULT 0.0 COMMENT '碳水化合物g',
    `sodium` DECIMAL(6,1) NOT NULL DEFAULT 0.0 COMMENT '钠含量mg',
    `nutrition_desc` VARCHAR(255) DEFAULT NULL COMMENT '营养说明文案',
    `allergens` VARCHAR(255) DEFAULT NULL COMMENT '过敏原',
    `storage_type` TINYINT NOT NULL DEFAULT 1 COMMENT '储存方式 1常温 2冷藏 3冷冻',
    `shelf_life_days` INT NOT NULL DEFAULT 0 COMMENT '保质期天数',
    `producer` VARCHAR(100) DEFAULT NULL COMMENT '生产商',
    `heating_method` VARCHAR(100) DEFAULT NULL COMMENT '食用方式',
    `health_tags` VARCHAR(100) DEFAULT NULL COMMENT '健康标签',
    `default_spec_id` BIGINT DEFAULT NULL COMMENT '默认规格ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`, `status`),
    KEY `idx_sales` (`sales_count`),
    KEY `idx_price` (`price`),
    FULLTEXT KEY `ft_name` (`name`) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='商品表';

CREATE TABLE `t_product_spec` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规格ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `name` VARCHAR(50) NOT NULL COMMENT '规格名称',
    `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '加价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '规格库存',
    `calories` INT NOT NULL DEFAULT 0 COMMENT '规格热量kcal',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认规格',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品规格表';

CREATE TABLE `t_product_option` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选项组ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `name` VARCHAR(50) NOT NULL COMMENT '选项组名称',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型 1单选 2多选',
    `required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必选',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品选项组表';

CREATE TABLE `t_product_option_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选项明细ID',
    `option_id` BIGINT NOT NULL COMMENT '选项组ID',
    `name` VARCHAR(50) NOT NULL COMMENT '选项名称',
    `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '加价',
    `calories` INT NOT NULL DEFAULT 0 COMMENT '附加热量kcal',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_option` (`option_id`)
) ENGINE=InnoDB COMMENT='商品选项明细表';

CREATE TABLE `t_product_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品图片ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0隐藏 1显示',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`, `sort`)
) ENGINE=InnoDB COMMENT='商品图集表';

CREATE TABLE `t_product_batch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `batch_no` VARCHAR(50) NOT NULL COMMENT '批次号',
    `production_date` DATE NOT NULL COMMENT '生产日期',
    `expire_date` DATE NOT NULL COMMENT '到期日期',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '批次可用库存',
    `lock_stock` INT NOT NULL DEFAULT 0 COMMENT '已锁定库存',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 2临期 3过期 4售罄',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_batch` (`product_id`, `batch_no`),
    KEY `idx_expire` (`expire_date`, `status`),
    KEY `idx_product_expire` (`product_id`, `expire_date`)
) ENGINE=InnoDB COMMENT='商品批次表';

CREATE TABLE `t_inventory_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存流水ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `batch_id` BIGINT DEFAULT NULL COMMENT '批次ID',
    `change_type` TINYINT NOT NULL COMMENT '变动类型 1入库 2出库 3退货 4报损 5盘点 6取消回补',
    `change_num` INT NOT NULL COMMENT '变动数量',
    `before_stock` INT NOT NULL DEFAULT 0 COMMENT '变动前数量',
    `after_stock` INT NOT NULL DEFAULT 0 COMMENT '变动后数量',
    `ref_type` VARCHAR(20) DEFAULT NULL COMMENT '关联单据类型',
    `ref_no` VARCHAR(32) DEFAULT NULL COMMENT '关联单号',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_batch` (`batch_id`),
    KEY `idx_ref` (`ref_no`)
) ENGINE=InnoDB COMMENT='库存变动流水表';

CREATE TABLE `t_cart` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `spec_id` BIGINT DEFAULT NULL COMMENT '规格ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `options` VARCHAR(500) DEFAULT NULL COMMENT '已选选项JSON快照',
    `checked` TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选',
    `product_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '商品名称快照',
    `cover` VARCHAR(255) DEFAULT NULL COMMENT '图片快照',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价快照',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `checked`)
) ENGINE=InnoDB COMMENT='购物车表';

CREATE TABLE `t_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
    `goods_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '商品总额',
    `freight_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
    `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `delivery_man_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '配送员配送提成',
    `receiver` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '收货人快照',
    `receiver_phone` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '收货电话快照',
    `receiver_address` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '收货地址快照',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '收货经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '收货纬度',
    `delivery_type` TINYINT NOT NULL DEFAULT 1 COMMENT '配送方式 1外卖配送 2到店自提',
    `expect_time` DATETIME DEFAULT NULL COMMENT '期望送达时间',
    `delivery_slot_id` BIGINT DEFAULT NULL COMMENT '预约配送时段ID',
    `delivery_slot_date` DATE DEFAULT NULL COMMENT '预约配送日期',
    `delivery_slot_name` VARCHAR(30) DEFAULT NULL COMMENT '配送时段名称快照',
    `delivery_man_id` BIGINT DEFAULT NULL COMMENT '配送员ID',
    `delivery_area_id` BIGINT DEFAULT NULL COMMENT '配送区域ID',
    `delivery_distance` DECIMAL(8,2) DEFAULT NULL COMMENT '配送距离km',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2已退款',
    `pay_type` TINYINT NOT NULL DEFAULT 0 COMMENT '支付方式 0未支付 1微信 2支付宝 3余额',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '用户备注',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `user_coupon_id` BIGINT DEFAULT NULL COMMENT '用户优惠券ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`, `status`),
    KEY `idx_status` (`status`, `create_time`),
    KEY `idx_slot` (`status`, `delivery_slot_date`),
    KEY `idx_delivery_man` (`delivery_man_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='订单主表';

CREATE TABLE `t_order_status_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '订单号快照',
    `from_status` TINYINT NOT NULL DEFAULT -1 COMMENT '原状态 -1表示创建',
    `to_status` TINYINT NOT NULL COMMENT '目标状态',
    `operator_type` TINYINT NOT NULL DEFAULT 0 COMMENT '操作方 0系统 1用户 2商家 3配送员',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '操作人姓名快照',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB COMMENT='订单状态流转日志表';

CREATE TABLE `t_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '订单号快照',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称快照',
    `product_cover` VARCHAR(255) DEFAULT NULL COMMENT '商品图片快照',
    `spec_id` BIGINT DEFAULT NULL COMMENT '规格ID',
    `spec_name` VARCHAR(50) DEFAULT NULL COMMENT '规格名称快照',
    `options` VARCHAR(500) DEFAULT NULL COMMENT '选项JSON快照',
    `options_desc` VARCHAR(255) DEFAULT NULL COMMENT '选项可读文案',
    `price` DECIMAL(10,2) NOT NULL COMMENT '成交单价快照',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `subtotal` DECIMAL(12,2) NOT NULL COMMENT '小计',
    `calories` INT NOT NULL DEFAULT 0 COMMENT '单份热量快照kcal',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='订单明细表';

CREATE TABLE `t_payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
    `pay_no` VARCHAR(32) NOT NULL COMMENT '支付流水号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号快照',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `pay_type` TINYINT NOT NULL COMMENT '支付方式 1微信 2支付宝 3余额',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待支付 1成功 2失败 3已关闭',
    `third_party_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
    `notify_content` TEXT DEFAULT NULL COMMENT '回调报文',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
    `expire_time` DATETIME DEFAULT NULL COMMENT '支付超时时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB COMMENT='支付流水表';

CREATE TABLE `t_delivery_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配送单ID',
    `delivery_no` VARCHAR(32) NOT NULL COMMENT '配送单号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号快照',
    `delivery_man_id` BIGINT DEFAULT NULL COMMENT '配送员ID',
    `delivery_man_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '配送员姓名快照',
    `delivery_man_phone` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '配送员电话快照',
    `area_id` BIGINT DEFAULT NULL COMMENT '配送区域ID',
    `distance` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '配送距离km',
    `freight` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
    `man_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送员提成',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '配送状态',
    `assign_type` TINYINT NOT NULL DEFAULT 0 COMMENT '派单方式 0待分配 1系统派单 2配送员抢单 3人工指派',
    `assign_time` DATETIME DEFAULT NULL COMMENT '分配时间',
    `pickup_time` DATETIME DEFAULT NULL COMMENT '取货时间',
    `pickup_address` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '取货地址',
    `receive_time` DATETIME DEFAULT NULL COMMENT '送达时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_delivery_no` (`delivery_no`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_man` (`delivery_man_id`, `status`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='配送单表';

CREATE TABLE `t_delivery_trace` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
    `delivery_order_id` BIGINT NOT NULL COMMENT '配送单ID',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '位置描述',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '配送状态快照',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_delivery` (`delivery_order_id`)
) ENGINE=InnoDB COMMENT='配送轨迹表';

CREATE TABLE `t_delivery_area` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配送区域ID',
    `name` VARCHAR(50) NOT NULL COMMENT '区域名称',
    `city` VARCHAR(30) NOT NULL COMMENT '所属城市',
    `district` VARCHAR(30) DEFAULT NULL COMMENT '覆盖区县',
    `base_fee` DECIMAL(10,2) NOT NULL DEFAULT 5.00 COMMENT '起步配送费',
    `per_km_fee` DECIMAL(10,2) NOT NULL DEFAULT 2.00 COMMENT '每公里加价',
    `free_threshold` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '免配送费门槛',
    `night_surcharge` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '夜间附加费',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='配送区域与运费规则表';

CREATE TABLE `t_delivery_slot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配送时段ID',
    `name` VARCHAR(30) NOT NULL COMMENT '时段名称',
    `start_time` TIME NOT NULL COMMENT '时段开始时间',
    `end_time` TIME NOT NULL COMMENT '时段结束时间',
    `order_deadline` TIME NOT NULL COMMENT '当日下单截止时间',
    `daily_capacity` INT NOT NULL DEFAULT 0 COMMENT '每日单量上限',
    `advance_days` INT NOT NULL DEFAULT 1 COMMENT '可提前预约天数',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='配送时段配置表';

CREATE TABLE `t_delivery_man_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '派单日志ID',
    `delivery_order_id` BIGINT NOT NULL COMMENT '配送单ID',
    `candidate_id` BIGINT NOT NULL COMMENT '候选配送员ID',
    `candidate_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '候选配送员姓名',
    `total_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '综合得分',
    `distance_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '距离得分',
    `load_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '负载得分',
    `rating_score` DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '评分得分',
    `distance_value` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '实际距离km',
    `load_value` INT NOT NULL DEFAULT 0 COMMENT '当前在途单量',
    `is_final` TINYINT NOT NULL DEFAULT 0 COMMENT '是否最终选中',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_delivery` (`delivery_order_id`)
) ENGINE=InnoDB COMMENT='派单策略日志表';
