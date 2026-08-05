package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GuildSettingsRepository {
    private static final Logger logger = LoggerFactory.getLogger(GuildSettingsRepository.class);

    public static void setReportChannel(long guildId, long channelId) {
        String sql = """
                INSERT INTO guild_settings (guild_id, report_channel_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE report_channel_id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, channelId);
            ps.setLong(3, channelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set report channel for guild {}", guildId, e);
        }
    }

    public static Long getReportChannel(long guildId) {
        String sql = "SELECT report_channel_id FROM guild_settings WHERE guild_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("report_channel_id");
                    return rs.wasNull() ? null : id;
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Failed to get report channel for guild {}", guildId, e);
            return null;
        }
    }
}