package app.launcher.data

import android.content.res.Configuration
import app.launcher.model.LauncherSettings
import app.launcher.model.LayoutVariant

/**
 * #150 / #151 / #152 layout variants, from the window's smallest
 * width + orientation — no fold-state library is a declared
 * dependency, so fold state is inferred from size class, which is
 * the same signal the two-pane decision needs.
 *
 * Thresholds (rules 68, 70): a second pane appears only at the
 * EXPANDED breakpoint — smallest width >= 840dp. Below that is a
 * phone (compact <600dp, medium 600–839dp). The Pixel 10a's smallest
 * width is about 412dp, so every foldable / tablet variant is dormant
 * there — present in code, never triggered on that device.
 *
 * Orientation comes from the OCCUPIED AREA (the window's width vs
 * height), not the device: `configuration.orientation` is device
 * orientation, and the original Pixel Fold opens in landscape
 * (rotation_0 = landscape). [orientationForArea] derives it from the
 * window size; [variantForWindow] is the call to use.
 */
object AdaptiveLayout {

    /** The expanded breakpoint: smallest width >= 840dp is a tablet /
     *  unfolded inner screen (two-pane); below that is a phone. */
    fun isWideDevice(smallestScreenWidthDp: Int): Boolean =
        smallestScreenWidthDp >= 840

    /** Orientation from the occupied area: the window is landscape
     *  when it is wider than it is tall. */
    fun orientationForArea(windowWidthDp: Int, windowHeightDp: Int): Int =
        if (windowWidthDp > windowHeightDp) Configuration.ORIENTATION_LANDSCAPE
        else Configuration.ORIENTATION_PORTRAIT

    /** The effective two-pane switch: both toggles on AND an expanded
     *  device. A stored ON on a phone install is off here, whatever
     *  the stored value says — the same gate LauncherStore applies
     *  at load time, so callers that read the raw flags stay safe. */
    fun twoPaneEnabled(settings: LauncherSettings, smallestScreenWidthDp: Int): Boolean =
        settings.twoPaneTablet && settings.separateScreenLayouts &&
            isWideDevice(smallestScreenWidthDp)

    /** Variant from the window: smallest width + the occupied area's
     *  orientation. Prefer this over [variantFor]. */
    fun variantForWindow(
        smallestScreenWidthDp: Int,
        windowWidthDp: Int,
        windowHeightDp: Int,
    ): LayoutVariant =
        variantFor(smallestScreenWidthDp, orientationForArea(windowWidthDp, windowHeightDp))

    /**
     * Variant from smallest width + orientation. The [orientation]
     * argument MUST be the window orientation (see [orientationForArea]),
     * not `configuration.orientation` — that is device orientation.
     * Kept for the existing HomeScreen call; new callers use
     * [variantForWindow].
     */
    fun variantFor(smallestScreenWidthDp: Int, orientation: Int): LayoutVariant {
        if (isWideDevice(smallestScreenWidthDp)) {
            return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LayoutVariant.FOLDABLE_INNER
            } else {
                LayoutVariant.TABLET_TWO_PANE
            }
        }
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LayoutVariant.PHONE_LANDSCAPE
        } else {
            LayoutVariant.PHONE_PORTRAIT
        }
    }

    /** Two-pane layouts (#152) apply to the wide variants only. */
    fun isTwoPane(variant: LayoutVariant): Boolean =
        variant == LayoutVariant.TABLET_TWO_PANE || variant == LayoutVariant.FOLDABLE_INNER

    /** #150 separate layouts per screen: the cover layout is the
     *  phone layout; the inner layout is the two-pane layout. */
    fun isLandscape(variant: LayoutVariant): Boolean =
        variant == LayoutVariant.PHONE_LANDSCAPE || variant == LayoutVariant.FOLDABLE_INNER
}
