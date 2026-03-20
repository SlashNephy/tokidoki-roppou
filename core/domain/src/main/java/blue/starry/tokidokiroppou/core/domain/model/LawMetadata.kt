package blue.starry.tokidokiroppou.core.domain.model

data class LawMetadata(
    val lawNum: String,
    val promulgationDate: String?,
    val lastAmendmentDate: String?,
    val lastAmendmentLawNum: String?,
)
