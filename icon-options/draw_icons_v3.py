#!/usr/bin/env python3
"""Marlow Launcher icon v3 — C "Recessed screen", revised for real size.

LESSON (2026-11-26, from Robert's installer screenshot): the v2 C design
was approved on its 512px square render, but under Android's circular
mask at launcher size it reads as four flat tiles in a dark circle — the
recessed panel's depth is invisible, the light spill is gone, and the
dock pill is cropped/invisible. The design stays C (Robert re-picked it);
this revision makes the SAME design read at 48px under the mask:

  * Panel edge: v2's panel (#1B1713) is barely darker than the
    background (#262019) and its 0.8dp top highlight is sub-pixel at
    48px. V3 deepens the panel to near-black (#0E0B08) and puts a
    clearly lighter warm rim (#57493B, 3dp) around it, so the panel
    reads as a distinct inset with a visible edge at small size.
  * Tiles: larger (92px vs 84px at 512) and moved up, so the grid
    fills more of the safe zone.
  * Spill: stronger and tighter — blurred tile copy at alpha 200
    (was 130) plus a smaller, stronger haze (alpha 110, was 55),
    so a blue haze is still visible around the glowing tile at 48px.
  * Dock pill: v2's dark pill (#332E28 on #1B1713) had almost no
    contrast and sat where the mask crops. V3 makes it paper
    (#F8F1E7), centred, well inside the circle, with a clear gap
    below the grid — a light bar that reads as a dock at 48px.

Verification rule from now on: NEVER approve an icon on the 512px
square alone. This script renders every candidate at 48px and 96px,
plain and under circular masks of 66dp AND 72dp diameter (the two
plausible mask sizes — the safe zone is 66dp; the screenshot's tile
ratio suggests ~62-72dp), and the acceptance test is the 48px masked
render, compared side by side with the v2 render at the same size.
"""
import os
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.dirname(os.path.abspath(__file__))
SIZE = 512

CHARCOAL = (0x26, 0x20, 0x19, 255)
RECESS_V3 = (0x0E, 0x0B, 0x08, 255)  # near-black inset panel
RIM = (0x57, 0x49, 0x3B, 255)        # warm mid-brown panel edge
HIGHLIGHT = (0x6B, 0x5F, 0x50, 255)  # top inner light, stronger than v2
PHOSPHOR = (0x55, 0x55, 0xFF, 255)
PAPER = (0xF8, 0xF1, 0xE7, 255)
BLACK = (0x00, 0x00, 0x00, 255)


