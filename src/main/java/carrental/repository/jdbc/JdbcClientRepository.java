package carrental.repository.jdbc;

import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;
import carrental.domain.model.Client;
import carrental.domain.model.ClientWithHash;
import carrental.repository.ClientRepository;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class JdbcClientRepository implements ClientRepository {
    private final DataSource ds;

    public JdbcClientRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public long create(Client client, String passwordHash) {
        try {
            Long id = JdbcUtils.queryOne(ds, """
                    INSERT INTO clients (full_name, passport_data, login, password_hash, email, phone, address)
                    VALUES (?,?,?,?,?,?,?)
                    RETURNING client_id
                    """, ps -> {
                ps.setString(1, client.fullName());
                ps.setString(2, client.passportData());
                ps.setString(3, client.login());
                ps.setString(4, passwordHash);
                ps.setString(5, client.email());
                ps.setString(6, client.phone());
                ps.setString(7, client.address());
            }, rs -> rs.getLong(1));

            if (id == null) throw new DomainException(ErrorCode.INTERNAL, "internal error");
            return id;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public ClientWithHash getByLogin(String login) {
        try {
            ClientWithHash row = JdbcUtils.queryOne(ds, """
                    SELECT client_id, full_name, passport_data, login, email, phone, address, password_hash
                    FROM clients
                    WHERE login = ?
                    """, ps -> ps.setString(1, login), rs -> {
                Client c = new Client(
                        rs.getLong("client_id"),
                        rs.getString("full_name"),
                        rs.getString("passport_data"),
                        rs.getString("login"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address")
                );
                return new ClientWithHash(c, rs.getString("password_hash"));
            });

            if (row == null) throw new DomainException(ErrorCode.NOT_FOUND, "not found");
            return row;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }

    @Override
    public boolean existsEmail(String email) {
        try {
            Integer cnt = JdbcUtils.queryOne(ds, "SELECT COUNT(*) AS cnt FROM clients WHERE email = ?",
                    ps -> ps.setString(1, email),
                    rs -> rs.getInt("cnt"));
            return cnt != null && cnt > 0;
        } catch (SQLException e) {
            throw PostgresErrorMapper.map(e);
        }
    }
}
