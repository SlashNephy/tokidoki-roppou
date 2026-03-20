package blue.starry.tokidokiroppou.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LawCode(
    val lawId: String,
    val displayName: String,
    val shortName: String,
) {
    CONSTITUTION(
        lawId = "321CONSTITUTION",
        displayName = "日本国憲法",
        shortName = "憲法",
    ),
    CIVIL_CODE(
        lawId = "129AC0000000089",
        displayName = "民法",
        shortName = "民法",
    ),
    COMMERCIAL_CODE(
        lawId = "132AC0000000048",
        displayName = "商法",
        shortName = "商法",
    ),
    PENAL_CODE(
        lawId = "140AC0000000045",
        displayName = "刑法",
        shortName = "刑法",
    ),
    CODE_OF_CIVIL_PROCEDURE(
        lawId = "408AC0000000109",
        displayName = "民事訴訟法",
        shortName = "民訴法",
    ),
    CODE_OF_CRIMINAL_PROCEDURE(
        lawId = "323AC0000000131",
        displayName = "刑事訴訟法",
        shortName = "刑訴法",
    ),
}
