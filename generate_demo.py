"""
Generates docs/assets/demo.gif — animated Pop-It bubble demo.
Renders a 5x5 rainbow grid on a dark background, pops bubbles one by one,
then shows the celebration screen and resets.
"""

from PIL import Image, ImageDraw
import math, random

# ── Config ────────────────────────────────────────────────────────────────────
W, H       = 360, 640        # phone-ish aspect
COLS, ROWS = 5, 5
FPS        = 20              # target frame rate
BG         = (26, 26, 46)    # #1A1A2E
TOOLBAR_H  = 56
COUNTER_H  = 40
PAD        = 12

COLORS = [
    (255, 107, 157),  # pink
    (196,  77, 255),  # purple
    (77,  159, 255),  # blue
    (77,  255, 180),  # teal
    (255, 209,  77),  # yellow
    (255, 140,  77),  # orange
]

# ── Helpers ───────────────────────────────────────────────────────────────────

def lerp(a, b, t):
    return a + (b - a) * t

def lerp_color(c1, c2, t):
    return tuple(int(lerp(a, b, t)) for a, b in zip(c1, c2))

def lighten(c, f):
    return tuple(min(255, int(v + (255 - v) * f)) for v in c)

def darken(c, f):
    return tuple(max(0, int(v * (1 - f))) for v in c)

def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))

def draw_inflated(draw, cx, cy, r, color):
    # Shadow
    for i in range(6, 0, -1):
        a = max(0, 35 - i * 4)
        s = r + i * 1.5
        draw.ellipse([cx - s + 4, cy - s + 6, cx + s + 4, cy + s + 6],
                     fill=(0, 0, 0, a))

    # Gradient body — approximate with concentric circles
    for i in range(int(r), 0, -1):
        t = 1 - (i / r)
        # Blend from lighten(top-left) to darken(bottom-right)
        mid_t = max(0.0, min(1.0, t * 1.6 - 0.3))
        if mid_t < 0.5:
            c = lerp_color(lighten(color, 0.65), color, mid_t * 2)
        else:
            c = lerp_color(color, darken(color, 0.32), (mid_t - 0.5) * 2)
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=c)

    # Specular highlight
    hx, hy, hr = cx - r * 0.18, cy - r * 0.22, r * 0.42
    for i in range(int(hr), 0, -1):
        t = 1 - (i / hr)
        a = int(lerp(180, 0, t))
        draw.ellipse([hx - i, hy - i, hx + i, hy + i],
                     fill=(255, 255, 255, a))

    # Rim
    draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                 outline=darken(color, 0.15), width=max(1, int(r * 0.05)))

def draw_popped(draw, cx, cy, r, color):
    # Outer ring
    for i in range(int(r), 0, -1):
        t = i / r
        if t > 0.72:
            ring_t = (t - 0.72) / 0.28
            c = lerp_color(darken(color, 0.50), darken(color, 0.20), ring_t)
        else:
            c = darken(color, 0.70)
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=c)

    # Inner concave
    ir = r * 0.68
    for i in range(int(ir), 0, -1):
        t = i / ir
        c = lerp_color(darken(color, 0.70), darken(color, 0.50), 1 - t)
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=c)

    # Inner rim glint
    draw.ellipse([cx - ir, cy - ir, cx + ir, cy + ir],
                 outline=(255, 255, 255, 50), width=1)

