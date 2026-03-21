package blue.starry.tokidokiroppou.core.domain.model

/**
 * 出力テキスト中の条文参照箇所の位置と参照先を表す。
 */
data class ArticleReferenceMatch(
    val range: IntRange,
    val articleNumber: String,
)

/**
 * キャプション挿入済みテキストとリンク情報をまとめたデータ。
 */
data class AnnotatedArticleText(
    val text: String,
    val references: List<ArticleReferenceMatch>,
)

/**
 * 条文テキスト中の参照箇所を検出し、関連条文のキャプション（別名）を
 * 〈〉 で挿入したテキストと、クリック可能なリンク情報を返す。
 *
 * 例: 「前条の規定により」→「前条〈短期賃貸借〉の規定により」
 */
fun buildAnnotatedArticleText(
    displayedText: String,
    article: Article,
    relatedArticles: List<Article>,
): AnnotatedArticleText {
    val articleMap = relatedArticles.associateBy { it.articleNumber }
    val currentNum = article.articleNumber.split("_")[0].toIntOrNull()

    val rawMatches = mutableListOf<RawMatch>()

    // 第X条のY (漢数字)
    val subKanji = Regex("第([一二三四五六七八九十百千万]+)条の([一二三四五六七八九十百千万]+)")
    for (m in subKanji.findAll(displayedText)) {
        val main = kanjiToArabicForAnnotator(m.groupValues[1])
        val sub = kanjiToArabicForAnnotator(m.groupValues[2])
        if (main > 0 && sub > 0) {
            val num = "${main}_$sub"
            if (num != article.articleNumber) {
                rawMatches.add(RawMatch(m.range, num, listOf(num)))
            }
        }
    }

    // 第X条のY (算用数字)
    val subArabic = Regex("第(\\d+)条の(\\d+)")
    for (m in subArabic.findAll(displayedText)) {
        val main = m.groupValues[1].toIntOrNull() ?: continue
        val sub = m.groupValues[2].toIntOrNull() ?: continue
        if (main > 0 && sub > 0) {
            val num = "${main}_$sub"
            if (num != article.articleNumber) {
                rawMatches.add(RawMatch(m.range, num, listOf(num)))
            }
        }
    }

    // 第X条 (漢数字、第X条のY を除外)
    val mainKanji = Regex("第([一二三四五六七八九十百千万]+)条(?!の[一二三四五六七八九十百千万])")
    for (m in mainKanji.findAll(displayedText)) {
        val num = kanjiToArabicForAnnotator(m.groupValues[1])
        if (num > 0) {
            val articleNum = num.toString()
            if (articleNum != article.articleNumber) {
                rawMatches.add(RawMatch(m.range, articleNum, listOf(articleNum)))
            }
        }
    }

    // 第X条 (算用数字、第X条のY を除外)
    val mainArabic = Regex("第(\\d+)条(?!の\\d)")
    for (m in mainArabic.findAll(displayedText)) {
        val num = m.groupValues[1].toIntOrNull() ?: continue
        if (num > 0) {
            val articleNum = num.toString()
            if (articleNum != article.articleNumber) {
                rawMatches.add(RawMatch(m.range, articleNum, listOf(articleNum)))
            }
        }
    }

    // 前条・次条・前N条・次N条
    if (currentNum != null) {
        // 前N条 (例: 前2条 → 2つ前までの条文すべてのキャプションを表示)
        val prevN = Regex("前([一二三四五六七八九十百千万\\d]+)条")
        for (m in prevN.findAll(displayedText)) {
            val n = m.groupValues[1].toIntOrNull()
                ?: kanjiToArabicForAnnotator(m.groupValues[1])
            if (n > 0) {
                val targets = (1..n)
                    .map { (currentNum - it).toString() }
                    .filter { it.toIntOrNull()?.let { v -> v > 0 } == true }
                    .reversed() // 昇順 (条番号の小さい順)
                val primary = (currentNum - 1).toString()
                rawMatches.add(RawMatch(m.range, primary, targets))
            }
        }

        // 次N条
        val nextN = Regex("次([一二三四五六七八九十百千万\\d]+)条")
        for (m in nextN.findAll(displayedText)) {
            val n = m.groupValues[1].toIntOrNull()
                ?: kanjiToArabicForAnnotator(m.groupValues[1])
            if (n > 0) {
                val targets = (1..n).map { (currentNum + it).toString() }
                val primary = (currentNum + 1).toString()
                rawMatches.add(RawMatch(m.range, primary, targets))
            }
        }

        // 前条 (数字なし)
        val prev = Regex("前(?![一二三四五六七八九十百千万\\d])条")
        for (m in prev.findAll(displayedText)) {
            val target = (currentNum - 1).toString()
            rawMatches.add(RawMatch(m.range, target, listOf(target)))
        }

        // 次条 (数字なし)
        val next = Regex("次(?![一二三四五六七八九十百千万\\d])条")
        for (m in next.findAll(displayedText)) {
            val target = (currentNum + 1).toString()
            rawMatches.add(RawMatch(m.range, target, listOf(target)))
        }
    }

    // 範囲の重複を除去（先にマッチした長いものを優先）
    val sorted = rawMatches.sortedBy { it.range.first }
    val deduped = mutableListOf<RawMatch>()
    for (match in sorted) {
        if (deduped.none { existing ->
                match.range.first in existing.range || match.range.last in existing.range
            }) {
            deduped.add(match)
        }
    }

    // キャプション挿入済みテキストとリンク情報を構築
    val sb = StringBuilder()
    val references = mutableListOf<ArticleReferenceMatch>()
    var lastEnd = 0

    for (raw in deduped) {
        // マッチ前のテキストをコピー
        sb.append(displayedText, lastEnd, raw.range.first)

        val refStart = sb.length

        // 参照テキスト本体をコピー
        sb.append(displayedText, raw.range.first, raw.range.last + 1)

        // 利用可能なキャプションを収集して 〈〉 で挿入
        val captions = raw.captionArticleNumbers.mapNotNull { num ->
            articleMap[num]?.articleCaption?.stripParentheses()?.takeIf { it.isNotEmpty() }
        }
        if (captions.isNotEmpty()) {
            sb.append("〈")
            sb.append(captions.joinToString("、"))
            sb.append("〉")
        }

        val refEnd = sb.length

        // リンクターゲットが利用可能な場合のみクリック可能にする
        if (raw.primaryArticleNumber in articleMap) {
            references.add(
                ArticleReferenceMatch(
                    range = refStart until refEnd,
                    articleNumber = raw.primaryArticleNumber,
                ),
            )
        }

        lastEnd = raw.range.last + 1
    }

    // 残りのテキストをコピー
    sb.append(displayedText, lastEnd, displayedText.length)

    return AnnotatedArticleText(
        text = sb.toString(),
        references = references,
    )
}

private data class RawMatch(
    val range: IntRange,
    val primaryArticleNumber: String,
    val captionArticleNumbers: List<String>,
)

private fun String.stripParentheses(): String {
    var s = trim()
    if (s.startsWith("（") && s.endsWith("）")) {
        s = s.removePrefix("（").removeSuffix("）")
    } else if (s.startsWith("(") && s.endsWith(")")) {
        s = s.removePrefix("(").removeSuffix(")")
    }
    return s.trim()
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
