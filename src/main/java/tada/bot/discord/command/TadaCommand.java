package tada.bot.discord.command;

import tada.bot.discord.database.GuildSettingsRepository;
import tada.bot.discord.database.ReportRepository;
import tada.bot.discord.database.VoiceRepository;
import tada.bot.discord.database.WarnRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
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
        } else if ("setup".equals(group) && "voice".equals(subcommand)) {
            setupVoice(event);
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
        if (guild == null)
            return;

        OptionMapping channelOption = event.getOption("channel");
        TextChannel targetChannel;
        if (channelOption != null) {
            targetChannel = channelOption.getAsChannel().asTextChannel();
        } else if (event.getChannelType() == ChannelType.TEXT) {
            targetChannel = event.getChannel().asTextChannel();
        } else {
            event.reply("โปรดใช้คำสั่งนี้ในห้องข้อความ หรือเลือกห้องข้อความครับ").setEphemeral(true).queue();
            return;
        }

        if (!GuildSettingsRepository.setReportChannel(guild.getIdLong(), targetChannel.getIdLong())) {
            event.reply("บันทึกการตั้งค่าไม่สำเร็จ โปรดลองใหม่อีกครั้งครับ").setEphemeral(true).queue();
            return;
        }

        event.reply("ตั้งค่าห้องรับรายงานเป็น " + targetChannel.getAsMention() + " เรียบร้อยครับ ✨")
                .setEphemeral(true).queue();
    }

    private void setupTicket(SlashCommandInteractionEvent event) {
        if (event.getChannelType() != ChannelType.TEXT) {
            event.reply("โปรดใช้คำสั่งนี้ในห้องข้อความเท่านั้นครับ").setEphemeral(true).queue();
            return;
        }
        var channel = event.getChannel().asTextChannel();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Ticket.gif");
        if (inputStream == null) {
            event.reply("ไม่พบไฟล์รูปภาพ ticket โปรดติดต่อแอดมิน").setEphemeral(true).queue();
            return;
        }
        FileUpload fileUpload = FileUpload.fromData(inputStream, "Ticket.gif");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🚨 กดปุ่มด้านล่างเพื่อเปิด Ticket 🎫");
        embedBuilder.setImage("attachment://Ticket.gif");
        embedBuilder.setColor(new Color(210, 15, 57));
        embedBuilder.setFooter("Powered by Tada ✨");

        MessageEmbed messageEmbed = embedBuilder.build();

        Button confirmButton = Button.success("ticketConfirmPermanent", "กดเปิด Ticket ✨");

        event.reply("ตั้งค่าห้อง ticket เรียบร้อยครับ ✨").setEphemeral(true).queue();

        channel.sendMessageEmbeds(messageEmbed).addFiles(fileUpload)
                .addComponents(ActionRow.of(confirmButton))
                .queue();
    }

    private void setupVoice(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null)
            return;

        OptionMapping channelOption = event.getOption("channel");
        if (channelOption == null) {
            event.reply("โปรดระบุห้องเสียงที่ต้องการตั้งเป็นจุดสร้างห้องด้วยครับ").setEphemeral(true).queue();
            return;
        }

        var channel = channelOption.getAsChannel();
        if (channel.getType() != ChannelType.VOICE) {
            event.reply("โปรดเลือกห้องเสียง (Voice Channel) เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        if (!VoiceRepository.setHubChannel(guild.getIdLong(), channel.getIdLong())) {
            event.reply("บันทึกการตั้งค่าไม่สำเร็จ โปรดลองใหม่อีกครั้งครับ").setEphemeral(true).queue();
            return;
        }

        event.reply("ตั้งค่าห้อง " + channel.getAsMention()
                + " เป็นห้องสร้างห้องเสียงอัตโนมัติเรียบร้อยครับ ✨")
                .setEphemeral(true).queue();
    }

    private void warnUser(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null)
            return;

        OptionMapping colorOption = event.getOption("color");
        OptionMapping userOption = event.getOption("user");
        OptionMapping reasonOption = event.getOption("reason");

        if (colorOption == null || userOption == null || reasonOption == null) {
            event.reply("โปรดระบุให้ครบด้วยครับ").setEphemeral(true).queue();
            return;
        }

        String color = colorOption.getAsString();
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

        event.deferReply(true).queue();
        var hook = event.getHook();

        if (!WarnRepository.insertWarn(guild.getIdLong(), targetUser.getIdLong(), moderatorId, reason, color)) {
            hook.editOriginal("⚠️ บันทึกใบเตือนไม่สำเร็จ โปรดลองใหม่อีกครั้งครับ").queue();
            return;
        }

        String colorLabel = switch (color) {
            case WarnRepository.YELLOW -> "🟡 เหลือง";
            case WarnRepository.ORANGE -> "🟠 ส้ม";
            default -> "🔴 แดง";
        };

        if (WarnRepository.RED.equals(color)) {
            dmThen(targetUser, "คุณได้รับใบเตือนสีแดงในเซิร์ฟเวอร์ " + guild.getName() +
                    "\nเหตุผล: " + reason + "\nใบเตือนสีแดงมีผลแบนทันทีครับ",
                    () -> guild.ban(targetUser, 0, TimeUnit.SECONDS)
                            .reason("ได้รับใบเตือนสีแดง (Auto-ban)")
                            .queue(
                                    success -> hook.editOriginal("🔴 " + targetUser.getAsMention()
                                            + " ได้รับใบเตือนสีแดงและถูกแบนอัตโนมัติทันทีครับ\nเหตุผล: " + reason).queue(),
                                    failure -> hook.editOriginal("⚠️ ให้ใบเตือนสำเร็จ แต่ไม่สามารถแบนผู้ใช้ได้ 😭").queue()));
            return;
        }

        int activeCount = WarnRepository.countActiveYellowOrange(guild.getIdLong(), targetUser.getIdLong());
        String dmText = "คุณได้รับใบเตือน " + colorLabel + " ในเซิร์ฟเวอร์ " + guild.getName() +
                "\nตอนนี้คุณมี " + activeCount + "/2\nเหตุผล: " + reason;

        if (activeCount >= 2) {
            dmThen(targetUser, dmText, () -> guild.ban(targetUser, 0, TimeUnit.SECONDS)
                    .reason("ได้รับใบเตือนสะสมครบ 2 ใบ (Auto-ban)")
                    .queue(
                            success -> hook.editOriginal(colorLabel + " " + targetUser.getAsMention()
                                    + " มีใบเตือนสะสม " + activeCount
                                    + "/2 ใบ จึงถูกแบนอัตโนมัติครับ\nเหตุผลล่าสุด: " + reason).queue(),
                            failure -> hook.editOriginal("⚠️ ให้ใบเตือนสำเร็จ แต่ไม่สามารถแบนผู้ใช้ได้ 😭").queue()));
        } else {
            dmThen(targetUser, dmText, () -> guild.timeoutFor(targetUser, 7, TimeUnit.DAYS)
                    .reason("ได้รับใบเตือน (Auto-timeout)")
                    .queue(
                            success -> hook.editOriginal(colorLabel + " " + targetUser.getAsMention()
                                    + " ได้รับใบเตือน (สะสมที่ยังไม่หมดอายุ " + activeCount
                                    + "/2 ใบ) และถูก Time out 7 วันอัตโนมัติครับ\nเหตุผล: " + reason).queue(),
                            failure -> hook.editOriginal("⚠️ ให้ใบเตือนสำเร็จ แต่ไม่สามารถ Time out ผู้ใช้ได้ 😭").queue()));
        }
    }

    private void dmThen(User user, String text, Runnable then) {
        user.openPrivateChannel().queue(
                dm -> dm.sendMessage(text).queue(s -> then.run(), f -> then.run()),
                f -> then.run());
    }

    private void showHistory(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null)
            return;

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

        String warnText = warns.isEmpty() ? "ไม่มีประวัติ"
                : warns.stream()
                        .map(w -> {
                            String colorEmoji = switch (w.color()) {
                                case WarnRepository.YELLOW -> "🟡";
                                case WarnRepository.ORANGE -> "🟠";
                                default -> "🔴";
                            };
                            String expiryText;
                            if (w.expiresAt() == null) {
                                expiryText = "";
                            } else if (w.expiresAt().isBefore(java.time.LocalDateTime.now())) {
                                expiryText = " (หมดอายุแล้ว)";
                            } else {
                                expiryText = " (หมดอายุ " + w.expiresAt().format(DATE_FORMAT) + ")";
                            }
                            return "• " + colorEmoji + " " + w.createdAt().format(DATE_FORMAT) + " — " + w.reason()
                                    + expiryText + " (โดย <@" + w.moderatorId() + ">)";
                        })
                        .collect(Collectors.joining("\n"));
        int activeCount = WarnRepository.countActiveYellowOrange(guildId, userId);
        embedBuilder.addField(
                "⚠️ ใบเตือน (ที่ยังไม่หมดอายุ " + activeCount + "/2 · ทั้งหมด "
                        + WarnRepository.countWarnings(guildId, userId) + " ครั้ง)",
                truncate(warnText), false);

        String receivedText = received.isEmpty() ? "ไม่มีประวัติ"
                : received.stream()
                        .map(r -> "• " + r.createdAt().format(DATE_FORMAT) + " — " + r.reason() + " (โดย <@"
                                + r.reporterId() + ">)")
                        .collect(Collectors.joining("\n"));
        embedBuilder.addField(
                "🚨 ถูกรายงาน (ทั้งหมด " + ReportRepository.countReceivedReports(guildId, userId) + " ครั้ง)",
                truncate(receivedText), false);

        String givenText = given.isEmpty() ? "ไม่มีประวัติ"
                : given.stream()
                        .map(r -> "• " + r.createdAt().format(DATE_FORMAT) + " — " + r.reason() + " (รายงาน <@"
                                + r.reportedId() + ">)")
                        .collect(Collectors.joining("\n"));
        embedBuilder.addField(
                "📤 รายงานผู้อื่น (ทั้งหมด " + ReportRepository.countGivenReports(guildId, userId) + " ครั้ง)",
                truncate(givenText), false);

        embedBuilder.setFooter("แสดงล่าสุดสูงสุด 10 รายการต่อประเภท");

        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
    }

    private static final String TRUNCATE_NOTE = "\n...(ดูเพิ่มเติมได้จาก database)";

    private String truncate(String text) {
        if (text.length() <= 1024) {
            return text;
        }
        int limit = 1024 - TRUNCATE_NOTE.length();
        if (Character.isHighSurrogate(text.charAt(limit - 1))) {
            limit--;
        }
        return text.substring(0, limit) + TRUNCATE_NOTE;
    }
}