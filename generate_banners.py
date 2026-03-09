"""
Generates 9-patch (.9.png) banner images for each sample event.
9-patch rules:
  - Image is padded with 1px transparent border on all sides.
  - Left edge  (col 0)      : black pixels = vertical STRETCH zone.
  - Top edge   (row 0)      : black pixels = horizontal STRETCH zone.
  - Right edge (last col)   : black pixels = vertical CONTENT/padding zone.
  - Bottom edge(last row)   : black pixels = horizontal CONTENT/padding zone.
Output: app/src/main/res/drawable/banner_<name>.9.png
"""

from PIL import Image, ImageDraw, ImageFont
import os
import math

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__),
    "app", "src", "main", "res", "drawable"
)

# Content dimensions (pixels) --- the 9-patch border adds +2 each side
CONTENT_W = 400
CONTENT_H = 160

# Events: (filename_slug, title, subtitle, date, category, colours (top, bottom))
EVENTS = [
    (
        "tech_summit",
        "Tech Summit 2026",
        "College of Engineering",
        "Mar 15, 2026",
        "ACADEMIC",
        ("#1A237E", "#283593"),   # deep indigo gradient
        "#E3F2FD",                # text colour
    ),
    (
        "art_fair",
        "Campus Art Fair",
        "Fine Arts Department",
        "Mar 20, 2026",
        "SOCIAL",
        ("#4A148C", "#7B1FA2"),   # deep purple gradient
        "#FCE4EC",
    ),
    (
        "career_week",
        "Career Week",
        "Career Services",
        "Apr 10, 2026",
        "ACADEMIC",
        ("#004D40", "#00695C"),   # teal gradient
        "#E0F2F1",
    ),
    (
        "music_festival",
        "Music Festival",
        "Student Government",
        "Apr 25, 2026",
        "SOCIAL",
        ("#E65100", "#F57C00"),   # deep orange gradient
        "#FFF8E1",
    ),
    (
        "android_workshop",
        "Android Workshop",
        "Google Developer Group",
        "May 5, 2026",
        "WORKSHOP",
        ("#1B5E20", "#2E7D32"),   # green gradient
        "#F1F8E9",
    ),
    (
        "basketball_finals",
        "Basketball Finals",
        "Sports Council",
        "May 12, 2026",
        "SPORTS",
        ("#B71C1C", "#C62828"),   # red gradient
        "#FFF3E0",
    ),
]

BLACK = (0, 0, 0, 255)
TRANSPARENT = (0, 0, 0, 0)


def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))


def draw_vertical_gradient(draw, x0, y0, x1, y1, color_top, color_bottom):
    """Draw a vertical linear gradient rectangle."""
    r1, g1, b1 = hex_to_rgb(color_top)
    r2, g2, b2 = hex_to_rgb(color_bottom)
    height = y1 - y0
    for y in range(y0, y1):
        t = (y - y0) / max(height - 1, 1)
        r = int(r1 + (r2 - r1) * t)
        g = int(g1 + (g2 - g1) * t)
        b = int(b1 + (b2 - b1) * t)
        draw.line([(x0, y), (x1, y)], fill=(r, g, b, 255))


