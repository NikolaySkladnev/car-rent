CREATE OR REPLACE VIEW vw_available_cars_today AS
SELECT car.*
FROM cars car
WHERE NOT EXISTS (
  SELECT 1
  FROM reservations r
  WHERE r.car_id = car.car_id
    AND r.status <> 'canceled'
    AND daterange(r.date_from, r.date_to, '[)') @> CURRENT_DATE
);

CREATE OR REPLACE VIEW vw_client_reservations AS
SELECT
  r.rental_id,
  r.client_id,
  c.full_name,
  r.car_id,
  car.plate_number,
  car.brand,
  car.model,
  r.date_from,
  r.date_to,
  r.status,
  r.daily_rate_at_booking,
  r.total_amount,
  r.penalty_amount,
  r.deposit_amount
FROM reservations r
JOIN clients c ON c.client_id = r.client_id
JOIN cars car ON car.car_id = r.car_id;
