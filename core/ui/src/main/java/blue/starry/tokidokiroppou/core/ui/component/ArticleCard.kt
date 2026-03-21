package blue.starry.tokidokiroppou.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.ArticleReferenceMatch

@Composable
fun ArticleCard(
    article: Article,
    useHalfWidthParentheses: Boolean = false,
    referenceMatches: List<ArticleReferenceMatch> = emptyList(),
    onReferenceClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = article.lawCode.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = article.displayTitle(useHalfWidthParentheses),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                if (referenceMatches.isNotEmpty() && onReferenceClick != null) {
                    val plainText = article.fullText(useHalfWidthParentheses)
                    val annotatedText = buildAnnotatedString {
                        append(plainText)
                        for (match in referenceMatches) {
                            if (match.range.last < plainText.length) {
                                addLink(
                                    clickable = LinkAnnotation.Clickable(
                                        tag = match.articleNumber,
                                        styles = TextLinkStyles(
                                            style = SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = TextDecoration.Underline,
                                                color = linkColor,
                                            ),
                                        ),
                                    ) {
                                        onReferenceClick(match.articleNumber)
                                    },
                                    start = match.range.first,
                                    end = match.range.last + 1,
                                )
                            }
                        }
                    }

                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = article.fullText(useHalfWidthParentheses),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
