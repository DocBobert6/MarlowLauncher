package app.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import app.launcher.model.AccentColour
import app.launcher.model.FontChoice
import app.launcher.model.LauncherThemeChoice
import app.launcher.model.SurfaceStyle
import app.launcher.model.ThemeMode

/**
 * Marlow theme (DESIGN-RESEARCH rules 1-9, 15, 17; rebuilt Phase 3).
 *
 * Roles, not values (rule 1): no composable outside this file carries a
 * raw hex. Every colour in the UI is a Material 3 role that resolves
 * differently in light and dark, static and dynamic schemes.
 *
 * Stock scheme (rules 4, 5): generated from a SINGLE seed per accent
 * through the HCT pipeline (Material Color Utilities, tonal-spot
 * variant -- the Android Material You variant), light + dark exported
 * -- see the GENERATED region below and tools/generate_stock_scheme.py.
 * Changing a seed regenerates everything; no role is hand-tuned
 * afterwards. The accent picker is a SEED choice: each AccentColour
 * is a seed that regenerates the whole scheme, never a colour painted
 * onto one role. The default seed is P22 phosphor blue (#3333CC light
 * / #5555FF dark).
 *
 * Warm neutrals (locked Robert decision, DESIGN-RESEARCH section 4.1):
 * the warm light background #F8F1E7 and dark background #262019 are
 * the neutral palette's expression. The seed-generated neutrals are
 * replaced -- for the neutral family ONLY -- by palettes built from
 * each mode's locked background colour, so every surface role
 * (surface / surface-container-*) inherits the warmth consistently.
 * This is a project decision overriding the seed-generated neutrals,
 * applied in the generator script, not a hand-tweak in this file.
 * In dark mode the container tones are shifted +2 from the M3
 * baseline so the ladder stays monotonic above the pinned surface
 * (the locked dark background is tone ~12.7; the baseline container
 * tone 12 would sit below it). See the script header for the detail.
 *
 * Dynamic colour (rule 3): Material You is the default theme on
 * Android 12+ (API 31+) -- the wallpaper's extracted colour replaces
 * the seed and the whole scheme regenerates through the same
 * pipeline. The Stock scheme is the fallback below API 31 and when
 * the user picks Stock. The app swaps the SCHEME, never individual
 * colours (rule 5): the Material You branch below applies no accent
 * override of any kind.
 *
 * Depth is tonal, not shadow (rule 17): home = surface, dock =
 * surface-container, search bar / sheets / dialogs =
 * surface-container-high. The dock and the search pill are more than
 * one container step apart (rule 6). No shadow-based depth is defined
 * in this theme.
 *
 * Shape (rule 15): the theme Shapes are the M3 defaults
 * (extra-small 4dp, small 8dp, medium 12dp, large 16dp, extra-large
 * 28dp). Pills are `full` at the component level (M3 button/chip
 * default). There are NO shape overrides: any future override must
 * be a recorded, deliberate single-component exception.
 *
 * Type (rules 9, 10): all text uses MaterialTheme.typography roles.
 * The M3 type scale supplies the 12sp roles builders use for icon
 * labels -- labelMedium (12/16) and bodySmall (12/16) -- so no
 * custom sizes are defined here; the font choice applies one family
 * to every style.
 *
 * Modes: Auto / Light / Dark (one global mode -- per-part dark mode
 * #177 was removed). Themes at release: exactly two -- Material You
 * (the system's dynamic colour, Android 12+) and the curated stock
 * theme (warm paper / warm charcoal). Custom themes are way later;
 * there is no theme maker here.
 *
 * #50 the wallpaper-derived Ambient theme is launcher-local: when
 * the stock theme is active, the accent comes from the accent picker
 * (the curated set in AccentColour); it does NOT read the system
 * wallpaper colours -- that is Material You's job, and the two are
 * separate themes by decision.
 *
 * #24 AMOLED black: pure-black background in dark mode when on.
 * Only the background is overridden -- the generated dark scheme's
 * surfaces stand, so the warm dark surfaces still read above it.
 *
 * #51 surface styles: Opaque (solid surfaces) or our custom blur
 * glass. Blur glass here is translucent surfaces with a soft border
 * plus true backdrop blur -- MainActivity blurs the system wallpaper
 * behind the window (Window.setBackgroundBlurRadius, API 31+)
 * whenever blur glass is on and a wallpaper is visible; on older
 * devices, or where the blur cannot apply, the translucent read is
 * the fallback. No Liquid Glass, no Frosted as a separate style.
 *
 * Halo (rule 8): phosphorGlow is an additive effect on top of
 * role-coloured elements, never the sole carrier of state -- it stays
 * ONLY on the page-indicator dot and the widget resize handle.
 */

