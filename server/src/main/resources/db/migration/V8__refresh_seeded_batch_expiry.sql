-- Seeded batch expiry dates are install-relative (V4 uses CURRENT_DATE + n) and
-- freeze at migration time, so they decay into the past as wall time advances:
-- FEFO (expire_date >= CURRENT_DATE) then skips every expired seed batch and the
-- seeded stock becomes unsellable. Shift all batches by one common offset so the
-- earliest batch sits 45 days ahead of the migration day; relative gaps, FEFO
-- order and per-product spreads are preserved. Then recompute the derived status
-- column so no batch keeps a stale near-expiry/expired flag.
SET @shift_days = IFNULL(
    (SELECT GREATEST(DATEDIFF(DATE_ADD(CURDATE(), INTERVAL 45 DAY), MIN(expire_date)), 0)
     FROM t_product_batch), 0);

UPDATE t_product_batch
SET expire_date = DATE_ADD(expire_date, INTERVAL @shift_days DAY);

UPDATE t_product_batch
SET status = CASE
        WHEN stock <= 0 THEN 4
        WHEN expire_date < CURRENT_DATE THEN 3
        WHEN expire_date <= CURRENT_DATE + INTERVAL 3 DAY THEN 2
        ELSE 1
    END;
