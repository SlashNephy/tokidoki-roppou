package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading

@Entity(tableName = "structure_headings")
data class StructureHeadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lawCode: String,
    val title: String,
    val level: String,
    val orderIndex: Int,
)

fun StructureHeading.toEntity(): StructureHeadingEntity {
    return StructureHeadingEntity(
        lawCode = lawId.value,
        title = title,
        level = level.name,
        orderIndex = orderIndex,
    )
}

fun StructureHeadingEntity.toDomain(): StructureHeading? {
    val lawId = lawCode.toStoredLawIdOrNull() ?: return null
    val headingLevel = runCatching { StructureHeading.Level.valueOf(level) }.getOrNull() ?: return null

    return StructureHeading(
        lawId = lawId,
        title = title,
        level = headingLevel,
        orderIndex = orderIndex,
    )
}
