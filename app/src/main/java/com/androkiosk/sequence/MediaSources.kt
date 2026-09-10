package com.androkiosk.sequence

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.util.UUID

object MediaSources {
    val documentTypes = arrayOf("image/*", "video/*")

    private val videoExtensions = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi", "m4v", "ts")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")

    fun from(context: Context, uri: Uri, persist: Boolean): MediaItem? {
        if (persist) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val kind = when (context.contentResolver.getType(uri)?.substringBefore('/')) {
            "image" -> MediaKind.Image
            "video" -> MediaKind.Video
            else -> return null
        }
        return MediaItem(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            name = displayName(context, uri),
            kind = kind,
            seconds = if (kind == MediaKind.Image) DEFAULT_IMAGE_SECONDS else FULL_LENGTH,
            sourceSeconds = if (kind == MediaKind.Video) videoSeconds(context, uri) else 0,
        )
    }

    fun fromFile(context: Context, file: java.io.File): MediaItem? {
        val uri = Uri.fromFile(file)
        val kind = when (file.extension.lowercase()) {
            in videoExtensions -> MediaKind.Video
            in imageExtensions -> MediaKind.Image
            else -> return null
        }
        return MediaItem(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            name = file.name,
            kind = kind,
            seconds = if (kind == MediaKind.Image) DEFAULT_IMAGE_SECONDS else FULL_LENGTH,
            sourceSeconds = if (kind == MediaKind.Video) videoSeconds(context, uri) else 0,
        )
    }

    fun videoSeconds(context: Context, uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            millis?.let { ((it + 500) / 1000).toInt() } ?: 0
        } catch (e: RuntimeException) {
            0
        } finally {
            retriever.release()
        }
    }

    private fun displayName(context: Context, uri: Uri): String {
        val name = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }
        return name ?: uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }
}
