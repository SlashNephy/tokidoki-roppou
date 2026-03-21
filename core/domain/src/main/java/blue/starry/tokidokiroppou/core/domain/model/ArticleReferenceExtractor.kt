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
    val subArticlePattern = Regex("第([〇一二三四五六七八九十百千万]+)条の([〇一二三四五六七八九十百千万]+)")
    for (match in subArticlePattern.findAll(text)) {
        val main = kanjiToArabicNumber(match.groupValues[1])
        val sub = kanjiToArabicNumber(match.groupValues[2])
        if (main > 0 && sub > 0) {
            refs.add("${main}_$sub")
        }
    }

    // 第X条 パターン (第X条のY は除外)
    val articlePattern = Regex("第([〇一二三四五六七八九十百千万]+)条(?!の[一二三四五六七八九十百千万])")
    for (match in articlePattern.findAll(text)) {
        val num = kanjiToArabicNumber(match.groupValues[1])
        if (num > 0) {
            refs.add(num.toString())
        }
    }

    // 前条・次条・前X条・次X条
    val currentNum = article.articleNumber.split("_")[0].toIntOrNull()
    if (currentNum != null) {
        // 前X条 (例: 「前2条」→ 現在の条文-1, 現在の条文-2)
        val prevNPattern = Regex("前([一二三四五六七八九十百千万\\d]+)条")
        for (match in prevNPattern.findAll(text)) {
            val n = match.groupValues[1].toIntOrNull()
                ?: kanjiToArabicNumber(match.groupValues[1])
            if (n > 0) {
                for (i in 1..n) {
                    val target = currentNum - i
                    if (target > 0) refs.add(target.toString())
                }
            }
        }

        // 次X条 (例: 「次2条」→ 現在の条文+1, 現在の条文+2)
        val nextNPattern = Regex("次([一二三四五六七八九十百千万\\d]+)条")
        for (match in nextNPattern.findAll(text)) {
            val n = match.groupValues[1].toIntOrNull()
                ?: kanjiToArabicNumber(match.groupValues[1])
            if (n > 0) {
                for (i in 1..n) {
                    refs.add((currentNum + i).toString())
                }
            }
        }

        // 前条・次条 (数字なし、前X条パターンにマッチしないもの)
        if (Regex("前(?![一二三四五六七八九十百千万\\d])条").containsMatchIn(text)) {
            refs.add((currentNum - 1).toString())
        }
        if (Regex("次(?![一二三四五六七八九十百千万\\d])条").containsMatchIn(text)) {
            refs.add((currentNum + 1).toString())
        }
    }

    // 自身を除外
    refs.remove(article.articleNumber)
    return refs.toList()
}

private val kanjiDigitMap = mapOf(
    '〇' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
    '六' to 6, '七' to 7, '八' to 8, '九' to 9,
)

private fun kanjiToArabicNumber(kanji: String): Int {
    if ('〇' in kanji) {
        return kanji.fold(0) { acc, ch -> acc * 10 + (kanjiDigitMap[ch] ?: 0) }
    }

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
