@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package studio.gooduse.kitchenprep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val content = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = profile.gutter, vertical = if (profile.compactHeight) 10.dp else 18.dp)
        .padding(bottom = 22.dp)

    Column(modifier = content) {
        Text(
            tr("settings", "Settings"),
            fontSize = profile.pageTitleSize,
            lineHeight = profile.pageTitleSize * 1.08f,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(if (profile.compactHeight) 10.dp else 16.dp))

        when {
            profile.width >= 1100.dp && !profile.largeText -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        PreferencesCard(
                            settings = settings,
                            strings = strings,
                            languageMenu = languageMenu,
                            onLanguageMenu = { languageMenu = it },
                            tr = tr,
                            onLanguage = onLanguage,
                            onAlerts = onAlerts,
                            onAwake = onAwake,
                            onCompact = onCompact,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        SubscriptionCard(tr, onRemoveAds, onManageSubscription)
                    }
                    Column(Modifier.weight(1f)) {
                        LegalCard(
                            tr = tr,
                            consentPrivacyRequired = consentPrivacyRequired,
                            onPrivacy = onPrivacy,
                            onTerms = onTerms,
                            onSafety = onSafety,
                            onSupport = onSupport,
                            onPrivacyChoices = onPrivacyChoices,
                            includeDelete = true,
                            onDelete = { confirmDelete = true },
                        )
                    }
                }
            }

            profile.compactHeight && profile.width >= 700.dp && !profile.largeText -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PreferencesCard(
                            settings = settings,
                            strings = strings,
                            languageMenu = languageMenu,
                            onLanguageMenu = { languageMenu = it },
                            tr = tr,
                            onLanguage = onLanguage,
                            onAlerts = onAlerts,
                            onAwake = onAwake,
                            onCompact = onCompact,
                        )
                        SubscriptionCard(tr, onRemoveAds, onManageSubscription)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegalCard(
                            tr = tr,
                            consentPrivacyRequired = consentPrivacyRequired,
                            onPrivacy = onPrivacy,
                            onTerms = onTerms,
                            onSafety = onSafety,
                            onSupport = onSupport,
                            onPrivacyChoices = onPrivacyChoices,
                            includeDelete = true,
                            onDelete = { confirmDelete = true },
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 980.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PreferencesCard(
                        settings = settings,
                        strings = strings,
                        languageMenu = languageMenu,
                        onLanguageMenu = { languageMenu = it },
                        tr = tr,
                        onLanguage = onLanguage,
                        onAlerts = onAlerts,
                        onAwake = onAwake,
                        onCompact = onCompact,
                    )
                    SubscriptionCard(tr, onRemoveAds, onManageSubscription)
                    LegalCard(
                        tr = tr,
                        consentPrivacyRequired = consentPrivacyRequired,
                        onPrivacy = onPrivacy,
                        onTerms = onTerms,
                        onSafety = onSafety,
                        onSupport = onSupport,
                        onPrivacyChoices = onPrivacyChoices,
                        includeDelete = false,
                        onDelete = { confirmDelete = true },
                    )
                    SettingsCard {
                        MinimalActionRow(
                            title = tr("deleteData", "Clear local data"),
                            danger = true,
                            onClick = { confirmDelete = true },
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("deleteConfirm", "Delete local app data?")) },
            text = { Text(tr("deleteData", "Clear local data")) },
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
private fun PreferencesCard(
    settings: SettingsState,
    strings: KitchenStrings,
    languageMenu: Boolean,
    onLanguageMenu: (Boolean) -> Unit,
    tr: Translate,
    onLanguage: (String) -> Unit,
    onAlerts: (Boolean) -> Unit,
    onAwake: (Boolean) -> Unit,
    onCompact: (Boolean) -> Unit,
) {
    SettingsCard {
        Box {
            MinimalActionRow(
                title = tr("language", "Language"),
                value = strings.languageName(
                    settings.languageTag,
                    KitchenStrings.supportedLocales.firstOrNull {
                        settings.languageTag.startsWith(it.tag.substringBefore("-"), true)
                    }?.fallbackName ?: "English",
                ),
                onClick = { onLanguageMenu(true) },
            )
            DropdownMenu(
                expanded = languageMenu,
                onDismissRequest = { onLanguageMenu(false) },
            ) {
                KitchenStrings.supportedLocales.forEach { locale ->
                    DropdownMenuItem(
                        text = { Text(strings.languageName(locale.tag, locale.fallbackName)) },
                        onClick = {
                            onLanguage(locale.tag)
                            onLanguageMenu(false)
                        },
                    )
                }
            }
        }
        SettingsDivider()
        MinimalToggleRow(tr("alerts", "Alerts"), settings.alerts, onAlerts)
        SettingsDivider()
        MinimalToggleRow(tr("awake", "Keep screen awake"), settings.keepAwake, onAwake)
        SettingsDivider()
        MinimalToggleRow(tr("compact", "Compact density"), settings.compactLive, onCompact)
    }
}

@Composable
private fun SubscriptionCard(
    tr: Translate,
    onRemoveAds: () -> Unit,
    onManageSubscription: () -> Unit,
) {
    SettingsCard {
        MinimalActionRow(tr("removeAds", "Remove ads"), onClick = onRemoveAds)
        SettingsDivider()
        MinimalActionRow(tr("manageSub", "Manage subscription"), onClick = onManageSubscription)
    }
}

@Composable
private fun LegalCard(
    tr: Translate,
    consentPrivacyRequired: Boolean,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onSafety: () -> Unit,
    onSupport: () -> Unit,
    onPrivacyChoices: () -> Unit,
    includeDelete: Boolean,
    onDelete: () -> Unit,
) {
    SettingsCard {
        MinimalActionRow(tr("privacy", "Privacy Policy"), onClick = onPrivacy)
        SettingsDivider()
        MinimalActionRow(tr("terms", "Terms of Use"), onClick = onTerms)
        SettingsDivider()
        MinimalActionRow(tr("safety", "Safety Notice"), onClick = onSafety)
        SettingsDivider()
        MinimalActionRow(tr("support", "Support"), onClick = onSupport)
        if (consentPrivacyRequired) {
            SettingsDivider()
            MinimalActionRow(tr("privacyChoices", "Privacy choices"), onClick = onPrivacyChoices)
        }
        if (includeDelete) {
            SettingsDivider()
            MinimalActionRow(
                title = tr("deleteData", "Clear local data"),
                danger = true,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun MinimalToggleRow(
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        Switch(checked = value, onCheckedChange = onValueChange)
    }
}

@Composable
private fun MinimalActionRow(
    title: String,
    value: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = if (danger) KitchenColors.TerraDeep else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        if (!value.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ) {
                Text(
                    value,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
    )
}
