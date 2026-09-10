package com.androkiosk.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.androkiosk.R
import com.androkiosk.data.KioskSettings
import com.androkiosk.data.Language
import com.androkiosk.data.LanguagePreference
import com.androkiosk.data.findActivity
import com.androkiosk.kiosk.KioskController
import com.androkiosk.ui.components.ChoiceDialog
import com.androkiosk.ui.components.PinDialog
import com.androkiosk.ui.components.RowDivider
import com.androkiosk.ui.components.Section
import com.androkiosk.ui.components.SettingRow
import com.androkiosk.ui.components.StatusValue
import com.androkiosk.ui.components.SwitchRow

private enum class PinPrompt { Create, Change, Options, VerifyForKiosk, VerifyForPin }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: KioskSettings, viewModel: KioskViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var isDefaultHome by remember { mutableStateOf(KioskController.isDefaultHome(context)) }
    var gesturesBlocked by remember { mutableStateOf(KioskController.canBlockGestures(context)) }
    var showPinningNotice by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf<PinPrompt?>(null) }
    var languageOpen by remember { mutableStateOf(false) }
    val language = remember { LanguagePreference.read(context) }
    var pendingKiosk by remember { mutableStateOf(false) }

    val refresh = {
        isDefaultHome = KioskController.isDefaultHome(context)
        gesturesBlocked = KioskController.canBlockGestures(context)
    }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh()
    }

    val openSystem = { intent: Intent ->
        activity?.let { KioskController.release(it) }
        chooser.launch(intent)
    }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    val applyKiosk = { enabled: Boolean ->
        KioskController.setHomeAliasEnabled(context, enabled)
        viewModel.setKioskEnabled(enabled)
        if (enabled) {
            showPinningNotice = !settings.pinningNoticeHidden
            openSystem(KioskController.homeChooser(context))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Section(stringResource(R.string.section_general)) {
                SettingRow(
                    title = stringResource(R.string.language),
                    onClick = { languageOpen = true },
                    trailing = {
                        Text(
                            text = language.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Section(stringResource(R.string.section_kiosk)) {
                SwitchRow(
                    title = stringResource(R.string.kiosk_mode),
                    subtitle = stringResource(R.string.kiosk_mode_summary),
                    checked = settings.kioskEnabled,
                    onCheckedChange = { enabled ->
                        if (settings.pin == null) {
                            applyKiosk(enabled)
                        } else {
                            pendingKiosk = enabled
                            prompt = PinPrompt.VerifyForKiosk
                        }
                    },
                )
            }

            Section(stringResource(R.string.section_lockdown)) {
                SettingRow(
                    title = stringResource(R.string.home_app),
                    onClick = { openSystem(KioskController.homeChooser(context)) },
                    trailing = {
                        StatusValue(
                            text = stringResource(
                                if (isDefaultHome) R.string.home_app_ready else R.string.home_app_missing,
                            ),
                            ready = isDefaultHome,
                        )
                    },
                )
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.triple_back),
                    subtitle = stringResource(R.string.triple_back_summary),
                    checked = settings.tripleBack,
                    onCheckedChange = viewModel::setTripleBack,
                )
                RowDivider()
                SettingRow(
                    title = stringResource(R.string.gestures),
                    onClick = { openSystem(KioskController.overlaySettings(context)) },
                    trailing = {
                        StatusValue(
                            text = stringResource(
                                if (gesturesBlocked) R.string.gestures_blocked else R.string.gestures_open,
                            ),
                            ready = gesturesBlocked,
                        )
                    },
                )
            }

            Section(stringResource(R.string.section_pin)) {
                SettingRow(
                    title = stringResource(R.string.app_pin),
                    subtitle = stringResource(R.string.app_pin_summary),
                    onClick = {
                        prompt = if (settings.pin == null) PinPrompt.Create else PinPrompt.VerifyForPin
                    },
                    trailing = {
                        StatusValue(
                            text = stringResource(
                                if (settings.pin != null) R.string.app_pin_set else R.string.app_pin_unset,
                            ),
                            ready = settings.pin != null,
                        )
                    },
                )
            }
        }
    }

    if (languageOpen) {
        ChoiceDialog(
            title = stringResource(R.string.language),
            options = Language.entries,
            selected = language,
            label = { it.label },
            onSelect = { chosen ->
                languageOpen = false
                if (chosen != language) {
                    LanguagePreference.write(context, chosen)
                    context.findActivity()?.recreate()
                }
            },
            onDismiss = { languageOpen = false },
        )
    }

    when (prompt) {
        PinPrompt.Create, PinPrompt.Change -> PinDialog(
            title = stringResource(
                if (prompt == PinPrompt.Create) R.string.pin_create_title else R.string.pin_change_title,
            ),
            confirmLabel = stringResource(R.string.save),
            errorLabel = stringResource(R.string.pin_length_error),
            requireRepeat = true,
            onDismiss = { prompt = null },
            onSubmit = { pin ->
                viewModel.setPin(pin)
                prompt = null
                true
            },
        )

        PinPrompt.Options -> AlertDialog(
            onDismissRequest = { prompt = null },
            title = { Text(stringResource(R.string.app_pin)) },
            confirmButton = {
                TextButton(onClick = { prompt = PinPrompt.Change }) {
                    Text(stringResource(R.string.pin_change))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearPin()
                        prompt = null
                    },
                ) {
                    Text(stringResource(R.string.pin_remove))
                }
            },
        )

        PinPrompt.VerifyForKiosk, PinPrompt.VerifyForPin -> PinDialog(
            title = stringResource(R.string.pin_verify_title),
            confirmLabel = stringResource(R.string.continue_action),
            errorLabel = stringResource(R.string.pin_wrong_error),
            onDismiss = { prompt = null },
            onSubmit = { entered ->
                val digest = settings.pin
                val granted = digest != null && viewModel.matches(entered, digest)
                if (granted) {
                    if (prompt == PinPrompt.VerifyForKiosk) {
                        applyKiosk(pendingKiosk)
                        prompt = null
                    } else {
                        prompt = PinPrompt.Options
                    }
                }
                granted
            },
        )

        null -> Unit
    }

    if (showPinningNotice) {
        PinningNotice(
            onDismiss = { hideForever ->
                if (hideForever) viewModel.setPinningNoticeHidden(true)
                showPinningNotice = false
            },
        )
    }
}

@Composable
private fun PinningNotice(onDismiss: (Boolean) -> Unit) {
    var hideForever by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(hideForever) },
        title = { Text(stringResource(R.string.pinning_notice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.pinning_notice_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.pinning_notice_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hideForever = !hideForever }
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Checkbox(
                        checked = hideForever,
                        onCheckedChange = { hideForever = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.pinning_notice_dismiss),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(hideForever) }) { Text(stringResource(R.string.got_it)) }
        },
    )
}
