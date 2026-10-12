package tada.bot.discord.command;

import tada.bot.discord.database.VoiceRepository;
import tada.bot.discord.util.VoiceActions;
import tada.bot.discord.util.VoiceRoomUI;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class VoiceCommand implements Command {

    @Override
    public String getName() {
        return "voice";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {

        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("โปรดระบุคำสั่งย่อยด้วยครับ").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        switch (subcommand) {
            case "rename" -> rename(event);
            case "limit" -> limit(event);
            case "lock" -> lock(event);
            case "unlock" -> unlock(event);
            case "permit" -> permit(event);
            case "kick" -> kick(event);
            case "ghost" -> ghost(event);
            case "unghost" -> unghost(event);
            case "claim" -> claim(event);
            default -> event.getHook().editOriginal("ไม่รู้จักคำสั่งนี้").queue();
        }
    }

    private VoiceChannel getOwnedVoiceChannel(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return null;
        }

        VoiceChannel voiceChannel = VoiceActions.resolveTempVoiceChannel(event, member);
        if (voiceChannel == null) {
            return null;
        }

        if (!VoiceRepository.isOwner(voiceChannel.getIdLong(), member.getIdLong())) {
            event.getHook().editOriginal("คุณไม่ใช่เจ้าของห้องนี้ครับ คำสั่งนี้ใช้ได้เฉพาะเจ้าของห้องเท่านั้น").queue();
            return null;
        }

        return voiceChannel;
    }

    private void rename(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        String newName = event.getOption("name", OptionMapping::getAsString);
        VoiceActions.rename(event, voiceChannel, event.getUser().getIdLong(), newName);
    }

    private void limit(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Integer amount = event.getOption("amount", OptionMapping::getAsInt);
        VoiceActions.limit(event, voiceChannel, amount);
    }

    private void lock(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Role publicRole = voiceChannel.getGuild().getPublicRole();
        voiceChannel.upsertPermissionOverride(publicRole)
                .deny(Permission.VOICE_CONNECT)
                .queue(
                        success -> {
                            VoiceRoomUI.refreshMessage(voiceChannel);
                            event.getHook().editOriginal("🔒 ล็อกห้องเรียบร้อยครับ คนอื่นจะเข้าห้องนี้ไม่ได้แล้ว").queue();
                        },
                        failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการล็อกห้องครับ").queue());
    }

    private void unlock(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Role publicRole = voiceChannel.getGuild().getPublicRole();
        voiceChannel.upsertPermissionOverride(publicRole)
                .clear(Permission.VOICE_CONNECT)
                .queue(
                        success -> {
                            VoiceRoomUI.refreshMessage(voiceChannel);
                            event.getHook().editOriginal("🔓 ปลดล็อกห้องเรียบร้อยครับ").queue();
                        },
                        failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการปลดล็อกห้องครับ").queue());
    }

    private void permit(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Member target = event.getOption("user", OptionMapping::getAsMember);
        VoiceActions.permit(event, voiceChannel, target);
    }

    private void kick(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Member target = event.getOption("user", OptionMapping::getAsMember);
        VoiceActions.kick(event, voiceChannel, target, event.getUser().getIdLong());
    }

    private void ghost(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Role publicRole = voiceChannel.getGuild().getPublicRole();
        voiceChannel.upsertPermissionOverride(publicRole)
                .deny(Permission.VIEW_CHANNEL)
                .queue(
                        success -> {
                            VoiceRoomUI.refreshMessage(voiceChannel);
                            event.getHook().editOriginal("👻 ซ่อนห้องเรียบร้อยครับ คนที่ไม่ได้อยู่ในห้องจะมองไม่เห็นห้องนี้")
                                    .queue();
                        },
                        failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการซ่อนห้องครับ").queue());
    }

    private void unghost(SlashCommandInteractionEvent event) {
        VoiceChannel voiceChannel = getOwnedVoiceChannel(event);
        if (voiceChannel == null)
            return;

        Role publicRole = voiceChannel.getGuild().getPublicRole();
        voiceChannel.upsertPermissionOverride(publicRole)
                .clear(Permission.VIEW_CHANNEL)
                .queue(
                        success -> {
                            VoiceRoomUI.refreshMessage(voiceChannel);
                            event.getHook().editOriginal("👀 เลิกซ่อนห้องเรียบร้อยครับ").queue();
                        },
                        failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการเลิกซ่อนห้องครับ").queue());
    }

    private void claim(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null)
            return;

        VoiceChannel voiceChannel = VoiceActions.resolveTempVoiceChannel(event, member);
        if (voiceChannel == null)
            return;

        VoiceActions.claim(event, event.getGuild(), member, voiceChannel);
    }
}