def load_font(size, bold=False):
    """Try to load a system font, fall back to default."""
    candidates = [
        "arialbd.ttf" if bold else "arial.ttf",
        "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for name in candidates:
        try:
            return ImageFont.truetype(name, size)
        except (IOError, OSError):
            pass
    return ImageFont.load_default()


def draw_rounded_rect(draw, xy, radius, fill):
    """Draw a filled rounded rectangle."""
    x0, y0, x1, y1 = xy
    draw.rectangle([x0 + radius, y0, x1 - radius, y1], fill=fill)
    draw.rectangle([x0, y0 + radius, x1, y1 - radius], fill=fill)
    draw.ellipse([x0, y0, x0 + 2 * radius, y0 + 2 * radius], fill=fill)
    draw.ellipse([x1 - 2 * radius, y0, x1, y0 + 2 * radius], fill=fill)
    draw.ellipse([x0, y1 - 2 * radius, x0 + 2 * radius, y1], fill=fill)
    draw.ellipse([x1 - 2 * radius, y1 - 2 * radius, x1, y1], fill=fill)


def make_banner(slug, title, subtitle, date, category, grad_colors, text_color):
    """Create a single 9-patch banner image."""
    # --- 1. Draw the content image (CONTENT_W x CONTENT_H) ----------------
    content = Image.new("RGBA", (CONTENT_W, CONTENT_H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(content)

    # Gradient background
    draw_vertical_gradient(draw, 0, 0, CONTENT_W, CONTENT_H,
                           grad_colors[0], grad_colors[1])

    # Decorative circles (top-right accent)
    accent = hex_to_rgb(text_color)
    for i, (cx, cy, r) in enumerate([
        (CONTENT_W - 30, -20, 60),
        (CONTENT_W - 10, 40, 40),
    ]):
        circ_img = Image.new("RGBA", (CONTENT_W, CONTENT_H), (0, 0, 0, 0))
        circ_draw = ImageDraw.Draw(circ_img)
        circ_draw.ellipse(
            [cx - r, cy - r, cx + r, cy + r],
            fill=(*accent, 25 - i * 8)
        )
        content = Image.alpha_composite(content, circ_img)
        draw = ImageDraw.Draw(content)

    # Category badge
    badge_font = load_font(11, bold=True)
    badge_text = f"  {category}  "
    bbox = draw.textbbox((0, 0), badge_text, font=badge_font)
    bw = bbox[2] - bbox[0] + 4
    bh = bbox[3] - bbox[1] + 4
    badge_x, badge_y = 16, 14
    draw_rounded_rect(draw, [badge_x, badge_y,
                              badge_x + bw, badge_y + bh], 6, (*hex_to_rgb(text_color), 50))
    draw.text((badge_x + 4, badge_y + 3), badge_text.strip(),
              font=badge_font, fill=(*hex_to_rgb(text_color), 220))

    # Title
    title_font = load_font(26, bold=True)
    draw.text((16, badge_y + bh + 10), title,
              font=title_font, fill=(*hex_to_rgb(text_color), 255))

    # Subtitle / organiser
    sub_font = load_font(13)
    draw.text((16, badge_y + bh + 10 + 32), subtitle,
              font=sub_font, fill=(*hex_to_rgb(text_color), 180))

    # Date — bottom-left
    date_font = load_font(12, bold=True)
    draw.text((16, CONTENT_H - 26), f"📅  {date}",
              font=date_font, fill=(*hex_to_rgb(text_color), 200))

    # Thin bottom accent line
    draw.rectangle([0, CONTENT_H - 4, CONTENT_W, CONTENT_H],
                   fill=(*hex_to_rgb(text_color), 60))

    # --- 2. Wrap in 9-patch border (total = CONTENT_W+2 x CONTENT_H+2) ----
    W9 = CONTENT_W + 2
    H9 = CONTENT_H + 2
    nine = Image.new("RGBA", (W9, H9), TRANSPARENT)
    nine.paste(content, (1, 1))  # content starts at (1,1)

    px = nine.load()

    # STRETCH zones: left col and top row — cover 20 %..80 % of content size
    h_start = 1 + int(CONTENT_H * 0.20)
    h_end   = 1 + int(CONTENT_H * 0.80)
    for row in range(h_start, h_end):
        px[0, row] = BLACK                  # left edge  → vertical stretch

    w_start = 1 + int(CONTENT_W * 0.05)
    w_end   = 1 + int(CONTENT_W * 0.95)
    for col in range(w_start, w_end):
        px[col, 0] = BLACK                  # top edge   → horizontal stretch

    # CONTENT/PADDING zones: right col and bottom row
    # vertical padding: leave 10 px top/bottom inside content
    pad_top    = 1 + 10
    pad_bottom = 1 + CONTENT_H - 10
    for row in range(pad_top, pad_bottom):
        px[W9 - 1, row] = BLACK             # right edge → vertical content

    pad_left  = 1 + 14
    pad_right = 1 + CONTENT_W - 14
    for col in range(pad_left, pad_right):
        px[col, H9 - 1] = BLACK             # bottom edge → horizontal content

    # --- 3. Save -----------------------------------------------------------
    out_path = os.path.join(OUTPUT_DIR, f"banner_{slug}.9.png")
    nine.save(out_path, "PNG")
    print(f"  Created: {os.path.basename(out_path)}")


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Generating 9-patch banners → {OUTPUT_DIR}\n")
    for slug, title, subtitle, date, category, grad, text_col in EVENTS:
        make_banner(slug, title, subtitle, date, category, grad, text_col)
    print(f"\nDone. {len(EVENTS)} banner(s) written.")


if __name__ == "__main__":
    main()
