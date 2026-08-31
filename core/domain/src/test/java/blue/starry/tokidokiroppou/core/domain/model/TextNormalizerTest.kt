package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TextNormalizerTest {
    @Test
    fun normalizeDisplayConvertsKanjiYearDuration() {
        assertEquals(
            "地代を支払うべきときは、1年前に予告をし、",
            "地代を支払うべきときは、一年前に予告をし、".normalizeDisplay(),
        )
        assertEquals(
            "20年以上50年以下の範囲内において",
            "二十年以上五十年以下の範囲内において".normalizeDisplay(),
        )
    }

    @Test
    fun normalizeDisplayConvertsKanjiYearWithEraName() {
        assertEquals("昭和23年法律第120号", "昭和二十三年法律第百二十号".normalizeDisplay())
    }

    @Test
    fun normalizeDisplayConvertsKanjiKagenen() {
        assertEquals("3箇年", "三箇年".normalizeDisplay())
    }

    @Test
    fun normalizeDisplayConvertsFractionEndingWithYear() {
        assertEquals("2分の1年", "二分の一年".normalizeDisplay())
    }

    @Test
    fun normalizeDisplayKeepsApproximateYearExpressions() {
        assertEquals("数十年にわたり", "数十年にわたり".normalizeDisplay())
        assertEquals("何百年もの間", "何百年もの間".normalizeDisplay())
    }

    @Test
    fun normalizeDisplayKeepsWordsEndingWithYearCharacter() {
        assertEquals("未成年者は、令和元年から", "未成年者は、令和元年から".normalizeDisplay())
    }

    @Test
    fun normalizeDisplayConvertsLegalArticleNumbers() {
        assertEquals("第268条第2項", "第二百六十八条第二項".normalizeDisplay())
    }

    @Test
    fun normalizeDisplayNormalizesFullWidthParentheses() {
        assertEquals("地上権 (物権) の一種", "地上権（物権）の一種".normalizeDisplay())
    }
}
