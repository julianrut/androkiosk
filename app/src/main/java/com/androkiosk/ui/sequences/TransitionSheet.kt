package com.androkiosk.ui.sequences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androkiosk.R
import com.androkiosk.sequence.MAX_TRANSITION_MS
import com.androkiosk.sequence.MIN_TRANSITION_MS
import com.androkiosk.sequence.Transition
import com.androkiosk.sequence.TransitionType
import com.androkiosk.ui.components.SwitchRow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransitionSheet(
    transition: Transition,
    onChange: (Transition) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.transition),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )

            SwitchRow(
                title = stringResource(R.string.transition_enabled),
                checked = transition.enabled,
                onCheckedChange = { onChange(transition.copy(enabled = it)) },
            )

            if (transition.enabled) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TransitionType.entries.forEach { type ->
                        FilterChip(
                            selected = type == transition.type,
                            onClick = { onChange(transition.copy(type = type)) },
                            label = { Text(stringResource(type.labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.transition_length),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.seconds_short, transition.millis.asSeconds()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = transition.millis.toFloat(),
                        onValueChange = { onChange(transition.copy(millis = it.toInt())) },
                        valueRange = MIN_TRANSITION_MS.toFloat()..MAX_TRANSITION_MS.toFloat(),
                        steps = (MAX_TRANSITION_MS - MIN_TRANSITION_MS) / 100 - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
    }
}

val TransitionType.labelRes: Int
    get() = when (this) {
        TransitionType.Crossfade -> R.string.transition_crossfade
        TransitionType.Fade -> R.string.transition_fade
        TransitionType.SlideRight -> R.string.transition_slide_right
        TransitionType.SlideLeft -> R.string.transition_slide_left
        TransitionType.SlideDown -> R.string.transition_slide_down
        TransitionType.SlideUp -> R.string.transition_slide_up
    }

fun Int.asSeconds(): String = String.format(Locale.getDefault(), "%.1f", this / 1000f)
