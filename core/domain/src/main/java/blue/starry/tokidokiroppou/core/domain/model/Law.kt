package blue.starry.tokidokiroppou.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Law(
    val id: LawId,
    val displayName: String,
    val lawNum: String? = null,
    val category: LawCategory = LawCategory.OTHERS,
    val isPreset: Boolean = false,
    val isAdded: Boolean = false,
)
