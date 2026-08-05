package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class DailyRepository {
    private static final Logger logger = LoggerFactory.getLogger(DailyRepository.class);

    public record DailyStatus(int currentStreak, int longestStreak, boolean alreadyCheckedInToday) {}
    public record TopStreak(long userId, int longestStreak) {}

    public static DailyStatus getStatus(long userId, long guildId) {
        String sql = "SELECT current_streak, longest_streak, last_checkin FROM daily_checkins WHERE user_id = ? AND guild_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date lastCheckin = rs.getDate("last_checkin");
                    boolean checkedToday = lastCheckin != null && lastCheckin.toLocalDate().isEqual(LocalDate.now());
                    return new DailyStatus(rs.getInt("current_streak"), rs.getInt("longest_streak"), checkedToday);
                }
                return new DailyStatus(0, 0, false);
            }
        } catch (SQLException e) {
            logger.error("Failed to get daily status for user {}", userId, e);
            return new DailyStatus(0, 0, false);
        }
    }

    public static DailyStatus checkIn(long userId, long guildId) {
        String selectSql = "SELECT current_streak, longest_streak, last_checkin FROM daily_checkins WHERE user_id = ? AND guild_id = ? FOR UPDATE";
        String upsertSql = """
                INSERT INTO daily_checkins (user_id, guild_id, current_streak, longest_streak, last_checkin)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE current_streak = ?, longest_streak = ?, last_checkin = ?
                """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int currentStreak = 0;
                int longestStreak = 0;
                LocalDate lastCheckin = null;

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, guildId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentStreak = rs.getInt("current_streak");
                            longestStreak = rs.getInt("longest_streak");
                            Date d = rs.getDate("last_checkin");
                            if (d != null) lastCheckin = d.toLocalDate();
                        }
                    }
                }

                LocalDate today = LocalDate.now();
                if (lastCheckin != null && lastCheckin.isEqual(today)) {
                    conn.rollback();
                    return null;
                }

                if (lastCheckin != null && lastCheckin.isEqual(today.minusDays(1))) {
                    currentStreak += 1;
                } else {
                    currentStreak = 1;
                }

                longestStreak = Math.max(longestStreak, currentStreak);

                try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, guildId);
                    ps.setInt(3, currentStreak);
                    ps.setInt(4, longestStreak);
                    ps.setDate(5, Date.valueOf(today));
                    ps.setInt(6, currentStreak);
                    ps.setInt(7, longestStreak);
                    ps.setDate(8, Date.valueOf(today));
                    ps.executeUpdate();
                }

                conn.commit();
                return new DailyStatus(currentStreak, longestStreak, false);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to check in user {}", userId, e);
            return null;
        }
    }

    public static TopStreak getServerTopStreak(long guildId) {
        String sql = "SELECT user_id, longest_streak FROM daily_checkins WHERE guild_id = ? ORDER BY longest_streak DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TopStreak(rs.getLong("user_id"), rs.getInt("longest_streak"));
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Failed to get top streak for guild {}", guildId, e);
            return null;
        }
    }
}