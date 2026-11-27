#!/usr/bin/env python3
"""Regenerate the Marlow Stock colour schemes (DESIGN-RESEARCH rules 3-6).

The Stock scheme is generated from a SINGLE seed per accent through the
HCT pipeline (Material Color Utilities, tonal-spot variant -- the Android
Material You variant), light + dark exported. No role is hand-tuned
afterwards, except the warm-neutral override documented below.

Seeds (DESIGN-RESEARCH section 4.1, locked Robert decision): each accent
option is a SEED, not a colour applied to the UI. The light value seeds
the light scheme, the dark value seeds the dark scheme. The default
accent is P22 phosphor blue: #3333CC light / #5555FF dark. These values
mirror `AccentColour` in LauncherModels.kt -- if that enum changes, the
ACCENTS table below must change with it, and vice versa.

Warm neutrals (locked Robert decision, DESIGN-RESEARCH section 4.1):
the warm light background #F8F1E7 and dark background #262019 are the
neutral palette's expression. The seed-generated neutrals are REPLACED
for the neutral family only: each mode's neutral / neutral-variant
palettes are built from that mode's locked background colour (its HCT
hue, pipeline-convention chroma 6 for neutral / 8 for neutral-variant --
the same chroma the tonal-spot pipeline itself uses for its neutrals),
and the M3 tone mapping is applied. Background and surface are pinned
to the locked values. In dark mode the container tones are shifted +2
from the M3 baseline (12/17/22 -> 14/19/24) so the container ladder
stays monotonic ABOVE the pinned surface: the locked dark background
is tone ~12.7, so the baseline container tone 12 would sit (invisibly)
below the surface and the dock would not read as a step above it.

Everything chromatic (primary / secondary / tertiary / error families,
inversePrimary, surfaceTint, scrim) comes straight from the pipeline.

Re-running:
    python3 -m venv .venv && .venv/bin/pip install material-color-utilities
    .venv/bin/python tools/generate_stock_scheme.py
(or: pip install material-color-utilities, then python3 the script.)
The script rewrites ONLY the region between the
    // BEGIN GENERATED STOCK SCHEMES
    // END GENERATED STOCK SCHEMES
markers in app/src/main/java/app/launcher/ui/theme/Theme.kt, then prints
the WCAG contrast table for the key role pairs and fails loudly if any
text pair is below 4.5:1 or any non-text/boundary pair below 3:1.

Requires: material-color-utilities (official Python package, the same
HCT engine as the Material Theme Builder). No network use at runtime
beyond the one-time pip install.
"""

import re
import sys
from pathlib import Path

try:
    import material_color_utilities as mcu
except ImportError:
    sys.exit(
        "material-color-utilities is not installed.\n"
        "Run: python3 -m venv .venv && .venv/bin/pip install material-color-utilities\n"
        "then: .venv/bin/python tools/generate_stock_scheme.py"
    )

# ---------------------------------------------------------------- seeds
# (name, light seed, dark seed) -- mirrors AccentColour in LauncherModels.kt.
ACCENTS = [
    ("Blue", "#3333CC", "#5555FF"),    # P22 phosphor blue -- the default
    ("Indigo", "#3F51B5", "#9FA8DA"),
    ("Violet", "#7C4DFF", "#B388FF"),
    ("Green", "#1E8E3E", "#81C995"),
    ("Orange", "#E8710A", "#F9AB00"),
    ("Red", "#D93025", "#F28B82"),
]

WARM_LIGHT_BG = "#F8F1E7"   # locked: warm paper
WARM_DARK_BG = "#262019"    # locked: warm charcoal

# Roles in the order lightColorScheme/darkColorScheme take them (Compose
# Material 3 1.4). Values are taken from DynamicScheme attributes of the
# same name (snake_case -> camelCase).
ROLES = [
    "primary", "on_primary", "primary_container", "on_primary_container",
    "inverse_primary", "secondary", "on_secondary", "secondary_container",
    "on_secondary_container", "tertiary", "on_tertiary",
    "tertiary_container", "on_tertiary_container", "background",
    "on_background", "surface", "on_surface", "surface_variant",
    "on_surface_variant", "surface_tint", "inverse_surface",
    "inverse_on_surface", "error", "on_error", "error_container",
    "on_error_container", "outline", "outline_variant", "scrim",
    "surface_dim", "surface_bright", "surface_container_lowest",
    "surface_container_low", "surface_container", "surface_container_high",
    "surface_container_highest",
]

