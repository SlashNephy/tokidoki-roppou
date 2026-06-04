package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawId

fun LawEntity.toDomain(): Law {
    val lawCategory = runCatching { LawCategory.valueOf(category) }.getOrDefault(LawCategory.OTHERS)

    return Law(
        id = LawId(lawId),
        displayName = displayName,
        lawNum = lawNum,
        category = lawCategory,
        isPreset = false,
        isAdded = true,
    )
}

fun Law.toEntity(): LawEntity {
    return LawEntity(
        lawId = id.value,
        displayName = displayName,
        lawNum = lawNum,
        category = category.name,
    )
}
