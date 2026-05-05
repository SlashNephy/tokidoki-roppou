package blue.starry.tokidokiroppou.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.BookmarkRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {
    data class ArticleNavigationTarget(
        val lawCode: String,
        val articleNumber: String,
        val supplementaryProvisionLabel: String? = null,
    )

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isBookmarked: StateFlow<Boolean> = _uiState.flatMapLatest { state ->
        if (state is HomeUiState.Loaded) {
            bookmarkRepository.observeIsBookmarked(
                state.article.lawCode,
                state.article.articleNumber,
                state.article.supplementaryProvisionLabel,
            )
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleBookmark() {
        val state = _uiState.value as? HomeUiState.Loaded ?: return
        toggleBookmarkForArticle(state.article)
    }

    fun toggleBookmarkForArticle(article: Article) {
        viewModelScope.launch {
            bookmarkRepository.toggle(
                article.lawCode,
                article.articleNumber,
                article.supplementaryProvisionLabel,
            )
        }
    }

    fun observeIsBookmarked(article: Article) = bookmarkRepository.observeIsBookmarked(
        article.lawCode,
        article.articleNumber,
        article.supplementaryProvisionLabel,
    )

    fun loadArticle(lawCode: String?, articleNumber: String?, supplementaryProvisionLabel: String?) {
        if (lawCode != null && articleNumber != null) {
            loadSpecificArticle(lawCode, articleNumber, supplementaryProvisionLabel)
        } else {
            loadRandomArticle()
        }
    }

    fun loadRandomArticle() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val settings = settingsRepository.get()
            if (settings.enabledLawCodes.isEmpty()) {
                _uiState.value = HomeUiState.NoLawSelected
                return@launch
            }

            val article = lawRepository.getRandomArticle(settings.enabledLawCodes, settings.excludeSupplementaryProvisions)

            if (article != null) {
                val related = lawRepository.getRelatedArticles(article)
                val metadata = lawRepository.getLawMetadata(article.lawCode)
                val navigation = buildNavigationTargets(article)
                _uiState.value = HomeUiState.Loaded(
                    article = article,
                    relatedArticles = related,
                    lawMetadata = metadata,
                    useHalfWidthParentheses = settings.useHalfWidthParentheses,
                    previousArticle = navigation.first,
                    nextArticle = navigation.second,
                )
            } else {
                _uiState.value = HomeUiState.Error("条文を取得できませんでした")
            }
        }
    }

    private fun loadSpecificArticle(lawCodeName: String, articleNumber: String, supplementaryProvisionLabel: String? = null) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val settings = settingsRepository.get()
            val lawCode = runCatching { LawCode.valueOf(lawCodeName) }.getOrNull()
            val article = lawCode?.let { lawRepository.getArticle(it, articleNumber, supplementaryProvisionLabel) }

            if (article != null) {
                val related = lawRepository.getRelatedArticles(article)
                val metadata = lawRepository.getLawMetadata(article.lawCode)
                val navigation = buildNavigationTargets(article)
                _uiState.value = HomeUiState.Loaded(
                    article = article,
                    relatedArticles = related,
                    lawMetadata = metadata,
                    useHalfWidthParentheses = settings.useHalfWidthParentheses,
                    previousArticle = navigation.first,
                    nextArticle = navigation.second,
                )
            } else {
                _uiState.value = HomeUiState.Error("条文を取得できませんでした")
            }
        }
    }

    fun navigateTo(target: ArticleNavigationTarget) {
        loadSpecificArticle(target.lawCode, target.articleNumber, target.supplementaryProvisionLabel)
    }

    private suspend fun buildNavigationTargets(article: Article): Pair<ArticleNavigationTarget?, ArticleNavigationTarget?> {
        val articles = lawRepository.getArticles(article.lawCode)
        val index = articles.indexOfFirst {
            it.articleNumber == article.articleNumber &&
                it.supplementaryProvisionLabel == article.supplementaryProvisionLabel
        }
        if (index == -1) return null to null
        val previous = articles.getOrNull(index - 1)?.let {
            ArticleNavigationTarget(it.lawCode.name, it.articleNumber, it.supplementaryProvisionLabel)
        }
        val next = articles.getOrNull(index + 1)?.let {
            ArticleNavigationTarget(it.lawCode.name, it.articleNumber, it.supplementaryProvisionLabel)
        }
        return previous to next
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Loaded(
        val article: Article,
        val relatedArticles: List<Article>,
        val lawMetadata: LawMetadata?,
        val useHalfWidthParentheses: Boolean,
        val previousArticle: HomeScreenViewModel.ArticleNavigationTarget?,
        val nextArticle: HomeScreenViewModel.ArticleNavigationTarget?,
    ) : HomeUiState
    data object NoLawSelected : HomeUiState
    data class Error(val message: String) : HomeUiState
}
