package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading
import kotlin.test.Test
import kotlin.test.assertEquals

class StructureHeadingEntityTest {
    @Test
    fun toDomainMapsLawIdStringBackToLawCode() {
        val entity = StructureHeadingEntity(
            lawCode = "129AC0000000089",
            title = "第一編",
            level = StructureHeading.Level.Part.name,
            orderIndex = 1,
        )

        assertEquals(LawCode.CIVIL_CODE, entity.toDomain()?.lawCode)
    }

    @Test
    fun toEntityStoresLawIdString() {
        val heading = StructureHeading(
            lawCode = LawCode.CIVIL_CODE,
            title = "第一編",
            level = StructureHeading.Level.Part,
            orderIndex = 1,
        )

        assertEquals("129AC0000000089", heading.toEntity().lawCode)
    }
}
