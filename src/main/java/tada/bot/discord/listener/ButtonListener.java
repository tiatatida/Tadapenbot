package tada.bot.discord.listener;

import tada.bot.discord.listener.component.DailyListener;
import tada.bot.discord.listener.component.TicketListener;
import tada.bot.discord.listener.component.VoiceListener;
import tada.bot.discord.util.VoiceRoomUI;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        String componentId = event.getComponentId();
        var guild = event.getGuild();
        var member = event.getMember();
        if (guild == null || member == null) return;

        if (componentId.startsWith(VoiceRoomUI.LOCK_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.LOCK_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handleLockToggle(event, guild, member, channelId);
            }
            return;
        }

        if (componentId.startsWith(VoiceRoomUI.GHOST_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.GHOST_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handleGhostToggle(event, guild, member, channelId);
            }
            return;
        }

        if (componentId.startsWith(VoiceRoomUI.RENAME_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.RENAME_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handleRenameButton(event, guild, member, channelId);
            }
            return;
        }

        if (componentId.startsWith(VoiceRoomUI.LIMIT_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.LIMIT_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handleLimitButton(event, guild, member, channelId);
            }
            return;
        }

        if (componentId.startsWith(VoiceRoomUI.PERMIT_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.PERMIT_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handlePermitButton(event, guild, member, channelId);
            }
            return;
        }

        if (componentId.startsWith(VoiceRoomUI.KICK_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.KICK_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handleKickButton(event, guild, member, channelId);
            }
            return;
        }

        if (componentId.startsWith(VoiceRoomUI.CLAIM_BUTTON_PREFIX)) {
            long channelId = parseChannelId(componentId, VoiceRoomUI.CLAIM_BUTTON_PREFIX);
            if (channelId != -1) {
                VoiceListener.handleClaimButton(event, guild, member, channelId);
            }
            return;
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

    private long parseChannelId(String componentId, String prefix) {
        try {
            return Long.parseLong(componentId.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}