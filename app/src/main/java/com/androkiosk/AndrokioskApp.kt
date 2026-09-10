package com.androkiosk

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.androkiosk.data.KioskPreferences
import com.androkiosk.kiosk.GestureBlocker
import com.androkiosk.sequence.SequenceStore

class AndrokioskApp : Application(), SingletonImageLoader.Factory {
    val preferences by lazy { KioskPreferences(this) }
    val gestureBlocker by lazy { GestureBlocker(this) }
    val sequences by lazy { SequenceStore(this) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
