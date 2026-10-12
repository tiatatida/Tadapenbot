package tada.bot.discord.listener.component;

import tada.bot.discord.database.VoiceRepository;
import tada.bot.discord.util.VoiceActions;
import tada.bot.discord.util.VoiceRoomUI;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.List;

public class VoiceListener {

    public static void handleLockToggle(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.reply("ห้องนี้ถูกลบไปแล้วครับ").setEphemeral(true).queue();
            return;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.reply("ปุ่มนี้ใช้ได้เฉพาะเจ้าของห้องเท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        Role publicRole = guild.getPublicRole();
        boolean locked = VoiceRoomUI.isLocked(voiceChannel);

        event.deferEdit().queue();

        PermissionOverrideAction action = locked
                ? voiceChannel.upsertPermissionOverride(publicRole).clear(Permission.VOICE_CONNECT)
                : voiceChannel.upsertPermissionOverride(publicRole).deny(Permission.VOICE_CONNECT);

        action.queue(
                success -> VoiceRoomUI.refreshMessage(voiceChannel),
                failure -> {
                });
    }

    public static void handleGhostToggle(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.reply("ห้องนี้ถูกลบไปแล้วครับ").setEphemeral(true).queue();
            return;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.reply("ปุ่มนี้ใช้ได้เฉพาะเจ้าของห้องเท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        Role publicRole = guild.getPublicRole();
        boolean ghosted = VoiceRoomUI.isGhosted(voiceChannel);

        event.deferEdit().queue();

        PermissionOverrideAction action = ghosted
                ? voiceChannel.upsertPermissionOverride(publicRole).clear(Permission.VIEW_CHANNEL)
                : voiceChannel.upsertPermissionOverride(publicRole).deny(Permission.VIEW_CHANNEL);

        action.queue(
                success -> VoiceRoomUI.refreshMessage(voiceChannel),
                failure -> {
                });
    }

    private static boolean canManage(ButtonInteractionEvent event, VoiceChannel voiceChannel, long channelId,
            Member member) {
        if (voiceChannel == null) {
            event.reply("ห้องนี้ถูกลบไปแล้วครับ").setEphemeral(true).queue();
            return false;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.reply("ปุ่มนี้ใช้ได้เฉพาะเจ้าของห้องเท่านั้นครับ").setEphemeral(true).queue();
            return false;
        }

        return true;
    }

    public static void handleRenameButton(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (!canManage(event, voiceChannel, channelId, member))
            return;

        TextInput nameInput = TextInput.create("name", TextInputStyle.SHORT)
                .setPlaceholder("ชื่อห้องใหม่")
                .setValue(voiceChannel.getName())
                .setRequired(true)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create(VoiceRoomUI.RENAME_MODAL_PREFIX + channelId, "เปลี่ยนชื่อห้อง")
                .addComponents(Label.of("ชื่อห้องใหม่", nameInput))
                .build();

        event.replyModal(modal).queue();
    }

    public static void handleLimitButton(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (!canManage(event, voiceChannel, channelId, member))
            return;

        TextInput amountInput = TextInput.create("amount", TextInputStyle.SHORT)
                .setPlaceholder("0-99 (0 = ไม่จำกัด)")
                .setValue(String.valueOf(voiceChannel.getUserLimit()))
                .setRequired(true)
                .setMaxLength(2)
                .build();

        Modal modal = Modal.create(VoiceRoomUI.LIMIT_MODAL_PREFIX + channelId, "จำกัดจำนวนคนในห้อง")
                .addComponents(Label.of("จำนวนคนสูงสุด", amountInput))
                .build();

        event.replyModal(modal).queue();
    }

    public static void handlePermitButton(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (!canManage(event, voiceChannel, channelId, member))
            return;

        EntitySelectMenu userSelect = EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("เลือกผู้ใช้ที่ต้องการอนุญาต")
                .setRequiredRange(1, 1)
                .build();

        Modal modal = Modal.create(VoiceRoomUI.PERMIT_MODAL_PREFIX + channelId, "อนุญาตผู้ใช้เข้าห้อง")
                .addComponents(Label.of("เลือกผู้ใช้", userSelect))
                .build();

        event.replyModal(modal).queue();
    }

    public static void handleKickButton(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (!canManage(event, voiceChannel, channelId, member))
            return;

        EntitySelectMenu userSelect = EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("เลือกผู้ใช้ที่ต้องการเตะออก")
                .setRequiredRange(1, 1)
                .build();

        Modal modal = Modal.create(VoiceRoomUI.KICK_MODAL_PREFIX + channelId, "เตะผู้ใช้ออกจากห้อง")
                .addComponents(Label.of("เลือกผู้ใช้", userSelect))
                .build();

        event.replyModal(modal).queue();
    }

    public static void handleClaimButton(ButtonInteractionEvent event, Guild guild, Member member, long channelId) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.reply("ห้องนี้ถูกลบไปแล้วครับ").setEphemeral(true).queue();
            return;
        }

        if (!VoiceRepository.isTempVoiceChannel(channelId)) {
            event.reply("ห้องนี้ไม่ใช่ห้องเสียงชั่วคราวที่สร้างโดยระบบครับ").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        VoiceActions.claim(event, guild, member, voiceChannel);
    }
    
    public static void handleRenameModalSubmit(ModalInteractionEvent event, Guild guild, Member member,
            long channelId) {
        event.deferReply(true).queue();

        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.getHook().editOriginal("ห้องนี้ถูกลบไปแล้วครับ").queue();
            return;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.getHook().editOriginal("คุณไม่ใช่เจ้าของห้องนี้แล้วครับ").queue();
            return;
        }

        ModalMapping mapping = event.getValue("name");
        String newName = mapping != null ? mapping.getAsString() : null;

        VoiceActions.rename(event, voiceChannel, member.getIdLong(), newName);
    }

