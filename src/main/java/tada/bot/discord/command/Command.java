package tada.bot.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface Command {
    void executeSlash(SlashCommandInteractionEvent event);
}
