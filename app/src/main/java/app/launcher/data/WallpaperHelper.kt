package app.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.IBinder

/**
 * Wallpaper (#53 note, #77): local-only. The picker is the system
 * picker (ACTION_GET_CONTENT, image mime type) — photos / wallpapers / other
 * apps already on the phone. There are NO built-in sources, no daily
 * rotation (#52 denied), no wallpaper backup (#54 denied).
 *
 * Setting goes through WallpaperManager, so the wallpaper is the
 * system wallpaper and survives the launcher. Scrolling (#77) moves
 * it with the home page via setWallpaperOffsets.
 */
object WallpaperHelper {

    /** Applies a picked image as the system wallpaper. */
    fun applyWallpaper(context: Context, uri: Uri): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                wallpaperManager.setStream(stream)
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /** #77 scrolling wallpaper: xOffset 0..1 across the home pages. */
    fun setPageOffset(context: Context, windowToken: IBinder?, pageIndex: Int, pageCount: Int) {
        if (windowToken == null || pageCount <= 1) return
        val offset = pageIndex.toFloat() / (pageCount - 1).toFloat()
        try {
            WallpaperManager.getInstance(context).setWallpaperOffsets(windowToken, offset, 0f)
        } catch (e: Exception) {
            // setWallpaperOffsets is best-effort on some devices.
        }
    }

    /** #50 Ambient: the system wallpaper's primary colour, if the
     *  system reports one. Null when there is no wallpaper colour to
     *  read (live wallpapers, older devices, any failure) — the
     *  theme falls back to the chosen accent. Re-derived when the
     *  theme is (re)selected or the wallpaper is applied, not live. */
    fun ambientAccent(context: Context): androidx.compose.ui.graphics.Color? {
        return try {
            val colors = WallpaperManager.getInstance(context)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
            val primary = colors.primaryColor ?: return null
            androidx.compose.ui.graphics.Color(primary.toArgb())
        } catch (e: Exception) {
            null
        }
    }
}
