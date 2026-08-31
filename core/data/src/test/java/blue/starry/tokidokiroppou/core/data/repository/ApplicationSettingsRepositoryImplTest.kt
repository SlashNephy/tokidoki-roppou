package blue.starry.tokidokiroppou.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.QuietHours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationSettingsRepositoryImplTest {
    private val notificationIntervalKey = intPreferencesKey("notification_interval")
    private val notificationEnabledKey = booleanPreferencesKey("notification_enabled")
    private val enabledLawCodesKey = stringSetPreferencesKey("enabled_law_codes")
    private val useHalfWidthParenthesesKey = booleanPreferencesKey("use_half_width_parentheses")
    private val excludeSupplementaryProvisionsKey = booleanPreferencesKey("exclude_supplementary_provisions")
    private val widgetUpdateIntervalKey = intPreferencesKey("widget_update_interval")
    private val quietHoursEnabledKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStartMinutesKey = intPreferencesKey("quiet_hours_start_minutes")
    private val quietHoursEndMinutesKey = intPreferencesKey("quiet_hours_end_minutes")

    @Test
    fun getMigratesStoredLegacyLawCodeNamesToLawIdsAndWritesBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[enabledLawCodesKey] = setOf("CIVIL_CODE", "PENAL_CODE")
            }

            val settings = testEnvironment.repository.get()

            assertEquals(
                setOf(LawId("129AC0000000089"), LawId("140AC0000000045")),
                settings.enabledLawIds,
            )
            assertEquals(
                setOf("129AC0000000089", "140AC0000000045"),
                testEnvironment.dataStore.data.first()[enabledLawCodesKey],
            )
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getKeepsUnknownLawIdWhenMigratingLegacyLawCodeNames() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[enabledLawCodesKey] = setOf("CIVIL_CODE", "999AC0000000001")
            }

            val settings = testEnvironment.repository.get()

            assertEquals(
                setOf(LawId("129AC0000000089"), LawId("999AC0000000001")),
                settings.enabledLawIds,
            )
            assertEquals(
                setOf("129AC0000000089", "999AC0000000001"),
                testEnvironment.dataStore.data.first()[enabledLawCodesKey],
            )
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun observeMigratesStoredLegacyLawCodeNamesToLawIdsAndWritesBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[enabledLawCodesKey] = setOf("CIVIL_CODE", "PENAL_CODE")
            }

            val settings = testEnvironment.repository.observe().first()

            assertEquals(
                setOf(LawId("129AC0000000089"), LawId("140AC0000000045")),
                settings.enabledLawIds,
            )
            assertEquals(
                setOf("129AC0000000089", "140AC0000000045"),
                testEnvironment.dataStore.data.first()[enabledLawCodesKey],
            )
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getPreservesExistingSettingsWhileMigratingLawIds() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[notificationIntervalKey] = 120
                preferences[notificationEnabledKey] = false
                preferences[enabledLawCodesKey] = setOf("CIVIL_CODE")
                preferences[useHalfWidthParenthesesKey] = true
                preferences[excludeSupplementaryProvisionsKey] = true
            }

            val settings = testEnvironment.repository.get()

            assertEquals(120, settings.notificationIntervalMinutes)
            assertEquals(false, settings.isNotificationEnabled)
            assertEquals(setOf(LawId("129AC0000000089")), settings.enabledLawIds)
            assertEquals(true, settings.useHalfWidthParentheses)
            assertEquals(true, settings.excludeSupplementaryProvisions)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun setLawEnabledUpdatesStoredLawIdsAfterNormalizingExistingLegacyLawCodeNames() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[enabledLawCodesKey] = setOf("CIVIL_CODE", "PENAL_CODE")
            }

            testEnvironment.repository.setLawEnabled(LawId("129AC0000000089"), enabled = false)

            assertEquals(
                setOf("140AC0000000045"),
                testEnvironment.dataStore.data.first()[enabledLawCodesKey],
            )
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun setWidgetUpdateIntervalMinutesPersistsValueAndIsReadBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.repository.setWidgetUpdateIntervalMinutes(240)

            assertEquals(240, testEnvironment.dataStore.data.first()[widgetUpdateIntervalKey])
            assertEquals(240, testEnvironment.repository.get().widgetUpdateIntervalMinutes)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getFallsBackToOneHourWhenWidgetUpdateIntervalIsAbsent() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            assertEquals(60, testEnvironment.repository.get().widgetUpdateIntervalMinutes)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getFallsBackToEnabledQuietHoursFrom23To7WhenKeysAreAbsent() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            val settings = testEnvironment.repository.get()

            assertTrue(settings.isQuietHoursEnabled)
            assertEquals(QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60), settings.quietHours)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun setQuietHoursEnabledPersistsValueAndIsReadBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.repository.setQuietHoursEnabled(false)

            assertEquals(false, testEnvironment.dataStore.data.first()[quietHoursEnabledKey])
            assertFalse(testEnvironment.repository.get().isQuietHoursEnabled)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun setQuietHoursPersistsBothEndpointsAndIsReadBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.repository.setQuietHours(startMinutesOfDay = 22 * 60 + 30, endMinutesOfDay = 6 * 60)

            val preferences = testEnvironment.dataStore.data.first()
            assertEquals(22 * 60 + 30, preferences[quietHoursStartMinutesKey])
            assertEquals(6 * 60, preferences[quietHoursEndMinutesKey])
            assertEquals(
                QuietHours(startMinutesOfDay = 22 * 60 + 30, endMinutesOfDay = 6 * 60),
                testEnvironment.repository.get().quietHours,
            )
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getFallsBackToDefaultQuietHoursWhenStoredMinutesAreOutOfRange() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[quietHoursStartMinutesKey] = -1
                preferences[quietHoursEndMinutesKey] = 1440
            }

            assertEquals(
                QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
                testEnvironment.repository.get().quietHours,
            )
        } finally {
            testEnvironment.close()
        }
    }

    private fun createTestEnvironment(): TestEnvironment {
        val scope = TestScope(UnconfinedTestDispatcher())
        val file = File.createTempFile("application-settings", ".preferences_pb").apply {
            delete()
        }
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            file
        }

        return TestEnvironment(
            dataStore = dataStore,
            repository = ApplicationSettingsRepositoryImpl(dataStore),
            scope = scope,
            file = file,
        )
    }

    private class TestEnvironment(
        val dataStore: androidx.datastore.core.DataStore<Preferences>,
        val repository: ApplicationSettingsRepositoryImpl,
        private val scope: TestScope,
        private val file: File,
    ) {
        fun close() {
            scope.cancel()
            file.delete()
        }
    }
}
