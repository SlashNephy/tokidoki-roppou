package blue.starry.tokidokiroppou.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
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
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRandomArticle()
    }

    fun loadRandomArticle() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val settings = settingsRepository.get()
            val article = lawRepository.getRandomArticle(settings.enabledLawCodes)

            _uiState.value = if (article != null) {
                HomeUiState.Loaded(article)
            } else {
                HomeUiState.Error("条文を取得できませんでした")
            }
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Loaded(val article: Article) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
