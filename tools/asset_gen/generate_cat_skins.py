import io
import os
import zipfile
import colorsys
from PIL import Image

JAR = r"C:\Users\VivianV\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client-extra.jar"
OUT = "src/main/resources/assets/kittytalents/textures/entity/cat"
os.makedirs(OUT, exist_ok=True)


def load(name):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/entity/cat/{name}.png"))).convert("RGBA")


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(stops, t):
    # stops: list of (pos, rgb)
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        if t <= p1:
            return lerp(c0, c1, (t - p0) / max(1e-6, p1 - p0))
    return stops[-1][1]


def is_eye(x, y, r, g, b):
    # Eyes live in the head UV area; preserve saturated green/yellow pixels there.
    if x >= 32 or y >= 16:
        return False
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s > 0.45 and 0.12 < h < 0.45 and v > 0.4


def recolor(base, stops, eye=None, gamma=1.0):
    im = base.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if is_eye(x, y, r, g, b):
                if eye:
                    px[x, y] = (*eye, a)
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            lum = max(0.0, min(1.0, lum)) ** gamma
            px[x, y] = (*ramp(stops, lum), a)
    return im


tabby = load("tabby")
white = load("white")
black = load("all_black")

skins = {
    # Tiger: orange coat with dark stripes (tabby markings preserved by luminance)
    11: recolor(tabby, [(0.0, (25, 15, 8)), (0.35, (140, 70, 15)), (0.7, (230, 130, 40)), (1.0, (255, 200, 120))], gamma=0.9),
    # Snow leopard: pale grey/white with dark rosettes
    12: recolor(tabby, [(0.0, (30, 32, 40)), (0.4, (140, 148, 160)), (0.75, (215, 220, 228)), (1.0, (250, 252, 255))], eye=(120, 200, 255), gamma=0.9),
    # Void: near-black with purple glow markings
    13: recolor(tabby, [(0.0, (8, 4, 14)), (0.45, (40, 15, 70)), (0.8, (120, 50, 190)), (1.0, (200, 120, 255))], eye=(230, 90, 255), gamma=1.2),
    # Golden maneki: gold coat
    14: recolor(white, [(0.0, (120, 80, 10)), (0.5, (220, 170, 40)), (0.85, (255, 215, 90)), (1.0, (255, 245, 170))], eye=(120, 40, 20)),
    # Ghost: translucent pale blue-white
    15: recolor(white, [(0.0, (120, 150, 190)), (0.6, (190, 215, 240)), (1.0, (240, 250, 255))], eye=(90, 255, 230)),
}

# Ghost gets partial transparency on body for a spectral look
g = skins[15]
gp = g.load()
for y in range(g.height):
    for x in range(g.width):
        r, gg, b, a = gp[x, y]
        if a and not is_eye(x, y, r, gg, b):
            gp[x, y] = (r, gg, b, 190)

for sid, img in skins.items():
    img.save(os.path.join(OUT, f"skin_{sid}.png"))
    print("wrote skin", sid, img.size)
