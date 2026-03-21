package blue.starry.tokidokiroppou.feature.laws.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LawsScreenViewModel @Inject constructor(
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
) : ViewModel() {

    val lawMetadata: StateFlow<Map<LawCode, LawMetadata>> = lawRepository.observeLawMetadata()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _useHalfWidthParentheses = MutableStateFlow(false)
    val useHalfWidthParentheses: StateFlow<Boolean> = _useHalfWidthParentheses.asStateFlow()

    private val _expandedLaw = MutableStateFlow<LawCode?>(null)
    val expandedLaw: StateFlow<LawCode?> = _expandedLaw.asStateFlow()

    private val _articles = MutableStateFlow<Map<LawCode, List<Article>>>(emptyMap())
    val articles: StateFlow<Map<LawCode, List<Article>>> = _articles.asStateFlow()

    private val _loadingLaw = MutableStateFlow<LawCode?>(null)
    val loadingLaw: StateFlow<LawCode?> = _loadingLaw.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<Map<LawCode, List<Article>>?>(null)
    val searchResults: StateFlow<Map<LawCode, List<Article>>?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observe().collect { settings ->
                _useHalfWidthParentheses.value = settings.useHalfWidthParentheses
            }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _searchQuery.debounce(300).collect { query ->
                if (query.isBlank()) {
                    _searchResults.value = null
                    _isSearching.value = false
                } else {
                    _isSearching.value = true
                    _searchResults.value = lawRepository.searchArticles(query)
                    _isSearching.value = false
                }
            }
        }
    }

    fun toggleLaw(lawCode: LawCode) {
        if (_expandedLaw.value == lawCode) {
            _expandedLaw.value = null
        } else {
            _expandedLaw.value = lawCode
            if (lawCode !in _articles.value) {
                loadArticles(lawCode)
            }
        }
    }

    private fun loadArticles(lawCode: LawCode) {
        viewModelScope.launch {
            _loadingLaw.value = lawCode
            val result = lawRepository.getArticles(lawCode)
            _articles.value = _articles.value + (lawCode to result)
            _loadingLaw.value = null
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredLawCodes(category: LawCategory): List<LawCode> {
        val query = _searchQuery.value
        val codes = LawCode.entries.filter { it.category == category }
        if (query.isBlank()) return codes
        val results = _searchResults.value
        return codes.filter { lawCode ->
            lawCode.displayName.contains(query, ignoreCase = true)
                || (results != null && lawCode in results)
        }
    }
}
