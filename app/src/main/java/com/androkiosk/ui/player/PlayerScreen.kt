@file:OptIn(UnstableApi::class)

package com.androkiosk.ui.player

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.delay
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.androkiosk.R
import com.androkiosk.data.KioskSettings
import com.androkiosk.sequence.FULL_LENGTH
import com.androkiosk.sequence.MediaItem
import com.androkiosk.sequence.MediaKind
import com.androkiosk.sequence.Sequence
import com.androkiosk.sequence.Transition
import com.androkiosk.sequence.TransitionType
import com.androkiosk.ui.KioskViewModel
import com.androkiosk.ui.components.ExitGestureArea
import com.androkiosk.ui.components.PinDialog
import androidx.compose.ui.graphics.Color as ComposeColor

private const val REQUIRED_BACK_PRESSES = 3
private const val BACK_WINDOW_MS = 2_000L
private const val START_TIMEOUT_MS = 8_000L

@Composable
fun PlayerScreen(
    sequence: Sequence,
    settings: KioskSettings,
    viewModel: KioskViewModel,
    onExit: () -> Unit,
) {
    var verifying by remember { mutableStateOf(false) }
    var backPresses by remember { mutableIntStateOf(0) }
    var lastBack by remember { mutableLongStateOf(0L) }
    val requestExit = { if (settings.pin == null) onExit() else verifying = true }

    BackHandler(true) {
        if (!settings.tripleBack) {
            requestExit()
        } else {
            val now = System.currentTimeMillis()
            backPresses = if (now - lastBack > BACK_WINDOW_MS) 1 else backPresses + 1
            lastBack = now
            if (backPresses >= REQUIRED_BACK_PRESSES) {
                backPresses = 0
                requestExit()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ComposeColor.Black),
    ) {
        SequencePlayer(sequence, Modifier.fillMaxSize())
        ExitGestureArea(
            onTriggered = requestExit,
            modifier = Modifier.fillMaxSize(),
        ) { }
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
                    onExit()
                }
                granted
            },
        )
    }
}

@Composable
private fun SequencePlayer(sequence: Sequence, modifier: Modifier = Modifier) {
    val items = sequence.items
    if (items.isEmpty()) return

    var index by remember(items) { mutableIntStateOf(0) }
    val current = items[index.coerceIn(items.indices)]
    val advance = { index = (index + 1) % items.size }

    AnimatedContent(
        targetState = current,
        modifier = modifier,
        transitionSpec = { contentTransform(sequence.transition) },
        label = "slide",
    ) { item ->
        Slide(
            item = item,
            loop = items.size == 1,
            onFinished = { if (item.id == current.id) advance() },
        )
    }
}

@Composable
private fun Slide(item: MediaItem, loop: Boolean, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)

    when (item.kind) {
        MediaKind.Image -> AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        MediaKind.Video -> VideoSlide(item, loop, finish)
    }

    if (item.seconds > FULL_LENGTH && !loop) {
        LaunchedEffect(item.id) {
            delay(item.seconds * 1000L)
            finish()
        }
    }
}

@Composable
private fun VideoSlide(item: MediaItem, loop: Boolean, onFinished: () -> Unit) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onFinished)

    var started by remember(item.id) { mutableStateOf(false) }

    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(item.uri))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) started = true
                if (state == Player.STATE_ENDED) finish()
            }

            override fun onPlayerError(error: PlaybackException) {
                finish()
            }
        }
        player.addListener(listener)
        if (player.playerError != null) finish()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(item.id) {
        delay(START_TIMEOUT_MS)
        if (!started) finish()
    }

    LifecycleStartEffect(player) {
        player.play()
        onStopOrDispose { player.pause() }
    }

    AndroidView(
        factory = {
            LayoutInflater.from(it).inflate(R.layout.player_surface, FrameLayout(it), false) as PlayerView
        },
        modifier = Modifier.fillMaxSize(),
        update = { it.player = player },
        onRelease = { it.player = null },
    )
}

private fun contentTransform(transition: Transition): ContentTransform {
    val size = SizeTransform(clip = false)
    if (!transition.enabled) {
        return ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = size)
    }
    val millis = transition.millis
    val half = millis / 2
    val shift = tween<IntOffset>(millis, easing = FastOutSlowInEasing)

    val (enter, exit) = when (transition.type) {
        TransitionType.Crossfade ->
            fadeIn(tween(millis, easing = LinearEasing)) to fadeOut(snap(delayMillis = millis))

        TransitionType.Fade ->
            fadeIn(tween(half, delayMillis = half, easing = LinearEasing)) to
                fadeOut(tween(half, easing = LinearEasing))

        TransitionType.SlideRight ->
            slideInHorizontally(shift) { -it } to slideOutHorizontally(shift) { it }

        TransitionType.SlideLeft ->
            slideInHorizontally(shift) { it } to slideOutHorizontally(shift) { -it }

        TransitionType.SlideDown ->
            slideInVertically(shift) { -it } to slideOutVertically(shift) { it }

        TransitionType.SlideUp ->
            slideInVertically(shift) { it } to slideOutVertically(shift) { -it }
    }
    return ContentTransform(enter, exit, targetContentZIndex = 1f, sizeTransform = size)
}
