package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lawCode: String,
    val articleNumber: String,
    val articleTitle: String,
    val articleCaption: String,
    val paragraphsJson: String,
    val supplementaryProvisionLabel: String? = null,
    val orderIndex: Int = 0,
)
