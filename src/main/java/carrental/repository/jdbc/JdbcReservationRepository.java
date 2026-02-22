package carrental.repository.jdbc;

import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;
import carrental.domain.model.ReservationView;
import carrental.repository.ReservationRepository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class JdbcReservationRepository implements ReservationRepository {
    private final DataSource ds;

    public JdbcReservationRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public boolean isClientAllowed(long clientId) {
        try {
            Integer cnt = JdbcUtils.queryOne(ds, """
                    SELECT COUNT(*) AS cnt
                    FROM reservations
                    WHERE client_id = ?
                      AND status IN ('pending','confirmed')
                      AND date_to >= CURRENT_DATE
                    """, ps -> ps.setLong(1, clientId), rs -> rs.getInt("cnt"));
            return cnt != null && cnt == 0;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public long create(long clientId, long carId, LocalDate dateFrom, LocalDate dateTo) {
        try {
            Long id = JdbcUtils.queryOne(ds, "SELECT create_reservation(?,?,?,?)",
                    ps -> {
                        ps.setLong(1, clientId);
                        ps.setLong(2, carId);
                        ps.setDate(3, Date.valueOf(dateFrom));
                        ps.setDate(4, Date.valueOf(dateTo));
                    },
                    rs -> rs.getLong(1));

            if (id == null) throw new DomainException(ErrorCode.INTERNAL, "internal error");
            return id;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public boolean updateStatus(long rentalId, long clientId, String status) {
        try {
            int n = JdbcUtils.exec(ds, """
                    UPDATE reservations
                    SET status = ?
                    WHERE rental_id = ? AND client_id = ?
                    """, ps -> {
                ps.setString(1, status);
                ps.setLong(2, rentalId);
                ps.setLong(3, clientId);
            });
            return n == 1;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public List<ReservationView> listByClient(long clientId) {
        try {
            return JdbcUtils.queryList(ds, """
                    SELECT
                        rental_id,
                        client_id,
                        full_name,
                        car_id,
                        plate_number,
                        brand,
                        model,
                        date_from,
                        date_to,
                        status,
                        daily_rate_at_booking,
                        total_amount,
                        penalty_amount,
                        deposit_amount
                    FROM vw_client_reservations
                    WHERE client_id = ?
                    ORDER BY date_from DESC, rental_id DESC
                    """, ps -> ps.setLong(1, clientId), JdbcReservationRepository::mapReservationView);
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public ReservationView getByIdForClient(long rentalId, long clientId) {
        try {
            ReservationView v = JdbcUtils.queryOne(ds, """
                    SELECT
                        rental_id,
                        client_id,
                        full_name,
                        car_id,
                        plate_number,
                        brand,
                        model,
                        date_from,
                        date_to,
                        status,
                        daily_rate_at_booking,
                        total_amount,
                        penalty_amount,
                        deposit_amount
                    FROM vw_client_reservations
                    WHERE rental_id = ? AND client_id = ?
                    """, ps -> {
                ps.setLong(1, rentalId);
                ps.setLong(2, clientId);
            }, JdbcReservationRepository::mapReservationView);

            if (v == null) throw new DomainException(ErrorCode.NOT_FOUND, "reservation not found");
            return v;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    private static ReservationView mapReservationView(ResultSet rs) throws SQLException {
        return new ReservationView(
                rs.getLong("rental_id"),
                rs.getLong("client_id"),
                rs.getString("full_name"),
                rs.getLong("car_id"),
                rs.getString("plate_number"),
                rs.getString("brand"),
                rs.getString("model"),
                rs.getDate("date_from").toLocalDate(),
                rs.getDate("date_to").toLocalDate(),
                rs.getString("status"),
                rs.getBigDecimal("daily_rate_at_booking"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("penalty_amount"),
                rs.getBigDecimal("deposit_amount")
        );
    }
}
