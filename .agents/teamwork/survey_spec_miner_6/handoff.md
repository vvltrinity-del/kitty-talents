# Test Infrastructure Survey & Verification Architecture Specification

- **Author**: `survey_spec_miner_6` (teamwork_preview_spec_miner)
- **Date**: 2026-10-06T21:00:00Z
- **Target Project**: Kitty Talents 3.0 Overhaul (`C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`)
- **Recipient**: `orchestrator_1` (`a549636f-050f-4476-a58e-0c527d96daeb`)

---

## Executive Summary

This survey report provides a comprehensive test architecture specification and exact verification protocols for all Acceptance Criteria outlined in `ORIGINAL_REQUEST.md` for the Kitty Talents 3.0 overhaul.

Key findings during this investigation:
1. **Build & Test Engine**: Minecraft Forge `1.20.1-47.3.0`, Java 17 toolchain (running under Java 21 LTS runtime), ForgeGradle 6.0/6.2. ForgeGradle provides native run configurations `runServer`, `runClient`, and `runGameTestServer`.
2. **Current Test Assets**: No automated tests currently exist in `src/test/`. An empty package `src/main/java/com/kittytalents/test` exists. No pre-generated test world exists in `run/saves/` or the repository.
3. **Audit Command Mock**: The in-game command `/kittytalents audit` in `KittyTalentsCommand.java` currently consists of hardcoded mock PASS messages with an explicit `// TODO: Hook up the actual audit harness` comment.
4. **Dedicated Server Crash Hazards**: Five critical client-side classloading violations exist across common network and item classes (`PacketSyncCatEquipment`, `PacketSyncTalents`, `PacketSyncTrustedPlayers`, `PacketSyncTwine`, `VariantChartItem`) that directly import `net.minecraft.client.Minecraft` or screen classes, which will cause `NoClassDefFoundError` on a dedicated server during initialization.
5. **AI Logic Hazards**: Multiple AI behaviors exhibit subtle bugs, including `CatRestOnBedGoal` which sets `setOrderedToSit(true)` without resetting it on `stop()`, causing cats to become permanently stuck in a sitting posture. Furthermore, server-side AI goals attempt to spawn particles via client-only methods (`cat.level().isClientSide` branches).

---

## Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Server Execution | `.\gradlew.bat runServer` | Launches headless Forge Dedicated Server with `--nogui` | `run/eula.txt`, `run/server.properties` | Server console log, port binding | If `eula.txt` missing/false, halts; if client classes imported, crashes with `NoClassDefFoundError` | `build.gradle:89-92` |
| 2 | GameTest Engine | `.\gradlew.bat runGameTestServer` | Launches headless Mojang GameTest server running tests under `forge.enabledGameTestNamespaces` | Registered `@GameTest` methods and structures | Test execution summary (pass/fail count) | Exits with failure code and assertion trace if any test fails or no tests exist | `build.gradle:97-99` |
| 3 | Client Execution | `.\gradlew.bat runClient` | Launches Minecraft Client GUI with development mod loaded | Player inputs, display options | Rendering window, `run/logs/latest.log` | Missing textures logged as `[minecraft/TextureAtlas]: Unable to resolve texture reference`; missing sounds logged to `SoundEngine` | `build.gradle:84-87` |
| 4 | Audit Command | `/kittytalents audit` | In-game command intended to verify feature registration and systems | In-game player or server console execution | Chat / console component output | Currently hardcoded mock output; must be replaced with real programmatic assertions | `KittyTalentsCommand.java:16-32` |
| 5 | Twine Control | `PacketTwineAction` | Initiates or terminates player control of targeted `TalentedCat` | `catEntityId`, `twine` (boolean) | `TwineManager` session registration, client camera binding, `PacketSyncTwine` | Rejects if cat not owned/trusted, out of range (>64m), or incapacitated | `PacketTwineAction.java:15-74`, `TwineManager.java` |
| 6 | Twine Input Sync | `PacketTwineInput` | Streams player movement, rotation, attack, jump, ability keys to server cat | `xxa`, `zza`, `jump`, `attack`, `pounce`, `ability1`, `ability2`, `yRot`, `xRot` | Updates server cat motion, pose, rotations, actions | Silently discarded if floats are infinite/NaN or player not twined with target cat | `PacketTwineInput.java:13-172` |
| 7 | Twine Camera Sync | `ClientTwineHandler` | Binds client camera to cat, aligns mouse look, suppresses player physical movement | Mouse inputs, WASD, KeyMappings | Cat camera view, player minion autonomous follow | Desync occurs if camera entity reverts or rotation updates decouple | `ClientTwineHandler.java:29-317` |
| 8 | AI: Torch Placement | `CatPlaceTorchGoal` | Cat places torches in dark areas (light <= 6) from catpack or tool slot | Low ambient block light, torch in inventory | Places `minecraft:torch`, decrements torch item, plays sound and particles | Fails if no valid solid surface or no torch available | `CatPlaceTorchGoal.java:21-201` |
| 9 | AI: Creeper Warden | `CatCreeperWardenGoal` | Hisses and repels nearby creepers/phantoms within 6–16m (`NIGHT_PROWLER`) | Creepers/Phantoms within radius | Repulsion knockback velocity, `CAT_HISS` sound, night vision buff to owner | Inactive if cat incapacitated or talent level == 0 | `CatCreeperWardenGoal.java:18-68` |
| 10 | AI: Fox Pounce | `CatFoxPounceGoal` | Prepares crouch and launches high arc pounce attack at target (3–14m) | Living target in range, cat on ground | Parabolic leap, crit damage, slowness debuff, impact shockwave | Cancels if target dies during crouch phase; resets pose to `STANDING` | `CatFoxPounceGoal.java:17-128` |
| 11 | AI: Toy Fetch | `CatFetchToyGoal` | Retrieves thrown toy entity (`toy_mouse`, `cat_ball`, etc.) and brings to owner | `ThrownToyEntity` in world within 16m | Navigates to toy, picks up into mouth/inventory, returns to owner | Ignored in `DOCILE` mode or if cat sitting/incapacitated | `CatFetchToyGoal.java:17-102` |
| 12 | AI: Fisher Cat | `CatFisherGoal` | Looks at nearby water and catches fish drops over 40–80 ticks | Adjacent water block, `FISHER_CAT` talent | ItemEntity drops (cod, salmon, tropical fish), increases hunger | Only runs on random chance; fails if water block removed | `CatFisherGoal.java:15-89` |
| 13 | AI: Bowl Eating | `CatEatFromBowlGoal` | Navigates to `CatFoodBowlBlock` and consumes edible food | Hunger < 96 or injured, food in bowl | Decrements bowl inventory, increases cat hunger/satiety/health | Fails if bowl empty or path unreachable | `CatEatFromBowlGoal.java:18-127` |
| 14 | AI: Bed Resting | `CatRestOnBedGoal` | Claims and sleeps on `CatBedBlock` to heal and recover from incapacitation | Claimed bed pos, nighttime or low health | Heals 1.5 HP / 2s, purrs, revives from incapacitation | **BUG**: leaves `isOrderedToSit(true)` permanent upon exit | `CatRestOnBedGoal.java:13-94` |
| 15 | AI: Scratcher Post | `CatUseScratcherGoal` | Scratches `CatScratcherBlock` to gain attack damage boost | Nearby scratcher block within 8m | Wood hit sound, purr, grants `DAMAGE_BOOST` (Strength II for 5 min) | **BUG**: Uses client-only particle method on server side | `CatUseScratcherGoal.java:15-92` |
| 16 | TACZ Gun Combat | `CatGunAttackGoal` | Tactically strafes and fires equipped TACZ firearms | Target in line of sight, gun equipped | Ranged projectile attack via `TacZCompat`, tactical strafing | Graceful fallback if TACZ mod not installed | `CatGunAttackGoal.java:11-93`, `TacZCompat.java` |
| 17 | Equipment Sync | `PacketSyncCatEquipment` | Syncs cat equipment, armor, accessories, cosmetics from server to client | Cat equipment changes | Updates client cat slots, refreshes `CatTalentScreen` UI | **CRASH HAZARD**: Directly references `Minecraft.getInstance()` and screen classes | `PacketSyncCatEquipment.java:75-99` |
| 18 | Talent Sync | `PacketSyncTalents` | Syncs unlocked talent levels from server to client | Cat talent allocation | Updates client cat talent map | **CRASH HAZARD**: Directly references `Minecraft.getInstance()` and `CatTalentScreen` | `PacketSyncTalents.java:54-65` |
| 19 | Trust Sync | `PacketSyncTrustedPlayers` | Syncs trusted player UUID list from server to client | Trust management packet | Updates client trust set | **CRASH HAZARD**: Directly references `Minecraft.getInstance()` and `CatTalentScreen` | `PacketSyncTrustedPlayers.java:50-60` |
| 20 | Twine Session Sync | `PacketSyncTwine` | Informs client of Twine session activation/deactivation | Server twine event | Sets `ClientTwineHandler.activeTwinedCatId`, binds camera | **CRASH HAZARD**: Directly references `ClientTwineHandler` in common packet | `PacketSyncTwine.java:3-29` |
| 21 | Variant Chart Codex | `VariantChartItem` | Right-click codex displaying feline breed genetics and breeding matrix | Right-click use in hand | Opens `VariantChartScreen` | **CRASH HAZARD**: Directly calls `Minecraft.getInstance()` in `use()` | `VariantChartItem.java:24-31` |

