package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WarnRepository {
    private static final Logger logger = LoggerFactory.getLogger(WarnRepository.class);

    public static final String YELLOW = "YELLOW";
    public static final String ORANGE = "ORANGE";
    public static final String RED = "RED";

    public record WarnEntry(long moderatorId, String reason, String color, LocalDateTime createdAt,
            LocalDateTime expiresAt) {
    }

    public static boolean insertWarn(long guildId, long userId, long moderatorId, String reason, String color) {
        LocalDateTime expiresAt = switch (color) {
            case YELLOW -> LocalDateTime.now().plusMonths(3);
            case ORANGE -> LocalDateTime.now().plusMonths(6);
            default -> null;
        };

        String sql = "INSERT INTO warnings (guild_id, user_id, moderator_id, reason, color, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            ps.setLong(3, moderatorId);
            ps.setString(4, reason);
            ps.setString(5, color);
            if (expiresAt != null) {
                ps.setTimestamp(6, Timestamp.valueOf(expiresAt));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Failed to insert warning", e);
            return false;
        }
    }

    public static int countWarnings(long guildId, long userId) {
        String sql = "SELECT COUNT(*) FROM warnings WHERE guild_id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to count warnings for user {}", userId, e);
            return 0;
        }
    }
    
    public static int countActiveYellowOrange(long guildId, long userId) {
        String sql = "SELECT COUNT(*) FROM warnings WHERE guild_id = ? AND user_id = ? "
                + "AND color IN ('YELLOW', 'ORANGE') AND expires_at > NOW()";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to count active yellow/orange warnings for user {}", userId, e);
            return 0;
        }
    }

    public static List<WarnEntry> getWarnings(long guildId, long userId, int limit) {
        String sql = "SELECT moderator_id, reason, color, expires_at, created_at FROM warnings "
                + "WHERE guild_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<WarnEntry> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp expiresAtTs = rs.getTimestamp("expires_at");
                    results.add(new WarnEntry(
                            rs.getLong("moderator_id"),
                            rs.getString("reason"),
                            rs.getString("color"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            expiresAtTs != null ? expiresAtTs.toLocalDateTime() : null));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query warnings for user {}", userId, e);
        }
        return results;
    }
}
