package tada.bot.discord.listener;

import tada.bot.discord.database.VoiceRepository;
import tada.bot.discord.util.NameUtil;
import tada.bot.discord.util.VoiceActions;
import tada.bot.discord.util.VoiceRoomUI;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class VoiceStateListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VoiceStateListener.class);

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        AudioChannelUnion joined = event.getChannelJoined();
        if (joined != null && joined.getType() == ChannelType.VOICE) {
            handleJoin(guild, member, joined.asVoiceChannel());
        }

        AudioChannelUnion left = event.getChannelLeft();
        if (left != null && left.getType() == ChannelType.VOICE) {
            handleLeft(left.asVoiceChannel());
        }
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        long id = event.getChannel().getIdLong();
        if (event.getChannelType() == ChannelType.VOICE && VoiceRepository.isTempVoiceChannel(id)) {
            VoiceRepository.deleteVoiceChannel(id);
        }
    }

    public static void sweepOrphans(JDA jda) {
        for (long id : VoiceRepository.getTempChannelIds()) {
            VoiceChannel channel = jda.getVoiceChannelById(id);
            if (channel == null) {
                VoiceRepository.deleteVoiceChannel(id);
            } else if (channel.getMembers().isEmpty()) {
                channel.delete().queue(s -> { }, f -> { });
            }
        }
    }

    private void handleJoin(Guild guild, Member member, VoiceChannel joinedChannel) {
        Long hubChannelId = VoiceRepository.getHubChannel(guild.getIdLong());
        if (hubChannelId == null || hubChannelId != joinedChannel.getIdLong()) {
            return;
        }

        Category category = joinedChannel.getParentCategory();
        String preferredName = VoiceRepository.getPreferredName(guild.getIdLong(), member.getIdLong());
        String channelName = (preferredName != null && !preferredName.isBlank())
                ? preferredName
                : "🔊 ห้องของ " + member.getEffectiveName();
        channelName = NameUtil.limit100(channelName);

        guild.createVoiceChannel(channelName, category)
                .setBitrate(joinedChannel.getBitrate())
                .addMemberPermissionOverride(member.getIdLong(),
                        VoiceActions.OWNER_PERMS,
                        Collections.emptySet())
                .queue(newChannel -> {
                    VoiceRepository.insertVoiceChannel(newChannel.getIdLong(), guild.getIdLong(), member.getIdLong());

                    sendWelcomeMessage(newChannel);

                    guild.moveVoiceMember(member, newChannel).queue(
                            success -> logger.debug("Created temp voice channel {} for {}",
                                    newChannel.getId(), member.getEffectiveName()),
                            failure -> {
                                logger.warn("Failed to move {} into new temp voice channel, cleaning up.",
                                        member.getEffectiveName(), failure);
                                newChannel.delete().queue(s -> {
                                }, f -> {
                                });
                            });
                }, failure -> logger.error("Failed to create temp voice channel for {}",
                        member.getEffectiveName(), failure));
    }

    private void sendWelcomeMessage(VoiceChannel channel) {
        channel.sendMessageEmbeds(VoiceRoomUI.buildEmbed(channel))
                .setComponents(VoiceRoomUI.buildButtons(channel))
                .queue(
                        message -> VoiceRepository.setMessageId(channel.getIdLong(), message.getIdLong()),
                        failure -> logger.warn("Failed to send welcome message in temp voice channel {}",
                                channel.getId(), failure));
    }

    private void handleLeft(VoiceChannel leftChannel) {
        long channelId = leftChannel.getIdLong();
        if (!VoiceRepository.isTempVoiceChannel(channelId)) {
            return;
        }

        if (!leftChannel.getMembers().isEmpty()) {
            return;
        }

        leftChannel.delete().queue(
                success -> logger.debug("Deleted empty temp voice channel {}", channelId),
                failure -> logger.warn("Failed to delete empty temp voice channel {}", channelId, failure));
    }
}
