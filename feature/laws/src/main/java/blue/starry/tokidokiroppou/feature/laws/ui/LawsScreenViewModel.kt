package blue.starry.tokidokiroppou.feature.laws.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.StructureHeading
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawCatalogRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LawsScreenViewModel @Inject constructor(
    private val lawRepository: LawRepository,
    private val lawCatalogRepository: LawCatalogRepository,
    private val settingsRepository: ApplicationSettingsRepository,
) : ViewModel() {

    val lawMetadata: StateFlow<Map<LawCode, LawMetadata>> = lawRepository.observeLawMetadata()
        .map { metadata ->
            LawCode.entries.mapNotNull { lawCode ->
                val lawMetadata = metadata[LawId(lawCode.lawId)] ?: return@mapNotNull null
                lawCode to lawMetadata
            }.toMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _useHalfWidthParentheses = MutableStateFlow(false)
    val useHalfWidthParentheses: StateFlow<Boolean> = _useHalfWidthParentheses.asStateFlow()

    val addedLaws: StateFlow<List<Law>> = lawCatalogRepository.observeLaws()
        .map { laws ->
            laws.filter { it.isAdded && !it.isPreset }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _expandedLaw = MutableStateFlow<LawId?>(null)
    val expandedLaw: StateFlow<LawId?> = _expandedLaw.asStateFlow()

    /** 構造見出し付きの条文リスト（法令展開時に使用） */
    private val _structuredContent = MutableStateFlow<Map<LawId, List<LawContentItem>>>(emptyMap())
    val structuredContent: StateFlow<Map<LawId, List<LawContentItem>>> = _structuredContent.asStateFlow()

    /** 折りたたまれている見出しの orderIndex の集合（法令ごと） */
    private val _collapsedHeadings = MutableStateFlow<Map<LawId, Set<Int>>>(emptyMap())
    val collapsedHeadings: StateFlow<Map<LawId, Set<Int>>> = _collapsedHeadings.asStateFlow()

    private val _loadingLaw = MutableStateFlow<LawId?>(null)
    val loadingLaw: StateFlow<LawId?> = _loadingLaw.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<Map<LawId, List<Article>>?>(null)
    val searchResults: StateFlow<Map<LawId, List<Article>>?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val knownLaws: StateFlow<List<Law>> = lawCatalogRepository.observeLaws()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val rawCatalogSearchResults = MutableStateFlow<List<Law>>(emptyList())

    val catalogSearchResults: StateFlow<List<Law>> = combine(rawCatalogSearchResults, knownLaws) { results, laws ->
        val addedLawIds = laws
            .filter { it.isAdded || it.isPreset }
            .mapTo(mutableSetOf()) { it.id }

        results.map { law ->
            law.copy(isAdded = law.id in addedLawIds)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isCatalogSearching = MutableStateFlow(false)
    val isCatalogSearching: StateFlow<Boolean> = _isCatalogSearching.asStateFlow()

    private val _catalogSearchError = MutableStateFlow<String?>(null)
    val catalogSearchError: StateFlow<String?> = _catalogSearchError.asStateFlow()

    private var catalogSearchJob: Job? = null
    private var catalogSearchGeneration = 0

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

    fun toggleLaw(lawId: LawId) {
        if (_expandedLaw.value == lawId) {
            _expandedLaw.value = null
        } else {
            _expandedLaw.value = lawId
            if (lawId !in _structuredContent.value) {
                loadStructuredContent(lawId)
            }
        }
    }

    private fun loadStructuredContent(lawId: LawId) {
        viewModelScope.launch {
            _loadingLaw.value = lawId
            val content = lawRepository.getStructuredContent(lawId)
            _structuredContent.value = _structuredContent.value + (lawId to content)
            // デフォルトで全見出しを折りたたみ状態にする
            val headingIndices = content
                .filterIsInstance<LawContentItem.Heading>()
                .map { it.orderIndex }
                .toSet()
            _collapsedHeadings.value = _collapsedHeadings.value + (lawId to headingIndices)
            // 同じ法令のロード完了時のみスピナーを解除する（別法令の読み込み中に誤って消さない）
            if (_loadingLaw.value == lawId) {
                _loadingLaw.value = null
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchCatalog(query: String) {
        val trimmedQuery = query.trim()
        val generation = ++catalogSearchGeneration
        catalogSearchJob?.cancel()
        _catalogSearchError.value = null

        if (trimmedQuery.isBlank()) {
            rawCatalogSearchResults.value = emptyList()
            _isCatalogSearching.value = false
            return
        }

        rawCatalogSearchResults.value = emptyList()
        _isCatalogSearching.value = true

        catalogSearchJob = viewModelScope.launch {
            delay(300)
            try {
                val results = lawCatalogRepository.searchEGovLaws(trimmedQuery)
                if (generation == catalogSearchGeneration) {
                    rawCatalogSearchResults.value = results
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (generation == catalogSearchGeneration) {
                    rawCatalogSearchResults.value = emptyList()
                    _catalogSearchError.value = "e-Gov 法令検索に失敗しました"
                }
            } finally {
                if (generation == catalogSearchGeneration) {
                    _isCatalogSearching.value = false
                }
            }
        }
    }

    fun addLawForBrowsing(law: Law) {
        viewModelScope.launch {
            lawCatalogRepository.addLaw(law, enableNotification = false)
        }
    }

    fun getFilteredLawCodes(category: LawCategory): List<LawCode> {
        val query = _searchQuery.value
        val codes = LawCode.entries.filter { it.category == category }
        if (query.isBlank()) return codes
        val results = _searchResults.value
        return codes.filter { lawCode ->
            val lawId = LawId(lawCode.lawId)
            lawCode.displayName.contains(query, ignoreCase = true)
                || (results != null && lawId in results)
        }
    }

    fun getFilteredAddedLaws(laws: List<Law>): List<Law> {
        val query = _searchQuery.value
        if (query.isBlank()) return laws
        val results = _searchResults.value

        return laws.filter { law ->
            law.displayName.contains(query, ignoreCase = true)
                || law.lawNum?.contains(query, ignoreCase = true) == true
                || law.id.value.contains(query, ignoreCase = true)
                || (results != null && law.id in results)
        }
    }

    /** 見出しの折りたたみ状態をトグルする */
    fun toggleHeading(lawId: LawId, orderIndex: Int) {
        val current = _collapsedHeadings.value
        val existing = current[lawId] ?: emptySet()
        val updated = if (orderIndex in existing) existing - orderIndex else existing + orderIndex
        _collapsedHeadings.value = current + (lawId to updated)
    }

    /**
     * 折りたたみ状態を反映したコンテンツリストを返す。
     * 折りたたまれた見出しの配下（同レベル以上の次の見出しまで）を非表示にする。
     */
    fun getVisibleContent(lawId: LawId): List<LawContentItem> {
        val content = _structuredContent.value[lawId] ?: return emptyList()
        val collapsed = _collapsedHeadings.value[lawId] ?: emptySet()
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
    fun getArticleCount(lawId: LawId): Int? {
        val content = _structuredContent.value[lawId] ?: return null
        return content.count { it is LawContentItem.ArticleItem }
    }
}
