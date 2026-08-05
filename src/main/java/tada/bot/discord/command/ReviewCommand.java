package tada.bot.discord.command;

import tada.bot.discord.database.ReviewRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class ReviewCommand implements Command {
    @Override
    public String getName() {
        return "review";
    }

    @Override
    public String getDescription() {
        return "ให้คะแนนรีวิวผู้ใช้ ✨";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        OptionMapping ratingOption = event.getOption("rating");

        if (userOption == null || ratingOption == null) {
            event.reply("โปรดระบุผู้ใช้และคะแนนด้วยครับ").setEphemeral(true).queue();
            return;
        }

        User target = userOption.getAsUser();
        long rating = ratingOption.getAsLong();

        if (rating < 1 || rating > 5) {
            event.reply("คะแนนต้องอยู่ระหว่าง 1-5 ครับ").setEphemeral(true).queue();
            return;
        }

        if (target.getIdLong() == event.getUser().getIdLong()) {
            event.reply("คุณไม่สามารถรีวิวตัวเองได้ครับ 😭").setEphemeral(true).queue();
            return;
        }

        if (target.isBot()) {
            event.reply("ไม่สามารถรีวิวบอทได้นะครับ").setEphemeral(true).queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        long reviewerId = event.getUser().getIdLong();
        long reviewedId = target.getIdLong();

        LocalDateTime recentReview = ReviewRepository.getRecentReviewTime(guildId, reviewerId, reviewedId);
        if (recentReview != null) {
            String remaining = formatRemaining(recentReview);
            event.reply("คุณรีวิว " + target.getName() + " ไปแล้วภายใน 24 ชม.ที่ผ่านมาครับ\n" +
                            "⏳ โปรดรออีก " + remaining + " ก่อนแก้ไขคะแนนอีกครั้ง")
                    .setEphemeral(true).queue();
            return;
        }

        ReviewRepository.upsertReview(guildId, reviewerId, reviewedId, (int) rating);

        ReviewRepository.ReviewStats stats = ReviewRepository.getStats(guildId, reviewedId);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("⭐ รีวิวสำเร็จ");
        embedBuilder.setDescription("คุณให้คะแนน " + target.getAsMention() + " " + rating + "/5 ⭐");
        embedBuilder.addField("คะแนนเฉลี่ยปัจจุบัน", ReviewRepository.buildStarText(stats.averageRating(), stats.reviewCount()), false);
        embedBuilder.setColor(new Color(250, 179, 135));
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());

        MessageEmbed messageEmbed = embedBuilder.build();
        event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
    }

    private String formatRemaining(LocalDateTime lastReviewTime) {
        Duration remaining = Duration.between(LocalDateTime.now(), lastReviewTime.plusHours(24));
        if (remaining.isNegative()) return "ไม่กี่นาที";

        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        if (hours > 0) {
            return hours + " ชม. " + minutes + " นาที";
        }
        return minutes + " นาที";
    }
}