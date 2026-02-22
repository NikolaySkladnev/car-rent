package carrental;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import carrental.config.AppConfig;
import carrental.api.ApiRoutes;
import carrental.repository.CarRepository;
import carrental.repository.ClientRepository;
import carrental.repository.ReservationRepository;
import carrental.repository.jdbc.JdbcCarRepository;
import carrental.repository.jdbc.JdbcClientRepository;
import carrental.repository.jdbc.JdbcReservationRepository;
import carrental.service.AuthService;
import carrental.service.CarsService;
import carrental.service.ReservationsService;

import javax.sql.DataSource;

public final class CarRentalApplication {

    public static void main(String[] args) {
        AppConfig cfg = AppConfig.fromEnv();

        DataSource ds = buildDataSource(cfg);
        migrate(ds);

        ClientRepository clientRepo = new JdbcClientRepository(ds);
        CarRepository carRepo = new JdbcCarRepository(ds);
        ReservationRepository resRepo = new JdbcReservationRepository(ds);

        AuthService authUC = new AuthService(clientRepo, cfg.jwtSecret());
        CarsService carsUC = new CarsService(carRepo);
        ReservationsService resUC = new ReservationsService(resRepo);

        Javalin app = ApiRoutes.build(cfg, authUC, carsUC, resUC);

        app.start(cfg.httpPort());
    }

    private static DataSource buildDataSource(AppConfig cfg) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(cfg.jdbcUrl());
        hc.setUsername(cfg.dbUser());
        hc.setPassword(cfg.dbPassword());
        return new HikariDataSource(hc);
    }

    private static void migrate(DataSource ds) {
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
