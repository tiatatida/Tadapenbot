package tada.bot.discord.command;

import tada.bot.discord.util.TimeUtil;
import tada.bot.discord.database.GuildSettingsRepository;
import tada.bot.discord.database.ReportRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.time.LocalDateTime;

public class ReportCommand implements Command {
    @Override
    public String getName() {
        return "report";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        Long reportChannelId = GuildSettingsRepository.getReportChannel(event.getGuild().getIdLong());
        if (reportChannelId == null) {
            event.reply("แอดมินยังไม่ได้ตั้งค่าห้องสำหรับรับรายงาน โปรดติดต่อแอดมิน").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        if (userOption == null) {
            event.reply("โปรดระบุผู้ใช้ที่ต้องการรายงานด้วยครับ").setEphemeral(true).queue();
            return;
        }

        User target = userOption.getAsUser();

        if (target.getIdLong() == event.getUser().getIdLong()) {
            event.reply("คุณไม่สามารถรายงานตัวเองได้ครับ 😭").setEphemeral(true).queue();
            return;
        }

        if (target.isBot()) {
            event.reply("ไม่สามารถรายงานบอทได้นะครับ").setEphemeral(true).queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        long reporterId = event.getUser().getIdLong();

        LocalDateTime recentReport = ReportRepository.getRecentReportTime(guildId, reporterId, target.getIdLong());
        if (recentReport != null) {
            String remaining = TimeUtil.formatRemaining(recentReport);
            event.reply("คุณรายงาน " + target.getName() + " ไปแล้วภายใน 24 ชม.ที่ผ่านมาครับ\n" +
                            "⏳ โปรดรออีก " + remaining + " ก่อนรายงานซ้ำ")
                    .setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🚨 รายงานผู้ใช้ " + target.getName());
        embedBuilder.setDescription("โปรดเลือกหัวข้อในการรายงานจากเมนูด้านล่างนี้ครับ");
        embedBuilder.setColor(new Color(210, 15, 57));
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());

        MessageEmbed messageEmbed = embedBuilder.build();

        StringSelectMenu topicMenu = StringSelectMenu.create("reportTopic:" + target.getIdLong())
                .setPlaceholder("เลือกหัวข้อในการรายงาน")
                .addOption("สแปม / โฆษณา", "spam", "ส่งข้อความซ้ำๆ หรือโฆษณาที่ไม่เกี่ยวข้อง")
                .addOption("คำพูดหยาบคาย / สร้างความไม่สบายใจ", "harassment", "ใช้ถ้อยคำหยาบคายหรือสร้างความไม่สบายใจต่อผู้อื่น")
                .addOption("เนื้อหาไม่เหมาะสม", "inappropriate", "ส่งภาพหรือข้อความที่ไม่เหมาะสม")
                .addOption("แอบอ้าง / หลอกลวง", "scam", "แอบอ้างตัวตนหรือพยายามหลอกลวง")
                .addOption("อื่นๆ", "other", "เหตุผลอื่นๆ นอกเหนือจากที่ระบุ")
                .build();

        event.replyEmbeds(messageEmbed)
                .addComponents(ActionRow.of(topicMenu))
                .setEphemeral(true).queue();
    }

}