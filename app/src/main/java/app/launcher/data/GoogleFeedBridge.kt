package app.launcher.data

import android.content.Context
import android.content.pm.PackageManager

/**
 * Google feed bridge (#38, audit F6 — KEPT as planned, fragility
 * noted).
 *
 * There is NO public Google feed API for a third-party launcher: the
 * Pixel launcher's Discover feed is served through unofficial
 * Google-app internals (the Google app's feed service), which Google
 * can change or withdraw at any time. This bridge therefore does NOT
 * pretend to fetch Google feed content. It only reports whether the
 * bridge could even be attempted, and the feed page degrades
 * gracefully to RSS-only whenever it is unavailable — which is the
 * honest state until a real integration is written against the
 * Google app's actual (unofficial) internals on a device that has
 * them.
 *
 * Recorded in BUILD_LOG.md as an open gap: no Google feed items are
 * shown, and none are invented.
 */
object GoogleFeedBridge {

    /** The Google app package whose internals a bridge would talk to. */
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    enum class BridgeStatus(val label: String) {
        UNAVAILABLE_NO_GOOGLE_APP("Google app not installed — bridge unavailable"),
        UNAVAILABLE_NO_INTEGRATION("Bridge not implemented — no public Google feed API exists"),
        ENABLED_BUT_DEGRADED("Bridge enabled, but no Google feed integration — showing RSS only"),
    }

    /** Whether the Google app is installed at all. */
    fun googleAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GOOGLE_APP_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * The bridge's status for the current device and toggle. There is
     * no state in which this returns Google feed items: the
     * integration itself does not exist yet (see the class doc).
     */
    fun status(context: Context, bridgeEnabled: Boolean): BridgeStatus {
        if (!googleAppInstalled(context)) return BridgeStatus.UNAVAILABLE_NO_GOOGLE_APP
        return if (bridgeEnabled) {
            BridgeStatus.ENABLED_BUT_DEGRADED
        } else {
            BridgeStatus.UNAVAILABLE_NO_INTEGRATION
        }
    }
}
