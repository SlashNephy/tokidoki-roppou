package blue.starry.tokidokiroppou.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.PresetLaw
import blue.starry.tokidokiroppou.core.domain.model.QuietHours
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ApplicationSettingsRepository {

    override fun observe(): Flow<ApplicationSettings> {
        return dataStore.data
            .onEach { preferences ->
                migrateEnabledLawIdsIfNeeded(preferences)
            }
            .map { preferences ->
                preferences.toApplicationSettings()
            }
    }

    override suspend fun get(): ApplicationSettings {
        val preferences = dataStore.data.first()
        migrateEnabledLawIdsIfNeeded(preferences)

        return preferences.toApplicationSettings()
    }

    override suspend fun setNotificationIntervalMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_INTERVAL] = minutes
        }
    }

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    override suspend fun setLawEnabled(lawId: LawId, enabled: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_ENABLED_LAW_CODES]
                ?.normalizeEnabledLawIds()
                ?: PresetLaw.defaultNotificationLawIds.mapTo(mutableSetOf()) { it.value }
            preferences[KEY_ENABLED_LAW_CODES] = if (enabled) {
                current + lawId.value
            } else {
                current - lawId.value
            }
        }
    }

    override suspend fun setUseHalfWidthParentheses(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_USE_HALF_WIDTH_PARENTHESES] = enabled
        }
    }

    override suspend fun setExcludeSupplementaryProvisions(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_EXCLUDE_SUPPLEMENTARY_PROVISIONS] = enabled
        }
    }

    override suspend fun setWidgetUpdateIntervalMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_UPDATE_INTERVAL] = minutes
        }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_QUIET_HOURS_ENABLED] = enabled
        }
    }

    override suspend fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int) {
        // 片方だけ書き込むと一時的に不整合な区間になるため、まとめて書き込む
        dataStore.edit { preferences ->
            preferences[KEY_QUIET_HOURS_START_MINUTES] = startMinutesOfDay
            preferences[KEY_QUIET_HOURS_END_MINUTES] = endMinutesOfDay
        }
    }

    private fun Preferences.toApplicationSettings(): ApplicationSettings {
        val enabledLawIds = this[KEY_ENABLED_LAW_CODES]
            ?.normalizeEnabledLawIds()
            ?.mapTo(mutableSetOf()) { LawId(it) }
            ?: PresetLaw.defaultNotificationLawIds

        return ApplicationSettings(
            notificationIntervalMinutes = this[KEY_NOTIFICATION_INTERVAL] ?: 60,
            enabledLawIds = enabledLawIds,
            isNotificationEnabled = this[KEY_NOTIFICATION_ENABLED] ?: true,
            useHalfWidthParentheses = this[KEY_USE_HALF_WIDTH_PARENTHESES] ?: false,
            excludeSupplementaryProvisions = this[KEY_EXCLUDE_SUPPLEMENTARY_PROVISIONS] ?: false,
            widgetUpdateIntervalMinutes = this[KEY_WIDGET_UPDATE_INTERVAL] ?: 60,
            isQuietHoursEnabled = this[KEY_QUIET_HOURS_ENABLED] ?: true,
            quietHours = readQuietHours(),
        )
    }

    // 手動での書き換えや将来の仕様変更で範囲外の値が入っていた場合に備え、既定値へフォールバックする
    private fun Preferences.readQuietHours(): QuietHours {
        val start = this[KEY_QUIET_HOURS_START_MINUTES] ?: QuietHours.DEFAULT.startMinutesOfDay
        val end = this[KEY_QUIET_HOURS_END_MINUTES] ?: QuietHours.DEFAULT.endMinutesOfDay
        if (!QuietHours.isValidMinutesOfDay(start) || !QuietHours.isValidMinutesOfDay(end)) {
            Timber.w("Stored quiet hours are out of range (start=%d, end=%d), falling back to default", start, end)
            return QuietHours.DEFAULT
        }

        return QuietHours(startMinutesOfDay = start, endMinutesOfDay = end)
    }

    private suspend fun migrateEnabledLawIdsIfNeeded(preferences: Preferences) {
        val current = preferences[KEY_ENABLED_LAW_CODES] ?: return
        val normalized = current.normalizeEnabledLawIds()
        if (current == normalized) {
            return
        }

        runCatching {
            dataStore.edit { mutablePreferences ->
                val latest = mutablePreferences[KEY_ENABLED_LAW_CODES] ?: return@edit
                val latestNormalized = latest.normalizeEnabledLawIds()
                if (latest != latestNormalized) {
                    mutablePreferences[KEY_ENABLED_LAW_CODES] = latestNormalized
                }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to write back migrated law IDs to DataStore")
        }
    }

    private fun Set<String>.normalizeEnabledLawIds(): Set<String> {
        return mapTo(mutableSetOf()) { value ->
            PresetLaw.fromLegacyCodeName(value)?.id?.value ?: value
        }
    }

    companion object {
        private val KEY_NOTIFICATION_INTERVAL = intPreferencesKey("notification_interval")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_ENABLED_LAW_CODES = stringSetPreferencesKey("enabled_law_codes")
        private val KEY_USE_HALF_WIDTH_PARENTHESES = booleanPreferencesKey("use_half_width_parentheses")
        private val KEY_EXCLUDE_SUPPLEMENTARY_PROVISIONS = booleanPreferencesKey("exclude_supplementary_provisions")
        private val KEY_WIDGET_UPDATE_INTERVAL = intPreferencesKey("widget_update_interval")
        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_HOURS_START_MINUTES = intPreferencesKey("quiet_hours_start_minutes")
        private val KEY_QUIET_HOURS_END_MINUTES = intPreferencesKey("quiet_hours_end_minutes")
    }
}
