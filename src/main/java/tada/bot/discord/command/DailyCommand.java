package tada.bot.discord.command;

import tada.bot.discord.database.DailyRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class DailyCommand implements Command {
    @Override
    public String getName() {
        return "daily";
    }

    @Override
    public String getDescription() {
        return "เช็คอินรายวัน ✨";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();

        long userId = event.getUser().getIdLong();
        long guildId = event.getGuild().getIdLong();

        DailyRepository.DailyStatus status = DailyRepository.getStatus(userId, guildId);
        DailyRepository.TopStreak topStreak = DailyRepository.getServerTopStreak(guildId);

        buildAndSendEmbed(event, status, topStreak);
    }

    private void buildAndSendEmbed(SlashCommandInteractionEvent event, DailyRepository.DailyStatus status,
                                   DailyRepository.TopStreak topStreak) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🗓️ เช็คอินรายวัน");
        embedBuilder.setDescription(status.alreadyCheckedInToday()
                ? "คุณเช็คอินวันนี้ไปแล้วครับ ✨ กลับมาใหม่พรุ่งนี้นะ"
                : "ตอนนี้คุณเช็คอินติดต่อกันมาแล้ว " + status.currentStreak() + " วัน 🔥");
        embedBuilder.setColor(new Color(4, 165, 229));

        String topText = "ยังไม่มีข้อมูล";
        if (topStreak != null) {
            var member = event.getGuild().getMemberById(topStreak.userId());
            String name = member != null ? member.getUser().getName() : "Unknown";
            topText = name + " (" + topStreak.longestStreak() + " วัน 🔥)";
        }
        embedBuilder.addField("สถิติสูงสุดของเซิร์ฟเวอร์", topText, false);
        embedBuilder.addField("สถิติสูงสุดของคุณ", String.valueOf(status.longestStreak()), true);
        embedBuilder.setFooter("Powered by Tada ✨");

        MessageEmbed messageEmbed = embedBuilder.build();

        Button confirmButton = Button.primary("dailyConfirm", "เช็คอินเลย").withDisabled(status.alreadyCheckedInToday());

        event.getHook().sendMessageEmbeds(messageEmbed)
                .addComponents(ActionRow.of(confirmButton))
                .queue();
    }
}