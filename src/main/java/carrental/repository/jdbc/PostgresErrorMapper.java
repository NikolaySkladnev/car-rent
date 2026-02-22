package carrental.repository.jdbc;

import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;

import java.sql.SQLException;

public final class PostgresErrorMapper {
    private PostgresErrorMapper() {}

    public static RuntimeException map(SQLException e) {
        if (e == null) return new DomainException(ErrorCode.INTERNAL, "internal error");

        String state = e.getSQLState();
        String msg = (e.getMessage() == null) ? "" : e.getMessage().toLowerCase();

        if ("23505".equals(state)) {
            return new DomainException(ErrorCode.CONFLICT, "conflict", e);
        }
        if ("23P01".equals(state)) {
            return new DomainException(ErrorCode.CONFLICT, "car is not available for selected dates", e);
        }
        if ("23503".equals(state) || "23514".equals(state)) {
            return new DomainException(ErrorCode.VALIDATION, "validation error", e);
        }
        if ("22007".equals(state) || "22008".equals(state) || "22P02".equals(state)) {
            return new DomainException(ErrorCode.VALIDATION, "validation error", e);
        }
        if ("P0001".equals(state)) {
            if (msg.contains("not available") || msg.contains("unavailable")) {
                return new DomainException(ErrorCode.CONFLICT, "car is not available for selected dates", e);
            }
            return new DomainException(ErrorCode.VALIDATION, "validation error", e);
        }

        return new DomainException(ErrorCode.INTERNAL, "internal error", e);
    }
}
