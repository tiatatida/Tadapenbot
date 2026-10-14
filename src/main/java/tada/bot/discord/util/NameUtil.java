package tada.bot.discord.util;

public class NameUtil {
    private NameUtil() {
    }

    public static String limit100(String name) {
        return name.codePointCount(0, name.length()) > 100
                ? name.substring(0, name.offsetByCodePoints(0, 100))
                : name;
    }
}
