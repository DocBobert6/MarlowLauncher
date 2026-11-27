package app.launcher.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import app.launcher.data.isSystemReducedMotion

/**
 * Motion — the single source of truth for every duration, easing
 * curve, spring, and transition pattern in Marlow (DESIGN-RESEARCH.md
 * §4.5, rules 53–56, 58, 60).
 *
 * The split (rule 55): springs for components — drags, flings, sheet
 * settling, predictive-back cancel; emphasized easing for screen
 * transitions. Never mix the two jobs. Only transform and opacity
 * are animated — never width/height/top/left/margin/padding, never
 * font size.
 *
 * No duration in this file is invented: every value comes from the
 * §4.5 table. Where the table gives a range (stagger 30–60 ms) the
 * midpoint is used and named.
 *
 * Reduced motion (rule 60): every spec function takes
 * [reducedMotion]. When it is true, transform motion collapses to
 * instant (snap) and fades collapse to a near-instant 100 ms
 * crossfade — no springs, no 400 ms easings, no parallax. Haptics
 * are NOT collapsed with motion (see Haptics.kt): reduced motion
 * keeps haptic feedback.
 *
 * Hero moments (rule 55 — at most two in the whole launcher):
 *  1. App launch — the system icon-anchored window growth (rule 57).
 *     Marlow contributes nothing layered on top of it; the system
 *     owns the animation and Marlow must not duplicate it.
 *  2. Folder open — the container transform: preview icons scale up
 *     from the folder icon, the background expands, items fade in
 *     with a 45 ms stagger (folderDropIn / folderContents specs
 *     below). No other screen has a hero animation.
 */
object Motion {

