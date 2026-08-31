package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuietHoursTest {
    @Test
    fun containsReturnsTrueInsideSameDayRange() {
        val quietHours = QuietHours(startMinutesOfDay = 13 * 60, endMinutesOfDay = 15 * 60)

        assertTrue(quietHours.contains(14 * 60))
    }

    @Test
    fun containsReturnsFalseOutsideSameDayRange() {
        val quietHours = QuietHours(startMinutesOfDay = 13 * 60, endMinutesOfDay = 15 * 60)

        assertFalse(quietHours.contains(12 * 60 + 59))
        assertFalse(quietHours.contains(16 * 60))
    }

    @Test
    fun containsIncludesStartAndExcludesEnd() {
        val quietHours = QuietHours(startMinutesOfDay = 13 * 60, endMinutesOfDay = 15 * 60)

        assertTrue(quietHours.contains(13 * 60))
        assertFalse(quietHours.contains(15 * 60))
    }

    @Test
    fun containsHandlesRangeCrossingMidnight() {
        val quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60)

        assertTrue(quietHours.contains(23 * 60))
        assertTrue(quietHours.contains(23 * 60 + 30))
        assertTrue(quietHours.contains(0))
        assertTrue(quietHours.contains(6 * 60 + 59))
        assertFalse(quietHours.contains(7 * 60))
        assertFalse(quietHours.contains(12 * 60))
        assertFalse(quietHours.contains(22 * 60 + 59))
    }

    @Test
    fun containsReturnsFalseWhenStartEqualsEnd() {
        val quietHours = QuietHours(startMinutesOfDay = 9 * 60, endMinutesOfDay = 9 * 60)

        assertFalse(quietHours.contains(9 * 60))
        assertFalse(quietHours.contains(0))
        assertFalse(quietHours.contains(23 * 60 + 59))
    }

    @Test
    fun isValidMinutesOfDayAcceptsOnlyValuesWithinOneDay() {
        assertTrue(QuietHours.isValidMinutesOfDay(0))
        assertTrue(QuietHours.isValidMinutesOfDay(1439))
        assertFalse(QuietHours.isValidMinutesOfDay(-1))
        assertFalse(QuietHours.isValidMinutesOfDay(1440))
    }

    @Test
    fun formatMinutesOfDayPadsHourAndMinuteToTwoDigits() {
        assertEquals("00:00", QuietHours.formatMinutesOfDay(0))
        assertEquals("07:05", QuietHours.formatMinutesOfDay(7 * 60 + 5))
        assertEquals("23:00", QuietHours.formatMinutesOfDay(23 * 60))
        assertEquals("23:59", QuietHours.formatMinutesOfDay(1439))
    }
}
