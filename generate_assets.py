import os
import json
from PIL import Image, ImageDraw

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "kittytalents")
ITEM_TEX_DIR = os.path.join(ASSETS_DIR, "textures", "item")
ITEM_MODEL_DIR = os.path.join(ASSETS_DIR, "models", "item")
BLOCK_MODEL_DIR = os.path.join(ASSETS_DIR, "models", "block")
BLOCKSTATE_DIR = os.path.join(ASSETS_DIR, "blockstates")

os.makedirs(ITEM_TEX_DIR, exist_ok=True)
os.makedirs(ITEM_MODEL_DIR, exist_ok=True)
os.makedirs(BLOCK_MODEL_DIR, exist_ok=True)
os.makedirs(BLOCKSTATE_DIR, exist_ok=True)

# 1. Blocks: blockstate, block model, item model
blocks = {
    "cat_bath": {
        "particle": "minecraft:block/iron_block",
        "top": "minecraft:block/iron_block",
        "bottom": "minecraft:block/smooth_stone",
        "side": "minecraft:block/iron_block"
    },
    "cat_rice_mill": {
        "particle": "minecraft:block/andesite",
        "top": "minecraft:block/smooth_stone",
        "bottom": "minecraft:block/andesite",
        "side": "minecraft:block/stone_bricks"
    },
    "kitty_habitat": {
        "particle": "minecraft:block/stripped_oak_log",
        "top": "minecraft:block/oak_planks",
        "bottom": "minecraft:block/oak_planks",
        "side": "minecraft:block/stripped_oak_log"
    },
    "feeding_station": {
        "particle": "minecraft:block/spruce_planks",
        "top": "minecraft:block/spruce_planks",
        "bottom": "minecraft:block/stripped_spruce_log",
        "side": "minecraft:block/spruce_planks"
    },
    "kitten_incubator": {
        "particle": "minecraft:block/glass",
        "top": "minecraft:block/smooth_stone",
        "bottom": "minecraft:block/iron_block",
        "side": "minecraft:block/glass"
    }
}

for name, tex in blocks.items():
    # Blockstate
    bs_data = {
        "variants": {
            "": {"model": f"kittytalents:block/{name}"}
        }
    }
    with open(os.path.join(BLOCKSTATE_DIR, f"{name}.json"), "w") as f:
        json.dump(bs_data, f, indent=2)

    # Block Model
    bm_data = {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": tex
    }
    with open(os.path.join(BLOCK_MODEL_DIR, f"{name}.json"), "w") as f:
        json.dump(bm_data, f, indent=2)

    # Item Model (parent to block model)
    im_data = {
        "parent": f"kittytalents:block/{name}"
    }
    with open(os.path.join(ITEM_MODEL_DIR, f"{name}.json"), "w") as f:
        json.dump(im_data, f, indent=2)

print("Created 5 blockstates, block models, and item models.")

# 2. Items: item model & 16x16 pixel art
item_keys = [
    "crown", "halo", "angel_wings", "cat_scarf", "phantom_mask",
    "toy_mouse", "cat_ball", "toy_fish", "frisbee",
    "empty_locator_orb", "feline_locator_orb",
    "breeding_catnip", "feline_wrap", "healing_treat", "gender_ribbon",
    "rice_grain", "rice_bowl",
    "resource_catalyst", "catnip_catalyst", "ore_essence",
    "upgrade_productivity_1", "upgrade_productivity_2", "upgrade_productivity_3",
    "upgrade_productivity_4", "upgrade_productivity_5", "upgrade_productivity_6"
]

for ik in item_keys:
    im_data = {
        "parent": "item/generated",
        "textures": {
            "layer0": f"kittytalents:item/{ik}"
        }
    }
    with open(os.path.join(ITEM_MODEL_DIR, f"{ik}.json"), "w") as f:
        json.dump(im_data, f, indent=2)

print(f"Created {len(item_keys)} item model JSONs.")

