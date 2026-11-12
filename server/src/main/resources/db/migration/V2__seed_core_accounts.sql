INSERT INTO `t_user` (
    `username`, `password`, `nickname`, `phone`, `gender`, `status`
) VALUES (
    'user01',
    '$2a$10$7eWq5WTPqI8aPGhOwWB9oOtOiwC6eZm6T3yO1oOaJJOe7dcazetEG',
    'FreshMeal User',
    '13800000001',
    0,
    1
);

INSERT INTO `t_admin` (
    `username`, `password`, `real_name`, `phone`, `role`, `status`
) VALUES (
    'admin',
    '$2a$10$tfI2vN6nqtRcZZW7NTN3A.N1crHv/MHLXJO.7UYLGSSYvLVhnVnn.',
    'Administrator',
    '13800000002',
    'super_admin',
    1
);

INSERT INTO `t_delivery_man` (
    `username`, `password`, `real_name`, `phone`, `work_status`,
    `online_status`, `max_order_num`, `status`
) VALUES (
    'rider01',
    '$2a$10$jFLkfTZ7C5PHdATdDig1eexGwLZjwf.pGGCqu1VLJV3yyP96vcQAW',
    'Rider 01',
    '13800000003',
    0,
    0,
    5,
    1
);
