@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package blue.starry.tokidokiroppou.feature.laws.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading
import blue.starry.tokidokiroppou.core.domain.model.normalizeDisplay

@Composable
fun LawsScreen(
    onArticleClick: (LawCode, String, String?) -> Unit,
    viewModel: LawsScreenViewModel = hiltViewModel(),
) {
    val lawMetadata by viewModel.lawMetadata.collectAsStateWithLifecycle()
    val useHalfWidth by viewModel.useHalfWidthParentheses.collectAsStateWithLifecycle()
    val expandedLaw by viewModel.expandedLaw.collectAsStateWithLifecycle()
    val structuredContent by viewModel.structuredContent.collectAsStateWithLifecycle()
    val collapsedHeadings by viewModel.collapsedHeadings.collectAsStateWithLifecycle()
    val loadingLaw by viewModel.loadingLaw.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    LawsContent(
        lawMetadata = lawMetadata,
        useHalfWidth = useHalfWidth,
        expandedLaw = expandedLaw,
        structuredContent = structuredContent,
        collapsedHeadings = collapsedHeadings,
        loadingLaw = loadingLaw,
        searchQuery = searchQuery,
        searchResults = searchResults,
        isSearching = isSearching,
        onSearchQueryChanged = viewModel::updateSearchQuery,
        onLawClick = viewModel::toggleLaw,
        onHeadingClick = viewModel::toggleHeading,
        onArticleClick = onArticleClick,
        getFilteredLawCodes = viewModel::getFilteredLawCodes,
        getArticleCount = viewModel::getArticleCount,
        getVisibleContent = viewModel::getVisibleContent,
    )
}

