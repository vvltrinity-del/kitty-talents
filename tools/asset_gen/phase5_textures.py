"""Phase 5 texture pass. Run from the repo root:  python tools/asset_gen/phase5_textures.py

1. Snow Leopard (skin_12.png): pale cream-grey fur with dark rosettes and a ringed tail; face/eyes and white belly kept.
2. Cat armor (cat_armor_*.png): the body's back face (u 36..40, v 6..22) was mostly transparent, so fur showed
   through the back plates; transparent pixels there are filled from the matching side-plate column.
3. Sky Feline wings (sky_feline_wings.png): derived from the angel wings' shape, recoloured sky blue -> white
   with a few star highlights (replaces the borrowed grey vanilla elytra texture).
Deterministic (fixed seed), so re-running gives identical files.
"""
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "kittytalents", "textures")


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# ---------------------------------------------------------------------------------------------------------------
def snow_leopard():
    path = os.path.join(TEX, "entity", "cat", "skin_12.png")
    img = Image.open(path).convert("RGBA")
    px = img.load()
    w, h = img.size
    rnd = random.Random(12)

    FUR_DARK, FUR_LIGHT = (176, 172, 164), (226, 222, 214)
    BELLY = (243, 241, 236)
    RING, CORE = (78, 78, 84), (158, 152, 142)

    # Head front face (eyes, nose, muzzle) and the nose box are kept exactly as drawn.
    def protected(x, y):
        return (5 <= x < 10 and 5 <= y < 9) or (0 <= x < 7 and 24 <= y < 28)

    # 1) base recolour, keeping the original shading
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0 or protected(x, y):
                continue
            l = lum((r, g, b))
            col = BELLY if l > 0.9 else mix(FUR_DARK, FUR_LIGHT, min(1.0, l * 1.15))
            px[x, y] = (*col, a)

    def paint(x, y, col):
        if 0 <= x < w and 0 <= y < h and px[x, y][3] > 0 and not protected(x, y) and lum(px[x, y][:3]) < 0.93:
            px[x, y] = (*col, px[x, y][3])

    # 2) rosettes: a dark broken ring around a slightly darker core, on a jittered grid
    rosette = [(-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (1, 1)]  # gap at the bottom = "broken" ring
    tail_cols = set(range(0, 8))
    for gy in range(1, h, 5):
        for gx in range(1 + (gy // 5) % 2 * 3, w, 6):
            if gx in tail_cols and 15 <= gy < 24:
                continue  # tail gets rings instead
            cx, cy = gx + rnd.randint(-1, 1), gy + rnd.randint(0, 1)
            if not (0 <= cx < w and 0 <= cy < h) or px[cx, cy][3] == 0 or protected(cx, cy):
                continue
            paint(cx, cy, CORE)
            for dx, dy in rosette:
                if rnd.random() < 0.75:
                    paint(cx + dx, cy + dy, RING)

    # 3) tail (two 1x8x1 segments at u 0..4 and 4..8, v 15..24): dark rings every 3 rows, dark tip
    for x in range(0, 8):
        for y in range(15, 24):
            if (y - 15) % 3 == 1 or (x >= 4 and y >= 21):
                paint(x, y, RING)
    img.save(path)


# ---------------------------------------------------------------------------------------------------------------
def armor_backs():
    folder = os.path.join(TEX, "models", "armor")
    for name in sorted(os.listdir(folder)):
        if not name.startswith("cat_armor_") or not name.endswith(".png"):
            continue
        path = os.path.join(folder, name)
        img = Image.open(path).convert("RGBA")
        px = img.load()
        filled = 0
        for y in range(6, 22):
            for i in range(4):
                bx, sx = 36 + i, 21 + i  # back face column <- matching west-side plate column
                if px[bx, y][3] == 0 and px[sx, y][3] > 0:
                    px[bx, y] = px[sx, y]
                    filled += 1
        img.save(path)
        print(f"  {name}: filled {filled} back-plate pixels")


# ---------------------------------------------------------------------------------------------------------------
def sky_wings():
    src = os.path.join(TEX, "models", "accessories", "angel_wings.png")
    dst = os.path.join(TEX, "models", "accessories", "sky_feline_wings.png")
    img = Image.open(src).convert("RGBA")
    px = img.load()
    w, h = img.size
    rnd = random.Random(7)
    SKY, WHITE = (96, 160, 230), (236, 246, 255)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            t = min(1.0, lum((r, g, b)) * 1.1)
            col = mix(SKY, WHITE, t * 0.5 + (y / h) * 0.2)
            if rnd.random() < 0.025:
                col = (255, 250, 200)  # star glint
            px[x, y] = (*col, a)
    img.save(dst)


if __name__ == "__main__":
    snow_leopard()
    print("snow leopard repainted")
    armor_backs()
    sky_wings()
    print("sky feline wings written")
