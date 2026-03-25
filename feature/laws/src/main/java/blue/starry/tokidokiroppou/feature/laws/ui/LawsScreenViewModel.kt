package blue.starry.tokidokiroppou.feature.laws.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading
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

    /** 構造見出し付きの条文リスト（法令展開時に使用） */
    private val _structuredContent = MutableStateFlow<Map<LawCode, List<LawContentItem>>>(emptyMap())
    val structuredContent: StateFlow<Map<LawCode, List<LawContentItem>>> = _structuredContent.asStateFlow()

    /** 折りたたまれている見出しの orderIndex の集合（法令ごと） */
    private val _collapsedHeadings = MutableStateFlow<Map<LawCode, Set<Int>>>(emptyMap())
    val collapsedHeadings: StateFlow<Map<LawCode, Set<Int>>> = _collapsedHeadings.asStateFlow()

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
            if (lawCode !in _structuredContent.value) {
                loadStructuredContent(lawCode)
            }
        }
    }

    private fun loadStructuredContent(lawCode: LawCode) {
        viewModelScope.launch {
            _loadingLaw.value = lawCode
            val content = lawRepository.getStructuredContent(lawCode)
            _structuredContent.value = _structuredContent.value + (lawCode to content)
            // デフォルトで全見出しを折りたたみ状態にする
            val headingIndices = content
                .filterIsInstance<LawContentItem.Heading>()
                .map { it.orderIndex }
                .toSet()
            _collapsedHeadings.value = _collapsedHeadings.value + (lawCode to headingIndices)
            // 同じ法令のロード完了時のみスピナーを解除する（別法令の読み込み中に誤って消さない）
            if (_loadingLaw.value == lawCode) {
                _loadingLaw.value = null
            }
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

    /** 見出しの折りたたみ状態をトグルする */
    fun toggleHeading(lawCode: LawCode, orderIndex: Int) {
        val current = _collapsedHeadings.value
        val existing = current[lawCode] ?: emptySet()
        val updated = if (orderIndex in existing) existing - orderIndex else existing + orderIndex
        _collapsedHeadings.value = current + (lawCode to updated)
    }

    /**
     * 折りたたみ状態を反映したコンテンツリストを返す。
     * 折りたたまれた見出しの配下（同レベル以上の次の見出しまで）を非表示にする。
     */
    fun getVisibleContent(lawCode: LawCode): List<LawContentItem> {
        val content = _structuredContent.value[lawCode] ?: return emptyList()
        val collapsed = _collapsedHeadings.value[lawCode] ?: emptySet()
        if (collapsed.isEmpty()) return content

        val result = mutableListOf<LawContentItem>()
        var skipUntilLevel: Int? = null

        for (item in content) {
            if (item is LawContentItem.Heading) {
                val depth = item.heading.level.depth
                // 折りたたみ中: 下位レベルの見出しもスキップする
                if (skipUntilLevel != null && depth > skipUntilLevel) {
                    continue
                }
                // 同レベル以上の見出しが来たらスキップ解除
                if (skipUntilLevel != null) {
                    skipUntilLevel = null
                }
                result.add(item)
                // この見出し自体が折りたたまれていたら、配下をスキップ開始
                if (item.orderIndex in collapsed) {
                    skipUntilLevel = depth
                }
            } else {
                if (skipUntilLevel == null) {
                    result.add(item)
                }
            }
        }
        return result
    }

    /** 展開中の法令の条文数を返す（見出しを除く） */
    fun getArticleCount(lawCode: LawCode): Int? {
        val content = _structuredContent.value[lawCode] ?: return null
        return content.count { it is LawContentItem.ArticleItem }
    }
}
