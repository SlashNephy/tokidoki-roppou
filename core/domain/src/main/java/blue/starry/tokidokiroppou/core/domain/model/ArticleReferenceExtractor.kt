package blue.starry.tokidokiroppou.core.domain.model

/**
 * 条文テキストから同一法令内の条文参照 (第X条, 第X条のY) を抽出し、
 * DB の articleNumber 形式 ("27", "27_3") に変換して返す。
 * 表示元の条文自身は除外する。
 */
fun extractArticleReferences(article: Article): List<String> {
    val text = article.fullText
    val refs = mutableSetOf<String>()

    // 第X条のY パターン (先にマッチさせる)
    val subArticlePattern = Regex("第([一二三四五六七八九十百千万]+)条の([一二三四五六七八九十百千万]+)")
    for (match in subArticlePattern.findAll(text)) {
        val main = kanjiToArabicNumber(match.groupValues[1])
        val sub = kanjiToArabicNumber(match.groupValues[2])
        if (main > 0 && sub > 0) {
            refs.add("${main}_$sub")
        }
    }

    // 第X条 パターン (第X条のY は除外)
    val articlePattern = Regex("第([一二三四五六七八九十百千万]+)条(?!の[一二三四五六七八九十百千万])")
    for (match in articlePattern.findAll(text)) {
        val num = kanjiToArabicNumber(match.groupValues[1])
        if (num > 0) {
            refs.add(num.toString())
        }
    }

    // 前条・次条
    val currentNum = article.articleNumber.split("_")[0].toIntOrNull()
    if (currentNum != null) {
        if ("前条" in text) {
            refs.add((currentNum - 1).toString())
        }
        if ("次条" in text) {
            refs.add((currentNum + 1).toString())
        }
    }

    // 自身を除外
    refs.remove(article.articleNumber)
    return refs.toList()
}

private val kanjiDigitMap = mapOf(
    '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
    '六' to 6, '七' to 7, '八' to 8, '九' to 9,
)

private fun kanjiToArabicNumber(kanji: String): Int {
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
            else -> current = kanjiDigitMap[ch] ?: 0
        }
    }
    return result + current
}
