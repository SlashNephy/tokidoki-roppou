package blue.starry.tokidokiroppou.core.domain.model

enum class PresetLaw(
    val legacyCodeName: String,
    val id: LawId,
    val displayName: String,
    val category: LawCategory,
) {
    CONSTITUTION(
        legacyCodeName = "CONSTITUTION",
        id = LawId("321CONSTITUTION"),
        displayName = "日本国憲法",
        category = LawCategory.ROPPOU,
    ),
    CIVIL_CODE(
        legacyCodeName = "CIVIL_CODE",
        id = LawId("129AC0000000089"),
        displayName = "民法",
        category = LawCategory.ROPPOU,
    ),
    COMMERCIAL_CODE(
        legacyCodeName = "COMMERCIAL_CODE",
        id = LawId("132AC0000000048"),
        displayName = "商法",
        category = LawCategory.ROPPOU,
    ),
    PENAL_CODE(
        legacyCodeName = "PENAL_CODE",
        id = LawId("140AC0000000045"),
        displayName = "刑法",
        category = LawCategory.ROPPOU,
    ),
    CODE_OF_CIVIL_PROCEDURE(
        legacyCodeName = "CODE_OF_CIVIL_PROCEDURE",
        id = LawId("408AC0000000109"),
        displayName = "民事訴訟法",
        category = LawCategory.ROPPOU,
    ),
    CODE_OF_CRIMINAL_PROCEDURE(
        legacyCodeName = "CODE_OF_CRIMINAL_PROCEDURE",
        id = LawId("323AC0000000131"),
        displayName = "刑事訴訟法",
        category = LawCategory.ROPPOU,
    ),
    LAND_AND_BUILDING_LEASE(
        legacyCodeName = "LAND_AND_BUILDING_LEASE",
        id = LawId("403AC0000000090"),
        displayName = "借地借家法",
        category = LawCategory.CIVIL_RELATED,
    ),
    CABINET_ACT(
        legacyCodeName = "CABINET_ACT",
        id = LawId("322AC0000000005"),
        displayName = "内閣法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    NATIONAL_GOVERNMENT_ORGANIZATION(
        legacyCodeName = "NATIONAL_GOVERNMENT_ORGANIZATION",
        id = LawId("323AC0000000120"),
        displayName = "国家行政組織法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    INFORMATION_DISCLOSURE(
        legacyCodeName = "INFORMATION_DISCLOSURE",
        id = LawId("411AC0000000042"),
        displayName = "行政機関の保有する情報の公開に関する法律",
        category = LawCategory.ADMINISTRATIVE,
    ),
    PUBLIC_RECORDS_MANAGEMENT(
        legacyCodeName = "PUBLIC_RECORDS_MANAGEMENT",
        id = LawId("421AC0000000066"),
        displayName = "公文書等の管理に関する法律",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_PROCEDURE(
        legacyCodeName = "ADMINISTRATIVE_PROCEDURE",
        id = LawId("405AC0000000088"),
        displayName = "行政手続法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_VICARIOUS_EXECUTION(
        legacyCodeName = "ADMINISTRATIVE_VICARIOUS_EXECUTION",
        id = LawId("323AC0000000043"),
        displayName = "行政代執行法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_APPEAL(
        legacyCodeName = "ADMINISTRATIVE_APPEAL",
        id = LawId("426AC0000000068"),
        displayName = "行政不服審査法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_CASE_LITIGATION(
        legacyCodeName = "ADMINISTRATIVE_CASE_LITIGATION",
        id = LawId("337AC0000000139"),
        displayName = "行政事件訴訟法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    STATE_REDRESS(
        legacyCodeName = "STATE_REDRESS",
        id = LawId("322AC0000000125"),
        displayName = "国家賠償法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    LOCAL_AUTONOMY(
        legacyCodeName = "LOCAL_AUTONOMY",
        id = LawId("322AC0000000067"),
        displayName = "地方自治法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    COMPANIES_ACT(
        legacyCodeName = "COMPANIES_ACT",
        id = LawId("417AC0000000086"),
        displayName = "会社法",
        category = LawCategory.COMMERCIAL_RELATED,
    ),
    ADMINISTRATIVE_SCRIVENER(
        legacyCodeName = "ADMINISTRATIVE_SCRIVENER",
        id = LawId("326AC1000000004"),
        displayName = "行政書士法",
        category = LawCategory.ADMINISTRATIVE_SCRIVENER,
    ),
    FAMILY_REGISTER(
        legacyCodeName = "FAMILY_REGISTER",
        id = LawId("322AC0000000224"),
        displayName = "戸籍法",
        category = LawCategory.ADMINISTRATIVE_SCRIVENER,
    ),
    RESIDENT_REGISTRY(
        legacyCodeName = "RESIDENT_REGISTRY",
        id = LawId("342AC0000000081"),
        displayName = "住民基本台帳法",
        category = LawCategory.ADMINISTRATIVE_SCRIVENER,
    ),
    DIGITAL_GOVERNMENT(
        legacyCodeName = "DIGITAL_GOVERNMENT",
        id = LawId("414AC0000000151"),
        displayName = "情報通信技術を活用した行政の推進等に関する法律",
        category = LawCategory.INFORMATION,
    ),
    PERSONAL_INFORMATION_PROTECTION(
        legacyCodeName = "PERSONAL_INFORMATION_PROTECTION",
        id = LawId("415AC0000000057"),
        displayName = "個人情報の保護に関する法律",
        category = LawCategory.INFORMATION,
    ),
    MY_NUMBER(
        legacyCodeName = "MY_NUMBER",
        id = LawId("425AC0000000027"),
        displayName = "行政手続における特定の個人を識別するための番号の利用等に関する法律",
        category = LawCategory.INFORMATION,
    ),
    INFORMATION_DISCLOSURE_REVIEW_BOARD(
        legacyCodeName = "INFORMATION_DISCLOSURE_REVIEW_BOARD",
        id = LawId("415AC0000000060"),
        displayName = "情報公開・個人情報保護審査会設置法",
        category = LawCategory.INFORMATION,
    ),
    ELECTRONIC_CONSUMER_CONTRACT(
        legacyCodeName = "ELECTRONIC_CONSUMER_CONTRACT",
        id = LawId("413AC0000000095"),
        displayName = "電子消費者契約に関する民法の特例に関する法律",
        category = LawCategory.INFORMATION,
    ),
    ELECTRONIC_SIGNATURE(
        legacyCodeName = "ELECTRONIC_SIGNATURE",
        id = LawId("412AC0000000102"),
        displayName = "電子署名及び認証業務に関する法律",
        category = LawCategory.INFORMATION,
    ),
    PUBLIC_INDIVIDUAL_AUTHENTICATION(
        legacyCodeName = "PUBLIC_INDIVIDUAL_AUTHENTICATION",
        id = LawId("414AC0000000153"),
        displayName = "電子署名等に係る地方公共団体情報システム機構の認証業務に関する法律",
        category = LawCategory.INFORMATION,
    ),
    ROAD_TRAFFIC_LAW(
        legacyCodeName = "ROAD_TRAFFIC_LAW",
        id = LawId("335AC0000000105"),
        displayName = "道路交通法",
        category = LawCategory.OTHERS,
    ),
    ;

    fun toLaw(): Law {
        return Law(
            id = id,
            displayName = displayName,
            category = category,
            isPreset = true,
            isAdded = true,
        )
    }

    companion object {
        val all: List<Law> = entries.map { it.toLaw() }

        val defaultNotificationLawIds: Set<LawId> = entries
            .filter { it.category == LawCategory.ROPPOU }
            .mapTo(mutableSetOf()) { it.id }

        fun fromLegacyCodeName(name: String): PresetLaw? {
            return entries.firstOrNull { it.legacyCodeName == name }
        }

        fun fromLawId(id: LawId): PresetLaw? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