@Composable
private fun LawsContent(
    lawMetadata: Map<LawCode, LawMetadata>,
    useHalfWidth: Boolean,
    expandedLaw: LawCode?,
    structuredContent: Map<LawCode, List<LawContentItem>>,
    collapsedHeadings: Map<LawCode, Set<Int>>,
    loadingLaw: LawCode?,
    searchQuery: String,
    searchResults: Map<LawCode, List<Article>>?,
    isSearching: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onLawClick: (LawCode) -> Unit,
    onHeadingClick: (LawCode, Int) -> Unit,
    onArticleClick: (LawCode, String, String?) -> Unit,
    getFilteredLawCodes: (LawCategory) -> List<LawCode>,
    getArticleCount: (LawCode) -> Int?,
    getVisibleContent: (LawCode) -> List<LawContentItem>,
) {
    val isInSearchMode = searchQuery.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("法令名・条文番号・条文内容で検索") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "クリア",
                            )
                        }
                    }
                },
                singleLine = true,
            )
        }

        if (isSearching) {
            item(key = "searching") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        LawCategory.entries.forEach { category ->
            val lawCodes = getFilteredLawCodes(category)
            if (lawCodes.isEmpty()) return@forEach

            item(key = "header_${category.name}") {
                CategoryHeader(
                    category = category,
                )
            }

            lawCodes.forEach { lawCode ->
                val isExpanded = expandedLaw == lawCode
                val metadata = lawMetadata[lawCode]
                val lawContent = structuredContent[lawCode]
                val isLoading = loadingLaw == lawCode
                val matchedArticles = if (isInSearchMode) searchResults?.get(lawCode) else null

                item(key = "law_${lawCode.name}") {
                    LawHeader(
                        lawCode = lawCode,
                        metadata = metadata,
                        useHalfWidth = useHalfWidth,
                        isExpanded = isExpanded,
                        articleCount = getArticleCount(lawCode),
                        matchedCount = matchedArticles?.size,
                        isLoading = isLoading,
                        isSearchMode = isInSearchMode,
                        onClick = { onLawClick(lawCode) },
                    )
                }

                // 検索モード: 検索結果の条文のみ表示（ヘッダーなし）
                if (isInSearchMode && matchedArticles != null) {
                    itemsIndexed(
                        items = matchedArticles,
                        key = { index, _ -> "${lawCode.name}_search_${index}" },
                    ) { _, article ->
                        ArticleListItem(
                            article = article,
                            useHalfWidth = useHalfWidth,
                            onClick = { onArticleClick(lawCode, article.articleNumber, article.supplementaryProvisionLabel) },
                        )
                    }
                }

                // 展開モード: 構造見出し + 条文をインターリーブ表示（折りたたみ対応）
                if (!isInSearchMode && isExpanded && lawContent != null) {
                    val visibleContent = getVisibleContent(lawCode)
                    itemsIndexed(
                        items = visibleContent,
                        key = { _, item -> "${lawCode.name}_content_${item.orderIndex}" },
                    ) { index, item ->
                        when (item) {
                            is LawContentItem.Heading -> {
                                // リスト末尾の見出しのみ区切り線を非表示
                                val nextItem = visibleContent.getOrNull(index + 1)
                                val showDivider = nextItem != null
                                StructureHeadingItem(
                                    heading = item.heading,
                                    useHalfWidth = useHalfWidth,
                                    isCollapsed = item.orderIndex in (collapsedHeadings[lawCode] ?: emptySet()),
                                    showDivider = showDivider,
                                    onClick = { onHeadingClick(lawCode, item.orderIndex) },
                                )
                            }
                            is LawContentItem.ArticleItem -> {
                                // 次のアイテムが見出しまたはリスト末尾なら区切り線を非表示
                                val nextItem = visibleContent.getOrNull(index + 1)
                                val showDivider = nextItem is LawContentItem.ArticleItem
                                ArticleListItem(
                                    article = item.article,
                                    useHalfWidth = useHalfWidth,
                                    showDivider = showDivider,
                                    onClick = { onArticleClick(lawCode, item.article.articleNumber, item.article.supplementaryProvisionLabel) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isInSearchMode && !isSearching && searchResults != null) {
            val totalHits = searchResults.values.sumOf { it.size }
            item(key = "search_summary") {
                Text(
                    text = if (totalHits > 0) {
                        "${searchResults.size}件の法令から${totalHits}条がヒット"
                    } else {
                        "該当する条文が見つかりませんでした"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "footer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryHeader(
    category: LawCategory,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LawHeader(
    lawCode: LawCode,
    metadata: LawMetadata?,
    useHalfWidth: Boolean,
    isExpanded: Boolean,
    articleCount: Int?,
    matchedCount: Int?,
    isLoading: Boolean,
    isSearchMode: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded || (isSearchMode && matchedCount != null)) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lawCode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                metadata?.let {
                    val lawNum = if (useHalfWidth) it.lawNum.normalizeDisplay() else it.lawNum
                    Text(
                        text = lawNum,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (isSearchMode && matchedCount != null) {
                    Text(
                        text = "${matchedCount}条がヒット",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (articleCount != null) {
                    Text(
                        text = "${articleCount}条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (isLoading) {
                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                LoadingIndicator(
                    modifier = Modifier.size(24.dp),
                )
            } else if (!isSearchMode) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "閉じる" else "開く",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 構造見出し（編・章・節など）の表示コンポーネント。タップで配下を折りたたみ可能。 */
@Composable
private fun StructureHeadingItem(
    heading: StructureHeading,
    useHalfWidth: Boolean,
    isCollapsed: Boolean,
    showDivider: Boolean = true,
    onClick: () -> Unit,
) {
    val startPadding = 24.dp + (heading.level.depth * 8).dp
    val title = if (useHalfWidth) heading.title.normalizeDisplay() else heading.title

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = startPadding, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = when (heading.level) {
                StructureHeading.Level.Part,
                StructureHeading.Level.SupplementaryProvision -> MaterialTheme.typography.titleSmall
                StructureHeading.Level.Chapter -> MaterialTheme.typography.titleSmall
                else -> MaterialTheme.typography.bodyMedium
            },
            fontWeight = when (heading.level) {
                StructureHeading.Level.Part,
                StructureHeading.Level.SupplementaryProvision -> FontWeight.Bold
                StructureHeading.Level.Chapter -> FontWeight.SemiBold
                else -> FontWeight.Medium
            },
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (isCollapsed) "展開" else "折りたたみ",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = startPadding, end = 16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun ArticleListItem(
    article: Article,
    useHalfWidth: Boolean,
    showDivider: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Article,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.displayTitle(useHalfWidth),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val firstParagraph = article.paragraphs.firstOrNull()?.text.orEmpty()
            val previewText = if (useHalfWidth) firstParagraph.normalizeDisplay() else firstParagraph
            if (previewText.isNotEmpty()) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
