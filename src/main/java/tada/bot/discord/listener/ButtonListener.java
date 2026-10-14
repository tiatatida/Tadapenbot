package tada.bot.discord.listener;

import tada.bot.discord.listener.component.DailyListener;
import tada.bot.discord.listener.component.TicketListener;
import tada.bot.discord.listener.component.VoiceListener;
import tada.bot.discord.util.VoiceRoomUI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ButtonListener extends ListenerAdapter {

    private interface VoiceButton {
        void handle(ButtonInteractionEvent event, Guild guild, Member member, long channelId);
    }

    private static final Map<String, VoiceButton> VOICE_BUTTONS = Map.of(
            VoiceRoomUI.LOCK_BUTTON_PREFIX, VoiceListener::handleLockToggle,
            VoiceRoomUI.GHOST_BUTTON_PREFIX, VoiceListener::handleGhostToggle,
            VoiceRoomUI.RENAME_BUTTON_PREFIX, VoiceListener::handleRenameButton,
            VoiceRoomUI.LIMIT_BUTTON_PREFIX, VoiceListener::handleLimitButton,
            VoiceRoomUI.PERMIT_BUTTON_PREFIX, VoiceListener::handlePermitButton,
            VoiceRoomUI.KICK_BUTTON_PREFIX, VoiceListener::handleKickButton,
            VoiceRoomUI.CLAIM_BUTTON_PREFIX, VoiceListener::handleClaimButton);

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        String componentId = event.getComponentId();
        var guild = event.getGuild();
        var member = event.getMember();
        if (guild == null || member == null) return;

        for (var entry : VOICE_BUTTONS.entrySet()) {
            if (componentId.startsWith(entry.getKey())) {
                long channelId = VoiceRoomUI.parseChannelId(componentId, entry.getKey());
                if (channelId != -1) {
                    entry.getValue().handle(event, guild, member, channelId);
                }
                return;
            }
        }

        switch (componentId) {
            case "ticketConfirm", "ticketCancel", "ticketConfirmPermanent" ->
                    TicketListener.handleTicketConfirmCancel(event, guild, member, componentId);
            case "ticketClose" -> TicketListener.handleTicketClose(event);
            case "ticketClaim" -> TicketListener.handleTicketClaim(event, guild, member);
            case "ticketUnclaim" -> TicketListener.handleTicketUnclaim(event, guild, member);
            case "dailyConfirm" -> DailyListener.handleDailyConfirm(event, member);
            default -> { /* . */ }
        }
    }
}
