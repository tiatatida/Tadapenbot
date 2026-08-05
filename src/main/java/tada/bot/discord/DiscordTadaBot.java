package tada.bot.discord;

import tada.bot.discord.database.DatabaseManager;
import tada.bot.discord.listener.ButtonListener;
import tada.bot.discord.listener.CommandListener;
import tada.bot.discord.listener.SelectMenuListener;
import tada.bot.discord.util.GetToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordTadaBot {

    private static final Logger logger = LoggerFactory.getLogger(DiscordTadaBot.class);
    private static JDA jda;

    public static void main(String[] args) {

        String Token = GetToken.getToken();

        if (Token == null || Token.isEmpty()) {
            logger.error("Token is empty or null!");
            return;
        }

        try {
            DatabaseManager.init();
            Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::shutdown));

            jda = JDABuilder.createDefault(Token)
                    .addEventListeners(new CommandListener())
                    .addEventListeners(new ButtonListener())
                    .addEventListeners(new SelectMenuListener())
                    .build();
            jda.awaitReady();
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
                Commands.slash("report", "รายงานผู้ใช้ที่ ✨")
                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการรายงาน", true),
                Commands.slash("tada", "คำสั่งจัดการระบบสำหรับแอดมิน ✨")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .addSubcommandGroups(
                                new SubcommandGroupData("setup", "ตั้งค่าระบบต่างๆ")
                                        .addSubcommands(
                                                new SubcommandData("report", "ตั้งค่าห้องรับรายงาน")
                                                        .addOption(OptionType.CHANNEL, "channel",
                                                                "ห้องที่ต้องการตั้ง (ค่าเริ่มต้นคือห้องปัจจุบัน)", false),
                                                new SubcommandData("ticket", "ตั้งค่าห้องเปิด ticket (แบบถาวร)")
                                        )
                        )
                        .addSubcommands(
                                new SubcommandData("warn", "ให้ใบเตือนผู้ใช้ (ครบ 2 ใบจะถูกแบนอัตโนมัติ)")
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการเตือน", true)
                                        .addOption(OptionType.STRING, "reason", "เหตุผลในการเตือน", true),
                                new SubcommandData("history", "ดูประวัติ report/warn ของผู้ใช้")
                                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการดูประวัติ", true)
                        ),
                Commands.slash("review", "ให้คะแนนรีวิวผู้ใช้ ✨")
                        .addOption(OptionType.USER, "user", "ผู้ใช้ที่ต้องการรีวิว", true)
                        .addOptions(new OptionData(OptionType.INTEGER, "rating", "คะแนน 1-5 ดาว", true)
                                .addChoice("⭐ (1 ดาว)", 1)
                                .addChoice("⭐⭐ (2 ดาว)", 2)
                                .addChoice("⭐⭐⭐ (3 ดาว)", 3)
                                .addChoice("⭐⭐⭐⭐ (4 ดาว)", 4)
                                .addChoice("⭐⭐⭐⭐⭐ (5 ดาว)", 5))
        ).queue(success -> logger.info("Slash commands have been successfully registered."),
                failure -> logger.error("Error flied to add slash commands!", failure));
    }
}