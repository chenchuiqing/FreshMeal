-- One coupon template can be claimed at most once per user:
--   1. Drop duplicate claims, keeping the earliest row of each
--      (user_id, coupon_id) group. The extra rows carry no used order.
--   2. Recompute t_coupon.received_count from the surviving rows so the
--      template counters match the stored instances again.
--   3. Add a unique key so concurrent receives cannot insert twice.

DELETE newer
FROM t_user_coupon newer
JOIN t_user_coupon older
  ON older.user_id = newer.user_id
 AND older.coupon_id = newer.coupon_id
 AND older.id < newer.id;

UPDATE t_coupon c
SET c.received_count = (
    SELECT COUNT(*) FROM t_user_coupon uc WHERE uc.coupon_id = c.id
);

ALTER TABLE `t_user_coupon`
    ADD UNIQUE KEY `uk_user_coupon_template` (`user_id`, `coupon_id`);
