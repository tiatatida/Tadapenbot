package tada.bot.discord.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameUtilTest {

    @Test
    void keepsShortName() {
        assertEquals("abc", NameUtil.limit100("abc"));
    }

    @Test
    void cutsLongNameTo100() {
        assertEquals(100, NameUtil.limit100("a".repeat(150)).length());
    }

    @Test
    void doesNotSplitEmoji() {
        String emoji = new String(Character.toChars(0x1F600));
        String cut = NameUtil.limit100("a".repeat(99) + emoji + "b");
        assertEquals("a".repeat(99) + emoji, cut);
    }
}
