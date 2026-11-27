package app.launcher.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.launcher.data.IconUtils
import app.launcher.data.WallpaperHelper
import app.launcher.model.AppEntry
import app.launcher.model.IconShape
import app.launcher.model.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** #101 live clock icon: real hands, updated every second.
 *  Same ticker shape as ClockWidgetContent (see its comment): the
 *  loop runs on Dispatchers.Default so Robolectric's paused main
 *  looper never fast-forwards it during the startup gate. */
@Composable
fun ClockIcon(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
            }
        }
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier) {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val hour = calendar.get(Calendar.HOUR) % 12
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        drawCircle(color = surface, radius = radius, center = centre)
        drawCircle(color = onSurface, radius = radius, center = centre, style = Stroke(width = 2f))
        fun hand(angleDegrees: Double, length: Float, color: Color, width: Float) {
            val angle = (angleDegrees - 90.0) * PI / 180.0
            drawLine(
                color = color,
                start = centre,
                end = Offset(
                    centre.x + (cos(angle) * length).toFloat(),
                    centre.y + (sin(angle) * length).toFloat(),
                ),
                strokeWidth = width,
            )
        }
        hand((hour + minute / 60.0) * 30.0, radius * 0.5f, onSurface, 5f)
        hand((minute + second / 60.0) * 6.0, radius * 0.75f, onSurface, 4f)
        hand(second * 6.0, radius * 0.85f, primary, 2f)
        drawCircle(color = primary, radius = 4f, center = centre)
    }
}

fun isClockApp(app: AppEntry): Boolean =
    "clock" in app.label.lowercase() || "clock" in app.packageName.lowercase()

/**
 * Rule 46: icon corner radius is PROPORTIONAL to icon size (a
 * quarter of the icon), not a fixed 28dp — so the apparent shape is
 * identical on every surface (home 56dp, folder minis, overview,
 * drawer). The old fixed `RoundedCornerShape(28)` was nearly a
 * circle on a 40dp icon and a squircle on a 72dp one.
 */
fun shapeFor(shape: IconShape, iconSize: Dp): Shape = when (shape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED -> RoundedCornerShape(iconSize * 0.25f)
    IconShape.SQUARE -> RectangleShape
}

/** Back-compat overload (56dp default grid icon) for callers that
 *  do not carry a size. */
fun shapeFor(shape: IconShape): Shape = shapeFor(shape, 56.dp)

/**
 * Rule 13/14: label text over wallpaper. The colour follows the
 * wallpaper's luminance, not the theme: dark text over bright art,
 * white text over dark art, theme text when the luminance cannot be
 * read (the ~8% veil fallback in HomeScreen covers that case).
 * The luminance-aware scrim behind the labels is drawn by
 * HomeScreen; this only picks the readable side of it.
 */
fun labelColorForWallpaper(luminance: Float?): Color =
    when {
        luminance == null -> Color.Unspecified
        luminance > 0.5f -> Color.Black
        else -> Color.White
    }

/**
 * One app icon + label (#19 pack, #20 themed, #22 shape/size/labels,
 * #98 rename via AppEntry.displayLabel, #100 Smartsize, #101 live clock).
 * `resolveDrawable` supplies the drawable after pack/override lookup.
 *
 * Rule 47: the drawable resolves synchronously (cache-backed, cheap)
 * but the bitmap decode runs off composition on Dispatchers.Default;
 * while it loads the cell is reserved with a surface placeholder —
 * the grid never reflows and neighbours never shift.
 *
 * Rule 48: themed icons use the MONOCHROME layer only, tinted from
 * the wallpaper palette. Apps without a monochrome layer keep their
 * normal recognisable icon (mixed grid below Android 16 QPR2; on
 * QPR2+ the same monochrome layer is what the system auto-themes
 * from). The old `ColorFilter.tint(primary, SrcIn)` over the
 * FULL-COLOUR icon is deleted — it is exactly the forced tint the
 * rule forbids.
 *
 * Rules 49/50: the badge is a dot only (see Gestures.kt) — the
 * count is never drawn here; it lives in the long-press menu.
 */