/** The colour a surface (dock, pill, glance strip) should use. */
fun surfaceColorFor(style: SurfaceStyle, scheme: ColorScheme, alpha: Float = 0.55f): Color =
    when (style) {
        SurfaceStyle.OPAQUE -> scheme.surfaceVariant
        SurfaceStyle.BLUR_GLASS -> scheme.surfaceVariant.copy(alpha = alpha)
    }

/**
 * Home backdrop gradient edge colours (rule 1: the hex lives here,
 * not in HomeScreen.kt). The audit-fix default backdrop is a subtle
 * warm vertical gradient behind the grid when no wallpaper is set:
 * deeper warm tone at top and bottom, the warm background in the
 * middle. Stock / Ambient only; Material You keeps its flat dynamic
 * background. Values are the audit-fix warm-family constants.
 */
val HomeBackdropEdgeLight: Color = Color(0xFFEFE4CF)
val HomeBackdropEdgeDark: Color = Color(0xFF1C1610)

/** Relative luminance (WCAG formula) -- `Color.luminance()` is not on
 *  the 1.7.6 classpath the local check compiles against, so it is
 *  computed here from the channels. */
fun relativeLuminance(color: Color): Float {
    fun channel(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * channel(color.red) +
        0.7152f * channel(color.green) +
        0.0722f * channel(color.blue)
}

/** Fits a wallpaper colour into an accent: dark enough to read on
 *  the warm paper in light mode, light enough to read on the warm
 *  charcoal in dark mode. Mid-tone colours pass through untouched.
 *  (Ambient theme only -- see the known-deviation note in
 *  LauncherTheme.) */
fun adjustAmbientAccent(color: Color, darkTheme: Boolean): Color {
    val luminance = relativeLuminance(color)
    return if (!darkTheme && luminance > 0.72f) {
        Color(color.red * 0.6f, color.green * 0.6f, color.blue * 0.6f, color.alpha)
    } else if (darkTheme && luminance < 0.28f) {
        Color(
            red = color.red + (1f - color.red) * 0.55f,
            green = color.green + (1f - color.green) * 0.55f,
            blue = color.blue + (1f - color.blue) * 0.55f,
            alpha = color.alpha,
        )
    } else {
        color
    }
}

private fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.SYSTEM -> FontFamily.Default
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONOSPACE -> FontFamily.Monospace
}

/** Applies one font family to every text style of a Typography
 *  (this Material 3 version has no defaultFontFamily parameter). */
private fun Typography.withFont(fontFamily: FontFamily): Typography {
    val apply = { style: TextStyle -> style.copy(fontFamily = fontFamily) }
    return copy(
        displayLarge = apply(displayLarge),
        displayMedium = apply(displayMedium),
        displaySmall = apply(displaySmall),
        headlineLarge = apply(headlineLarge),
        headlineMedium = apply(headlineMedium),
        headlineSmall = apply(headlineSmall),
        titleLarge = apply(titleLarge),
        titleMedium = apply(titleMedium),
        titleSmall = apply(titleSmall),
        bodyLarge = apply(bodyLarge),
        bodyMedium = apply(bodyMedium),
        bodySmall = apply(bodySmall),
        labelLarge = apply(labelLarge),
        labelMedium = apply(labelMedium),
        labelSmall = apply(labelSmall),
    )
}

/** Shape table (rule 15): the M3 defaults, stated explicitly so the
 *  table is a deliberate decision, not an accident of the default.
 *  No overrides -- see the file header. */
val MarlowShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// BEGIN GENERATED STOCK SCHEMES
// Generated by tools/generate_stock_scheme.py from one seed per
// accent through the HCT pipeline (Material Color Utilities,
// tonal-spot variant, standard contrast). Do NOT hand-edit:
// change the seed in the script and re-run it -- changing the
// seed regenerates everything. Warm neutrals replace the
// seed-generated neutrals (see the script header); every other
// role is the pipeline's output, unmodified.

// Stock light, Blue scheme -- seed #3333CC.
private val StockLightBlue: ColorScheme = lightColorScheme(
    primary = Color(0xFF42447B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8B8DC9),
    onPrimaryContainer = Color(0xFF0D0D45),
    inversePrimary = Color(0xFFC0C1FF),
    secondary = Color(0xFF47475C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9090A7),
    onSecondaryContainer = Color(0xFF141426),
    tertiary = Color(0xFF623E54),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB0859E),
    onTertiaryContainer = Color(0xFF280C1F),
    background = Color(0xFFF8F1E7),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFF8F1E7),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceTint = Color(0xFF575992),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    outline = Color(0xFF7D7667),
    outlineVariant = Color(0xFFCEC6B4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E2D4),
)

