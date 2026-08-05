package tada.bot.discord.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;

    public static void init() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("config.properties not found!");
            }
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Failed to load database config!", e);
            throw new RuntimeException(e);
        }

        String host = properties.getProperty("db.host");
        String port = properties.getProperty("db.port");
        String name = properties.getProperty("db.name");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + name);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("TadaBotPool");

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized.");

        createTables();
    }

    private static void createTables() {
        String tickets = """
                CREATE TABLE IF NOT EXISTS tickets (
                    channel_id BIGINT PRIMARY KEY,
                    guild_id BIGINT NOT NULL,
                    owner_id BIGINT NOT NULL,
                    claimed_by BIGINT NULL,
                    status ENUM('OPEN','CLAIMED','CLOSED') NOT NULL DEFAULT 'OPEN',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    closed_at TIMESTAMP NULL
                )
                """;

        String dailyCheckins = """
                CREATE TABLE IF NOT EXISTS daily_checkins (
                    user_id BIGINT NOT NULL,
                    guild_id BIGINT NOT NULL,
                    current_streak INT NOT NULL DEFAULT 0,
                    longest_streak INT NOT NULL DEFAULT 0,
                    last_checkin DATE NULL,
                    PRIMARY KEY (user_id, guild_id)
                )
                """;

        String guildSettings = """
        CREATE TABLE IF NOT EXISTS guild_settings (
            guild_id BIGINT PRIMARY KEY,
            report_channel_id BIGINT NULL
        )
        """;

        String reports = """
        CREATE TABLE IF NOT EXISTS reports (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            guild_id BIGINT NOT NULL,
            reporter_id BIGINT NOT NULL,
            reported_id BIGINT NOT NULL,
            reason VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_reported (guild_id, reported_id),
            INDEX idx_reporter (guild_id, reporter_id)
        )
        """;

        String warnings = """
        CREATE TABLE IF NOT EXISTS warnings (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            guild_id BIGINT NOT NULL,
            user_id BIGINT NOT NULL,
            moderator_id BIGINT NOT NULL,
            reason VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_user (guild_id, user_id)
        )
        """;

        String reviews = """
        CREATE TABLE IF NOT EXISTS reviews (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            guild_id BIGINT NOT NULL,
            reviewer_id BIGINT NOT NULL,
            reviewed_id BIGINT NOT NULL,
            rating TINYINT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_review (guild_id, reviewer_id, reviewed_id),
            INDEX idx_reviewed (guild_id, reviewed_id)
        )
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(tickets);
            stmt.execute(dailyCheckins);
            stmt.execute(guildSettings);
            stmt.execute(reports);
            stmt.execute(warnings);
            stmt.execute(reviews);
            logger.info("Database tables verified/created.");
        } catch (SQLException e) {
            logger.error("Failed to create tables!", e);
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager has not been initialized!");
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }
}