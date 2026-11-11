# Kitty Talents 4.0 plan

Status: **approved by the owner on 2026-11-09**, with all optional content and a 3D remake of blocks and accessories.
Detailed designs per phase are in `docs/4.0/`. Progress is logged at the bottom of this file.

## Theme

3.0 rebuilt the mod on a clean base (`OVERHAUL_PLAN.md`). 4.0 uses that base for four things:

1. **Open:** skins, talent numbers, modes and jobs can be changed with packs or addons, without editing the mod.
2. **Shared:** playing with other people's cats nearby feels deliberate, not accidental.
3. **Alive:** cats grow up, have personalities and bond with their owner; the mod's blocks and accessories look like
   real cat things instead of plain cubes.
4. **Smoother:** the rough edges found in the 3.0 playtests go away.

## Rules carried over from 3.0

- One phase at a time. Each phase ends with a green build, passing GameTests, and an in-game check by the owner.
- 3.0 worlds must load into 4.0 with nothing lost (cats, blocks placed in the world, items in chests).
- Server decides, client asks. No client-only changes to cat state.
- Every new feature gets a GameTest; every new screen text goes through `KTText`.
- Anything visual is checked in rendered images before it is called done.

## Branching

`v3.0.0` is tagged on master. 4.0 work happens on the `4.0` branch so 3.0 can still get fixes.

## Phases

### Phase 1: Loose ends (small)

- **Job picker:** the Home tab's job button shows each job's requirements and skips or greys out jobs the cat can't do.
  The cat's work status ("Unqualified", "Too hungry") shows on the screen.
- **Stalk always ends in a pounce:** remove the race between the stalk and melee goals (the one rare flaky test).
- **Status effects off every mob:** move `FelineStatusEffects` from persistent data to a capability, so statuses aren't
  written to disk on every mob.
- **Done when:** the stalk test passes 30 runs in a row, and a zombie with a status saves no Kitty Talents data once it expires.

### Phase 2: 3D blocks and accessories (large, visual)

- **Blocks:** every Kitty Talents block gets a proper 3D model with its own shape and hitbox instead of a cube: bowl,
  bed, scratcher, bath, rice mill, habitat, feeding station, incubator, work station, treat oven. Cat-themed details
  (paw prints, ears, fish bones, cushions, sisal rope).
- **Accessories and cosmetics:** everything a cat wears (crown, cape, collar charms, and the rest of the cosmetic
  items) is remodelled in 3D so it reads as a real object on the cat, attached to the model's anchor bones.
- **New block: Cat Tree.** A multi-level perch cats climb and rest on; a cat resting on it watches over the base.
- **Check harness:** the render harness also renders every block and every accessory on a cat, so each one is reviewed
  in images.
- **Done when:** every block and accessory has before/after images, no missing-model or missing-texture lines in the
  log, and blocks placed in a 3.0 world keep their contents and facing.

### Phase 3: Skins from packs (medium)

- Skins become a datapack registry (`data/<namespace>/kittytalents/skins/*.json`) that the server sends to players on join.
  Textures still come from a resource pack.
- `StyleTab.SKIN_NAMES` and `PacketChangeCatSkin.SKIN_COUNT` go away; the Style tab lists whatever the server has.
- Skins are saved by id (name), with the 17 old numbers mapped on load.
- **Done when:** a sample pack in `docs/examples/` adds a skin with no code, and a 3.0 cat keeps its skin.

### Phase 4: Tunable talents and abilities, plus new abilities (medium to large)

- Each talent's and ability's numbers (per-rank values, cooldowns, ranges, energy costs, max rank, costs) live in
  datapack JSON with the current values as defaults. The server sends them to clients so tooltips show the real numbers.
- **New abilities** for the elements that have few: Frost, Poison and Arcane (skills and at least one ultimate).
- **Done when:** changing one JSON value changes behaviour and the tooltip after `/reload` (GameTest), with no pack the
  numbers match 3.0 exactly, and every new ability has an icon, a GameTest and AI use.

### Phase 5: Drag-and-drop Gear (medium)

- The Gear tab gets real slots and the player's inventory, so items drag onto the cat. Shift-click moves items both ways.
- Slot rules (rank-gated armor, weapon types) are enforced on the server and shown as a tooltip on refusal.
- **Done when:** every slot accepts and rejects the right items by drag, shift-click and click, with tests for each rule.

### Phase 6: Living cats (large)

- **Kitten growth:** kittens start small with their own look, grow through stages over time (treats speed it up), and
  inherit one talent rank from a parent.
