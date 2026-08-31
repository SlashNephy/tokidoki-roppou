package blue.starry.tokidokiroppou.core.domain.model

/**
 * 通知を抑止する時間帯。
 *
 * 時刻はその日の 0:00 からの経過分 (0..1439) で表す。
 */
data class QuietHours(
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
) {
    /**
     * [minutesOfDay] が抑止対象かを返す。
     *
     * 開始時刻ちょうどは抑止し、終了時刻ちょうどは抑止しない (半開区間)。
     * 開始と終了が同値のときは空区間とみなし、常に false を返す。通知が丸一日止まる事故を避けるため。
     */
    fun contains(minutesOfDay: Int): Boolean {
        return when {
            startMinutesOfDay == endMinutesOfDay -> false
            startMinutesOfDay < endMinutesOfDay -> minutesOfDay >= startMinutesOfDay && minutesOfDay < endMinutesOfDay
            // 日をまたぐ区間
            else -> minutesOfDay >= startMinutesOfDay || minutesOfDay < endMinutesOfDay
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60

        val DEFAULT = QuietHours(
            startMinutesOfDay = 23 * 60,
            endMinutesOfDay = 7 * 60,
        )

        fun isValidMinutesOfDay(minutesOfDay: Int): Boolean {
            return minutesOfDay in 0 until MINUTES_PER_DAY
        }

        fun formatMinutesOfDay(minutesOfDay: Int): String {
            val hour = minutesOfDay / 60
            val minute = minutesOfDay % 60
            return "%02d:%02d".format(hour, minute)
        }
    }
}
