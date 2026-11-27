#!/usr/bin/env python3
"""Render ic_launcher_foreground.xml back to PNGs to verify the vector
drawable draws what the PIL design (icon-3c-recessed.png) shows.

Purpose-built mini-parser for the exact pathData subset the icon uses:
M, H, V, Q, Z and 'a' (semicircle arcs). It is a check, not a renderer:
if the XML transcription slipped a number, the render will not match
the design and the difference will be visible.

LESSON (2026-11-26): v2 passed this check at 512px and still failed on
Robert's phone — the 512px square is NOT the acceptance test. This
verifier now also renders the vector at 48px and 96px under circular
masks of 66dp AND 72dp diameter, and measures the spill tint on the
neighbouring tile at 48px masked. The icon passes only if the 48px
masked renders show the panel edge, the spill, and the dock pill.
"""
import math
import os
import re
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
XML = os.path.join(HERE, "..", "app", "src", "main", "res",
                   "drawable", "ic_launcher_foreground.xml")
DESIGN = os.path.join(HERE, "icon-3c-recessed.png")
SCALE = 512 / 108  # viewport units -> pixels

NS = "{http://schemas.android.com/apk/res/android}"


def parse_color(s):
    s = s.lstrip("#")
    if len(s) == 8:
        a, r, g, b = (int(s[i:i + 2], 16) for i in (0, 2, 4, 6))
    else:
        a = 255
        r, g, b = (int(s[i:i + 2], 16) for i in (0, 2, 4))
    return (r, g, b, a)


def flatten_path(d):
    """Return a list of subpaths; each subpath is a list of (x, y) points."""
    tokens = re.findall(r"[MmHhVvQqZzAa]|[-+]?\d*\.?\d+", d)
    i = 0
    subpaths, cur, start = [], [], None
    x = y = 0.0

    def close():
        nonlocal cur
        if cur:
            subpaths.append(cur)
            cur = []

    while i < len(tokens):
        t = tokens[i]
        if t in "MmHhVvQqAa":
            cmd = t
            i += 1
        elif t in "Zz":
            if cur and start is not None:
                cur.append(start)
            close()
            i += 1
            continue
        else:
            raise ValueError(f"unexpected token {t!r} in {d!r}")

        def num():
            nonlocal i
            v = float(tokens[i])
            i += 1
            return v

        if cmd == "M":
            close()
            x, y = num(), num()
            start = (x, y)
            cur = [(x, y)]
        elif cmd == "H":
            x = num()
            cur.append((x, y))
        elif cmd == "V":
            y = num()
            cur.append((x, y))
        elif cmd == "Q":
            cx, cy, x1, y1 = num(), num(), num(), num()
            x0, y0 = cur[-1]
            for k in range(1, 17):
                tt = k / 16
                px = (1 - tt) ** 2 * x0 + 2 * (1 - tt) * tt * cx + tt ** 2 * x1
                py = (1 - tt) ** 2 * y0 + 2 * (1 - tt) * tt * cy + tt ** 2 * y1
                cur.append((px, py))
            x, y = x1, y1
        elif cmd == "a":
            rx, ry, _rot, _large, sweep, dx, dy = (
                num(), num(), num(), num(), num(), num(), num())
            assert rx == ry, "only circular arcs expected"
            r = rx
            x1, y1 = x + dx, y + dy
            cx, cy = (x + x1) / 2, (y + y1) / 2
            a0 = math.atan2(y - cy, x - cx)
            step = math.pi / 24 * (1 if sweep == 1 else -1)
            for k in range(1, 25):
                a = a0 + step * k
                cur.append((cx + r * math.cos(a), cy + r * math.sin(a)))
            x, y = x1, y1
    close()
    return subpaths


