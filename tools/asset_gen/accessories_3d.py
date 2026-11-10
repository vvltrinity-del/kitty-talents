"""Kitty Talents 4.0, Phase 2b: 3D accessory models and their textures.

Run from anywhere:  python tools/asset_gen/accessories_3d.py
Writes, per accessory:
  assets/kittytalents/models/accessories/<id>.json   geometry (parts, pivots, boxes, texture offsets), loaded by
                                                      client/model/AccessoryModel at runtime
  assets/kittytalents/textures/models/accessories/<id>.png   painted to match the box layout

Coordinates are model pixels in the space of the anchor the accessory attaches to (see KTCatModel):
  HEAD_TOP  skull top at y=0, up is -Y, the face is -Z
  HEAD      the head bone (head box x -2.5..2.5, y -2..2, z -3..2), the face is -Z
  NECK      chest front under the chin; +Y toward the tail, +Z out of the back, -Z toward the belly
  BACK      top of the shoulders, same axes as NECK
Part names drive animation in TalentedCatAccessoryLayer: "sway*" (cloth: swings and lifts while walking),
"bob*" (floats), "flap_l"/"flap_r" (wings).
Deterministic: re-running gives identical files.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "kittytalents")
GEO_DIR = os.path.join(ASSETS, "models", "accessories")
TEX_DIR = os.path.join(ASSETS, "textures", "models", "accessories")


def clamp(v):
    return max(0, min(255, int(round(v))))


def sh(c, f):
    return tuple(clamp(x * f) for x in c[:3])


# ============================================================================================ materials
# Each material: (base colour, pattern). Patterns add texture detail inside every face.
MATERIALS = {
    "felt_black": ((40, 38, 46), "felt"),
    "felt_purple": ((92, 52, 138), "felt"),
    "ribbon_red": ((196, 40, 52), "satin"),
    "ribbon_gold": ((222, 176, 60), "satin"),
    "feather_white": ((244, 244, 248), "feather"),
    "feather_tip": ((206, 214, 230), "feather"),
    "gold": ((236, 190, 64), "metal"),
    "ruby": ((214, 30, 60), "gem"),
    "sapphire": ((40, 96, 220), "gem"),
    "emerald": ((40, 190, 100), "gem"),
    "velvet_red": ((150, 20, 36), "felt"),
    "fur_trim": ((248, 246, 240), "fur"),
    "halo_glow": ((255, 226, 120), "glow"),
    "brass": ((190, 140, 60), "metal"),
    "glass_amber": ((250, 180, 70), "glass"),
    "leather": ((110, 70, 40), "leather"),
    "mask_white": ((236, 236, 240), "porcelain"),
    "mask_teal": ((40, 160, 160), "satin"),
    "knit_red": ((196, 52, 52), "knit"),
    "knit_cream": ((238, 226, 198), "knit"),
    "cape_red": ((176, 30, 40), "cloth"),
    "cape_inner": ((40, 70, 160), "cloth"),
    "wing_white": ((246, 246, 250), "feather"),
    "wing_shade": ((214, 220, 236), "feather"),
    "sky_blue": ((120, 196, 246), "feather"),
    "sky_white": ((236, 248, 255), "feather"),
}

# Face regions of a Minecraft box (ModelPart.Cube): visual top/bottom in the first row, then side row.
FACE_SHADE = {"top": 1.12, "bottom": 0.72, "right": 0.9, "front": 1.0, "left": 0.9, "back": 0.84}


def face_rects(u, v, w, h, d):
    w, h, d = math.ceil(w), math.ceil(h), math.ceil(d)
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def paint_face(img, rnd, rect, mat, face, decal=None):
    x0, y0, w, h = rect
    base, pattern = MATERIALS[mat]
    f = FACE_SHADE[face]
    for yy in range(h):
        for xx in range(w):
            c = sh(base, f)
            n = rnd.uniform(-0.04, 0.04)
            if pattern == "felt":
                n += 0.05 if (xx + yy * 3) % 5 == 0 else 0
            elif pattern == "satin":
                n += 0.12 * math.sin((xx + yy) * 0.9)
            elif pattern == "feather":
                if yy % 3 == 2:
                    n -= 0.08
                if xx % 4 == 0:
                    n -= 0.04
            elif pattern == "metal":
                n += 0.22 if (xx + yy) % 4 == 0 else (-0.08 if (xx + 2 * yy) % 5 == 0 else 0)
            elif pattern == "gem":
                n += 0.35 if (xx == 0 and yy == 0) else 0.0
            elif pattern == "glow":
                n += 0.1
            elif pattern == "glass":
                n += 0.25 if xx == yy else 0
            elif pattern == "leather":
                n += 0.06 if rnd.random() < 0.15 else 0
            elif pattern == "porcelain":
                n *= 0.3
            elif pattern == "knit":
                n += -0.12 if (xx % 2 == 0 and yy % 2 == 1) else 0.04
                if yy % 4 == 3:
                    c = sh(MATERIALS["knit_cream"][0] if mat == "knit_red" else MATERIALS["knit_red"][0], f)
            elif pattern == "cloth":
                n += 0.05 if (xx * 2 + yy) % 6 == 0 else 0
            elif pattern == "fur":
                n += rnd.choice([0.0, -0.08, 0.06])
            c = sh(c, 1 + n)
            # soft darker rim on every face edge gives the box an outline
            if w > 2 and h > 2 and (xx == 0 or yy == 0 or xx == w - 1 or yy == h - 1):
                c = sh(c, 0.88)
            img.putpixel((x0 + xx, y0 + yy), c + (255,))
    if decal and face == decal[0]:
        decal[1](img, x0, y0, w, h)


def eye_holes(img, x0, y0, w, h):
    for cx in (x0 + w // 4, x0 + w - 1 - w // 4):
        for yy in range(y0 + h // 2 - 1, y0 + h // 2 + 1):
            for xx in range(cx - 1, cx + 1):
                if 0 <= xx < img.width and 0 <= yy < img.height:
                    img.putpixel((xx, yy), (20, 20, 30, 255))


def paw_emblem(img, x0, y0, w, h):
    cx, cy = x0 + w // 2, y0 + h // 2
    gold = (240, 200, 70, 255)
    for dx, dy in [(-1, 1), (0, 1), (1, 1), (-1, 2), (0, 2), (1, 2), (0, 0)]:
        img.putpixel((cx + dx, cy + dy), gold)
    for dx, dy in [(-2, -1), (-1, -2), (1, -2), (2, -1)]:
        img.putpixel((cx + dx, cy + dy), gold)


DECALS = {"eye_holes": eye_holes, "paw": paw_emblem}


# ============================================================================================ geometry
def B(frm, size, mat, inflate=0.0, decal=None):
    return {"from": list(frm), "size": list(size), "mat": mat, "inflate": inflate, "decal": decal}


def P(name, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=(), children=()):
    return {"name": name, "pivot": list(pivot), "rot": list(rot), "boxes": list(boxes), "children": list(children)}


def cross(x, y, z, w, h, d, mat, cut=0.0, inflate=0.0):
    """Two overlapping boxes forming a rounded (octagonal) block: w x d footprint with corners cut by `cut`."""
    return [B((x + cut, y, z), (w - 2 * cut, h, d), mat, inflate), B((x, y, z + cut), (w, h, d - 2 * cut), mat, inflate)]


def top_hat():
    hat = P("hat", (0, 0, -0.2), (0, 0, 7), boxes=(
        cross(-3.6, -0.6, -3.4, 7.2, 0.6, 6.4, "felt_black", cut=1.1)
        + cross(-2.3, -5.4, -2.1, 4.6, 4.8, 4.2, "felt_black", cut=0.8)
        + cross(-2.4, -1.9, -2.2, 4.8, 1.2, 4.4, "ribbon_red", cut=0.85)
        + [B((-2.9, -3.5, -0.5), (0.6, 1.9, 1.3), "feather_white"),
           B((-3.0, -4.3, -0.3), (0.5, 1.0, 0.9), "feather_tip")]))
    return {"anchor": "HEAD_TOP", "parts": [hat]}


def witch_hat():
    tip3 = P("tip3", (0, -1.7, 0.1), (-38, 0, 0), boxes=[B((-0.5, -1.6, -0.5), (1.0, 1.6, 1.0), "felt_purple")])
    tip2 = P("tip2", (0, -1.9, 0.1), (-22, 0, 0), boxes=[B((-1.0, -1.9, -1.0), (2.0, 1.9, 2.0), "felt_purple")], children=[tip3])
    tip1 = P("tip1", (0, -2.5, 0.0), (-10, 0, 0), boxes=[B((-1.55, -2.0, -1.55), (3.1, 2.0, 3.1), "felt_purple")], children=[tip2])
    hat = P("hat", (0, 0, 0.2), (-4, 0, 0), boxes=(
        cross(-4.2, -0.5, -4.0, 8.4, 0.5, 8.0, "felt_purple", cut=1.6)
        + cross(-2.3, -2.6, -2.3, 4.6, 2.1, 4.6, "felt_purple", cut=0.7)
        + cross(-2.4, -1.7, -2.4, 4.8, 1.0, 4.8, "ribbon_gold", cut=0.75)
        + [B((-0.8, -1.9, -2.7), (1.6, 1.4, 0.4), "gold")]), children=[tip1])
    return {"anchor": "HEAD_TOP", "parts": [hat]}


def crown():
    boxes = [B((-1.9, -1.0, -1.9), (3.8, 1.0, 3.8), "velvet_red"),
             B((-2.7, -1.5, -2.7), (5.4, 1.5, 0.7), "gold"), B((-2.7, -1.5, 2.0), (5.4, 1.5, 0.7), "gold"),
             B((-2.7, -1.5, -2.0), (0.7, 1.5, 4.0), "gold"), B((2.0, -1.5, -2.0), (0.7, 1.5, 4.0), "gold"),
             B((-2.75, -0.35, -2.75), (5.5, 0.5, 5.5), "fur_trim")]
    for (x, z) in [(-2.6, -2.7), (2.0, -2.7), (-2.6, 2.1), (2.0, 2.1), (-0.3, -2.7), (-0.3, 2.1), (-2.7, -0.3), (2.1, -0.3)]:
        boxes.append(B((x, -2.6, z), (0.6, 1.1, 0.6), "gold"))
    boxes += [B((-0.55, -1.25, -2.95), (1.1, 1.0, 0.35), "ruby"), B((-0.5, -1.2, 2.6), (1.0, 0.9, 0.3), "sapphire"),
              B((-2.95, -1.2, -0.5), (0.3, 0.9, 1.0), "emerald"), B((2.65, -1.2, -0.5), (0.3, 0.9, 1.0), "emerald"),
              B((-0.45, -2.1, -0.45), (0.9, 0.9, 0.9), "gold"), B((-0.2, -2.9, -0.2), (0.4, 0.8, 0.4), "gold")]
    return {"anchor": "HEAD_TOP", "parts": [P("crown", boxes=boxes)]}


def halo():
    segs = [P("seg%d" % k, (0, 0, 0), (0, k * 45, 0), boxes=[B((-1.2, -0.3, -3.0), (2.4, 0.6, 0.7), "halo_glow")]) for k in range(8)]
    return {"anchor": "HEAD_TOP", "emissive": True, "parts": [P("bob_halo", (0, -3.4, 0.4), children=segs)]}


def feline_goggles():
    boxes = []
    for x in (-2.45, 0.45):
        boxes += cross(x, -1.05, -3.75, 2.0, 2.0, 1.1, "brass", cut=0.35)
        boxes.append(B((x + 0.4, -0.65, -3.9), (1.2, 1.2, 0.2), "glass_amber"))
    boxes += [B((-0.45, -0.35, -3.6), (0.9, 0.6, 0.5), "brass"),
              B((-2.95, -0.55, -3.0), (0.5, 0.9, 5.3), "leather"), B((2.45, -0.55, -3.0), (0.5, 0.9, 5.3), "leather"),
              B((-2.95, -0.55, 2.0), (5.9, 0.9, 0.5), "leather"),
              B((-3.05, -0.35, -1.0), (0.3, 0.5, 0.5), "brass"), B((2.75, -0.35, -1.0), (0.3, 0.5, 0.5), "brass")]
    return {"anchor": "HEAD", "parts": [P("goggles", boxes=boxes)]}


def phantom_mask():
    wl = P("wing_l", (-2.6, -1.0, -3.3), (0, 28, 0), boxes=[B((-0.4, -0.9, 0), (0.4, 1.8, 1.8), "mask_white"),
                                                         B((-0.45, -1.6, 0.8), (0.4, 0.8, 1.2), "mask_teal")])
    wr = P("wing_r", (2.6, -1.0, -3.3), (0, -28, 0), boxes=[B((0, -0.9, 0), (0.4, 1.8, 1.8), "mask_white"),
                                                         B((0.05, -1.6, 0.8), (0.4, 0.8, 1.2), "mask_teal")])
    mask = P("mask", boxes=[B((-2.6, -1.95, -3.55), (5.2, 2.3, 0.6), "mask_white", decal="eye_holes"),
                            B((-0.6, 0.2, -3.95), (1.2, 1.1, 0.5), "mask_white"),
                            B((-1.7, -2.6, -3.45), (3.4, 0.7, 0.5), "mask_teal")], children=[wl, wr])
    return {"anchor": "HEAD", "parts": [mask]}


def bowtie():
    tail_l = P("sway_bow_l", (-0.4, -0.3, -1.5), (0, 0, 0), boxes=[B((-0.7, -0.25, -2.2), (0.7, 0.45, 2.2), "ribbon_red")])
    tail_r = P("sway_bow_r", (0.4, -0.3, -1.5), (0, 0, 0), boxes=[B((0.0, -0.25, -2.0), (0.7, 0.45, 2.0), "ribbon_red")])
    bow = P("bow", boxes=[B((-0.6, -0.95, -1.95), (1.2, 0.95, 1.2), "ribbon_red"),
                          B((-2.4, -0.75, -2.45), (1.85, 0.6, 2.3), "ribbon_red"), B((-3.0, -0.75, -2.15), (0.65, 0.6, 1.7), "ribbon_red"),
                          B((0.55, -0.75, -2.45), (1.85, 0.6, 2.3), "ribbon_red"), B((2.35, -0.75, -2.15), (0.65, 0.6, 1.7), "ribbon_red")],
              children=[tail_l, tail_r])
    return {"anchor": "NECK", "parts": [bow]}


def cat_scarf():
    t1 = P("sway_scarf_a", (-0.6, 0.6, -3.4), (0, 0, 0), boxes=[B((-0.75, -0.3, -3.8), (1.5, 0.6, 3.8), "knit_red")])
    t2 = P("sway_scarf_b", (0.7, 0.9, -3.3), (0, 0, 0), boxes=[B((-0.7, -0.3, -2.9), (1.4, 0.55, 2.9), "knit_red")])
    wrap = P("scarf", boxes=[B((-2.5, -0.4, 1.8), (5.0, 2.2, 1.3), "knit_red"),
                             B((-2.5, -0.4, -3.4), (5.0, 2.2, 1.3), "knit_red"),
                             B((-2.7, -0.4, -2.2), (1.0, 2.2, 4.1), "knit_red"),
                             B((1.7, -0.4, -2.2), (1.0, 2.2, 4.1), "knit_red"),
                             B((-1.1, -0.7, -3.75), (2.2, 1.6, 1.4), "knit_red")],
                children=[t1, t2])
    return {"anchor": "NECK", "parts": [wrap]}


def hero_cape():
    def seg(name, pivot, length, width, children=(), emblem=False, hem=False):
        boxes = [B((-width / 2, 0, 0), (width, length, 0.35), "cape_red", decal="paw" if emblem else None),
                 B((-width / 2 - 0.3, 0.3, -2.4), (0.35, length - 0.3, 2.4), "cape_red"),
                 B((width / 2 - 0.05, 0.3, -2.4), (0.35, length - 0.3, 2.4), "cape_red")]
        if hem:
            boxes.append(B((-width / 2 - 0.3, length - 0.45, -0.05), (width + 0.6, 0.45, 0.45), "ribbon_gold"))
        return P(name, pivot, (0, 0, 0), boxes=boxes, children=list(children))
    c3 = seg("sway_cape_3", (0, 4.4, 0), 4.2, 6.0, hem=True)
    c2 = seg("sway_cape_2", (0, 4.4, 0), 4.4, 5.6, [c3])
    c1 = seg("sway_cape_1", (0, 0.3, 0.45), 4.4, 5.3, [c2], emblem=True)
    collar = P("collar", boxes=[B((-2.7, -0.7, -0.5), (5.4, 1.1, 1.3), "cape_red"),
                                B((-2.75, -0.75, 0.55), (5.5, 0.4, 0.45), "ribbon_gold"),
                                B((-0.6, -1.15, 0.2), (1.2, 0.8, 0.6), "gold"),
                                B((-0.35, -1.3, 0.75), (0.7, 0.5, 0.2), "ruby")], children=[c1])
    return {"anchor": "BACK", "parts": [collar]}


def wings(main, shade):
    def wing(side):
        s = -1 if side == "l" else 1
        x = (lambda a, w: a if s > 0 else -a - w)
        boxes = [B((x(0, 3.5), -0.6, 0), (3.5, 2.6, 0.5), shade),
                 B((x(0.4, 5.0), 1.6, 0), (5.0, 3.2, 0.4), main),
                 B((x(1.0, 5.6), 4.4, 0), (5.6, 3.0, 0.35), main),
                 B((x(2.2, 4.8), 7.0, 0), (4.8, 2.0, 0.3), shade)]
        # Folded along the back toward the tail at rest (z rotation), tilted a little outward (y rotation).
        return P("flap_" + side, (s * 1.3, 1.0, 0.8), (-6, s * 14, s * 68), boxes=boxes)
    return {"anchor": "BACK", "parts": [wing("l"), wing("r")]}


ACCESSORIES = {
    "top_hat": top_hat, "witch_hat": witch_hat, "crown": crown, "halo": halo, "feline_goggles": feline_goggles,
    "phantom_mask": phantom_mask, "bowtie": bowtie, "cat_scarf": cat_scarf, "hero_cape": hero_cape,
    "angel_wings": lambda: wings("wing_white", "wing_shade"),
    "sky_feline_wings": lambda: wings("sky_white", "sky_blue"),
}


# ============================================================================================ packing + output
def all_boxes(parts):
    for p in parts:
        for b in p["boxes"]:
            yield b
        yield from all_boxes(p["children"])


def pack(parts, tex_w=64):
    """Shelf-pack every box's texture footprint; returns the texture height used."""
    x = y = row_h = 0
    for b in all_boxes(parts):
        w, h, d = (math.ceil(v) for v in b["size"])
        fw, fh = 2 * d + 2 * w, d + h
        if x + fw > tex_w:
            x, y, row_h = 0, y + row_h, 0
        b["uv"] = [x, y]
        x += fw
        row_h = max(row_h, fh)
    used = y + row_h
    h = 32
    while h < used:
        h *= 2
    return h


