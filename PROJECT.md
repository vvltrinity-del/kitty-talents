# Project: Kitty Talents 3.0 Overhaul

## Architecture

Kitty Talents 3.0 transforms the mod into a production-grade feline creature-RPG featuring three seamlessly connected gameplay layers, strict creature identity, real-time Aniimo-style Twine control, Doggy Talents AI parity, deterministic production breeding, and 100% dedicated server multiplayer compatibility.

### 1. Three Gameplay Layers (Unified Stats & State)
- **Layer A — Companion RPG**: Persistent feline companion with XP, levels, talents, hunger, wellbeing, and equipment.
- **Layer B — Command Mode**: The player directs the cat using the Kitty Whistle and tactical commands. Cat operates via autonomous AI state machine.
- **Layer C — Twine Mode**: Direct creature control. The player inhabits the cat: feline movement, 4-way dodging, perfect dodge, Pounce 2.0, active ability hotbar, and feline perspective.
- **Unified Engine**: All three layers share the exact same underlying stats, talents, equipment, status effects, active abilities, Energy pool, and progression.

### 2. Strict Cat Identity & Separation
- **Breed / Feline Identity**: Actual anatomical breed/species identity (Tabby, Siamese, Persian, Panther, Tiger, Snow Leopard, etc.).
- **Skin**: Purely cosmetic visual overlay. Does NOT affect stats, breeding, production tier, or abilities.
- **Production Tier**: Deterministic production progression independent of skin:
  `STANDARD -> COAL -> COPPER -> IRON -> GOLD -> LAPIS -> REDSTONE -> EMERALD -> DIAMOND -> NETHERITE`
  Same valid breeding inputs strictly produce the exact next production tier with zero random rolls.
- **Nekomata Guarantee**: If either valid parent receives Spirit Catnip, offspring is **100% guaranteed Nekomata**. Nekomata features dual symmetrical tails anchored to the actual body pivot, dual strike combat perk, double harvest production perk, soul-flame visual effects, and bespoke Twine abilities.

### 3. Combat System & Active Ability Engine
- **Real-Time Combat**: Active frame data, feline combo chains (Swipe -> Swipe -> Bite -> Heavy Claw), weapon-dependent combos (Claws, Swords, TacZ Firearms).
- **Feline Dodging**: 4-directional dodging (forward, backward, left, right) with invulnerability frames and Energy cost.
- **Perfect Dodge**: Precision dodge against incoming attacks granting damage immunity, brief enemy slowdown, Energy refund, feedback sound, and subtle VFX.
- **Pounce 2.0**: Aim -> Target trajectory -> Crouch -> Launch arc -> Air control -> Impact damage & status effects.
- **Active Ability Registry & Loadout**: Registered abilities (`CatAbilities.REGISTRY`) with 3 equipped combat skills + 1 Ultimate. Supported abilities: Pounce, Feline Hiss, Healing Purr, Shadow Strike, Utility Interact, Volt Dash.
- **Attack VFX & Bonuses/Debuffs Scaling**: Attacks emit custom elemental VFX and apply damage, defense, and support bonuses/debuffs scaled dynamically by the cat's Element/Role Type and Production Tier.

### 4. Unified Derived Stat Engine
- Authoritative idempotent calculation pipeline:
  `Final Stat = Base + Level Modifier + Talent Modifiers + Equipment Modifiers + Variant Modifiers + Temporary Buffs + Status Effects`
- Recalculation is completely idempotent and cached.

### 5. Network & Sidedness Architecture
- Dedicated server safety via strict proxy separation: All client calls, screens, and keybinds are isolated in `com.kittytalents.client.*`.
- Client-bound packet handling routed via `ClientPacketHandlers` through `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ...)`.
- Full multiplayer entity tracking sync via `PlayerEvent.StartTracking` ensures newly joined/rendered players receive complete cat equipment, talent, and trust data.
- Dual-simulation conflict during Twining eliminated: minion player body remains in safe, dormant, damage-resistant pose while twined.

---

## Feature Inventory

