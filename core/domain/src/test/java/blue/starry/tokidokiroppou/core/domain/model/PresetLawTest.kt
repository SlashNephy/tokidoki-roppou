package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresetLawTest {
    @Test
    fun fromLegacyCodeNameReturnsPresetLawForKnownLegacyCodeNames() {
        assertEquals(LawId("129AC0000000089"), PresetLaw.fromLegacyCodeName("CIVIL_CODE")?.id)
        assertEquals(LawId("140AC0000000045"), PresetLaw.fromLegacyCodeName("PENAL_CODE")?.id)
    }

    @Test
    fun fromLegacyCodeNameReturnsNullForUnknownLegacyCodeName() {
        assertNull(PresetLaw.fromLegacyCodeName("UNKNOWN"))
    }

    @Test
    fun defaultNotificationLawIdsReturnsRoppouLawIds() {
        assertEquals(
            listOf(
                LawId("321CONSTITUTION"),
                LawId("129AC0000000089"),
                LawId("132AC0000000048"),
                LawId("140AC0000000045"),
                LawId("408AC0000000109"),
                LawId("323AC0000000131"),
            ),
            PresetLaw.defaultNotificationLawIds,
        )
    }
}
