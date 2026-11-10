"""Kitty Talents 4.0, Phase 2: 3D block models, textures, blockstates and item models.

Run from anywhere:  python tools/asset_gen/blocks_3d.py
Deterministic (fixed seeds): re-running produces identical files.

Every block is modelled facing NORTH (its front toward -Z); blockstates rotate it with the FACING property.
Element UVs are left out on purpose: Minecraft projects each face onto its 16x16 texture from the element's
position, so small parts pick up the matching texture detail automatically.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "kittytalents")
TEX_DIR = os.path.join(ASSETS, "textures", "block")
MODEL_DIR = os.path.join(ASSETS, "models", "block")
ITEM_DIR = os.path.join(ASSETS, "models", "item")
STATE_DIR = os.path.join(ASSETS, "blockstates")
NS = "kittytalents"


# ============================================================================================ textures
def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, f):
    return tuple(clamp(x * f) for x in c[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def new(base, seed, var=0.06):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            f = 1.0 + rnd.uniform(-var, var)
            im.putpixel((x, y), shade(base, f) + (255,))
    return im, rnd


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def paw(im, cx, cy, c):
    """A 5x5 paw print: pad + four toes."""
    for dx, dy in [(-1, 1), (0, 1), (1, 1), (-1, 2), (0, 2), (1, 2), (0, 0)]:
        put(im, cx + dx, cy + dy, c)
    for dx, dy in [(-2, -1), (-1, -2), (1, -2), (2, -1)]:
        put(im, cx + dx, cy + dy, c)


def tex_fabric(base, seed, stitch=None):
    im, rnd = new(base, seed, 0.03)
    for y in range(16):
        for x in range(16):
            if (x + y) % 2 == 0:
                put(im, x, y, shade(im.getpixel((x, y)), 1.05))
    if stitch:
        for i in range(1, 15, 2):
            for p in [(i, 1), (i, 14), (1, i), (14, i)]:
                put(im, p[0], p[1], stitch)
    return im


def tex_plush(base, seed):
    im, rnd = new(base, seed, 0.05)
    for _ in range(26):
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(im, x, y, shade(base, 1.12))
    return im


def tex_planks(base, seed, vertical=False):
    im, rnd = new(base, seed, 0.04)
    for y in range(16):
        for x in range(16):
            u, v = (y, x) if vertical else (x, y)
            if v % 4 == 3:
                put(im, x, y, shade(base, 0.72))
            elif rnd.random() < 0.12:
                put(im, x, y, shade(base, 0.88))
    for row in range(4):
        cut = (row * 7 + 3) % 16
        for k in range(3):
            x, y = (row * 4 + k, cut) if vertical else (cut, row * 4 + k)
            put(im, x, y, shade(base, 0.75))
    return im


def tex_sisal(seed):
    base = (196, 164, 112)
    im, rnd = new(base, seed, 0.03)
    for y in range(16):
        for x in range(16):
            band = (y + (x // 4)) % 3
            f = [1.08, 0.96, 0.84][band]
            put(im, x, y, shade(base, f + rnd.uniform(-0.03, 0.03)))
    return im


def tex_carpet(base, seed):
    im, rnd = new(base, seed, 0.08)
    for _ in range(40):
        put(im, rnd.randrange(16), rnd.randrange(16), shade(base, rnd.choice([0.8, 1.15])))
    return im


def tex_ceramic(base, seed, rim=None, decal=None):
    im, rnd = new(base, seed, 0.015)
    for y in range(16):
        for x in range(16):
            put(im, x, y, shade(im.getpixel((x, y)), 1.04 - y * 0.008))
    if rim:
        for x in range(16):
            put(im, x, 1, rim)
            put(im, x, 2, rim)
    if decal:
        paw(im, 8, 8, decal)
    return im


def tex_metal(base, seed):
    im, rnd = new(base, seed, 0.02)
    for x in range(16):
        put(im, x, 0, shade(base, 1.25))
        put(im, x, 15, shade(base, 0.7))
    for _ in range(6):
        put(im, rnd.randrange(16), rnd.randrange(1, 15), shade(base, 1.15))
    return im


def tex_brick(seed):
    base, mortar = (168, 82, 62), (196, 186, 170)
    im, rnd = new(base, seed, 0.07)
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                put(im, x, y, shade(mortar, 1 + rnd.uniform(-0.04, 0.04)))
    return im


def tex_stone(base, seed):
    im, rnd = new(base, seed, 0.09)
    for _ in range(10):
        x, y = rnd.randrange(15), rnd.randrange(15)
        put(im, x, y, shade(base, 0.75))
        put(im, x + 1, y, shade(base, 0.82))
    return im


def tex_shingles(base, seed):
    im, rnd = new(base, seed, 0.05)
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 2 if row % 2 else 0
            if y % 4 == 3:
                put(im, x, y, shade(base, 0.62))
            elif (x + off) % 4 == 0:
                put(im, x, y, shade(base, 0.78))
            elif y % 4 == 0:
                put(im, x, y, shade(base, 1.12))
    return im


def tex_water(seed):
    im, rnd = new((70, 140, 205), seed, 0.03)
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 9 == 0:
                put(im, x, y, (160, 210, 245))
    return im


def tex_kibble(seed):
    im, rnd = new((120, 72, 40), seed, 0.05)
    for _ in range(34):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = rnd.choice([(150, 92, 52), (96, 58, 32), (176, 118, 70), (140, 60, 50)])
        put(im, x, y, c)
        put(im, x + 1, y, shade(c, 0.85))
    return im


def tex_rice(seed):
    im, rnd = new((236, 230, 212), seed, 0.04)
    for _ in range(30):
        put(im, rnd.randrange(16), rnd.randrange(16), (250, 248, 238))
    return im


def tex_wicker(seed):
    base = (186, 140, 82)
    im, rnd = new(base, seed, 0.04)
    for y in range(16):
        for x in range(16):
            over = ((x // 2) + (y // 2)) % 2 == 0
            put(im, x, y, shade(base, 1.1 if over else 0.8))
    return im


def tex_blanket(seed):
    im, rnd = new((236, 160, 182), seed, 0.03)
    for y in range(16):
        for x in range(16):
            if (x // 4 + y // 4) % 2 == 0:
                put(im, x, y, (246, 206, 218))
    return im


def tex_solid(c, seed, var=0.04):
    im, rnd = new(c, seed, var)
    return im


def tex_glow(seed):
    im, rnd = new((255, 226, 140), seed, 0.03)
    for y in range(5, 11):
        for x in range(5, 11):
            put(im, x, y, (255, 246, 210))
    return im


def tex_yarn(seed):
    base = (206, 52, 62)
    im, rnd = new(base, seed, 0.03)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 5 == 0:
                put(im, x, y, shade(base, 1.25))
    return im


def tex_paper(seed):
    im, rnd = new((238, 232, 214), seed, 0.02)
    for y in range(3, 15, 2):
        for x in range(2, 14):
            put(im, x, y, (150, 160, 190))
    paw(im, 11, 4, (200, 90, 90))
    return im


def tex_oven_front(seed):
    im = tex_brick(seed)
    for y in range(5, 16):
        for x in range(4, 12):
            dx = x - 7.5
            if y >= 9 or dx * dx + (y - 9) ** 2 <= 16:
                glow = 1.0 - (16 - y) / 16 * 0.4
                put(im, x, y, (clamp(255 * glow), clamp(120 * glow), 30))
    for x in range(4, 12):
        put(im, x, 15, (60, 40, 30))
    return im


def tex_plaque(seed, base=(150, 100, 60), mark=(250, 230, 200), fish=False):
    im, rnd = new(base, seed, 0.03)
    for x in range(16):
        put(im, x, 0, shade(base, 0.7))
        put(im, x, 15, shade(base, 0.7))
    if fish:
        for x in range(4, 11):
            for y in range(6, 10):
                if abs(y - 7.5) <= 2.0 - abs(x - 7.0) * 0.25:
                    put(im, x, y, mark)
        for p in [(11, 6), (11, 9), (12, 5), (12, 10), (11, 7), (11, 8)]:
            put(im, p[0], p[1], mark)
        put(im, 5, 7, shade(base, 0.6))
    else:
        paw(im, 8, 8, mark)
    return im


def tex_window(seed):
    im = tex_planks((156, 108, 64), seed)
    for y in range(4, 12):
        for x in range(4, 12):
            put(im, x, y, (150, 196, 220) if (x + y) % 5 else (220, 240, 250))
    for i in range(4, 12):
        put(im, 7, i, (110, 74, 44))
        put(im, i, 7, (110, 74, 44))
    return im


TEXTURES = {
    "bed_fabric": lambda: tex_fabric((168, 58, 60), 11, stitch=(236, 200, 160)),
    "bed_cushion": lambda: tex_plush((242, 226, 200), 12),
    "plush_fish": lambda: tex_solid((96, 150, 196), 13),
    "plush_fish_fin": lambda: tex_solid((236, 160, 70), 14),
    "ceramic": lambda: tex_ceramic((236, 236, 230), 21, rim=(80, 130, 196)),
    "ceramic_paw": lambda: tex_ceramic((236, 236, 230), 22, rim=(80, 130, 196), decal=(80, 130, 196)),
    "ceramic_inner": lambda: tex_ceramic((214, 214, 208), 23),
    "ear_pink": lambda: tex_solid((236, 150, 170), 24),
    "kibble": lambda: tex_kibble(25),
    "sisal": lambda: tex_sisal(31),
    "carpet": lambda: tex_carpet((150, 112, 168), 32),
    "carpet_light": lambda: tex_carpet((214, 196, 226), 33),
    "yarn": lambda: tex_yarn(34),
    "string": lambda: tex_solid((236, 228, 214), 35),
    "tub_wood": lambda: tex_planks((168, 116, 68), 41, vertical=True),
    "metal_band": lambda: tex_metal((150, 156, 166), 42),
    "water": lambda: tex_water(43),
    "bubble": lambda: tex_solid((236, 246, 252), 44, 0.01),
    "duck": lambda: tex_solid((248, 214, 60), 45),
    "beak": lambda: tex_solid((236, 128, 40), 46),
    "mill_stone": lambda: tex_stone((150, 146, 140), 51),
    "mill_base": lambda: tex_stone((112, 108, 104), 52),
    "rice": lambda: tex_rice(53),
    "wood_light": lambda: tex_planks((188, 146, 96), 54),
    "wood_dark": lambda: tex_planks((108, 74, 46), 55),
    "wood_mid": lambda: tex_planks((150, 104, 62), 56),
    "roof": lambda: tex_shingles((172, 74, 62), 61),
    "house_window": lambda: tex_window(62),
    "plaque_paw": lambda: tex_plaque(63),
    "plaque_fish": lambda: tex_plaque(64, base=(60, 110, 160), fish=True),
    "wicker": lambda: tex_wicker(71),
    "blanket": lambda: tex_blanket(72),
    "lamp_shade": lambda: tex_metal((86, 130, 120), 73),
    "bulb": lambda: tex_glow(74),
    "paper": lambda: tex_paper(81),
    "toolbox": lambda: tex_metal((200, 58, 52), 82),
    "brick": lambda: tex_brick(91),
    "oven_front": lambda: tex_oven_front(92),
    "oven_stone": lambda: tex_stone((128, 124, 118), 93),
    "feeder_window": lambda: tex_kibble(101),
    "glass_frame": lambda: tex_solid((210, 226, 232), 102, 0.02),
}


# ============================================================================================ model helpers
FACE_NAMES = ["down", "up", "north", "south", "west", "east"]


def box(frm, to, tex, faces=None, skip=(), shade=True, rot=None):
    """One element. tex: default texture key for all faces; faces: {face: key} overrides; skip: faces to omit."""
    faces = faces or {}
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for f in FACE_NAMES:
        if f in skip:
            continue
        el["faces"][f] = {"texture": "#" + faces.get(f, tex)}
    if not shade:
        el["shade"] = False
    if rot:
        el["rotation"] = rot
    return el


def ring(x0, z0, x1, z1, y0, y1, t, tex, faces=None, gap_north=None):
    """Four walls of thickness t around a rectangle (outer bounds), optional gap (x range) in the north wall."""
    out = []
    if gap_north:
        g0, g1 = gap_north
        if g0 > x0:
            out.append(box((x0, y0, z0), (g0, y1, z0 + t), tex, faces))
        if g1 < x1:
            out.append(box((g1, y0, z0), (x1, y1, z0 + t), tex, faces))
    else:
        out.append(box((x0, y0, z0), (x1, y1, z0 + t), tex, faces))
    out.append(box((x0, y0, z1 - t), (x1, y1, z1), tex, faces))
    out.append(box((x0, y0, z0 + t), (x0 + t, y1, z1 - t), tex, faces))
    out.append(box((x1 - t, y0, z0 + t), (x1, y1, z1 - t), tex, faces))
    return out


def octa_ring(c0, c1, y0, y1, t, ch, tex, faces=None):
    """A rounded ring: four straight walls with chamfered corners (ch) for a round, soft look."""
    out = [
        box((c0 + ch, y0, c0), (c1 - ch, y1, c0 + t), tex, faces),
        box((c0 + ch, y0, c1 - t), (c1 - ch, y1, c1), tex, faces),
        box((c0, y0, c0 + ch), (c0 + t, y1, c1 - ch), tex, faces),
        box((c1 - t, y0, c0 + ch), (c1, y1, c1 - ch), tex, faces),
    ]
    for (x, z) in [(c0, c0), (c1, c0), (c0, c1), (c1, c1)]:
        px = x + (ch * 0.5 if x == c0 else -ch * 0.5)
        pz = z + (ch * 0.5 if z == c0 else -ch * 0.5)
        h = t * 0.75
        out.append(box((px - h, y0, pz - h), (px + h, y1, pz + h), tex, faces,
                       rot={"origin": [px, y0, pz], "axis": "y", "angle": 45}))
    return out


def model(textures, elements, parent="block/block", display=None):
    m = {"parent": parent, "textures": dict({"particle": "%s:block/%s" % (NS, textures[0])},
                                             **{k: "%s:block/%s" % (NS, k) for k in textures})}
    m["elements"] = elements
    if display:
        m["display"] = display
    return m


def used_textures(elements):
    keys = []
    for el in elements:
        for f in el["faces"].values():
            k = f["texture"][1:]
            if k not in keys:
                keys.append(k)
    return keys


def mk(elements, display=None):
    return model(used_textures(elements), elements, display=display)


# ============================================================================================ blocks
def food_bowl(level):
    els = [box((4, 0, 4), (12, 1, 12), "ceramic")]
    els += octa_ring(3, 13, 1, 4, 1, 1.5, "ceramic", {"up": "ceramic_inner"})
    # paw decal on the front (north) wall
    els[1]["faces"]["north"] = {"texture": "#ceramic_paw"}
    els += octa_ring(2.5, 13.5, 3.5, 4.5, 1, 1.8, "ceramic", {"down": "ceramic_inner"})
    # cat ears on the back rim
    for x0 in (4.5, 9.5):
        els.append(box((x0, 4.5, 12), (x0 + 2, 5.5, 13), "ceramic", {"north": "ear_pink"}))
        els.append(box((x0 + 0.5, 5.5, 12), (x0 + 1.5, 6.5, 13), "ceramic", {"north": "ear_pink"}))
    els.append(box((4, 1, 4), (12, 1.2, 12), "ceramic_inner", skip=("down",)))
    if level > 0:
        top = [0, 2.0, 2.8, 3.6][level]
        els.append(box((4, 1.2, 4), (12, top, 12), "kibble", skip=("down",)))
        # a few loose kibbles on top
        els.append(box((6, top, 6), (7, top + 0.6, 7), "kibble"))
        els.append(box((9, top, 8), (10, top + 0.6, 9), "kibble"))
    return mk(els)


def cat_bed():
    els = [box((1, 0, 1), (15, 2, 15), "bed_fabric")]
    els.append(box((3, 2, 3), (13, 3, 13), "bed_cushion", skip=("down",)))
    # rounded rim, lower at the front so the cat can step in
    els += [box((3, 2, 13), (13, 6, 15), "bed_fabric"),
            box((1, 2, 3), (3, 6, 13), "bed_fabric"),
            box((13, 2, 3), (15, 6, 13), "bed_fabric"),
            box((3, 2, 1), (5, 6, 3), "bed_fabric"),
            box((11, 2, 1), (13, 6, 3), "bed_fabric"),
            box((5, 2, 1), (11, 4, 3), "bed_fabric")]
    for (x, z) in [(2, 2), (14, 2), (2, 14), (14, 14)]:
        els.append(box((x - 1.3, 2, z - 1.3), (x + 1.3, 5.5, z + 1.3), "bed_fabric",
                       rot={"origin": [x, 2, z], "axis": "y", "angle": 45}))
    # soft roll on top of the rim
    els.append(box((3, 6, 13.2), (13, 6.6, 14.8), "bed_cushion"))
    els.append(box((1.2, 6, 3), (2.8, 6.6, 13), "bed_cushion"))
    els.append(box((13.2, 6, 3), (14.8, 6.6, 13), "bed_cushion"))
    # plush fish toy
    els.append(box((8, 3, 8), (11.5, 4.6, 10), "plush_fish"))
    els.append(box((11.5, 3.2, 8.4), (12.8, 4.4, 9.6), "plush_fish_fin"))
    els.append(box((9, 4.6, 8.6), (10.5, 5.1, 9.4), "plush_fish_fin"))
    return mk(els)


def cat_scratcher():
    els = [box((1, 0, 1), (15, 2, 15), "carpet")]
    els.append(box((6, 2, 6), (10, 13, 10), "sisal"))
    els.append(box((3, 13, 3), (13, 15, 13), "carpet"))
    els.append(box((4, 15, 4), (12, 15.6, 12), "carpet_light", skip=("down",)))
    # toy on a string hanging from the platform edge
    els.append(box((11.75, 8.5, 7.75), (12.25, 13, 8.25), "string"))
    els.append(box((11, 7, 7.25), (13, 9, 9.25), "yarn"))
    els.append(box((4, 2, 4), (6.5, 2.8, 6.5), "yarn"))
    return mk(els)


def cat_bath():
    els = [box((3, 0, 3), (13, 1, 13), "tub_wood")]
    els += octa_ring(2, 14, 0, 8, 1, 2, "tub_wood")
    for y in (1.5, 6):
        els += octa_ring(1.8, 14.2, y, y + 0.8, 0.3, 2.1, "metal_band")
    els.append(box((3, 6, 3), (13, 6.5, 13), "water", skip=("down",), shade=False))
    for (x, z, s) in [(5, 5, 1), (9.5, 8, 1.4), (7, 10.5, 0.9), (4.5, 10, 0.7)]:
        els.append(box((x, 6.5, z), (x + s, 6.5 + s * 0.6, z + s), "bubble", shade=False))
    els.append(box((9, 6.3, 3.8), (11.6, 7.8, 6.4), "duck"))
    els.append(box((10, 7.8, 4.2), (11.6, 9.3, 5.8), "duck"))
    els.append(box((11.6, 8.3, 4.7), (12.4, 8.8, 5.3), "beak"))
    return mk(els)


def cat_rice_mill():
    els = [box((1, 0, 1), (15, 3, 15), "mill_base")]
    els.append(box((4, 3, 4), (12, 6, 12), "mill_stone"))
    els.append(box((4.5, 6, 4.5), (11.5, 9, 11.5), "mill_stone"))
    els.append(box((6.5, 9, 6.5), (9.5, 11, 9.5), "wood_light"))
    els.append(box((7, 10.8, 7), (9, 11.1, 9), "rice", skip=("down",)))
    els.append(box((7, 4.5, 3), (9, 5.5, 4), "wood_light"))
    els.append(box((5.5, 3, 1.5), (10.5, 4.5, 3.5), "ceramic"))
    els.append(box((6, 4.3, 2), (10, 4.6, 3), "rice", skip=("down",)))
    els.append(box((11.5, 7, 7.5), (14.5, 8, 8.5), "wood_dark"))
    els.append(box((13.5, 8, 7.5), (14.5, 10.5, 8.5), "wood_dark"))
    return mk(els)


def kitty_habitat():
    els = [box((1, 0, 1), (15, 1, 15), "wood_dark")]
    side = {"west": "house_window", "east": "house_window"}
    els.append(box((1, 1, 14), (15, 10, 15), "wood_mid"))
    els.append(box((1, 1, 2), (2, 10, 14), "wood_mid", side))
    els.append(box((14, 1, 2), (15, 10, 14), "wood_mid", side))
    # front wall with an arched door
    els.append(box((1, 1, 1), (6, 10, 2), "wood_mid"))
    els.append(box((10, 1, 1), (15, 10, 2), "wood_mid"))
    els.append(box((6, 7, 1), (10, 10, 2), "wood_mid"))
    els.append(box((6, 6, 1), (7, 7, 2), "wood_mid"))
    els.append(box((9, 6, 1), (10, 7, 2), "wood_mid"))
    els.append(box((2, 1, 2), (14, 1.6, 14), "blanket", skip=("down",)))
    els.append(box((6.5, 8, 0.5), (9.5, 9.6, 1), "plaque_paw"))
    # stepped pitched roof
    for i, (x0, x1) in enumerate([(0, 16), (1, 15), (3, 13), (5, 11), (6.5, 9.5)]):
        els.append(box((x0, 10 + i, 0), (x1, 11 + i, 16), "roof"))
    # cat ears on the roof ridge at the front
    els.append(box((4.6, 13, 0.4), (6.1, 15, 1.6), "roof", {"north": "ear_pink"}))
    els.append(box((9.9, 13, 0.4), (11.4, 15, 1.6), "roof", {"north": "ear_pink"}))
    return mk(els)


def feeding_station():
    els = [box((1, 0, 2), (15, 1.5, 14), "wood_light")]
    els.append(box((4, 1.5, 8), (12, 13, 13), "wood_mid", {"north": "glass_frame"}))
    els.append(box((5, 4, 7.8), (11, 11.5, 8), "feeder_window", shade=False))
    els.append(box((3.5, 13, 7.5), (12.5, 14, 13.5), "wood_dark"))
    els.append(box((6.5, 14, 9.5), (9.5, 14.6, 11.5), "wood_dark"))
    els.append(box((6.5, 1.5, 6), (9.5, 3.5, 8), "wood_mid"))
    els += ring(4, 2.5, 12, 6.5, 1.5, 3.5, 0.75, "ceramic")
    els.append(box((4.75, 1.5, 3.25), (11.25, 3, 5.75), "kibble", skip=("down",)))
    els.append(box((6, 12, 7.6), (10, 12.8, 8), "plaque_paw"))
    return mk(els)


def kitten_incubator():
    els = [box((3, 0, 4), (13, 1, 12), "wicker")]
    els += ring(2, 3, 14, 13, 1, 5, 1, "wicker")
    els.append(box((3, 1, 4), (13, 3.5, 12), "blanket", skip=("down",)))
    els.append(box((3.5, 3.5, 4.5), (6, 4.5, 7), "blanket"))
    # carry handle arching over the basket
    els.append(box((2.4, 5, 7.5), (3.4, 12, 8.5), "wood_dark"))
    els.append(box((12.6, 5, 7.5), (13.6, 12, 8.5), "wood_dark"))
    els.append(box((2.4, 12, 7.5), (13.6, 13, 8.5), "wood_dark"))
    # little warming lamp hanging from the handle
    els.append(box((7.75, 10.5, 7.75), (8.25, 12, 8.25), "metal_band"))
    els.append(box((6.5, 9, 6.5), (9.5, 10.5, 9.5), "lamp_shade"))
    els.append(box((7.2, 8.3, 7.2), (8.8, 9, 8.8), "bulb", shade=False))
    return mk(els)


def kitty_work_station():
    els = [box((1, 10, 1), (15, 12, 15), "wood_light")]
    for (x, z) in [(1.5, 1.5), (13, 1.5), (1.5, 13), (13, 13)]:
        els.append(box((x, 0, z), (x + 1.5, 10, z + 1.5), "wood_dark"))
    els.append(box((2, 3, 2), (14, 4, 14), "wood_mid"))
    els.append(box((3, 4, 9), (6, 6.5, 13), "yarn"))
    els.append(box((8, 4, 9), (12, 5, 13), "kibble"))
    els.append(box((3, 12, 3), (8, 12.4, 9.5), "paper", skip=("down",)))
    els.append(box((4.5, 12.4, 2.6), (6.5, 13, 3.6), "metal_band"))
    els.append(box((9, 12, 9), (14, 14.5, 13), "toolbox"))
    els.append(box((10.5, 14.5, 10.75), (12.5, 15, 11.25), "metal_band"))
    els.append(box((6, 7.5, 0.7), (10, 9.5, 1.2), "plaque_paw"))
    return mk(els)


def treat_oven():
    els = [box((1, 0, 1), (15, 3, 15), "oven_stone")]
    els.append(box((2, 3, 2), (14, 11, 14), "brick", {"north": "oven_front"}))
    els.append(box((3, 11, 3), (13, 13, 13), "brick"))
    els.append(box((5, 13, 5), (11, 14, 11), "brick"))
    els.append(box((10, 13, 10), (12, 16, 12), "oven_stone"))
    els.append(box((5.5, 11.3, 1.5), (10.5, 12.9, 2), "plaque_fish"))
    els.append(box((6, 2.5, 1.2), (10, 3, 2), "metal_band"))
    return mk(els)


def cat_tree_lower():
    els = [box((0.5, 0, 0.5), (15.5, 2, 15.5), "carpet")]
    els.append(box((6, 2, 6), (10, 16, 10), "sisal"))
    # side platform with a small cubby
    els.append(box((9, 7, 1.5), (15, 8.5, 9.5), "carpet"))
    els.append(box((10, 2, 2), (14.5, 7, 7), "carpet_light", {"north": "plaque_paw"}))
    els.append(box((11.5, 3, 1.9), (13, 5.5, 2), "wood_dark"))
    els.append(box((3.75, 9, 7.75), (4.25, 14, 8.25), "string"))
    els.append(box((3, 7.5, 7), (5, 9.5, 9), "yarn"))
    els.append(box((3.5, 14, 7.5), (6, 14.5, 8.5), "carpet"))
    return mk(els)


def cat_tree_upper():
    els = [box((6, 0, 6), (10, 8, 10), "sisal")]
    els.append(box((2, 8, 2), (14, 10, 14), "carpet"))
    els += ring(2, 2, 14, 14, 10, 11.5, 1, "carpet_light")
    els.append(box((3, 10, 3), (13, 10.6, 13), "bed_cushion", skip=("down",)))
    # ears on the front of the perch
    els.append(box((3.5, 11.5, 2), (5, 13, 3), "carpet_light", {"north": "ear_pink"}))
    els.append(box((11, 11.5, 2), (12.5, 13, 3), "carpet_light", {"north": "ear_pink"}))
    return mk(els)


def cat_tree_item():
    lower = cat_tree_lower()["elements"]
    upper = []
    for el in cat_tree_upper()["elements"]:
        e = json.loads(json.dumps(el))
        e["from"][1] += 16
        e["to"][1] += 16
        if "rotation" in e:
            e["rotation"]["origin"][1] += 16
        upper.append(e)
    els = lower + upper
    display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.38, 0.38, 0.38]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.35, 0.35, 0.35]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    }
    return mk(els, display)


# ============================================================================================ writing
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def facing_state(model_name, extra=None):
    """Blockstate with facing variants (and optional extra property values -> model suffix)."""
    variants = {}
    extras = extra or [("", "")]
    for prop, suffix in extras:
        for facing, y in FACING_Y.items():
            key = "facing=%s" % facing + ("," + prop if prop else "")
            v = {"model": "%s:block/%s%s" % (NS, model_name, suffix)}
            if y:
                v["y"] = y
            variants[key] = v
    return {"variants": variants}


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    for name, fn in TEXTURES.items():
        fn().save(os.path.join(TEX_DIR, name + ".png"))

    simple = {
        "cat_bed": cat_bed, "cat_scratcher": cat_scratcher, "cat_bath": cat_bath, "cat_rice_mill": cat_rice_mill,
        "kitty_habitat": kitty_habitat, "feeding_station": feeding_station, "kitten_incubator": kitten_incubator,
        "kitty_work_station": kitty_work_station, "treat_oven": treat_oven,
    }
    for name, fn in simple.items():
        write_json(os.path.join(MODEL_DIR, name + ".json"), fn())
        write_json(os.path.join(STATE_DIR, name + ".json"), facing_state(name))
        write_json(os.path.join(ITEM_DIR, name + ".json"), {"parent": "%s:block/%s" % (NS, name)})

    # Food bowl: four fill levels
    for lvl in range(4):
        write_json(os.path.join(MODEL_DIR, "cat_food_bowl%s.json" % ("" if lvl == 0 else "_food%d" % lvl)), food_bowl(lvl))
    write_json(os.path.join(STATE_DIR, "cat_food_bowl.json"),
               facing_state("cat_food_bowl", [("food=0", "")] + [("food=%d" % i, "_food%d" % i) for i in (1, 2, 3)]))
    write_json(os.path.join(ITEM_DIR, "cat_food_bowl.json"), {"parent": "%s:block/cat_food_bowl_food2" % NS})

    # Cat tree: two halves
    write_json(os.path.join(MODEL_DIR, "cat_tree_lower.json"), cat_tree_lower())
    write_json(os.path.join(MODEL_DIR, "cat_tree_upper.json"), cat_tree_upper())
    write_json(os.path.join(STATE_DIR, "cat_tree.json"),
               facing_state("cat_tree", [("half=lower", "_lower"), ("half=upper", "_upper")]))
    write_json(os.path.join(ITEM_DIR, "cat_tree.json"), cat_tree_item())
    print("wrote %d textures and the block models" % len(TEXTURES))


if __name__ == "__main__":
    main()
