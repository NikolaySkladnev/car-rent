package carrental.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import carrental.api.dto.CreateReservationRequest;
import carrental.api.dto.LoginRequest;
import carrental.api.dto.PasswordRecoveryRequest;
import carrental.api.dto.RegisterRequest;
import carrental.config.AppConfig;
import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;
import carrental.service.AuthService;
import carrental.service.CarsService;
import carrental.service.ReservationsService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

public final class ApiRoutes {
    private ApiRoutes() {
    }

    public static Javalin build(AppConfig cfg,
                                AuthService authUC,
                                CarsService carsUC,
                                ReservationsService resUC) {

        JwtService jwt = new JwtService(cfg.jwtSecret());
        AuthMiddleware auth = new AuthMiddleware(jwt);

        Javalin app = Javalin.create(jc -> {
            jc.http.defaultContentType = "application/json; charset=utf-8";
            jc.jsonMapper(new JavalinJackson());
        });

        app.exception(DomainException.class, (e, ctx) -> {
            HttpStatus st = switch (e.code()) {
                case VALIDATION -> HttpStatus.BAD_REQUEST;
                case CONFLICT -> HttpStatus.CONFLICT;
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
                case CLIENT_BLOCKED -> HttpStatus.FORBIDDEN;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            ctx.status(st).json(Map.of("error", e.getMessage(), "status", st.getCode()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(Map.of("error", "internal error", "status", 500));
        });

        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        app.post("/api/v1/auth/register", ctx -> {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
            long id = authUC.register(
                    req.fullName(),
                    req.passportData(),
                    req.login(),
                    req.password(),
                    req.email(),
                    req.phone(),
                    req.address()
            );
            ctx.status(HttpStatus.CREATED).json(Map.of("client_id", id));
        });

        app.post("/api/v1/auth/login", ctx -> {
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            String token = authUC.login(req.login(), req.password());
            ctx.json(Map.of("token", token));
        });

        app.post("/api/v1/auth/password-recovery", ctx -> {
            PasswordRecoveryRequest req = ctx.bodyAsClass(PasswordRecoveryRequest.class);
            var out = authUC.passwordRecovery(req.email());
            ctx.json(Map.of(
                    "token", out.token(),
                    "ttlSec", out.ttlSec(),
                    "ttl_sec", out.ttlSec()
            ));
        });

        app.before("/api/v1/auth/logout", auth);
        app.post("/api/v1/auth/logout", ctx -> ctx.json(Map.of("status", "ok")));

        app.get("/api/v1/cars", ctx -> {
            String fromStr = ctx.queryParam("date_from");
            String toStr = ctx.queryParam("date_to");

            LocalDate from = null;
            LocalDate to = null;

            if ((fromStr != null && !fromStr.isBlank()) || (toStr != null && !toStr.isBlank())) {
                try {
                    from = LocalDate.parse(Objects.requireNonNull(fromStr));
                    to = LocalDate.parse(Objects.requireNonNull(toStr));
                } catch (DateTimeParseException ex) {
                    throw new DomainException(ErrorCode.VALIDATION, "date_from/date_to must be YYYY-MM-DD");
                }
                if (!to.isAfter(from)) {
                    throw new DomainException(ErrorCode.VALIDATION, "date_to must be greater than date_from");
                }
            }

            ctx.json(carsUC.listAvailable(from, to));
        });

        app.get("/api/v1/cars/{id}", ctx -> {
            long id = parseId(ctx.pathParam("id"));
            ctx.json(carsUC.get(id));
        });

        app.before("/api/v1/reservations", auth);
        app.before("/api/v1/reservations/*", auth);

        app.post("/api/v1/reservations", ctx -> {
            long clientId = requireClientId(ctx);

            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);
            LocalDate from = parseDate(req.dateFrom(), "date_from");
            LocalDate to = parseDate(req.dateTo(), "date_to");

            var out = resUC.createAndConfirm(clientId, req.carId(), from, to);
            ctx.status(HttpStatus.CREATED).json(out);
        });

        app.get("/api/v1/reservations/me", ctx -> {
            long clientId = requireClientId(ctx);
            ctx.json(resUC.listMine(clientId));
        });

        app.get("/api/v1/reservations/{id}", ctx -> {
            long clientId = requireClientId(ctx);
            long id = parseId(ctx.pathParam("id"));
            ctx.json(resUC.getMine(id, clientId));
        });

        app.delete("/api/v1/reservations/{id}", ctx -> {
            long clientId = requireClientId(ctx);
            long id = parseId(ctx.pathParam("id"));
            resUC.cancel(id, clientId);
            ctx.json(Map.of("rental_id", id, "status", "canceled"));
        });

        return app;
    }

    private static long requireClientId(Context ctx) {
        Long id = ctx.attribute(AuthMiddleware.ATTR_CLIENT_ID);
        if (id == null || id <= 0) throw new DomainException(ErrorCode.UNAUTHORIZED, "unauthorized");
        return id;
    }

    private static long parseId(String s) {
        try {
            long id = Long.parseLong(s);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.VALIDATION, "invalid id");
        }
    }

    private static LocalDate parseDate(String s, String fieldName) {
        if (s == null || s.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION, fieldName + " must be YYYY-MM-DD");
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            throw new DomainException(ErrorCode.VALIDATION, fieldName + " must be YYYY-MM-DD");
        }
    }
}