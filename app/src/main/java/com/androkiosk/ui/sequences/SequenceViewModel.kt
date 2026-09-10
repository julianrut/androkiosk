package com.androkiosk.ui.sequences

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.androkiosk.AndrokioskApp
import com.androkiosk.sequence.MediaItem
import com.androkiosk.sequence.MediaKind
import com.androkiosk.sequence.MediaSources
import com.androkiosk.sequence.Sequence
import com.androkiosk.sequence.SequenceStore
import com.androkiosk.sequence.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SequenceViewModel(
    application: Application,
    private val store: SequenceStore,
) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()

    val sequences = store.sequences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String): String {
        val sequence = Sequence(UUID.randomUUID().toString(), name, createdAt = System.currentTimeMillis())
        viewModelScope.launch { store.add(sequence) }
        return sequence.id
    }

    fun rename(id: String, name: String) = update(id) { it.copy(name = name) }

    fun setTransition(id: String, transition: Transition) = update(id) { it.copy(transition = transition) }

    fun removeItem(id: String, itemId: String) =
        update(id) { sequence -> sequence.copy(items = sequence.items.filterNot { it.id == itemId }) }

    fun setSeconds(id: String, itemId: String, seconds: Int) = update(id) { sequence ->
        sequence.copy(items = sequence.items.map { if (it.id == itemId) it.copy(seconds = seconds) else it })
    }

    fun reorder(id: String, orderedIds: List<String>) = update(id) { sequence ->
        val byId = sequence.items.associateBy { it.id }
        val ordered = orderedIds.mapNotNull(byId::get)
        sequence.copy(items = ordered + sequence.items.filterNot { it.id in orderedIds })
    }

    fun move(id: String, itemId: String, offset: Int) = update(id) { sequence ->
        val items = sequence.items.toMutableList()
        val from = items.indexOfFirst { it.id == itemId }
        val to = from + offset
        if (from < 0 || to !in items.indices) {
            sequence
        } else {
            items.add(to, items.removeAt(from))
            sequence.copy(items = items)
        }
    }

    fun addMedia(id: String, uris: List<Uri>, persist: Boolean) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.mapNotNull { MediaSources.from(context, it, persist) }
            }
            if (added.isNotEmpty()) store.update(id) { it.copy(items = it.items + added) }
        }
    }

    fun addFiles(id: String, files: List<File>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                files.mapNotNull { MediaSources.fromFile(context, it) }
            }
            if (added.isNotEmpty()) store.update(id) { it.copy(items = it.items + added) }
        }
    }

    fun resolveMissingLengths(sequence: Sequence) {
        val pending = sequence.items.filter { it.kind == MediaKind.Video && it.sourceSeconds == 0 }
        if (pending.isEmpty()) return
        viewModelScope.launch {
            val lengths = withContext(Dispatchers.IO) {
                pending.associate { it.id to MediaSources.videoSeconds(context, it.uri.toUri()) }
            }.filterValues { it > 0 }
            if (lengths.isEmpty()) return@launch
            store.update(sequence.id) { current ->
                current.copy(
                    items = current.items.map { item ->
                        val resolved = lengths[item.id]
                        if (resolved != null && item.sourceSeconds == 0) {
                            item.copy(sourceSeconds = resolved)
                        } else {
                            item
                        }
                    },
                )
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    private fun update(id: String, transform: (Sequence) -> Sequence) {
        viewModelScope.launch { store.update(id, transform) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AndrokioskApp
                SequenceViewModel(app, app.sequences)
            }
        }
    }
}
