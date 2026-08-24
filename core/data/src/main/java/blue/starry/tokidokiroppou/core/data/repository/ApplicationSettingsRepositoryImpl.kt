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
        )
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
    }
}
