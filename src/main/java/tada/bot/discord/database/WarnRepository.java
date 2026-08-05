package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WarnRepository {
    private static final Logger logger = LoggerFactory.getLogger(WarnRepository.class);

    public record WarnEntry(long moderatorId, String reason, LocalDateTime createdAt) {}

    public static void insertWarn(long guildId, long userId, long moderatorId, String reason) {
        String sql = "INSERT INTO warnings (guild_id, user_id, moderator_id, reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            ps.setLong(3, moderatorId);
            ps.setString(4, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert warning", e);
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

    public static List<WarnEntry> getWarnings(long guildId, long userId, int limit) {
        String sql = "SELECT moderator_id, reason, created_at FROM warnings " +
                "WHERE guild_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<WarnEntry> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new WarnEntry(
                            rs.getLong("moderator_id"),
                            rs.getString("reason"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query warnings for user {}", userId, e);
        }
        return results;
    }
}