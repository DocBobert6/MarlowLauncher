package app.launcher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Icon drawing helpers: #100 Smartsize normalisation (every icon drawn
 * into the same square with the same padding, so packs and system icons
 * sit uniformly) and #20 / rule 48 themed-icon preparation.
 *
 * Rule 47: legacy icons are normalised onto a tray at a FIXED scale —
 * the icon is drawn at 0.72 of the cell (14% inset each side), the
 * One UI / AOSP tray scale, so a legacy icon never renders smaller
 * than its adaptive neighbours.
 *
 * Rule 48: themed icons use the MONOCHROME layer only, tinted from
 * the wallpaper palette — never by tinting the full-colour icon.
 * [monochromeDrawable] returns an app's monochrome layer when it has
 * one (API 33+ `AdaptiveIconDrawable.monochrome`), null otherwise;
 * apps without one keep their normal recognisable icon (the mixed
 * grid below Android 16 QPR2, per the coordinator's decision).
 */
object IconUtils {

    /** #100: centre the icon in a square canvas at 0.72 scale (14%
     *  inset each side), so differently-shaped source icons
     *  normalise to one apparent size. */
    fun normalize(drawable: Drawable, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val inset = (sizePx * 0.14f).toInt()
        val inner = sizePx - inset * 2
        val bitmap = drawable.toBitmap(inner, inner)
        canvas.drawBitmap(bitmap, inset.toFloat(), inset.toFloat(), null)
        return out
    }

    fun drawableToImageBitmap(drawable: Drawable, sizePx: Int, smartSize: Boolean): ImageBitmap {
        return if (smartSize) {
            normalize(drawable, sizePx).asImageBitmap()
        } else {
            drawable.toBitmap(sizePx, sizePx).asImageBitmap()
        }
    }

    /** Rule 48: the app's monochrome (themed-icon) layer, or null
     *  when the app has none. Only `AdaptiveIconDrawable` carries
     *  one, and only on API 33+ — anything else keeps its normal
     *  icon (mixed grid, never a forced tint). */
    fun monochromeDrawable(drawable: Drawable): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        if (drawable !is AdaptiveIconDrawable) return null
        return try {
            drawable.monochrome
        } catch (e: Exception) {
            null
        }
    }
}
