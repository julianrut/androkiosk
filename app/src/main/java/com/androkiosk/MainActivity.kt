package com.androkiosk

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.androkiosk.data.LanguagePreference
import com.androkiosk.data.withLanguage
import com.androkiosk.kiosk.KioskController
import com.androkiosk.ui.KioskViewModel
import com.androkiosk.ui.Root
import com.androkiosk.ui.theme.AndrokioskTheme

private const val BAR_HIDE_DELAY_MS = 3_000L

class MainActivity : ComponentActivity() {
    private val gestureBlocker by lazy { (application as AndrokioskApp).gestureBlocker }
    private val rehideBars = Runnable { if (immersive) hideSystemBars(true) }
    private var kiosk = false
    private var immersive = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withLanguage(LanguagePreference.read(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val backLock = onBackPressedDispatcher.addCallback(this, enabled = false) {}

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            if (immersive && insets.isVisible(WindowInsetsCompat.Type.systemBars())) scheduleRehide()
            insets
        }

        setContent {
            AndrokioskTheme {
                val viewModel: KioskViewModel = viewModel(factory = KioskViewModel.Factory)
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                var playing by remember { mutableStateOf(false) }

                settings?.let { current ->
                    LaunchedEffect(current.kioskEnabled) {
                        kiosk = current.kioskEnabled
                        backLock.isEnabled = current.kioskEnabled
                        applyKiosk(current.kioskEnabled)
                    }
                    LaunchedEffect(current.kioskEnabled, playing) {
                        applyImmersive(current.kioskEnabled || playing)
                    }
                    Root(current, viewModel) { playing = it }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (kiosk) KioskController.engage(this)
        if (immersive) {
            gestureBlocker.attach(statusBarInset())
            hideSystemBars(true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && immersive) hideSystemBars(true)
    }

    private fun applyKiosk(enabled: Boolean) {
        if (enabled) KioskController.engage(this) else KioskController.release(this)
    }

    private fun applyImmersive(enabled: Boolean) {
        immersive = enabled
        hideSystemBars(enabled)
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            gestureBlocker.attach(statusBarInset())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.decorView.removeCallbacks(rehideBars)
            gestureBlocker.detach()
        }
    }

    private fun scheduleRehide() {
        window.decorView.removeCallbacks(rehideBars)
        window.decorView.postDelayed(rehideBars, BAR_HIDE_DELAY_MS)
    }

    private fun statusBarInset(): Int =
        ViewCompat.getRootWindowInsets(window.decorView)
            ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
            ?.top
            ?: 0

    private fun hideSystemBars(hidden: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !hidden)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hidden) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