Every requirement, directive item, and user request is inventoried and assigned to an exact milestone:

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Sided Packet Handling | Move client packet handling to `ClientPacketHandlers` via `DistExecutor` to eliminate `NoClassDefFoundError` on Dedicated Server | M1 | survey_explorer_2, survey_spec_miner_6 |
| 2 | Client Imports Elimination | Clean up `net.minecraft.client.*` imports from common items, packets, entities (`VariantChartItem`, `KittyWhistleItem`, `TalentedCat`) | M1 | survey_explorer_2 |
| 3 | PacketTeamCommand Registration | Register missing `PacketTeamCommand` in `PacketHandler.register()` | M1 | survey_explorer_2 |
| 4 | Multiplayer Tracking Sync | Implement `PlayerEvent.StartTracking` in `ForgeEvents` to sync talents, equipment, and trust to remote tracking players | M1 | survey_explorer_2 |
| 5 | Dedicated Server Test Infra | Configure `run/eula.txt` and `run/server.properties` for headless dedicated server verification | M1 | survey_spec_miner_6 |
| 6 | Global LivingTick Throttling | Eliminate 20Hz NBT allocation on all world entities in `FelineStatusEffects.onLivingTick` | M2 | survey_explorer_3 |
| 7 | Torch Goal TPS Throttling | Fix infinite 20Hz loop in `CatPlaceTorchGoal`, reduce search radius, set 40t cooldown on null, eliminate 885k block scan | M2 | survey_explorer_2, survey_explorer_3, survey_explorer_4 |
| 8 | Entity Scan Throttling | Throttle `CatCreeperWardenGoal` to 10-tick interval, throttle Gale Purr to 20-tick interval, throttle `CatBathBlockEntity` | M2 | survey_explorer_2, survey_explorer_3 |
| 9 | Radar & Locator Item Fix | Add cooldown to `KittyTrackerItem` and replace 2,048-block scan with direct `ServerLevel.getEntity(uuid)` in `FelineLocatorOrbItem` | M2 | survey_explorer_2, survey_explorer_3 |
| 10 | Pathfinder Thrashing Fix | Throttle `moveTo` calls in `CatRestOnBedGoal`, `CatGunAttackGoal`, `CatFetchToyGoal` with navigation cooldown timer | M2 | survey_explorer_3, survey_explorer_4 |
| 11 | Particle Sidedness Fix | Change `isClientSide` checks inside server AI goals (`CatRestOnBedGoal`, `CatUseScratcherGoal`) to `((ServerLevel)level).sendParticles` | M2 | survey_spec_miner_6, survey_explorer_2 |
| 12 | Twine Camera Smoothing | Interpolate camera rotation in `ComputeCameraAngles` using `Mth.rotLerp(partialTick)` to eliminate 20Hz micro-jitter | M3 | survey_explorer_1 |
| 13 | Front Camera 180° Fix | Respect third-person front view inversion in `ComputeCameraAngles` | M3 | survey_explorer_1 |
| 14 | Network Echo Suppression | Suppress `ServerEntity` position/rotation echo packets back to the controlling twined client | M3 | survey_explorer_1 |
| 15 | Dormant Player Body Pose | Replace conflicting dual-simulation of player body with a safe, dormant, damage-resistant pose during Twine | M3 | survey_explorer_1, survey_explorer_2 |
| 16 | Twine Fluid Swimming | Allow jump key to swim/ascend in water and lava instead of requiring `onGround()` | M3 | survey_explorer_1 |
| 17 | Twine Movement Prediction | Implement client-side prediction for pounce leap, wall leap, and flight | M3 | survey_explorer_1 |
| 18 | Sprint & Crouch Rebinding | Connect sprinting keybind to cat sprint, and rebind sneak key to feline crouch/stalk instead of premature untwine | M3 | survey_explorer_1 |
| 19 | Twine Step-Height Assist | Grant temporary 1.0 block step-height while twined to prevent getting stuck on 1-block terrain elevations | M3 | survey_explorer_1 |
| 20 | Mount vs Sit Conflict Fix | Ensure empty-hand right click toggles sit/stand; mounting requires sneak or specific interaction | M3 | survey_explorer_1, survey_explorer_4 |
| 21 | Feline 4-Way Dodge | Implement forward, backward, left, right dodge with invulnerability frames and Energy cost | M3 | MASTER DIRECTIVE §21 |
| 22 | Perfect Dodge Mechanic | Implement precision perfect dodge with damage avoidance, brief enemy slowdown, Energy refund, and audio/VFX feedback | M3 | MASTER DIRECTIVE §22 |
| 23 | Pounce 2.0 Real-Time Action | Aim -> Crouch -> Launch with parabolic arc -> Impact damage & status effects | M3 | MASTER DIRECTIVE §23 |
| 24 | Bed Resting Sit-Lock Fix | Call `setOrderedToSit(false)` in `CatRestOnBedGoal.stop()` so cats do not remain permanently sit-locked | M4 | survey_spec_miner_6, survey_explorer_4, survey_explorer_1 |
| 25 | Bed Goal DOCILE Loop Fix | Fix `canContinueToUse()` in `CatRestOnBedGoal` so it does not loop infinitely during daylight in DOCILE mode | M4 | survey_explorer_4 |
| 26 | Downed Cat Bed Pathing Fix | Prevent incapacitated cats from walking/sprinting to bed | M4 | survey_explorer_4 |
| 27 | Nine Lives Godmode Fix | Implement finite daily life charges or a 10-minute revival cooldown with particle burst | M4 | survey_explorer_4 |
| 28 | Fisher & Mole Crash Fix | Clamp random bound with `Math.max(1, bound)` in `CatFisherGoal` and `CatMoleMiningGoal` to prevent crash on level >= 10 | M4 | survey_explorer_4, survey_explorer_1 |
| 29 | Scratcher Timeout Protection | Add an 80-tick timeout to `CatUseScratcherGoal` so cats do not freeze if scratcher is unreachable | M4 | survey_explorer_4 |
| 30 | NBT Persistence of State | Serialize `carriedToy`, `abilitySlot1..3`, and `energy` in `addAdditionalSaveData`/`readAdditionalSaveData` | M4 | survey_explorer_4 |
| 31 | Energy Regeneration Engine | Implement continuous Energy regeneration in `TalentedCat.tick()` (+1 energy/sec, modified by role and stats) | M4 | survey_explorer_4, MASTER DIRECTIVE §12 |
| 32 | Active Abilities Registration | Register all 6 abilities in `CatAbilities.REGISTRY` and register `UseAbilityGoal` in `goalSelector` at Priority 4 | M4 | survey_explorer_4 |
| 33 | Fox Pounce Talent Check | Require `Talents.POUNCE > 0` in `CatFoxPounceGoal` and add a 60-tick airborne safety timeout | M4 | survey_explorer_4, survey_explorer_1 |
| 34 | Cliff Leaping Safety Fix | Replace raw air check in `customServerAiStep` with safe gap-check to prevent suicidal cliff diving | M4 | survey_explorer_4 |
| 35 | Whistle Command Overhaul | Fix `QUIET` (mute meows instead of granting invisibility), implement `ATTACK_TARGET` and `FETCH`, fix `STAND` | M4 | survey_explorer_4, survey_explorer_1 |
| 36 | Stub Talents Implementation | Implement functional logic for `CRITICAL_CLAWS`, `FELINE_REFLEXES`, `HOME_WHISKERS`, and `CUSHION_PAWS` | M4 | survey_explorer_4, survey_explorer_1 |
| 37 | Goal Priority Realignment | Restructure `goalSelector` so `FollowOwnerGoal` (Priority 6) outranks optional utility goals (Priority 8-9) | M4 | survey_explorer_4, survey_explorer_1 |
| 38 | Missing Modes Implementation | Implement full AI behaviors for `BERSERKER`, `PATROL`, `STAY`, `PROTECT`, fix `GUARD` anchor, and `TACTICAL` tether | M4 | survey_explorer_4 |
| 39 | Backpack Auto-Feeding | Enable automatic consumption of food from `catPack` and `KittyFeedingStationBlock` when hunger < 60% or injured | M4 | survey_explorer_1 |
| 40 | Vector Normalization NaN Guard | Guard vector `normalize()` and distance division against zero-length vectors across combat and movement | M4 | survey_explorer_1 |
| 41 | Deterministic Production Evolution | Implement deterministic breeding tier evolution matrix (Standard -> Netherite) with zero random tier rolls | M5 | MASTER DIRECTIVE §8 |
| 42 | Strict Identity Separation | Enforce strict separation of Breed vs Skin vs Production Tier across entities, screens, and packets | M5 | MASTER DIRECTIVE §7 |
| 43 | Nekomata Dual-Tail & Perks | Spirit Catnip 100% guarantee; dual tail pivot attachment; dual strike; double harvest; soul-flame VFX | M5 | MASTER DIRECTIVE §9 |
| 44 | Derived Stat Engine Pipeline | Idempotent derived stat calculation engine (Base + Level + Talents + Equip + Buffs = Final) | M5 | MASTER DIRECTIVE §13 |
| 45 | Attack VFX & Bonuses Scaling | Add element/role-based visual effects and tier-scaled damage/defense/support bonuses and debuffs to cat attacks | M5 | Follow-up 2026-10-06T21:03:43Z |
| 46 | Hero Cape Model Offset Fix | In `TalentedCatAccessoryLayer.java`, change $Z$ coordinates from `-8.2F` to `-1.8F` (clasp) and `-1.9F` (drape) so cape sits on cat's back | M6 | Follow-up 2026-10-06T20:57:34Z, survey_spec_miner_5 |
| 47 | Cat Armor Remodel & Fitted Mesh | Refine `TalentedCatArmorModel` with open cowl, fitted barding harness, flank guards, paw cuffs; eliminate duplicate torso mesh on leggings | M6 | Follow-up 2026-10-06T20:58:51Z, survey_spec_miner_5 |
| 48 | Cat Armor Textures Polish | Polish 8 armor texture PNGs (`cat_armor_*.png`) with fitted straps, buckles, and open face/ear sections | M6 | survey_spec_miner_5 |
| 49 | Black Panther Blue Eyes Texture | Update `panther.png` and `skin_16.png` eye pixels $(5,6)$ and $(9,6)$ to bright blue (`#38BDF8`/`#0284C7`) | M6 | Follow-up 2026-10-06T20:58:51Z, survey_spec_miner_5 |
| 50 | PacketChangeCatSkin Clamping Fix | Change `totalSkins = 16` to `totalSkins = 17` in `PacketChangeCatSkin.java` to enable Panther skin 16 | M6 | survey_spec_miner_5 |
| 51 | Vanilla Cat Conversion Skin Map | Map `cat.getVariant()` to appropriate skin index in `ForgeEvents.java` when converting a vanilla cat | M6 | survey_spec_miner_5 |
| 52 | Creative Tab Deduplication | Remove duplicate `YARN_BALL` and `FEATHER_TOY` from `ModCreativeTabs.java` | M6 | survey_spec_miner_5 |
| 53 | Automated Log Scanner | Deploy `tools/verify_logs.py` to scan `run/logs/latest.log` for zero `Missing texture` or `Missing sound` warnings | M7 | Acceptance Criteria §3, survey_spec_miner_6 |
| 54 | Dedicated Server Verification | Verify headless compile, launch (`Done (`), and clean shutdown with zero `NoClassDefFoundError` crashes | M7 | Acceptance Criteria §2, survey_spec_miner_6 |
| 55 | Multiplayer Connection Verification | Connect client to dedicated server, spawn cat, interact, verify zero server-side console errors or desyncs | M7 | Acceptance Criteria §2, survey_spec_miner_6 |
| 56 | Twine Agent-as-Judge Verification | Programmatic verification of smooth Twine control, camera alignment, and zero desyncs | M7 | Acceptance Criteria §1, survey_spec_miner_6 |
| 57 | Overhauled AI Verification | Programmatic verification of 3 distinct overhauled AI behaviors (Torch Kitty, Creeper Warden, Fox Pounce) | M7 | Acceptance Criteria §1, survey_spec_miner_6 |
| 58 | Real Audit Command | Replace mock `/kittytalents audit` stub with genuine programmatic system verification | M7 | survey_spec_miner_6 |
| 59 | Forensic Integrity Audit | Independent audit by `teamwork_preview_auditor` confirming zero mock stubs, real implementations, and full test pass | M7 | System Requirements |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Sidedness & Server/Multiplayer Hardening | Features 1–5: `ClientPacketHandlers` proxy, client imports elimination, `PacketTeamCommand`, `PlayerEvent.StartTracking` sync, server test configs | none | PLANNED |
| M2 | Performance Sweep & TPS/Memory Optimization | Features 6–11: `FelineStatusEffects` livingTick, `CatPlaceTorchGoal` throttling, entity scan throttling, radar item fix, pathfinding throttling, particle sidedness | M1 | PLANNED |
| M3 | Twine System Debugging & Real-Time Controls | Features 12–23: Camera lerp, network echo suppression, dormant player body, fluid swimming, movement prediction, sprint/crouch, step-height, mount fix, 4-way dodge, perfect dodge, Pounce 2.0 | M1 | PLANNED |
| M4 | AI Logic Overhaul & Doggy Talents Parity | Features 24–40: 16 AI bug fixes (sit lock, DOCILE loop, Nine Lives, level 10 crash, scratcher timeout, NBT save, energy regen, ability registration, cliff leaping, whistle fixes, stub talents), goal priorities, missing modes, auto-feeding | M2 | PLANNED |
| M5 | Unified Stat Engine, Production Evolution & Attack VFX | Features 41–45: Deterministic production evolution, identity separation, Nekomata guarantees, derived stat engine, attack VFX & bonuses/debuffs scaling | M3, M4 | PLANNED |
| M6 | Asset Polish, Remodeling & Rendering Polish | Features 46–52: Hero cape back offset, armor remodeling & fitted mesh, armor textures, black panther blue eyes, skin packet clamp, vanilla taming skin map, creative tab deduplication | M1 | PLANNED |
| M7 | E2E Testing, Log Scanning & Victory Audit | Features 53–59: Headless server verification, multiplayer sync, log scanning (`verify_logs.py`), agent-as-judge Twine & AI tests, real audit command, forensic audit | M1–M6 | PLANNED |

