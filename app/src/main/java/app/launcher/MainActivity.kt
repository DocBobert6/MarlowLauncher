package app.launcher

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import app.launcher.data.HeadphoneMonitor
import app.launcher.data.NowBriefReceiver
import app.launcher.data.WallpaperHelper
import app.launcher.model.ThemeMode
import app.launcher.ui.HomeScreen
import app.launcher.ui.HomeViewModel
import app.launcher.ui.theme.LauncherTheme

/**
 * Phase 2 home: the real launcher — paged grid, dock, drawer,
 * folders, icon packs, long-press sheet, overview/edit mode, plus
 * universal search, At a Glance / Now Brief, themes, wallpaper,
 * stickers and headphone music. The ViewModel is created here so
 * the home survives config changes.
 *
 * The theme comes from settings (mode / theme / accent / font /
 * AMOLED), not the system default. The window background is
 * transparent so the system wallpaper can show through when the
 * user has turned the home wallpaper layer on (#53/#77); the home
 * surface is opaque otherwise.
 *
 * #57 hide status bar is applied here from settings via the
 * WindowInsetsController. #61 the headphone monitor is registered
 * here for the activity's lifetime.
 */
class MainActivity : ComponentActivity() {

    private var headphoneMonitor: HeadphoneMonitor? = null

    // #37 the Now Brief notification's launch extra, carried as
    // state so a tap while the home is already running (singleTask
    // → onNewIntent) opens the sheet too. Defaults to false: a
    // plain launch — including the Robolectric startup gate — is
    // unaffected, and HomeScreen consumes the flag once it opens
    // the sheet.
    private val openNowBriefRequest = mutableStateOf(false)

    // #51 blur glass: the background-blur radius currently applied
    // to the window (0 = none). Starts at the no-blur default, so a
    // plain launch never calls setBackgroundBlurRadius at all; the
    // radius is only (re)applied when the desired value changes.
    private var appliedBlurRadius = 0

    companion object {
        /** #51 blur-glass radius in pixels (API range 0–150): a
         *  frosted read that still lets the wallpaper show
         *  through. */
        private const val BLUR_GLASS_RADIUS_PX = 32
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        if (intent?.getBooleanExtra(NowBriefReceiver.EXTRA_OPEN_NOW_BRIEF, false) == true) {
            openNowBriefRequest.value = true
        }
        val viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        headphoneMonitor = HeadphoneMonitor { viewModel.onHeadphonesConnected() }.register(this)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val wallpaperVersion by viewModel.wallpaperVersion.collectAsState()
            // #50 Ambient: the wallpaper's primary colour, re-derived
            // when the theme is (re)selected or a wallpaper is
            // applied. Null falls back to the chosen accent.
            val ambientAccent = remember(settings.launcherTheme, wallpaperVersion) {
                if (settings.launcherTheme == app.launcher.model.LauncherThemeChoice.AMBIENT) {
                    WallpaperHelper.ambientAccent(applicationContext)
                } else {
                    null
                }
            }
            LauncherTheme(
                mode = settings.themeMode,
                theme = settings.launcherTheme,
                accent = settings.accent,
                font = settings.fontChoice,
                amoledBlack = settings.amoledBlack,
                ambientAccent = ambientAccent,
            ) {
                // Audit fix (2026-11-26): the system-bar icon colour
                // follows the theme — dark icons on the light theme,
                // light icons on dark — so the status and navigation
                // bars stay readable against the warm backgrounds.
                // Harmless when the bars are hidden (#57 / #154).
                val darkTheme = when (settings.themeMode) {
                    ThemeMode.AUTO -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                // #57 hide / show the status bar from settings, plus
                // #154 the general bar control. #57 is the immersive
                // mode; #154 HIDE_BOTH hides both bars (and wins over
                // #57 for the status bar), HIDE_NAVIGATION hides only
                // the navigation bar, SYSTEM_DEFAULT leaves the bars
                // alone except for #57.
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                    // #57: when bars are hidden, a swipe brings them
                    // back transiently (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
                    // instead of the hidden state sticking until the
                    // app is left — that is what "ultra-immersive" means
                    // here. Set once: harmless when the bars are shown.
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    when (settings.barVisibility) {
                        app.launcher.model.BarVisibility.HIDE_BOTH -> {
                            controller.hide(
                                WindowInsetsCompat.Type.statusBars() or
                                    WindowInsetsCompat.Type.navigationBars(),
                            )
                        }
                        app.launcher.model.BarVisibility.HIDE_NAVIGATION -> {
                            controller.hide(WindowInsetsCompat.Type.navigationBars())
                            if (settings.hideStatusBar) {
                                controller.hide(WindowInsetsCompat.Type.statusBars())
                            } else {
                                controller.show(WindowInsetsCompat.Type.statusBars())
                            }
                        }
                        app.launcher.model.BarVisibility.SYSTEM_DEFAULT -> {
                            controller.show(WindowInsetsCompat.Type.navigationBars())
                            if (settings.hideStatusBar) {
                                controller.hide(WindowInsetsCompat.Type.statusBars())
                            } else {
                                controller.show(WindowInsetsCompat.Type.statusBars())
                            }
                        }
                    }
                }
                // #51 blur glass: true backdrop blur of the system
                // wallpaper behind the window, via
                // Window.setBackgroundBlurRadius (API 31+). Only
                // when blur glass is on and a wallpaper is visible;
                // the try/catch covers devices that report the API
                // but cannot apply it, and the radius is only
                // applied when it changes (see appliedBlurRadius).
                SideEffect {
                    val desiredBlurRadius =
                        if (settings.surfaceStyle == app.launcher.model.SurfaceStyle.BLUR_GLASS &&
                            settings.wallpaperVisible
                        ) {
                            BLUR_GLASS_RADIUS_PX
                        } else {
                            0
                        }
                    if (desiredBlurRadius != appliedBlurRadius) {
                        appliedBlurRadius = desiredBlurRadius
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                window.setBackgroundBlurRadius(desiredBlurRadius)
                            }
                        } catch (e: Exception) {
                            // Blur behind the window unsupported on
                            // this device; the translucent surfaces
                            // stay as the blur-glass read.
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (settings.wallpaperVisible) Color.Transparent else MaterialTheme.colorScheme.background,
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        openNowBrief = openNowBriefRequest.value,
                        onNowBriefOpened = { openNowBriefRequest.value = false },
                    )
                }
            }
        }
    }

    // #37 singleTask: a notification tap while the home is already
    // running arrives here, not in onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NowBriefReceiver.EXTRA_OPEN_NOW_BRIEF, false)) {
            openNowBriefRequest.value = true
        }
    }

    override fun onDestroy() {
        headphoneMonitor?.unregister(this)
        headphoneMonitor = null
        super.onDestroy()
    }
}
