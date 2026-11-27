package app.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Phase 5 accessibility helpers (#175) + debug info (#171).
 *
 * Reduced motion: the launcher honours the system setting — the
 * animator-duration scale the system exposes. When the user (or the
 * system, e.g. battery saver) has turned animations off, the scale
 * reads 0 and the launcher's own animated transitions become instant
 * jumps instead (see Widgets.kt Smart Stack auto-rotate; the home
 * pager already navigates with scrollToPage, never an animation).
 * There is no launcher-level reduced-motion toggle: the system
 * setting is the source of truth.
 *
 * Touch targets: Material 3 IconButton / Switch / FilterChip already
 * enforce a 48dp minimum touch target; the launcher's own rows
 * (SettingsControls.kt) add an explicit 48dp minimum height. The
 * two known exceptions are recorded in BUILD_LOG.md (alphabet edge
 * index letters, widget resize handle — enlarged hit area).
 */

/** Minimum touch-target size for interactive elements (#175). */
const val MIN_TOUCH_TARGET_DP = 48

// ---- TalkBack helpers (rule 69) ----

/**
 * An icon-only control announces its ACTION ("Settings"), never what
 * it looks like ("gear icon").
 */
fun Modifier.actionDescription(action: String): Modifier =
    semantics { contentDescription = action }

/**
 * State via `stateDescription` (selected / checked / expanded),
 * never colour alone.
 */
fun Modifier.stateText(state: String): Modifier =
    semantics { stateDescription = state }

/**
 * Decorative elements (wallpaper art, decorative dividers,
 * page-indicator decoration) are hidden from the accessibility tree.
 */
fun Modifier.decorative(): Modifier = clearAndSetSemantics { }

/**
 * App-icon announcements: "Chrome, page 1, position 3". Page and
 * position are 1-based, as the user counts them.
 */
fun appIconDescription(appName: String, page: Int, position: Int): String =
    "$appName, page $page, position $position"

/**
 * Folder announcements: "Social, folder, 6 apps".
 */
fun folderDescription(folderName: String, appCount: Int): String =
    if (appCount == 1) "$folderName, folder, 1 app"
    else "$folderName, folder, $appCount apps"


/**
 * True when the system has animations turned off (animator duration
 * scale or transition animation scale is 0). Both scales are read;
 * either at 0 means the user wants no motion.
 */
fun isSystemReducedMotion(context: Context): Boolean {
    val resolver = context.contentResolver
    val animatorScale = try {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    } catch (e: Exception) {
        1f
    }
    val transitionScale = try {
        Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    } catch (e: Exception) {
        1f
    }
    return animatorScale == 0f || transitionScale == 0f
}

/** App version for the #171 debug screen, from the PackageManager
 *  (no BuildConfig reference — there is no generated class under
 *  the kotlinc check). Returns (versionName, versionCode). */
fun appVersionInfo(context: Context): Pair<String, Long> {
    return try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        Pair(info.versionName ?: "unknown", code)
    } catch (e: Exception) {
        Pair("unknown", 0L)
    }
}
