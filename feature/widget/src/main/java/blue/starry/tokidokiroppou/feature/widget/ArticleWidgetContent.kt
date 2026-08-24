package blue.starry.tokidokiroppou.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.data.R as CoreDataR

/**
 * ウィジェットの本体。
 * article が null のときはフォールバック表示にする。
 */
@Composable
fun ArticleWidgetContent(
    article: Article?,
    lawDisplayName: String,
    useHalfWidthParentheses: Boolean,
    launchAction: Action,
) {
    Scaffold(
        titleBar = {
            TitleBar(
                startIcon = ImageProvider(CoreDataR.drawable.ic_notification),
                title = lawDisplayName,
                actions = {
                    RefreshArticleButton()
                },
                modifier = GlanceModifier.clickable(launchAction),
            )
        },
        modifier = GlanceModifier.fillMaxSize().padding(bottom = 12.dp),
    ) {
        when (article) {
            null -> EmptyContent(
                modifier = GlanceModifier.clickable(launchAction),
            )
            else -> ArticleContent(
                article = article,
                useHalfWidthParentheses = useHalfWidthParentheses,
                modifier = GlanceModifier.clickable(launchAction),
            )
        }
    }
}

@Composable
fun RefreshArticleButton(modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current

    CircleIconButton(
        imageProvider = ImageProvider(R.drawable.ic_refresh),
        contentDescription = context.getString(R.string.widget_refresh_content_description),
        backgroundColor = null,
        onClick = actionRunCallback<RefreshArticleAction>(),
        modifier = modifier,
    )
}

@Composable
fun ArticleContent(
    article: Article,
    useHalfWidthParentheses: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = article.displayTitle(useHalfWidthParentheses),
            style = WidgetTextStyle.Overline,
            maxLines = 1,
        )

        // 本文は長さがまちまちなのでスクロールさせる。Glance に汎用の縦スクロール修飾子は
        // なく、LazyColumn (RemoteViews の ListView) を使う。1 アセットで足りるので
        // 段落ごとに分けず単一アイテムに入れ、Article.fullText の整形をそのまま使う。
        //
        // 引き換えに、ListView がタッチを消費するため本文タップではアプリを開けない。
        // 遷移は TitleBar と条文名の行が受ける (どちらも ListView の外)。
        //
        // itemId を明示するとウィジェット更新をまたいでスクロール位置が保たれるが、
        // これが効くのは API 31 以降。それ未満では更新のたびに先頭へ戻る。
        LazyColumn(modifier = GlanceModifier.defaultWeight().padding(top = 4.dp)) {
            item(itemId = ARTICLE_BODY_ITEM_ID) {
                Text(
                    text = article.fullText(useHalfWidthParentheses),
                    style = WidgetTextStyle.Headline,
                )
            }
        }
    }
}

@Composable
fun EmptyContent(modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.widget_empty_message),
            style = WidgetTextStyle.Headline,
        )
    }
}

/** 本文アイテムの ID。Glance の予約範囲 (-2^62 以下) を避ける */
private const val ARTICLE_BODY_ITEM_ID = 1L
