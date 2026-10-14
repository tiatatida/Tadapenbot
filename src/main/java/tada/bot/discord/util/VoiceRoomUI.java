package tada.bot.discord.util;

import tada.bot.discord.database.VoiceRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.awt.Color;
import java.util.List;

public class VoiceRoomUI {

    public static final String LOCK_BUTTON_PREFIX = "voiceLock:";
    public static final String GHOST_BUTTON_PREFIX = "voiceGhost:";
    public static final String RENAME_BUTTON_PREFIX = "voiceRename:";
    public static final String LIMIT_BUTTON_PREFIX = "voiceLimit:";
    public static final String PERMIT_BUTTON_PREFIX = "voicePermit:";
    public static final String KICK_BUTTON_PREFIX = "voiceKick:";
    public static final String CLAIM_BUTTON_PREFIX = "voiceClaim:";

    public static final String RENAME_MODAL_PREFIX = "voiceRenameModal:";
    public static final String LIMIT_MODAL_PREFIX = "voiceLimitModal:";
    public static final String PERMIT_MODAL_PREFIX = "voicePermitModal:";
    public static final String KICK_MODAL_PREFIX = "voiceKickModal:";

    public static long parseChannelId(String componentId, String prefix) {
        try {
            return Long.parseLong(componentId.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean isLocked(VoiceChannel channel) {
        Role publicRole = channel.getGuild().getPublicRole();
        PermissionOverride override = channel.getPermissionOverride(publicRole);
        return override != null && override.getDenied().contains(Permission.VOICE_CONNECT);
    }

    public static boolean isGhosted(VoiceChannel channel) {
        Role publicRole = channel.getGuild().getPublicRole();
        PermissionOverride override = channel.getPermissionOverride(publicRole);
        return override != null && override.getDenied().contains(Permission.VIEW_CHANNEL);
    }

    public static MessageEmbed buildEmbed(VoiceChannel channel) {
        Guild guild = channel.getGuild();
        long channelId = channel.getIdLong();

        Long creatorId = VoiceRepository.getCreator(channelId);
        Long ownerId = VoiceRepository.getOwner(channelId);

        String creatorMention = creatorId != null ? "<@" + creatorId + ">" : "ไม่ทราบ";
        String ownerMention = ownerId != null ? "<@" + ownerId + ">" : "ไม่ทราบ";

        boolean locked = isLocked(channel);
        boolean ghosted = isGhosted(channel);
        String status = (locked ? "🔒 ล็อก" : "🔓 ปลดล็อก") + "  ·  " + (ghosted ? "👻 ซ่อน" : "👀 มองเห็นได้");

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode("#f5e0dc"))
                .setTitle("🔊 Voice Channel")
                .addField("เปิดโดย", creatorMention, true)
                .addField("เจ้าของปัจจุบัน", ownerMention, true)
                .addField("สถานะ", status, false);

        Member ownerMember = ownerId != null ? guild.getMemberById(ownerId) : null;
        if (ownerMember != null) {
            embed.setThumbnail(ownerMember.getUser().getEffectiveAvatarUrl());
        }

        return embed.build();
    }

    public static List<ActionRow> buildButtons(VoiceChannel channel) {
        long channelId = channel.getIdLong();
        boolean locked = isLocked(channel);
        boolean ghosted = isGhosted(channel);

        ActionRow actionRow = ActionRow.of(
                Button.of(ButtonStyle.PRIMARY, RENAME_BUTTON_PREFIX + channelId, Emoji.fromUnicode("✏️")),
                Button.of(ButtonStyle.PRIMARY, LIMIT_BUTTON_PREFIX + channelId, Emoji.fromUnicode("🔢")),
                Button.of(ButtonStyle.PRIMARY, KICK_BUTTON_PREFIX + channelId, Emoji.fromUnicode("👢")),
                Button.of(ButtonStyle.PRIMARY, PERMIT_BUTTON_PREFIX + channelId, Emoji.fromUnicode("✅")));

        ActionRow toggleRow = ActionRow.of(
                Button.of(locked ? ButtonStyle.SUCCESS : ButtonStyle.DANGER,
                        LOCK_BUTTON_PREFIX + channelId, Emoji.fromUnicode("🔒")),
                Button.of(ghosted ? ButtonStyle.SUCCESS : ButtonStyle.DANGER,
                        GHOST_BUTTON_PREFIX + channelId, Emoji.fromUnicode("👻")));

        ActionRow claimRow = ActionRow.of(
                Button.of(ButtonStyle.SECONDARY, CLAIM_BUTTON_PREFIX + channelId, Emoji.fromUnicode("👑")));

        return List.of(actionRow, toggleRow, claimRow);
    }

    public static void refreshMessage(VoiceChannel channel) {
        Long messageId = VoiceRepository.getMessageId(channel.getIdLong());
        if (messageId == null) {
            return;
        }

        channel.editMessageEmbedsById(messageId, buildEmbed(channel))
                .setComponents(buildButtons(channel))
                .queue(success -> {
                }, failure -> {
                });
    }
}
