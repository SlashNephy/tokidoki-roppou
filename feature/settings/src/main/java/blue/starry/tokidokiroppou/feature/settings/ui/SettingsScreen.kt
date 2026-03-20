package blue.starry.tokidokiroppou.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.ui.component.SettingItem
import blue.starry.tokidokiroppou.core.ui.component.SettingSection

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        val currentSettings = settings
        if (currentSettings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            SettingsContent(
                settings = currentSettings,
                onNotificationEnabledChanged = viewModel::setNotificationEnabled,
                onIntervalChanged = viewModel::setNotificationInterval,
                onLawCodeEnabledChanged = viewModel::setLawCodeEnabled,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SettingsContent(
    settings: ApplicationSettings,
    onNotificationEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onLawCodeEnabledChanged: (LawCode, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            SettingSection(title = "通知") {
                SettingItem(
                    headline = "通知を有効にする",
                    supporting = "条文を定期的に通知します",
                    leadingIcon = Icons.Default.Notifications,
                    trailing = {
                        Switch(
                            checked = settings.isNotificationEnabled,
                            onCheckedChange = onNotificationEnabledChanged,
                        )
                    },
                    onClick = {
                        onNotificationEnabledChanged(!settings.isNotificationEnabled)
                    },
                )

                var showIntervalMenu by remember { mutableStateOf(false) }
                Box {
                    SettingItem(
                        headline = "通知間隔",
                        supporting = ApplicationSettings.intervalDisplayText(settings.notificationIntervalMinutes),
                        leadingIcon = Icons.Default.Schedule,
                        onClick = { showIntervalMenu = true },
                    )
                    DropdownMenu(
                        expanded = showIntervalMenu,
                        onDismissRequest = { showIntervalMenu = false },
                    ) {
                        ApplicationSettings.INTERVAL_OPTIONS.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(ApplicationSettings.intervalDisplayText(minutes)) },
                                onClick = {
                                    onIntervalChanged(minutes)
                                    showIntervalMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingSection(title = "法令") {
                LawCode.entries.forEach { lawCode ->
                    val isEnabled = lawCode in settings.enabledLawCodes
                    SettingItem(
                        headline = lawCode.displayName,
                        leadingIcon = Icons.Default.Book,
                        trailing = {
                            Checkbox(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    onLawCodeEnabledChanged(lawCode, checked)
                                },
                            )
                        },
                        onClick = {
                            onLawCodeEnabledChanged(lawCode, !isEnabled)
                        },
                    )
                }
            }
        }
    }
}
