package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ReviewRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReviewRepository.class);

    public record ReviewStats(double averageRating, int reviewCount) {}

    public static void upsertReview(long guildId, long reviewerId, long reviewedId, int rating) {
        String sql = """
                INSERT INTO reviews (guild_id, reviewer_id, reviewed_id, rating)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE rating = ?, created_at = CURRENT_TIMESTAMP
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, reviewerId);
            ps.setLong(3, reviewedId);
            ps.setInt(4, rating);
            ps.setInt(5, rating);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to upsert review", e);
        }
    }

    public static LocalDateTime getRecentReviewTime(long guildId, long reviewerId, long reviewedId) {
        String sql = """
                SELECT created_at FROM reviews
                WHERE guild_id = ? AND reviewer_id = ? AND reviewed_id = ?
                  AND created_at > (NOW() - INTERVAL 24 HOUR)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, reviewerId);
            ps.setLong(3, reviewedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp("created_at").toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            logger.error("Failed to check recent review time for reviewer {} -> reviewed {}", reviewerId, reviewedId, e);
            return null;
        }
    }

    public static ReviewStats getStats(long guildId, long reviewedId) {
        String sql = "SELECT AVG(rating) AS avg_rating, COUNT(*) AS review_count FROM reviews " +
                "WHERE guild_id = ? AND reviewed_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, reviewedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_rating");
                    int count = rs.getInt("review_count");
                    return new ReviewStats(count > 0 ? avg : 0.0, count);
                }
                return new ReviewStats(0.0, 0);
            }
        } catch (SQLException e) {
            logger.error("Failed to get review stats for user {}", reviewedId, e);
            return new ReviewStats(0.0, 0);
        }
    }

    public static String buildStarText(double average, int count) {
        if (count == 0) {
            return "☆☆☆☆☆ (ยังไม่มีรีวิว)";
        }

        int fullStars = (int) Math.floor(average);
        double remainder = average - fullStars;
        boolean hasHalfStar = remainder >= 0.5;

        // กันเผื่อ edge case ค่าเฉลี่ยเป็น 5.0 พอดี ไม่ให้เกิน 5 ดวง
        if (fullStars >= 5) {
            fullStars = 5;
            hasHalfStar = false;
        }

        int emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        String stars = "⭐".repeat(fullStars) + (hasHalfStar ? "✨" : "") + "☆".repeat(Math.max(0, emptyStars));
        return stars + String.format(" (%.1f/5 จาก %d รีวิว)", average, count);
    }
}