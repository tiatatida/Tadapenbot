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

public class ReportRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReportRepository.class);

    public record ReportEntry(long reporterId, long reportedId, String reason, LocalDateTime createdAt) {}

    public static void insertReport(long guildId, long reporterId, long reportedId, String reason) {
        String sql = "INSERT INTO reports (guild_id, reporter_id, reported_id, reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, reporterId);
            ps.setLong(3, reportedId);
            ps.setString(4, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert report", e);
        }
    }

    public static LocalDateTime getRecentReportTime(long guildId, long reporterId, long reportedId) {
        String sql = """
                SELECT created_at FROM reports
                WHERE guild_id = ? AND reporter_id = ? AND reported_id = ?
                  AND created_at > (NOW() - INTERVAL 24 HOUR)
                ORDER BY created_at DESC LIMIT 1
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, reporterId);
            ps.setLong(3, reportedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp("created_at").toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            logger.error("Failed to check recent report time for reporter {} -> reported {}", reporterId, reportedId, e);
            return null;
        }
    }

    public static int countReceivedReports(long guildId, long userId) {
        return count("SELECT COUNT(*) FROM reports WHERE guild_id = ? AND reported_id = ?", guildId, userId);
    }

    public static int countGivenReports(long guildId, long userId) {
        return count("SELECT COUNT(*) FROM reports WHERE guild_id = ? AND reporter_id = ?", guildId, userId);
    }

    public static List<ReportEntry> getReceivedReports(long guildId, long userId, int limit) {
        String sql = "SELECT reporter_id, reported_id, reason, created_at FROM reports " +
                "WHERE guild_id = ? AND reported_id = ? ORDER BY created_at DESC LIMIT ?";
        return queryEntries(sql, guildId, userId, limit);
    }

    public static List<ReportEntry> getGivenReports(long guildId, long userId, int limit) {
        String sql = "SELECT reporter_id, reported_id, reason, created_at FROM reports " +
                "WHERE guild_id = ? AND reporter_id = ? ORDER BY created_at DESC LIMIT ?";
        return queryEntries(sql, guildId, userId, limit);
    }

    private static List<ReportEntry> queryEntries(String sql, long guildId, long userId, int limit) {
        List<ReportEntry> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new ReportEntry(
                            rs.getLong("reporter_id"),
                            rs.getLong("reported_id"),
                            rs.getString("reason"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query report entries for user {}", userId, e);
        }
        return results;
    }

    private static int count(String sql, long guildId, long userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to count reports for user {}", userId, e);
            return 0;
        }
    }
}