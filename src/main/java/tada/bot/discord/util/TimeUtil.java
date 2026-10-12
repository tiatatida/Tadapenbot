package tada.bot.discord.util;

import java.time.Duration;
import java.time.LocalDateTime;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static String formatRemaining(LocalDateTime lastTime) {
        Duration remaining = Duration.between(LocalDateTime.now(), lastTime.plusHours(24));
        if (remaining.isNegative()) return "ไม่กี่นาที";

        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        if (hours > 0) {
            return hours + " ชม. " + minutes + " นาที";
        }
        return minutes + " นาที";
    }
}