def create_pixel_art(name):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    if name == "crown":
        # Golden crown with ruby jewel
        # Base band
        for x in range(3, 13):
            img.putpixel((x, 11), (255, 215, 0, 255))
            img.putpixel((x, 12), (200, 150, 0, 255))
        # Spikes
        for y in range(6, 11):
            img.putpixel((3, y), (255, 215, 0, 255))
            img.putpixel((12, y), (255, 215, 0, 255))
        for y in range(4, 11):
            img.putpixel((7, y), (255, 230, 80, 255))
            img.putpixel((8, y), (255, 230, 80, 255))
        img.putpixel((5, 8), (255, 215, 0, 255))
        img.putpixel((10, 8), (255, 215, 0, 255))
        # Ruby
        img.putpixel((7, 9), (220, 20, 60, 255))
        img.putpixel((8, 9), (255, 60, 80, 255))
    elif name == "halo":
        # Floating golden ring
        for x in range(4, 12):
            img.putpixel((x, 6), (255, 235, 120, 255))
            img.putpixel((x, 9), (230, 180, 20, 255))
        img.putpixel((3, 7), (255, 220, 50, 255))
        img.putpixel((3, 8), (255, 200, 30, 255))
        img.putpixel((12, 7), (255, 220, 50, 255))
        img.putpixel((12, 8), (255, 200, 30, 255))
        # Glow
        for x in range(5, 11):
            img.putpixel((x, 5), (255, 255, 200, 120))
            img.putpixel((x, 10), (255, 215, 0, 100))
    elif name == "angel_wings":
        # Feathered wings
        for x in range(2, 8):
            for y in range(4, 13):
                if (x + y) % 2 == 0:
                    img.putpixel((x, y), (245, 248, 255, 255))
                else:
                    img.putpixel((x, y), (200, 220, 245, 255))
        for x in range(8, 14):
            for y in range(4, 13):
                if (x + y) % 2 == 0:
                    img.putpixel((x, y), (245, 248, 255, 255))
                else:
                    img.putpixel((x, y), (200, 220, 245, 255))
    elif name == "cat_scarf":
        # Red and gold warm scarf
        for x in range(4, 12):
            img.putpixel((x, 7), (200, 30, 30, 255))
            img.putpixel((x, 8), (255, 215, 0, 255))
            img.putpixel((x, 9), (200, 30, 30, 255))
        for y in range(9, 14):
            img.putpixel((5, y), (200, 30, 30, 255))
            img.putpixel((6, y), (255, 215, 0, 255))
    elif name == "phantom_mask":
        # Violet mask with glowing cyan feline eyes
        for x in range(3, 13):
            for y in range(6, 11):
                img.putpixel((x, y), (60, 40, 95, 255))
        # Cyan eyes
        img.putpixel((5, 8), (50, 255, 220, 255))
        img.putpixel((6, 8), (30, 220, 200, 255))
        img.putpixel((9, 8), (30, 220, 200, 255))
        img.putpixel((10, 8), (50, 255, 220, 255))
    elif name == "toy_mouse":
        # Gray mouse with pink ears and tail
        for x in range(5, 11):
            for y in range(7, 12):
                img.putpixel((x, y), (140, 140, 145, 255))
        img.putpixel((4, 8), (80, 80, 85, 255)) # nose
        img.putpixel((6, 6), (255, 180, 200, 255)) # ear
        img.putpixel((8, 6), (255, 180, 200, 255)) # ear
        img.putpixel((11, 10), (240, 170, 190, 255)) # tail
        img.putpixel((12, 11), (240, 170, 190, 255))
        img.putpixel((13, 10), (240, 170, 190, 255))
    elif name == "cat_ball":
        # Red & cyan striped ball
        for x in range(5, 11):
            for y in range(5, 11):
                img.putpixel((x, y), (230, 50, 50, 255) if (x + y) % 2 == 0 else (50, 200, 240, 255))
    elif name == "toy_fish":
        # Plush blue fish
        for x in range(3, 12):
            for y in range(7, 10):
                img.putpixel((x, y), (70, 150, 240, 255))
        img.putpixel((2, 8), (40, 100, 180, 255)) # mouth
        img.putpixel((12, 6), (50, 120, 220, 255)) # tail
        img.putpixel((12, 10), (50, 120, 220, 255))
        img.putpixel((5, 7), (255, 255, 255, 255)) # eye
        img.putpixel((5, 8), (0, 0, 0, 255))
    elif name == "frisbee":
        # Orange disc
        for x in range(3, 13):
            img.putpixel((x, 8), (255, 140, 0, 255))
            img.putpixel((x, 9), (230, 90, 0, 255))
        img.putpixel((3, 8), (255, 255, 255, 255))
        img.putpixel((12, 8), (255, 255, 255, 255))
    elif name == "empty_locator_orb":
        # Brass hollow glass orb
        for x in range(5, 11):
            img.putpixel((x, 4), (180, 140, 60, 255))
            img.putpixel((x, 11), (180, 140, 60, 255))
        for y in range(5, 11):
            img.putpixel((4, y), (180, 140, 60, 255))
            img.putpixel((11, y), (180, 140, 60, 255))
        for x in range(6, 10):
            for y in range(6, 10):
                img.putpixel((x, y), (200, 230, 255, 90))
    elif name == "feline_locator_orb":
        # Pulsing glowing emerald orb with paw
        for x in range(5, 11):
            for y in range(5, 11):
                img.putpixel((x, y), (40, 220, 100, 255))
        # Inner paw print
        img.putpixel((7, 8), (20, 80, 40, 255))
        img.putpixel((8, 8), (20, 80, 40, 255))
        img.putpixel((6, 6), (20, 80, 40, 255))
        img.putpixel((7, 6), (20, 80, 40, 255))
        img.putpixel((9, 6), (20, 80, 40, 255))
    elif name == "breeding_catnip":
        # Green catnip with pink heart
        for x in range(4, 12):
            for y in range(5, 12):
                img.putpixel((x, y), (50, 170, 70, 255))
        img.putpixel((6, 7), (255, 100, 160, 255))
        img.putpixel((7, 7), (255, 100, 160, 255))
        img.putpixel((8, 7), (255, 100, 160, 255))
        img.putpixel((9, 7), (255, 100, 160, 255))
        img.putpixel((7, 8), (255, 80, 140, 255))
        img.putpixel((8, 8), (255, 80, 140, 255))
        img.putpixel((7, 9), (255, 60, 120, 255))
    elif name == "feline_wrap":
        # Bandage roll with red cross
        for x in range(4, 12):
            for y in range(5, 11):
                img.putpixel((x, y), (240, 240, 245, 255))
        img.putpixel((8, 7), (220, 30, 30, 255))
        img.putpixel((8, 8), (220, 30, 30, 255))
        img.putpixel((8, 9), (220, 30, 30, 255))
        img.putpixel((7, 8), (220, 30, 30, 255))
        img.putpixel((9, 8), (220, 30, 30, 255))
    elif name == "healing_treat":
        # Golden heart treat
        for x in range(4, 12):
            for y in range(5, 11):
                img.putpixel((x, y), (255, 180, 40, 255))
        img.putpixel((8, 8), (100, 220, 80, 255))
    elif name == "gender_ribbon":
        # Half pink, half blue ribbon bow
        for x in range(4, 8):
            for y in range(6, 11):
                img.putpixel((x, y), (100, 180, 255, 255))
        for x in range(8, 12):
            for y in range(6, 11):
                img.putpixel((x, y), (255, 140, 200, 255))
        img.putpixel((7, 8), (255, 215, 0, 255))
        img.putpixel((8, 8), (255, 215, 0, 255))
    elif name == "rice_grain":
        # Rice grains
        img.putpixel((6, 7), (250, 250, 250, 255))
        img.putpixel((7, 8), (240, 240, 245, 255))
        img.putpixel((9, 7), (250, 250, 250, 255))
        img.putpixel((10, 8), (240, 240, 245, 255))
        img.putpixel((7, 10), (250, 250, 250, 255))
        img.putpixel((8, 11), (240, 240, 245, 255))
    elif name == "rice_bowl":
        # Bowl of steaming white rice
        for x in range(4, 12):
            img.putpixel((x, 11), (120, 70, 40, 255))
            img.putpixel((x, 10), (160, 90, 50, 255))
        for x in range(5, 11):
            for y in range(7, 10):
                img.putpixel((x, y), (245, 245, 250, 255))
        img.putpixel((7, 7), (40, 50, 40, 255)) # nori flake
        img.putpixel((8, 7), (40, 50, 40, 255))
    elif name == "resource_catalyst":
        # Radiant prism
        for x in range(5, 11):
            for y in range(4, 12):
                img.putpixel((x, y), (180, 100, 255, 255) if (x+y)%2==0 else (255, 150, 240, 255))
    elif name == "catnip_catalyst":
        # Flask with bubbling green liquid
        for x in range(5, 11):
            for y in range(7, 13):
                img.putpixel((x, y), (30, 210, 80, 255))
        for x in range(6, 10):
            img.putpixel((x, 4), (200, 200, 220, 255))
            img.putpixel((x, 5), (200, 200, 220, 255))
            img.putpixel((x, 6), (200, 200, 220, 255))
    elif name == "ore_essence":
        # Shimmering multi-mineral ingot
        for x in range(4, 12):
            for y in range(6, 11):
                img.putpixel((x, y), (70, 70, 85, 255))
        img.putpixel((5, 7), (255, 215, 0, 255)) # gold
        img.putpixel((9, 7), (80, 240, 240, 255)) # diamond
        img.putpixel((6, 9), (220, 40, 40, 255)) # redstone
        img.putpixel((10, 9), (50, 90, 220, 255)) # lapis
    elif name.startswith("upgrade_productivity_"):
        tier = int(name.split("_")[-1])
        colors = {
            1: ((210, 210, 215), (160, 160, 170)), # Iron / Basic
            2: ((80, 200, 100), (40, 150, 60)),    # Green / Improved
            3: ((60, 210, 230), (30, 160, 180)),   # Aqua / Advanced
            4: ((255, 200, 30), (210, 140, 10)),   # Gold / Superior
            5: ((220, 80, 240), (160, 40, 180)),   # Amethyst / Master
            6: ((140, 20, 180), (70, 10, 100))     # Dark Dire / Dire
        }
        main_c, dark_c = colors.get(tier, ((200, 200, 200), (100, 100, 100)))
        for x in range(4, 12):
            for y in range(4, 12):
                img.putpixel((x, y), main_c + (255,))
        # Cog teeth
        img.putpixel((7, 3), dark_c + (255,))
        img.putpixel((8, 3), dark_c + (255,))
        img.putpixel((7, 12), dark_c + (255,))
        img.putpixel((8, 12), dark_c + (255,))
        img.putpixel((3, 7), dark_c + (255,))
        img.putpixel((3, 8), dark_c + (255,))
        img.putpixel((12, 7), dark_c + (255,))
        img.putpixel((12, 8), dark_c + (255,))
        # Center core
        for x in range(7, 9):
            for y in range(7, 9):
                img.putpixel((x, y), (255, 255, 255, 255) if tier <= 3 else (255, 80, 80, 255))

    out_path = os.path.join(ITEM_TEX_DIR, f"{name}.png")
    img.save(out_path)

for ik in item_keys:
    create_pixel_art(ik)

print(f"Generated {len(item_keys)} 16x16 pixel-art textures successfully!")
