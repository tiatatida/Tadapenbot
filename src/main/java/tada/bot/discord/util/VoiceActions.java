package tada.bot.discord.util;

import tada.bot.discord.database.VoiceRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceActions {

    public static final EnumSet<Permission> OWNER_PERMS = EnumSet.of(
            Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS, Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL);

    private static final long RENAME_COOLDOWN_MS = 5 * 60_000L;
    private static final Map<Long, Long> LAST_RENAME = new ConcurrentHashMap<>();

    private VoiceActions() {
    }

    public static void rename(IReplyCallback event, VoiceChannel voiceChannel, long actorId, String newNameRaw) {
        if (newNameRaw == null || newNameRaw.isBlank()) {
            event.getHook().editOriginal("โปรดระบุชื่อห้องใหม่ครับ").queue();
            return;
        }

        String newName = newNameRaw.length() > 100 ? newNameRaw.substring(0, 100) : newNameRaw;

        long now = System.currentTimeMillis();
        Long last = LAST_RENAME.get(voiceChannel.getIdLong());
        if (last != null && now - last < RENAME_COOLDOWN_MS) {
            event.getHook().editOriginal("เปลี่ยนชื่อห้องได้ทุก 5 นาทีครับ (ข้อจำกัดของ Discord) โปรดรออีก "
                    + (RENAME_COOLDOWN_MS - (now - last)) / 1000 + " วินาที").queue();
            return;
        }
        LAST_RENAME.values().removeIf(t -> now - t > RENAME_COOLDOWN_MS);

        voiceChannel.getManager().setName(newName).queue(
                success -> {
                    LAST_RENAME.put(voiceChannel.getIdLong(), now);
                    VoiceRepository.setPreferredName(voiceChannel.getGuild().getIdLong(), actorId, newName);
                    VoiceRoomUI.refreshMessage(voiceChannel);
                    event.getHook().editOriginal("เปลี่ยนชื่อห้องเป็น **" + newName + "** เรียบร้อยครับ ✨").queue();
                },
                failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการเปลี่ยนชื่อห้องครับ").queue());
    }

    public static void limit(IReplyCallback event, VoiceChannel voiceChannel, Integer amount) {
        if (amount == null || amount < 0 || amount > 99) {
            event.getHook().editOriginal("โปรดระบุจำนวนคนระหว่าง 0 (ไม่จำกัด) ถึง 99 ครับ").queue();
            return;
        }

        voiceChannel.getManager().setUserLimit(amount).queue(
                success -> {
                    String text = amount == 0 ? "ไม่จำกัดจำนวนคน" : amount + " คน";
                    event.getHook().editOriginal("ตั้งค่าจำนวนคนสูงสุดในห้องเป็น " + text + " เรียบร้อยครับ ✨").queue();
                },
                failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการตั้งค่าจำนวนคนครับ").queue());
    }

    public static void permit(IReplyCallback event, VoiceChannel voiceChannel, Member target) {
        if (target == null) {
            event.getHook().editOriginal("ไม่พบผู้ใช้ที่ระบุครับ").queue();
            return;
        }

        voiceChannel.upsertPermissionOverride(target)
                .grant(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)
                .queue(
                        success -> event.getHook()
                                .editOriginal("✅ อนุญาตให้ " + target.getAsMention() + " เข้าห้องนี้ได้แล้วครับ").queue(),
                        failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการอนุญาตผู้ใช้ครับ").queue());
    }

    public static void kick(IReplyCallback event, VoiceChannel voiceChannel, Member target, long actorId) {
        if (target == null) {
            event.getHook().editOriginal("ไม่พบผู้ใช้ที่ระบุครับ").queue();
            return;
        }

        if (target.getIdLong() == actorId) {
            event.getHook().editOriginal("คุณเตะตัวเองออกจากห้องตัวเองไม่ได้ครับ 555+").queue();
            return;
        }

        GuildVoiceState targetVoiceState = target.getVoiceState();
        boolean inThisChannel = targetVoiceState != null
                && targetVoiceState.getChannel() != null
                && targetVoiceState.getChannel().getIdLong() == voiceChannel.getIdLong();

        if (!inThisChannel) {
            event.getHook().editOriginal("ผู้ใช้นี้ไม่ได้อยู่ในห้องของคุณครับ").queue();
            return;
        }

        voiceChannel.getGuild().kickVoiceMember(target).queue(
                success -> event.getHook().editOriginal("👢 เตะ " + target.getAsMention() + " ออกจากห้องเรียบร้อยครับ")
                        .queue(),
                failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการเตะผู้ใช้ครับ").queue());
    }

    public static VoiceChannel resolveTempVoiceChannel(IReplyCallback event, Member member) {
        GuildVoiceState voiceState = member.getVoiceState();
        AudioChannelUnion audioChannel = voiceState != null ? voiceState.getChannel() : null;

        if (audioChannel == null || audioChannel.getType() != ChannelType.VOICE) {
            event.getHook().editOriginal("คุณต้องอยู่ในห้องเสียงชั่วคราวก่อนถึงจะใช้คำสั่งนี้ได้ครับ").queue();
            return null;
        }

        VoiceChannel voiceChannel = audioChannel.asVoiceChannel();

        if (!VoiceRepository.isTempVoiceChannel(voiceChannel.getIdLong())) {
            event.getHook().editOriginal("ห้องนี้ไม่ใช่ห้องเสียงชั่วคราวที่สร้างโดยระบบครับ").queue();
            return null;
        }

        return voiceChannel;
    }

    public static void claim(IReplyCallback event, Guild guild, Member member, VoiceChannel voiceChannel) {
        long channelId = voiceChannel.getIdLong();
        Long currentOwnerId = VoiceRepository.getOwner(channelId);

        if (currentOwnerId != null && currentOwnerId == member.getIdLong()) {
            event.getHook().editOriginal("คุณเป็นเจ้าของห้องนี้อยู่แล้วครับ").queue();
            return;
        }

        boolean ownerStillHere = false;
        if (currentOwnerId != null) {
            Member ownerMember = guild.getMemberById(currentOwnerId);
            GuildVoiceState ownerVoiceState = ownerMember != null ? ownerMember.getVoiceState() : null;
            ownerStillHere = ownerVoiceState != null
                    && ownerVoiceState.getChannel() != null
                    && ownerVoiceState.getChannel().getIdLong() == channelId;
        }

        if (ownerStillHere) {
            event.getHook().editOriginal("เจ้าของห้องยังอยู่ในห้องนี้ครับ คุณ claim ห้องนี้ไม่ได้ตอนนี้").queue();
            return;
        }

        voiceChannel.upsertPermissionOverride(member).grant(OWNER_PERMS).queue(
                success -> {
                    VoiceRepository.updateOwner(channelId, member.getIdLong());
                    if (currentOwnerId != null) {
                        voiceChannel.getMemberPermissionOverrides().stream()
                                .filter(o -> o.getIdLong() == currentOwnerId)
                                .forEach(o -> o.delete().queue(s -> { }, f -> { }));
                    }
                    VoiceRoomUI.refreshMessage(voiceChannel);
                    event.getHook().editOriginal("👑 คุณได้เป็นเจ้าของห้องนี้คนใหม่เรียบร้อยครับ").queue();
                },
                failure -> event.getHook().editOriginal("เกิดข้อผิดพลาดในการรับสิทธิ์เจ้าของห้องครับ").queue());
    }
}
