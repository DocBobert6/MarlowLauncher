#!/usr/bin/env python3
"""Marlow Launcher icon options v2 — a miniature home screen, with depth.

Direction (Robert, 2026-11-26): the icon must show what the app IS —
a home screen — not a generic glyph. V1 (M, house, TV, dots, disc)
was rightly rejected as clip-art that any app could wear.

All four variants hold the layout constant (2x2 app tiles + dock on the
warm charcoal, one tile glowing phosphor blue) and vary ONLY the depth
treatment, so the choice is about depth, not layout:

  A · Raised tiles   — tiles float above the charcoal on soft shadows.
  B · Floating screen — a warm-paper screen card lifts off the charcoal.
  C · Recessed screen — a dark inset panel; the blue tile's light spills
                         over its neighbours.
  D · Stacked pages  — two home pages overlap, front page raised.

Same hand-drawn method as v1: exact geometric primitives with PIL,
no AI generation. Shadows are blurred black copies of the shapes;
the glow is a blurred phosphor-blue copy at low alpha.
"""
import os
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.dirname(os.path.abspath(__file__))
SIZE = 512
C = 256

CHARCOAL = (0x26, 0x20, 0x19, 255)
CHARCOAL_LT = (0x33, 0x2E, 0x28, 255)  # lighter charcoal (back page / dock)
RECESS = (0x1B, 0x17, 0x13, 255)       # darker charcoal (inset panel)
PHOSPHOR = (0x55, 0x55, 0xFF, 255)
PAPER = (0xF8, 0xF1, 0xE7, 255)
TILE_MUTED = (0xDC, 0xD2, 0xBE, 255)   # quiet warm grey tiles on paper
BLACK = (0x00, 0x00, 0x00, 255)


def base():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, SIZE, SIZE], radius=120, fill=CHARCOAL)
    return img


