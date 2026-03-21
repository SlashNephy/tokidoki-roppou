package blue.starry.tokidokiroppou.feature.collection.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionScreenViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: ApplicationSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            bookmarkRepository.observeAll().collect { articles ->
                _uiState.value = if (articles.isEmpty()) {
                    CollectionUiState.Empty
                } else {
                    CollectionUiState.Loaded(articles, settings.useHalfWidthParentheses)
                }
            }
        }
    }

    fun removeBookmark(article: Article) {
        viewModelScope.launch {
            bookmarkRepository.remove(
                article.lawCode,
                article.articleNumber,
                article.supplementaryProvisionLabel,
            )
        }
    }
}

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data object Empty : CollectionUiState
    data class Loaded(
        val articles: List<Article>,
        val useHalfWidthParentheses: Boolean,
    ) : CollectionUiState
}
