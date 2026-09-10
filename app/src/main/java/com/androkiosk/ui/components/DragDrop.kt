package com.androkiosk.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class DragDropState(private val listState: LazyListState) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    var offset by mutableFloatStateOf(0f)
        private set

    private val dragged
        get() = draggingIndex?.let { index ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }

    internal fun start(y: Float) {
        draggingIndex = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { y.toInt() in it.offset..(it.offset + it.size) }
            ?.index
        offset = 0f
    }

    internal fun drag(amount: Float, onMove: (Int, Int) -> Unit) {
        offset += amount
        val item = dragged ?: return
        val centre = item.offset + offset + item.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index != item.index && centre.toInt() in it.offset..(it.offset + it.size)
        } ?: return
        onMove(item.index, target.index)
        draggingIndex = target.index
        offset -= (target.offset - item.offset)
    }

    internal fun stop() {
        draggingIndex = null
        offset = 0f
    }
}

@Composable
fun rememberDragDropState(listState: LazyListState) = remember(listState) { DragDropState(listState) }

fun Modifier.dragToReorder(
    state: DragDropState,
    onMove: (Int, Int) -> Unit,
    onSettled: () -> Unit,
) = this.pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.start(it.y) },
        onDrag = { change, amount ->
            change.consume()
            state.drag(amount.y, onMove)
        },
        onDragEnd = {
            state.stop()
            onSettled()
        },
        onDragCancel = {
            state.stop()
            onSettled()
        },
    )
}
