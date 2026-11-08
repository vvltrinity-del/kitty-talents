# Kitty Talents — Audit & Architecture Overhaul Plan

> **Goal:** find everything in 1.0, 2.0 and 3.0 that is missing, dead, or poorly built, then rebuild the
> architecture so 3.0 can actually ship and **4.0 can be added without touching the core**.
> **Priorities (user-selected):** AI & companion behavior, Visuals & UI.
> **Date:** 2026-11-07 · **Baseline commit:** `8ea19f6`

---

## 0. How this audit was done (and its limits)

- **Static audit only.** I read the code, traced references, and checked registration. I did **not** run the game.
  The build is currently broken on this machine (see §0.1), so nothing below is "verified in-game".
- Status legend used throughout:
  - **DEAD**: code exists but nothing calls it, so it never runs.
  - **BROKEN**: it runs, but it's wrong or contradicts its own spec.
  - **SHALLOW**: wired up, but the implementation is thin or hard-coded in the wrong place.
  - **WIRED**: reachable and looks plausible. Still needs an in-game check.
- The `.agents/teamwork` history shows 3.0 Batch 2 **never passed its gate** (`orchestrator_3/GATE_STATUS.md` = PENDING),
  and no 3.0 jar was ever built (`build/libs` only has 1.0.0 and 2.0.0). Treat every "PASS" claim in agent
  handoffs as unverified.

### 0.1 Blocker: the build environment is broken

`JAVA_HOME` points to `C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`, which no longer exists.
The only JDK installed is `jdk-21.0.13.21-hotspot`, and `build.gradle` asks for a **Java 17 toolchain**.
**Fix before anything else:** install a JDK 17 (Temurin or Microsoft), point `JAVA_HOME` at it, then run
`gradlew build` and `gradlew runClient` to get a real baseline.

---

## 1. Audit findings

### 1.1 Codebase health (all versions)

