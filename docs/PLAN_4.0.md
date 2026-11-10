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

