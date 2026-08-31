package blue.starry.tokidokiroppou.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.data.repository.LawRepositoryImpl
import blue.starry.tokidokiroppou.core.data.worker.ArticleNotificationScheduler
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetUpdater
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val settingsRepository: ApplicationSettingsRepository,
    private val lawRepository: LawRepositoryImpl,
    private val lawCatalogRepository: LawCatalogRepository,
    private val scheduler: ArticleNotificationScheduler,
    private val widgetScheduler: ArticleWidgetScheduler,
    private val widgetUpdater: ArticleWidgetUpdater,
) : ViewModel() {

    val settings: StateFlow<ApplicationSettings?> = settingsRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val lawMetadata: StateFlow<Map<LawCode, LawMetadata>> = lawRepository.observeLawMetadata()
        .map { metadata ->
            LawCode.entries.mapNotNull { lawCode ->
                val lawMetadata = metadata[LawId(lawCode.lawId)] ?: return@mapNotNull null
                lawCode to lawMetadata
            }.toMap()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    private val knownLaws: StateFlow<List<Law>> = lawCatalogRepository.observeLaws()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val addedLaws: StateFlow<List<Law>> = knownLaws
        .map { laws ->
            laws.filter { it.isAdded && !it.isPreset }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val rawCatalogSearchResults = MutableStateFlow<List<Law>>(emptyList())

    val catalogSearchResults: StateFlow<List<Law>> = combine(rawCatalogSearchResults, knownLaws) { results, laws ->
        val addedLawIds = laws
            .filter { it.isAdded || it.isPreset }
            .mapTo(mutableSetOf()) { it.id }

        results.map { law ->
            law.copy(isAdded = law.id in addedLawIds)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _isCatalogSearching = MutableStateFlow(false)
    val isCatalogSearching: StateFlow<Boolean> = _isCatalogSearching.asStateFlow()

    private val _catalogSearchError = MutableStateFlow<String?>(null)
    val catalogSearchError: StateFlow<String?> = _catalogSearchError.asStateFlow()

    private var catalogSearchJob: Job? = null
    private var catalogSearchGeneration = 0

    init {
        refreshStaleData()
    }

    private fun refreshStaleData() {
        viewModelScope.launch {
            val enabledLawIds = settingsRepository.get().enabledLawIds
            val needsRefresh = lawRepository.getLawIdsNeedingRefresh()
                .filter { it in enabledLawIds }
            if (needsRefresh.isEmpty()) return@launch

            for (lawId in needsRefresh) {
                lawRepository.refreshLawId(lawId)
            }
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationEnabled(enabled)
            val settings = settingsRepository.get()
            if (enabled) {
                scheduler.schedule(settings.notificationIntervalMinutes)
            } else {
                scheduler.cancel()
            }
        }
    }

    fun setNotificationInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setNotificationIntervalMinutes(minutes)
            val settings = settingsRepository.get()
            if (settings.isNotificationEnabled) {
                scheduler.schedule(minutes)
            }
        }
    }

    fun setLawCodeEnabled(lawCode: LawCode, enabled: Boolean) {
        setLawEnabled(LawId(lawCode.lawId), enabled)
    }

    fun setLawEnabled(lawId: LawId, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLawEnabled(lawId, enabled)
            if (enabled) {
                lawRepository.refreshLawId(lawId)
            }
        }
    }

    fun setUseHalfWidthParentheses(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseHalfWidthParentheses(enabled)
            // 設定の保存後に再描画する。ウィジェットは再描画時に最新の設定を読み直すため、この順序が必要
            widgetUpdater.rerenderAll()
        }
    }

    fun setExcludeSupplementaryProvisions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExcludeSupplementaryProvisions(enabled)
        }
    }

    fun setWidgetUpdateInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setWidgetUpdateIntervalMinutes(minutes)
            // 未配置のときはスケジュールしない。
            // 次にウィジェットが配置された際、onEnabled が保存済みの間隔で登録する
            if (widgetUpdater.hasPlacedWidget()) {
                widgetScheduler.schedule(minutes)
            }
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursEnabled(enabled)
        }
    }

    fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietHours(startMinutesOfDay, endMinutesOfDay)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun clearCacheAndRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            lawRepository.clearCache()
            val enabledLawIds = settingsRepository.get().enabledLawIds
            for (lawId in enabledLawIds) {
                lawRepository.refreshLawId(lawId)
            }
            _isRefreshing.value = false
        }
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
            delay(CATALOG_SEARCH_DEBOUNCE_MILLIS)
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

    fun addLawForNotifications(law: Law) {
        viewModelScope.launch {
            lawCatalogRepository.addLaw(law, enableNotification = true)
            lawRepository.refreshLawId(law.id)
        }
    }

    private companion object {
        const val CATALOG_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
