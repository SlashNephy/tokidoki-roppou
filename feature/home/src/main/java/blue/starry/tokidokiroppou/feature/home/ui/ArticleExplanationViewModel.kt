package blue.starry.tokidokiroppou.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.ai.ArticleExplanationRepository
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.ui.component.ExplanationSheetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArticleExplanationViewModel @Inject constructor(
    private val repository: ArticleExplanationRepository,
) : ViewModel() {

    private val _sheetState = MutableStateFlow<ExplanationSheetState>(ExplanationSheetState.Hidden)
    val sheetState: StateFlow<ExplanationSheetState> = _sheetState.asStateFlow()

    // 再試行用に最後にリクエストした条文を保持する
    private var lastArticle: Article? = null
    private var explanationJob: Job? = null

    fun explainArticle(article: Article) {
        lastArticle = article
        explanationJob?.cancel()
        explanationJob = viewModelScope.launch {
            _sheetState.value = ExplanationSheetState.Loading
            try {
                val accumulated = StringBuilder()
                repository.explainArticle(article).collect { chunk ->
                    accumulated.append(chunk)
                    _sheetState.value = ExplanationSheetState.Streaming(accumulated.toString())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate article explanation")
                _sheetState.value = ExplanationSheetState.Error("解説の生成に失敗しました")
            }
        }
    }

    fun retry() {
        lastArticle?.let { explainArticle(it) }
    }

    fun dismissSheet() {
        explanationJob?.cancel()
        _sheetState.value = ExplanationSheetState.Hidden
    }
}
