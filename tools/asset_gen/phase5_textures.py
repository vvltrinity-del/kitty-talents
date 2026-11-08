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
    """Recoloured from the pristine source (tools/asset_gen/source/skin_12_base.png) every run, so it is repeatable."""
    src = os.path.join(ROOT, "tools", "asset_gen", "source", "skin_12_base.png")
    path = os.path.join(TEX, "entity", "cat", "skin_12.png")
    img = Image.open(src).convert("RGBA")
    px = img.load()
    w, h = img.size
    rnd = random.Random(12)

    # Smoky warm grey, not white: snow leopards are pale but never paper-white except the belly.
    FUR_DARK, FUR_LIGHT = (150, 146, 138), (204, 199, 189)
    BELLY = (232, 229, 222)
    RING, CORE, SPOT = (74, 71, 68), (165, 159, 150), (88, 85, 81)

    def protected(x, y):  # head front face (eyes, nose, muzzle) and the nose box stay as drawn
        return (5 <= x < 10 and 5 <= y < 9) or (0 <= x < 7 and 24 <= y < 28)

    def is_tail(x, y):
        return 0 <= x < 8 and 15 <= y < 24

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0 or protected(x, y):
                continue
            l = lum((r, g, b))
            col = BELLY if l > 0.9 else mix(FUR_DARK, FUR_LIGHT, min(1.0, l * 1.1))
            px[x, y] = (*col, a)

    def paintable(x, y):
        return 0 <= x < w and 0 <= y < h and px[x, y][3] > 0 and not protected(x, y) and lum(px[x, y][:3]) < 0.88

    def paint(x, y, col):
        if paintable(x, y):
            px[x, y] = (*col, px[x, y][3])

    # Open rosettes: a 3x2 broken ring (no bottom-middle pixel) around a light core, sparse and jittered.
    ring = [(-1, 0), (1, 0), (-1, -1), (0, -1), (1, -1), (-1, 1), (1, 1)]
    for gy in range(2, h, 6):
        for gx in range(2 + (gy // 6) % 2 * 3, w, 7):
            cx, cy = gx + rnd.randint(-1, 1), gy + rnd.randint(-1, 1)
            if is_tail(cx, cy) or not paintable(cx, cy):
                continue
            paint(cx, cy, CORE)
            for dx, dy in ring:
                if rnd.random() < 0.7:
                    paint(cx + dx, cy + dy, RING)
    # Small single spots fill the gaps (legs and head mostly).
    for _ in range(70):
        x, y = rnd.randrange(w), rnd.randrange(h)
        if not is_tail(x, y) and paintable(x, y) and lum(px[x, y][:3]) > 0.5:
            paint(x, y, SPOT)
    # Thick spotted tail with a dark tip.
    for x in range(0, 8):
        for y in range(15, 24):
            if (x + y) % 3 == 0 and rnd.random() < 0.8:
                paint(x, y, RING)
            if x >= 4 and y >= 22:
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
        # Leggings bottoms (they looked open from below). Hind legs: texOffs(8,13) 2x6x2 -> bottom face u12..14 v13..15,
        # transparent in every texture; fill it from the bottom row (v20) of the front side face (u10..12).
        # (Boots/front legs are fully painted already.)
        bottoms = 0
        for dy in range(2):
            for dx in range(2):
                if px[12 + dx, 13 + dy][3] == 0 and px[10 + dx, 20][3] > 0:
                    px[12 + dx, 13 + dy] = px[10 + dx, 20]
                    bottoms += 1
        img.save(path)
        print(f"  {name}: filled {filled} back-plate pixels, {bottoms} leg-bottom pixels")


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
