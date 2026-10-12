package tada.bot.discord.listener.component;

import tada.bot.discord.util.TimeUtil;
import tada.bot.discord.database.GuildSettingsRepository;
import tada.bot.discord.database.ReportRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

public class ReportListener {

    private static final Map<String, String> TOPIC_LABELS = Map.of(
            "spam", "สแปม / โฆษณา",
            "harassment", "คำพูดหยาบคาย / ก้าวร้าว",
            "inappropriate", "เนื้อหาไม่เหมาะสม",
            "scam", "แอบอ้าง / หลอกลวง",
            "other", "อื่นๆ"
    );

    public static void handleReportTopic(StringSelectInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length != 2) {
            event.reply("เกิดข้อผิดพลาด โปรดลองใหม่อีกครั้งครับ").setEphemeral(true).queue();
            return;
        }

        String topicKey = event.getValues().get(0);
        String topicLabel = TOPIC_LABELS.getOrDefault(topicKey, topicKey);

        TextInput reasonInput = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("อธิบายเหตุผลในการรายงาน")
                .setRequired(true)
                .setMaxLength(255)
                .build();

        Modal modal = Modal.create("reportModal:" + parts[1] + ":" + topicKey, "รายงาน: " + topicLabel)
                .addComponents(Label.of("เหตุผล", reasonInput))
                .build();

        event.replyModal(modal).queue();
    }

    public static void handleReportModal(ModalInteractionEvent event, Guild guild) {
        String[] parts = event.getModalId().split(":");
        if (parts.length != 3) {
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

        ModalMapping mapping = event.getValue("reason");
        String reason = mapping != null ? mapping.getAsString().trim() : "";
        if (reason.isEmpty()) {
            event.reply("โปรดกรอกเหตุผลในการรายงานครับ").setEphemeral(true).queue();
            return;
        }

        long guildId = guild.getIdLong();
        long reporterId = event.getUser().getIdLong();

        String topicLabel = TOPIC_LABELS.getOrDefault(parts[2], parts[2]);
        LocalDateTime recentReport;
        boolean saved;
        synchronized (ReportListener.class) {
            recentReport = ReportRepository.getRecentReportTime(guildId, reporterId, reportedId);
            saved = recentReport == null
                    && ReportRepository.insertReport(guildId, reporterId, reportedId, topicLabel, reason);
        }
        if (recentReport != null) {
            String remaining = TimeUtil.formatRemaining(recentReport);
            event.reply("คุณรายงานผู้ใช้นี้ไปแล้วภายใน 24 ชม.ที่ผ่านมาครับ\nโปรดรออีก " + remaining + " ก่อนรายงานซ้ำ")
                    .setEphemeral(true).queue();
            return;
        }

        if (!saved) {
            event.reply("บันทึกรายงานไม่สำเร็จ โปรดลองใหม่อีกครั้งครับ").setEphemeral(true).queue();
            return;
        }

        event.reply("ส่งรายงานเรียบร้อยครับ ✨").setEphemeral(true).queue();

        Long reportChannelId = GuildSettingsRepository.getReportChannel(guildId);
        if (reportChannelId == null) return;

        TextChannel channel = guild.getChannelById(TextChannel.class, reportChannelId);
        if (channel == null) return;

        guild.retrieveMemberById(reportedId).queue(
                reportedMember -> sendReportEmbed(channel, event, reportedMember.getUser().getName(), reportedId, topicLabel, reason),
                failure -> sendReportEmbed(channel, event, "Unknown User", reportedId, topicLabel, reason)
        );
    }

    private static void sendReportEmbed(TextChannel channel, ModalInteractionEvent event,
                                        String reportedName, long reportedId, String topicLabel, String reason) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🚨 รายงานใหม่");
        embedBuilder.addField("ผู้ถูกรายงาน", reportedName + " (<@" + reportedId + ">)", false);
        embedBuilder.addField("รายงานโดย", event.getUser().getAsMention(), true);
        embedBuilder.addField("หัวข้อ", topicLabel, true);
        embedBuilder.addField("เหตุผล", reason, false);
        embedBuilder.setColor(new Color(210, 15, 57));
        embedBuilder.setTimestamp(Instant.now());

        MessageEmbed messageEmbed = embedBuilder.build();
        channel.sendMessageEmbeds(messageEmbed).queue();
    }
}
