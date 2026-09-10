package com.androkiosk.ui.sequences

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androkiosk.R
import com.androkiosk.sequence.FULL_LENGTH
import com.androkiosk.sequence.MediaItem
import com.androkiosk.sequence.MediaKind
import com.androkiosk.sequence.MediaSources
import com.androkiosk.sequence.Sequence
import com.androkiosk.ui.components.ActionTile
import com.androkiosk.ui.components.dragToReorder
import com.androkiosk.ui.components.rememberDragDropState
import com.androkiosk.ui.components.RowDivider
import com.androkiosk.ui.components.SettingRow
import com.androkiosk.ui.components.TextInputDialog
import com.androkiosk.ui.components.Thumbnail

private const val MAX_PICK = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceEditorScreen(
    sequence: Sequence,
    viewModel: SequenceViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    var addOpen by remember { mutableStateOf(false) }
    var durationTarget by remember { mutableStateOf<MediaItem?>(null) }
    var transitionOpen by remember { mutableStateOf(false) }
    var receiveOpen by remember { mutableStateOf(false) }

    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addMedia(sequence.id, uris, persist = true)
    }
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK),
    ) { uris ->
        viewModel.addMedia(sequence.id, uris, persist = false)
    }

    LaunchedEffect(sequence.id, sequence.items.count { it.kind == MediaKind.Video && it.sourceSeconds == 0 }) {
        viewModel.resolveMissingLengths(sequence)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sequence.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    if (sequence.items.isNotEmpty()) {
                        IconButton(onClick = onPlay) {
                            Icon(painterResource(R.drawable.ic_play), stringResource(R.string.play))
                        }
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box {
                ActionTile(
                    title = stringResource(R.string.add_media),
                    icon = R.drawable.ic_add,
                    onClick = { addOpen = true },
                )
                DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.from_files)) },
                        onClick = {
                            addOpen = false
                            files.launch(MediaSources.documentTypes)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.from_network)) },
                        onClick = {
                            addOpen = false
                            receiveOpen = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.from_gallery)) },
                        onClick = {
                            addOpen = false
                            gallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                            )
                        },
                    )
                }
            }

            ActionTile(
                title = stringResource(R.string.transition),
                icon = R.drawable.ic_transition,
                onClick = { transitionOpen = true },
            )

            if (sequence.items.isEmpty()) {
                EmptyItems(Modifier.fillMaxSize())
            } else {
                var ordered by remember(sequence.items) { mutableStateOf(sequence.items) }
                val listState = rememberLazyListState()
                val dragState = rememberDragDropState(listState)

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.dragToReorder(
                        state = dragState,
                        onMove = { from, to ->
                            ordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
                        },
                        onSettled = { viewModel.reorder(sequence.id, ordered.map { it.id }) },
                    ),
                ) {
                    itemsIndexed(ordered, key = { _, item -> item.id }) { index, item ->
                        val dragging = dragState.draggingIndex == index
                        MediaRow(
                            item = item,
                            dragging = dragging,
                            onEditDuration = { durationTarget = item },
                            onMove = { viewModel.move(sequence.id, item.id, it) },
                            onRemove = { viewModel.removeItem(sequence.id, item.id) },
                            modifier = Modifier
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer { translationY = if (dragging) dragState.offset else 0f },
                        )
                        if (index < ordered.lastIndex) RowDivider()
                    }
                }
            }
        }
    }

    if (receiveOpen) {
        ReceiveSheet(
            onFiles = { viewModel.addFiles(sequence.id, it) },
            onDismiss = { receiveOpen = false },
        )
    }

    if (transitionOpen) {
        TransitionSheet(
            transition = sequence.transition,
            onChange = { viewModel.setTransition(sequence.id, it) },
            onDismiss = { transitionOpen = false },
        )
    }

    durationTarget?.let { item ->
        val video = item.kind == MediaKind.Video
        val range = if (video) 0..3600 else 1..3600
        TextInputDialog(
            title = stringResource(R.string.duration),
            label = stringResource(R.string.seconds),
            confirmLabel = stringResource(R.string.save),
            errorLabel = stringResource(if (video) R.string.duration_error_video else R.string.duration_error),
            initial = item.seconds.toString(),
            keyboardType = KeyboardType.Number,
            helperText = if (video) stringResource(R.string.duration_full_hint) else null,
            onDismiss = { durationTarget = null },
            onSubmit = { input ->
                val seconds = input.toIntOrNull()?.takeIf { it in range }
                if (seconds != null) {
                    viewModel.setSeconds(sequence.id, item.id, seconds)
                    durationTarget = null
                }
                seconds != null
            },
        )
    }
}

@Composable
private fun MediaRow(
    item: MediaItem,
    dragging: Boolean,
    onEditDuration: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val duration = when {
        item.seconds != FULL_LENGTH -> stringResource(R.string.duration_seconds, item.seconds)
        item.sourceSeconds > 0 -> stringResource(R.string.duration_full_known, item.sourceSeconds)
        else -> stringResource(R.string.duration_full)
    }

    SettingRow(
        title = item.name,
        modifier = modifier.background(
            if (dragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
        ),
        subtitle = duration,
        onClick = onEditDuration,
        leading = { Thumbnail(item.uri) },
        trailing = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.set_duration)) },
                        onClick = {
                            menuOpen = false
                            onEditDuration()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_up)) },
                        onClick = {
                            menuOpen = false
                            onMove(-1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_down)) },
                        onClick = {
                            menuOpen = false
                            onMove(1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        onClick = {
                            menuOpen = false
                            onRemove()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun EmptyItems(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.sequence_empty),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.sequence_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
