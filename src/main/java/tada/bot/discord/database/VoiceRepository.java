package tada.bot.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceRepository {
    private static final Logger logger = LoggerFactory.getLogger(VoiceRepository.class);

    private static final Set<Long> tempIds = ConcurrentHashMap.newKeySet();
    private static final Map<Long, Long> hubs = new ConcurrentHashMap<>();

    public static void loadCache() {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT channel_id FROM temp_voice_channels")) {
                while (rs.next()) tempIds.add(rs.getLong(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT guild_id, channel_id FROM voice_hub")) {
                while (rs.next()) hubs.put(rs.getLong(1), rs.getLong(2));
            }
        } catch (SQLException e) {
            logger.error("Failed to load voice cache!", e);
            throw new RuntimeException(e);
        }
    }

    public static List<Long> getTempChannelIds() {
        return new ArrayList<>(tempIds);
    }

    public static boolean setHubChannel(long guildId, long channelId) {
        String sql = """
                INSERT INTO voice_hub (guild_id, channel_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE channel_id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, channelId);
            ps.setLong(3, channelId);
            ps.executeUpdate();
            hubs.put(guildId, channelId);
            return true;
        } catch (SQLException e) {
            logger.error("Failed to set voice hub channel for guild {}", guildId, e);
            return false;
        }
    }

    public static Long getHubChannel(long guildId) {
        return hubs.get(guildId);
    }

    public static void insertVoiceChannel(long channelId, long guildId, long ownerId) {
        String sql = "INSERT INTO temp_voice_channels (channel_id, guild_id, owner_id, creator_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.setLong(2, guildId);
            ps.setLong(3, ownerId);
            ps.setLong(4, ownerId);
            ps.executeUpdate();
            tempIds.add(channelId);
        } catch (SQLException e) {
            logger.error("Failed to insert temp voice channel {}", channelId, e);
        }
    }

    public static void deleteVoiceChannel(long channelId) {
        tempIds.remove(channelId);
        String sql = "DELETE FROM temp_voice_channels WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete temp voice channel {}", channelId, e);
        }
    }

    public static boolean isTempVoiceChannel(long channelId) {
        return tempIds.contains(channelId);
    }

    public static boolean isOwner(long channelId, long userId) {
        String sql = "SELECT 1 FROM temp_voice_channels WHERE channel_id = ? AND owner_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check owner of temp voice channel {}", channelId, e);
            return false;
        }
    }

    public static Long getOwner(long channelId) {
        String sql = "SELECT owner_id FROM temp_voice_channels WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("owner_id") : null;
            }
        } catch (SQLException e) {
            logger.error("Failed to get owner of temp voice channel {}", channelId, e);
            return null;
        }
    }

    public static boolean transferOwner(long channelId, Long expectedOwnerId, long newOwnerId) {
        String sql = "UPDATE temp_voice_channels SET owner_id = ? WHERE channel_id = ? AND owner_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newOwnerId);
            ps.setLong(2, channelId);
            ps.setObject(3, expectedOwnerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.error("Failed to transfer owner of temp voice channel {}", channelId, e);
            return false;
        }
    }

    public static Long getCreator(long channelId) {
        String sql = "SELECT creator_id FROM temp_voice_channels WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long creatorId = rs.getLong("creator_id");
                return rs.wasNull() ? null : creatorId;
            }
        } catch (SQLException e) {
            logger.error("Failed to get creator of temp voice channel {}", channelId, e);
            return null;
        }
    }

    public static void setMessageId(long channelId, long messageId) {
        String sql = "UPDATE temp_voice_channels SET message_id = ? WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setLong(2, channelId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set control message id for temp voice channel {}", channelId, e);
        }
    }

    public static Long getMessageId(long channelId) {
        String sql = "SELECT message_id FROM temp_voice_channels WHERE channel_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long messageId = rs.getLong("message_id");
                return rs.wasNull() ? null : messageId;
            }
        } catch (SQLException e) {
            logger.error("Failed to get control message id for temp voice channel {}", channelId, e);
            return null;
        }
    }

    public static void setPreferredName(long guildId, long userId, String name) {
        String sql = """
                INSERT INTO voice_user_prefs (guild_id, user_id, preferred_name)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE preferred_name = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            ps.setString(3, name);
            ps.setString(4, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set preferred voice channel name for user {}", userId, e);
        }
    }

    public static String getPreferredName(long guildId, long userId) {
        String sql = "SELECT preferred_name FROM voice_user_prefs WHERE guild_id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("preferred_name") : null;
            }
        } catch (SQLException e) {
            logger.error("Failed to get preferred voice channel name for user {}", userId, e);
            return null;
        }
    }
}