- **Personalities:** each cat has a trait (brave, lazy, curious, playful, shy, greedy...) that nudges its idle
  behaviour and gives one small bonus and one small quirk. Shown on the Home tab. Kittens can inherit a parent's trait.
- **Bond level:** time together, petting, feeding and fighting side by side raise a bond level that unlocks small perks
  and new petting / play interactions.
- **Done when:** each trait has a visible behaviour difference and a GameTest, growth stages render correctly in the
  render matrix, bond perks are tested, and 3.0 cats load as adults with a rolled trait and zero bond.

### Phase 7: Playing together (medium)

- **HUD:** co-owned cats show whose they are; optional cards for friends' cats near you.
- **Whistle:** a "mine / mine and co-owned" switch, and a favourites row for the commands you use most.
- **Co-owner roles:** the owner chooses what each co-owner may do (ride only, command, full).
- **Twine:** other players see a small marker over a twined cat and its sleeping owner.
- **Done when:** a two-player session (owner + co-owner) confirms each item, and permission tests cover the new roles.

### Phase 8: Addon modes and jobs (large, riskiest)

- `CatMode` and `CatJob` become registries instead of fixed lists, so addons can add both. Saves already use names.
- API goes to **2.0.0** (this changes types addons may use). `KittyTalentsCompat` is updated in the same phase.
- **Done when:** a test addon adds a mode and a job through the API only, and all 12 modes and 9 jobs behave as before.
- If this runs long, it moves to 4.1 without holding the release.

### Phase 9: Release (small)