# (foreground role, background role, minimum ratio, kind)
CONTRAST_PAIRS = [
    ("on_primary", "primary", 4.5, "text"),
    ("on_primary_container", "primary_container", 4.5, "text"),
    ("on_secondary", "secondary", 4.5, "text"),
    ("on_secondary_container", "secondary_container", 4.5, "text"),
    ("on_tertiary", "tertiary", 4.5, "text"),
    ("on_tertiary_container", "tertiary_container", 4.5, "text"),
    ("on_error", "error", 4.5, "text"),
    ("on_error_container", "error_container", 4.5, "text"),
    ("on_background", "background", 4.5, "text"),
    ("on_surface", "surface", 4.5, "text"),
    ("on_surface_variant", "surface_variant", 4.5, "text"),
    ("on_surface", "surface_container", 4.5, "text"),
    ("on_surface", "surface_container_high", 4.5, "text"),
    ("inverse_on_surface", "inverse_surface", 4.5, "text"),
    ("outline", "surface", 3.0, "boundary"),
    # outline_variant is a decorative divider, not a meaningful
    # boundary: M3's own baseline is ~1.5:1 by design, so no 3:1
    # requirement applies. It is reported (info) but never fails.
]


def argb_of(hex_str):
    return int(hex_str[1:], 16) | 0xFF000000


def hct_of(hex_str):
    return mcu.theme_from_argb_color(
        argb_of(hex_str), variant=mcu.Variant.TONALSPOT
    ).schemes.light.source_color_hct


def warm_palettes():
    """Neutral palettes per mode, from each mode's locked background."""
    light_hct = hct_of(WARM_LIGHT_BG)
    dark_hct = hct_of(WARM_DARK_BG)
    return {
        "light": (
            mcu.TonalPalette(light_hct.hue, 6.0),
            mcu.TonalPalette(light_hct.hue, 8.0),
        ),
        "dark": (
            mcu.TonalPalette(dark_hct.hue, 6.0),
            mcu.TonalPalette(dark_hct.hue, 8.0),
        ),
    }


def tone(palette, tone_value):
    return "#%06x" % (palette.get_argb(tone_value) & 0xFFFFFF)


def apply_warm_neutrals(roles, mode, palettes):
    """Replace the neutral family with the warm family (rule 4 note).

    M3 tone mapping; dark container tones shifted +2 (see module docstring).
    """
    neutral, neutral_variant = palettes[mode]
    locked_bg = WARM_LIGHT_BG if mode == "light" else WARM_DARK_BG
    if mode == "light":
        container_tones = (100, 96, 94, 92, 90)  # lowest, low, container, high, highest
        overrides = {
            "background": locked_bg,
            "surface": locked_bg,
            "surface_dim": tone(neutral, 87),
            "surface_bright": tone(neutral, 98),
            "on_background": tone(neutral, 10),
            "on_surface": tone(neutral, 10),
            "on_surface_variant": tone(neutral_variant, 30),
            "surface_variant": tone(neutral_variant, 90),
            "outline": tone(neutral_variant, 50),
            "outline_variant": tone(neutral_variant, 80),
            "inverse_surface": tone(neutral, 20),
            "inverse_on_surface": tone(neutral, 95),
        }
    else:
        container_tones = (4, 10, 14, 19, 24)  # +2 shift, see module docstring
        overrides = {
            "background": locked_bg,
            "surface": locked_bg,
            "surface_dim": tone(neutral, 6),
            "surface_bright": tone(neutral, 24),
            "on_background": tone(neutral, 90),
            "on_surface": tone(neutral, 90),
            "on_surface_variant": tone(neutral_variant, 80),
            "surface_variant": tone(neutral_variant, 30),
            "outline": tone(neutral_variant, 60),
            "outline_variant": tone(neutral_variant, 30),
            "inverse_surface": tone(neutral, 90),
            "inverse_on_surface": tone(neutral, 20),
        }
    for key, t in zip(
        (
            "surface_container_lowest", "surface_container_low",
            "surface_container", "surface_container_high",
            "surface_container_highest",
        ),
        container_tones,
    ):
        overrides[key] = tone(neutral, t)
    roles.update(overrides)


def camel(snake):
    parts = snake.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


def kotlin_color(hex_str):
    return "Color(0xFF%s)" % hex_str[1:].upper()


def luminance(hex_str):
    argb = argb_of(hex_str)
    def channel(c):
        c = c / 255.0
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r = channel((argb >> 16) & 0xFF)
    g = channel((argb >> 8) & 0xFF)
    b = channel(argb & 0xFF)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(fg_hex, bg_hex):
    l1, l2 = luminance(fg_hex), luminance(bg_hex)
    hi, lo = max(l1, l2), min(l1, l2)
    return (hi + 0.05) / (lo + 0.05)


