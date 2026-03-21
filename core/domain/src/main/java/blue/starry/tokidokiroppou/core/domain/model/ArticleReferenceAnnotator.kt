package blue.starry.tokidokiroppou.core.domain.model

/**
 * 条文テキスト中の参照箇所（第X条, 前条, 次条 等）の位置と参照先を表す。
 */
data class ArticleReferenceMatch(
    val range: IntRange,
    val articleNumber: String,
)

/**
 * 表示用テキスト中の条文参照箇所を検出し、位置と参照先の articleNumber を返す。
 * [availableArticleNumbers] に含まれる参照のみを返す（実際に表示される関連条文に限定）。
 */
fun findArticleReferenceRanges(
    displayedText: String,
    article: Article,
    availableArticleNumbers: Set<String>,
): List<ArticleReferenceMatch> {
    val matches = mutableListOf<ArticleReferenceMatch>()
    val currentNum = article.articleNumber.split("_")[0].toIntOrNull()

    // 第X条のY パターン (漢数字)
    val subArticleKanji = Regex("第([一二三四五六七八九十百千万]+)条の([一二三四五六七八九十百千万]+)")
    for (match in subArticleKanji.findAll(displayedText)) {
        val main = kanjiToArabicForAnnotator(match.groupValues[1])
        val sub = kanjiToArabicForAnnotator(match.groupValues[2])
        if (main > 0 && sub > 0) {
            val num = "${main}_$sub"
            if (num != article.articleNumber && num in availableArticleNumbers) {
                matches.add(ArticleReferenceMatch(match.range, num))
            }
        }
    }

    // 第X条のY パターン (算用数字 — normalizeDisplay 適用後)
    val subArticleArabic = Regex("第(\\d+)条の(\\d+)")
    for (match in subArticleArabic.findAll(displayedText)) {
        val main = match.groupValues[1].toIntOrNull() ?: continue
        val sub = match.groupValues[2].toIntOrNull() ?: continue
        if (main > 0 && sub > 0) {
            val num = "${main}_$sub"
            if (num != article.articleNumber && num in availableArticleNumbers) {
                matches.add(ArticleReferenceMatch(match.range, num))
            }
        }
    }

    // 第X条 パターン (漢数字、第X条のY を除外)
    val mainArticleKanji = Regex("第([一二三四五六七八九十百千万]+)条(?!の[一二三四五六七八九十百千万])")
    for (match in mainArticleKanji.findAll(displayedText)) {
        val num = kanjiToArabicForAnnotator(match.groupValues[1])
        if (num > 0) {
            val articleNum = num.toString()
            if (articleNum != article.articleNumber && articleNum in availableArticleNumbers) {
                matches.add(ArticleReferenceMatch(match.range, articleNum))
            }
        }
    }

    // 第X条 パターン (算用数字、第X条のY を除外)
    val mainArticleArabic = Regex("第(\\d+)条(?!の\\d)")
    for (match in mainArticleArabic.findAll(displayedText)) {
        val num = match.groupValues[1].toIntOrNull() ?: continue
        if (num > 0) {
            val articleNum = num.toString()
            if (articleNum != article.articleNumber && articleNum in availableArticleNumbers) {
                matches.add(ArticleReferenceMatch(match.range, articleNum))
            }
        }
    }

    // 前X条・次X条
    if (currentNum != null) {
        val prevNPattern = Regex("前([一二三四五六七八九十百千万\\d]+)条")
        for (match in prevNPattern.findAll(displayedText)) {
            val n = match.groupValues[1].toIntOrNull()
                ?: kanjiToArabicForAnnotator(match.groupValues[1])
            if (n > 0) {
                // 最も近い条文にリンク
                val target = (currentNum - 1).toString()
                if (target in availableArticleNumbers) {
                    matches.add(ArticleReferenceMatch(match.range, target))
                }
            }
        }

        val nextNPattern = Regex("次([一二三四五六七八九十百千万\\d]+)条")
        for (match in nextNPattern.findAll(displayedText)) {
            val n = match.groupValues[1].toIntOrNull()
                ?: kanjiToArabicForAnnotator(match.groupValues[1])
            if (n > 0) {
                val target = (currentNum + 1).toString()
                if (target in availableArticleNumbers) {
                    matches.add(ArticleReferenceMatch(match.range, target))
                }
            }
        }

        // 前条 (数字なし)
        val prevPattern = Regex("前(?![一二三四五六七八九十百千万\\d])条")
        for (match in prevPattern.findAll(displayedText)) {
            val target = (currentNum - 1).toString()
            if (target in availableArticleNumbers) {
                matches.add(ArticleReferenceMatch(match.range, target))
            }
        }

        // 次条 (数字なし)
        val nextPattern = Regex("次(?![一二三四五六七八九十百千万\\d])条")
        for (match in nextPattern.findAll(displayedText)) {
            val target = (currentNum + 1).toString()
            if (target in availableArticleNumbers) {
                matches.add(ArticleReferenceMatch(match.range, target))
            }
        }
    }

    // 範囲の重複を除去（先にマッチしたものを優先）
    val result = mutableListOf<ArticleReferenceMatch>()
    for (match in matches.sortedBy { it.range.first }) {
        if (result.none { existing -> match.range.first in existing.range || match.range.last in existing.range }) {
            result.add(match)
        }
    }

    return result
}

private val kanjiDigitMapForAnnotator = mapOf(
    '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
    '六' to 6, '七' to 7, '八' to 8, '九' to 9,
)

private fun kanjiToArabicForAnnotator(kanji: String): Int {
    var result = 0
    var current = 0
    for (ch in kanji) {
        when (ch) {
            '万' -> {
                result += (if (current == 0) 1 else current) * 10000
                current = 0
            }
            '千' -> {
                result += (if (current == 0) 1 else current) * 1000
                current = 0
            }
            '百' -> {
                result += (if (current == 0) 1 else current) * 100
                current = 0
            }
            '十' -> {
                result += (if (current == 0) 1 else current) * 10
                current = 0
            }
            else -> current = kanjiDigitMapForAnnotator[ch] ?: 0
        }
    }
    return result + current
}