---

## Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Dedicated Server Launch | Mod JAR loaded into Dedicated Server | Fails during `FMLCommonSetupEvent` when registering network packets because `PacketSyncCatEquipment`, `PacketSyncTalents`, `PacketSyncTrustedPlayers`, `PacketSyncTwine` import `net.minecraft.client.*`. |
| 2 | Dedicated Server Launch | Missing `run/eula.txt` | Dedicated server generates `eula.txt` with `eula=false` and immediately shuts down with exit code 0. |
| 3 | AI Bed Resting | Cat finishes resting or bed is broken | Cat remains sitting permanently because `CatRestOnBedGoal#stop()` fails to call `cat.setOrderedToSit(false)`. |
| 4 | AI Scratcher & Bed Particles | Goal executes on server | Particles fail to render because code checks `if (cat.level().isClientSide)` which is always `false` on the server thread; server must use `((ServerLevel) cat.level()).sendParticles()`. |
| 5 | Twine Control | Player exceeds 64 blocks distance | Server-side `ForgeEvents#onLivingTick` detects `distanceToSqr > 4096.0D` and executes `TwineManager.endForPlayer(sp, "Your cat is out of range - untwined.")`. |
| 6 | Twine Control | Player takes damage dropping below 25% health | Server-side `ForgeEvents` triggers emergency untwine to protect player body. |
| 7 | Twine Control | Physical player body movement while twined | `ClientTwineHandler#onMovementInput` zeroes out `input.forwardImpulse` and `input.leftImpulse`, preventing player's body from walking off cliffs while controlling cat. |
| 8 | Twine Camera | Cat moves in third-person view | `ClientTwineHandler#onComputeCameraAngles` aligns camera yaw/pitch to cat; first-person offsets camera forward by `0.18D` via reflection so cat body does not clip camera. |
| 9 | Torch Placement | Dark area with non-solid floor (e.g. lava/air) | `CatPlaceTorchGoal#isValidPlacementLocation` checks `BlockState#isFaceSturdy` for `Direction.UP`; aborts placement if surface is invalid. |
| 10 | Asset Logging | Client loads resource pack with missing model/texture | Vanilla `TextureAtlas` logs `[WARN] Unable to resolve texture reference: #layer0 in kittytalents:item/...`; `latest.log` scanner detects pattern. |

---

## Acceptance Criteria Verification Procedures

### AC 1: Dedicated Server Compilation & Launch (Headless)

