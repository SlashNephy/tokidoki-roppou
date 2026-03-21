package blue.starry.tokidokiroppou.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val route = savedStateHandle.toRoute<HomeRoute>()
        if (route.lawCode != null && route.articleNumber != null) {
            loadSpecificArticle(route.lawCode, route.articleNumber, route.supplementaryProvisionLabel)
        } else {
            loadRandomArticle()
        }
    }

    fun loadRandomArticle() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val settings = settingsRepository.get()
            val article = lawRepository.getRandomArticle(settings.enabledLawCodes, settings.excludeSupplementaryProvisions)

            if (article != null) {
                val related = lawRepository.getRelatedArticles(article)
                val metadata = lawRepository.getLawMetadata(article.lawCode)
                _uiState.value = HomeUiState.Loaded(article, related, metadata, settings.useHalfWidthParentheses)
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
                _uiState.value = HomeUiState.Loaded(article, related, metadata, settings.useHalfWidthParentheses)
            } else {
                _uiState.value = HomeUiState.Error("条文を取得できませんでした")
            }
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Loaded(
        val article: Article,
        val relatedArticles: List<Article>,
        val lawMetadata: LawMetadata?,
        val useHalfWidthParentheses: Boolean,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