@Composable
fun AppIconImage(
    app: AppEntry,
    settings: LauncherSettings,
    resolveDrawable: (AppEntry) -> Drawable?,
    modifier: Modifier = Modifier,
    iconSize: Dp? = null,
    badgeCount: Int = 0,
    badgeBitmap: android.graphics.Bitmap? = null,
    overWallpaper: Boolean = false,
    wallpaperLuminance: Float? = null,
) {
    val density = LocalDensity.current
    val iconSize = iconSize ?: settings.iconSizeDp.dp
    val sizePx = with(density) { iconSize.roundToPx() }
    val context = LocalContext.current
    // Rule 48 tint source: the wallpaper palette (Ambient accent),
    // falling back to the theme primary when the wallpaper yields
    // no usable colour.
    val wallpaperTint = remember(context, settings.themedIcons) {
        if (settings.themedIcons) WallpaperHelper.ambientAccent(context) else null
    }
    val tintColor = wallpaperTint ?: MaterialTheme.colorScheme.primary
    val drawable = resolveDrawable(app)
    val monochrome = if (settings.themedIcons) {
        remember(drawable) { drawable?.let { IconUtils.monochromeDrawable(it) } }
    } else {
        null
    }
    // Rule 47: decode off the composition thread. The state starts
    // null (placeholder) and fills in without moving neighbours.
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var bitmapIsThemed by remember { mutableStateOf(false) }
    LaunchedEffect(drawable, monochrome, sizePx, settings.smartSizeIcons) {
        bitmap = null
        val decoded = withContext(Dispatchers.Default) {
            val source = monochrome ?: drawable
            source?.let {
                IconUtils.drawableToImageBitmap(it, sizePx, settings.smartSizeIcons) to (monochrome != null)
            }
        }
        bitmap = decoded?.first
        bitmapIsThemed = decoded?.second ?: false
    }
    // Outer Box is unclipped so the badge (a sibling of the clipped
    // icon below) is never cut off at the corner.
    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(shapeFor(settings.iconShape, iconSize))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (settings.liveClockIcon && isClockApp(app)) {
                ClockIcon(modifier = Modifier.size(iconSize))
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = app.displayLabel,
                    modifier = Modifier.size(iconSize),
                    // Rule 48: the tint applies ONLY to the
                    // monochrome layer. A full-colour icon is never
                    // tinted — apps without a monochrome layer keep
                    // their normal icon.
                    colorFilter = if (bitmapIsThemed) {
                        ColorFilter.tint(tintColor, BlendMode.SrcIn)
                    } else {
                        null
                    },
                )
            } else {
                // Rule 47 placeholder: the reserved cell, in the
                // surface colour, until the icon decodes. No empty
                // hole, no reflow.
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
        // #31 badges: a dot over the icon's corner (rules 49/50).
        // `badgeBitmap` (dynamic-badge artwork) is accepted for
        // signature compat and ignored — the dot is the only badge.
        NotificationBadge(count = badgeCount, largeIcon = null, settings = settings)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconTile(
    app: AppEntry,
    settings: LauncherSettings,
    resolveDrawable: (AppEntry) -> Drawable?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeBitmap: android.graphics.Bitmap? = null,
    overWallpaper: Boolean = false,
    wallpaperLuminance: Float? = null,
    // Rule 69: TalkBack custom actions for the drag-only actions.
    // No Move action: there is no programmatic drag entry point,
    // so it must not be faked as a no-op.
    onRemove: () -> Unit = {},
    onRename: () -> Unit = {},
    onUninstall: () -> Unit = {},
    // Decision F5: the dock passes false — labels are always off
    // there, whatever the global showLabels setting is.
    showLabel: Boolean = true,
) {
    // #175 TalkBack: the tile names the app on its clickable node
    // (label + double-tap action), instead of relying on the merged
    // child text alone.
    Column(
        modifier = modifier
            .semantics {
                contentDescription = app.displayLabel
                customActions = listOf(
                    CustomAccessibilityAction("Remove") {
                        onRemove()
                        true
                    },
                    CustomAccessibilityAction("Rename") {
                        onRename()
                        true
                    },
                    CustomAccessibilityAction("Uninstall") {
                        onUninstall()
                        true
                    },
                )
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIconImage(
            app = app, settings = settings, resolveDrawable = resolveDrawable,
            badgeCount = badgeCount, badgeBitmap = badgeBitmap,
            overWallpaper = overWallpaper, wallpaperLuminance = wallpaperLuminance,
        )
        if (showLabel && settings.showLabels) {
            // Rules 10/12: ONE label role everywhere — body-small
            // (12sp), one line + ellipsis. The cell height comes
            // from the grid span, so hiding labels does not change
            // it; at 200% font scale the label grows inside the
            // cell's slack (a 5×6 cell is ~116dp tall against
            // ~92dp of icon + 200% label) and truncates, never
            // overlapping the icon below — device check still owed.
            val labelColor = if (overWallpaper) {
                val wallColor = labelColorForWallpaper(wallpaperLuminance)
                if (wallColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else wallColor
            } else {
                MaterialTheme.colorScheme.onBackground
            }
            Text(
                text = app.displayLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = labelColor,
            )
        }
    }
}