#### Requirement
The mod must successfully compile and launch a Dedicated Server instance without crashing due to client-side class loading errors (e.g. `NoClassDefFoundError` for rendering classes).

#### Pre-Launch Prerequisites
1. **Directory Preparation**:
   The working directory is `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\run`.
   Create `run/eula.txt` with exact content:
   ```properties
   #By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).
   eula=true
   ```
2. **Server Properties Preparation**:
   Create `run/server.properties` with:
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

#### Launch Command
```powershell
.\gradlew.bat runServer --no-daemon
```

#### Readiness & Completion Detection
- The automation process must stream stdout line by line until matching:
  `[Server thread/INFO] [minecraft/DedicatedServer]: Done (`
- Maximum startup timeout: 180 seconds.

#### Clean Termination Protocol
- Pipe `stop` followed by newline (`stop\r\n`) to server process standard input (`stdin`).
- Wait up to 30 seconds for process termination. Confirm exit code is `0`.

#### Crash & Error Detection Protocol
1. Scan `run/crash-reports/` directory: Must be empty or contain 0 newly created files.
2. Scan `run/logs/latest.log` for any of the following failure tokens:
   - `java.lang.NoClassDefFoundError`
   - `java.lang.ClassNotFoundException`
   - `net.minecraftforge.fml.ModLoadingException`
   - `FATAL`
3. Specifically verify that no references to `net/minecraft/client` or `com/kittytalents/client` appear in server-side stack traces.

---

### AC 2: Multiplayer & Client Interaction Verification

#### Requirement
The client must successfully connect to the local Dedicated Server instance, spawn a cat, and interact with it without generating server-side console errors or desyncs.

#### Verification Procedure
1. **Server Startup**: Launch Dedicated Server instance as specified in AC 1. Wait for `Done (`.
2. **Server Console Entity Validation**:
   Send following commands to server console:
   ```
   summon kittytalents:talented_cat ~ ~ ~ {Tamed:1b,Owner:"00000000-0000-0000-0000-000000000001"}
   data get entity @e[type=kittytalents:talented_cat,limit=1]
   ```
   Confirm server successfully serializes entity NBT containing `kittytalents` custom attributes (`CatLevel`, `Talents`, `CatMode`).
3. **Dedicated Server GameTest Execution**:
   Forge supports running GameTests directly in headless environments:
   ```powershell
   .\gradlew.bat runGameTestServer --no-daemon
   ```
   A dedicated test class (`src/main/java/com/kittytalents/test/KittyTalentsGameTests.java`) must be registered using `@GameTest` annotations:
   - Spawns `TalentedCat`.
   - Simulates player interaction via `helper.makeMockPlayer()`.
   - Asserts talent training, mode cycling, and inventory operations.
   - Confirms zero console exceptions or desync events.

---

### AC 3: Asset Verification (`latest.log` Scanner)

#### Requirement
The Minecraft client log (`latest.log`) must be scanned after loading into the test world to ensure there are exactly zero `Missing texture` or `Missing sound` warnings related to the mod's namespace.

#### Log File Location
`C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\run\logs\latest.log`

#### Scanning Signatures
The scanner must search line-by-line for lines matching any of the following regular expressions:

```regex
(?i)(missing\s+texture|unable\s+to\s+resolve\s+texture|textureatlas.*unable).*kittytalents
(?i)kittytalents.*(missing\s+texture|missing\s+sound|filenotfoundexception)
(?i)missing\s+sound\s+for\s+event:\s+kittytalents:
(?i)unable\s+to\s+load\s+model:\s+'kittytalents:
```

