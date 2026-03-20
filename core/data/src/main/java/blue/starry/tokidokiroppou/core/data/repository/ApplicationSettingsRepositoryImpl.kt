package blue.starry.tokidokiroppou.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ApplicationSettingsRepository {

    override fun observe(): Flow<ApplicationSettings> {
        return dataStore.data.map { preferences ->
            preferences.toApplicationSettings()
        }
    }

    override suspend fun get(): ApplicationSettings {
        return dataStore.data.first().toApplicationSettings()
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

    override suspend fun setLawCodeEnabled(lawCode: LawCode, enabled: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_ENABLED_LAW_CODES]
                ?: LawCode.entries.map { it.name }.toSet()
            preferences[KEY_ENABLED_LAW_CODES] = if (enabled) {
                current + lawCode.name
            } else {
                current - lawCode.name
            }
        }
    }

    private fun Preferences.toApplicationSettings(): ApplicationSettings {
        val enabledCodes = this[KEY_ENABLED_LAW_CODES]
            ?.mapNotNull { name ->
                runCatching { LawCode.valueOf(name) }.getOrNull()
            }
            ?.toSet()
            ?: LawCode.entries.toSet()

        return ApplicationSettings(
            notificationIntervalMinutes = this[KEY_NOTIFICATION_INTERVAL] ?: 60,
            enabledLawCodes = enabledCodes,
            isNotificationEnabled = this[KEY_NOTIFICATION_ENABLED] ?: true,
        )
    }

    companion object {
        private val KEY_NOTIFICATION_INTERVAL = intPreferencesKey("notification_interval")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_ENABLED_LAW_CODES = stringSetPreferencesKey("enabled_law_codes")
    }
}
