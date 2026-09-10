package com.androkiosk.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val REQUIRED_TAPS = 5
private const val TAP_WINDOW_MS = 2_000L

@Composable
fun ExitGestureArea(
    onTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var taps by remember { mutableIntStateOf(0) }
    var firstTap by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures {
                val now = System.currentTimeMillis()
                if (now - firstTap > TAP_WINDOW_MS) {
                    firstTap = now
                    taps = 1
                } else {
                    taps++
                }
                if (taps >= REQUIRED_TAPS) {
                    taps = 0
                    onTriggered()
                }
            }
        },
        content = content,
    )
}
