package blue.starry.tokidokiroppou.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import blue.starry.tokidokiroppou.core.data.worker.ArticleNotificationScheduler
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val settingsRepository: ApplicationSettingsRepository,
    private val scheduler: ArticleNotificationScheduler,
) : ViewModel() {

    val settings: StateFlow<ApplicationSettings?> = settingsRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

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
            settingsRepository.setLawCodeEnabled(lawCode, enabled)
        }
    }

    fun setUseHalfWidthParentheses(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseHalfWidthParentheses(enabled)
        }
    }
}
