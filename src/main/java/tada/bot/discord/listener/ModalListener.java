package tada.bot.discord.listener;

import tada.bot.discord.listener.component.ReportListener;
import tada.bot.discord.listener.component.VoiceListener;
import tada.bot.discord.util.VoiceRoomUI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ModalListener extends ListenerAdapter {

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null)
            return;

        if (modalId.startsWith("reportModal:")) {
            ReportListener.handleReportModal(event, guild);
            return;
        }

        if (modalId.startsWith(VoiceRoomUI.RENAME_MODAL_PREFIX)) {
            long channelId = VoiceRoomUI.parseChannelId(modalId, VoiceRoomUI.RENAME_MODAL_PREFIX);
            if (channelId != -1)
                VoiceListener.handleRenameModalSubmit(event, guild, member, channelId);
            return;
        }

        if (modalId.startsWith(VoiceRoomUI.LIMIT_MODAL_PREFIX)) {
            long channelId = VoiceRoomUI.parseChannelId(modalId, VoiceRoomUI.LIMIT_MODAL_PREFIX);
            if (channelId != -1)
                VoiceListener.handleLimitModalSubmit(event, guild, member, channelId);
            return;
        }

        if (modalId.startsWith(VoiceRoomUI.PERMIT_MODAL_PREFIX)) {
            long channelId = VoiceRoomUI.parseChannelId(modalId, VoiceRoomUI.PERMIT_MODAL_PREFIX);
            if (channelId != -1)
                VoiceListener.handlePermitModalSubmit(event, guild, member, channelId);
            return;
        }

        if (modalId.startsWith(VoiceRoomUI.KICK_MODAL_PREFIX)) {
            long channelId = VoiceRoomUI.parseChannelId(modalId, VoiceRoomUI.KICK_MODAL_PREFIX);
            if (channelId != -1)
                VoiceListener.handleKickModalSubmit(event, guild, member, channelId);
        }
    }
}
