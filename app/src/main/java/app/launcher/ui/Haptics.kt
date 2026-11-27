package app.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptics — the rule 59 map, implemented with predefined
 * [HapticFeedbackType] constants only. Never raw
 * `Vibrator.vibrate(long)` (grep for it: there must be none).
 *
 * Vocabulary limit (recorded honestly): this project builds on
 * Compose UI 1.7.6 (BOM 2025.01.00), whose HapticFeedbackType
 * exposes only TWO constants — LongPress (the click class) and
 * TextHandleMove (the tick class). The richer vocabulary the
 * research names (Confirm, SegmentTick, ToggleOn/Off,
 * GestureThresholdActivate) arrives in Compose UI 1.8.0. The map
 * below therefore expresses the click/tick distinction with the
 * two constants that exist, and every event still fires exactly
 * one crisp impulse at the event moment. When the BOM moves to a
 * Compose UI with the full set, the mapping — and only the
 * mapping — should be widened to the named constants; the event
 * list does not change.
 *
 * The map (DESIGN-RESEARCH.md §4.5, rule 59) — one crisp impulse
 * each, at the event moment, on the same frame as the visual:
 *  - long-press pickup → LongPress (click)
 *  - drag snap (neighbours slide aside on reorder) → TextHandleMove (tick)
 *  - drop → LongPress (click)
 *  - page snap → TextHandleMove (tick)
 *  - folder/drawer threshold (folder hover-open at 800 ms) →
 *    TextHandleMove (tick)
 *  - toggle flip → LongPress on / TextHandleMove off
 *  - successful confirm → LongPress (click)
 *
 * Never fires: icon tap, page swipe, scrolling, typing in search,
 * opening an app, errors (visual only). Repeated haptics during a
 * drag are discrete quiet ticks with silence between them — never
 * one long buzz. Keyclick scale is 10–20 ms (the predefined
 * CLICK/TICK class the platform supplies).
 *
 * Gating: the whole map sits behind `settings.hapticsEnabled`
 * (LauncherSettings, OFF by default — an extra, like every other
 * non-stock toggle). Marlow never overrides the system haptics
 * setting and never forces haptics on. Reduced motion does NOT
 * disable haptics: motion collapses, feedback stays (rule 60
 * keeps essential feedback; the task states it plainly).
 *
 * On Android 16+, any custom haptic would be an envelope (PWLE);
 * Marlow defines no custom haptic, so there is nothing to port —
 * every effect here is a predefined constant with the platform's
 * own fallback.
 *
 * Review is by hand on the Pixel 10a, naming each effect
 * click / tick / low tick — see rebuild-notes-motion.md.
 */
class MarlowHaptics(
    private val feedback: HapticFeedback?,
    private val enabled: Boolean,
) {
    /** Long-press pickup — fires AT the long-press moment, not on
     *  lift; drag starts only after this haptic (rule 56). */
    fun longPressPickup() = fire(HapticFeedbackType.LongPress)

    /** Drag snap — neighbours slide aside after the 650 ms reorder
     *  hover (rule 56). */
    fun dragSnap() = fire(HapticFeedbackType.TextHandleMove)

    /** Drop — the dragged item lands (move, folder create/add,
     *  remove/uninstall zone). */
    fun drop() = fire(HapticFeedbackType.LongPress)

    /** Page snap — the home pager settles on a page. */
    fun pageSnap() = fire(HapticFeedbackType.TextHandleMove)

    /** Folder/drawer threshold — the 800 ms drag hover over a
     *  closed folder opens it (rule 40). */
    fun threshold() = fire(HapticFeedbackType.TextHandleMove)

    /** Toggle flip — any settings switch changes state. */
    fun toggleFlip(checked: Boolean) =
        fire(if (checked) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)

    /** Successful confirm — e.g. a folder rename is confirmed. */
    fun confirm() = fire(HapticFeedbackType.LongPress)

    private fun fire(type: HapticFeedbackType) {
        if (!enabled) return
        feedback?.performHapticFeedback(type)
    }

    companion object {
        /** No-op instance: used as the CompositionLocal default, so
         *  any surface composed outside HomeScreen simply feels
         *  nothing instead of crashing. */
        val Disabled = MarlowHaptics(null, false)
    }
}

/**
 * The haptics instance for this composition, provided at the
 * HomeScreen root (see HomeScreen) and read anywhere below it —
 * HomeGrid, FolderDialog, SwitchRow — without threading a
 * parameter through every call site.
 */
val LocalMarlowHaptics = staticCompositionLocalOf { MarlowHaptics.Disabled }

/** Remembers the gated instance for the current haptics setting. */
@Composable
fun rememberMarlowHaptics(enabled: Boolean): MarlowHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { MarlowHaptics(feedback, enabled) }
}
