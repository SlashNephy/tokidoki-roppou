package blue.starry.tokidokiroppou.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LawCategory(val displayName: String) {
    ROPPOU("六法"),
    CIVIL_RELATED("民法関連"),
    ADMINISTRATIVE("行政法"),
    COMMERCIAL_RELATED("商法関連"),
    ADMINISTRATIVE_SCRIVENER("行政書士業務関連"),
    INFORMATION("情報関連法"),
}

@Serializable
enum class LawCode(
    val lawId: String,
    val displayName: String,
    val category: LawCategory,
) {
    // 六法
    CONSTITUTION(
        lawId = "321CONSTITUTION",
        displayName = "日本国憲法",
        category = LawCategory.ROPPOU,
    ),
    CIVIL_CODE(
        lawId = "129AC0000000089",
        displayName = "民法",
        category = LawCategory.ROPPOU,
    ),
    COMMERCIAL_CODE(
        lawId = "132AC0000000048",
        displayName = "商法",
        category = LawCategory.ROPPOU,
    ),
    PENAL_CODE(
        lawId = "140AC0000000045",
        displayName = "刑法",
        category = LawCategory.ROPPOU,
    ),
    CODE_OF_CIVIL_PROCEDURE(
        lawId = "408AC0000000109",
        displayName = "民事訴訟法",
        category = LawCategory.ROPPOU,
    ),
    CODE_OF_CRIMINAL_PROCEDURE(
        lawId = "323AC0000000131",
        displayName = "刑事訴訟法",
        category = LawCategory.ROPPOU,
    ),

    // 民法関連
    LAND_AND_BUILDING_LEASE(
        lawId = "403AC0000000090",
        displayName = "借地借家法",
        category = LawCategory.CIVIL_RELATED,
    ),

    // 行政法
    CABINET_ACT(
        lawId = "322AC0000000005",
        displayName = "内閣法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    NATIONAL_GOVERNMENT_ORGANIZATION(
        lawId = "323AC0000000120",
        displayName = "国家行政組織法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    INFORMATION_DISCLOSURE(
        lawId = "411AC0000000042",
        displayName = "行政機関の保有する情報の公開に関する法律",
        category = LawCategory.ADMINISTRATIVE,
    ),
    PUBLIC_RECORDS_MANAGEMENT(
        lawId = "421AC0000000066",
        displayName = "公文書等の管理に関する法律",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_PROCEDURE(
        lawId = "405AC0000000088",
        displayName = "行政手続法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_VICARIOUS_EXECUTION(
        lawId = "323AC0000000043",
        displayName = "行政代執行法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_APPEAL(
        lawId = "426AC0000000068",
        displayName = "行政不服審査法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    ADMINISTRATIVE_CASE_LITIGATION(
        lawId = "337AC0000000139",
        displayName = "行政事件訴訟法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    STATE_REDRESS(
        lawId = "322AC0000000125",
        displayName = "国家賠償法",
        category = LawCategory.ADMINISTRATIVE,
    ),
    LOCAL_AUTONOMY(
        lawId = "322AC0000000067",
        displayName = "地方自治法",
        category = LawCategory.ADMINISTRATIVE,
    ),

    // 商法関連
    COMPANIES_ACT(
        lawId = "417AC0000000086",
        displayName = "会社法",
        category = LawCategory.COMMERCIAL_RELATED,
    ),

    // 行政書士業務関連
    ADMINISTRATIVE_SCRIVENER(
        lawId = "326AC1000000004",
        displayName = "行政書士法",
        category = LawCategory.ADMINISTRATIVE_SCRIVENER,
    ),
    FAMILY_REGISTER(
        lawId = "322AC0000000224",
        displayName = "戸籍法",
        category = LawCategory.ADMINISTRATIVE_SCRIVENER,
    ),
    RESIDENT_REGISTRY(
        lawId = "342AC0000000081",
        displayName = "住民基本台帳法",
        category = LawCategory.ADMINISTRATIVE_SCRIVENER,
    ),

    // 情報関連法
    DIGITAL_GOVERNMENT(
        lawId = "414AC0000000151",
        displayName = "情報通信技術を活用した行政の推進等に関する法律",
        category = LawCategory.INFORMATION,
    ),
    PERSONAL_INFORMATION_PROTECTION(
        lawId = "415AC0000000057",
        displayName = "個人情報の保護に関する法律",
        category = LawCategory.INFORMATION,
    ),
    MY_NUMBER(
        lawId = "425AC0000000027",
        displayName = "行政手続における特定の個人を識別するための番号の利用等に関する法律",
        category = LawCategory.INFORMATION,
    ),
    INFORMATION_DISCLOSURE_REVIEW_BOARD(
        lawId = "415AC0000000060",
        displayName = "情報公開・個人情報保護審査会設置法",
        category = LawCategory.INFORMATION,
    ),
    ELECTRONIC_CONSUMER_CONTRACT(
        lawId = "413AC0000000095",
        displayName = "電子消費者契約に関する民法の特例に関する法律",
        category = LawCategory.INFORMATION,
    ),
    ELECTRONIC_SIGNATURE(
        lawId = "412AC0000000102",
        displayName = "電子署名及び認証業務に関する法律",
        category = LawCategory.INFORMATION,
    ),
    PUBLIC_INDIVIDUAL_AUTHENTICATION(
        lawId = "414AC0000000153",
        displayName = "電子署名等に係る地方公共団体情報システム機構の認証業務に関する法律",
        category = LawCategory.INFORMATION,
    ),
}
