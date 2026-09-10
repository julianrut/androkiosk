package com.androkiosk.ui.sequences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androkiosk.R
import com.androkiosk.sequence.Sequence
import com.androkiosk.ui.components.ActionTile
import com.androkiosk.ui.components.RowDivider
import com.androkiosk.ui.components.SettingRow
import com.androkiosk.ui.components.TextInputDialog
import com.androkiosk.ui.components.Thumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequencesScreen(
    sequences: List<Sequence>,
    viewModel: SequenceViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Sequence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sequences)) },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.new_sequence),
                icon = R.drawable.ic_add,
                onClick = { creating = true },
            )

            if (sequences.isEmpty()) {
                EmptySequences(Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(sequences, key = { _, item -> item.id }) { index, sequence ->
                        SequenceRow(
                            sequence = sequence,
                            onOpen = { onEdit(sequence.id) },
                            onPlay = { onPlay(sequence.id) },
                            onRename = { renaming = sequence },
                            onDelete = { viewModel.delete(sequence.id) },
                        )
                        if (index < sequences.lastIndex) RowDivider()
                    }
                }
            }
        }
    }

    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.new_sequence),
            label = stringResource(R.string.sequence_name),
            confirmLabel = stringResource(R.string.create),
            errorLabel = stringResource(R.string.sequence_name),
            onDismiss = { creating = false },
            onSubmit = { name ->
                creating = false
                onEdit(viewModel.create(name.trim()))
                true
            },
        )
    }

    renaming?.let { sequence ->
        TextInputDialog(
            title = stringResource(R.string.rename),
            label = stringResource(R.string.sequence_name),
            confirmLabel = stringResource(R.string.save),
            errorLabel = stringResource(R.string.sequence_name),
            initial = sequence.name,
            onDismiss = { renaming = null },
            onSubmit = { name ->
                viewModel.rename(sequence.id, name.trim())
                renaming = null
                true
            },
        )
    }
}

@Composable
private fun SequenceRow(
    sequence: Sequence,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    SettingRow(
        title = sequence.name,
        subtitle = pluralStringResource(R.plurals.item_count, sequence.items.size, sequence.items.size),
        onClick = onOpen,
        leading = { Thumbnail(sequence.items.firstOrNull()?.uri) },
        trailing = {
            IconButton(onClick = onPlay) {
                Icon(painterResource(R.drawable.ic_play), stringResource(R.string.play))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun EmptySequences(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.sequences_empty),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.sequences_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