def generate():
    palettes = warm_palettes()
    schemes = {}   # (accent, mode) -> {role: "#rrggbb"}
    for name, light_seed, dark_seed in ACCENTS:
        for mode, seed in (("light", light_seed), ("dark", dark_seed)):
            theme = mcu.theme_from_argb_color(
                argb_of(seed), variant=mcu.Variant.TONALSPOT
            )
            scheme = getattr(theme.schemes, mode)
            # The pipeline exposes no on_background: M3 defines it as
            # on_surface, and the warm override below sets it explicitly.
            roles = {
                role: getattr(scheme, role)
                for role in ROLES if role != "on_background"
            }
            roles["on_background"] = roles["on_surface"]
            apply_warm_neutrals(roles, mode, palettes)
            schemes[(name, mode)] = roles
    return schemes


def emit_kotlin(schemes):
    lines = [
        "// Generated by tools/generate_stock_scheme.py from one seed per",
        "// accent through the HCT pipeline (Material Color Utilities,",
        "// tonal-spot variant, standard contrast). Do NOT hand-edit:",
        "// change the seed in the script and re-run it -- changing the",
        "// seed regenerates everything. Warm neutrals replace the",
        "// seed-generated neutrals (see the script header); every other",
        "// role is the pipeline's output, unmodified.",
    ]
    for name, _, _ in ACCENTS:
        for mode in ("light", "dark"):
            roles = schemes[(name, mode)]
            fn = "lightColorScheme" if mode == "light" else "darkColorScheme"
            lines.append("")
            lines.append(
                "// Stock %s, %s scheme -- seed %s."
                % (
                    mode,
                    name,
                    dict((n, (l, d)) for n, l, d in ACCENTS)[name][
                        0 if mode == "light" else 1
                    ],
                )
            )
            lines.append(
                "private val Stock%s%s: ColorScheme = %s("
                % (mode.capitalize(), name, fn)
            )
            for role in ROLES:
                lines.append(
                    "    %s = %s," % (camel(role), kotlin_color(roles[role]))
                )
            lines.append(")")
    return "\n".join(lines)


def check_contrast(schemes):
    print("Contrast ratios (WCAG, foreground on background):")
    failures = []
    for (accent, mode), roles in sorted(schemes.items()):
        print("  Stock %s / %s:" % (mode, accent))
        for fg, bg, minimum, kind in CONTRAST_PAIRS:
            ratio = contrast(roles[fg], roles[bg])
            flag = "" if ratio >= minimum else "  <-- FAILS (min %.1f)" % minimum
            if flag:
                failures.append((accent, mode, fg, bg, ratio, minimum))
            print(
                "    %-24s on %-24s %5.2f:1%s"
                % (fg, bg, ratio, flag)
            )
        print(
            "    %-24s on %-24s %5.2f:1 (info: decorative divider, no minimum)"
            % (
                "outline_variant", "surface",
                contrast(roles["outline_variant"], roles["surface"]),
            )
        )
    if failures:
        print("\n%d contrast FAILURE(S) -- fix the seed, never a one-off hex." % len(failures))
        sys.exit(1)
    print("\nAll key pairs pass (>= 4.5:1 text, >= 3:1 boundary).")


def main():
    script_dir = Path(__file__).resolve().parent
    theme_kt = (
        script_dir.parent
        / "app/src/main/java/app/launcher/ui/theme/Theme.kt"
    )
    if not theme_kt.exists():
        sys.exit("Theme.kt not found at %s" % theme_kt)
    schemes = generate()
    block = emit_kotlin(schemes)
    text = theme_kt.read_text()
    pattern = re.compile(
        r"// BEGIN GENERATED STOCK SCHEMES.*?// END GENERATED STOCK SCHEMES",
        re.DOTALL,
    )
    if not pattern.search(text):
        sys.exit(
            "Markers '// BEGIN GENERATED STOCK SCHEMES' / "
            "'// END GENERATED STOCK SCHEMES' not found in Theme.kt"
        )
    new_text = pattern.sub(
        "// BEGIN GENERATED STOCK SCHEMES\n" + block + "\n// END GENERATED STOCK SCHEMES",
        text,
    )
    theme_kt.write_text(new_text)
    print("Wrote generated schemes into %s" % theme_kt)
    check_contrast(schemes)


if __name__ == "__main__":
    main()
