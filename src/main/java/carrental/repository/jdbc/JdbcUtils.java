package carrental.repository.jdbc;

import javax.sql.DataSource;
import java.sql.*;

public final class JdbcUtils {
    private JdbcUtils() {}

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public interface SqlConsumer<T> {
        void accept(T t) throws SQLException;
    }

    public static <T> T queryOne(DataSource ds, String sql, SqlConsumer<PreparedStatement> binder, RowMapper<T> mapper) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapper.map(rs);
            }
        }
    }

    public static <T> java.util.List<T> queryList(DataSource ds, String sql, SqlConsumer<PreparedStatement> binder, RowMapper<T> mapper) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.ArrayList<T> out = new java.util.ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }

    public static int exec(DataSource ds, String sql, SqlConsumer<PreparedStatement> binder) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) binder.accept(ps);
            return ps.executeUpdate();
        }
    }
}
