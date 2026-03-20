package blue.starry.tokidokiroppou.core.domain.model

fun String.normalizeDisplay(): String {
    return normalizeParentheses().convertLegalKanjiNumbers()
}

// 文中の全角かっこを半角に変換し、前後にスペースを挿入する。
// ただし文頭の「（」や文末の「）」、既にスペースがある場合はスペースを追加しない。
private fun String.normalizeParentheses(): String {
    val sb = StringBuilder(length)
    for (i in indices) {
        when (this[i]) {
            '（' -> {
                // 前の文字が存在し、スペースでなければスペースを挿入
                if (i > 0 && sb.isNotEmpty() && sb.last() != ' ' && sb.last() != '\n') {
                    sb.append(' ')
                }
                sb.append('(')
            }
            '）' -> {
                sb.append(')')
                // 後の文字が存在し、スペースや改行でなければスペースを挿入
                val next = getOrNull(i + 1)
                if (next != null && next != ' ' && next != '\n' && next != '。' && next != '、' && next != '）') {
                    sb.append(' ')
                }
            }
            else -> sb.append(this[i])
        }
    }
    return sb.toString()
}

// 第X条, 第X項, 第X号 など法令番号パターンの漢数字のみを算用数字に変換する。
// 「第三者」「第一審」などは法令接尾辞が続かないため変換されない。
private val legalNumberPattern =
    Regex("第([一二三四五六七八九十百千万]+)(条|項|号|編|章|節|款|目)")

// 条の二, 項の二 などの枝番号
private val subNumberPattern =
    Regex("(条の|項の|号の)([一二三四五六七八九十百千万]+)")

// LawNum 用: 「昭和二十三年法律第百二十号」→「昭和23年法律第120号」
private val eraYearPattern =
    Regex("(明治|大正|昭和|平成|令和)([一二三四五六七八九十百千万]+)年")

private val lawNumNumberPattern =
    Regex("第([一二三四五六七八九十百千万]+)号")

private fun String.convertLegalKanjiNumbers(): String {
    var result = legalNumberPattern.replace(this) { match ->
        "第${kanjiToArabic(match.groupValues[1])}${match.groupValues[2]}"
    }
    result = subNumberPattern.replace(result) { match ->
        "${match.groupValues[1]}${kanjiToArabic(match.groupValues[2])}"
    }
    result = eraYearPattern.replace(result) { match ->
        "${match.groupValues[1]}${kanjiToArabic(match.groupValues[2])}年"
    }
    result = lawNumNumberPattern.replace(result) { match ->
        "第${kanjiToArabic(match.groupValues[1])}号"
    }
    return result
}

private val kanjiDigitMap = mapOf(
    '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
    '六' to 6, '七' to 7, '八' to 8, '九' to 9,
)

private fun kanjiToArabic(kanji: String): Int {
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
