package tada.bot.discord.listener.component;

import tada.bot.discord.database.GuildSettingsRepository;
import tada.bot.discord.database.ReportRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

public class ReportListener {

    private static final Map<String, String> REASON_LABELS = Map.of(
            "spam", "สแปม / โฆษณา",
            "harassment", "คำพูดหยาบคาย / ก้าวร้าว",
            "inappropriate", "เนื้อหาไม่เหมาะสม",
            "scam", "แอบอ้าง / หลอกลวง",
            "other", "อื่นๆ"
    );

    public static void handleReportReason(StringSelectInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null) return;

        String[] parts = event.getComponentId().split(":");
        if (parts.length != 2) {
            event.reply("เกิดข้อผิดพลาด โปรดลองใหม่อีกครั้งครับ").setEphemeral(true).queue();
            return;
        }

        long reportedId;
        try {
            reportedId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            event.reply("เกิดข้อผิดพลาด โปรดลองใหม่อีกครั้งครับ").setEphemeral(true).queue();
            return;
        }

        long guildId = guild.getIdLong();
        long reporterId = event.getUser().getIdLong();

        // เช็คซ้ำอีกครั้ง กันกรณีเปิดเมนูค้างไว้นาน หรือกดพร้อมกันหลายอัน (race condition)
        LocalDateTime recentReport = ReportRepository.getRecentReportTime(guildId, reporterId, reportedId);
        if (recentReport != null) {
            String remaining = formatRemaining(recentReport);
            event.reply("คุณรายงานผู้ใช้นี้ไปแล้วภายใน 24 ชม.ที่ผ่านมาครับ\nโปรดรออีก " + remaining + " ก่อนรายงานซ้ำ")
                    .setEphemeral(true).queue();
            return;
        }

        String reasonKey = event.getValues().get(0);
        String reasonLabel = REASON_LABELS.getOrDefault(reasonKey, reasonKey);

        ReportRepository.insertReport(guildId, reporterId, reportedId, reasonLabel);

        event.reply("ส่งรายงานเรียบร้อยครับ ✨").setEphemeral(true).queue();

        Long reportChannelId = GuildSettingsRepository.getReportChannel(guildId);
        if (reportChannelId == null) return;

        TextChannel channel = guild.getChannelById(TextChannel.class, reportChannelId);
        if (channel == null) return;

        guild.retrieveMemberById(reportedId).queue(
                reportedMember -> sendReportEmbed(channel, event, reportedMember.getUser().getName(), reportedId, reasonLabel),
                failure -> sendReportEmbed(channel, event, "Unknown User", reportedId, reasonLabel)
        );
    }

    private static void sendReportEmbed(TextChannel channel, StringSelectInteractionEvent event,
                                        String reportedName, long reportedId, String reasonLabel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🚨 รายงานใหม่");
        embedBuilder.addField("ผู้ถูกรายงาน", reportedName + " (<@" + reportedId + ">)", false);
        embedBuilder.addField("รายงานโดย", event.getUser().getAsMention(), true);
        embedBuilder.addField("เหตุผล", reasonLabel, true);
        embedBuilder.setColor(new Color(210, 15, 57));
        embedBuilder.setTimestamp(Instant.now());

        MessageEmbed messageEmbed = embedBuilder.build();
        channel.sendMessageEmbeds(messageEmbed).queue();
    }

    private static String formatRemaining(LocalDateTime lastReportTime) {
        Duration remaining = Duration.between(LocalDateTime.now(), lastReportTime.plusHours(24));
        if (remaining.isNegative()) return "ไม่กี่นาที";

        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        if (hours > 0) {
            return hours + " ชม. " + minutes + " นาที";
        }
        return minutes + " นาที";
    }
}