-- V7 hardcodes delivery_man_id = 3 on the demo delivery orders, but the seeded
-- rider account 'rider01' gets its id from auto-increment (id = 1 in a fresh
-- database). The rider endpoints query by the logged-in rider id, so the demo
-- tasks never show up in the rider app. Re-point them to the real seeded rider
-- using the stable username, skipping rows whose current owner still exists.
-- The statement is idempotent so Flyway reruns stay no-ops.
UPDATE `t_delivery_order`
SET `delivery_man_id` = (
    SELECT `id` FROM `t_delivery_man` WHERE `username` = 'rider01' LIMIT 1)
WHERE `delivery_no` IN ('DLVDEMO0003', 'DLVDEMO0004')
  AND NOT EXISTS (
    SELECT 1 FROM `t_delivery_man`
    WHERE `id` = `t_delivery_order`.`delivery_man_id`);
