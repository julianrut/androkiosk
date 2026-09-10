package com.androkiosk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.androkiosk.data.KioskSettings
import com.androkiosk.ui.player.PlayerScreen
import com.androkiosk.ui.sequences.SequenceEditorScreen
import com.androkiosk.ui.sequences.SequenceViewModel
import com.androkiosk.ui.sequences.SequencesScreen

private sealed interface Screen {
    data object Home : Screen
    data object Sequences : Screen
    data object Settings : Screen
    data class Editor(val id: String) : Screen
    data class Player(val id: String) : Screen
}

@Composable
fun Root(settings: KioskSettings, kiosk: KioskViewModel, onPlayingChange: (Boolean) -> Unit) {
    val sequenceViewModel: SequenceViewModel = viewModel(factory = SequenceViewModel.Factory)
    val sequences by sequenceViewModel.sequences.collectAsStateWithLifecycle()
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val pop: () -> Unit = { if (stack.size > 1) { stack.removeAt(stack.lastIndex) } }

    val playing = stack.last() is Screen.Player
    LaunchedEffect(playing) { onPlayingChange(playing) }

    BackHandler(stack.size > 1 && !playing) { pop() }

    when (val current = stack.last()) {
        Screen.Home -> HomeScreen(
            settings = settings,
            viewModel = kiosk,
            onOpenSequences = { stack.add(Screen.Sequences) },
            onOpenSettings = { stack.add(Screen.Settings) },
        )

        Screen.Sequences -> SequencesScreen(
            sequences = sequences.orEmpty(),
            viewModel = sequenceViewModel,
            onBack = pop,
            onEdit = { stack.add(Screen.Editor(it)) },
            onPlay = { stack.add(Screen.Player(it)) },
        )

        Screen.Settings -> SettingsScreen(settings, kiosk, pop)

        is Screen.Editor -> {
            val sequence = sequences?.firstOrNull { it.id == current.id }
            if (sequence == null) {
                LaunchedEffect(current.id) { pop() }
            } else {
                SequenceEditorScreen(
                    sequence = sequence,
                    viewModel = sequenceViewModel,
                    onBack = pop,
                    onPlay = { stack.add(Screen.Player(sequence.id)) },
                )
            }
        }

        is Screen.Player -> {
            val sequence = sequences?.firstOrNull { it.id == current.id }
            if (sequence == null) {
                LaunchedEffect(current.id) { pop() }
            } else {
                PlayerScreen(sequence, settings, kiosk, pop)
            }
        }
    }
}
