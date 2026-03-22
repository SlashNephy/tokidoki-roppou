@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package blue.starry.tokidokiroppou.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.normalizeDisplay
import blue.starry.tokidokiroppou.core.ui.component.SettingItem
import blue.starry.tokidokiroppou.core.ui.component.SettingSection

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lawMetadata by viewModel.lawMetadata.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val currentSettings = settings
    if (currentSettings == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
    } else {
        SettingsContent(
            settings = currentSettings,
            lawMetadata = lawMetadata,
            isRefreshing = isRefreshing,
            onNotificationEnabledChanged = viewModel::setNotificationEnabled,
            onIntervalChanged = viewModel::setNotificationInterval,
            onLawCodeEnabledChanged = viewModel::setLawCodeEnabled,
            onUseHalfWidthParenthesesChanged = viewModel::setUseHalfWidthParentheses,
            onExcludeSupplementaryProvisionsChanged = viewModel::setExcludeSupplementaryProvisions,
            onClearCacheAndRefresh = viewModel::clearCacheAndRefresh,
        )
    }
}

@Composable
private fun SettingsContent(
    settings: ApplicationSettings,
    lawMetadata: Map<LawCode, LawMetadata>,
    isRefreshing: Boolean,
    onNotificationEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onLawCodeEnabledChanged: (LawCode, Boolean) -> Unit,
    onUseHalfWidthParenthesesChanged: (Boolean) -> Unit,
    onExcludeSupplementaryProvisionsChanged: (Boolean) -> Unit,
    onClearCacheAndRefresh: () -> Unit,
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

                var showIntervalDialog by remember { mutableStateOf(false) }
                SettingItem(
                    headline = "通知間隔",
                    supporting = ApplicationSettings.intervalDisplayText(settings.notificationIntervalMinutes),
                    leadingIcon = Icons.Default.Schedule,
                    onClick = { showIntervalDialog = true },
                )
                if (showIntervalDialog) {
                    AlertDialog(
                        onDismissRequest = { showIntervalDialog = false },
                        title = { Text("通知間隔") },
                        text = {
                            Column {
                                ApplicationSettings.INTERVAL_OPTIONS.forEach { minutes ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onIntervalChanged(minutes)
                                                showIntervalDialog = false
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = minutes == settings.notificationIntervalMinutes,
                                            onClick = {
                                                onIntervalChanged(minutes)
                                                showIntervalDialog = false
                                            },
                                        )
                                        Text(
                                            text = ApplicationSettings.intervalDisplayText(minutes),
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                    )
                }

                SettingItem(
                    headline = "附則を除外する",
                    supporting = "通知対象から附則の条文を除外します",
                    leadingIcon = Icons.Default.Book,
                    trailing = {
                        Switch(
                            checked = settings.excludeSupplementaryProvisions,
                            onCheckedChange = onExcludeSupplementaryProvisionsChanged,
                        )
                    },
                    onClick = {
                        onExcludeSupplementaryProvisionsChanged(!settings.excludeSupplementaryProvisions)
                    },
                )
            }
        }

        item {
            SettingSection(title = "表示") {
                SettingItem(
                    headline = "読みやすい表記にする",
                    supporting = "全角かっこを半角に、法令番号の漢数字を算用数字に変換します",
                    leadingIcon = Icons.Default.TextFormat,
                    trailing = {
                        Switch(
                            checked = settings.useHalfWidthParentheses,
                            onCheckedChange = onUseHalfWidthParenthesesChanged,
                        )
                    },
                    onClick = {
                        onUseHalfWidthParenthesesChanged(!settings.useHalfWidthParentheses)
                    },
                )
            }
        }

        val lawCodesByCategory = LawCode.entries.groupBy { it.category }
        LawCategory.entries.forEach { category ->
            val lawCodes = lawCodesByCategory[category] ?: return@forEach
            item(key = category.name) {
                SettingSection(title = category.displayName) {
                    lawCodes.forEach { lawCode ->
                        val isEnabled = lawCode in settings.enabledLawCodes
                        val metadata = lawMetadata[lawCode]
                        val subtitle = metadata?.let {
                            val num = it.lastAmendmentLawNum ?: it.lawNum
                            val amendment = it.lastAmendmentDate
                            val promulgation = it.promulgationDate
                            val text = if (amendment != null) {
                                "${num}・${formatIsoDate(amendment)}改正"
                            } else if (promulgation != null) {
                                "${num}・${formatIsoDate(promulgation)}公布"
                            } else {
                                num
                            }
                            if (settings.useHalfWidthParentheses) text.normalizeDisplay() else text
                        }
                        SettingItem(
                            headline = lawCode.displayName,
                            supporting = subtitle,
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

        item(key = "cache_actions") {
            SettingSection(title = "データ") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Button(
                        onClick = onClearCacheAndRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isRefreshing) {
                            LoadingIndicator(
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "ダウンロード中…",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        } else {
                            Text("キャッシュを破棄して再ダウンロード")
                        }
                    }
                }
            }
        }
    }
}

private fun formatIsoDate(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0].toIntOrNull() ?: return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    return "${year}年${month}月${day}日"
}