def to_json(p):
    return {"name": p["name"], "pivot": p["pivot"], "rot": p["rot"],
            "boxes": [{"uv": b["uv"], "from": b["from"], "size": b["size"], "inflate": b["inflate"]} for b in p["boxes"]],
            "children": [to_json(c) for c in p["children"]]}


def build(acc_id, spec, seed):
    tex_w = 64
    tex_h = pack(spec["parts"], tex_w)
    img = Image.new("RGBA", (tex_w, tex_h), (0, 0, 0, 0))
    rnd = random.Random(seed)
    for b in all_boxes(spec["parts"]):
        decal = (("front" if b["decal"] == "eye_holes" else "back"), DECALS[b["decal"]]) if b["decal"] else None
        for face, rect in face_rects(b["uv"][0], b["uv"][1], *b["size"]).items():
            if rect[2] > 0 and rect[3] > 0:
                paint_face(img, rnd, rect, b["mat"], face, decal)
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(GEO_DIR, exist_ok=True)
    img.save(os.path.join(TEX_DIR, acc_id + ".png"))
    out = {"texture": "kittytalents:textures/models/accessories/%s.png" % acc_id, "texture_size": [tex_w, tex_h],
           "anchor": spec["anchor"], "emissive": spec.get("emissive", False),
           "parts": [to_json(p) for p in spec["parts"]]}
    with open(os.path.join(GEO_DIR, acc_id + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1)
        f.write("\n")


def main():
    for i, (acc_id, fn) in enumerate(ACCESSORIES.items()):
        build(acc_id, fn(), 1000 + i)
    print("wrote %d accessories" % len(ACCESSORIES))


if __name__ == "__main__":
    main()
