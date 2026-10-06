"""Generates a static screenshot showing ~half the bubbles popped."""
from PIL import Image, ImageDraw
import math

W, H       = 360, 640
COLS, ROWS = 5, 5
BG         = (26, 26, 46)
TOOLBAR_H  = 56
COUNTER_H  = 40
PAD        = 12
COLORS = [
    (255, 107, 157),(196, 77, 255),(77, 159, 255),
    (77, 255, 180),(255, 209, 77),(255, 140, 77),
]

def lerp_color(c1, c2, t):
    return tuple(int(c1[i] + (c2[i]-c1[i])*t) for i in range(3))
def lighten(c, f): return tuple(min(255,int(v+(255-v)*f)) for v in c)
def darken(c, f):  return tuple(max(0,int(v*(1-f))) for v in c)

def draw_inflated(draw, cx, cy, r, color):
    for i in range(int(r)+5, 0, -1):
        if i > r:
            a = max(0, 40 - (i-int(r))*10)
            draw.ellipse([cx-i+4,cy-i+6,cx+i+4,cy+i+6], fill=(0,0,0,a))
    for i in range(int(r), 0, -1):
        t = 1-(i/r); mid_t = max(0.0,min(1.0,t*1.6-0.3))
        if mid_t < 0.5: c = lerp_color(lighten(color,0.65), color, mid_t*2)
        else:            c = lerp_color(color, darken(color,0.32), (mid_t-0.5)*2)
        draw.ellipse([cx-i,cy-i,cx+i,cy+i], fill=c)
    hx,hy,hr = cx-r*0.18, cy-r*0.22, r*0.42
    for i in range(int(hr),0,-1):
        a = int(180*(1-i/hr))
        draw.ellipse([hx-i,hy-i,hx+i,hy+i], fill=(255,255,255,a))
    draw.ellipse([cx-r,cy-r,cx+r,cy+r], outline=darken(color,0.15), width=max(1,int(r*0.05)))

def draw_popped(draw, cx, cy, r, color):
    for i in range(int(r),0,-1):
        t = i/r
        if t > 0.72: c = lerp_color(darken(color,0.50),darken(color,0.20),(t-0.72)/0.28)
        else:        c = darken(color,0.70)
        draw.ellipse([cx-i,cy-i,cx+i,cy+i], fill=c)
    ir = r*0.68
    for i in range(int(ir),0,-1):
        c = lerp_color(darken(color,0.70),darken(color,0.50),1-i/ir)
        draw.ellipse([cx-i,cy-i,cx+i,cy+i], fill=c)

POPPED = {0,1,5,6,10,11,12,15,20}  # diagonal of popped bubbles

img  = Image.new("RGBA",(W,H),BG+(255,))
draw = ImageDraw.Draw(img,"RGBA")

draw.rectangle([0,0,W,TOOLBAR_H], fill=(15,52,96))
draw.text((16,16), "🫧 PopItBubble", fill=(255,255,255))
draw.rectangle([0,TOOLBAR_H,W,TOOLBAR_H+COUNTER_H], fill=(22,33,62))
draw.text((16,TOOLBAR_H+10), f"Popped:  {len(POPPED)} / 25", fill=(176,184,204))

grid_top = TOOLBAR_H+COUNTER_H+PAD
grid_h   = H-grid_top-PAD-72
cell_w   = (W-PAD*2)/COLS
cell_h   = grid_h/ROWS
base_r   = min(cell_w,cell_h)*0.42

for idx in range(25):
    row,col = idx//COLS, idx%COLS
    cx = PAD+cell_w*col+cell_w/2
    cy = grid_top+cell_h*row+cell_h/2
    color = COLORS[idx%len(COLORS)]
    if idx in POPPED: draw_popped(draw,cx,cy,base_r*0.88,color)
    else:             draw_inflated(draw,cx,cy,base_r,color)

fab_x,fab_y,fab_r = W-44,H-44,24
draw.ellipse([fab_x-fab_r,fab_y-fab_r,fab_x+fab_r,fab_y+fab_r], fill=(255,107,157))
draw.text((fab_x-7,fab_y-8), "↺", fill=(255,255,255))

out = "docs/assets/screenshot.png"
img.convert("RGB").save(out)
print(f"Saved {out}")
