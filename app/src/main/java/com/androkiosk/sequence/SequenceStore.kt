package com.androkiosk.sequence

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.sequenceStore by preferencesDataStore("sequences")

private val Sequences = stringPreferencesKey("sequences")

class SequenceStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val sequences: Flow<List<Sequence>> = context.sequenceStore.data.map { stored ->
        decode(stored[Sequences]).sortedByDescending { it.createdAt }
    }

    suspend fun add(sequence: Sequence) {
        context.sequenceStore.edit { stored ->
            stored.write(decode(stored[Sequences]) + sequence)
        }
    }

    suspend fun update(id: String, transform: (Sequence) -> Sequence) {
        context.sequenceStore.edit { stored ->
            val current = decode(stored[Sequences])
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) return@edit
            stored.write(current.toMutableList().apply { set(index, transform(current[index])) })
        }
    }

    suspend fun delete(id: String) {
        context.sequenceStore.edit { stored ->
            stored.write(decode(stored[Sequences]).filterNot { it.id == id })
        }
    }

    private fun MutablePreferences.write(sequences: List<Sequence>) {
        this[Sequences] = json.encodeToString(sequences)
    }

    private fun decode(raw: String?): List<Sequence> =
        raw?.let { runCatching { json.decodeFromString<List<Sequence>>(it) }.getOrNull() }.orEmpty()
}
