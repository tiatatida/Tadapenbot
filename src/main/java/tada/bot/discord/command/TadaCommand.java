package tada.bot.discord.command;

import tada.bot.discord.database.GuildSettingsRepository;
import tada.bot.discord.database.ReportRepository;
import tada.bot.discord.database.WarnRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TadaCommand implements Command {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    @Override
    public String getName() {
        return "tada";
    }

    @Override
    public String getDescription() {
        return "คำสั่งจัดการระบบสำหรับแอดมิน ✨";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {

        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะแอดมินเท่านั้นครับคุณพรี่!").setEphemeral(true).queue();
            return;
        }

        String group = event.getSubcommandGroup();
        String subcommand = event.getSubcommandName();

        if ("setup".equals(group) && "report".equals(subcommand)) {
            setupReport(event);
        } else if ("setup".equals(group) && "ticket".equals(subcommand)) {
            setupTicket(event);
        } else if ("warn".equals(subcommand)) {
            warnUser(event);
        } else if ("history".equals(subcommand)) {
            showHistory(event);
        } else {
            event.reply("ไม่รู้จักคำสั่งนี้").setEphemeral(true).queue();
        }
    }

    private void setupReport(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        OptionMapping channelOption = event.getOption("channel");
        TextChannel targetChannel = channelOption != null
                ? channelOption.getAsChannel().asTextChannel()
                : event.getChannel().asTextChannel();

        GuildSettingsRepository.setReportChannel(guild.getIdLong(), targetChannel.getIdLong());

        event.reply("ตั้งค่าห้องรับรายงานเป็น " + targetChannel.getAsMention() + " เรียบร้อยครับ ✨")
                .setEphemeral(true).queue();
    }

    private void setupTicket(SlashCommandInteractionEvent event) {
        var channel = event.getChannel().asTextChannel();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Ticket.jpg");
        if (inputStream == null) {
            event.reply("ไม่พบไฟล์รูปภาพ ticket โปรดติดต่อแอดมิน").setEphemeral(true).queue();
            return;
        }
        FileUpload fileUpload = FileUpload.fromData(inputStream, "Ticket.jpg");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🚨 กดปุ่มด้านล่างเพื่อเปิด Ticket 🎫");
        embedBuilder.setImage("attachment://Ticket.jpg");
        embedBuilder.setColor(new Color(210, 15, 57));
        embedBuilder.setFooter("Powered by Tada ✨");

        MessageEmbed messageEmbed = embedBuilder.build();

        Button confirmButton = Button.success("ticketConfirmPermanent", "กดเปิด Ticket ✨");

        event.reply("ตั้งค่าห้อง ticket เรียบร้อยครับ ✨").setEphemeral(true).queue();

        channel.sendMessageEmbeds(messageEmbed).addFiles(fileUpload)
                .addComponents(ActionRow.of(confirmButton))
                .queue();
    }

    private void warnUser(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        OptionMapping userOption = event.getOption("user");
        OptionMapping reasonOption = event.getOption("reason");

        if (userOption == null || reasonOption == null) {
            event.reply("โปรดระบุผู้ใช้และเหตุผลด้วยครับ").setEphemeral(true).queue();
            return;
        }

        User targetUser = userOption.getAsUser();
        String reason = reasonOption.getAsString();
        long moderatorId = event.getUser().getIdLong();

        if (targetUser.getIdLong() == event.getUser().getIdLong()) {
            event.reply("คุณไม่สามารถเตือนตัวเองได้ครับ 555+").setEphemeral(true).queue();
            return;
        }

        if (targetUser.isBot()) {
            event.reply("ไม่สามารถเตือนบอทได้ครับ 😭").setEphemeral(true).queue();
            return;
        }

        WarnRepository.insertWarn(guild.getIdLong(), targetUser.getIdLong(), moderatorId, reason);
        int warnCount = WarnRepository.countWarnings(guild.getIdLong(), targetUser.getIdLong());

        targetUser.openPrivateChannel().queue(
                dm -> dm.sendMessage("คุณได้รับใบเตือนในเซิร์ฟเวอร์ " + guild.getName() +
                        " ครั้งที่ " + warnCount + "/2\nเหตุผล: " + reason).queue(s -> {}, f -> {}),
                f -> { /* DM ปิดอยู่ */ }
        );

        if (warnCount >= 2) {
            guild.ban(targetUser, 0, TimeUnit.SECONDS)
                    .reason("ได้รับใบเตือนครบ 2 ครั้ง (Auto-ban)")
                    .queue(
                            success -> event.reply("⚠️ " + targetUser.getAsMention() + " ได้รับใบเตือนครั้งที่ " + warnCount +
                                            " และถูกแบนอัตโนมัติเนื่องจากได้รับใบเตือนครบ 2 ครั้งครับ\nเหตุผลล่าสุด " + reason)
                                    .setEphemeral(true).queue(),
                            failure -> event.reply("⚠️ ให้ใบเตือนสำเร็จ แต่ไม่สามารถแบนผู้ใช้ได้ 😭")
                                    .setEphemeral(true).queue()
                    );
        } else {
            guild.timeoutFor(targetUser, 7, TimeUnit.DAYS)
                    .reason("ได้รับใบเตือน 1 ครั้ง (Auto-timeout)")
                    .queue(
                            success -> event.reply("⚠️ " + targetUser.getAsMention() + " ได้รับใบเตือนครั้งที่ " + warnCount +
                                            " และถูก Time out อัตโนมัติเนื่องจากได้รับใบเตือนครับ\nเหตุผล " + reason)
                                    .setEphemeral(true).queue(),
                            failure -> event.reply("⚠️ ให้ใบเตือนสำเร็จ แต่ไม่สามารถ Time out ผู้ใช้ได้ 😭")
                                    .setEphemeral(true).queue()
                    );
        }
    }

    private void showHistory(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        OptionMapping userOption = event.getOption("user");
        if (userOption == null) {
            event.reply("โปรดระบุผู้ใช้ที่ต้องการดูประวัติครับ").setEphemeral(true).queue();
            return;
        }

        User target = userOption.getAsUser();
        long guildId = guild.getIdLong();
        long userId = target.getIdLong();

        List<WarnRepository.WarnEntry> warns = WarnRepository.getWarnings(guildId, userId, 10);
        List<ReportRepository.ReportEntry> received = ReportRepository.getReceivedReports(guildId, userId, 10);
        List<ReportRepository.ReportEntry> given = ReportRepository.getGivenReports(guildId, userId, 10);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("📋 ประวัติของ " + target.getName());
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());
        embedBuilder.setColor(new Color(88, 101, 242));

        String warnText = warns.isEmpty() ? "ไม่มีประวัติ" : warns.stream()
                .map(w -> "• " + w.createdAt().format(DATE_FORMAT) + " — " + w.reason() + " (โดย <@" + w.moderatorId() + ">)")
                .collect(Collectors.joining("\n"));
        embedBuilder.addField("⚠️ ใบเตือน (ทั้งหมด " + WarnRepository.countWarnings(guildId, userId) + " ครั้ง)",
                truncate(warnText), false);

        String receivedText = received.isEmpty() ? "ไม่มีประวัติ" : received.stream()
                .map(r -> "• " + r.createdAt().format(DATE_FORMAT) + " — " + r.reason() + " (โดย <@" + r.reporterId() + ">)")
                .collect(Collectors.joining("\n"));
        embedBuilder.addField("🚨 ถูกรายงาน (ทั้งหมด " + ReportRepository.countReceivedReports(guildId, userId) + " ครั้ง)",
                truncate(receivedText), false);

        String givenText = given.isEmpty() ? "ไม่มีประวัติ" : given.stream()
                .map(r -> "• " + r.createdAt().format(DATE_FORMAT) + " — " + r.reason() + " (รายงาน <@" + r.reportedId() + ">)")
                .collect(Collectors.joining("\n"));
        embedBuilder.addField("📤 รายงานผู้อื่น (ทั้งหมด " + ReportRepository.countGivenReports(guildId, userId) + " ครั้ง)",
                truncate(givenText), false);

        embedBuilder.setFooter("แสดงล่าสุดสูงสุด 10 รายการต่อประเภท");

        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
    }

    private String truncate(String text) {
        if (text.length() > 1000) {
            return text.substring(0, 1000) + "\n...(ดูเพิ่มเติมได้จาก database)";
        }
        return text;
    }
}