def draw_frame(popped_set, scales, pop_count, total, show_celebrate=False):
    img  = Image.new("RGBA", (W, H), BG + (255,))
    draw = ImageDraw.Draw(img, "RGBA")

    # Toolbar
    draw.rectangle([0, 0, W, TOOLBAR_H], fill=(15, 52, 96))
    # App name text (approximated as a coloured bar)
    draw.text((16, 16), "🫧 PopItBubble", fill=(255, 255, 255))

    # Counter bar
    draw.rectangle([0, TOOLBAR_H, W, TOOLBAR_H + COUNTER_H], fill=(22, 33, 62))
    draw.text((16, TOOLBAR_H + 10), f"Popped:  {pop_count} / {total}",
              fill=(176, 184, 204))

    # Grid
    grid_top = TOOLBAR_H + COUNTER_H + PAD
    grid_h   = H - grid_top - PAD - 72   # leave room for FAB
    cell_w   = (W - PAD * 2) / COLS
    cell_h   = grid_h / ROWS
    base_r   = min(cell_w, cell_h) * 0.42

    for idx in range(total):
        row = idx // COLS
        col = idx % COLS
        cx  = PAD + cell_w * col + cell_w / 2
        cy  = grid_top + cell_h * row + cell_h / 2
        color = COLORS[idx % len(COLORS)]
        scale = scales.get(idx, 1.0)
        r     = base_r * scale

        if idx in popped_set:
            draw_popped(draw, cx, cy, r, color)
        else:
            draw_inflated(draw, cx, cy, r, color)

    # FAB
    fab_x, fab_y, fab_r = W - 44, H - 44, 24
    draw.ellipse([fab_x - fab_r, fab_y - fab_r, fab_x + fab_r, fab_y + fab_r],
                 fill=(255, 107, 157))
    draw.text((fab_x - 7, fab_y - 8), "↺", fill=(255, 255, 255))

    # Celebration overlay
    if show_celebrate:
        overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        od = ImageDraw.Draw(overlay, "RGBA")
        od.rectangle([0, 0, W, H], fill=(0, 0, 0, 90))
        bw, bh = 240, 80
        bx, by = (W - bw) // 2, (H - bh) // 2
        od.rounded_rectangle([bx, by, bx + bw, by + bh], radius=20,
                              fill=(15, 52, 96, 210),
                              outline=(255, 107, 157), width=2)
        od.text((bx + 28, by + 20), "🎉 All Popped! 🎉", fill=(255, 255, 255))
        img = Image.alpha_composite(img, overlay)

    return img.convert("RGB")  # GIF needs palette/RGB

# ── Animation sequence ────────────────────────────────────────────────────────

frames     = []
durations  = []   # ms per frame
TOTAL      = COLS * ROWS

# Pop order: roughly diagonal sweep for visual appeal
def pop_order():
    order = []
    for diag in range(COLS + ROWS - 1):
        for col in range(min(diag, COLS - 1), max(-1, diag - ROWS), -1):
            row = diag - col
            if 0 <= row < ROWS and 0 <= col < COLS:
                order.append(row * COLS + col)
    return order

ORDER = pop_order()

popped = set()
scales = {}

def add_frame(dur_ms, show_cel=False):
    f = draw_frame(popped, scales, len(popped), TOTAL, show_cel)
    frames.append(f)
    durations.append(dur_ms)

# 1. Hold initial state (0.8 s)
for _ in range(int(FPS * 0.8)):
    add_frame(50)

# 2. Pop bubbles one by one
for idx in ORDER:
    # Squeeze-down frames
    for step in range(5):
        t = step / 4
        scales[idx] = 1.0 - 0.40 * t        # 1.0 → 0.60
        add_frame(25)

    # Mark popped + spring-back
    popped.add(idx)
    for step in range(4):
        t = step / 3
        # overshoot: 0.60 → 0.96 → 0.88
        if t < 0.6:
            scales[idx] = 0.60 + 0.36 * (t / 0.6)
        else:
            overshoot = math.sin((t - 0.6) / 0.4 * math.pi) * 0.08
            scales[idx] = 0.96 + overshoot - 0.08
        add_frame(25)

    scales[idx] = 0.88
    # Brief pause between pops (shorter for later bubbles to feel faster)
    pause = max(1, 3 - len(popped) // 8)
    add_frame(pause * 30)

# 3. Celebrate (1.2 s)
for _ in range(int(FPS * 1.2)):
    add_frame(50, show_cel=True)

# 4. Reset + hold (1.0 s)
popped.clear()
scales.clear()
for _ in range(int(FPS * 1.0)):
    add_frame(50)

# ── Save GIF ─────────────────────────────────────────────────────────────────
out = "docs/assets/demo.gif"
frames[0].save(
    out,
    save_all=True,
    append_images=frames[1:],
    duration=durations,
    loop=0,
    optimize=True,
)
print(f"Saved {out}  ({len(frames)} frames)")
