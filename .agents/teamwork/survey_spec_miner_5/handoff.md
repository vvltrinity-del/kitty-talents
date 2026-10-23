# Handoff Report — R4 Asset Polish Audit (Textures, VFX, GFX, SFX, Models, Localization)

- **Agent**: survey_spec_miner_5 (SPECIFICATION MINER)
- **Working Directory**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\survey_spec_miner_5`
- **Project Root**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`
- **Target Milestone**: R4 (Asset Polish: Textures, VFX, GFX, SFX, Models, Localization)

---

## 1. Observation

### 1.1 Resource Inventory & Structure
A comprehensive scan of `src/main/resources` and `src/main/java` revealed the following exact counts:
- **Total JSON Resource Files**: 148 files (all verified 100% syntactically valid JSON).
  - Blockstates: 8 files (`assets/kittytalents/blockstates/*.json`)
  - Block Models: 8 files (`assets/kittytalents/models/block/*.json`)
  - Item Models: 67 files (`assets/kittytalents/models/item/*.json`)
  - Lang File: 1 file (`assets/kittytalents/lang/en_us.json`, containing 220 keys)
  - Data Recipes: 60 files (`data/kittytalents/recipes/*.json`)
  - Data Item Tags: 4 files (`data/kittytalents/tags/items/*.json`)
  - Pack / Mod metadata: `pack.mcmeta` (pack_format 15) and `META-INF/mods.toml`
- **Total PNG Texture Files**: 83 files (all verified with valid 8-byte PNG header signatures `89 50 4E 47 0D 0A 1A 0A`, power-of-2 dimensions, non-zero file sizes):
  - Item Textures: 58 files in `assets/kittytalents/textures/item/` (all 16x16 pixels).
  - Entity Cat Textures: 7 files in `assets/kittytalents/textures/entity/cat/` (`panther.png`, `skin_11.png` through `skin_16.png`, all 64x32 pixels).
  - Accessory Textures: 10 files in `assets/kittytalents/textures/models/accessories/` (`angel_wings.png`, `bowtie.png`, `cat_scarf.png`, `crown.png`, `feline_goggles.png`, `halo.png`, `hero_cape.png`, `phantom_mask.png`, `top_hat.png`, `witch_hat.png`, all 32x32 pixels).
  - Armor Textures: 8 files in `assets/kittytalents/textures/models/armor/` (`cat_armor_chainmail.png`, `cat_armor_diamond.png`, `cat_armor_golden.png`, `cat_armor_iron.png`, `cat_armor_leather_overlay.png`, `cat_armor_leather.png`, `cat_armor_netherite.png`, `cat_armor_turtle.png`, all 64x32 pixels).
- **Missing / Orphaned Files**:
  - Exactly 0 missing item models: all 67 items in `ModItems.java` have corresponding models.
  - Exactly 0 missing item textures: all 58 non-block item models map to existing PNG textures.
  - Exactly 0 orphaned item textures: all 58 PNGs have corresponding item model definitions.
  - Exactly 0 missing block models: all 8 blocks in `ModBlocks.java` have corresponding block models and blockstates.

### 1.2 Sound System Audit
- **Sounds Directory & sounds.json**:
  - `src/main/resources/assets/kittytalents/sounds.json`: **Does not exist**.
  - `src/main/resources/assets/kittytalents/sounds/`: **Does not exist** (0 `.ogg` audio files).
  - `com.kittytalents.registry.ModSounds`: **Does not exist** (no custom `DeferredRegister<SoundEvent>` registered).