| Finding | Status | Evidence |
|---|---|---|
| `TalentedCat.java` is a 2,634-line god class: stats, talents, equipment, NBT, Twine, breeding, mount, flight and every talent effect in `tick()` | SHALLOW | `entity/TalentedCat.java` (talent effects ~L1300–1960) |
| `CatTalentScreen.java` is 1,180 lines, with all 7 tabs in one class | SHALLOW | `client/gui/CatTalentScreen.java` |
| **Entire `breeding/` package is dead.** `breeding/CatBreedingManager`, `BreedingMatrix` and `FelineGenetics` have 0 callers; the live one is `entity/CatBreedingManager` | DEAD | grep: 0 refs to `breeding.CatBreedingManager` |
| Duplicate TacZ compat: `compat/TacZCompat.java` vs `compat/tacz/TacZCompat.java`, both used | SHALLOW | 3 files use the old one, 12 the new |
| `combat/CatAttackEffects` (the whole "attack VFX + element/tier scaling" feature, PROJECT #45) is never called | DEAD | 0 refs |
| `command/KittyRPGCommand` is never registered | DEAD | 0 refs |
| Ability cooldowns, `MutationType` and `SpiritCatnipUsed` are stored in `getPersistentData()` with string keys built on every call | BROKEN | `TalentedCat.java:218,225,518,2566` |
| No NBT schema version or migrator, so a future format change will silently reset cats | missing | `addAdditionalSaveData` L2270 |
| Renderer allocates a `new ResourceLocation` every frame for every cat | SHALLOW | `TalentedCatRenderer.getTextureLocation` |
| Renderer reads `ClientTwineHandler` static fields directly (tight coupling) | SHALLOW | `TalentedCatRenderer.render` |
| Junk files at repo root: `Test.java`, `test.png`, `white_base.png`, `all_black_base.png`, 4 one-off Python generators | cleanup | repo root |
| Only 3 JUnit tests plus a thin GameTest set | missing | `src/test`, `test/KittyTalentsGameTests.java` |
| The sibling `KittyTalentsCompat` project was never updated for 3.0 (follow-up request 2026-10-07T03:28) | missing | `../KittyTalentsCompat` |

### 1.2 Version 1.0 features (from README)

| Feature | Status | Notes |
|---|---|---|
| Leveling 1–100, rank-scaled skill cost, respec elixir | WIRED | verify cost curve in-game |
| AI modes Docile/Follow/Wander/Hunting/Guard/Aggressive | WIRED | but see AI conflicts in §1.4 |
| Fox Pounce (`CatFoxPounceGoal`) | WIRED | |
| Cat Mount with rank scaling | WIRED | |
| Cat Armory (sword in mouth) and Feline Armor | WIRED | armor was remodeled in 3.0; needs a visual check |
| **Nine Lives** | **BROKEN** | Still unlimited: every lethal hit is cancelled with no charges and no cooldown (`TalentedCat.java:1453–1463`). The PROJECT #27 fix was never done. |
| Dire Treat gating, Sky Feline, Mole Cat, Frenzy Focus | WIRED | `CatMoleMiningGoal` and `CatFisherGoal` were **deleted** in 2.0 and replaced by Jobs. Check that Mole Cat and Fisher Cat still do something outside WORK mode. |
| Frenzy Milk, collars, naming, 16 skins with 3D preview | WIRED | |
| **Home Whiskers** talent | **DEAD** | Only feeds the role score in `CatRoleEngine`. No gameplay effect. |

### 1.3 Version 2.0 features (commit `101eeb4`)

| Feature | Status | Notes |
|---|---|---|
| Worker engine plus 9 jobs (Farmer, Lumberjack, Miner, Rancher, Courier, Sorter, Fisher, Collector, Gunsmith) | WIRED | All 9 goals are registered at the same priority (7). Each one ticks `canUse()` on every cat, even cats not in WORK mode. |
| Work Station, Treat Oven, Habitat recipes | WIRED | |
| TacZ guns, ammo from the pack, Marksman/Quick Reload/Ammo Scavenger | WIRED | Gun→sword fallback (3.0 request) needs an in-game check |
| JEI / Jade / Curios / Create / Patchouli compat | WIRED | soft-fail when mods are absent still unverified |
| GameTests | SHALLOW | `KittyTalentsGameTests.java` is 86 lines |

### 1.4 Version 3.0 features (R1–R40 spec), AI & visuals first

**AI (priority area)**

| Issue | Status | Evidence |
|---|---|---|
| **No central targeting.** `CatCentralTargetGoal` (R12) is added *next to* 9 vanilla target goals (OwnerHurtBy, OwnerHurt, TrustedHurtBy ×2, HurtBy, 4× NearestAttackable, Mouser), so they still fight each other | BROKEN | `TalentedCat.java:376–412`, `CatAIInitializer.java` |
| **Two follow systems.** `CatFollowAndEscortGoal` (prio 5) and vanilla `FollowOwnerGoal` (prio 6) both run | BROKEN | |
| **AI cats never use abilities.** `ai/UseAbilityGoal` is never registered, so abilities only fire from Twine or team-command packets | DEAD | 0 refs |
| `CatPounceAttackGoal` is never registered (separate from FoxPounce) | DEAD | 0 refs |
| Modes **DEFENSIVE, ESCORT, SUPPORT, PROTECT** have zero behavior references; **BERSERKER, FETCH, MINE, FARM, FISH, TACTICAL** have 1–3 | BROKEN | cycling the whistle into them does nothing distinct |
| Goals are registered from 3 places, and 3 goal *constructors* call `CatAIInitializer.initializeGoals()` as a side effect, guarded by a flag saved in persistent NBT | BROKEN | `CatComfortGoal:18`, `CatFoxPounceGoal:28`, `CatRestOnBedGoal:24` |
| Priority collisions: FoxPounce, Gun and Breed at 2; Melee and BackpackFeed at 3; Stalk and CreeperWarden at 4; Follow ×2 and RestOnBed at 5–6; 3 goals plus 9 jobs at 6–7 | BROKEN | |
| Talent effects scan entities separately (`getEntitiesOfClass` ×8 in `tick()`), so there's no shared sensor | SHALLOW | `TalentedCat.java:1772–1957` |

**Visuals & UI (priority area)**

| Issue | Status | Evidence |
|---|---|---|
| Model is vanilla `CatModel` with thin getters. Nekomata's second tail re-renders the **same** tail parts, so it can't be posed independently | SHALLOW | `TalentedCatModel.renderSecondTail` |
| Skin and breed are tangled: `skinId == 0` means "use the breed texture", and ids 11–16 are hard-coded `if/else` | SHALLOW | `TalentedCatRenderer.java:42–70` |
| Only 7 custom cat textures. Panther blue eyes and Snow Leopard eye fix need a visual check | unverified | `textures/entity/cat/` |
| Cosmetics use hard-coded offsets per item (the cape bug class) instead of bone anchor points | SHALLOW | `TalentedCatAccessoryLayer.java` |
| The HUD is drawn in a `RenderGuiOverlayEvent.Post` handler inside the 741-line Twine handler, not a registered overlay | SHALLOW | `ClientTwineHandler.java:459` |
| Element descriptions in the GUI are hard-coded strings, not lang keys | SHALLOW | `CatTalentScreen.java:997` |

**Other 3.0**

| Item | Status |
|---|---|
| Abilities (12 skills + 5 ultimates) exist and run through `tryCast` | WIRED (Twine/command only) |
| Energy engine, elements, statuses, roles | WIRED; role is *computed for display*, not used by AI |
| Breeding: live manager says "zero random" but uses `nextFloat() < 0.10` at L221 | check: probably the mutation roll, confirm it's not tier |
| Neko Night, natural spawns, tier-upgrade treats, Deep Fried Chicken | WIRED |
| Network security, persistence, multiplayer (Batch 2) | unverified, since the gate never passed |

---

## 2. Target architecture (built for 4.0)

Design principle: **the entity is a thin shell; behavior lives in registries, components, and data.**
Adding a 4.0 talent, ability, mode, skin, job, or element should mean *adding a file*, not editing `TalentedCat`.

```
com.kittytalents
├── api/                    ← PUBLIC, stable for 4.0 & KittyTalentsCompat
│   ├── registry/           KTRegistries: TALENTS, ABILITIES, MODES, JOBS, ELEMENTS, STATUSES, SKINS, BEHAVIORS
│   ├── cat/                ICat (read-only view), CatStatType, CatCapability (CAN_FLY, CAN_DIG…)
│   ├── talent/             Talent, TalentHook (onTick/onHurt/onAttack/onKill/statMods/capabilities)
│   ├── ability/            Ability, AbilityContext (caster = AI | TWINE | COMMAND), Targeting
│   ├── ai/                 CatBehavior, ModeProfile
│   └── event/              CatLevelUpEvent, CatAbilityCastEvent, CatTargetEvent, CatRewardEvent…
├── core/
│   ├── entity/TalentedCat  ~400 lines: owns components, delegates
│   ├── component/          CatProgression, CatTalents, CatEquipment, CatEnergy, CatStatus,
│   │                       CatIdentity(breed/skin/gender), CatProduction, CatTwineState, CatWellbeing
│   ├── stats/              DerivedStatEngine (single source of truth → attribute modifiers)
│   ├── persist/            CatSchema (version int) + Migrators (v1→v2→v3…), with tests
│   └── perm/               CatPermissions: OWNER / TRUSTED / STRANGER × Action matrix
├── ai/
│   ├── CatBrain            the ONE place goals are registered; priority bands are constants
│   ├── sensor/             CatSensors: shared cached scans (hostiles, items, players, POIs) every N ticks
│   ├── targeting/          CatTargeting: single scorer (mode, owner threat, distance, LOS, weapon range…)
│   ├── mode/               one ModeProfile per mode: which behaviors are enabled + params
│   └── behavior/           Follow, Guard, Patrol, Hunt, Retreat, UseAbility, Pounce, Gun, Groom, Rest, Eat, Fetch…
├── content/                built-in talents/abilities/modes/jobs, each its own class, registered into api registries
│                           numbers in data/kittytalents/talents/*.json (datapack-tunable)
├── network/                PacketBase: decode → validate → authorize(CatPermissions) → apply → sync
├── client/
│   ├── model/              KTCatModel (own LayerDefinition: real 2nd tail bone, anchor bones for cosmetics)
│   ├── anim/               CatAnimator (idle/walk/run/pounce/dodge/sit/sleep/groom/cast)
│   ├── render/             renderer + layers; texture lookup via cached SkinRegistry
│   ├── skin/               SkinRegistry loaded from assets/kittytalents/skins/*.json (no code per skin)
│   ├── gui/framework/      KTScreen + Tab + widgets (bars, slot grid, ability card, 3D cat preview)
│   ├── gui/tabs/           HomeTab, TalentsTab, GearTab, StyleTab, StatsTab, CombatTab, TwineTab
│   ├── hud/                registered IGuiOverlay: CompanionHud, TwineHud
│   └── twine/              TwineCamera, TwineInput, ClientTwineState (renderer reads this, not the handler)
└── compat/                 one package per mod, each behind a ModList check, with soft-fail
```

### Key decisions

1. **AI = one Brain, profiles, shared sensors.** `CatBrain.install(cat)` is the *only* place goals are registered,
   and it runs on every construction with no persisted flag. Modes become `ModeProfile` objects that enable or disable
   behaviors, so a new 4.0 mode is a new profile plus a lang key. All vanilla target goals are removed in favor of
   `CatTargeting`. Sensors run once every 10–20 ticks and every behavior reads the cached result.
   Distant, sitting, and working cats tick on a reduced schedule (AI level of detail).
2. **Talents are hook objects, not `if` branches in `tick()`.** Each talent implements only the hooks it needs.
   `TalentedCat.tick()` just runs `talents.forEachHook(onTick)`. This removes about 700 lines from the entity.
3. **Abilities are caster-agnostic.** The same `Ability.cast(AbilityContext)` is used by the AI `UseAbility` behavior,
   Twine input, and whistle commands, with role and element feeding AI scoring. That gives real AI and Twine parity.
4. **Own model instead of vanilla `CatModel`.** We define the bones ourselves, so the Nekomata tail is a real
   child of the tail root, and cosmetics/armor attach to named **anchor bones** (`head_top`, `back`, `neck`, `tail_root`,
   `mouth`) instead of magic offsets. **Open question:** GeckoLib for animations (more expressive, adds a dependency)
   vs a hand-written `CatAnimator` (no dependency). Recommendation: hand-written for 3.0; revisit GeckoLib in 4.0
   if keyframe animation is needed.
5. **Data-driven skins.** Each skin is a JSON file with `id`, `texture`, `eyes_emissive?`, `breed_default_for?`.
   Breed, skin, and production tier become three separate fields in `CatIdentity`.
6. **Versioned persistence.** Write `kt_schema` into NBT and run migrators on load. Old 1.0, 2.0, and partial-3.0 cats must load.
   Everything currently stored in `getPersistentData()` moves into real component fields.
7. **One packet pipeline and one permission matrix.** Every packet extends `PacketBase` and declares the `Action`
   it needs. `CatPermissions` decides (owner vs trusted vs stranger), so there are no ad-hoc checks.

---

## 2.5 Progress log

### 2026-11-07: Phase 0 done, Phase 1 hotfixes done (in-game walkthrough still pending)
- **Build:** works on JDK 17 from the Gradle cache. Use
  `JAVA_HOME=C:\Users\VivianV\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1`
  (the system `JAVA_HOME` still points to a deleted JDK 21; fix that in Windows environment variables).
- **GameTests had never run.** The `empty` template structure was missing, so the test server crashed before
  running any test. Added `data/kittytalents/structures/empty.nbt` (generator: `tools/gen_gametest_structures.py`).
  Baseline was 3/4 passing. **Now 8/8 passing.**
- **Dedicated server:** boots to `Done (2.6s)` and stops cleanly (`tools/test_server_boot.py`).
- **Cleanup:** removed root junk (`Test.java`, `test.png`, base PNGs); moved the Python generators to `tools/asset_gen/` (paths fixed).
- **Fixes (each covered by a new GameTest):**
  - Clocking in a cat with a job now always switches it to WORK mode (`setClockedIn` enforces it; the test was failing before).
  - **Nine Lives** revive now has a cooldown (10 min − 1 min/rank, saved to NBT as `NineLivesReadyAt`). A lethal hit while it's recharging downs the cat.
  - Downed cats still take void and `/kill` damage (they used to fall forever).
  - **NEW BUG FOUND AND FIXED:** untamed cats were immune to all environmental damage when friendly fire was off
    (`source.getEntity() == getOwner()` was `null == null`).
  - `UseAbilityGoal` is now registered: AI cats cast abilities, but only in combat, skip UTILITY abilities, and need priority ≥ 1.0.

### 2026-11-07: Phase 2 (core foundation) done. Commits `51d15e4` → `b6bc3d2`
- **Tests are real now.** Added JUnit 5. The three old "tests" were `main()` classes that never ran (one tested the dead
  breeding package). Now: **24 JUnit + 11 GameTests passing**; the GameTest suite was run 3× to confirm it's stable.
- **Test infrastructure:** `tools/run_gametests.sh` fails unless it sees "All N required tests passed"
  (`runGameTestServer` exits 0 even when the server can't start). GameTests run in `run-gametest/` and the dedicated
  server in `run-server/` (port 25599), so neither collides with a dev client running in `run/`.
- **Dead code removed:** `breeding/` package, `compat/TacZCompat` forwarder.
- **Versioned saves:** `core/persist/CatSchema` (schema 1). CatMode is saved **by name**, and legacy ordinals are decoded
  from a frozen table. Cooldowns, the Spirit Catnip flag and combat scratch state moved out of Forge persistent data.
  Covered by a JUnit test and a GameTest that loads a hand-built 2.0 save.
- **Components:** `core/component/` holds CatTalentLevels, CatEquipment, CatStatistics, CatTrust and CatCombatState,
  with the same NBT keys as before. `TalentedCat` keeps thin delegates, so none of the ~100 call sites changed.
  Interactions moved to `entity/interact/CatInteractions`.
- **TalentedCat: 2,634 → 2,194 lines.** The "<600" target was too early for Phase 2. The remaining bulk is
  `registerGoals` (moves into `CatBrain` in P3) and the talent effects in `tick()`/`hurt()`/`doHurtTarget()` (become
  hooks in P4). The `api/` registries also move to P4, where they're first used.
- **Bugs fixed during Phase 2 (each with a test):**
  - **Skill-point exploit:** respec refunded the free rank-5 baseline of the 4 standard talents, so every respec
    minted about 32 free points.
  - AI cats could almost never cast abilities mid-fight (`LOOK` flag clash with melee at equal priority).
  - Venom Fang's AI range (5 blocks) didn't match its bite range (4 blocks).
  - Dodged hits counted as damage taken.

### 2026-11-07: Phase 3 (AI rewrite) done. Commits `ffd7004` → `738ca71` + stress test
- **12 modes** (DOCILE, STAY, FOLLOW, WANDER, GUARD, HUNTING, AGGRESSIVE, TACTICAL, SUPPORT, FETCH, WORK, RETURN_HOME).
  Each one declares its follow / anchor / target-policy / retreat behavior as data. Legacy names map on load
  (ESCORT, PROTECT, DEFENSIVE, RETREAT → FOLLOW; PATROL → GUARD; BERSERKER → AGGRESSIVE; MINE, FARM, FISH → WORK + job).
- **`ai/CatBrain`** is the only place goals are registered, in documented priority bands. **`ai/CatTargeting`**
  is the single friend/foe and scoring rule; `CatTargetGoal` replaces 10 competing target goals.
- **Pounce** is every cat's default combat opener (stalk, then leap via the same engine as Twine). The talent ("Pounce
  Training") only scales it, and it's no longer an equippable ability (schema v2 clears old slots).
- STAY/GUARD anchor where ordered (saved). There's now one follow goal. Low-health retreat is automatic (never in AGGRESSIVE).
- **Bugs fixed:**
  - Owner/trusted threat checks compared another entity's timestamps with the cat's clock.
  - Pounce landings hit villagers, pets and bystanders.
  - Fox pounce hit targets it missed.
  - Vanilla follow fought the GUARD anchor.
  - BERSERKER + Clocked In at the same time.
  - **The whole Combat tab was client-only.** Role, element and ability slots never reached the server; now there's
    a validated `PacketSetCatLoadout` plus a sync back to clients.
- **Test infrastructure bugs found and fixed:**
  - Every GameTest had spawned entities *inside* the floor (the structure sits 1 block above the helper origin).
  - Orphaned dedicated-server processes from the boot script were the real cause of the earlier "run/ locked" and
    "port 25565 taken" failures.
- **Tests:** 27 GameTests (12 new AI tests covering every mode policy, the pounce, bystander safety, retreat and anchors)
  plus JUnit `CatModeTest`.
- **Performance:** 100 cats in mixed modes plus 10 hostiles average **6.1 ms per server tick** (the budget is 50 ms).
  The stress test fails above 25 ms.
- **Not covered by automated tests (needs an in-game check):** anything that depends on a real owner, i.e. FOLLOW,
  TACTICAL and SUPPORT tethering and teleport-to-owner, and defending the owner. GameTest fake players aren't
  resolvable as owners.

### 2026-11-07: Phase 4 (talents, mastery, XP) done. Commits `b4f6db3` and Phase 4b
- **Talent hooks:** `talent/effect/` holds the `TalentEffect` interface (tick with a staggered interval, outgoing and
  incoming damage, on-hit, attributes, kill, owner-hurt, owner-fall) and the `TalentEngine` dispatcher. About 45 talents
  moved out of `TalentedCat` and the deleted `TalentGameplayHandler` into small classes grouped by category. Talents whose
  gameplay belongs to a system (jobs, goals, TacZ, Pounce) stay there and check `TalentEngine.isMastered(...)`.
- **Mastery perks:** all 51 talents have one at max rank, shown in the Talents tab (gold when earned) and listed in
  `en_us.json` as `talent.kittytalents.<id>.mastery`. `LangCompletenessTest` enforces them. Standard talents must be *trained* to max (the free rank 5 doesn't count); Nine Lives max is 5 → 8.
- **XP:** `core/progression/CatProgression`. Every kill credited to the cat (melee, pounce, abilities, guns) gives XP
  (hostiles ≈ max HP / 2, bosses ×5), and so does each job task. Levels give skill points, the cap is respected, and the Home tab shows XP.
- **Abilities:** hostile AoEs and AI enemy counts use `CatTargeting.canHarm`, heals use `isFriendly`; Venom Fang is skipped against undead.
- **`CatAttackEffects`** (role effects, tier bleed, element visuals) is wired into melee hits.
- **About 12 bugs fixed**, listed in the `b4f6db3` commit message: the Aqua Kitty boost was applied twice and accelerated
  forever; Gate Purr left gates open; the Catnip burst re-triggered itself; Hunter Cat's "double drops" was never
  implemented; Home Whiskers had no effect; and more.
- **Tests:** 35 GameTests (8 new). 100-cat stress: **~4.9 ms/tick** (was 6.1).
- **Deferred:** talent numbers in datapack JSON (talents are now one small class each, so this is a mechanical follow-up,
  best done together with the 4.0 `api/` registries), and moving status effects from Forge persistent data to a capability.

### 2026-11-08: Phase 5 (visuals) part 1: render matrix, own rig, anchors, skin JSON. Commits `5cfc9cb` to `b57e234`
- **Render matrix:** `./gradlew runRenderMatrix` (wrapped by `tools/run_render_matrix.sh`) opens a client in `run-render/`,
  quick-plays a copy of the server test world, renders 21 cat combinations from 4 angles offscreen and quits (about a
  minute; a game window appears). Output: `run-render/matrix/*.png`; `tools/matrix_sheets.py` makes contact sheets.
  Only active with `-Dkittytalents.renderMatrix`. Baseline, results and a side-by-side are in `docs/render-matrix/`.
- **Own rig:** `client/model/KTCatModel` (vanilla proportions and UVs, own LayerDefinitions) with anchor bones
  `head_top`, `mouth`, `back`, `neck`, `tail_root`. The tail is a real chain, and the Nekomata second tail is a mirrored
  sibling chain on `tail_root`. `client/anim/CatAnimator` reproduces the vanilla poses and adds an idle tail sway and the twin-tail splay.
- **Armor** is a fitted shell on the same skeleton (helmet, chest, hind legs, front legs). The armor textures were
  already drawn for the vanilla cat UV layout; the old model sampled them with the wrong UVs, which caused the
  "deformed netherite" look. **Layers** use anchors: mouth items have the grip between the teeth, cape/pack/wings sit on
  `back`, scarf/bowtie on `neck` (the scarf tassel no longer reaches the floor), hats on `head_top`.
- **Skins:** `assets/kittytalents/skins/*.json` and a cached `client/skin/SkinRegistry` reload listener; the renderer's
  if/else chain is gone. `SkinFilesTest` guards the files.
- Verified: build (JUnit), 38/38 GameTests, dedicated server boot, render matrix with 0 texture warnings.
- **Still open in Phase 5:** CatAnimator only covers the vanilla poses and the tails; the R32 pounce/dodge/sleep/groom/cast
  animations aren't written yet. Snow Leopard still looks like a grey tabby (it needs a texture pass with rosettes).
  The chest armor textures are mostly transparent on top, so fur shows through the back plates (a texture-art issue, not
  geometry). Sky Feline wings use the grey vanilla elytra texture. Nothing has been checked in the live game yet: the
  matrix uses the inventory renderer, so walking, sprinting and lying down need an in-world check at the phase gate.

### 2026-11-08: Phase 5 part 2: animations and textures. Commit `d30f07f`
- **One-shot animations:** pounce leap, dodge, ability cast and groom. The server broadcasts entity events 60–63
  (`TalentedCat.playAnimation`), and `CatAnimator` times and blends them client-side. Stalking uses the synced crouch
  pose. Sleeping on the bed now curls up (vanilla "lying" state + the side roll in the renderer).
- **Textures** (`tools/asset_gen/phase5_textures.py`): Snow Leopard has rosettes and a ringed tail, armor back plates are
  filled in for every material, and Sky Feline has its own blue wing texture.
- **Skipped at the user's request:** new skins (their idea was Pokémon-style skins; copies of Pokémon designs aren't
  possible, and they dropped the request).
- **Phase 5 is complete pending the user's in-game playtest** (walking, sprinting, sleeping, pouncing and Twine in the
  live world; the render matrix can't show motion).

### 2026-11-08: Playtest round 2 fixes. Commits `820ff76` → `a10c1ae`
- **Spirit Catnip never made a Nekomata:** vanilla breeding consumed it before the item's own logic ran. Fixed, with a test.
- **Dire Treat:** the recipe no longer needs a Nether Star (Master Catnip Treat + Ghast Tear + Diamond), and only one
  treat is needed (it awakens the cat). Dire ranks used to consume a treat each, 26 Nether Stars total.
- **Production tiers** scale health, armor, toughness and damage (Netherite +36 HP, +9 armor, +6.75 damage). The tier
  enum was out of order (Iron listed before Coal), which broke Tier Upgrade Treats, the Variant Chart, attack effects and breeding.
- **Starting skill points:** tamed cats get enough to max any one non-Dire talent (17), once. Existing cats get it on load.
- **Forests:** Cat Mount size applies only while ridden; a stuck follower teleports to its owner after 2 seconds.
- **Sleep:** the bed and Return Home goals no longer order the cat to sit, which had cut the lie-down off.
- **AI stalking** (crouched creep toward unaware targets) and **AI dodging** (projectiles, swelling creepers, melee swings).
- **Armor:** per-slot shell sizes (no z-fighting), shorter chest shell (no rump plate), filled leggings bottoms.
  Snow Leopard redone in a softer style.
- **Follow-up:** after a stalk, melee sometimes reaches the target before the pounce starts (a race between goals at
  different priorities). Worth tightening so a completed stalk always ends in a pounce.

### 2026-11-08: Phase 6 part 1: companion screen redesign
- **Look:** wood and leather theme from the user's mockups. Dark-oak frame with iron rivets, spruce title strip
  ("TALENTED CAT — section"), folder tabs with coloured labels on top, stitched-leather panels, stone buttons with gold
  (TRAIN), amethyst (TWINE) and red-brick (Respec) accents. The block textures come from the game, so resource packs restyle the screen.
  Theme code: `client/gui/theme` (`KTTheme` drawing helpers, `KTButton`, `KTWidget`, `KTText`).
- **Structure:** `CatTalentScreen` is now a 255-line shell (geometry, tabs, title strip, text-field focus, refresh hooks).
  Each page is a class in `client/gui/tab` (Home, Talents, Gear, Style, Stats, Combat, Twine). Tab objects outlive
  widget rebuilds, so the selected talent, scroll position and sub-page survive server syncs. All packets and behaviours are unchanged.
- **Layouts:** Talents has a scrollable list with rank pips, a detail card, a mastery box and a gold TRAIN button.
  Gear has a framed cat with slots around it. Style has a round portrait. Stats has two icon columns. Combat has
  full-width ability cards and presets. Twine has a key map and capability badges. Home switches to a compact layout on small GUIs.
  The cat model now scales to its frame.
- **Keyboard:** Ctrl+Tab / Ctrl+Shift+Tab or 1-7 switch tabs; text fields swallow keys while typing.
- **Lang:** every screen label is a `gui.kittytalents.screen.*` key with an English fallback. `tools/gen_screen_lang.py`
  regenerates the en_us block (169 keys), and `LangCompletenessTest` now fails if a `KTText` key is missing.
- **Screenshot harness:** `./gradlew runScreenShots` opens every tab at GUI scales 2 and 3 and writes `run-render/screens/*.png`.

### 2026-11-08: Phase 6 part 2: HUDs, synced statuses, collar. Phase 6 done
- **Twine HUD** (`client/hud/TwineHud`) is a registered overlay in the same wood-and-leather style. It shows name/level/role,
  HP, Energy and Ultimate bars, the four ability slots (icon, key, cooldown sweep with seconds, a red tint when Energy is
  short, a gold blinking frame when the ultimate is ready), status badges with time left, and a controls or ammo line.
  The old drawing code in `ClientTwineHandler` is gone.
- **Companion HUD** (`client/hud/CompanionHud`, new): a card for each of your cats nearby that is following you, hurt
  below half or downed. Each card has name, level, HP bar and status badges. Settings are in `kittytalents-client.toml`
  (on/off, max cards, range).
- **Synced statuses:** `FelineStatusEffects` mirrors a cat's statuses into synced entity data as {End, Power}. The values
  only change on apply, refresh or removal, not every tick. Covered by a GameTest. This fixes the "badges can never appear" finding.
- **Screen reads synced state:** tabs only send requests now. The shell rebuilds when the cat's synced state changes
  (it waits while a text field is focused), so a rejected change no longer shows as applied.
- **Twine kick fix:** while twined, the crosshair could land on the player's own invisible body, and a vanilla attack on
  yourself disconnects you ("attempted to attack an invalid entity"). Vanilla entity attacks are now cancelled while
  twined (the cat attacks through `PacketTwineInput`), and so are clicks on your body or the twined cat.
- **Collar:** our own collar texture (`tools/asset_gen/collar_texture.py`) is a full ring with a tag under the chin.
  The vanilla texture left the throat bare, so the collar looked cut off from the front.
- The `runScreenShots` harness also writes `hud.png` (both HUDs with sample statuses).

### Findings to carry into later phases
- **No XP system exists.** Cats level only from treats, plus a 20% chance of a full level per *gun* kill, with no cap
  check. Melee kills give nothing. Phase 4 needs a real `CatProgression` (XP from kills, work and training, with a level cap).
- ~~**Twine HUD status badges can never appear.**~~ Fixed in Phase 6 part 2. `ClientTwineHandler.renderActiveStatusBadges` reads the cat's Forge persistent
  data on the client, and that data is never synced. Phase 6 needs synced status state.
- Check **every** ability's `getAiPriority` range against its `execute` range (Venom Fang was wrong; others weren't audited yet). Phase 4.
- Status effects on *any* entity (zombies, etc.) live in Forge persistent data (`FelineStatusEffects`). Phase 4 should
  move them to a capability so they aren't written to disk on every mob.
- Most player-facing strings in `CatInteractions` and the GUI are hard-coded English. Phase 6 should move them to lang keys.
- **CatMode is saved by ordinal** (`tag.putInt("CatMode", ordinal)`). The planned 21→12 mode merge **must** switch to saving
  by name and translate old ordinals in the Phase 2 migrator. Otherwise saved cats get scrambled modes.
- Ability "enemy count" checks (Hiss, Frost Nova, Apex Predator) count *any* `LivingEntity`, including cows and villagers.
  Phase 4 should switch them to `CatTargeting` hostility.
- ~~`CatTalentScreen` sets job and clock-in on the client as an instant preview.~~ Fixed in Phase 6 part 2.
- The Create compat recipes log "serializer returned null" when Create is absent. They need a `forge:mod_loaded` condition.

## 3. Phased plan

Each phase ends with a **build + `runClient` smoke test + GameTests passing** and a commit/tag.
Bugs found in the audit are fixed *as part of migrating the subsystem they live in* (no throwaway patching),
except blockers marked **[hotfix]**.

### Phase 0: Baseline & tooling (small)
- Install JDK 17 and fix `JAVA_HOME`. Run `gradlew build` and `runClient`; save `latest.log` as the baseline.
- Tag `v3.0-partial-baseline`. Create a dedicated test world with a command-block test arena.
- Delete root junk files; move the Python generators into `tools/`.
- **Done when:** the jar builds, the client loads into a world, and a baseline log is saved.

### Phase 1: In-game audit confirmation (small)
- Walk the §1 tables in-game and mark each row PASS/FAIL. Repurpose `/kittytalents audit` to print the live results.
- **[hotfix]** Nine Lives infinite revive (add charges + cooldown); register `UseAbilityGoal` so AI cats cast abilities.
- **Done when:** every §1 row has a confirmed status.

### Phase 2: Core foundation (large; nothing else can start before it)
- Create `api/` and the registries; split `TalentedCat` into components; add `DerivedStatEngine` as the single stat source.
- `CatSchema` + migrators, with JUnit round-trip tests using saved NBT fixtures from 1.0, 2.0, and partial 3.0 cats.
- Move persistent-data fields into components. Delete the dead `breeding/` package and the old `compat/TacZCompat`.
- **Done when:** old saves load with no lost data, `TalentedCat` is under 600 lines, and the build is green.

### Phase 3: AI rewrite ★ priority (large; needs Phase 2)
- `CatBrain`, `CatSensors`, `CatTargeting`; remove all vanilla target goals and the duplicate follow.
- One `ModeProfile` per mode. Every one of the 21 modes gets distinct, testable behavior, or the mode is **merged or cut**
  (e.g. ESCORT→PROTECT, MINE/FARM/FISH→WORK+job) to keep the list honest.
- The `UseAbility` behavior uses role- and element-aware scoring. Feline behaviors (stalk, groom, rest, scratch, toy, food) move into a
  low-priority "idle life" band so they never interrupt combat or follow.
- AI level of detail for distant, sitting, and working cats. The 9 jobs only tick when the cat is in WORK mode with that job.
- **GameTests:** one per mode, plus targeting priority, ability use, retreat at low HP, and gun→sword fallback.
- **Done when:** every mode has a passing GameTest, no two goals fight over MOVE/TARGET flags, and a 100-cat stress test is measured.

### Phase 4: Talents & abilities, data-driven (medium; needs Phase 2, can run alongside Phase 3)
- Port the 50 talents to hook classes, with numbers in JSON. Give Home Whiskers a real effect.
- Port the 17 abilities to the caster-agnostic `AbilityContext`. Wire `CatAttackEffects` into the real hit path, or delete it.
- **Done when:** every talent has a GameTest or unit test that proves a gameplay change at rank 1.

### Phase 5: Visuals ★ priority (large; can start after Phase 2, alongside Phases 3 and 4)
- `KTCatModel` with its own bones and anchor bones; `CatAnimator` covering the animation list in spec R32.
- Nekomata tail as a real bone; check it standing, sitting, walking, and in the GUI preview.
- Armor and cosmetic layers attach to anchors; support 3 cosmetics at once; fix cape and clipping for good.
- Skin JSON registry, cached texture lookups; Panther and Snow Leopard texture pass.
- **Done when:** there's a screenshot matrix (each skin × armor set × pose) and zero missing-texture lines in the log.

### Phase 6: UI ★ priority (medium; can start after Phase 2, alongside Phase 5)
- `KTScreen` tab framework; split `CatTalentScreen` into 7 tab classes; compact layout with the world visible behind.
- `CompanionHud` and `TwineHud` as registered overlays; all strings become lang keys.
- **Done when:** every tab is reachable by keyboard and mouse, the screen file is under 300 lines, and the HUD shows HP/Energy/Ult/cooldowns/status.

### Phase 7: Network, permissions, Twine hardening (medium)
- `PacketBase` and a `CatPermissions` matrix; port all 20 packets; write malicious-packet tests.
- Split Twine into camera, input, and state; check edge cases (death, disconnect, dimension change, chunk unload).
- Dedicated server and LAN smoke test.

### Phase 8: Performance, regression & 3.0 release (medium)
- Stress test 10, 50, and 100 cats; record MSPT before and after. Run the R36 regression list. Update `KittyTalentsCompat` to the new `api/`.
- Build and tag **3.0.0**. Rewrite README and PROJECT.md to match reality.

### Phase 9: 4.0 runway
- Freeze the `api/` package with semver rules. Write `docs/EXTENDING.md` (how to add a talent, ability, mode, skin, or job).
- Gather 4.0 feature ideas. Each one should be expressible as registry entries plus data, which is the test that the architecture worked.

### Dependency graph
```
P0 → P1 → P2 ─┬─ P3 (AI) ─────┐
              ├─ P4 (talents) ─┤
              ├─ P5 (visuals) ─┼─ P7 → P8 → P9
              └─ P6 (UI) ──────┘
```
Phases 3–6 can run in parallel *only* after Phase 2 lands, because they all touch the new component API.
Phases 3 and 4 share `content/`, so coordinate there.

---

## 4. Decisions

1. **Mode list:** ✅ DECIDED (2026-11-07): merge the 21 modes into about 12, each with distinct behavior.
2. **Animation:** ✅ DECIDED (2026-11-07): hand-written `CatAnimator`, no GeckoLib.

3. **Pounce:** ✅ DECIDED (2026-11-07, from playtest): Pounce is **every cat's built-in default combat move**, used
   automatically by the AI in fights as the mod's signature hook. It is not an unlockable ability. Phase 3/4: make Pounce
   part of the base combat behavior for all cats. The Pounce talent (if kept) only *upgrades* it (range, damage,
   cooldown), and the separate "pounce" ability slot default goes away.
5. **Mastery perks (Phase 4):** ✅ REQUESTED (2026-11-07). When a talent reaches its max rank, the cat gains a passive
   "mastery" perk that sums up the whole talent. Example: Cat Armor maxed gives **free Mending** on its equipped gear.
   Implement as an `onMastered` hook in the talent-hook system, with one mastery per talent defined in data, shown in
   the Talents tab, and covered by a GameTest each.
6. **Visual bugs from playtest (Phase 5):** netherite armor looks deformed (floating carapace boxes, helmet block on
   the head, loose side pouch); the axe/tool isn't seated in the mouth. Both come from per-item magic offsets, which
   the anchor bones fix.
7. **Gear screen (Phase 6):** a stopgap is in (click equips from inventory, click unequips to inventory). The rebuilt
   screen needs real player-inventory slots so drag-and-drop works.
8. **Co-owners:** done. Online real players only, plus owner-side removal (stopgap UI; Phase 6 gives it a proper panel).
4. **UI:** user dislikes the current GUI. Full redesign in Phase 6 (playtest also showed overlapping text on Home,
   Combat and Twine tabs, and Gear slots drawn over the 3D cat preview).

Still open:
3. **Talent tuning in datapacks:** expose talent and ability numbers to datapacks (server owners can rebalance), or keep them internal?
4. **Save compatibility:** must 1.0 and 2.0 worlds load into 3.0? (The plan assumes **yes**.)
5. **Agent workflow:** last time 30+ agents produced unverified "PASS" reports. Recommendation: one phase at a time,
   with a human in-game check at each phase gate.