// Stock dark, Blue scheme -- seed #5555FF.
private val StockDarkBlue: ColorScheme = darkColorScheme(
    primary = Color(0xFFC1C1FF),
    onPrimary = Color(0xFF1D1D53),
    primaryContainer = Color(0xFF6667A1),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF4E4F87),
    secondary = Color(0xFFC6C4DD),
    onSecondary = Color(0xFF222335),
    secondaryContainer = Color(0xFF6B6B80),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFE9B9D3),
    onTertiary = Color(0xFF391A2E),
    tertiaryContainer = Color(0xFF886178),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF262019),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF262019),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceTint = Color(0xFFC1C1FF),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF18120B),
    surfaceBright = Color(0xFF3F3830),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF29231B),
    surfaceContainerHigh = Color(0xFF342D25),
    surfaceContainerHighest = Color(0xFF3F3830),
)

// Stock light, Indigo scheme -- seed #3F51B5.
private val StockLightIndigo: ColorScheme = lightColorScheme(
    primary = Color(0xFF3C457B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF858EC9),
    onPrimaryContainer = Color(0xFF040F46),
    inversePrimary = Color(0xFFBAC3FF),
    secondary = Color(0xFF45485C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF8E90A7),
    onSecondaryContainer = Color(0xFF121526),
    tertiary = Color(0xFF603F57),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFAE86A2),
    onTertiaryContainer = Color(0xFF270C22),
    background = Color(0xFFF8F1E7),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFF8F1E7),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceTint = Color(0xFF515B92),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    outline = Color(0xFF7D7667),
    outlineVariant = Color(0xFFCEC6B4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E2D4),
)

// Stock dark, Indigo scheme -- seed #9FA8DA.
private val StockDarkIndigo: ColorScheme = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF132054),
    primaryContainer = Color(0xFF5E69A1),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF455187),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF202335),
    secondaryContainer = Color(0xFF686B80),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFE5BAD9),
    onTertiary = Color(0xFF361B32),
    tertiaryContainer = Color(0xFF85627D),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF262019),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF262019),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceTint = Color(0xFFB9C3FF),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF18120B),
    surfaceBright = Color(0xFF3F3830),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF29231B),
    surfaceContainerHigh = Color(0xFF342D25),
    surfaceContainerHighest = Color(0xFF3F3830),
)

// Stock light, Violet scheme -- seed #7C4DFF.
private val StockLightViolet: ColorScheme = lightColorScheme(
    primary = Color(0xFF4D4079),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9789C7),
    onPrimaryContainer = Color(0xFF190942),
    inversePrimary = Color(0xFFCDBDFF),
    secondary = Color(0xFF4B465B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF958EA6),
    onSecondaryContainer = Color(0xFF171325),
    tertiary = Color(0xFF653D4C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB58495),
    onTertiaryContainer = Color(0xFF2A0B19),
    background = Color(0xFFF8F1E7),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFF8F1E7),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceTint = Color(0xFF63568F),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    outline = Color(0xFF7D7667),
    outlineVariant = Color(0xFFCEC6B4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E2D4),
)

// Stock dark, Violet scheme -- seed #B388FF.
private val StockDarkViolet: ColorScheme = darkColorScheme(
    primary = Color(0xFFD4BBFC),
    onPrimary = Color(0xFF2D184F),
    primaryContainer = Color(0xFF77629C),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF5E4A82),
    secondary = Color(0xFFCDC2DB),
    onSecondary = Color(0xFF282134),
    secondaryContainer = Color(0xFF72697F),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFF1B7C4),
    onTertiary = Color(0xFF3C1923),
    tertiaryContainer = Color(0xFF8F5F6B),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF262019),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF262019),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceTint = Color(0xFFD4BBFC),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF18120B),
    surfaceBright = Color(0xFF3F3830),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF29231B),
    surfaceContainerHigh = Color(0xFF342D25),
    surfaceContainerHighest = Color(0xFF3F3830),
)

