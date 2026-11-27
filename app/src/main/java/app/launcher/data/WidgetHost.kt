package app.launcher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Third-party widgets: the AppWidgetHost / AppWidgetManager
 * wrapper. The host id (1024) is the launcher's own; host views are
 * created in the UI layer (Widgets.kt) from this host.
 *
 * Binding a provider needs the user's consent the first time: the
 * system bind flow (ACTION_APPWIDGET_BIND) is started from the UI
 * when [bindWidget] reports the id is not yet bound. Nothing here
 * invents a widget API — this is the public AppWidget framework.
 *
 * Sizing (rule 41) follows the platform table, not a guess:
 * n cells = 70 x n - 30 dp (1 cell = 40 dp, 2 = 110 dp,
 * 3 = 180 dp, 4 = 250 dp). [cellsForDp] inverts the table and
 * [dpForCells] applies it, so the host and the placement math
 * always agree on what a widget's minimum size means in cells.
 */
object WidgetHost {

    const val HOST_ID = 1024

    /** The platform widget sizing table (rule 41): n cells in dp. */
    fun dpForCells(cells: Int): Int = 70 * cells.coerceAtLeast(1) - 30

    /** The platform table inverted: how many whole cells a dp size needs. */
    fun cellsForDp(dp: Int): Int = ((dp + 30) / 70).coerceAtLeast(1)

    /**
     * Whole-cell span for a provider's minimum size. A provider
     * with no usable minimum (0 or negative) gets the 2 x 1
     * default the placement math has always used.
     */
    fun spanForMinSizeDp(minWidthDp: Int, minHeightDp: Int): Pair<Int, Int> {
        if (minWidthDp <= 0 || minHeightDp <= 0) return Pair(2, 1)
        return Pair(cellsForDp(minWidthDp), cellsForDp(minHeightDp))
    }

    fun host(context: Context): AppWidgetHost = AppWidgetHost(context, HOST_ID)

    fun manager(context: Context): AppWidgetManager =
        context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager

    /** All widget providers installed on the device, soonest to pick. */
    fun installedProviders(context: Context): List<AppWidgetProviderInfo> =
        manager(context).installedProviders

    /** Allocates a fresh host id for a widget the user just picked. */
    fun allocateId(context: Context): Int = host(context).allocateAppWidgetId()

    /** Releases a host id when its widget is removed. */
    fun deleteId(context: Context, appWidgetId: Int) {
        if (appWidgetId >= 0) host(context).deleteAppWidgetId(appWidgetId)
    }

    /**
     * Tries to bind [appWidgetId] to [provider]. Returns true when the
     * id is bound (either already, or by this call). False means the
     * system wants the user to confirm — the caller must start the
     * ACTION_APPWIDGET_BIND flow (see [bindIntent]).
     */
    fun bindWidget(context: Context, appWidgetId: Int, provider: ComponentName): Boolean {
        val mgr = manager(context)
        return try {
            if (mgr.getAppWidgetInfo(appWidgetId) != null) true
            else mgr.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        } catch (e: Exception) {
            false
        }
    }

    /** The user-confirmation intent for a bind the system declined. */
    fun bindIntent(context: Context, appWidgetId: Int, provider: ComponentName): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }

    /** Provider info for a bound id, or null when the provider is gone. */
    fun providerInfo(context: Context, appWidgetId: Int): AppWidgetProviderInfo? =
        try {
            manager(context).getAppWidgetInfo(appWidgetId)
        } catch (e: Exception) {
            null
        }

    /**
     * Whether this provider needs a configuration step before it
     * can be placed (rule 43). A provider with a configure activity
     * is never placed bare: the caller runs the configuration flow
     * first (see [configureIntent]) and only places the widget on
     * the configuration result, so configuration cannot fail to add.
     */
    fun hasConfigureActivity(info: AppWidgetProviderInfo?): Boolean =
        info?.configure != null

    /**
     * The intent that opens a provider's configuration activity for
     * an already-bound id. The caller launches it for a result:
     * RESULT_OK means the widget is configured and may be placed
     * (or, for a placed widget, reconfigured in place); anything
     * else means the id is freed and nothing is placed.
     */
    fun configureIntent(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): Intent? {
        val configure = info.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    /**
     * Tells the provider the widget's size in dp, from its grid span
     * in half-cells (rule 41). The dp values come from the platform
     * table ([dpForCells]), never from a per-cell guess.
     */
    fun updateWidgetSize(context: Context, appWidgetId: Int, spanXHalfCells: Int, spanYHalfCells: Int) {
        val cellsX = (spanXHalfCells / 2).coerceAtLeast(1)
        val cellsY = (spanYHalfCells / 2).coerceAtLeast(1)
        val widthDp = dpForCells(cellsX)
        val heightDp = dpForCells(cellsY)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        try {
            manager(context).updateAppWidgetOptions(appWidgetId, options)
        } catch (e: Exception) {
            // Provider gone or id invalid; the host view shows the
            // "unavailable" placeholder instead.
        }
    }
}
