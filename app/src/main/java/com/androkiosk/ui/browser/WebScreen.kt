package com.androkiosk.ui.browser

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.androkiosk.R
import com.androkiosk.data.KioskSettings
import com.androkiosk.ui.KioskViewModel
import com.androkiosk.ui.components.PinDialog
import androidx.compose.ui.graphics.Color as ComposeColor

private const val REQUIRED_BACK_PRESSES = 3
private const val BACK_WINDOW_MS = 2_000L

private val allowedSchemes = setOf("http", "https")

private object KioskWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.url.scheme?.lowercase() !in allowedSchemes
}

private object KioskWebChromeClient : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        request.grant(request.resources)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: android.webkit.GeolocationPermissions.Callback,
    ) {
        callback.invoke(origin, true, false)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String, settings: KioskSettings, viewModel: KioskViewModel, onExit: () -> Unit) {
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
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    webViewClient = KioskWebViewClient
                    webChromeClient = KioskWebChromeClient
                    setBackgroundColor(Color.BLACK)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    isLongClickable = false
                    setOnLongClickListener { true }
                    this.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        setSupportZoom(false)
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        allowFileAccess = false
                        allowContentAccess = false
                        setGeolocationEnabled(true)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { if (it.url != url) it.loadUrl(url) },
            onRelease = {
                it.stopLoading()
                it.destroy()
            },
        )
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