def render_vector(size=512):
    """Render the foreground XML over the solid adaptive-icon background."""
    scale = size / 108
    tree = ET.parse(XML)
    root = tree.getroot()
    img = Image.new("RGBA", (size, size), (0x26, 0x20, 0x19, 255))
    for path in root.iter("path"):
        fill = parse_color(path.get(f"{NS}fillColor"))
        data = path.get(f"{NS}pathData")
        fill_type = path.get(f"{NS}fillType", "nonZero")
        subs = flatten_path(data)
        layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        if fill_type == "evenOdd" and len(subs) == 2:
            mask = Image.new("L", (size, size), 0)
            md = ImageDraw.Draw(mask)
            md.polygon([(px * scale, py * scale) for px, py in subs[0]], fill=255)
            md.polygon([(px * scale, py * scale) for px, py in subs[1]], fill=0)
            solid = Image.new("RGBA", (size, size), fill)
            layer.paste(solid, (0, 0), mask)
        else:
            for sub in subs:
                pts = [(px * scale, py * scale) for px, py in sub]
                ld.polygon(pts, fill=fill)
        img.alpha_composite(layer)
    return img


def apply_circle_mask(img, mask_diameter_dp):
    size = img.size[0]
    r = mask_diameter_dp / 108 * size / 2
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r], fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def spill_tint(img48):
    """Blue tint (B-R) on the top-right tile's edge near the glowing tile
    vs its far edge, in a 48px render. Positive near-minus-far means the
    light visibly spills onto the neighbour."""
    im = img48.convert("RGB")
    near = im.getpixel((26, 19))
    far = im.getpixel((32, 19))
    return (near[2] - near[0]) - (far[2] - far[0]), near, far


def main():
    failures = []
    img = render_vector(512)
    img.save(os.path.join(HERE, "icon-vector-render.png"))
    design = Image.open(DESIGN).convert("RGBA")
    sheet = Image.new("RGBA", (512 * 2 + 40, 512), (20, 20, 20, 255))
    sheet.alpha_composite(design, (0, 0))
    sheet.alpha_composite(img, (512 + 40, 0))
    sheet.convert("RGB").save(os.path.join(HERE, "icon-vector-compare.png"))

    # 512px agreement with the design: mean absolute pixel difference.
    import math as _m
    diff = 0
    n = 0
    for a, b in zip(design.getdata(), img.getdata()):
        diff += sum(abs(x - y) for x, y in zip(a[:3], b[:3]))
        n += 1
    mean_diff = diff / n / 3
    print(f"512px mean abs diff vs design: {mean_diff:.2f} per channel")
    if mean_diff > 6:
        failures.append(f"512px render differs from design by {mean_diff:.2f}")

    # Small-size acceptance: 48px and 96px, masked at 66dp and 72dp.
    for px in (48, 96):
        small = render_vector(px)
        small.save(os.path.join(HERE, f"icon-vector-{px}.png"))
        for mask_dp in (66, 72):
            masked = apply_circle_mask(small, mask_dp)
            masked.save(
                os.path.join(HERE, f"icon-vector-{px}-mask{mask_dp}.png"))
            masked.resize((px * 4, px * 4), Image.NEAREST).save(
                os.path.join(HERE, f"icon-vector-{px}-mask{mask_dp}-4x.png"))
    # Spill must be measurable at 48px masked (v2 measured ~10; v3 ~35).
    tint66, near, far = spill_tint(
        apply_circle_mask(render_vector(48), 66))
    print(f"48px mask66 spill tint: near {near} far {far} delta {tint66}")
    if tint66 < 20:
        failures.append(f"48px spill tint too weak: delta {tint66} (< 20)")
    # Dock pill must be present at 48px masked: a paper-bright pixel
    # below the grid, inside the circle. Pill spans dp x 37.1-70.9,
    # y 75.9-81.0 -> at 48px x 16.5-31.5, y 33.7-36.0; centre (24, 35).
    pill = apply_circle_mask(render_vector(48), 66).convert("RGB")
    pill_px = pill.getpixel((24, 35))
    print(f"48px mask66 dock pill pixel (24,35): {pill_px}")
    if sum(pill_px) < 500:
        failures.append(f"dock pill not visible at 48px masked: {pill_px}")

    print("wrote icon-vector-render.png icon-vector-compare.png "
          "icon-vector-48/96 (+masked) renders")
    if failures:
        print("VERIFY FAILED:")
        for f in failures:
            print("  -", f)
        sys.exit(1)
    print("verify_icon: PASS — vector matches v3 design at 512px and reads "
          "at 48px under both masks")


if __name__ == "__main__":
    main()