- **Sound Usage in Code**:
  - All sound effects throughout abilities, AI goals, block entities, items, and events invoke vanilla Minecraft `SoundEvents`:
    - `SoundEvents.CAT_PURREOW`, `CAT_PURR`, `CAT_HISS`, `CAT_BEG_FOR_FOOD`, `CAT_DEATH`, `CAT_EAT`
    - `SoundEvents.ARMOR_EQUIP_LEATHER`, `PLAYER_BURP`, `COW_MILK`, `BUCKET_EMPTY`, `FISHING_BOBBER_SPLASH`
    - `SoundEvents.TOTEM_USE`, `FOX_BITE`, `ENDERMAN_TELEPORT`, `LIGHTNING_BOLT_THUNDER`, `RAVAGER_ATTACK`
    - `SoundEvents.ITEM_BREAK`, `PLAYER_ATTACK_STRONG`, `PLAYER_ATTACK_SWEEP`, `GENERIC_EXTINGUISH_FIRE`
    - `SoundEvents.BEACON_ACTIVATE`, `PHANTOM_FLAP`, `FENCE_GATE_OPEN`, `SNIFFER_DIGGING`, `BREWING_STAND_BREW`
    - `SoundEvents.GENERIC_EXPLODE`, `DISPENSER_FAIL`, `PLAYER_ATTACK_CRIT`, `GENERIC_SWIM`
  - Verifiable Result: Since no custom `kittytalents:...` sound events exist, loading into the game produces **zero** "Missing sound" log warnings.

### 1.3 VFX, GFX & Particle Audit
- Custom `ParticleType` registrations: None.
- Particle Usage: All particle effects in `TalentedCat`, abilities, AI tasks, and combat effects call vanilla `net.minecraft.core.particles.ParticleTypes`:
  - `ELECTRIC_SPARK`, `CRIT`, `HEART`, `ANGRY_VILLAGER`, `FLAME`, `SMOKE`, `ENCHANTED_HIT`, `POOF`, `SPLASH`, `SNOWFLAKE`, `SNEEZE`, `SOUL_FIRE_FLAME`, `CAMPFIRE_COSY_SMOKE`, `DRAGON_BREATH`, `ENCHANT`, `SWEEP_ATTACK`, `END_ROD`, `HAPPY_VILLAGER`, `ITEM`, `BLOCK`, `EXPLOSION_EMITTER`, `DAMAGE_INDICATOR`, `GLOW`, `SONIC_BOOM`.
- Projectile Renderers in `ClientModEvents.java:32-33`:
  - `ModEntities.THROWN_TOY` registers `ThrownItemRenderer::new`. `ThrownToyEntity.java:34-36` overrides `getDefaultItem()` returning `ModItems.YARN_BALL.get()`.
  - `ModEntities.FELINE_HISS_PROJECTILE` registers `ThrownItemRenderer::new`. `FelineHissProjectileEntity.java:31-33` overrides `getDefaultItem()` returning `Items.FIRE_CHARGE`.

### 1.4 Localization Audit (`en_us.json`)
- Translation Key Count: 220 keys in `src/main/resources/assets/kittytalents/lang/en_us.json`.
- Key Coverage:
  - 100% of `ModItems` (all 67 items mapped to `item.kittytalents.<name>` or `block.kittytalents.<name>`).
  - 100% of `ModBlocks` (all 8 blocks mapped to `block.kittytalents.<name>`).
  - 100% of `ModEntities` (all 3 entities mapped to `entity.kittytalents.<name>`).
  - 100% of `Talents` (all 44 talents mapped to `talent.kittytalents.<id>` and `talent.kittytalents.<id>.desc`).
  - 100% of `CatMode` (all 11 modes mapped to `mode.kittytalents.<mode>`).
  - 100% of keybindings in `ClientTwineHandler.java` (`key.categories.kittytalents`, `key.kittytalents.twine`, `key.kittytalents.untwine`, `key.kittytalents.ability_1`, `key.kittytalents.ability_2`).
  - All interactive messages (`message.kittytalents.*`, `tooltip.kittytalents.*`, `container.kittytalents.*`).

### 1.5 Identified Asset Defects & Root Causes

#### Defect 1: Cape Accessory Renders Under Belly Instead of Above Back
- **File**: `src/main/java/com/kittytalents/client/renderer/TalentedCatAccessoryLayer.java`, lines 113-116:
  ```java
  // 9. HERO_CAPE: Clasp + long cape drape
  root.addOrReplaceChild("hero_cape", CubeListBuilder.create()
          .texOffs(0, 0).addBox(-2.2F, 2.8F, -8.2F, 4.4F, 1.0F, 0.6F)
          .texOffs(0, 2).addBox(-2.5F, 3.8F, -8.3F, 5.0F, 12.0F, 0.4F),
          PartPose.ZERO);
  ```
  And lines 188-191:
  ```java
  } else if (stack.is(ModItems.HERO_CAPE.get())) {
      this.getParentModel().getBodyPart().translateAndRotate(poseStack);
      VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(HERO_CAPE_TEX));
      this.heroCapePart.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
  }
  ```