// Stock light, Green scheme -- seed #1E8E3E.
private val StockLightGreen: ColorScheme = lightColorScheme(
    primary = Color(0xFF215329),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6A9E6D),
    onPrimaryContainer = Color(0xFF001B05),
    inversePrimary = Color(0xFF9DD49E),
    secondary = Color(0xFF3C4D3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF849782),
    onSecondaryContainer = Color(0xFF0A190B),
    tertiary = Color(0xFF224F56),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF6D99A1),
    onTertiaryContainer = Color(0xFF00191D),
    background = Color(0xFFF8F1E7),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFF8F1E7),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceTint = Color(0xFF37693D),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    outline = Color(0xFF7D7667),
    outlineVariant = Color(0xFFCEC6B4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E2D4),
)

// Stock dark, Green scheme -- seed #81C995.
private val StockDarkGreen: ColorScheme = darkColorScheme(
    primary = Color(0xFF96D5A6),
    onPrimary = Color(0xFF002B13),
    primaryContainer = Color(0xFF3D7851),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF245F3A),
    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF16281B),
    secondaryContainer = Color(0xFF5D7261),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFA2CEDA),
    onTertiary = Color(0xFF002931),
    tertiaryContainer = Color(0xFF49737E),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF262019),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF262019),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceTint = Color(0xFF96D5A6),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF18120B),
    surfaceBright = Color(0xFF3F3830),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF29231B),
    surfaceContainerHigh = Color(0xFF342D25),
    surfaceContainerHighest = Color(0xFF3F3830),
)

// Stock light, Orange scheme -- seed #E8710A.
private val StockLightOrange: ColorScheme = lightColorScheme(
    primary = Color(0xFF723A13),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC88154),
    onPrimaryContainer = Color(0xFF290E00),
    inversePrimary = Color(0xFFFFB68A),
    secondary = Color(0xFF5E4333),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFAD8B77),
    onSecondaryContainer = Color(0xFF251105),
    tertiary = Color(0xFF4C4B20),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF969462),
    onTertiaryContainer = Color(0xFF171700),
    background = Color(0xFFF8F1E7),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFF8F1E7),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceTint = Color(0xFF8C4F26),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    outline = Color(0xFF7D7667),
    outlineVariant = Color(0xFFCEC6B4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E2D4),
)

// Stock dark, Orange scheme -- seed #F9AB00.
private val StockDarkOrange: ColorScheme = darkColorScheme(
    primary = Color(0xFFF2BE6E),
    onPrimary = Color(0xFF342000),
    primaryContainer = Color(0xFF8E651D),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF734D03),
    secondary = Color(0xFFDCC3A1),
    onSecondary = Color(0xFF30220B),
    secondaryContainer = Color(0xFF7E694D),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB6CEA3),
    onTertiary = Color(0xFF17290C),
    tertiaryContainer = Color(0xFF5E734E),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF262019),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF262019),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceTint = Color(0xFFF2BE6E),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF18120B),
    surfaceBright = Color(0xFF3F3830),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF29231B),
    surfaceContainerHigh = Color(0xFF342D25),
    surfaceContainerHighest = Color(0xFF3F3830),
)

// Stock light, Red scheme -- seed #D93025.
private val StockLightRed: ColorScheme = lightColorScheme(
    primary = Color(0xFF76362E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCD7C71),
    onPrimaryContainer = Color(0xFF330403),
    inversePrimary = Color(0xFFFFB4A9),
    secondary = Color(0xFF60423D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFAF8983),
    onSecondaryContainer = Color(0xFF26100D),
    tertiary = Color(0xFF59471B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA78F5C),
    onTertiaryContainer = Color(0xFF1F1400),
    background = Color(0xFFF8F1E7),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFF8F1E7),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceTint = Color(0xFF904A41),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8F0E2),
    error = Color(0xFF98000A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF574B),
    onErrorContainer = Color(0xFF360001),
    outline = Color(0xFF7D7667),
    outlineVariant = Color(0xFFCEC6B4),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE1D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF3E5),
    surfaceContainer = Color(0xFFF5EDDF),
    surfaceContainerHigh = Color(0xFFEFE7DA),
    surfaceContainerHighest = Color(0xFFE9E2D4),
)

// Stock dark, Red scheme -- seed #F28B82.
private val StockDarkRed: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB3AC),
    onPrimary = Color(0xFF47120F),
    primaryContainer = Color(0xFFA15852),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF84413B),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF361E1B),
    secondaryContainer = Color(0xFF866461),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFE1C38C),
    onTertiary = Color(0xFF312200),
    tertiaryContainer = Color(0xFF81693B),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF262019),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF262019),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceTint = Color(0xFFFFB3AC),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF520003),
    errorContainer = Color(0xFFCF2C27),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF18120B),
    surfaceBright = Color(0xFF3F3830),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF29231B),
    surfaceContainerHigh = Color(0xFF342D25),
    surfaceContainerHighest = Color(0xFF3F3830),
)
// END GENERATED STOCK SCHEMES

