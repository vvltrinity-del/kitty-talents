# E2E Test Infra: Kitty Talents 3.0 Overhaul

## Test Philosophy
- **Opaque-Box & Requirement-Driven**: Verification strictly derives from `ORIGINAL_REQUEST.md` and user-facing acceptance criteria, independent of internal module implementation.
- **Methodology**: Systematic 4-tier testing hierarchy (Category-Partition, Boundary Value Analysis, Pairwise Combinations, Real-World Scenarios) + Automated Log Scanning + Headless Server Verification + Agent-as-Judge In-Engine Behavioral Verification.

---

## Test Architecture

### 1. Test Runners & Execution Entry Points
| Runner / Task | Invocations | Expected Result | Pass/Fail Semantics |
|---|---|---|---|
| **Compilation** | `.\gradlew.bat compileJava --no-daemon` | Code compiles cleanly with 0 errors | Exit code 0 |
| **Dedicated Server Boot** | `.\gradlew.bat runServer --no-daemon` | Headless Forge Dedicated Server boots, loads registries, executes setup, outputs `Done (` | Streams stdout until `Done (` within 180s; terminates cleanly on `stop`; 0 `NoClassDefFoundError` in `run/logs/latest.log` |
| **Asset Log Scanner** | `python tools/verify_logs.py` | Scans `run/logs/latest.log` for missing textures, missing sounds, or broken JSON models | Exit code 0 if 0 warnings; Exit code 1 with line numbers if any warnings found |
| **Audit Command Verification** | In console: `kittytalents audit` | Programmatic verification of talents, abilities, network channels, and AI state | Output components reporting all systems functional |
| **Twine & AI Verification** | Agent-as-Judge test protocols | Programmatic inspection of cat movement, camera alignment, and 3 distinct overhauled AI behaviors | Exit code 0; zero exceptions, zero stuck loops |

### 2. Pre-Flight Server Configurations
- `run/eula.txt`:
  ```properties
  eula=true
  ```
- `run/server.properties`:
  ```properties
  online-mode=false
  server-port=25565
  gamemode=creative
  difficulty=peaceful
  spawn-protection=0
  sync-chunk-writes=false
  view-distance=10
  simulation-distance=10
  enable-rcon=false
  level-name=world
  ```

### 3. Log Scanner Signatures (`tools/verify_logs.py`)
- Regex signatures tested against `latest.log`:
  1. `(?i)(missing\s+texture|unable\s+to\s+resolve\s+texture|textureatlas.*unable).*kittytalents`
  2. `(?i)kittytalents.*(missing\s+texture|missing\s+sound|filenotfoundexception)`
  3. `(?i)missing\s+sound\s+for\s+event:\s+kittytalents:`
  4. `(?i)unable\s+to\s+load\s+model:\s+'kittytalents:`
  5. `java.lang.NoClassDefFoundError`
  6. `java.lang.ClassNotFoundException`

---

## Acceptance Criteria Test Suites

### Suite 1: Dedicated Server & Multiplayer Compatibility (AC 2)
1. **Headless Boot Test**: Launch `./gradlew runServer --no-daemon`. Confirm process reaches `Done (` without `NoClassDefFoundError: net/minecraft/client/Minecraft` or `KeyMapping`.
2. **Clean Shutdown Test**: Send `stop\r\n` to stdin; confirm server halts gracefully with exit code 0.
3. **Multiplayer Tracking Test**: Server console issues entity summon and inspection commands; confirms complete talent, equipment, and trust attributes serialized in NBT with zero console warnings.

### Suite 2: Asset Integrity Verification (AC 3)
1. Scan `run/logs/latest.log` using `tools/verify_logs.py`.
2. Assert count of `Missing texture` is strictly 0.
3. Assert count of `Missing sound` is strictly 0.
4. Verify Hero Cape coordinate offset is placed on dorsal back ($y_{world} \approx 13.8$) rather than under belly ($y_{world} \approx 20.2$).
5. Verify Black Panther eye texture displays bright blue pixels (`#38BDF8` / `#0284C7`).
6. Verify cat armor models fit cat anatomy without monolithic head boxes or duplicate torso meshes.

### Suite 3: Twine Control System (AC 1)
1. **Activation**: Initiate Twine on owned cat; verify camera transitions to cat view.
2. **Movement & Camera Alignment**: Verify mouse movement and WASD drive cat smoothly without camera jitter or rubberbanding.
3. **Feline Dodge**: Execute forward, backward, left, right dodge; verify invulnerability frames and Energy consumption.
4. **Fluid Mobility**: Hold jump while in water; verify cat swims upward smoothly without sinking or drowning.
5. **Dismount / Untwine**: Press untwine key; verify camera smoothly reverts to player and cat resumes autonomous behavior.

### Suite 4: Overhauled AI Behaviors (AC 1)
1. **Torch Kitty Placement (`CatPlaceTorchGoal`)**: Spawn cat in dark room (light <= 4) with torches; verify cat navigates, places torch, plays flame particles/sound, decrements item, and resumes idle behavior without freezing.
2. **Creeper Warden Repulsion (`CatCreeperWardenGoal`)**: Spawn creeper 8 blocks away; verify cat hisses, applies repulsion velocity, repels creeper beyond boundary without NPEs or fuse crashes.
3. **Fox Pounce Leap Attack (`CatFoxPounceGoal`)**: Spawn hostile target 6–10 blocks away; verify cat crouches, launches into parabolic leap, lands with crit impact particles, inflicts slowness, and resets pose to standing without infinite airborne lock.

---

## Real-World Application Scenarios (Tier 4)

| # | Scenario | Features Exercised | Pass Criteria |
|---|----------|--------------------|---------------|
| 1 | Dedicated Server Colony | M1 Sidedness, M2 Performance, M4 Modes | Server runs 10 cats in different modes (Guard, Follow, Patrol, Wander) at steady 20 TPS with zero console exceptions. |
| 2 | Hostile Dungeon Infiltration (Twined) | M3 Twine, 4-Way Dodge, Perfect Dodge, Pounce 2.0 | Player twined as Panther navigates narrow tunnels, dodges skeleton arrows, executes Pounce 2.0, leaps on targets, and returns safely. |
| 3 | Companion Night Expedition | M4 AI Overhaul, Torch Kitty, Creeper Warden | Player explores cave with companion cat; cat continuously lights dark areas and repels creepers, preventing owner damage. |
| 4 | Production Breeding Ladder | M5 Deterministic Evolution, Nekomata | Breed Standard cats up to Netherite tier deterministically; feed Spirit Catnip and verify 100% Nekomata offspring with twin tails and dual strike. |
| 5 | Feline Fashion & Armor Showcase | M6 Cape Offset, Armor Remodel, Panther Eyes | Equip Hero Cape (sits on back), full Diamond Armor (fitted cowl and harness), Panther skin (bright blue eyes); zero log warnings. |