- **Direct Cause**: Vanilla `OcelotModel` / `CatModel` defines `body` as `addBox(-2.0F, 3.0F, -8.0F, 4.0F, 16.0F, 6.0F)` with rotation `x = +PI / 2` (+90 degrees). Under a +90° X rotation:
  $$y_{model} = -z_{local}$$
  For the cat's body depth from $z = -8.0F$ to $z = -2.0F$:
  - At $z = -8.0F$: $y_{model} = +8.0F$, which adds to base Y offset 12.0F yielding $y_{world} = 20.0F$ (closer to ground 24.0F, which is the **belly / underside** of the cat).
  - At $z = -2.0F$: $y_{model} = +2.0F$, yielding $y_{world} = 14.0F$ (higher from ground, which is the **dorsal spine / back** of the cat).
  - Because `hero_cape` specified $Z = -8.2F$ and $Z = -8.3F$, the cape is placed at $y_{world} = 20.2F$, rendering underneath the cat's belly instead of resting on its back!

#### Defect 2: Black Panther Skin Has Amber/Yellow Eyes Instead of Bright Blue Eyes
- **Files**:
  - `src/main/resources/assets/kittytalents/textures/entity/cat/panther.png`
  - `src/main/resources/assets/kittytalents/textures/entity/cat/skin_16.png`
