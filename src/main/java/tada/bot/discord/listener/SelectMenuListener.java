package tada.bot.discord.listener;

import tada.bot.discord.listener.component.ReportListener;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SelectMenuListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith("reportReason:")) {
            ReportListener.handleReportReason(event);
        }
    }
}