def _alpha_scaled(layer, alpha):
    r, g, b, a = layer.split()
    return Image.merge("RGBA", (r, g, b, a.point(lambda v: v * alpha // 255)))


def drop_shadow(img, draw_fn, dx=0, dy=8, blur=14, alpha=100):
    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(_alpha_scaled(layer, alpha), (dx, dy))


def glow(img, draw_fn, blur=20, alpha=130):
    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(_alpha_scaled(layer, alpha))


def rrect(d, box, radius, fill):
    d.rounded_rectangle(box, radius=radius, fill=fill)


def tiles_2x2(d, x0, y0, size, gap, fills, radius=None):
    radius = radius if radius is not None else size // 4
    boxes = [
        (x0, y0, x0 + size, y0 + size),
        (x0 + size + gap, y0, x0 + 2 * size + gap, y0 + size),
        (x0, y0 + size + gap, x0 + size, y0 + 2 * size + gap),
        (x0 + size + gap, y0 + size + gap, x0 + 2 * size + gap, y0 + 2 * size + gap),
    ]
    for box, fill in zip(boxes, fills):
        rrect(d, box, radius, fill)
    return boxes


def base():
    """Foreground over the adaptive-icon background (solid charcoal)."""
    img = Image.new("RGBA", (SIZE, SIZE), CHARCOAL)
    return img


def icon_c_v3():
    """C, revised for small size under the circular mask."""
    img = base()
    d = ImageDraw.Draw(img)
    # warm rim, then the near-black inset panel on top of it
    rrect(d, [124, 114, 388, 426], 38, RIM)
    rrect(d, [138, 128, 374, 412], 32, RECESS_V3)
    # top inner light, thicker and lighter than v2
    d.rectangle([150, 131, 362, 137], fill=HIGHLIGHT)
    # strong, tight glow from the top-left tile
    glow(img, lambda dd: rrect(dd, [158, 152, 250, 244], 18, PHOSPHOR),
         blur=14, alpha=200)
    d = ImageDraw.Draw(img)
    fills = [PHOSPHOR, PAPER, PAPER, PAPER]
    boxes = tiles_2x2(d, 158, 152, 92, 16, fills, radius=18)
    # light spill: a smaller, stronger blue haze over the whole panel
    spill = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(spill)
    sd.ellipse([120, 115, 300, 295], fill=PHOSPHOR)
    spill = spill.filter(ImageFilter.GaussianBlur(40))
    img.alpha_composite(_alpha_scaled(spill, 140))
    # re-draw the glowing tile crisp on top of the haze
    d = ImageDraw.Draw(img)
    rrect(d, boxes[0], 18, PHOSPHOR)
    # dock pill: paper, centred, clearly inside the circle
    rrect(d, [176, 360, 336, 384], 12, PAPER)
    return img


def icon_c_v2():
    """The shipped v2 C, re-rendered here for side-by-side comparison."""
    import draw_icons_v2 as v2
    return v2.icon_c_recessed()


def apply_circle_mask(img, mask_diameter_dp):
    """Crop the 108dp viewport to a circular mask of the given diameter."""
    size = img.size[0]
    r = mask_diameter_dp / 108 * size / 2
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r], fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main():
    v3 = icon_c_v3()
    v2 = icon_c_v2()
    v3.save(os.path.join(OUT, "icon-3c-recessed.png"))
    print("wrote icon-3c-recessed.png")

    # Small-size acceptance renders: 48px and 96px, plain and masked.
    for name, img in (("v2c", v2), ("v3c", v3)):
        for px in (48, 96):
            small = img.resize((px, px), Image.LANCZOS)
            small.save(os.path.join(OUT, f"icon-{name}-{px}.png"))
            for mask_dp in (66, 72):
                masked = apply_circle_mask(small, mask_dp)
                masked.save(
                    os.path.join(OUT, f"icon-{name}-{px}-mask{mask_dp}.png"))
                # 4x nearest-neighbour enlargement for pixel inspection
                big = masked.resize((px * 4, px * 4), Image.NEAREST)
                big.save(
                    os.path.join(OUT, f"icon-{name}-{px}-mask{mask_dp}-4x.png"))
    print("wrote 48/96 plain + masked renders for v2c and v3c")

    # Contact sheet: 48px masked (66dp) v2 vs v3, then 96px masked v3,
    # then the 512px designs — the 48px cells are shown at 4x so the
    # sheet itself is legible, but the acceptance files are the 1x PNGs.
    sheet = Image.new("RGBA", (1100, 560), (20, 20, 20, 255))
    d = ImageDraw.Draw(sheet)
    cells = [
        ("v2 C @48 masked 66dp (SHIPPED)", "icon-v2c-48-mask66-4x.png", 40, 40),
        ("v3 C @48 masked 66dp", "icon-v3c-48-mask66-4x.png", 320, 40),
        ("v3 C @48 masked 72dp", "icon-v3c-48-mask72-4x.png", 600, 40),
        ("v3 C @96 masked 66dp", "icon-v3c-96-mask66.png", 880, 40),
    ]
    for label, fn, x, y in cells:
        cell = Image.open(os.path.join(OUT, fn)).convert("RGBA")
        if cell.size[0] == 96:
            cell = cell.resize((192, 192), Image.NEAREST)
        sheet.alpha_composite(cell, (x, y))
        d.text((x + cell.size[0] // 2, y + cell.size[1] + 12), label,
               fill=(255, 255, 255, 255), anchor="ma")
    # 512px designs along the bottom, small
    for label, fn, x in (("v2 C 512", "icon-2c-recessed.png", 40),
                         ("v3 C 512", "icon-3c-recessed.png", 320)):
        cell = Image.open(os.path.join(OUT, fn)).convert("RGBA").resize(
            (180, 180), Image.LANCZOS)
        sheet.alpha_composite(cell, (x, 330))
        d.text((x + 90, 520), label, fill=(255, 255, 255, 255), anchor="ma")
    sheet.convert("RGB").save(os.path.join(OUT, "icon-v3-compare.png"))
    print("wrote icon-v3-compare.png")


if __name__ == "__main__":
    main()