- Stress test again (target: no slower than 3.0's ~2.4 ms per tick for 100 cats).
- Full regression run, README / PROJECT / EXTENDING updated, version `4.0.0`, tag.

## Order

1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9. Phase 4 follows 3 (same server-to-client sync). Phase 6 follows 4 (trait and bond
numbers are tunable from the start). Phase 8 is last so the release isn't blocked by it.

## Progress log

### 2026-11-09: Phase 1: Loose ends complete
- **Stalk to pounce transition:** Raised CatPounceGoal priority band to POUNCE = 2, placed CatStalkGoal at COMBAT = 3, and prevented CatMeleeGoal from engaging unaware targets beyond 2.5m. Completed stalks now seamlessly transition into pounce without goal racing. Verified with 30 consecutive successful runs of testStalksUnawareTargetThenPounces.
- **Status effects off persistent NBT:** Moved FelineStatusEffects to Forge Capability FelineStatusProvider.CAPABILITY on LivingEntity. Non-cat mobs no longer accumulate persistent NBT tags, and expired statuses leave zero residual data on disk. Added GameTest testZombieSavesNoDataAfterStatusExpires.
- **Job Picker UI & Work Status:** Added qualification check, detailed requirements tooltip (level, talent, TacZ), and automatic skipping of unqualified jobs on regular click (shift-click enables viewing locked jobs). If an unqualified job is active, the button reflects (Unqualified) with muted styling. Added visual work status badge on the screen header displaying Unqualified, Too Hungry, or active work status (Working, Scanning, etc.).
- **Verification:** All 31 JUnit tests pass, all 75 GameTests pass (including 30x stalk runs and zombie data cleanliness), and RenderMatrix produced 28 clean images with 0 missing texture errors.


### 2026-11-10: Phase 1 review (Claude)
- Removed double counting of kills and damage (doHurtTarget and ForgeEvents both recorded them); stats sync is now batched every 20 ticks.
- Network protocol bumped to "3" for Gemini's four new packets. PacketConductCat checks the owner before loading a chunk, retries for 40 ticks, and uses CatPermissions.
- `api/CatGoalBand` reads the CatBrain constants, so the API matches the new POUNCE band.
- Test world turns off mob loot: FETCH cats were chasing stress-test zombie drops in the self-defense test.

### 2026-11-10: Phase 2a: 3D blocks and the Cat Tree
- `tools/asset_gen/blocks_3d.py` writes models, blockstates and textures for all 10 blocks. Blocks face the player, with shapes rotated to match.
- The food bowl shows four fill levels. Seven blocks were missing loot tables; they now have them.
- New two-block Cat Tree with a recipe. GUARD and WANDER cats perch on it and mark nearby hostiles with Glowing.
- Four new GameTests.

### 2026-11-10: Phase 2b: 3D accessories
- `tools/asset_gen/accessories_3d.py` writes 11 accessory models in a JSON format that resource packs can replace.
- `AccessoryModel` animates cloth sway, floating and wing flaps. Accessory items render in 3D in inventories and hands.
- A real Elytra keeps the vanilla look. Before and after renders are in `docs/render-matrix/accessories-*`.

### 2026-11-10: Phase 3: Skins from packs
- Skins are datapack data: `data/<ns>/kittytalents/skins/*.json` with `name`, `texture`, `order` and `breed_default_for`.
  The 17 built-in skins were moved there from `assets/kittytalents/skins`, and the client-only SkinRegistry was removed.
- The server sends the skin list on join and after `/reload` (PacketSyncSkins). Network protocol is "4".
- Cats save the skin by name (`Skin`). CatSchema 3 maps the old `SkinId` numbers, with 0 becoming Default.
  Cats whose skin pack is missing show their default look and keep the id.
- PacketChangeCatSkin sends a string (an explicit id, or next/previous) and the server refuses ids it hasn't loaded.
- The Style tab lists Default plus every synced skin. The list pages, opens on the page with the current skin, and shows a page counter.
- Tests:
  - new GameTest testSkinsAreDatapackDrivenAndMigrate (legacy numbers, unknown id refused, pack skin applies and saves);
  - the legacy-save test now also checks SkinId 12 → snow_leopard;
  - SkinFilesTest checks the new folder and the lang names.
- Example pack in `docs/examples/sunset_skin_pack/`. The skin section of EXTENDING.md was rewritten.
- Also fixed: `gen_screen_lang.py` drops keys added after its generated block, which had removed the Producer Stick item names. Those names were restored above the block.
- Verification: build green, all 80 GameTests pass, Style skin page checked at GUI scales 2 and 3.

### 2026-11-10: Phase 4: Tunable talents and abilities, plus new abilities
- `core/tuning/Tuning` holds about 250 named numbers.
  - Each talent has `max_rank` and `cost_scale`, plus its per-rank values. The four talents whose logic is in the combat engine use `talent/TalentTuning`.
  - Each ability has `energy_cost` and `cooldown_ticks`, plus its damage, radius and status values.
  - Defaults are the 3.0 numbers. With no pack, the test confirms every value equals its default.
- Datapack files go in `data/<ns>/kittytalents/tuning/*.json`. Overrides are synced to players (PacketSyncTuning).
  After `/reload`, every loaded cat recalculates its attributes and talents.
- Commands: `/kittytalents tuning export` (writes all defaults as a pack file) and `/kittytalents tuning changed`.
- Tooltips:
  - Talents tab: hover the description for "Current numbers". If the description is cut short, the tooltip shows the full text first.
  - Combat tab: the hovered ability lists its values.
- New abilities, each with an icon (`tools/asset_gen/ability_icons_40.py`), an AI priority and a GameTest:
  - Frost: Ice Lance (ranged freeze), Blizzard (ultimate).
  - Poison: Toxic Cloud (the AI skips it against undead), Plague Bloom (ultimate, heals per foe).
  - Arcane: Arcane Blink (teleport behind a target and strike), Starfall (ultimate, up to six targets).
- Tests: KittyTuningGameTests (10). Covers defaults matching 3.0; one JSON change altering behaviour and the tooltip, with clamping and unknown names; a lowered max rank capping and refunding; each new ability; and the AI casting Ice Lance on its own.
- Example pack `docs/examples/tuning_pack/`; tuning section in EXTENDING.md.
- Verification: build green, all 90 GameTests pass, Talents tab checked at GUI scales 2 and 3.

### 2026-11-10: Phase 5: Drag-and-drop Gear
- `CatTalentMenu` has real slots: the cat's 10 gear slots (indices 0..9, backed by the cat through a Container) and the player's 36 inventory slots.
  - Vanilla container clicks now handle drag, shift-click, right-click and swapping, and sync to the client.
  - The client places slots by replacing each Slot at the same index, because Slot x/y are final. Slots are active only on the Gear tab.
- `core/gear/CatGearRules` holds the slot rules: the item type for each slot, the Cat Armory and Cat Armor rank gates, and the refusal reason. The slots and the old click packet both use them.
  - Gear slots also require the EQUIP permission and hold one item.
  - Shift-click moves one item per click (no filling all three cosmetic slots from one stack).
- The Gear tab is redrawn as a gear block around a larger portrait, plus the player's inventory with a stats line and a hint.
  - Hovering an empty slot names it.
  - Hovering a slot while holding an item it refuses shows the reason in red: wrong slot, the rank needed, or owner only.
- Fixes found along the way:
  - Icon-only KTButtons drew a stray "…".
  - 3D accessory item icons sat off-centre. The renderer now centres and sizes each model from its real box bounds.
- Tests: KittyGearGameTests (4). Covers each slot accepting and rejecting by drag; rank gates by drag, shift-click and click; shift-click both ways with swaps; and strangers being refused.
- Verification: build green, all 94 GameTests pass, Gear tab and item icons checked at GUI scales 2 and 3.

### 2026-11-10: Phase 6: Living cats
- **Growth:** `core/living/GrowthStage` (Newborn, Kitten, Junior, Adult), derived from the vanilla age and synced to clients.
  - Each stage has its own body size and head size (kittens have big heads). Kittens have less health and damage until grown.
  - Fish skips 10% of the remaining growing time and a treat skips 25%.
  - A kitten is born with rank 1 in one talent a parent trained.
- **Personalities:** `core/living/CatTrait` (Brave, Lazy, Curious, Playful, Shy, Greedy), each with one bonus, one quirk and one idle habit (`ai/goal/CatTraitGoal`).
  - Rolled on natural spawn, on taming and on the first load of a pre-4.0 cat.
  - Kittens take a parent's trait 80% of the time.
- **Bond:** `core/living/CatBond`, levels 0–10 from time together, petting, feeding, play and kills near the owner, with a cooldown per kind.
  - Perks: Comfort (2), Playtime with String (3), Quick Learner (5), Kindred (7), Soulmates (10).
- All numbers are tunable in new tuning sections `traits`, `growth` and `bond`.
- Home tab: a bond bar with the bond level and personality. Hovering shows the bonus, quirk, perks and growth stage.
- Tests: KittyLivingGameTests (10). Render matrix has four growth combos, including a kitten with a hat.
- Verification: build green, all 104 GameTests pass, growth renders and the Home tab checked.

### 2026-11-10: Playtest fixes (between Phases 6 and 7)
- **Twined gun aim:** a Twined shot used the rotation of the player's sleeping body. It now uses the cat's view rotation; sneaking aims down sights. Not testable without TacZ in the test world, so this needs an in-game check.
- **Sorter / Courier reach:** "arrived" was any spot within 3 blocks, so a cat above an underground chest worked through the floor. Container jobs now need a clear line to the container and path to a free spot beside it. Deposits do the same, with a fallback after about 5 seconds stuck.
- **Riding:** mounting stands the cat up (Docile or Stay becomes Follow). A ridden cat ignores sit orders. Kittens and downed cats can't be ridden.
- **Twine body:** the dormant body used only the vanilla invisibility flag, which still draws armor and held items. The server now sends the set of dormant bodies (PacketSyncTwineBodies) and clients skip drawing them.
- Tests: KittyPlaytestFixGameTests (2). All 106 GameTests pass.

### 2026-11-10: Phase 6 closed
- Added the tests that were missing: hunger rates for Greedy and Playful, bond from a kill near the owner, and bond from a minute together (and none while the owner is away). KittyLivingGameTests is now 12 tests.
- EXTENDING.md documents the `traits`, `growth` and `bond` tuning sections.
- Growth renders saved in `docs/render-matrix/growth/`.
- Every "done when" item is met: each trait has a visible habit and a test, growth stages render correctly, bond perks are tested, and pre-4.0 cats load as adults with a rolled trait and zero bond.
- Verification: build green, all 109 GameTests pass. Still waiting on the user's in-game check.

### 2026-11-11: Phase 7: Playing together
- The user confirmed in game: bond bar, Twined gun aim, and cats standing up for a rider all work.
- **Co-owner roles:** `core/perm/TrustRole` (Rider, Handler, Full). Each `CatAction` names the lowest role that may do it; `CatPermissions.can` uses the co-owner's own role.
  - Rider: ride and feed. Handler: also modes, sit, whistle and Twine. Full: also gear, pack, style, loadout and jobs.
  - Co-owners saved before 4.0 load as Full, which is what a co-owner could do in 3.0.
  - The owner changes a role by clicking the co-owner's chip on the Home tab (PacketManageTrust SET_ROLE). A demotion ends that player's Twine session.
  - Right-click interactions follow the role too.
- **Whistle:** a "Mine only / Mine + co-owned" switch, and a favourites row (right-click a command to pin up to six). Both are saved per player in `config/kittytalents-client-prefs.json`.
- **HUD:** cards for cats you don't own name the owner. A client option (`showFriendCats`, off by default) adds cards for cats of players you share a cat with.
- **Twine markers:** other players see "Twined: name" over the cat and "name (Twined)" where the body rests (`showTwineMarkers`, on by default).
- Tests: KittyTogetherGameTests (4): the role matrix, roles through packets, the gear menu, whistle and clicks, roles in saves, and the whistle scope.
- Verification: build green, all 113 GameTests pass, Home tab and whistle screen checked.
- Not yet done: the two-player check of the HUD labels and Twine markers, which can only be seen with a second player.
