@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package blue.starry.tokidokiroppou.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawId
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
    val addedLaws by viewModel.addedLaws.collectAsStateWithLifecycle()
    val catalogSearchResults by viewModel.catalogSearchResults.collectAsStateWithLifecycle()
    val isCatalogSearching by viewModel.isCatalogSearching.collectAsStateWithLifecycle()
    val catalogSearchError by viewModel.catalogSearchError.collectAsStateWithLifecycle()
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
            addedLaws = addedLaws,
            catalogSearchResults = catalogSearchResults,
            isCatalogSearching = isCatalogSearching,
            catalogSearchError = catalogSearchError,
            isRefreshing = isRefreshing,
            onNotificationEnabledChanged = viewModel::setNotificationEnabled,
            onIntervalChanged = viewModel::setNotificationInterval,
            onLawCodeEnabledChanged = viewModel::setLawCodeEnabled,
            onLawEnabledChanged = viewModel::setLawEnabled,
            onCatalogSearchQueryChanged = viewModel::searchCatalog,
            onAddCatalogLaw = viewModel::addLawForNotifications,
            onUseHalfWidthParenthesesChanged = viewModel::setUseHalfWidthParentheses,
            onExcludeSupplementaryProvisionsChanged = viewModel::setExcludeSupplementaryProvisions,
            onWidgetUpdateIntervalChanged = viewModel::setWidgetUpdateInterval,
            onClearCacheAndRefresh = viewModel::clearCacheAndRefresh,
        )
    }
}

@Composable
private fun SettingsContent(
    settings: ApplicationSettings,
    lawMetadata: Map<LawCode, LawMetadata>,
    addedLaws: List<Law>,
    catalogSearchResults: List<Law>,
    isCatalogSearching: Boolean,
    catalogSearchError: String?,
    isRefreshing: Boolean,
    onNotificationEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onLawCodeEnabledChanged: (LawCode, Boolean) -> Unit,
    onLawEnabledChanged: (LawId, Boolean) -> Unit,
    onCatalogSearchQueryChanged: (String) -> Unit,
    onAddCatalogLaw: (Law) -> Unit,
    onUseHalfWidthParenthesesChanged: (Boolean) -> Unit,
    onExcludeSupplementaryProvisionsChanged: (Boolean) -> Unit,
    onWidgetUpdateIntervalChanged: (Int) -> Unit,
    onClearCacheAndRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddLawDialog by rememberSaveable { mutableStateOf(false) }

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
                    IntervalPickerDialog(
                        title = "通知間隔",
                        selectedMinutes = settings.notificationIntervalMinutes,
                        onSelect = onIntervalChanged,
                        onDismiss = { showIntervalDialog = false },
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

        item {
            SettingSection(title = "ウィジェット") {
                var showWidgetIntervalDialog by remember { mutableStateOf(false) }
                SettingItem(
                    headline = "更新間隔",
                    supporting = ApplicationSettings.intervalDisplayText(settings.widgetUpdateIntervalMinutes),
                    leadingIcon = Icons.Default.Widgets,
                    onClick = { showWidgetIntervalDialog = true },
                )
                if (showWidgetIntervalDialog) {
                    IntervalPickerDialog(
                        title = "ウィジェットの更新間隔",
                        selectedMinutes = settings.widgetUpdateIntervalMinutes,
                        onSelect = onWidgetUpdateIntervalChanged,
                        onDismiss = { showWidgetIntervalDialog = false },
                    )
                }
            }
        }

        val lawCodesByCategory = LawCode.entries.groupBy { it.category }
        LawCategory.entries.forEach { category ->
            val lawCodes = lawCodesByCategory[category] ?: return@forEach
            item(key = category.name) {
                SettingSection(title = category.displayName) {
                    lawCodes.forEach { lawCode ->
                        val lawId = LawId(lawCode.lawId)
                        val isEnabled = lawId in settings.enabledLawIds
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

        item(key = "added_laws") {
            SettingSection(title = "追加した法令") {
                SettingItem(
                    headline = "法令を追加",
                    leadingIcon = Icons.Default.Add,
                    onClick = { showAddLawDialog = true },
                )

                addedLaws.forEach { law ->
                    val isEnabled = law.id in settings.enabledLawIds
                    SettingItem(
                        headline = law.displayName,
                        supporting = listOfNotNull(law.lawNum, law.id.value).joinToString(" / "),
                        leadingIcon = Icons.Default.Book,
                        trailing = {
                            Checkbox(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    onLawEnabledChanged(law.id, checked)
                                },
                            )
                        },
                        onClick = {
                            onLawEnabledChanged(law.id, !isEnabled)
                        },
                    )
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

    if (showAddLawDialog) {
        AddLawDialog(
            searchResults = catalogSearchResults,
            isSearching = isCatalogSearching,
            searchError = catalogSearchError,
            onQueryChanged = onCatalogSearchQueryChanged,
            onAddLaw = onAddCatalogLaw,
            onDismissRequest = {
                showAddLawDialog = false
                onCatalogSearchQueryChanged("")
            },
        )
    }
}

@Composable
private fun IntervalPickerDialog(
    title: String,
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ApplicationSettings.INTERVAL_OPTIONS.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(minutes)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = minutes == selectedMinutes,
                            onClick = {
                                onSelect(minutes)
                                onDismiss()
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

@Composable
private fun AddLawDialog(
    searchResults: List<Law>,
    isSearching: Boolean,
    searchError: String?,
    onQueryChanged: (String) -> Unit,
    onAddLaw: (Law) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("法令を追加") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { value ->
                        query = value
                        onQueryChanged(value)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e-Gov の法令名・法令番号で検索") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    onQueryChanged("")
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "クリア",
                                )
                            }
                        }
                    },
                    singleLine = true,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column {
                        when {
                            isSearching -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator(modifier = Modifier.size(24.dp))
                            }

                            searchError != null -> Text(
                                text = searchError,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )

                            query.isNotBlank() && searchResults.isEmpty() -> Text(
                                text = "該当する法令が見つかりませんでした",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        searchResults.forEach { law ->
                            CatalogSearchResultItem(
                                law = law,
                                onAddClick = { onAddLaw(law) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("閉じる")
            }
        },
    )
}

@Composable
private fun CatalogSearchResultItem(
    law: Law,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = law.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(law.lawNum, law.id.value).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (law.isAdded || law.isPreset) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "追加済み",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "追加",
                )
            }
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun formatIsoDate(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0].toIntOrNull() ?: return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    return "${year}年${month}月${day}日"
}
