package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Entity

/**
 * ブックマーク（保存済み条文）のエンティティ
 *
 * 条文データ自体は articles テーブルにキャッシュされているため、
 * ここでは参照キーと保存日時のみを保持する。
 */
@Entity(
    tableName = "bookmarks",
    primaryKeys = ["lawCode", "articleNumber", "supplementaryProvisionLabel"],
)
data class BookmarkEntity(
    val lawCode: String,
    val articleNumber: String,
    val supplementaryProvisionLabel: String = "",
    val bookmarkedAt: Long = System.currentTimeMillis(),
)
