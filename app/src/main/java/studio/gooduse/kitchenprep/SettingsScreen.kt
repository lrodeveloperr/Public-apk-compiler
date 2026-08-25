@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package studio.gooduse.kitchenprep

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import studio.gooduse.kitchenprep.data.SettingsState

@Composable
fun SettingsScreen(
    settings: SettingsState,
    strings: KitchenStrings,
    profile: KitchenWindowProfile,
    billingPrice: String?,
    consentPrivacyRequired: Boolean,
    tr: Translate,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onAlerts: (Boolean) -> Unit,
    onAwake: (Boolean) -> Unit,
    onCompact: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onSupport: () -> Unit,
    onSafety: () -> Unit,
    onPrivacyChoices: () -> Unit,
    onRemoveAds: () -> Unit,
    onManageSubscription: () -> Unit,
    onDelete: () -> Unit,
) {
    var languageMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = centeredContentModifier(profile)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = profile.gutter, vertical = 18.dp)
            .padding(bottom = 24.dp),
    ) {
        PageTitle(tr("settings", "Settings"), profile)
        Spacer(Modifier.height(12.dp))
        WorkbenchCard {
            SettingRow(
                title = tr("language", "Language"),
                stacked = profile.width < 360.dp || profile.largeText,
                trailing = {
                    Box {
                        OutlinedButton(
                            onClick = { languageMenu = true },
                            modifier = Modifier.heightIn(min = 48.dp).widthIn(max = 210.dp),
                        ) {
                            Text(
                                strings.languageName(
                                    settings.languageTag,
                                    KitchenStrings.supportedLocales.firstOrNull {
                                        settings.languageTag.startsWith(it.tag.substringBefore("-"), true)
                                    }?.fallbackName ?: "English",
                                ),
                                maxLines = 2,
                            )
                        }
                        DropdownMenu(
                            expanded = languageMenu,
                            onDismissRequest = { languageMenu = false },
                        ) {
                            KitchenStrings.supportedLocales.forEach { locale ->
                                DropdownMenuItem(
                                    text = {
                                        Text(strings.languageName(locale.tag, locale.fallbackName))
                                    },
                                    onClick = {
                                        onLanguage(locale.tag)
                                        languageMenu = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            SettingRow(
                title = tr("theme", "Theme"),
                stacked = profile.width < 600.dp || profile.largeText,
                trailing = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            "system" to tr("system", "System"),
                            "light" to tr("light", "Light"),
                            "dark" to tr("dark", "Dark"),
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = settings.themeMode == value,
                                onClick = { onTheme(value) },
                                label = { Text(label, maxLines = 2) },
                            )
                        }
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ToggleSetting(tr("alerts", "Alerts"), settings.alerts, onAlerts)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ToggleSetting(tr("awake", "Screen awake"), settings.keepAwake, onAwake)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ToggleSetting(tr("compact", "Compact"), settings.compactLive, onCompact)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ToggleSetting(tr("haptics", "Haptics"), settings.haptics, onHaptics)
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(tr("legal", "Privacy & legal"))
        WorkbenchCard {
            LegalRow(tr("privacy", "Privacy"), onPrivacy)
            DividerInCard()
            LegalRow(tr("terms", "Terms"), onTerms)
            DividerInCard()
            LegalRow(tr("support", "Support"), onSupport)
            DividerInCard()
            LegalRow(tr("safety", "Safety"), onSafety)
            if (consentPrivacyRequired) {
                DividerInCard()
                LegalRow(
                    tr("privacyChoices", "Privacy choices"),
                    onPrivacyChoices,
                    subtitle = tr("managedGoogle", "Managed by Google where required"),
                )
            }
            DividerInCard()
            LegalRow(
                tr("removeAds", "Remove ads"),
                onRemoveAds,
                subtitle = "${billingPrice ?: "US$1.49"} · ${tr("monthlyPlan", "Monthly")}",
            )
            DividerInCard()
            LegalRow(tr("manageSub", "Manage subscription"), onManageSubscription)
            DividerInCard()
            LegalRow(
                tr("deleteData", "Delete local data"),
                onClick = { confirmDelete = true },
                danger = true,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("deleteConfirm", "Delete local app data?")) },
            text = { Text(tr("deleteData", "Delete local data")) },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tr("cancel", "Cancel")) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KitchenColors.Terra,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(tr("delete", "Delete"))
                }
            },
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    stacked: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            trailing()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            trailing()
        }
    }
}

@Composable
fun ToggleSetting(title: String, value: Boolean, onValue: (Boolean) -> Unit) {
    SettingRow(title) {
        Switch(checked = value, onCheckedChange = onValue)
    }
}

@Composable
fun LegalRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (danger) KitchenColors.TerraDeep else MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