---

## Interface Contracts

### 1. Common Packets ↔ Client Network Proxy (`ClientPacketHandlers`)
- **Package**: `com.kittytalents.client.network.ClientPacketHandlers` (Client-only)
- **Signatures**:
  ```java
  public static void handleSyncCatEquipment(int entityId, ItemStack weapon, ItemStack tool, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots, ItemStack accessory, List<ItemStack> cosmetics);
  public static void handleSyncTalents(int entityId, int skillPoints, Map<String, Integer> talentMap);
  public static void handleSyncTrustedPlayers(int entityId, Map<UUID, String> players);
  public static void handleSyncTwine(int catEntityId, boolean twined);
  ```
- **Invocation**: Packets invoke via double lambda:
  `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSync...(args));`

### 2. Derived Stat Engine ↔ Entity State
- **Class**: `com.kittytalents.combat.DerivedStatEngine`
- **Signatures**:
  ```java
  public static float calculateFinalStat(TalentedCat cat, CatStatType statType);
  public static void recalculateAllStats(TalentedCat cat);
  ```
- **Contract**: Idempotent; reads base, level, talents, equipment, active buffs, and status effects. Never stacks duplicate modifiers.

### 3. Production Breeding Engine ↔ Cat Genetics
- **Class**: `com.kittytalents.breeding.CatBreedingManager`
- **Signatures**:
  ```java
  public static ProductionTier getEvolutionTier(ProductionTier parent1, ProductionTier parent2);
  public static boolean isNekomataGuaranteed(TalentedCat parent1, TalentedCat parent2, ItemStack breedItem);
  ```
