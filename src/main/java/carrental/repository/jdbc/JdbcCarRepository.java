package carrental.repository.jdbc;

import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;
import carrental.domain.model.Car;
import carrental.repository.CarRepository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class JdbcCarRepository implements CarRepository {
    private final DataSource ds;

    public JdbcCarRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public List<Car> listAvailable(LocalDate dateFrom, LocalDate dateTo) {
        try {
            return JdbcUtils.queryList(ds, """
                    SELECT car_id, plate_number, brand, model, COALESCE(status,'available') AS status,
                           daily_cost, insurance_cost, prod_year
                    FROM cars c
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM reservations r
                        WHERE r.car_id = c.car_id
                          AND r.status <> 'canceled'
                          AND daterange(r.date_from, r.date_to, '[)') && daterange(?, ?, '[)')
                    )
                    ORDER BY car_id
                    """, ps -> {
                ps.setDate(1, Date.valueOf(dateFrom));
                ps.setDate(2, Date.valueOf(dateTo));
            }, rs -> new Car(
                    rs.getLong("car_id"),
                    rs.getString("plate_number"),
                    rs.getString("brand"),
                    rs.getString("model"),
                    rs.getString("status"),
                    rs.getBigDecimal("daily_cost"),
                    rs.getBigDecimal("insurance_cost"),
                    (Integer) rs.getObject("prod_year")
            ));
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public Car getById(long id) {
        try {
            Car c = JdbcUtils.queryOne(ds, """
                    SELECT car_id, plate_number, brand, model, COALESCE(status,'available') AS status,
                           daily_cost, insurance_cost, prod_year
                    FROM cars
                    WHERE car_id = ?
                    """, ps -> ps.setLong(1, id), rs -> new Car(
                    rs.getLong("car_id"),
                    rs.getString("plate_number"),
                    rs.getString("brand"),
                    rs.getString("model"),
                    rs.getString("status"),
                    rs.getBigDecimal("daily_cost"),
                    rs.getBigDecimal("insurance_cost"),
                    (Integer) rs.getObject("prod_year")
            ));

            if (c == null) throw new DomainException(ErrorCode.NOT_FOUND, "car not found");
            return c;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }
}
