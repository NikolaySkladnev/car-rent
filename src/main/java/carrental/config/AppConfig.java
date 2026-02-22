package carrental.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public record AppConfig(
        String httpAddr,
        int httpPort,
        String jwtSecret,
        String jdbcUrl,
        String dbUser,
        String dbPassword
) {
    private static final String ENV_HTTP_ADDR = "HTTP_ADDR";
    private static final String ENV_JWT_SECRET = "JWT_SECRET";

    private static final String ENV_DATABASE_URL = "DATABASE_URL";
    private static final String ENV_JDBC_URL = "JDBC_URL";
    private static final String ENV_DB_USER = "DB_USER";
    private static final String ENV_DB_PASSWORD = "DB_PASSWORD";

    private static final String DEFAULT_HTTP_ADDR = ":8080";
    private static final int DEFAULT_HTTP_PORT = 8080;

    private static final String DEFAULT_JWT_SECRET = "dev-secret";

    private static final String DEFAULT_DB_USER = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";
    private static final String DEFAULT_DB_NAME = "car_house";
    private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:5432/" + DEFAULT_DB_NAME;

    public static AppConfig fromEnv() {
        Map<String, String> env = System.getenv();

        String httpAddr = readHttpAddr(env);
        int httpPort = parsePort(httpAddr);

        String jwtSecret = readJwtSecret(env);

        DbParsed db = readDatabase(env);

        return new AppConfig(
                httpAddr,
                httpPort,
                jwtSecret,
                db.jdbcUrl(),
                db.user(),
                db.password()
        );
    }

    private static String readHttpAddr(Map<String, String> env) {
        String raw = env.getOrDefault(ENV_HTTP_ADDR, DEFAULT_HTTP_ADDR);
        return raw == null ? DEFAULT_HTTP_ADDR : raw.trim();
    }

    private static String readJwtSecret(Map<String, String> env) {
        String raw = env.getOrDefault(ENV_JWT_SECRET, DEFAULT_JWT_SECRET);
        return (raw == null || raw.isBlank()) ? DEFAULT_JWT_SECRET : raw.trim();
    }

    private static int parsePort(String httpAddr) {
        if (httpAddr == null) return DEFAULT_HTTP_PORT;
        String s = httpAddr.trim();
        int idx = s.lastIndexOf(':');
        if (idx < 0 || idx == s.length() - 1) return DEFAULT_HTTP_PORT;
        try {
            int p = Integer.parseInt(s.substring(idx + 1));
            return (p > 0 && p <= 65535) ? p : DEFAULT_HTTP_PORT;
        } catch (NumberFormatException e) {
            return DEFAULT_HTTP_PORT;
        }
    }

    private record DbCreds(String user, String password) {}
    private record DbParsed(String jdbcUrl, String user, String password) {}

    private static DbParsed readDatabase(Map<String, String> env) {
        DbCreds creds = readDbCreds(env);

        String raw = env.getOrDefault(ENV_DATABASE_URL, "");
        String trimmed = (raw == null) ? "" : raw.trim();

        if (trimmed.isBlank()) {
            return fromJdbcEnvOrDefault(env, creds);
        }

        if (trimmed.startsWith("jdbc:")) {
            return new DbParsed(trimmed, creds.user(), creds.password());
        }

        if (isPostgresScheme(trimmed)) {
            return fromPostgresUriOrFallback(env, trimmed, creds);
        }

        return fromJdbcEnvOrDefault(env, creds);
    }

    private static DbCreds readDbCreds(Map<String, String> env) {
        String user = env.getOrDefault(ENV_DB_USER, DEFAULT_DB_USER);
        String pass = env.getOrDefault(ENV_DB_PASSWORD, DEFAULT_DB_PASSWORD);

        user = (user == null || user.isBlank()) ? DEFAULT_DB_USER : user.trim();
        pass = (pass == null) ? DEFAULT_DB_PASSWORD : pass;

        return new DbCreds(user, pass);
    }

    private static boolean isPostgresScheme(String s) {
        return s.startsWith("postgres://") || s.startsWith("postgresql://");
    }

    private static DbParsed fromJdbcEnvOrDefault(Map<String, String> env, DbCreds creds) {
        String jdbc = env.getOrDefault(ENV_JDBC_URL, DEFAULT_JDBC_URL);
        jdbc = (jdbc == null || jdbc.isBlank()) ? DEFAULT_JDBC_URL : jdbc.trim();
        return new DbParsed(jdbc, creds.user(), creds.password());
    }

    private static DbParsed fromPostgresUriOrFallback(Map<String, String> env, String rawUrl, DbCreds creds) {
        try {
            URI uri = new URI(rawUrl);

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return fromJdbcEnvOrDefault(env, creds);
            }

            int port = (uri.getPort() == -1) ? 5432 : uri.getPort();

            String db = normalizeDbName(uri.getPath());
            String jdbc = buildJdbcUrl(host, port, db, uri.getQuery());

            DbCreds effectiveCreds = overrideCredsFromUserInfo(creds, uri.getUserInfo());

            return new DbParsed(jdbc, effectiveCreds.user(), effectiveCreds.password());
        } catch (URISyntaxException e) {
            return fromJdbcEnvOrDefault(env, creds);
        }
    }

    private static String normalizeDbName(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) return DEFAULT_DB_NAME;
        String p = path.startsWith("/") ? path.substring(1) : path;
        return p.isBlank() ? DEFAULT_DB_NAME : p;
    }

    private static String buildJdbcUrl(String host, int port, String db, String query) {
        String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        if (query != null && !query.isBlank()) {
            jdbc += "?" + query;
        }
        return jdbc;
    }

    private static DbCreds overrideCredsFromUserInfo(DbCreds base, String userInfo) {
        if (userInfo == null || userInfo.isBlank()) return base;

        if (userInfo.contains(":")) {
            String[] parts = userInfo.split(":", 2);
            String user = parts[0];
            String pass = parts.length > 1 ? parts[1] : "";
            if (user != null && !user.isBlank()) {
                return new DbCreds(user, pass);
            }
        } else {
            String user = userInfo.trim();
            if (!user.isBlank()) {
                return new DbCreds(user, base.password());
            }
        }
        return base;
    }
}