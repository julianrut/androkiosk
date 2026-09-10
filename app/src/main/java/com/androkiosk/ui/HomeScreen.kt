package com.androkiosk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androkiosk.BuildConfig
import com.androkiosk.R
import com.androkiosk.data.KioskSettings
import com.androkiosk.ui.components.ActionTile
import com.androkiosk.ui.components.ExitGestureArea
import com.androkiosk.ui.components.PinDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: KioskSettings,
    viewModel: KioskViewModel,
    onOpenSequences: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var verifying by remember { mutableStateOf(false) }

    val requestSettings = {
        if (settings.pin == null) onOpenSettings() else verifying = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        ExitGestureArea(
            onTriggered = requestSettings,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionTile(stringResource(R.string.sequences), R.drawable.ic_sequences, onOpenSequences)
                ActionTile(stringResource(R.string.settings), R.drawable.ic_settings, requestSettings)
            }
        }
    }

    if (verifying) {
        PinDialog(
            title = stringResource(R.string.pin_verify_title),
            confirmLabel = stringResource(R.string.continue_action),
            errorLabel = stringResource(R.string.pin_wrong_error),
            onDismiss = { verifying = false },
            onSubmit = { entered ->
                val digest = settings.pin
                val granted = digest != null && viewModel.matches(entered, digest)
                if (granted) {
                    verifying = false
                    onOpenSettings()
                }
                granted
            },
        )
    }
}
