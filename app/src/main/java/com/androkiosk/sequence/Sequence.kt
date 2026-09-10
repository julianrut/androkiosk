package com.androkiosk.sequence

import kotlinx.serialization.Serializable

const val DEFAULT_IMAGE_SECONDS = 10
const val FULL_LENGTH = 0
const val MIN_TRANSITION_MS = 100
const val MAX_TRANSITION_MS = 2000

@Serializable
enum class MediaKind { Image, Video }

@Serializable
data class MediaItem(
    val id: String,
    val uri: String,
    val name: String,
    val kind: MediaKind,
    val seconds: Int,
    val sourceSeconds: Int = 0,
)

@Serializable
enum class TransitionType { Crossfade, Fade, SlideRight, SlideLeft, SlideDown, SlideUp }

@Serializable
data class Transition(
    val enabled: Boolean = true,
    val type: TransitionType = TransitionType.Crossfade,
    val millis: Int = 600,
)

@Serializable
data class Sequence(
    val id: String,
    val name: String,
    val items: List<MediaItem> = emptyList(),
    val createdAt: Long = 0,
    val transition: Transition = Transition(),
)
