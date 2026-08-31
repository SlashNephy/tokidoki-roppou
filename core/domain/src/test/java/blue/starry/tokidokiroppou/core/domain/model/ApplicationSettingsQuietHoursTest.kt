package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationSettingsQuietHoursTest {
    @Test
    fun shouldSuppressNotificationAtReturnsTrueInsideQuietHours() {
        val settings = ApplicationSettings(
            isQuietHoursEnabled = true,
            quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
        )

        assertTrue(settings.shouldSuppressNotificationAt(3 * 60))
    }

    @Test
    fun shouldSuppressNotificationAtReturnsFalseOutsideQuietHours() {
        val settings = ApplicationSettings(
            isQuietHoursEnabled = true,
            quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
        )

        assertFalse(settings.shouldSuppressNotificationAt(12 * 60))
    }

    @Test
    fun shouldSuppressNotificationAtReturnsFalseWhenQuietHoursAreDisabled() {
        val settings = ApplicationSettings(
            isQuietHoursEnabled = false,
            quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
        )

        assertFalse(settings.shouldSuppressNotificationAt(3 * 60))
    }
}