#### Automated Python Scanner Specification (`tools/verify_logs.py`)
```python
import sys
import re

LOG_PATH = r"run\logs\latest.log"

PATTERNS = [
    re.compile(r"missing\s+texture.*kittytalents", re.I),
    re.compile(r"unable\s+to\s+resolve\s+texture.*kittytalents", re.I),
    re.compile(r"missing\s+sound.*kittytalents", re.I),
    re.compile(r"kittytalents:textures/.*filenotfound", re.I),
    re.compile(r"unable\s+to\s+load\s+model:\s+'kittytalents:", re.I)
]

def scan():
    errors = []
    with open(LOG_PATH, "r", encoding="utf-8", errors="ignore") as f:
        for lineno, line in enumerate(f, 1):
            for pat in PATTERNS:
                if pat.search(line):
                    errors.append((lineno, line.strip()))
                    break
    if errors:
        print(f"FAILED: Found {len(errors)} asset warnings in {LOG_PATH}:")
        for num, err in errors:
            print(f"  Line {num}: {err}")
        sys.exit(1)
    else:
        print("PASSED: Zero Missing Texture or Missing Sound warnings detected.")
        sys.exit(0)

if __name__ == "__main__":
    scan()
```

---

### AC 4: Twine System Verification (Agent-as-Judge)

#### Requirement
An independent agent must launch the provided test world, initiate the Twine system, and confirm smooth control without camera or input desyncs.

#### Verification Procedure
1. **Initiate Twine**:
   - Player looks at an owned `TalentedCat` and triggers `PacketTwineAction(catId, true)` (or presses `Z` key).
   - **Verification Check 1**: Server registers session (`TwineManager.isPlayerTwined(player.getUUID()) == true`).
   - **Verification Check 2**: Cat syncs `DATA_TWINED = true`.
   - **Verification Check 3**: Client camera switches to cat (`mc.getCameraEntity() == cat`).
   - **Verification Check 4**: Screen overlay displays `✦ TWINED WITH <NAME> ✦`.
2. **Camera & Input Desync Test**:
   - Transmit movement input packets (`PacketTwineInput`):
     - `forward = 1.0F`, `strafe = 0.0F`. Confirm cat walks in look direction; verify `cat.getDeltaMovement().horizontalDistanceSqr() > 0.01`.
     - `yRot = +45.0F`, `xRot = -15.0F`. Confirm cat yaw and pitch update on server without rubberbanding.
     - Confirm physical player body has impulse zeroed out by `MovementInputUpdateEvent` and smoothly follows behind cat (at 3–16 block distance) without getting desynced or stuck in walls.
3. **Disconnection & Untwine Failsafes**:
   - Player presses `KEY_UNTWINE` (`U`) or Sneak (`Shift`).
   - **Verification Check 5**: Camera immediately reverts to `mc.player`.
   - **Verification Check 6**: `TwineManager` clears session. Cat returns to autonomous AI state.
   - Test low health trigger (< 25%): confirm emergency untwine message appears.
   - Test out of range trigger (> 64 blocks): confirm automatic untwine triggers cleanly.

---

### AC 5: Overhauled AI Behaviors Verification (Agent-as-Judge)

#### Requirement
An independent agent must trigger at least 3 distinct overhauled AI behaviors in the test world and confirm they execute completely without getting stuck or throwing errors.

#### Behavior 1: Torch Kitty Placement (`CatPlaceTorchGoal`)
- **Setup**: Spawn `TalentedCat` in an enclosed room with light level <= 4. Train `TORCH_KITTY` talent to Level 1+. Give cat torches in catpack or tool slot.
- **Trigger**: Goal selector activates `CatPlaceTorchGoal`.
- **Expected Execution**:
  1. Cat identifies valid dark surface (`targetTorchPos`).
  2. Cat navigates within 2 blocks (`moveTo`).
  3. Cat looks at block and executes placement timer (`placementProgress >= 30`).
  4. Block at position becomes `minecraft:torch`.
  5. Torch stack in inventory shrinks by 1.
  6. Flame particles and `WOOD_PLACE` sound emitted.
- **Completion Check**: Goal terminates cleanly via `stop()`, sets cooldown, and cat resumes idle/follow behavior without freezing navigation.

