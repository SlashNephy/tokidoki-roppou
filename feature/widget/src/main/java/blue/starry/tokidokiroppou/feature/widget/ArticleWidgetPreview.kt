package blue.starry.tokidokiroppou.feature.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.action.action
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId

/**
 * IDE 上でウィジェットの見た目を確認するための Preview。
 *
 * Glance は RemoteViews へコンパイルされるため実機での見た目を完全には再現しないが、
 * 条文名のクランプや本文の溢れ方といったレイアウトの当たりを取るのには使える。
 * 実際の配置・更新の挙動は実機で確認する必要がある。
 *
 * Preview 関数は Android Studio がリフレクションで呼び出すため、引数を取らない。
 */

/** 3x2 セル相当。既定サイズでの見た目 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 150)
@Composable
private fun ArticleWidgetContentPreview() {
    GlanceTheme {
        ArticleWidgetContent(
            article = previewArticle,
            lawDisplayName = "日本国憲法",
            useHalfWidthParentheses = false,
            launchAction = action {},
        )
    }
}

/** 条文名が横幅に収まらない場合。1 行にクランプされることを確認する */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 150)
@Composable
private fun ArticleWidgetContentLongTitlePreview() {
    GlanceTheme {
        ArticleWidgetContent(
            article = previewLongTitleArticle,
            lawDisplayName = "民事訴訟法",
            useHalfWidthParentheses = false,
            launchAction = action {},
        )
    }
}

/** 法令名が TitleBar に収まらない場合。TitleBar 側の省略を確認する */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 110)
@Composable
private fun ArticleWidgetContentNarrowPreview() {
    GlanceTheme {
        ArticleWidgetContent(
            article = previewLongTitleArticle,
            lawDisplayName = "私的独占の禁止及び公正取引の確保に関する法律",
            useHalfWidthParentheses = false,
            launchAction = action {},
        )
    }
}

/** 「読みやすい表記にする」を有効にした場合 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 150)
@Composable
private fun ArticleWidgetContentHalfWidthPreview() {
    GlanceTheme {
        ArticleWidgetContent(
            article = previewLongTitleArticle,
            lawDisplayName = "民事訴訟法",
            useHalfWidthParentheses = true,
            launchAction = action {},
        )
    }
}

/** 条文を解決できなかった場合のフォールバック */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 150)
@Composable
private fun ArticleWidgetContentEmptyPreview() {
    GlanceTheme {
        ArticleWidgetContent(
            article = null,
            lawDisplayName = "ときどき六法",
            useHalfWidthParentheses = false,
            launchAction = action {},
        )
    }
}

private val previewArticle = Article(
    lawId = LawId("321CONSTITUTION"),
    articleNumber = "9",
    articleTitle = "第九条",
    articleCaption = "（戦争の放棄）",
    paragraphs = listOf(
        Article.Paragraph(
            number = 1,
            text = "日本国民は、正義と秩序を基調とする国際平和を誠実に希求し、" +
                "国権の発動たる戦争と、武力による威嚇又は武力の行使は、" +
                "国際紛争を解決する手段としては、永久にこれを放棄する。",
        ),
        Article.Paragraph(
            number = 2,
            text = "前項の目的を達するため、陸海空軍その他の戦力は、これを保持しない。" +
                "国の交戦権は、これを認めない。",
        ),
    ),
)

private val previewLongTitleArticle = Article(
    lawId = LawId("408AC0000000109"),
    articleNumber = "215_3",
    articleTitle = "第二百十五条の三",
    articleCaption = "（映像等の送受信による通話の方法による陳述）",
    paragraphs = listOf(
        Article.Paragraph(
            number = 1,
            text = "裁判所は、鑑定人に口頭で意見を述べさせる場合において、相当と認めるときは、" +
                "最高裁判所規則で定めるところにより、映像と音声の送受信により相手の状態を" +
                "相互に認識しながら通話をすることができる方法によって、意見を述べさせることができる。",
        ),
    ),
)
