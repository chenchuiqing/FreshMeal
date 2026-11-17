-- Rider income settlement historically wrote total_income back unchanged
-- (completeRiderStatistics never added the delivered task's man_fee), so every
-- completed delivery left the counter at zero. Recompute the lifetime total
-- from delivered tasks themselves. Idempotent: reruns converge to the same sum.
UPDATE `t_delivery_man` m
SET m.`total_income` = IFNULL((
    SELECT SUM(d.`man_fee`) FROM `t_delivery_order` d
    WHERE d.`delivery_man_id` = m.`id` AND d.`status` = 4), 0);
