package tada.bot.discord.database;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Test
    void continuesStreakWhenLastCheckinWasYesterday() {
        assertEquals(4, DailyRepository.nextStreak(TODAY.minusDays(1), TODAY, 3));
    }

    @Test
    void startsOverWhenLastCheckinWasTwoDaysAgo() {
        assertEquals(1, DailyRepository.nextStreak(TODAY.minusDays(2), TODAY, 5));
    }

    @Test
    void startsAtOneForFirstCheckin() {
        assertEquals(1, DailyRepository.nextStreak(null, TODAY, 0));
    }
}