/** The generated Stock scheme for an accent seed (rule 4: the accent
 *  picker is a seed choice -- the whole scheme comes from the seed). */
private fun stockScheme(accent: AccentColour, darkTheme: Boolean): ColorScheme =
    if (darkTheme) {
        when (accent) {
            AccentColour.BLUE -> StockDarkBlue
            AccentColour.INDIGO -> StockDarkIndigo
            AccentColour.VIOLET -> StockDarkViolet
            AccentColour.GREEN -> StockDarkGreen
            AccentColour.ORANGE -> StockDarkOrange
            AccentColour.RED -> StockDarkRed
        }
    } else {
        when (accent) {
            AccentColour.BLUE -> StockLightBlue
            AccentColour.INDIGO -> StockLightIndigo
            AccentColour.VIOLET -> StockLightViolet
            AccentColour.GREEN -> StockLightGreen
            AccentColour.ORANGE -> StockLightOrange
            AccentColour.RED -> StockLightRed
        }
    }

/**
 * Phosphor glow -- the soft, subtle halo on the accent (DECIDED
 *  2026-11-26). A low-alpha accent shadow, not a neon ring: 6.dp
 * elevation, ambient 0.35 / spot 0.25. Applied only to the accent's
 * hero shapes (page-indicator dot, widget resize handle) -- never to
 * text, icons, or the scrollbar.
 */
fun Modifier.phosphorGlow(color: Color, shape: Shape = CircleShape): Modifier =
    shadow(6.dp, shape, ambientColor = color.copy(alpha = 0.35f), spotColor = color.copy(alpha = 0.25f))

@Composable
fun LauncherTheme(
    mode: ThemeMode = ThemeMode.AUTO,
    theme: LauncherThemeChoice = LauncherThemeChoice.MATERIAL_YOU,
    accent: AccentColour = AccentColour.BLUE,
    font: FontChoice = FontChoice.SYSTEM,
    amoledBlack: Boolean = false,
    // #50 Ambient: the wallpaper's primary colour, derived by the
    // caller. Null falls back to the chosen accent (the wallpaper
    // yielded nothing usable). Only used when theme == AMBIENT.
    ambientAccent: Color? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when (theme) {
        LauncherThemeChoice.STOCK -> {
            val base = stockScheme(accent, darkTheme)
            if (amoledBlack && darkTheme) base.copy(background = Color.Black) else base
        }
        LauncherThemeChoice.AMBIENT -> {
            // KNOWN DEVIATION (rules 2, 5): Ambient replaces the
            // primary role with the wallpaper colour and derives
            // onPrimary from a luminance threshold. That is a
            // hand-picked pairing, not a pipeline derivation -- the
            // correct fix is generating the scheme from the wallpaper
            // colour as a seed at runtime, which needs the
            // material-color-utilities artifact at runtime (it is not
            // in the offline Gradle cache, so the Stock scheme is
            // generated offline instead). Until that dependency lands
            // or Ambient is re-scoped, this branch keeps its previous
            // behaviour and is recorded as a deviation in
            // rebuild-notes-theme.md. Everything else -- the warm
            // backgrounds, the surfaces -- stays stock, so Ambient
            // is a tint, not a new theme.
            val base = stockScheme(accent, darkTheme)
            val primary = ambientAccent?.let { adjustAmbientAccent(it, darkTheme) }
                ?: Color(if (darkTheme) accent.darkArgb else accent.lightArgb)
            val onPrimary = if (relativeLuminance(primary) > 0.5f) Color(0xFF1F1F1F) else Color.White
            base.copy(primary = primary, onPrimary = onPrimary)
        }
        LauncherThemeChoice.MATERIAL_YOU -> {
            // Rule 5: swap the SCHEME, never individual colours.
            // The dynamic scheme is used exactly as the system
            // derives it from the wallpaper -- no accent override.
            // Below API 31 there is no dynamic colour, so the Stock
            // scheme (from the same accent seed) is the fallback.
            // The accent picker therefore applies to Stock only;
            // under Material You the wallpaper is the seed.
            val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                stockScheme(accent, darkTheme)
            }
            if (amoledBlack && darkTheme) dynamic.copy(background = Color.Black) else dynamic
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = MarlowShapes,
        typography = Typography().withFont(fontFamilyFor(font)),
        content = content,
    )
}