- **Contract**: Strictly deterministic. $Tier_N + Tier_N = Tier_{N+1}$. Spirit Catnip unconditionally yields Nekomata with $P=1.0$.

### 4. Attack Effects & Scaling Engine
- **Class**: `com.kittytalents.combat.CatAttackEffects`
- **Signatures**:
  ```java
  public static void applyAttackEffects(TalentedCat attacker, LivingEntity target, float baseDamage);
  ```
- **Contract**: Computes damage multipliers, applies elemental particle bursts, and applies status buffs/debuffs scaled by `CatRole` and `ProductionTier`.

---

## Code Layout

Existing source layout strictly respected:
```
src/main/java/com/kittytalents/
├── KittyTalents.java                         # Main mod entrypoint
├── ability/                                  # Active abilities (Pounce, Hiss, Purr, ShadowStrike, etc.)
├── block/                                    # Blocks & BlockEntities (Bowls, Beds, Baths, Scratchers, Habitats)
├── client/
│   ├── ClientModEvents.java                  # Client setup & renderer registrations
│   ├── ClientTwineHandler.java               # Twine camera, inputs, HUD overlay
│   ├── gui/                                  # Screens (CatTalentScreen, WhistleScreen, HabitatScreen)
│   ├── model/                                # 3D entity models
│   ├── network/ClientPacketHandlers.java     # Isolated client packet handling
│   └── renderer/                             # Layers (AccessoryLayer, ArmorLayer, WeaponLayer, TailLayer)
├── combat/                                   # Real-time combat, combos, dodge, derived stat engine, VFX
├── command/                                  # In-game commands (/kittytalents audit)
├── compat/                                   # Third-party integrations (TacZ, Apotheosis)
├── entity/
│   ├── TalentedCat.java                      # Core entity class
│   ├── TwineManager.java                     # Twine session tracking & safety
│   ├── CatMode.java                          # Companion modes enum
│   └── ai/                                   # AI Goals & Target selectors
├── event/
│   ├── ForgeEvents.java                      # Server/common event hooks
│   └── ModEvents.java                        # Common registry events
├── item/                                     # Items (Treats, Whistles, Twine, Toys, Accessories, Armor)
├── network/                                  # Sided network packets
├── registry/                                 # DeferredRegisters (Items, Blocks, Entities, Menus)
└── talent/                                   # Talent definitions & mechanics

tools/
└── verify_logs.py                            # Automated Log4j missing texture/sound scanner
```
