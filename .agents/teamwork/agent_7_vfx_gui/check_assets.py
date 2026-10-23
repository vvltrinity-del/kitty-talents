import os
import re
import json

base_dir = r"C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents"
mod_items = os.path.join(base_dir, "src", "main", "java", "com", "kittytalents", "registry", "ModItems.java")
mod_blocks = os.path.join(base_dir, "src", "main", "java", "com", "kittytalents", "registry", "ModBlocks.java")
assets_dir = os.path.join(base_dir, "src", "main", "resources", "assets", "kittytalents")

with open(mod_items, "r", encoding="utf-8") as f:
    items_content = f.read()

items = re.findall(r'ITEMS\.register\(\s*"([^"]+)"', items_content)
print(f"Registered items: {len(items)}")

item_models_dir = os.path.join(assets_dir, "models", "item")
missing_item_models = []
for it in items:
    p = os.path.join(item_models_dir, f"{it}.json")
    if not os.path.exists(p):
        missing_item_models.append(it)

print(f"Missing item models: {len(missing_item_models)}: {missing_item_models}")

# Also check block models / blockstates
with open(mod_blocks, "r", encoding="utf-8") as f:
    blocks_content = f.read()

blocks = re.findall(r'BLOCKS\.register\(\s*"([^"]+)"', blocks_content)
print(f"Registered blocks: {len(blocks)}")
blockstates_dir = os.path.join(assets_dir, "blockstates")
missing_blockstates = []
for b in blocks:
    p = os.path.join(blockstates_dir, f"{b}.json")
    if not os.path.exists(p):
        missing_blockstates.append(b)
print(f"Missing blockstates: {len(missing_blockstates)}: {missing_blockstates}")
