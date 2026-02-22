package carrental.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.mindrot.jbcrypt.BCrypt;
import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;
import carrental.domain.model.Client;
import carrental.domain.model.ClientWithHash;
import carrental.repository.ClientRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthService {
    private final ClientRepository clients;
    private final String jwtSecret;
    private final Map<String, RecoveryItem> recovery = new ConcurrentHashMap<>();
    private final SecureRandom rnd = new SecureRandom();

    private record RecoveryItem(String token, Instant expiresAt) {
    }

    public AuthService(ClientRepository clients, String jwtSecret) {
        this.clients = clients;
        this.jwtSecret = jwtSecret;
    }

    public long register(String fullName, String passportData, String login, String password, String email, String phone, String address) {
        fullName = trim(fullName);
        passportData = trim(passportData);
        login = trim(login);
        email = trim(email);

        if (fullName.isEmpty() || passportData.isEmpty() || login.isEmpty() || password == null || password.isBlank() || email.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION, "validation error");
        }

        phone = normalizeOptional(phone);
        address = normalizeOptional(address);

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        Client c = new Client(0, fullName, passportData, login, email, phone, address);
        return clients.create(c, hash);
    }

    public String login(String login, String password) {
        login = trim(login);
        if (login.isEmpty() || password == null || password.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION, "validation error");
        }

        ClientWithHash row;
        try {
            row = clients.getByLogin(login);
        } catch (DomainException e) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "unauthorized");
        }

        if (!BCrypt.checkpw(password, row.passwordHash())) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "unauthorized");
        }

        Algorithm alg = Algorithm.HMAC256(jwtSecret);
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(24 * 3600);

        return JWT.create().withSubject(String.valueOf(row.client().clientId())).withClaim("login", row.client().login()).withIssuedAt(now).withExpiresAt(exp).sign(alg);
    }

    public PasswordRecoveryResult passwordRecovery(String email) {
        email = trim(email);
        if (email.isEmpty()) throw new DomainException(ErrorCode.VALIDATION, "validation error");

        byte[] b = new byte[16];
        rnd.nextBytes(b);
        String token = HexFormat.of().formatHex(b);

        recovery.put(email, new RecoveryItem(token, Instant.now().plusSeconds(15 * 60)));
        return new PasswordRecoveryResult(token, 900);
    }

    public record PasswordRecoveryResult(String token, int ttlSec) {
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizeOptional(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
