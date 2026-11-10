"""4.0 Phase 4: icons for the new Frost / Poison / Arcane abilities (16x16, same gem style as the 3.0 icons, plus a glyph).

Skills are the 3.0 diamond gem; ultimates get a gold rim. A small white glyph names the ability:
ice lance = arrow, blizzard = snowflake, toxic cloud = puff, plague bloom = flower, arcane blink = swirl, starfall = star.
Run from the repo root:  python tools/asset_gen/ability_icons_40.py
"""
from PIL import Image

OUT = "src/main/resources/assets/kittytalents/textures/ability/"
OUTLINE = (24, 26, 36, 255)
GOLD = (240, 196, 72, 255)
WHITE = (250, 252, 255, 255)

ICONS = {
    # id: (light, mid, dark, ultimate, glyph rows)
    "ice_lance": ((190, 240, 255), (110, 200, 240), (60, 140, 200), False,
                  ["......", "....##", "...##.", "..##..", ".##...", "#....."]),
    "blizzard": ((200, 240, 255), (120, 200, 245), (60, 130, 210), True,
                 ["..#..", "#.#.#", ".###.", "#.#.#", "..#.."]),
    "toxic_cloud": ((200, 240, 120), (130, 200, 60), (80, 140, 40), False,
                    [".##...", "####.#", "######", ".####."]),
    "plague_bloom": ((220, 150, 240), (160, 90, 200), (100, 50, 150), True,
                     [".#.#.", "#####", ".###.", "#####", ".#.#."]),
    "arcane_blink": ((230, 190, 255), (170, 120, 240), (110, 70, 190), False,
                     [".###.", "#...#", "#.#.#", "#..#.", ".##.."]),
    "starfall": ((150, 120, 230), (100, 70, 190), (60, 40, 140), True,
                 ["..#..", "..#..", "#####", ".###.", "#...#"]),
}


def gem(light, mid, dark, ultimate):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    c = 7.5
    for y in range(16):
        for x in range(16):
            d = abs(x - c) + abs(y - c)
            if d <= 7.0:
                if d > 6.0:
                    px[x, y] = GOLD if ultimate else OUTLINE
                elif x < c and y < c:
                    px[x, y] = light + (255,)
                elif x >= c and y >= c:
                    px[x, y] = dark + (255,)
                else:
                    px[x, y] = mid + (255,)
    return im


def stamp(im, rows):
    h, w = len(rows), max(len(r) for r in rows)
    ox, oy = (16 - w) // 2, (16 - h) // 2
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                px[ox + x, oy + y] = WHITE


for name, (light, mid, dark, ult, glyph) in ICONS.items():
    icon = gem(light, mid, dark, ult)
    stamp(icon, glyph)
    icon.save(OUT + name + ".png")
    print("wrote", name)
