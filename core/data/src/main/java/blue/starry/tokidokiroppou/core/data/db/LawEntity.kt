package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "laws")
data class LawEntity(
    @PrimaryKey
    val lawId: String,
    val displayName: String,
    val lawNum: String?,
    val category: String,
    val addedAt: Long = System.currentTimeMillis(),
)