    // ---- Easing curves (§4.5, rule 53) ----
    /** Enter: Emphasized decelerate cubic-bezier(0.05, 0.7, 0.1, 1.0). */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    /** Exit: Emphasized accelerate cubic-bezier(0.3, 0, 0.8, 0.15). */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** Begin-and-end on screen: Emphasized cubic-bezier(0.2, 0, 0, 1.0). */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1.0f)
    /** Standard decelerate (small utility enter). */
    val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1.0f)
    /** Standard accelerate (small utility exit). */
    val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    // ---- Durations, ms (§4.5) ----
    const val ENTER_MS = 400
    const val EXIT_MS = 200
    const val BEGIN_END_MS = 500
    const val UTILITY_MS = 300
    const val UTILITY_ENTER_MS = 250
    const val UTILITY_EXIT_MS = 200
    /** Single-step ceiling (rule 53): no transition exceeds 500 ms. */
    const val SINGLE_STEP_CEILING_MS = 500
    /** Micro-interaction ceiling (rule 53): press ripple, toggle,
     *  icon-state change — none exceeds 200 ms. */
    const val MICRO_CEILING_MS = 200
    /** Container first, contents 30–60 ms after (rule 54) — midpoint. */
    const val STAGGER_MS = 45
    /** Folder drop-in / initial-item / final-item (T5 §4.2). */
    const val FOLDER_DROP_IN_MS = 400
    const val FOLDER_INITIAL_ITEM_MS = 350
    const val FOLDER_FINAL_ITEM_MS = 200
    /** Widget resize snap (rule 42): 66% of a cell, 150 ms snap. */
    const val WIDGET_RESIZE_SNAP_MS = 150
    /** Empty-page snap-off / fade (rule 29). */
    const val EMPTY_PAGE_SNAP_OFF_MS = 400
    const val EMPTY_PAGE_FADE_MS = 150
    /** Adjacent-page drop (T5 §2.4) — also the programmatic
     *  page-move duration (scrollHomeBy). */
    const val PAGE_MOVE_MS = 300
    /** Drawer search transition (T5 §6.1). */
    const val DRAWER_SEARCH_MS = 300
    /** Reorder hover before neighbours slide aside (rule 56 —
     *  650 ms current AOSP; 350 ms is the older value). */
    const val REORDER_HOVER_MS = 650L
    /** Folder spring-load open on drag hover (rule 40). */
    const val FOLDER_HOVER_OPEN_MS = 800L
    /** Reduced-motion fade: opacity-only changes are kept (rule 60),
     *  collapsed to a near-instant crossfade. */
    const val REDUCED_FADE_MS = 100

    // ---- Predictive back (rule 58) ----
    /** A back swipe previews the destination with the surface scaled
     *  to ~90%; cancelling springs back to 100% with no state change. */
    const val PREDICTIVE_BACK_SCALE = 0.90f

    // ---- Screen-transition specs (rule 53) ----
    /** Enter: Emphasized decelerate 400 ms. */
    fun enterSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(ENTER_MS, easing = EmphasizedDecelerate)

    /** Exit: Emphasized accelerate 200 ms. */
    fun exitSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(EXIT_MS, easing = EmphasizedAccelerate)

    /** Begin-and-end on screen: Emphasized 500 ms. */
    fun beginEndSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(BEGIN_END_MS, easing = Emphasized)

    /** Small utility transition: Standard 300 ms. */
    fun utilitySpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(UTILITY_MS, easing = Emphasized)

    /** Small utility enter: 250 ms decelerate. */
    fun utilityEnterSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(UTILITY_ENTER_MS, easing = StandardDecelerate)

    /** Small utility exit: 200 ms accelerate. */
    fun utilityExitSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(UTILITY_EXIT_MS, easing = StandardAccelerate)

    /** Fade-through for unrelated content swaps (rule 54): outgoing
     *  fades out, incoming fades in while scaling 92% → 100%.
     *  Under reduced motion this is a near-instant crossfade. */
    fun fadeThroughSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) tween(REDUCED_FADE_MS, easing = LinearEasing)
        else tween(BEGIN_END_MS, easing = Emphasized)

    /** Fade for in-bounds appear/disappear (rule 54): dialogs, menus,
     *  tooltips, badges — entering scales 80% → 100%. The scale half
     *  lives in the call site (fadeSpec covers the alpha). */
    fun fadeSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) tween(REDUCED_FADE_MS, easing = LinearEasing)
        else tween(UTILITY_MS, easing = EmphasizedDecelerate)

    // ---- Springs (rule 55) ----
    /** Drag lift: the dragged tile scales up on a spring (rule 56).
     *  Keeps the stiffness the grid already used
     *  (StiffnessMediumLow); under reduced motion the lift is
     *  instant — the drag itself is finger-driven, so there is no
     *  spring to settle. */
    fun dragLiftSpring(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap()
        else spring(stiffness = Spring.StiffnessMediumLow)

    /** Predictive-back cancel: a cancelled back swipe springs back
     *  to 100% with no state change (rule 58). A plain spring —
     *  no bounce, so the surface settles exactly where it was. */
    fun predictiveBackCancelSpring(): FiniteAnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /** Widget resize snap: 150 ms to the new span once the handle
     *  crosses 66% of a cell (rule 42). */
    fun widgetResizeSnapSpec(reducedMotion: Boolean): FiniteAnimationSpec<IntSize> =
        if (reducedMotion) snap()
        else tween(WIDGET_RESIZE_SNAP_MS, easing = EmphasizedDecelerate)

    /** Programmatic page move (scrollHomeBy / adjacent-page drop):
     *  300 ms Emphasized decelerate. Under reduced motion the call
     *  site uses scrollToPage instead — instant, no animation. */
    fun pageMoveSpec(): FiniteAnimationSpec<Float> =
        tween(PAGE_MOVE_MS, easing = EmphasizedDecelerate)

    // ---- Folder (rules 39, 54; T5 §4.2) ----
    /** Folder container drop-in: 400 ms Emphasized decelerate. */
    fun folderDropInSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(FOLDER_DROP_IN_MS, easing = EmphasizedDecelerate)

    /** Folder contents: 350 ms, starting STAGGER_MS after the
     *  container (rule 54 choreography). */
    fun folderContentsSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) tween(REDUCED_FADE_MS, easing = LinearEasing)
        else tween(FOLDER_INITIAL_ITEM_MS, delayMillis = STAGGER_MS, easing = EmphasizedDecelerate)

    /** Folder exit (container + contents together): 200 ms
     *  Emphasized accelerate. */
    fun folderExitSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(FOLDER_FINAL_ITEM_MS, easing = EmphasizedAccelerate)

    // ---- Drawer (rules 31, 54; shared-axis z) ----
    /** Drawer enter: shared-axis z — scale 80% → 100% + fade,
     *  400 ms Emphasized decelerate. Under reduced motion a
     *  near-instant crossfade. */
    fun drawerEnter(reducedMotion: Boolean): EnterTransition =
        if (reducedMotion) {
            fadeIn(tween(REDUCED_FADE_MS, easing = LinearEasing))
        } else {
            fadeIn(tween(ENTER_MS, easing = EmphasizedDecelerate)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(ENTER_MS, easing = EmphasizedDecelerate),
                )
        }

    /** Drawer exit: shared-axis z — scale 100% → 80% + fade,
     *  200 ms Emphasized accelerate. Under reduced motion a
     *  near-instant crossfade. */
    fun drawerExit(reducedMotion: Boolean): ExitTransition =
        if (reducedMotion) {
            fadeOut(tween(REDUCED_FADE_MS, easing = LinearEasing))
        } else {
            fadeOut(tween(EXIT_MS, easing = EmphasizedAccelerate)) +
                scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(EXIT_MS, easing = EmphasizedAccelerate),
                )
        }

    /** Drawer search field appear/disappear: 300 ms (T5 §6.1). */
    fun drawerSearchEnter(reducedMotion: Boolean): EnterTransition =
        if (reducedMotion) {
            fadeIn(tween(REDUCED_FADE_MS, easing = LinearEasing))
        } else {
            fadeIn(tween(DRAWER_SEARCH_MS, easing = EmphasizedDecelerate)) +
                expandVertically(
                    animationSpec = tween(DRAWER_SEARCH_MS, easing = EmphasizedDecelerate),
                    expandFrom = Alignment.Top,
                )
        }

    fun drawerSearchExit(reducedMotion: Boolean): ExitTransition =
        if (reducedMotion) {
            fadeOut(tween(REDUCED_FADE_MS, easing = LinearEasing))
        } else {
            fadeOut(tween(DRAWER_SEARCH_MS, easing = EmphasizedAccelerate)) +
                shrinkVertically(
                    animationSpec = tween(DRAWER_SEARCH_MS, easing = EmphasizedAccelerate),
                    shrinkTowards = Alignment.Top,
                )
        }

    // ---- Empty page (rule 29) ----
    /** Empty-page snap-off 400 ms / fade 150 ms. Specified here from
     *  the table; NOT wired — page removal is a pager pageCount
     *  change and the pager exposes no removal-animation hook, so
     *  the page vanishes with the pager's own settle. Recorded in
     *  rebuild-notes-motion.md. */
    fun emptyPageSnapOffSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(EMPTY_PAGE_SNAP_OFF_MS, easing = EmphasizedDecelerate)

    fun emptyPageFadeSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reducedMotion) tween(REDUCED_FADE_MS, easing = LinearEasing)
        else tween(EMPTY_PAGE_FADE_MS, easing = EmphasizedAccelerate)
}

/**
 * Reads the system reduced-motion setting (rule 60): the animator
 * duration scale or the transition animation scale at 0 means the
 * user wants no motion (see data/Accessibility.kt). There is no
 * launcher-level toggle — the system setting is the source of
 * truth. Remembered per composition; the value is re-read on each
 * recomposition that re-runs this call.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { isSystemReducedMotion(context) }
}