#### Behavior 2: Creeper Warden Repel (`CatCreeperWardenGoal`)
- **Setup**: Spawn `TalentedCat` with `NIGHT_PROWLER` Level 1+. Spawn a `Creeper` 8 blocks away.
- **Trigger**: Creeper enters warden radius (6–16 blocks).
- **Expected Execution**:
  1. Cat detects creeper in bounding box.
  2. Cat emits `CAT_HISS` sound (rate-limited by `hissTimer = 60`).
  3. Creeper receives repulsion velocity (`setDeltaMovement(fleeDir.x * 0.4D, 0.1D, fleeDir.z * 0.4D)`).
  4. Creeper's pathing / explosion fuse is disrupted.
- **Completion Check**: Creeper is repelled outside warden boundary without throwing NPE or causing pathfinding crashes.

#### Behavior 3: Fox Pounce Leap Attack (`CatFoxPounceGoal`)
- **Setup**: Spawn `TalentedCat` with `POUNCE` Level 1+ set to `AGGRESSIVE` mode. Spawn a hostile target (e.g. `Zombie` or `Spider`) 6–10 blocks away.
- **Trigger**: Target is between 3 and 14 blocks away on ground.
- **Expected Execution**:
  1. Phase 1 (Crouch): Cat adopts `Pose.CROUCHING`, halts navigation, emits hiss sound, crouches for 15 ticks.
  2. Phase 2 (Leap): Cat launches into parabolic arc (`ySpeed = 0.65D + pounceLvl * 0.05D`, `hasImpulse = true`).
  3. Phase 3 (Impact): Upon landing within 2 blocks of target, deals crit damage, applies `Slowness II` for 3 seconds, emits crit/sweep particles, and plays `PLAYER_ATTACK_CRIT` sound.
- **Completion Check**: Goal calls `stop()`, resets pose to `Pose.STANDING`, sets 40 tick cooldown, and resumes combat without locking pose.

#### Bonus Behavior 4: Toy Fetch (`CatFetchToyGoal`)
- **Setup**: Cat in `FOLLOW` or `WANDER` mode. Player throws toy (`ThrownToyEntity`).
- **Trigger**: Toy lands within 16 blocks.
- **Expected Execution**: Cat paths to toy, collects item into mouth, paths back to owner, drops or returns toy, purrs.
- **Completion Check**: Cat exits goal and returns to following owner.

---

## 5-Component Handoff Report

### 1. Observation
1. **Build Configuration**:
   - In `build.gradle` line 5: `id 'net.minecraftforge.gradle' version '[6.0,6.2)'`
   - In `gradle.properties` line 10, 16: `minecraft_version=1.20.1`, `forge_version=47.3.0`
   - In `build.gradle` line 89-92:
     ```groovy
     server {
         property 'forge.enabledGameTestNamespaces', mod_id
         args '--nogui'
     }
     ```
   - In `build.gradle` line 97-99:
     ```groovy
     gameTestServer {
         property 'forge.enabledGameTestNamespaces', mod_id
     }
     ```
2. **Directory & Workspace State**:
   - `run/` directory does not currently exist.
   - `src/test/` does not currently exist.
   - `src/main/java/com/kittytalents/test` exists as an empty directory.
   - No pre-saved worlds exist in `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`.
3. **Audit Command Mock**:
   - In `src/main/java/com/kittytalents/command/KittyTalentsCommand.java` lines 20-30:
     ```java
     // TODO: Hook up the actual audit harness
     source.sendSuccess(() -> Component.literal("§a[PASS]§r Talents Registered"), false);
     ...
     ```
4. **Sidedness Violations in Common Packages**:
   - `src/main/java/com/kittytalents/network/PacketSyncCatEquipment.java` line 5: `import net.minecraft.client.Minecraft;`
   - `src/main/java/com/kittytalents/network/PacketSyncCatEquipment.java` line 77: `if (Minecraft.getInstance().level != null)`
   - `src/main/java/com/kittytalents/network/PacketSyncTalents.java` line 4: `import net.minecraft.client.Minecraft;`
   - `src/main/java/com/kittytalents/network/PacketSyncTrustedPlayers.java` line 4: `import net.minecraft.client.Minecraft;`
   - `src/main/java/com/kittytalents/network/PacketSyncTwine.java` line 3: `import com.kittytalents.client.ClientTwineHandler;`
   - `src/main/java/com/kittytalents/item/VariantChartItem.java` line 27: `net.minecraft.client.Minecraft.getInstance().setScreen(...)`
