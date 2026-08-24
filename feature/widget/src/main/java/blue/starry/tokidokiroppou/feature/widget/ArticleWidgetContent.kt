package blue.starry.tokidokiroppou.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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
    val context = LocalContext.current

    Scaffold(
        titleBar = {
            TitleBar(
                startIcon = ImageProvider(CoreDataR.drawable.ic_notification),
                title = lawDisplayName,
                actions = {
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = context.getString(R.string.widget_refresh_content_description),
                        backgroundColor = null,
                        contentColor = GlanceTheme.colors.onSurface,
                        onClick = actionRunCallback<RefreshArticleAction>(),
                    )
                },
            )
        },
    ) {
        if (article == null) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = context.getString(R.string.widget_empty_message),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                )
            }
            return@Scaffold
        }

        Column(modifier = GlanceModifier.fillMaxSize().clickable(launchAction)) {
            Text(
                text = article.displayTitle(useHalfWidthParentheses),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = article.fullText(useHalfWidthParentheses),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                ),
                modifier = GlanceModifier.padding(top = 6.dp),
            )
        }
    }
}
