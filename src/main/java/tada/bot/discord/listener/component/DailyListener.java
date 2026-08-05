package tada.bot.discord.listener.component;

import tada.bot.discord.database.DailyRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class DailyListener {

    public static void handleDailyConfirm(ButtonInteractionEvent event, Member member) {
        long userId = member.getIdLong();
        long guildId = event.getGuild().getIdLong();

        DailyRepository.DailyStatus result = DailyRepository.checkIn(userId, guildId);

        if (result == null) {
            event.reply("คุณเช็คอินวันนี้ไปแล้วครับ").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        var originalEmbed = event.getMessage().getEmbeds().get(0);
        EmbedBuilder updatedEmbed = new EmbedBuilder(originalEmbed)
                .setDescription("เช็คอินสำเร็จ! ตอนนี้คุณเช็คอินติดต่อกันมาแล้ว " + result.currentStreak() + " วัน")
                .clearFields()
                .addField("สถิติสูงสุดของเซิร์ฟเวอร์", originalEmbed.getFields().get(0).getValue(), false)
                .addField("สถิติสูงสุดของคุณ", String.valueOf(result.longestStreak()), true);

        Button disabledButton = Button.primary("dailyConfirm", "เช็คอินแล้ว").asDisabled();

        event.getHook().editOriginalEmbeds(updatedEmbed.build())
                .setComponents(ActionRow.of(disabledButton))
                .queue();
    }
}