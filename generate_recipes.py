import os
import json

RECIPES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "src", "main", "resources", "data", "kittytalents", "recipes")
os.makedirs(RECIPES_DIR, exist_ok=True)

recipes = {
    "cat_bath": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["I I", "IWI", "III"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "W": {"item": "minecraft:water_bucket"}
        },
        "result": {"item": "kittytalents:cat_bath"}
    },
    "cat_rice_mill": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["SPS", "SBS", "SSS"],
        "key": {
            "S": {"item": "minecraft:stone_bricks"},
            "P": {"item": "minecraft:piston"},
            "B": {"item": "minecraft:bowl"}
        },
        "result": {"item": "kittytalents:cat_rice_mill"}
    },
    "kitty_habitat": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["PPP", "PBP", "PPP"],
        "key": {
            "P": {"item": "minecraft:oak_planks"},
            "B": {"item": "kittytalents:cat_bed"}
        },
        "result": {"item": "kittytalents:kitty_habitat"}
    },
    "feeding_station": {
        "type": "minecraft:crafting_shaped",
        "pattern": [" C ", " H ", " F "],
        "key": {
            "C": {"item": "minecraft:chest"},
            "H": {"item": "minecraft:hopper"},
            "F": {"item": "kittytalents:cat_food_bowl"}
        },
        "result": {"item": "kittytalents:feeding_station"}
    },
    "kitten_incubator": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["GGG", "GRG", "SSS"],
        "key": {
            "G": {"item": "minecraft:glass"},
            "R": {"item": "minecraft:redstone"},
            "S": {"item": "minecraft:smooth_stone"}
        },
        "result": {"item": "kittytalents:kitten_incubator"}
    },
    "toy_mouse": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:iron_nugget"},
            {"item": "minecraft:string"},
            {"item": "minecraft:gray_wool"}
        ],
        "result": {"item": "kittytalents:toy_mouse"}
    },
    "cat_ball": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:slime_ball"},
            {"item": "minecraft:red_dye"},
            {"item": "minecraft:cyan_dye"}
        ],
        "result": {"item": "kittytalents:cat_ball"}
    },
    "toy_fish": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:cod"},
            {"item": "kittytalents:catnip"},
            {"item": "minecraft:string"}
        ],
        "result": {"item": "kittytalents:toy_fish"}
    },
    "frisbee": {
        "type": "minecraft:crafting_shaped",
        "pattern": [" O ", "OIO", " O "],
        "key": {
            "O": {"item": "minecraft:orange_wool"},
            "I": {"item": "minecraft:iron_nugget"}
        },
        "result": {"item": "kittytalents:frisbee"}
    },
    "empty_locator_orb": {
        "type": "minecraft:crafting_shaped",
        "pattern": [" G ", "GCG", " G "],
        "key": {
            "G": {"item": "minecraft:gold_nugget"},
            "C": {"item": "minecraft:compass"}
        },
        "result": {"item": "kittytalents:empty_locator_orb"}
    },
    "breeding_catnip": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "kittytalents:catnip"},
            {"item": "minecraft:golden_carrot"},
            {"item": "minecraft:sweet_berries"}
        ],
        "result": {"item": "kittytalents:breeding_catnip"}
    },
    "feline_wrap": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:string"},
            {"item": "minecraft:white_wool"},
            {"item": "minecraft:glistering_melon_slice"}
        ],
        "result": {"item": "kittytalents:feline_wrap"}
    },
    "healing_treat": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "kittytalents:catnip_treat"},
            {"item": "minecraft:glistering_melon_slice"}
        ],
        "result": {"item": "kittytalents:healing_treat"}
    },
    "gender_ribbon": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:pink_dye"},
            {"item": "minecraft:light_blue_dye"},
            {"item": "minecraft:string"},
            {"item": "minecraft:gold_nugget"}
        ],
        "result": {"item": "kittytalents:gender_ribbon"}
    },
    "rice_bowl": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["RRR", " B "],
        "key": {
            "R": {"item": "kittytalents:rice_grain"},
            "B": {"item": "minecraft:bowl"}
        },
        "result": {"item": "kittytalents:rice_bowl"}
    },
    "crown": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["G G", "GRG", "GGG"],
        "key": {
            "G": {"item": "minecraft:gold_ingot"},
            "R": {"item": "minecraft:redstone"}
        },
        "result": {"item": "kittytalents:crown"}
    },
    "halo": {
        "type": "minecraft:crafting_shaped",
        "pattern": [" N ", "N N", " N "],
        "key": {
            "N": {"item": "minecraft:gold_nugget"}
        },
        "result": {"item": "kittytalents:halo"}
    },
    "angel_wings": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["F F", "FQF", "F F"],
        "key": {
            "F": {"item": "minecraft:feather"},
            "Q": {"item": "minecraft:quartz"}
        },
        "result": {"item": "kittytalents:angel_wings"}
    },
    "cat_scarf": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["R R", " R ", "RYR"],
        "key": {
            "R": {"item": "minecraft:red_wool"},
            "Y": {"item": "minecraft:yellow_wool"}
        },
        "result": {"item": "kittytalents:cat_scarf"}
    },
    "phantom_mask": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["P P", "PAP"],
        "key": {
            "P": {"item": "minecraft:phantom_membrane"},
            "A": {"item": "minecraft:amethyst_shard"}
        },
        "result": {"item": "kittytalents:phantom_mask"}
    },
    "upgrade_productivity_1": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["IRI", "RTR", "IRI"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "R": {"item": "minecraft:redstone"},
            "T": {"item": "kittytalents:catnip_treat"}
        },
        "result": {"item": "kittytalents:upgrade_productivity_1"}
    },
    "upgrade_productivity_2": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["GGG", "GUG", "GGG"],
        "key": {
            "G": {"item": "minecraft:gold_ingot"},
            "U": {"item": "kittytalents:upgrade_productivity_1"}
        },
        "result": {"item": "kittytalents:upgrade_productivity_2"}
    },
    "upgrade_productivity_3": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["DDD", "DUD", "DDD"],
        "key": {
            "D": {"item": "minecraft:diamond"},
            "U": {"item": "kittytalents:upgrade_productivity_2"}
        },
        "result": {"item": "kittytalents:upgrade_productivity_3"}
    },
    "upgrade_productivity_4": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["EEE", "EUE", "EEE"],
        "key": {
            "E": {"item": "minecraft:emerald"},
            "U": {"item": "kittytalents:upgrade_productivity_3"}
        },
        "result": {"item": "kittytalents:upgrade_productivity_4"}
    },
    "upgrade_productivity_5": {
        "type": "minecraft:crafting_shaped",
        "pattern": ["NNN", "NUN", "NNN"],
        "key": {
            "N": {"item": "minecraft:netherite_scrap"},
            "U": {"item": "kittytalents:upgrade_productivity_4"}
        },
        "result": {"item": "kittytalents:upgrade_productivity_5"}
    },
    "upgrade_productivity_6": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "kittytalents:upgrade_productivity_5"},
            {"item": "kittytalents:dire_treat"}
        ],
        "result": {"item": "kittytalents:upgrade_productivity_6"}
    }
}

for name, rdata in recipes.items():
    with open(os.path.join(RECIPES_DIR, f"{name}.json"), "w") as f:
        json.dump(rdata, f, indent=2)

print(f"Generated {len(recipes)} crafting recipes successfully!")
