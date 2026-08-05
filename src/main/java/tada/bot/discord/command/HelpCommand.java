package tada.bot.discord.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class HelpCommand implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "ดูคำสั่งทั้งหมดที่ใช้งานได้ ✨";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์เท่านั้นครับ").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("✨ เมนูช่วยเหลือ");
        embedBuilder.setDescription("""
                คำสั่งทั้งหมดที่ใช้งานได้
                
                🚨`/report`  —  แจ้งปัญหาหรือเรื่องร้องเรียน
                🎫`/ticket`  —  เปิด ticket แจ้งเรื่อง
                👤`/info`    —  เช็คข้อมูลผู้ใช้งาน
                🗓️`/daily`   —  เช็คอินรายวัน
                📝`/review`  —  ให้คะแนนรีวิวผู้ใช้""");
        embedBuilder.setColor(new Color(239, 241, 245));
        embedBuilder.setFooter("Powered by Tada ✨");

        MessageEmbed messageEmbed = embedBuilder.build();
        event.replyEmbeds(messageEmbed).setEphemeral(true).queue();

    }
}
