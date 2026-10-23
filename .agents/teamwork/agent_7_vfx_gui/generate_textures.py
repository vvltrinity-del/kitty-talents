import os
import numpy as np
from PIL import Image

def ensure_dir(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)

base_assets = r"C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\src\main\resources\assets\kittytalents\textures"

# ==========================================
# 1. GENERATE TIER TREAT TEXTURES (16x16)
# ==========================================
# Base fish/treat shape mask
treat_mask = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "............##..",
    "....########....",
    "...#########....",
    "...#########....",
    "....########....",
    "............##..",
    "................",
    "................",
    "................",
    "................",
    "................"
]

tier_palettes = {
    "coal_cat_treat": {
        "dark": (35, 35, 38, 255),
        "mid": (58, 58, 64, 255),
        "light": (95, 95, 105, 255)
    },
    "copper_cat_treat": {
        "dark": (145, 75, 45, 255),
        "mid": (195, 110, 65, 255),
        "light": (235, 150, 105, 255)
    },
    "iron_cat_treat": {
        "dark": (140, 140, 145, 255),
        "mid": (190, 190, 195, 255),
        "light": (240, 240, 245, 255)
    },
    "gold_cat_treat": {
        "dark": (180, 135, 25, 255),
        "mid": (245, 195, 40, 255),
        "light": (255, 235, 120, 255)
    },
    "lapis_cat_treat": {
        "dark": (20, 45, 130, 255),
        "mid": (35, 75, 185, 255),
        "light": (75, 130, 240, 255)
    },
    "redstone_cat_treat": {
        "dark": (140, 15, 15, 255),
        "mid": (215, 30, 30, 255),
        "light": (255, 85, 85, 255)
    },
    "emerald_cat_treat": {
        "dark": (15, 115, 45, 255),
        "mid": (30, 190, 75, 255),
        "light": (85, 245, 135, 255)
    },
    "diamond_cat_treat": {
        "dark": (45, 160, 175, 255),
        "mid": (75, 215, 230, 255),
        "light": (175, 250, 255, 255)
    },
    "netherite_cat_treat": {
        "dark": (45, 35, 42, 255),
        "mid": (70, 58, 65, 255),
        "light": (115, 98, 110, 255)
    }
}

for name, pal in tier_palettes.items():
    arr = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            if treat_mask[y][x] == '#':
                # Tail fins vs main body highlight
                if x >= 12:
                    arr[y, x] = pal["dark"]
                elif y == 7 and (x == 5 or x == 6):
                    arr[y, x] = pal["light"]
                elif x == 3:
                    arr[y, x] = pal["dark"]
                else:
                    arr[y, x] = pal["mid"]
    img = Image.fromarray(arr, 'RGBA')
    path = os.path.join(base_assets, "item", name + ".png")
    ensure_dir(path)
    img.save(path)
    print(f"Generated {path}")

# ==========================================
# 2. GENERATE ABILITY ICONS (16x16)
# ==========================================
ability_icons = {
    "pounce": [(180, 60, 40), (240, 120, 60)],
    "feline_hiss": [(160, 140, 40), (230, 210, 60)],
    "feline_dash": [(40, 140, 200), (90, 200, 255)],
    "fortress_guard": [(80, 90, 110), (140, 160, 190)],
    "utility_interact": [(160, 100, 50), (220, 160, 90)],
    "healing_purr": [(30, 170, 90), (80, 240, 150)],
    "shadow_strike": [(90, 30, 140), (160, 70, 220)],
    "volt_dash": [(200, 170, 20), (255, 235, 70)],
    "inferno_spin": [(210, 50, 20), (255, 140, 30)],
    "frost_nova": [(60, 160, 210), (150, 230, 255)],
    "tidal_surge": [(20, 90, 200), (60, 170, 255)],
    "venom_fang": [(100, 160, 30), (170, 230, 60)],
    "catnip_frenzy": [(220, 30, 100), (255, 90, 170)],
    "soul_flame_barrage": [(20, 150, 180), (70, 220, 240)],
    "apex_predator": [(160, 20, 20), (240, 60, 60)],
    "celestial_purr": [(210, 160, 230), (255, 230, 255)],
    "tempest_claws": [(70, 130, 210), (160, 200, 255)]
}

for name, (c_dark, c_light) in ability_icons.items():
    arr = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            # Hexagonal / Rounded Crest badge
            dx = abs(x - 7.5)
            dy = abs(y - 7.5)
            dist = dx + dy
            if dist <= 6.5:
                # Border
                if dist >= 5.5:
                    arr[y, x] = (30, 35, 45, 255)
                elif y < 8 and x < 8:
                    arr[y, x] = (*c_light, 255)
                else:
                    arr[y, x] = (*c_dark, 255)
    img = Image.fromarray(arr, 'RGBA')
    path = os.path.join(base_assets, "ability", name + ".png")
    ensure_dir(path)
    img.save(path)
    print(f"Generated {path}")

# ==========================================
# 3. GENERATE GUI ICONS (16x16)
# ==========================================
gui_icons = {
    "hp_heart": (230, 40, 40),
    "energy_bolt": (245, 180, 25),
    "ultimate_star": (220, 90, 240),
    "status_burn": (240, 80, 20),
    "status_freeze": (70, 190, 245),
    "status_shock": (250, 225, 40),
    "status_poison": (60, 190, 60),
    "status_bleed": (190, 20, 30),
    "status_marked": (170, 60, 230),
    "status_fortified": (90, 150, 210)
}

for name, rgb in gui_icons.items():
    arr = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            dist = np.hypot(x - 7.5, y - 7.5)
            if dist <= 6.0:
                if dist >= 5.0:
                    arr[y, x] = (20, 25, 35, 255)
                else:
                    arr[y, x] = (*rgb, 255)
    img = Image.fromarray(arr, 'RGBA')
    path = os.path.join(base_assets, "gui", "icons", name + ".png")
    ensure_dir(path)
    img.save(path)
    print(f"Generated {path}")

print("All textures successfully created!")
