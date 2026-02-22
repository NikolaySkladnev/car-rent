CREATE TABLE cars (
  car_id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  plate_number VARCHAR(20) NOT NULL,
  brand VARCHAR(50) NOT NULL,
  model VARCHAR(50) NOT NULL,
  status VARCHAR(20),
  daily_cost NUMERIC(10,2) NOT NULL,
  insurance_cost NUMERIC(10,2),
  prod_year INTEGER
);

ALTER TABLE cars ADD CONSTRAINT cars_ck_1 CHECK (daily_cost >= 0);
ALTER TABLE cars ADD CONSTRAINT cars_ck_2 CHECK (insurance_cost >= 0);
ALTER TABLE cars ADD CONSTRAINT cars_pk PRIMARY KEY (car_id);
ALTER TABLE cars ADD CONSTRAINT cars__un UNIQUE (plate_number);

CREATE TABLE clients (
  client_id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  full_name VARCHAR(200) NOT NULL,
  passport_data VARCHAR(50) NOT NULL,
  login VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(30),
  birth_date DATE,
  address VARCHAR(300),
  driver_license_number VARCHAR(50),
  client_status VARCHAR(20)
);

ALTER TABLE clients ADD CONSTRAINT clients_pk PRIMARY KEY (client_id);
ALTER TABLE clients ADD CONSTRAINT clients__un UNIQUE (passport_data);
ALTER TABLE clients ADD CONSTRAINT clients__unv1 UNIQUE (login);
ALTER TABLE clients ADD CONSTRAINT clients__unv2 UNIQUE (email);
ALTER TABLE clients ADD CONSTRAINT clients__unv3 UNIQUE (driver_license_number);
ALTER TABLE clients ADD CONSTRAINT clients__unv4 UNIQUE (phone);

CREATE TABLE reservations (
  rental_id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  client_id BIGINT NOT NULL,
  car_id BIGINT NOT NULL,
  date_from DATE NOT NULL,
  date_to DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  total_amount NUMERIC(12,2),
  daily_rate_at_booking NUMERIC(10,2),
  penalty_amount NUMERIC(10,2) DEFAULT 0 NOT NULL,
  deposit_amount NUMERIC(10,2) DEFAULT 0 NOT NULL
);

CREATE INDEX reservations__idx   ON reservations (car_id ASC);
CREATE INDEX reservations__idxv1 ON reservations (client_id ASC);

ALTER TABLE reservations ADD CONSTRAINT reservations_ck_1 CHECK (date_to > date_from);
ALTER TABLE reservations ADD CONSTRAINT reservations_ck_2 CHECK (
  COALESCE(total_amount, 0) >= 0
  AND COALESCE(daily_rate_at_booking, 0) >= 0
  AND penalty_amount >= 0
  AND deposit_amount >= 0
);
ALTER TABLE reservations ADD CONSTRAINT reservations_status_ck
  CHECK (status IN ('pending','confirmed','canceled','completed'));

ALTER TABLE reservations ADD CONSTRAINT reservations_pk PRIMARY KEY (rental_id);

ALTER TABLE reservations
  ADD CONSTRAINT reservations_cars_fk
  FOREIGN KEY (car_id) REFERENCES cars (car_id);

ALTER TABLE reservations
  ADD CONSTRAINT reservations_clients_fk
  FOREIGN KEY (client_id) REFERENCES clients (client_id);

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
ADD CONSTRAINT reservations_no_overlap
EXCLUDE USING gist (
  car_id WITH =,
  daterange(date_from, date_to, '[)') WITH &&
)
WHERE (status <> 'canceled');
