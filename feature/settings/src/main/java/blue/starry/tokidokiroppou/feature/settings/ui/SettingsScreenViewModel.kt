package blue.starry.tokidokiroppou.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.data.repository.LawRepositoryImpl
import blue.starry.tokidokiroppou.core.data.worker.ArticleNotificationScheduler
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val settingsRepository: ApplicationSettingsRepository,
    private val lawRepository: LawRepositoryImpl,
    private val scheduler: ArticleNotificationScheduler,
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
        viewModelScope.launch {
            val lawId = LawId(lawCode.lawId)
            settingsRepository.setLawEnabled(lawId, enabled)
            if (enabled) {
                lawRepository.refreshLawId(lawId)
            }
        }
    }

    fun setUseHalfWidthParentheses(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseHalfWidthParentheses(enabled)
        }
    }

    fun setExcludeSupplementaryProvisions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExcludeSupplementaryProvisions(enabled)
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
}
