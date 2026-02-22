package carrental.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;

public final class JwtService {
    private final JWTVerifier verifier;

    public JwtService(String secret) {
        Algorithm alg = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(alg).build();
    }

    public long verifyAndGetClientId(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            long id = Long.parseLong(jwt.getSubject());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (Exception e) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "invalid token");
        }
    }
}
