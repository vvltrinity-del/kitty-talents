# Kitty Talents 3.0: developer guide

Minecraft 1.20.1, Forge 47, Java 17. Player-facing features are in `README.md`; the history and decisions of the 3.0
rewrite are in `OVERHAUL_PLAN.md`.

## Building and running

The system `JAVA_HOME` on the dev machine is stale, so set it first:

```bash
export JAVA_HOME="C:/Users/VivianV/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20.1+1"
./gradlew build          # compiles and runs the JUnit tests; jar in build/libs
./gradlew runClient      # dev client (run/)
```

## Verifying a change

| Check | Command | Passes when |
|---|---|---|
| Unit tests | `./gradlew build` | BUILD SUCCESSFUL |
| GameTests (in-world, ~70 tests) | `bash tools/run_gametests.sh` | it prints `All N required tests passed`. Gradle exits 0 even when the server fails, so trust only that line. Each run starts from a fresh world with mob spawning off. |
| Dedicated server boots | `python tools/test_server_boot.py` | the `Done (` line appears (it always ends with "did not stop in 45s"; that is expected) |
| Cat rendering | `bash tools/run_render_matrix.sh` | 0 texture warnings; PNGs in `run-render/matrix` (contact sheets: `tools/matrix_sheets.py`) |
| Screen and HUD look | `./gradlew runScreenShots` | PNGs of every tab at GUI scale 2 and 3, plus `hud.png`, in `run-render/screens` |
| Missing assets in a log | `python tools/verify_logs.py` | no missing textures or sounds |

Writing GameTests: entities spawn at y=2 (the structure floor is y=1); templates are `empty` (8x8) and `arena` (32x32).
`KittyTalentGameTests.joinFakeOwner` / `leave` give a cat a real, resolvable owner. A tamed cat with no owner sits
forever (vanilla), so tests that need movement should use one.

Performance: the stress test grows tamed cats 0 → 10 → 50 → 100 and times only their own ticks (`util/TickCost`).
At 3.0.0, 100 cats cost about 2.4 ms per server tick (~24 µs per cat). The test fails above 25 ms.

## Package layout (`src/main/java/com/kittytalents`)

| Package | Contents |
|---|---|
| `entity` | `TalentedCat`, `CatMode` (12 modes), `TwineManager` / `TwineMoves` (server Twine sessions), breeding, variants and tiers |
| `entity/interact` | `CatInteractions`: every right-click interaction (taming, feeding, dye, revive, mount, treats) |
| `core/component` | State split out of `TalentedCat`: equipment, loadout, trust, job data, stats, combat state |
| `core/persist` | `CatSchema` and migrators: 1.0 / 2.0 saves load into 3.0 |
| `core/progression` | `CatProgression`: XP, levels, skill points |
| `core/perm` | `CatPermissions` + `CatAction`: owner / co-owner / stranger matrix used by every packet |
| `ai` | `CatBrain` (goal priority bands), `CatTargeting` (`canHarm` / `isFriendly`) |
| `ai/goal`, `entity/ai` | AI goals: follow, hold anchor, pounce, stalk, dodge, melee, retreat, needs, idle life |
| `talent`, `talent/effect` | 51 talents; `TalentEngine` dispatches talent hooks (tick, hits, damage taken, attributes, kills, mastery) |
| `ability` | 16 active abilities (10 skills, 5 ultimates, one utility) and the `CatAbilities` registry |
| `combat` | Roles, elements, Energy, dodges, status effects (statuses sync to the client for the HUDs) |
| `work`, `work/jobs` | Jobs and their goals (only run in Work mode while clocked in) |
| `network` | All packets extend `PacketBase`; server handlers are `public static apply(...)` so GameTests can call them |
| `block`, `inventory`, `recipe`, `item`, `registry` | Blocks and block entities (habitat, bowl, bed, oven...), menus, recipes, items, registries |
| `client/model`, `client/anim`, `client/renderer`, `client/skin` | `KTCatModel` (own rig with anchor bones), `CatAnimator`, layers, JSON skin registry |
| `client/gui`, `client/gui/tab`, `client/gui/theme` | `CatTalentScreen` shell, one class per tab, the wood-and-leather theme (`KTTheme`, `KTButton`, `KTWidget`, `KTText`) |
| `client/hud` | `TwineHud`, `CompanionHud` (registered GUI overlays) |
| `client/twine` | Client Twine: `ClientTwineState`, `TwineCamera`, `TwineInput` |
| `client/debug` | Dev harnesses (`RenderMatrix`, `ScreenShots`), inactive unless their system property is set |
| `compat` | TaCZ, Apotheosis, Curios, Jade, JEI (each only active when that mod is loaded) |
| `api` | Public API for addons (see below) |
| `test` | GameTests (shipped in the jar but only registered when GameTests are enabled) |

## Conventions

- **Screen text** goes through `KTText.t("key", "English fallback")`. After adding one, run
  `python tools/gen_screen_lang.py` to regenerate the `gui.kittytalents.screen.*` block in `en_us.json`.
  `LangCompletenessTest` fails on any missing key (talent names, descriptions and mastery texts included).
- **Server authority:** the client only sends requests. The server validates them through `CatPermissions`, and the
  screen rebuilds from synced state, so a refused change never shows as applied.
- **Python asset scripts** run without `-I` (Pillow is a user-site install). Textures come from `tools/asset_gen`.

## Addon API (`com.kittytalents.api`)

Full guide with examples and the stability rules: `docs/EXTENDING.md`. API version 1.0.0 (`KittyTalentsAPI.API_VERSION`).

- `KittyTalentsAPI.registerTalent(talent, effect)` / `registerAbility(ability)`: add talents and abilities from an addon.

- `event.RegisterCatGoalsEvent`: fired once per cat when its AI is built. Use
  `event.addGoal(CatGoalBand.NEEDS, goal)` (bands, highest first: SAFETY, ORDERS, COMBAT, MELEE, COMMAND, WORK,
  NEEDS, IDLE) or `addTargetGoal(goal)`. Don't add goals from `EntityJoinLevelEvent`: it fires again on every chunk
  reload and dimension change.
- `event.CatRewardEvent`: fired when a cat brings its owner an item (Mouser morning gifts). Change the reward, or
  cancel it when you delivered the item yourself.

The `KittyTalentsCompat` addon (sibling folder) uses both events.
