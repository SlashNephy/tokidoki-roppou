@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package blue.starry.tokidokiroppou.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AI 解説のボトムシートの状態を表す。
 */
sealed interface ExplanationSheetState {
    /** 非表示 */
    data object Hidden : ExplanationSheetState

    /** 生成中 (ストリーミング開始前) */
    data object Loading : ExplanationSheetState

    /** ストリーミング中 (テキストが逐次追加される) */
    data class Streaming(val text: String) : ExplanationSheetState

    /** エラー */
    data class Error(val message: String) : ExplanationSheetState
}

/**
 * 条文の AI 解説を表示するモーダルボトムシート。
 * [uiState] に応じてローディング、ストリーミングテキスト、エラーを表示する。
 */
@Composable
fun ArticleExplanationSheet(
    uiState: ExplanationSheetState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
) {
    if (uiState is ExplanationSheetState.Hidden) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // タイトル
            Text(
                text = "AI による解説",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 免責事項 (常時表示)
            Text(
                text = "この機能は AI を利用しているため、誤りを含むことがあります。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            when (uiState) {
                is ExplanationSheetState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            LoadingIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "解説を生成中…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                is ExplanationSheetState.Streaming -> {
                    val scrollState = rememberScrollState()

                    // テキストが追加されるたびに末尾へ自動スクロール
                    LaunchedEffect(uiState.text) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                    ) {
                        Text(
                            text = uiState.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 24.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                is ExplanationSheetState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) {
                            Text("再試行")
                        }
                    }
                }

                is ExplanationSheetState.Hidden -> Unit
            }
        }
    }
}
