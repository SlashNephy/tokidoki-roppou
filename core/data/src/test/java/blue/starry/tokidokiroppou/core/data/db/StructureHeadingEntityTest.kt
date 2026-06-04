package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StructureHeadingEntityTest {
    @Test
    fun roundTripPreservesLawId() {
        val heading = StructureHeading(
            lawId = LawId("999AC0000000001"),
            title = "第一編",
            level = StructureHeading.Level.Part,
            orderIndex = 1,
        )

        val entity = heading.toEntity()
        val restored = entity.toDomain()

        assertEquals("999AC0000000001", entity.lawCode)
        assertEquals(LawId("999AC0000000001"), restored?.lawId)
    }

    @Test
    fun toDomainReturnsNullForBlankLawCode() {
        val entity = StructureHeadingEntity(
            lawCode = "",
            title = "第一編",
            level = StructureHeading.Level.Part.name,
            orderIndex = 1,
        )

        assertNull(entity.toDomain())
    }
}
