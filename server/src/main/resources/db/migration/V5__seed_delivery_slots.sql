INSERT INTO `t_delivery_slot`
(`name`, `start_time`, `end_time`, `order_deadline`, `daily_capacity`,
 `advance_days`, `sort`, `status`)
VALUES
('午市 11:00-13:00', '11:00:00', '13:00:00', '10:30:00', 50, 1, 10, 1),
('下午茶 14:00-16:00', '14:00:00', '16:00:00', '13:30:00', 30, 1, 20, 1),
('晚市 17:00-20:00', '17:00:00', '20:00:00', '16:30:00', 80, 1, 30, 1);

UPDATE `t_delivery_man`
SET `service_area_id` = 1,
    `longitude` = 121.483701,
    `latitude` = 31.235416,
    `rating` = 4.80
WHERE `username` = 'rider01';
