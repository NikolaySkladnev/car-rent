package carrental.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;

public final class AuthMiddleware implements Handler {
    public static final String ATTR_CLIENT_ID = "client_id";
    private static final String BEARER = "bearer";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtService jwt;

    public AuthMiddleware(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public void handle(Context ctx) {
        String header = ctx.header(AUTH_HEADER);
        if (header == null || header.isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "missing Authorization header");
        }
        String[] parts = header.split(" ", 2);
        if (parts.length != 2 || !BEARER.equalsIgnoreCase(parts[0])) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "invalid Authorization header");
        }
        String token = parts[1].trim();
        if (token.isEmpty()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "missing token");
        }
        ctx.attribute(ATTR_CLIENT_ID, jwt.verifyAndGetClientId(token));
    }
}
