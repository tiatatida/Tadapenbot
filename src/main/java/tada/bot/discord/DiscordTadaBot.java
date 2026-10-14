package tada.bot.discord;

import tada.bot.discord.database.DatabaseManager;
import tada.bot.discord.database.VoiceRepository;
import tada.bot.discord.listener.ButtonListener;
import tada.bot.discord.listener.CommandListener;
import tada.bot.discord.listener.ModalListener;
import tada.bot.discord.listener.SelectMenuListener;
import tada.bot.discord.listener.VoiceStateListener;
import tada.bot.discord.util.GetToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;
import java.util.concurrent.Executors;

public class DiscordTadaBot {

    private static final Logger logger = LoggerFactory.getLogger(DiscordTadaBot.class);
    private static JDA jda;

    public static void main(String[] args) {

        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("TZ", "Asia/Bangkok")));

        String Token = GetToken.getToken();

        if (Token == null || Token.isEmpty()) {
            logger.error("Token is empty or null!");
            return;
        }

        try {
            DatabaseManager.init();
            VoiceRepository.loadCache();
            Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::shutdown));

            jda = JDABuilder.createDefault(Token)
                    .setEventPool(Executors.newScheduledThreadPool(8))
                    .addEventListeners(new CommandListener())
                    .addEventListeners(new ButtonListener())
                    .addEventListeners(new SelectMenuListener())
                    .addEventListeners(new ModalListener())
                    .addEventListeners(new VoiceStateListener())
                    .build();
            jda.awaitReady();
            VoiceStateListener.sweepOrphans(jda);
            logger.info("{} is online.", jda.getSelfUser().getName());

            registerSlashCommands();
        } catch (Exception e) {
            logger.error("Error starting the bot!", e);
        }
    }

    private static void registerSlashCommands() {

        if (jda == null) {
            logger.error("JDA has not been initialized. Cannot register commands!");
            return;
        }

        logger.info("Registering slash commands.");
        jda.updateCommands().addCommands(
                Commands.slash("help", "ดูคำสั่งทั้งหมดที่ใช้งานได้ ✨"),
                Commands.slash("ticket", "จัดการ ticket ✨")
                        .addSubcommands(
                                new SubcommandData("open", "เปิด ticket ใหม่ ✨"),
                                new SubcommandData("add", "เพิ่มสมาชิกเข้า ticket ✨")
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการเพิ่ม", true)
                        ),
                Commands.slash("daily", "เช็คอินรายวัน ✨"),
                Commands.slash("info", "แสดงข้อมูลผู้ใช้ ✨")
                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการ", true),
                Commands.slash("report", "รายงานผู้ใช้ ✨")
                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการรายงาน", true),
                Commands.slash("tada", "คำสั่งจัดการระบบสำหรับแอดมิน ✨")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .addSubcommandGroups(
                                new SubcommandGroupData("setup", "ตั้งค่าระบบต่างๆ")
                                        .addSubcommands(
                                                new SubcommandData("report", "ตั้งค่าห้องรับรายงาน")
                                                        .addOptions(new OptionData(OptionType.CHANNEL, "channel",
                                                                "ห้องที่ต้องการตั้ง (ค่าเริ่มต้นคือห้องปัจจุบัน)", false)
                                                                .setChannelTypes(ChannelType.TEXT)),
                                                new SubcommandData("ticket", "ตั้งค่าห้องเปิด ticket (แบบถาวร)"),
                                                new SubcommandData("voice", "ตั้งค่าห้องสร้างห้องเสียงอัตโนมัติ (Join to Create)")
                                                        .addOptions(new OptionData(OptionType.CHANNEL, "channel",
                                                                "ห้องเสียงที่ใช้เป็นจุดเข้าเพื่อสร้างห้องใหม่", true)
                                                                .setChannelTypes(ChannelType.VOICE))
                                        )
                        )
                        .addSubcommands(
                                new SubcommandData("warn", "ให้ใบเตือนผู้ใช้")
                                        .addOptions(new OptionData(OptionType.STRING, "color", "สี", true)
                                                .addChoice("🟡 เหลือง (หมดอายุ 3 เดือน)", "YELLOW")
                                                .addChoice("🟠 ส้ม (หมดอายุ 6 เดือน)", "ORANGE")
                                                .addChoice("🔴 แดง (แบนทันที)", "RED"))
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการเตือน", true)
                                        .addOptions(new OptionData(OptionType.STRING, "reason", "เหตุผลในการเตือน", true)
                                                .setMaxLength(255)),
                                new SubcommandData("history", "ดูประวัติ ใบเตือน, ถูกรายงาน, รายงานผู้อื่น ของผู้ใช้")
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการดูประวัติ", true)
                        ),
                Commands.slash("review", "ให้คะแนนรีวิวผู้ใช้ ✨")
                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการรีวิว", true)
                        .addOptions(new OptionData(OptionType.INTEGER, "rating", "คะแนน 1-5 ดาว", true)
                                .addChoice("⭐", 1)
                                .addChoice("⭐⭐", 2)
                                .addChoice("⭐⭐⭐", 3)
                                .addChoice("⭐⭐⭐⭐", 4)
                                .addChoice("⭐⭐⭐⭐⭐", 5)),
                Commands.slash("voice", "จัดการห้องของคุณ ✨")
                        .addSubcommands(
                                new SubcommandData("rename", "เปลี่ยนชื่อห้องของคุณ ✨")
                                        .addOption(OptionType.STRING, "name", "ชื่อห้องใหม่", true),
                                new SubcommandData("limit", "ตั้งค่าจำนวนคนสูงสุดที่เข้าห้องได้ ✨")
                                        .addOption(OptionType.INTEGER, "amount", "จำนวนคนสูงสุด ✨ (0 = ไม่จำกัด)", true),
                                new SubcommandData("lock", "ล็อกห้องไม่ให้คนอื่นเข้า ✨"),
                                new SubcommandData("unlock", "ปลดล็อกห้อง ✨"),
                                new SubcommandData("permit", "อนุญาตให้ผู้ใช้เข้าห้องได้แม้ห้องจะถูกล็อก/ซ่อนอยู่ ✨")
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการอนุญาต ✨", true),
                                new SubcommandData("kick", "เตะผู้ใช้ออกจากห้องของคุณ ✨")
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการเตะออก", true),
                                new SubcommandData("ghost", "ซ่อนห้องจากคนที่ไม่ได้อยู่ในห้อง ✨"),
                                new SubcommandData("unghost", "เลิกซ่อนห้อง ✨"),
                                new SubcommandData("claim", "รับสิทธิ์เป็นเจ้าของห้องใหม่ ✨ (เมื่อเจ้าของเดิมออกจากห้องไปแล้ว)")
                        )
        ).queue(success -> logger.info("Slash commands have been successfully registered."),
                failure -> logger.error("Failed to add slash commands!", failure));
    }
}