package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlinx.coroutines.flow.Flow

interface ApplicationSettingsRepository {
    fun observe(): Flow<ApplicationSettings>

    suspend fun get(): ApplicationSettings

    suspend fun setNotificationIntervalMinutes(minutes: Int)

    suspend fun setNotificationEnabled(enabled: Boolean)

    suspend fun setLawEnabled(lawId: LawId, enabled: Boolean)

    suspend fun setUseHalfWidthParentheses(enabled: Boolean)

    suspend fun setExcludeSupplementaryProvisions(enabled: Boolean)
}
