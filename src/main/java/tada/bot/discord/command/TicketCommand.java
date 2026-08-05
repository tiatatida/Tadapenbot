package tada.bot.discord.command;

import tada.bot.discord.database.TicketRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.InputStream;
import java.time.Instant;
import java.util.EnumSet;

public class TicketCommand implements Command {
    @Override
    public String getName() {
        return "ticket";
    }

    @Override
    public String getDescription() {
        return "เปิด ticket แจ้งเรื่อง ✨";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {

        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("โปรดระบุคำสั่งเพิ่ม เช่น `/ticket open` หรือ `/ticket add`").setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "open" -> openTicket(event);
            case "add" -> addMember(event);
            default -> event.reply("ไม่รู้จักคำสั่งนี้").setEphemeral(true).queue();
        }
    }

    private void openTicket(SlashCommandInteractionEvent event) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Ticket.jpg");
        if (inputStream == null) {
            event.reply("ไม่พบไฟล์รูปภาพ ticket โปรดติดต่อแอดมิน").setEphemeral(true).queue();
            return;
        }
        FileUpload fileUpload = FileUpload.fromData(inputStream, "Ticket.jpg");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("🚨 โปรดกดยืนยันหรือยกเลิกการเปิด Ticket 🎫");
        embedBuilder.setImage("attachment://Ticket.jpg");
        embedBuilder.setColor(new Color(210, 15, 57));
        embedBuilder.setFooter("Powered by Tada ✨");

        MessageEmbed messageEmbed = embedBuilder.build();

        Button cancelButton = Button.danger("ticketCancel", "ยกเลิก");
        Button confirmButton = Button.success("ticketConfirm", "ยืนยัน");

        event.replyEmbeds(messageEmbed).addFiles(fileUpload)
                .addComponents(ActionRow.of(cancelButton, confirmButton))
                .setEphemeral(true).queue();
    }

    private void addMember(SlashCommandInteractionEvent event) {
        var channel = event.getChannel();
        var guild = event.getGuild();

        if (guild == null || !TicketRepository.isTicketChannel(channel.getIdLong())) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในห้อง ticket เท่านั้นนะครับ").setEphemeral(true).queue();
            return;
        }

        long callerId = event.getUser().getIdLong();
        Member caller = event.getMember();
        boolean isOwner = TicketRepository.isOwner(channel.getIdLong(), callerId);
        boolean isStaff = caller != null &&
                (caller.hasPermission(Permission.MANAGE_CHANNEL) || caller.hasPermission(Permission.ADMINISTRATOR));

        if (!isOwner && !isStaff) {
            event.reply("คุณไม่มีสิทธิ์เพิ่มสมาชิกใน ticket นี้ครับ").setEphemeral(true).queue();
            return;
        }

        Member targetMember = event.getOption("user", OptionMapping::getAsMember);
        if (targetMember == null) {
            event.reply("ไม่พบผู้ใช้ที่ระบุ").setEphemeral(true).queue();
            return;
        }

        TextChannel textChannel = channel.asTextChannel();
        textChannel.upsertPermissionOverride(targetMember)
                .setAllowed(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .queue(
                        success -> {
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setTitle("มีผู้ใช้ถูกเพิ่มเข้ามาใน ticket นี้ ✨");
                            embedBuilder.setColor(new Color(114, 135, 253));
                            embedBuilder.setDescription(targetMember.getAsMention());
                            embedBuilder.setThumbnail(targetMember.getEffectiveAvatarUrl());
                            embedBuilder.setTimestamp(Instant.now());
                            MessageEmbed messageEmbed = embedBuilder.build();
                            textChannel.sendMessageEmbeds(messageEmbed).queue();
                        },
                        failure -> event.reply("เกิดปัญหาในการเพิ่มผู้ใช้").setEphemeral(true).queue()
                );
    }
}