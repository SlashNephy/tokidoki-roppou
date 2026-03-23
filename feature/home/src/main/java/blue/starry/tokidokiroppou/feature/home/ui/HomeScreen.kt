@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package blue.starry.tokidokiroppou.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.buildAnnotatedArticleText
import blue.starry.tokidokiroppou.core.domain.model.normalizeDisplay
import blue.starry.tokidokiroppou.core.ui.component.ArticleCard
import blue.starry.tokidokiroppou.core.ui.component.ArticleExplanationSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    lawCode: String? = null,
    articleNumber: String? = null,
    supplementaryProvisionLabel: String? = null,
    showRefreshFab: Boolean = true,
    onNavigateToSettings: (() -> Unit)? = null,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    explanationViewModel: ArticleExplanationViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.loadArticle(lawCode, articleNumber, supplementaryProvisionLabel)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val explanationSheetState by explanationViewModel.sheetState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LoadingIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "条文を取得中…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            is HomeUiState.Loaded -> {
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                val relatedCardPositions = remember { mutableStateMapOf<String, Int>() }

                val annotatedArticleText = remember(state.article, state.relatedArticles, state.useHalfWidthParentheses) {
                    buildAnnotatedArticleText(
                        displayedText = state.article.fullText(state.useHalfWidthParentheses),
                        article = state.article,
                        relatedArticles = state.relatedArticles,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ArticleCard(
                        article = state.article,
                        useHalfWidthParentheses = state.useHalfWidthParentheses,
                        annotatedArticleText = annotatedArticleText,
                        onReferenceClick = { articleNumber ->
                            val targetY = relatedCardPositions[articleNumber] ?: return@ArticleCard
                            coroutineScope.launch {
                                scrollState.animateScrollTo(targetY)
                            }
                        },
                        isBookmarked = isBookmarked,
                        onBookmarkClick = { viewModel.toggleBookmark() },
                        onExplainClick = { explanationViewModel.explainArticle(state.article) },
                    )

                    state.lawMetadata?.let { metadata ->
                        LawAmendmentInfo(
                            metadata = metadata,
                            useHalfWidthParentheses = state.useHalfWidthParentheses,
                        )
                    }

                    if (state.relatedArticles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "関連条文",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        state.relatedArticles.forEach { related ->
                            ArticleCard(
                                article = related,
                                useHalfWidthParentheses = state.useHalfWidthParentheses,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    relatedCardPositions[related.articleNumber] =
                                        coordinates.positionInParent().y.roundToInt()
                                },
                            )
                        }
                    }

                    state.lawMetadata?.let { metadata ->
                        Spacer(modifier = Modifier.height(4.dp))
                        DataSourceFooter(metadata = metadata)
                    }
                }
            }

            is HomeUiState.NoLawSelected -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "法令が選択されていません",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "設定画面で通知対象の法令を選択してください",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        if (onNavigateToSettings != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToSettings) {
                                Text("設定を開く")
                            }
                        }
                    }
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "右下のボタンで再試行できます",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (showRefreshFab) {
            FloatingActionButton(
                onClick = { viewModel.loadRandomArticle() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "別の条文を表示",
                )
            }
        }

        // AI 解説ボトムシート
        ArticleExplanationSheet(
            uiState = explanationSheetState,
            onDismiss = { explanationViewModel.dismissSheet() },
            onRetry = { explanationViewModel.retry() },
            onRefresh = { explanationViewModel.refreshExplanation() },
        )
    }
}

@Composable
private fun LawAmendmentInfo(
    metadata: LawMetadata,
    useHalfWidthParentheses: Boolean,
) {
    val lawNumText = metadata.lastAmendmentLawNum ?: metadata.lawNum
    val dateText = metadata.lastAmendmentDate?.let { "${formatIsoDate(it)}改正" }
        ?: metadata.promulgationDate?.let { "${formatIsoDate(it)}公布" }
    val metadataText = if (dateText != null) "$lawNumText・$dateText" else lawNumText
    val displayMetadata = if (useHalfWidthParentheses) metadataText.normalizeDisplay() else metadataText

    SelectionContainer {
        Text(
            text = displayMetadata,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DataSourceFooter(metadata: LawMetadata) {
    Text(
        text = "法令データはデジタル庁の提供する、e-Gov 法令 API より取得されています。\n(取得日時: ${formatTimestamp(metadata.lastRefreshedAt)})",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

private val isoDateFormat = Regex("(\\d{4})-(\\d{2})-(\\d{2})")

private fun formatIsoDate(isoDate: String): String {
    val match = isoDateFormat.matchEntire(isoDate) ?: return isoDate
    val year = match.groupValues[1].toIntOrNull() ?: return isoDate
    val month = match.groupValues[2].toIntOrNull() ?: return isoDate
    val day = match.groupValues[3].toIntOrNull() ?: return isoDate
    return "${year}年${month}月${day}日"
}

private fun formatTimestamp(millis: Long): String {
    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    return format.format(Date(millis))
}
