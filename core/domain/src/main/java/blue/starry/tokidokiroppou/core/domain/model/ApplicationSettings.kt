package blue.starry.tokidokiroppou.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class ApplicationSettings(
    val notificationIntervalMinutes: Int = 60,
    val enabledLawIds: Set<LawId> = PresetLaw.defaultNotificationLawIds,
    val isNotificationEnabled: Boolean = true,
    val useHalfWidthParentheses: Boolean = false,
    val excludeSupplementaryProvisions: Boolean = false,
    val widgetUpdateIntervalMinutes: Int = 60,
    val isQuietHoursEnabled: Boolean = true,
    val quietHours: QuietHours = QuietHours.DEFAULT,
) {
    val notificationInterval: Duration
        get() = notificationIntervalMinutes.toLong().let { Duration.parse("${it}m") }

    val enabledLawCodes: Set<LawCode>
        get() = enabledLawIds
            .mapNotNull { lawId -> PresetLaw.fromLawId(lawId)?.legacyCodeName }
            .mapNotNull { name -> runCatching { LawCode.valueOf(name) }.getOrNull() }
            .toSet()

    /** [minutesOfDay] (その日の 0:00 からの経過分) が通知を抑止すべき時刻かを返す */
    fun shouldSuppressNotificationAt(minutesOfDay: Int): Boolean {
        return isQuietHoursEnabled && quietHours.contains(minutesOfDay)
    }

    companion object {
        val INTERVAL_OPTIONS = listOf(15, 30, 60, 120, 240, 480, 720, 1440)

        fun intervalDisplayText(minutes: Int): String = when {
            minutes < 60 -> "${minutes}分"
            minutes % 60 == 0 -> "${minutes / 60}時間"
            else -> "${minutes / 60}時間${minutes % 60}分"
        }
    }
}