5. **AI Bugs Observed**:
   - In `src/main/java/com/kittytalents/entity/ai/CatRestOnBedGoal.java` line 61: `cat.setOrderedToSit(true);` is invoked, but line 90 `stop()` only clears `restTicks = 0;` without resetting sitting state, permanently locking the cat in sitting posture.
   - In `src/main/java/com/kittytalents/entity/ai/CatUseScratcherGoal.java` lines 63, 72: `if (cat.level().isClientSide)` checks are used inside server AI step, preventing particles from ever rendering.

### 2. Logic Chain
1. Forge Dedicated Server jars do not include client classes (`net.minecraft.client.Minecraft`, `Screen`, etc.).
2. When `KittyTalents.java` executes `commonSetup`, it calls `PacketHandler.register()`.
3. `PacketHandler.register()` registers `PacketSyncCatEquipment`, `PacketSyncTalents`, `PacketSyncTrustedPlayers`, and `PacketSyncTwine`.
4. Because these packet classes directly import and invoke client-only classes, the JVM classloader will attempt to resolve `net.minecraft.client.Minecraft` on the Dedicated Server and throw `java.lang.NoClassDefFoundError`, crashing the server before startup completes.
5. Therefore, refactoring packet handlers to delegate client operations via `DistExecutor` or separate client-only handler classes (`ClientPacketHandler`) is a hard requirement for AC 1.
6. For asset verification, missing textures and missing sounds are captured by Minecraft's Log4j logger (`TextureAtlas` and `SoundEngine`) into `run/logs/latest.log`. A regex-based Python scanner provides deterministic, automated verification.
7. For Twine and AI verification, the absence of pre-existing test worlds necessitates automated world bootstrap or GameTest integration. The AI bugs identified (sitting lockup and particle sidedness) directly threaten the criterion "confirm they execute completely without getting stuck or throwing errors".

### 3. Caveats
- No GUI automation tool (e.g. Sikuli or simulated GLFW window) is present in the environment; interactive Agent-as-Judge verification of GUI/camera controls must be conducted via either `runClient` with human/agent inspection or via server-side GameTest/bot simulation.
- Forge GameTest framework requires structure templates or programmatic block layout.

### 4. Conclusion
The testing infrastructure for Kitty Talents 3.0 requires three key components:
1. **Sidedness Sanitization**: Eliminate all direct client imports from packets and common items to enable dedicated server boot without `NoClassDefFoundError`.
2. **Deterministic Automated Verification**: Deploy `tools/verify_logs.py` (for AC 3 log scanning) and implement the real `/kittytalents audit` command.
3. **AI Logic Fixes & Test Suite**: Fix the `CatRestOnBedGoal` sitting trap and server particle calls; provide reproducible test scenarios for Torch Placement, Creeper Warden, and Fox Pounce.

### 5. Verification Method
1. **Dedicated Server Boot Verification**:
   ```powershell
   # 1. Create EULA
   New-Item -ItemType Directory -Force -Path "run"
   Set-Content -Path "run\eula.txt" -Value "eula=true"
   # 2. Compile and launch
   .\gradlew.bat compileJava
   .\gradlew.bat runServer --no-daemon
   # Verify log contains "Done (" and 0 NoClassDefFoundError
   ```
2. **Log Asset Verification**:
   ```powershell
   python -c "
   import re
   with open('run/logs/latest.log', 'r', encoding='utf-8', errors='ignore') as f:
       text = f.read()
   warns = re.findall(r'(?i).*(missing (?:texture|sound)|unable to resolve texture).*kittytalents.*', text)
   print('Asset warnings found:', len(warns))
   assert len(warns) == 0, 'Found asset warnings!'
   "
   ```
3. **Audit Command Execution**:
   Run `/kittytalents audit` via server console or GameTest to verify registry, twine, and AI goal sanity.
