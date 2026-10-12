package tada.bot.discord.listener.component;

import tada.bot.discord.database.TicketRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.Color;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class TicketListener {

    public static void handleTicketConfirmCancel(ButtonInteractionEvent event, Guild guild, Member member, String componentId) {

        boolean isPermanent = componentId.equals("ticketConfirmPermanent");

        if (isPermanent) {
            event.deferReply(true).queue();
            createTicketChannel(event, guild, member);
            return;
        }

        List<Button> disabledButtons = event.getMessage().getComponentTree()
                .findAll(Button.class).stream()
                .map(Button::asDisabled)
                .collect(Collectors.toList());
        event.deferEdit().queue();
        event.getHook().editOriginalComponents(ActionRow.of(disabledButtons)).queue();

        if (componentId.equals("ticketConfirm")) {
            createTicketChannel(event, guild, member);
        } else {
            event.getHook().sendMessage("ยกเลิกการเปิด ticket เรียบร้อยครับ ✨")
                    .setEphemeral(true).queue();
        }
    }

    private static void createTicketChannel(ButtonInteractionEvent event, Guild guild, Member member) {
        Long existingChannelId = TicketRepository.getOpenTicketChannelId(guild.getIdLong(), member.getIdLong());
        if (existingChannelId != null) {
            TextChannel existingChannel = guild.getTextChannelById(existingChannelId);
            if (existingChannel != null) {
                event.getHook().sendMessage("คุณมี ticket ที่ยังเปิดอยู่แล้วครับ ✨ " + existingChannel.getAsMention())
                        .setEphemeral(true).queue();
                return;
            }
            TicketRepository.closeTicket(existingChannelId);
        }

        String channelName = "ticket-" + member.getUser().getName() + "🚨";
        guild.createTextChannel(channelName)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    TicketRepository.insertTicket(channel.getIdLong(), guild.getIdLong(), member.getIdLong());
                    event.getHook().sendMessage("เปิด ticket เรียบร้อยครับ ✨ " + channel.getAsMention())
                            .setEphemeral(true).queue();
                    sendTicketWelcomeEmbed(channel, member);
                }, throwable -> {
                    event.getHook().sendMessage("เกิดปัญหาในการเปิด ticket โปรดติดต่อแอดมิน")
                            .setEphemeral(true).queue();
                });
    }

    private static void sendTicketWelcomeEmbed(TextChannel channel, Member member) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎫 Ticket")
                .setDescription("ระบุเคสที่จะแจ้งไว้แล้วแอดมินจะมารับเคสให้ครับ")
                .addField("เปิดโดย", member.getUser().getAsMention(), true)
                .addField("สถานะ", "ยังไม่มีคนรับเคส", true)
                .setColor(new Color(92, 95, 119))
                .setThumbnail(member.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build())
                .addComponents(ticketControls(false))
                .queue();
    }

    private static ActionRow ticketControls(boolean claimed) {
        return ActionRow.of(
                Button.success("ticketClaim", claimed ? "รับเคสแล้ว" : "รับเคส").withDisabled(claimed),
                Button.secondary("ticketUnclaim", "ยกเลิกรับเคส").withDisabled(!claimed),
                Button.danger("ticketClose", "ปิด Ticket"));
    }

    private static boolean isStaff(Member member) {
        return member.hasPermission(Permission.MANAGE_CHANNEL) || member.hasPermission(Permission.ADMINISTRATOR);
    }

    public static void handleTicketClaim(ButtonInteractionEvent event, Guild guild, Member staff) {
        if (!isStaff(staff)) {
            event.reply("กดได้แค่เฉพาะแอดมินเท่านั้นนะครับ").setEphemeral(true).queue();
            return;
        }

        TicketRepository.setClaimed(event.getChannel().getIdLong(), staff.getIdLong());
        event.deferEdit().queue();
        updateTicketMessage(event, staff);

        var channel = event.getChannel().asTextChannel();
        renameChannel(channel, "💬");

        EmbedBuilder claimNoticeEmbed = new EmbedBuilder();
        claimNoticeEmbed.setTitle("แอดมินมารับเคสแล้วครับ ✨");
        claimNoticeEmbed.setDescription(staff.getAsMention());
        claimNoticeEmbed.setColor(new Color(136, 57, 239));
        var thumbnail = event.getMessage().getEmbeds().get(0).getThumbnail();
        if (thumbnail != null) {
            claimNoticeEmbed.setThumbnail(thumbnail.getUrl());
        }
        claimNoticeEmbed.setTimestamp(Instant.now());

        channel.sendMessageEmbeds(claimNoticeEmbed.build()).queue();
    }

    public static void handleTicketUnclaim(ButtonInteractionEvent event, Guild guild, Member staff) {
        if (!isStaff(staff)) {
            event.reply("กดได้แค่เฉพาะแอดมินเท่านั้นนะครับ").setEphemeral(true).queue();
            return;
        }

        TicketRepository.setUnclaimed(event.getChannel().getIdLong());
        event.deferEdit().queue();
        updateTicketMessage(event, null);
        renameChannel(event.getChannel().asTextChannel(), "🚨");
    }

    private static void updateTicketMessage(ButtonInteractionEvent event, Member staff) {
        boolean claimed = staff != null;
        var originalEmbed = event.getMessage().getEmbeds().get(0);
        EmbedBuilder updatedEmbed = new EmbedBuilder(originalEmbed)
                .clearFields()
                .addField("เปิดโดย", originalEmbed.getFields().get(0).getValue(), true)
                .addField("สถานะ", claimed ? "รับเคสโดย " + staff.getAsMention() : "ยังไม่มีคนรับเคส", true)
                .setColor(claimed ? new Color(64, 160, 43) : new Color(92, 95, 119));

        event.getHook().editOriginalEmbeds(updatedEmbed.build())
                .setComponents(ticketControls(claimed))
                .queue();
    }

    private static void renameChannel(TextChannel channel, String statusSuffix) {
        String baseName = channel.getName().replaceAll("[\\s\\p{So}\\p{Cn}]+$", "");
        channel.getManager().setName(baseName + statusSuffix).queue();
    }

    public static void handleTicketClose(ButtonInteractionEvent event) {
        long channelId = event.getChannel().getIdLong();
        long callerId = event.getUser().getIdLong();
        Member caller = event.getMember();

        boolean isOwner = TicketRepository.isOwner(channelId, callerId);
        boolean isStaff = caller != null && isStaff(caller);

        if (!isOwner && !isStaff) {
            event.reply("คุณไม่มีสิทธิ์ปิด ticket นี้ครับ (ต้องเป็นเจ้าของ ticket หรือแอดมินเท่านั้น)")
                    .setEphemeral(true).queue();
            return;
        }

        TicketRepository.closeTicket(event.getChannel().getIdLong());
        event.getChannel().delete().queue();
    }
}
