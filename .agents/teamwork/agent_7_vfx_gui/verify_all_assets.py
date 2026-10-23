import os
import re
import json

base_dir = r"C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents"
assets_dir = os.path.join(base_dir, "src", "main", "resources", "assets", "kittytalents")

print("--- 1. Checking Item & Block Model Textures ---")
missing_textures = []
for model_type in ["item", "block"]:
    model_dir = os.path.join(assets_dir, "models", model_type)
    if not os.path.exists(model_dir):
        continue
    for fname in os.listdir(model_dir):
        if not fname.endswith(".json"):
            continue
        p = os.path.join(model_dir, fname)
        try:
            with open(p, "r", encoding="utf-8") as f:
                data = json.load(f)
            textures = data.get("textures", {})
            for tex_key, tex_path in textures.items():
                if tex_path.startswith("kittytalents:"):
                    rel = tex_path[len("kittytalents:"):]
                    png_path = os.path.join(assets_dir, "textures", rel.replace("/", os.sep) + ".png")
                    if not os.path.exists(png_path):
                        missing_textures.append((fname, tex_key, tex_path, png_path))
        except Exception as e:
            print(f"Error reading model {fname}: {e}")

if missing_textures:
    print(f"FAILED: Found {len(missing_textures)} missing textures:")
    for m in missing_textures:
        print(" ", m)
else:
    print("SUCCESS: 0 missing textures across all models!")

print("\n--- 2. Checking ModSounds & sounds.json ---")
mod_sounds_path = os.path.join(base_dir, "src", "main", "java", "com", "kittytalents", "registry", "ModSounds.java")
sounds_json_path = os.path.join(assets_dir, "sounds.json")

with open(mod_sounds_path, "r", encoding="utf-8") as f:
    mod_sounds_content = f.read()

sound_registrations = re.findall(r'register\(\s*"([^"]+)"\)', mod_sounds_content)
print(f"Registered SoundEvents in ModSounds: {len(sound_registrations)}")

with open(sounds_json_path, "r", encoding="utf-8") as f:
    sounds_data = json.load(f)

missing_in_json = []
missing_ogg_files = []

for s in sound_registrations:
    if s not in sounds_data:
        missing_in_json.append(s)
    else:
        sound_entry = sounds_data[s]
        for sfile in sound_entry.get("sounds", []):
            if isinstance(sfile, dict):
                sname = sfile["name"]
            else:
                sname = sfile
            if sname.startswith("kittytalents:"):
                sname = sname[len("kittytalents:"):]
            ogg_path = os.path.join(assets_dir, "sounds", sname.replace("/", os.sep) + ".ogg")
            if not os.path.exists(ogg_path):
                missing_ogg_files.append((s, sname, ogg_path))

if missing_in_json:
    print(f"FAILED: Sounds registered but missing from sounds.json: {missing_in_json}")
else:
    print("SUCCESS: All ModSounds registered are mapped in sounds.json!")

if missing_ogg_files:
    print(f"FAILED: Missing .ogg files: {missing_ogg_files}")
else:
    print("SUCCESS: All sound files exist as valid .ogg files!")

print("\n--- 3. Checking CatAbilities & Ability Icons ---")
cat_abilities_path = os.path.join(base_dir, "src", "main", "java", "com", "kittytalents", "ability", "CatAbilities.java")
with open(cat_abilities_path, "r", encoding="utf-8") as f:
    cat_abilities_content = f.read()

# find all registered abilities
ability_ids = re.findall(r'public static final CatAbility\s+([A-Z0-9_]+)\s*=', cat_abilities_content)
ability_tex_dir = os.path.join(assets_dir, "textures", "ability")
existing_ability_icons = os.listdir(ability_tex_dir) if os.path.exists(ability_tex_dir) else []
print(f"Found {len(ability_ids)} ability definitions in CatAbilities.")
print(f"Found {len(existing_ability_icons)} ability icons in textures/ability/.")

print("\n--- 4. Checking GUI Icons ---")
gui_icons_dir = os.path.join(assets_dir, "textures", "gui", "icons")
existing_gui_icons = os.listdir(gui_icons_dir) if os.path.exists(gui_icons_dir) else []
print(f"Found {len(existing_gui_icons)} icons in textures/gui/icons/: {existing_gui_icons}")
