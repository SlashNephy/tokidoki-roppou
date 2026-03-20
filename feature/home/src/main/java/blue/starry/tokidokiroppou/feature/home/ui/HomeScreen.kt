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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.normalizeDisplay
import blue.starry.tokidokiroppou.core.ui.component.ArticleCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadRandomArticle() },
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "別の条文を表示",
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "条文を取得中…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            is HomeUiState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ArticleCard(
                        article = state.article,
                        useHalfWidthParentheses = state.useHalfWidthParentheses,
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
                            )
                        }
                    }

                    state.lawMetadata?.let { metadata ->
                        Spacer(modifier = Modifier.height(4.dp))
                        DataSourceFooter(metadata = metadata)
                    }
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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

@Composable
private fun DataSourceFooter(metadata: LawMetadata) {
    Text(
        text = "法令データはデジタル庁の提供する、e-Gov 法令 API より取得されています。 (取得日時: ${formatTimestamp(metadata.lastRefreshedAt)})",
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