    public static void handleLimitModalSubmit(ModalInteractionEvent event, Guild guild, Member member,
            long channelId) {
        event.deferReply(true).queue();

        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.getHook().editOriginal("ห้องนี้ถูกลบไปแล้วครับ").queue();
            return;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.getHook().editOriginal("คุณไม่ใช่เจ้าของห้องนี้แล้วครับ").queue();
            return;
        }

        ModalMapping mapping = event.getValue("amount");
        Integer amount = null;
        if (mapping != null) {
            try {
                amount = Integer.parseInt(mapping.getAsString().trim());
            } catch (NumberFormatException ignored) {
            }
        }

        VoiceActions.limit(event, voiceChannel, amount);
    }

    public static void handlePermitModalSubmit(ModalInteractionEvent event, Guild guild, Member member,
            long channelId) {
        event.deferReply(true).queue();

        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.getHook().editOriginal("ห้องนี้ถูกลบไปแล้วครับ").queue();
            return;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.getHook().editOriginal("คุณไม่ใช่เจ้าของห้องนี้แล้วครับ").queue();
            return;
        }

        Member target = extractSelectedMember(event, "user");
        VoiceActions.permit(event, voiceChannel, target);
    }

    public static void handleKickModalSubmit(ModalInteractionEvent event, Guild guild, Member member,
            long channelId) {
        event.deferReply(true).queue();

        VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
        if (voiceChannel == null) {
            event.getHook().editOriginal("ห้องนี้ถูกลบไปแล้วครับ").queue();
            return;
        }

        if (!VoiceRepository.isOwner(channelId, member.getIdLong())) {
            event.getHook().editOriginal("คุณไม่ใช่เจ้าของห้องนี้แล้วครับ").queue();
            return;
        }

        Member target = extractSelectedMember(event, "user");
        VoiceActions.kick(event, voiceChannel, target, member.getIdLong());
    }

    private static Member extractSelectedMember(ModalInteractionEvent event, String componentId) {
        ModalMapping mapping = event.getValue(componentId);
        if (mapping == null) {
            return null;
        }

        List<Member> members = mapping.getAsMentions().getMembers();
        return members.isEmpty() ? null : members.get(0);
    }
}

