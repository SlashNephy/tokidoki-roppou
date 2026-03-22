package blue.starry.tokidokiroppou.feature.collection.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionScreenViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    settingsRepository: ApplicationSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<CollectionUiState> = combine(
        bookmarkRepository.observeAll(),
        settingsRepository.observe(),
    ) { articles, settings ->
        if (articles.isEmpty()) {
            CollectionUiState.Empty
        } else {
            CollectionUiState.Loaded(articles, settings.useHalfWidthParentheses)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionUiState.Loading,
    )

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
