package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "law_metadata")
data class LawMetadataEntity(
    @PrimaryKey
    val lawCode: String,
    val lawNum: String,
    val promulgationDate: String?,
)
