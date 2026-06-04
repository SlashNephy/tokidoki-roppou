package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredLawIdMapperTest {
    @Test
    fun toStoredLawIdOrNullNormalizesLegacyCodeName() {
        assertEquals(LawId("129AC0000000089"), "CIVIL_CODE".toStoredLawIdOrNull())
    }

    @Test
    fun toStoredLawIdOrNullKeepsUnknownNonBlankValue() {
        assertEquals(LawId("999AC0000000001"), "999AC0000000001".toStoredLawIdOrNull())
    }

    @Test
    fun toStoredLawIdOrNullDropsBlankValue() {
        assertNull("".toStoredLawIdOrNull())
    }
}
