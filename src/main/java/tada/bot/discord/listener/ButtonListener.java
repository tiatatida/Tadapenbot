package tada.bot.discord.listener;

import tada.bot.discord.listener.component.DailyListener;
import tada.bot.discord.listener.component.TicketListener;
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

        switch (componentId) {
            case "ticketConfirm", "ticketCancel", "ticketConfirmPermanent" ->
                    TicketListener.handleTicketConfirmCancel(event, guild, member, componentId);
            case "ticketClose" -> TicketListener.handleTicketClose(event);
            case "ticketClaim" -> TicketListener.handleTicketClaim(event, guild, member);
            case "dailyConfirm" -> DailyListener.handleDailyConfirm(event, member);
            default -> { /* . */ }
        }
    }
}