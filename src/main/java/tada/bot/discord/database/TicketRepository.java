package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TicketRepository {
    private static final Logger logger = LoggerFactory.getLogger(TicketRepository.class);

    public static void insertTicket(long channelId, long guildId, long ownerId) {
        String sql = "INSERT INTO tickets (channel_id, guild_id, owner_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.setLong(2, guildId);
            ps.setLong(3, ownerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert ticket for channel {}", channelId, e);
        }
    }

    public static boolean isTicketChannel(long channelId) {
        String sql = "SELECT 1 FROM tickets WHERE channel_id = ? AND status != 'CLOSED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check ticket channel {}", channelId, e);
            return false;
        }
    }

    public static boolean isOwner(long channelId, long userId) {
        String sql = "SELECT 1 FROM tickets WHERE channel_id = ? AND owner_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check ticket owner for channel {}", channelId, e);
            return false;
        }
    }

    public static Long getOpenTicketChannelId(long guildId, long ownerId) {
        String sql = "SELECT channel_id FROM tickets WHERE guild_id = ? AND owner_id = ? AND status != 'CLOSED' " +
                "ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("channel_id") : null;
            }
        } catch (SQLException e) {
            logger.error("Failed to get open ticket for owner {}", ownerId, e);
            return null;
        }
    }

    public static void setClaimed(long channelId, long staffId) {
        String sql = "UPDATE tickets SET status = 'CLAIMED', claimed_by = ? WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setLong(2, channelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set claimed for channel {}", channelId, e);
        }
    }

    public static void closeTicket(long channelId) {
        String sql = "UPDATE tickets SET status = 'CLOSED', closed_at = NOW() WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to close ticket for channel {}", channelId, e);
        }
    }
}