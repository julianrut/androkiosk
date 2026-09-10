package com.androkiosk.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.getSystemService
import com.androkiosk.R
import com.androkiosk.data.LanguagePreference
import com.androkiosk.data.withLanguage
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.serverConfig
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ServerSocket
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private const val PIN_HEADER = "X-Upload-Pin"
private const val MAX_ATTEMPTS = 5
private const val IDLE_TIMEOUT_SECONDS = 3600
private val PREFERRED_PORTS = intArrayOf(8080, 8088, 8888)

private val WEB_STRINGS = mapOf(
    "enterPin" to R.string.web_enter_pin,
    "sendFiles" to R.string.web_send_files,
    "choose" to R.string.web_choose,
    "drag" to R.string.web_drag,
    "wrongPin" to R.string.web_wrong_pin,
    "offline" to R.string.web_offline,
    "sent" to R.string.web_sent,
    "failed" to R.string.web_failed,
    "rejected" to R.string.web_rejected,
)

class UploadServer(
    private val context: Context,
    private val onReceived: (File) -> Unit,
) {
    val pin: String = SecureRandom().let { random -> buildString { repeat(4) { append(random.nextInt(10)) } } }

    private val failures = AtomicInteger()
    private var engine: EmbeddedServer<*, *>? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var port = 0

    val address: String?
        get() = LocalAddress.find()?.let { "http://$it:$port" }

    fun start() {
        if (engine != null) return
        port = freePort()
        acquireWifiLock()
        engine = embeddedServer(
            CIO,
            serverConfig { module { routes() } },
        ) {
            connector { port = this@UploadServer.port }
            connectionIdleTimeoutSeconds = IDLE_TIMEOUT_SECONDS
        }.also { it.start(wait = false) }
    }

    private fun Application.routes() {
        routing {
            get("/") {
                call.respondBytes(renderPage(), ContentType.Text.Html)
            }
            post("/verify") {
                val supplied = call.receiveText().trim()
                call.respondText(
                    "",
                    status = if (accepts(supplied)) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                )
            }
            post("/upload") {
                if (!accepts(call.request.headers[PIN_HEADER])) {
                    call.respondText("", status = HttpStatusCode.Unauthorized)
                    return@post
                }
                val saved = call.readUpload()
                if (saved == null) {
                    call.respondText("", status = HttpStatusCode.BadRequest)
                } else {
                    onReceived(saved)
                    call.respondText("", status = HttpStatusCode.Created)
                }
            }
        }
    }

    fun stop() {
        engine?.stop(0, 0)
        engine = null
        wifiLock?.takeIf { it.isHeld }?.release()
        wifiLock = null
    }

    private fun renderPage(): ByteArray {
        val language = LanguagePreference.read(context)
        val localized = context.withLanguage(language)
        var html = context.assets.open("upload.html").use { it.readBytes() }.decodeToString()
        html = html.replace("{{lang}}", language.tag)
        WEB_STRINGS.forEach { (key, id) -> html = html.replace("{{" + key + "}}", localized.getString(id)) }
        return html.encodeToByteArray()
    }

    private fun acquireWifiLock() {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = context.getSystemService<WifiManager>()
            ?.createWifiLock(mode, "androkiosk:upload")
            ?.apply { acquire() }
    }

    private fun accepts(supplied: String?): Boolean {
        if (failures.get() >= MAX_ATTEMPTS) return false
        if (supplied == pin) return true
        failures.incrementAndGet()
        return false
    }

    private fun safeName(raw: String?): String {
        val cleaned = raw.orEmpty()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .filter { it.isLetterOrDigit() || it in " ._-()[]" }
            .trim()
        return cleaned.ifEmpty { UUID.randomUUID().toString() }
    }

    private fun unique(directory: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "")
        var candidate = File(directory, name)
        var index = 2
        while (candidate.exists()) {
            val next = if (extension.isEmpty()) "$base ($index)" else "$base ($index).$extension"
            candidate = File(directory, next)
            index++
        }
        return candidate
    }

    private fun freePort(): Int {
        PREFERRED_PORTS.forEach { candidate ->
            runCatching { ServerSocket(candidate).close() }.onSuccess { return candidate }
        }
        return ServerSocket(0).use { it.localPort }
    }

    private suspend fun ApplicationCall.readUpload(): File? {
        var saved: File? = null
        receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem && saved == null) {
                saved = store(part)
            }
            part.release()
        }
        return saved
    }

    private suspend fun store(part: PartData.FileItem): File? = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "media").apply { mkdirs() }
        val target = unique(directory, safeName(part.originalFileName))
        runCatching {
            part.provider().copyAndClose(target.writeChannel(Dispatchers.IO))
            target
        }.getOrElse {
            target.delete()
            null
        }
    }
}
