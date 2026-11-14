-- 配送区域与运费规则
INSERT INTO `t_delivery_area`
(`id`, `name`, `city`, `district`, `base_fee`, `per_km_fee`,
 `free_threshold`, `night_surcharge`, `sort`, `status`)
VALUES
(1, '上海全市配送', '上海市', NULL, 5.00, 2.00, 80.00, 6.00, 10, 1),
(2, '浦东核心区', '上海市', '浦东新区', 6.00, 2.50, 100.00, 8.00, 1, 1);

-- 商品批次：每个可售商品提供两个 FEFO 批次，批次总库存与商品总库存一致
INSERT INTO `t_product_batch`
(`id`, `product_id`, `batch_no`, `production_date`, `expire_date`,
 `stock`, `lock_stock`, `status`, `remark`)
VALUES
(5001, 101, 'B101-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 80, 0, 2, 'FEFO first'),
(5002, 101, 'B101-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 3 DAY, 70, 0, 2, 'FEFO second'),
(5003, 102, 'B102-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 70, 0, 2, 'FEFO first'),
(5004, 102, 'B102-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 2 DAY, 50, 0, 2, 'FEFO second'),
(5005, 103, 'B103-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 60, 0, 2, 'FEFO first'),
(5006, 103, 'B103-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 3 DAY, 40, 0, 2, 'FEFO second'),
(5007, 104, 'B104-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 45, 0, 2, 'FEFO first'),
(5008, 104, 'B104-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 3 DAY, 35, 0, 2, 'FEFO second'),
(5009, 105, 'B105-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 90, 0, 2, 'FEFO first'),
(5010, 105, 'B105-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 3 DAY, 70, 0, 2, 'FEFO second'),
(5011, 106, 'B106-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 65, 0, 2, 'FEFO first'),
(5012, 106, 'B106-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 3 DAY, 45, 0, 2, 'FEFO second'),
(5013, 107, 'B107-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 55, 0, 2, 'FEFO first'),
(5014, 107, 'B107-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 3 DAY, 35, 0, 2, 'FEFO second'),
(5015, 108, 'B108-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 80, 0, 2, 'FEFO first'),
(5016, 108, 'B108-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 2 DAY, 60, 0, 2, 'FEFO second'),
(5017, 109, 'B109-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 1 DAY, 40, 0, 2, 'FEFO first'),
(5018, 109, 'B109-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 2 DAY, 30, 0, 2, 'FEFO second'),
(5019, 110, 'B110-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 89 DAY, 0, 0, 4, '售罄批次'),
(5020, 110, 'B110-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 90 DAY, 0, 0, 4, '售罄批次'),
(5021, 111, 'B111-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 4 DAY, 120, 0, 1, 'FEFO first'),
(5022, 111, 'B111-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 7 DAY, 80, 0, 1, 'FEFO second'),
(5023, 112, 'B112-A', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 30 DAY, 35, 0, 1, 'FEFO first'),
(5024, 112, 'B112-B', CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 90 DAY, 25, 0, 1, 'FEFO second');

-- 种子用户的默认收货地址，坐标用于运费试算
INSERT INTO `t_address`
(`user_id`, `receiver`, `phone`, `province`, `city`, `district`, `detail`,
 `longitude`, `latitude`, `tag`, `is_default`)
SELECT `id`, 'FreshMeal 用户', '13800000001', '上海市', '上海市', '浦东新区',
       '世纪大道100号12层', 121.493701, 31.240416, '公司', 1
FROM `t_user`
WHERE `username` = 'user01'
  AND NOT EXISTS (
      SELECT 1 FROM `t_address`
      WHERE `t_address`.`user_id` = `t_user`.`id`
        AND `deleted` = 0
  );