def _alpha_scaled(layer, alpha):
    r, g, b, a = layer.split()
    return Image.merge("RGBA", (r, g, b, a.point(lambda v: v * alpha // 255)))


def drop_shadow(img, draw_fn, dx=0, dy=8, blur=14, alpha=100):
    """Blurred black copy of a shape, offset down — the depth cue."""
    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(_alpha_scaled(layer, alpha), (dx, dy))


def glow(img, draw_fn, blur=20, alpha=130):
    """Blurred phosphor-blue copy of a shape — the light, not a ring."""
    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(_alpha_scaled(layer, alpha))


def rrect(d, box, radius, fill):
    d.rounded_rectangle(box, radius=radius, fill=fill)


def tiles_2x2(d, x0, y0, size, gap, fills, radius=None):
    """Four rounded tiles; fills is a list of 4 colours, row-major."""
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


def icon_a_raised():
    """A — tiles float above the charcoal on soft shadows; one glows."""
    img = base()
    d = ImageDraw.Draw(img)
    boxes = [(150, 140, 246, 236), (266, 140, 362, 236),
             (150, 256, 246, 352), (266, 256, 362, 352)]
    for box in boxes:
        drop_shadow(img, lambda dd, b=box: rrect(dd, b, 22, BLACK), dy=7, blur=12, alpha=95)
    glow(img, lambda dd: rrect(dd, boxes[0], 22, PHOSPHOR), blur=22, alpha=130)
    d = ImageDraw.Draw(img)
    for box in boxes[1:]:
        rrect(d, box, 22, PAPER)
    rrect(d, boxes[0], 22, PHOSPHOR)
    # slim dock bar
    drop_shadow(img, lambda dd: rrect(dd, [150, 384, 362, 412], 14, BLACK), dy=5, blur=10, alpha=90)
    d = ImageDraw.Draw(img)
    rrect(d, [150, 384, 362, 412], 14, CHARCOAL_LT)
    return img


def icon_b_floating():
    """B — warm-paper screen card lifts off the charcoal; grid on it."""
    img = base()
    drop_shadow(img, lambda dd: rrect(dd, [138, 128, 374, 400], 32, BLACK),
                dy=12, blur=26, alpha=150)
    d = ImageDraw.Draw(img)
    rrect(d, [138, 128, 374, 400], 32, PAPER)
    glow(img, lambda dd: rrect(dd, [176, 160, 248, 232], 18, PHOSPHOR), blur=20, alpha=130)
    d = ImageDraw.Draw(img)
    fills = [PHOSPHOR, TILE_MUTED, TILE_MUTED, TILE_MUTED]
    tiles_2x2(d, 176, 160, 72, 16, fills, radius=18)
    rrect(d, [176, 344, 336, 368], 12, CHARCOAL)
    return img


def icon_c_recessed():
    """C — dark inset panel; the blue tile's light spills over neighbours."""
    img = base()
    d = ImageDraw.Draw(img)
    rrect(d, [138, 128, 374, 412], 32, RECESS)
    # thin top highlight sells the inset
    d.line([(150, 132), (362, 132)], fill=(0x3A, 0x34, 0x2C, 255), width=2)
    glow(img, lambda dd: rrect(dd, [163, 160, 247, 244], 20, PHOSPHOR), blur=20, alpha=130)
    d = ImageDraw.Draw(img)
    fills = [PHOSPHOR, PAPER, PAPER, PAPER]
    boxes = tiles_2x2(d, 163, 160, 84, 18, fills, radius=20)
    # light spill: a wide soft blue haze over the whole panel
    spill = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(spill)
    sd.ellipse([95, 92, 315, 312], fill=PHOSPHOR)
    spill = spill.filter(ImageFilter.GaussianBlur(60))
    img.alpha_composite(_alpha_scaled(spill, 55))
    # re-draw the glowing tile crisp on top of the haze
    d = ImageDraw.Draw(img)
    rrect(d, boxes[0], 20, PHOSPHOR)
    rrect(d, [163, 356, 349, 380], 12, CHARCOAL_LT)
    return img


def icon_d_stacked():
    """D — two home pages overlap; the front page is raised."""
    img = base()
    # back page peeks out top-left
    drop_shadow(img, lambda dd: rrect(dd, [138, 106, 350, 368], 28, BLACK),
                dy=8, blur=18, alpha=120)
    d = ImageDraw.Draw(img)
    rrect(d, [138, 106, 350, 368], 28, CHARCOAL_LT)
    # front page, raised above the back page
    drop_shadow(img, lambda dd: rrect(dd, [174, 142, 386, 404], 28, BLACK),
                dy=8, blur=18, alpha=130)
    d = ImageDraw.Draw(img)
    rrect(d, [174, 142, 386, 404], 28, PAPER)
    glow(img, lambda dd: rrect(dd, [196, 180, 260, 244], 16, PHOSPHOR), blur=18, alpha=130)
    d = ImageDraw.Draw(img)
    fills = [PHOSPHOR, TILE_MUTED, TILE_MUTED, TILE_MUTED]
    tiles_2x2(d, 196, 180, 64, 14, fills, radius=16)
    rrect(d, [196, 344, 338, 366], 11, CHARCOAL)
    return img


ICONS = [
    ("2a-raised", "A · Raised tiles", icon_a_raised),
    ("2b-floating", "B · Floating screen", icon_b_floating),
    ("2c-recessed", "C · Recessed screen", icon_c_recessed),
    ("2d-stacked", "D · Stacked pages", icon_d_stacked),
]


def main():
    rendered = []
    for slug, label, fn in ICONS:
        img = fn()
        img.save(os.path.join(OUT, f"icon-{slug}.png"))
        rendered.append((label, img))

    sheet = Image.new("RGBA", (1100, 340), (0x14, 0x14, 0x14, 255))
    d = ImageDraw.Draw(sheet)
    x = 40
    for label, img in rendered:
        small = img.resize((180, 180), Image.LANCZOS)
        sheet.alpha_composite(small, (x, 40))
        d.text((x + 90, 245), label, fill=(255, 255, 255, 255), anchor="ma")
        x += 260
    sheet.convert("RGB").save(os.path.join(OUT, "comparison2.png"))
    print("wrote", [f"icon-{s}.png" for s, _, _ in ICONS], "comparison2.png")


if __name__ == "__main__":
    main()
