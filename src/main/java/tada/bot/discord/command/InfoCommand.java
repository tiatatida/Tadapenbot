package tada.bot.discord.command;

import tada.bot.discord.database.DailyRepository;
import tada.bot.discord.database.ReviewRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;

public class InfoCommand implements Command {
    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "แสดงข้อมูลผู้ใช้ ✨";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        if (userOption == null) {
            event.reply("เกิดข้อผิดพลาด").setEphemeral(true).queue();
            return;
        }

        User user = userOption.getAsUser();
        long guildId = event.getGuild().getIdLong();
        long userId = user.getIdLong();

        DailyRepository.DailyStatus dailyStatus = DailyRepository.getStatus(userId, guildId);
        ReviewRepository.ReviewStats reviewStats = ReviewRepository.getStats(guildId, userId);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("ข้อมูลของ - " + user.getName());
        embedBuilder.setColor(new Color(234, 118, 203));
        embedBuilder.setThumbnail(user.getEffectiveAvatarUrl());
        embedBuilder.addField("คะแนนรีวิว", ReviewRepository.buildStarText(reviewStats.averageRating(), reviewStats.reviewCount()), false);
        embedBuilder.addField("เช็คอินติดต่อกัน 🔥", dailyStatus.currentStreak() + " วัน", false);
        embedBuilder.addField("สถิติเช็คอินสูงสุด 🏆", dailyStatus.longestStreak() + " วัน", false);

        MessageEmbed messageEmbed = embedBuilder.build();
        event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
    }
}