CREATE OR REPLACE FUNCTION create_reservation(
  p_client_id BIGINT,
  p_car_id BIGINT,
  p_date_from DATE,
  p_date_to   DATE
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
  new_id BIGINT;
  v_daily_cost NUMERIC(10,2);
  v_days INTEGER;
BEGIN
  IF p_date_to <= p_date_from THEN
    RAISE EXCEPTION 'date_to must be greater than date_from';
  END IF;

  SELECT daily_cost
  INTO v_daily_cost
  FROM cars
  WHERE car_id = p_car_id;

  IF v_daily_cost IS NULL THEN
    RAISE EXCEPTION 'car not found';
  END IF;

  v_days := (p_date_to - p_date_from);
  IF v_days < 1 THEN
    v_days := 1;
  END IF;

  INSERT INTO reservations (
    client_id, car_id, date_from, date_to, status,
    daily_rate_at_booking, total_amount, penalty_amount, deposit_amount
  )
  VALUES (
    p_client_id, p_car_id, p_date_from, p_date_to, 'pending',
    v_daily_cost, (v_days * v_daily_cost), 0, 0
  )
  RETURNING rental_id INTO new_id;

  RETURN new_id;

EXCEPTION
  WHEN exclusion_violation THEN
    RAISE EXCEPTION 'car is not available for selected dates';
END;
$$;
