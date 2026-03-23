package blue.starry.tokidokiroppou.core.ai.db

import androidx.room.Entity

/**
 * AI 解説のキャッシュエントリ。
 * 同一条文に対する解説を一定期間保持する。
 */
@Entity(
    tableName = "explanation_cache",
    primaryKeys = ["lawCode", "articleNumber", "supplementaryProvisionLabel"],
)
data class ExplanationCacheEntity(
    val lawCode: String,
    val articleNumber: String,
    val supplementaryProvisionLabel: String,
    val explanation: String,
    val createdAt: Long,
)
