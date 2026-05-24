"""Generates a wide README banner using the app's bubble aesthetic."""
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1280, 400
BG_TOP    = (22, 22, 42)
BG_BOTTOM = (15, 52, 96)
COLORS = [
    (255, 107, 157), (196, 77, 255), (77, 159, 255),
    (77, 255, 180), (255, 209, 77), (255, 140, 77),
]
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def lerp_color(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
def lighten(c, f): return tuple(min(255, int(v + (255 - v) * f)) for v in c)
def darken(c, f):  return tuple(max(0, int(v * (1 - f))) for v in c)

def draw_inflated(draw, cx, cy, r, color):
    # soft drop shadow
    for i in range(int(r) + 6, 0, -1):
        if i > r:
            a = max(0, 36 - (i - int(r)) * 8)
            draw.ellipse([cx - i + 5, cy - i + 7, cx + i + 5, cy + i + 7], fill=(0, 0, 0, a))
    # 3D dome via radial shading
    for i in range(int(r), 0, -1):
        t = 1 - (i / r); mid = max(0.0, min(1.0, t * 1.6 - 0.3))
        if mid < 0.5: c = lerp_color(lighten(color, 0.65), color, mid * 2)
        else:         c = lerp_color(color, darken(color, 0.32), (mid - 0.5) * 2)
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=c)
    # specular highlight
    hx, hy, hr = cx - r * 0.18, cy - r * 0.22, r * 0.42
    for i in range(int(hr), 0, -1):
        a = int(190 * (1 - i / hr))
        draw.ellipse([hx - i, hy - i, hx + i, hy + i], fill=(255, 255, 255, a))
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=darken(color, 0.15), width=max(1, int(r * 0.05)))

def draw_popped(draw, cx, cy, r, color):
    for i in range(int(r), 0, -1):
        t = i / r
        if t > 0.72: c = lerp_color(darken(color, 0.50), darken(color, 0.20), (t - 0.72) / 0.28)
        else:        c = darken(color, 0.70)
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=c)

# vertical gradient background
img = Image.new("RGBA", (W, H))
bg = ImageDraw.Draw(img, "RGBA")
for y in range(H):
    bg.line([(0, y), (W, y)], fill=lerp_color(BG_TOP, BG_BOTTOM, y / H))
draw = ImageDraw.Draw(img, "RGBA")

# decorative bubble field on the right third
import random
random.seed(7)
positions = [
    (980, 110, 56), (1090, 90, 44), (1180, 150, 50), (1030, 210, 60),
    (1150, 250, 46), (920, 230, 42), (1090, 300, 54), (1200, 320, 40),
    (960, 330, 44),
]
popped_idx = {2, 5, 7}
for i, (cx, cy, r) in enumerate(positions):
    color = COLORS[i % len(COLORS)]
    if i in popped_idx:
        draw_popped(draw, cx, cy, r * 0.9, color)
    else:
        draw_inflated(draw, cx, cy, r, color)

# a few small accent bubbles behind the text
for i, (cx, cy, r) in enumerate([(120, 320, 28), (260, 70, 22), (760, 330, 24)]):
    draw_inflated(draw, cx, cy, r, COLORS[(i + 1) % len(COLORS)])

# title + tagline
title_font = ImageFont.truetype(FONT, 92)
tag_font   = ImageFont.truetype(FONT, 34)
sub_font   = ImageFont.truetype(FONT, 24)

draw.text((70, 120), "PopItBubble", font=title_font, fill=(255, 255, 255))
draw.text((74, 222), "The satisfying Pop-It fidget, on Android", font=tag_font, fill=(255, 209, 77))
draw.text((74, 280), "3D bubbles  •  haptics  •  pop sounds  •  challenge mode",
          font=sub_font, fill=(176, 184, 204))

out = "docs/assets/banner.png"
img.convert("RGB").save(out)
print(f"Saved {out}  ({W}x{H})")