- **Pixel Sampling Observation**:
  Inspection of front face eye coordinates $(x=5..9, y=5..8)$ revealed:
  - $(5,6) = \text{\#E1B945}$ (Golden Yellow)
  - $(6,6) = \text{\#C07832}$ (Amber Brown)
  - $(8,6) = \text{\#C07832}$ (Amber Brown)
  - $(9,6) = \text{\#E1B945}$ (Golden Yellow)
- **Contradiction**:
  - `CatVariant.java:27`:
    `PANTHER("panther", "Black Panther", VariantRarity.ADVANCED, "Bright Blue", 0x0F172A, () -> ItemStack.EMPTY, "Night Stalker")`
  - User Requirement under R4: "The black panther skin should be updated to have a proper panther texture with blue eyes."

#### Defect 3: PacketChangeCatSkin Clamps to 15, Blocking Panther Skin 16
- **File**: `src/main/java/com/kittytalents/network/PacketChangeCatSkin.java`, lines 41-51:
  ```java
  int currentSkin = cat.getSkinId();
  int totalSkins = 16;
  int newSkin;
  if (targetSkin == -2) {
      newSkin = (currentSkin + 1) % totalSkins;
  } else if (targetSkin == -1) {
      newSkin = (currentSkin - 1 + totalSkins) % totalSkins;
  } else {
      newSkin = Math.max(0, Math.min(targetSkin, totalSkins - 1));
  }
  cat.setSkinId(newSkin);
  ```
- **Direct Cause**: `totalSkins` is hardcoded to 16, clamping explicit skin requests to $16 - 1 = 15$. But `CatTalentScreen.SKIN_NAMES` has 17 elements (indices 0 to 16), where index 16 is "Panther". When a player selects Panther in the GUI, the packet clamps it to 15 ("Ghost Phantom"), making skin 16 unobtainable via packet.

#### Defect 4: Vanilla Cat Conversion Resets Skin to Tabby (Skin 0)
- **File**: `src/main/java/com/kittytalents/event/ForgeEvents.java`, lines 68-71:
  ```java
  talentedCat.setVariant(cat.getVariant());
  talentedCat.setCatVariant(com.kittytalents.entity.CatVariant.fromVanillaCatVariant(cat.getVariant()));
  talentedCat.setFemale(cat.getRandom().nextBoolean());
  talentedCat.setCollarColor(cat.getCollarColor());
  ```
- **Direct Cause**: When training a vanilla cat, `skinId` is never initialized. `TalentedCat` initializes `DATA_SKIN_ID` to 0. `TalentedCatRenderer.java:45` checks `skinId >= 0 && skinId < VANILLA_SKINS.length` and returns `VANILLA_SKINS[skinId]`. Because `skinId` is 0, any vanilla cat trained (Siamese, Calico, Persian, Black, White, etc.) visibly reverts to Tabby (`VANILLA_SKINS[0]`).

#### Defect 5: Advanced Variants Summoned/Bred with Skin 0 Default to Tabby Texture
- **File**: `src/main/java/com/kittytalents/client/renderer/TalentedCatRenderer.java`, lines 40-50:
  ```java
  int skinId = cat.getSkinId();
  if (skinId >= 0 && skinId < VANILLA_SKINS.length) {
      if (skinId == 0 && cat.getCatVariant() == com.kittytalents.entity.CatVariant.PANTHER) {
          return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/panther.png");
      }
      return VANILLA_SKINS[skinId];
  } else if (skinId >= 11 && skinId <= 16) {
      return new ResourceLocation(KittyTalents.MODID, "textures/entity/cat/skin_" + skinId + ".png");
  }
  return cat.getResourceLocation();
  ```
- **Direct Cause**: Only `CatVariant.PANTHER` has an override when `skinId == 0`. Other advanced variants (`TIGER`, `SNOW_LEOPARD`, `VOID_ENDER`, `GOLDEN_MANEKI`, `GHOST_PHANTOM`) do not check `cat.getCatVariant()`. If summoned or bred with `skinId == 0`, they render as Tabby instead of their designated custom skins (`skin_11` through `skin_15`).

#### Defect 6: Cat Armor Model Architecture ("Goofy" Appearance)
- **Files**:
  - `src/main/java/com/kittytalents/client/renderer/TalentedCatArmorLayer.java`
  - `src/main/java/com/kittytalents/client/renderer/TalentedCatArmorModel.java`
- **Direct Cause**:
  1. The armor model is instantiated directly from `OcelotModel.createBodyMesh(new CubeDeformation(0.22F))` (outer) and `(0.12F)` (inner). It duplicates the full feline mesh, enclosing the head (including muzzle and ears) in an opaque inflated box.
  2. For `LEGS`, `this.body.visible = (slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS);` causes the torso box to render a second time on top of the chestplate.
  3. For `FEET`, all 4 legs are inflated as monolithic pillar boxes.
  4. The 8 armor textures (`cat_armor_*.png`) have flat color fills without form-fitting cutouts, making the armored cat look bulky, boxy, and unnatural.

#### Defect 7: Duplicate Items in Creative Mode Tab
- **File**: `src/main/java/com/kittytalents/registry/ModCreativeTabs.java`, lines 44-45 and lines 69-70:
  ```java
  // Lines 44-45 (Toys)
  output.accept(ModItems.YARN_BALL.get());
  output.accept(ModItems.FEATHER_TOY.get());
  ...
  // Lines 69-70 (Toys repeated)
  output.accept(ModItems.YARN_BALL.get());
  output.accept(ModItems.FEATHER_TOY.get());
  ```
  `ModItems.YARN_BALL` and `ModItems.FEATHER_TOY` appear twice in the creative inventory tab.

---

## 2. Logic Chain

1. **Premise 1 (Zero Missing Textures / Missing Sounds)**:
   - Auditing all 67 item registrations, 8 block registrations, 3 entity registrations, and 60 recipes confirmed 100% resolution to existing models and textures.
   - Auditing audio confirmed zero custom sound registrations and zero `sounds.json` entries. All mod sounds resolve directly to vanilla Minecraft `SoundEvents`.
   - Invalidation condition: If a custom sound event were registered in Java without a corresponding entry in `sounds.json` or without `.ogg` audio files, the client log would report `Missing sound`. Because none are registered, exactly 0 missing sound warnings occur.

2. **Premise 2 (Hero Cape Coordinate Geometry)**:
   - In Minecraft's entity model system, the body part has an X-rotation of $+90^\circ$ ($\frac{\pi}{2}$ radians).
   - The transformation matrix transforms local coordinates $(x, y, z)$ to $(x, -z, y)$.
   - In the ocelot model, the body box is located at $Z \in [-8.0, -2.0]$.
   - Transforming local Z coordinates to model space yields:
     $$\text{At } z = -8.0F: \quad y_{model} = -(-8.0F) = +8.0F \quad (\text{toward ground } \rightarrow \text{Belly})$$
     $$\text{At } z = -2.0F: \quad y_{model} = -(-2.0F) = +2.0F \quad (\text{toward sky } \rightarrow \text{Back / Spine})$$
   - `hero_cape` placed the clasp and drape at $Z = -8.2F$ and $Z = -8.3F$.
   - Therefore, the cape renders at $y_{model} = +8.2F$, placing it directly underneath the cat's belly. Moving the Z coordinate to $[-1.8F, -1.9F]$ places it at $y_{model} \approx +1.8F$, resting directly on top of the cat's back.

3. **Premise 3 (Black Panther Eye Color Discrepancy)**:
   - Sampling pixel $(5, 6)$ and $(6, 6)$ on `panther.png` and `skin_16.png` proves the eye pixels are amber/yellow ($\text{\#E1B945}$ and $\text{\#C07832}$).
   - `CatVariant.PANTHER` specifies eye color "Bright Blue".
   - The user explicitly requested updating the black panther skin to have blue eyes.
   - Adjusting eye pixels $(5,6)$ and $(8,6)$ to bright blue ($\text{\#38BDF8}$ / $\text{\#0284C7}$) restores fidelity with the specification.

4. **Premise 4 (Packet Clamping and Skin Inheritance)**:
   - `PacketChangeCatSkin` clamps `targetSkin` using `totalSkins = 16`, preventing selection of skin index 16.
   - Increasing `totalSkins = 17` allows index 16 ("Panther") to be transmitted and stored.
   - In `ForgeEvents.java`, mapping `cat.getVariant()` to the integer skin index $(0..10)$ upon taming ensures newly trained cats retain their visual appearance rather than defaulting to Tabby.

---

## 3. Caveats

- **Runtime Audio Playback**: KittyTalents currently has zero custom sound files (`.ogg`). All cat abilities, weapons, whistles, and foods reuse vanilla Minecraft sound effects. This fulfills the acceptance criterion of zero `Missing sound` warnings, but lacks bespoke mod sound design.
- **Dedicated Client Headless Testing**: Visual render layers and client model geometry require a Minecraft client rendering context or test world to be visually inspected in real time. Mathematical model coordinate proofs were conducted analytically.

---

## 4. Conclusion & Actionable Recommendations

KittyTalents has a solid resource foundation (all 148 JSONs valid, all 83 PNG textures power-of-2 with valid headers, full translation coverage). However, several key visual defects and limitations were identified that must be resolved for R4 Polish:

### Recommended Fix Roadmap:
1. **Cape Accessory Model Offset Fix**:
   - In `TalentedCatAccessoryLayer.java:114-115`, change $Z$ coordinates from `-8.2F` and `-8.3F` to `-1.8F` (clasp) and `-1.9F` (drape):
     ```java
     root.addOrReplaceChild("hero_cape", CubeListBuilder.create()
             .texOffs(0, 0).addBox(-2.2F, 2.8F, -1.8F, 4.4F, 1.0F, 0.6F)
             .texOffs(0, 2).addBox(-2.5F, 3.8F, -1.9F, 5.0F, 12.0F, 0.4F),
             PartPose.ZERO);
     ```
2. **Black Panther Skin Update**:
   - Update `panther.png` and `skin_16.png` at eye coordinates $(5,6)$ and $(9,6)$ to bright blue `#38BDF8`, and pupils $(6,6)$ and $(8,6)$ to `#0284C7` / `#1E3A8A`.
3. **Cat Armor Remodel & Texture Polish**:
   - In `TalentedCatArmorModel.java`, separate helmet, barding harness, flank guards, and paw cuffs so armor fits snugly around the cat's anatomy without monolithic full-body inflation.
   - Update `cat_armor_*.png` textures with distinct straps, buckles, and open face/ear sections.
4. **PacketChangeCatSkin Total Skins Fix**:
   - Change `int totalSkins = 16;` to `int totalSkins = 17;` (or `CatTalentScreen.SKIN_NAMES.length`).
5. **ForgeEvents Skin ID Mapping Fix**:
   - In `ForgeEvents.java`, compute and set `talentedCat.setSkinId(...)` from vanilla `cat.getVariant()` when converting a cat.
6. **Creative Tab Deduplication**:
   - In `ModCreativeTabs.java`, remove duplicate `ModItems.YARN_BALL` and `ModItems.FEATHER_TOY` from lines 44-45.

---

## 5. Verification Method

1. **Compile & Build Verification**:
   - Run `./gradlew compileJava --no-daemon` to ensure 100% clean compilation.
   - Run `./gradlew test --no-daemon` to verify all test suites pass.
2. **Resource Integrity Verification**:
   - Check all JSON files: `Get-ChildItem -Path "src/main/resources" -Filter "*.json" -Recurse | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json -AsHashtable }`
   - Check all PNG dimensions: Ensure widths and heights are powers of 2.
3. **Client Log Missing Texture/Sound Check**:
   - Launch client into test world, search `latest.log` for `[Client thread/WARN]: Missing texture: kittytalents:` and `[Client thread/WARN]: Missing sound for event: kittytalents:`. Verify count is exactly 0.
4. **Visual Cape & Panther Verification**:
   - Equip Hero Cape on cat; verify cape lies across the dorsal spine, flowing down to tail.
   - Equip Panther skin; verify cat renders with vivid bright blue eyes.

---

## Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Progression | Catnip & Treats | 5 tiers of treats (Catnip Treat, Super, Master, Dire, Amnesia) for leveling | Right-click cat | Levels cat up, opens talent points | Rejects if cat level out of tier bounds | `ModItems.java`, `CatTreatItem.java` |
| 2 | Utilities | Kitty Whistle | Remote command center for issuing group commands to cats | Right-click | Opens Whistle GUI / issues command | No-op if player has no owned cats | `KittyWhistleItem.java`, `KittyWhistleScreen.java` |
| 3 | Utilities | Kitty Tracker | Radar item that locates and points toward the owner's cats | Held / Right-click | Chat coordinates & directional particle | Notifies if no cats bound | `KittyTrackerItem.java` |
| 4 | Utilities | Pet Carrier | Item for picking up and safely transporting a cat | Right-click cat | Cat stored into carrier ItemStack NBT | Fails if carrier already full | `CatCarrierItem.java` |
| 5 | Utilities | Laser Pointer | Emits red beam particles for cats to chase | Hold right-click | Dust particles & AI chase vector | Ignored if cat is docile/sitting | `CatLaserPointerItem.java` |
| 6 | Furniture | Cat Food Bowl | Block storing feline foods for cats to automatically eat | Block interaction / hopper | Dispenses nourishment to nearby cats | Ignores non-food items | `CatFoodBowlBlock.java`, `CatFoodBowlBlockEntity.java` |
| 7 | Furniture | Cat Bed | Comfy bed where cats rest, heal rapidly, and set home spawn | Right-click cat on bed | Links bed to cat UUID | Notifies if no cat nearby | `CatBedBlock.java`, `CatBedBlockEntity.java` |
| 8 | Furniture | Cat Bathtub | Washing block with bubble particles for feline hygiene | Water bucket + Cat | Cleans cat, grants purr buff | Inactive if empty of water | `CatBathBlock.java`, `CatBathBlockEntity.java` |
| 9 | Furniture | Cat Scratching Post | Scratching post where cats sharpen claws, granting crit buff | Cat interaction | Crit & heart particles, claw buff | Inactive if already sharpened | `CatScratcherBlock.java` |
| 10 | Furniture | Kitty Habitat | Production workstation assigning a cat to passive resource production | Gui / Worker assignment | Automates periodic item generation | Pauses if inventory is full | `KittyHabitatBlock.java`, `KittyHabitatScreen.java` |
| 11 | Furniture | Kitten Incubator | Machine speeding up kitten maturation with heat / catnip | Power / fuel + Kitten | Accelerates growth ticks | Idle if no kitten present | `KittenIncubatorBlock.java` |
| 12 | Furniture | Feeding Station | Automated food dispenser keeping nearby colony cats fed | Hopper / chest link | Feeds cats within 12 blocks | Empty warning if no meat/fish | `KittyFeedingStationBlock.java` |
| 13 | Cosmetics | 3D Wearable Accessories | 10 custom 3D accessories (Top Hat, Witch Hat, Crown, Halo, Goggles, Bowtie, Cape, Scarf, Mask, Angel Wings) | Equipped in accessory slot | Renders custom 3D layers on cat model | Falls back to generic item render if unknown | `TalentedCatAccessoryLayer.java` |
| 14 | Combat | Feline Armor | 8 armor tiers (Leather, Chainmail, Iron, Golden, Diamond, Netherite, Turtle, Leather Overlay) | Equipped in armor slots 1-4 | Provides damage reduction and render layer | Requires Cat Armor talent points | `TalentedCatArmorLayer.java`, `TalentedCatArmorModel.java` |
| 15 | Combat | Mouth Weapons | Equips melee weapons and TacZ firearms in mouth | Equipped in weapon slot | Custom jaw render angle & enhanced claw damage | Requires Cat Armory talent points | `TalentedCatWeaponLayer.java` |
| 16 | Breeding | Mutation Nekomata | Double-tailed mystical cat mutation resulting from rare breeding | Breeding recipes | Renders two animated waving tails | Normal single tail if not mutated | `TalentedCatTailLayer.java`, `CatBreedingManager.java` |
| 17 | Flight | Angel & Elytra Wings | Celestial wings providing gliding, flight, and air combat | Sky Feline talent / Elytra / Angel Wings | Renders flared wings on torso with air dynamics | Hidden if cat invisible or grounded | `TalentedCatWingLayer.java` |
| 18 | Cosmetics | Cat Skin Customization | 17 selectable skins (11 vanilla breeds + 6 advanced skins: Tiger, Snow Leopard, Void Ender, Golden Maneki, Ghost Phantom, Panther) | Style tab in CatTalentScreen | Updates entity synched `DATA_SKIN_ID` | Clamped to 15 in packet (Defect 3) | `CatTalentScreen.java`, `PacketChangeCatSkin.java` |
| 19 | UI | Dynamic Cat UI | 5-tab container UI (Home, Talents, Gear, Style, Stats) rendered via GuiGraphics | Right-click cat with Stick | Interactive HUD overlay | Disabled if cat not owned | `CatTalentScreen.java` |
| 20 | Control | Feline Twine System | Remote neural link allowing player to take first-person control of cat | Keybind Z (Twine) / U (Untwine) | Smooth camera switch, hidden head model, input sync | Disengages on cat hurt or untwine key | `ClientTwineHandler.java`, `ClientModEvents.java` |

---

## Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Hero Cape Rendering | Equipping `hero_cape` accessory | Renders upside down / underneath the cat's belly due to $Z = -8.2F$ coordinate inversion. |
| 2 | Black Panther Texture | Selecting Panther skin (Skin 16) | Displays amber/yellow eyes (`#E1B945`), contradicting the blue eyes specification. |
| 3 | Skin Selection Network Sync | Selecting "Panther" in Style GUI | Packet clamps `targetSkin` with `totalSkins = 16`, setting skin 15 ("Ghost Phantom") instead. |
| 4 | Vanilla Cat Taming Conversion | Feeding `catnip_treat` to vanilla cat | Successfully converts to `TalentedCat` but fails to initialize `skinId`, resetting cat to Tabby. |
| 5 | Advanced Variant Summoning | Spawning cat with variant `TIGER` and `skinId = 0` | Renders Tabby texture because `TalentedCatRenderer` only checks variant for `PANTHER` when `skinId == 0`. |
| 6 | Armor Slot Overlap | Equipping both Chestplate and Leggings | Torso body mesh is rendered twice by outer and inner armor models, causing visual z-fighting. |
| 7 | Helmet Face Coverage | Equipping any helmet on cat | Inflated head mesh covers the entire face, eyes, and muzzle without eyeholes or ear cutouts. |
| 8 | Creative Tab Display | Opening Kitty Talents creative tab | `ModItems.YARN_BALL` and `ModItems.FEATHER_TOY` appear twice in the item list. |
| 9 | Audio Playback | Triggering abilities / interactions | Vanilla Minecraft sounds play reliably; zero missing sound log errors, but no custom audio. |
| 10 | First-Person Twine Camera | Activating Twine control mode | Cat's head model part is automatically hidden (`model.getHeadPart().visible = false`) to prevent clipping. |
