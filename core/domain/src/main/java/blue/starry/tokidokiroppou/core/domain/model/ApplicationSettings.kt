package blue.starry.tokidokiroppou.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class ApplicationSettings(
    val notificationIntervalMinutes: Int = 60,
    val enabledLawCodes: Set<LawCode> = LawCode.entries.filter { it.category == LawCategory.ROPPOU }.toSet(),
    val isNotificationEnabled: Boolean = true,
    val useHalfWidthParentheses: Boolean = false,
    val excludeSupplementaryProvisions: Boolean = false,
) {
    val notificationInterval: Duration
        get() = notificationIntervalMinutes.toLong().let { Duration.parse("${it}m") }

    companion object {
        val INTERVAL_OPTIONS = listOf(15, 30, 60, 120, 240, 480, 720, 1440)

        fun intervalDisplayText(minutes: Int): String = when {
            minutes < 60 -> "${minutes}分"
            minutes % 60 == 0 -> "${minutes / 60}時間"
            else -> "${minutes / 60}時間${minutes % 60}分"
        }
    }
}
