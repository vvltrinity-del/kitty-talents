# Survey Report: R1 (Twine System Debugging) & R3 (AI Logic Overhaul)

**Surveyor**: survey_explorer_1  
**Project**: Kitty Talents 3.0 Overhaul  
**Target Path**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`  
**Date**: 2026-10-06  

---

## 1. Observation

A full architectural and implementation survey was performed on the Kitty Talents codebase with specific focus on **R1 (Twine System Debugging & Riding / Movement Control)** and **R3 (AI Logic Overhaul & Doggy Talents Parity)**.

### 1.1 Twine System Architecture & Data Flow

The Twine system consists of seven primary components:
1. **Client Event Handler & Input Processor**: `src/main/java/com/kittytalents/client/ClientTwineHandler.java`
2. **Action Packet (Start / Stop Twine)**: `src/main/java/com/kittytalents/network/PacketTwineAction.java`
3. **Tick Input Packet (Controls, Abilities, Yaw/Pitch)**: `src/main/java/com/kittytalents/network/PacketTwineInput.java`
4. **Twine State Sync Packet (S2C)**: `src/main/java/com/kittytalents/network/PacketSyncTwine.java`
5. **Session Registry & Server Failsafes**: `src/main/java/com/kittytalents/entity/TwineManager.java`
6. **Server Event Hooks & Player AI Simulation**: `src/main/java/com/kittytalents/event/ForgeEvents.java`
7. **Entity Movement & State Integration**: `src/main/java/com/kittytalents/entity/TalentedCat.java`

#### Session Lifecycle
- **Initiation**: Triggered by either pressing Keybind `Z` (`KEY_TWINE` in `ClientTwineHandler:233`) while raycasting directly at an owned/trusted cat, or by clicking the `✦ TWINE ✦` button in `CatTalentScreen:266`.
- **Validation**: `PacketTwineAction:45-67` validates ownership, trust, incapacitated state, distance (`<= 64 blocks`), and ensures no other player is twined. Registers session in `TwineManager` map (`UUID player -> UUID cat`) and sends `PacketSyncTwine(catId, true)` to client.
- **Client Camera Switch**: `ClientTwineHandler:60` calls `mc.setCameraEntity(cat)`. Player input is zeroed in `MovementInputUpdateEvent:91-102`.
- **Tick Control**: Every client tick, `ClientTwineHandler:185` sends `PacketTwineInput` containing `(strafe, forward, jump, attack, pounce, ability1, ability2, mouseYaw, mousePitch)` to the server.
- **Termination**: Triggered by pressing `U` (`KEY_UNTWINE`), pressing `Shift` (Sneak), cat death/incapacitation, player death, player logout, dimension change, or emergency untwine (player HP <= 25%).

---

### 1.2 Direct Observations of Twine Bugs, Desyncs & Edge Cases (R1)

#### Observation 1.1: Frame-Rate Camera Jitter via Raw Tick Yaw in `ComputeCameraAngles`
- **File**: `ClientTwineHandler.java:289-315`
- **Verbatim Code**:
  ```java
  @SubscribeEvent
  public static void onComputeCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
      Minecraft mc = Minecraft.getInstance();
      if (isTwined() && mc.level != null) {
          Entity entity = mc.level.getEntity(activeTwinedCatId);
          if (entity instanceof TalentedCat cat && mc.getCameraEntity() == cat) {
              ...
              // Camera orientation strictly matches cat
              event.setYaw(cat.getYRot());
              event.setPitch(cat.getXRot());
  ```
- **Finding**: Forge's `ComputeCameraAngles` fires on **every rendered frame** (e.g. 60, 144, 240+ FPS). Vanilla `Camera.setup()` smoothly interpolates entity rotation using `Mth.rotLerp(partialTick, cat.yRotO, cat.getYRot())`. By overriding camera angles with raw tick getters `cat.getYRot()` and `cat.getXRot()` (which only update at 20 Hz tick rate), camera view stutter and micro-jitter occurs on all frames between ticks.

#### Observation 1.2: Server Network Echo Overrides Client Prediction (Rubberbanding & Rotation Snapping)
- **Files**: `ClientTwineHandler.java:176-185`, `PacketTwineInput.java:78-82`, and vanilla `net.minecraft.server.level.ServerEntity.sendChanges()`
- **Mechanism**:
  - In vanilla Minecraft riding (`ServerboundMoveVehiclePacket`), the server's `ServerEntity` tracks ridden vehicles and explicitly **suppresses** sending `ClientboundMoveEntityPacket` or `ClientboundTeleportEntityPacket` to the controlling player.
  - In the Twine system, the player is NOT a passenger (`cat.getFirstPassenger() == null`). Vanilla `ServerEntity` treats the twined player as a regular remote observer and sends `ClientboundMoveEntityPacket.Rot` and `ClientboundMoveEntityPacket.PosRot` back to the twined client on every server tick.
  - When the client receives these delayed packets, vanilla's `ClientPacketListener` calls `cat.lerpTo()`, forcefully snapping the cat's client-side predicted yaw and position backward to the state from one round-trip latency ago.
  - This results in violent camera twitching, view snapping, and movement stutter whenever moving the mouse or steering.

#### Observation 1.3: Catastrophic Client-Server Dual Simulation of Player Body
- **Files**: `ClientTwineHandler.java:191-224` and `ForgeEvents.java:221-284`
- **Verbatim Code on Client**:
  ```java
  double distSq = mc.player.distanceToSqr(cat);
  if (distSq > 256.0D) { // > 16 blocks: teleport behind cat
      Vec3 behind = cat.position().subtract(cat.getLookAngle().scale(2.5D));
      mc.player.setPos(behind.x, cat.getY(), behind.z);
  } else if (distSq > 9.0D) {
      Vec3 diff = cat.position().subtract(mc.player.position());
      Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize();
      ...
      mc.player.setDeltaMovement(dir.x * followSpeed, mc.player.getDeltaMovement().y, dir.z * followSpeed);
  ```
- **Verbatim Code on Server**:
  ```java
  double distSq = sp.distanceToSqr(cat);
  if (distSq > 1024.0D) { // > 32 blocks
      sp.teleportTo(cat.getX() - cat.getLookAngle().x * 2.0D, cat.getY(), cat.getZ() - cat.getLookAngle().z * 2.0D);
  }
  // Find threats ... attack threats ... else follow cat:
  sp.lookAt(Anchor.EYES, cat.position());
  sp.setDeltaMovement(dir.x * speed, sp.getDeltaMovement().y, dir.z * speed);
  sp.hurtMarked = true;
  ```
- **Finding**: Both client and server run conflicting, uncoordinated physics and teleportation routines on the player's physical body:
  1. Client teleports at distance > 16 blocks (`distSq > 256.0D`) using `mc.player.setPos()`; Server teleports at distance > 32 blocks (`distSq > 1024.0D`) using `sp.teleportTo()`.
  2. The server commands the player body to turn and attack nearby hostile mobs (`sp.lookAt(threat)` and `sp.attack(threat)`), while the client commands the player body to run toward the cat (`mc.player.setDeltaMovement(dir)`).
  3. Minecraft player physics are client-authoritative with server validation. The server setting `sp.setDeltaMovement()` directly triggers vanilla "Player moved wrongly!" and "Player moved too quickly!" checks, resulting in rubberbanding and erratic vibration in multiplayer.
  4. Neither routine uses pathfinding: both use raw Euclidean vectors (`dir = diff.normalize()`), causing the player body to walk directly off cliffs, into lava pits, and get stuck against 1-block walls.

#### Observation 1.4: Inability to Swim or Ascend in Water (Drowning Bug)
- **Files**: `PacketTwineInput.java:104` and `ClientTwineHandler.java:169`
- **Verbatim Code in PacketTwineInput**:
  ```java
  if (jump) {
      if (cat.getTalentLevel(Talents.SKY_FELINE) > 0) {
          ...
      } else if (cat.getTalentLevel(Talents.FELINE_AGILITY) >= 3 && !cat.onGround() && cat.horizontalCollision) {
          ...
      } else if (cat.onGround()) {
          cat.doJump();
      }
  }
  ```
- **Verbatim Code in ClientTwineHandler**:
  ```java
  if (jump && cat.onGround()) {
      cat.doJump();
  }
  ```
- **Finding**: When a cat enters water or lava, `cat.onGround()` evaluates to `false`. Jump inputs are completely ignored in fluids on both client and server. The cat cannot swim up, sinking to the seafloor and drowning even when spacebar is held down.

#### Observation 1.5: Missing Client Prediction for Special Movements (Flight, Wall Leap, Pounce)
- **Files**: `PacketTwineInput.java:93-142` vs `ClientTwineHandler.java:161-172`
- **Finding**:
  - `SKY_FELINE` flight ascending (`motion.y + 0.18D`), `FELINE_AGILITY` wall leap (`motion.y = 0.45D`), and `POUNCE` leap velocity (`look.scale(1.35D)`) are executed ONLY on the server in `PacketTwineInput.handle()`.
  - `ClientTwineHandler.onClientTick()` implements NONE of these movement adjustments. On the client, the cat experiences standard gravity and stops at walls.
  - When the server packet arrives, the cat's position is suddenly teleported or lerped on the client, producing extreme camera jerking and visual stutter.

#### Observation 1.6: Uncapped 20 Hz Attack & Pounce Spam (Zero Cooldown)
- **File**: `PacketTwineInput.java:110-142`
- **Finding**: `PacketTwineInput` executes every client tick (20 Hz). If the player holds down left-click or right-click:
  ```java
  if (attack) {
      ...
      for (LivingEntity target : reach) {
          cat.doHurtTarget(target);
          break;
      }
  }
  ```
  Damage is dealt every 50 ms (20 times per second), resetting attack tickers, producing sound spam, and obliterating balance.

#### Observation 1.7: Sprinting is Non-Functional
- **Files**: `ClientTwineHandler.java:134-147` and `TalentedCat.java:622,660,1339`
- **Finding**: `TalentedCat` movement code contains multipliers `if (this.isSprinting()) speed *= 1.35F;`, but `ClientTwineHandler.java` NEVER checks `mc.options.keySprint.isDown()` and never calls `cat.setSprinting(true)`. Sprinting is impossible while twined.

#### Observation 1.8: Sneak/Shift Premature Untwine (Inability to Prowl/Crouch)
- **File**: `ClientTwineHandler.java:128-132`
- **Finding**: `mc.options.keyShift.isDown()` triggers instant `requestUntwine()`. Players cannot crouch, stealth-prowl (crucial for `SHADOW_PELT`), or prevent walking off ledges.

#### Observation 1.9: Dead MoveControl and LookControl Classes
- **File**: `TalentedCat.java:610-647, 650-663`
- **Finding**: `TalentedCat.customServerAiStep()` returns immediately at line 662 when `isTwined()` is true, completely bypassing `super.customServerAiStep()`. In vanilla `Mob`, `this.moveControl.tick()` and `this.lookControl.tick()` are invoked inside `customServerAiStep()`. Thus, `TalentedCatMoveControl` and `TalentedCatLookControl` are never executed while twined.

#### Observation 1.10: Step-Height Obstruction (0.6 Block Stumble)
- **Files**: `TalentedCat.java:650-675`
- **Finding**: Vanilla Cat's `maxUpStep` is 0.6 blocks. In `TalentedCat.customServerAiStep()`, the auto-jump check (`this.horizontalCollision -> getJumpControl().jump()`) is skipped because line 662 returns early. As a result, the twined cat gets stuck against any 1-block terrain elevation, slab, or staircase unless the player manually times a jump.

#### Observation 1.11: Third-Person Front Camera Inversion
- **File**: `ClientTwineHandler.java:303-304`
- **Finding**: In vanilla third-person front view (F5 twice), Minecraft rotates the camera 180 degrees to look at the entity's face. `ComputeCameraAngles` blindly calls `event.setYaw(cat.getYRot())`, overriding the 180-degree flip and causing third-person front to look behind the cat.

#### Observation 1.12: Mount & Twine State Machine Collision
- **Files**: `PacketTwineAction.java:45-66` and `TalentedCat.java:1335-1385, 1983-1988`
- **Finding**: `PacketTwineAction` does not check if `sender.getVehicle() == cat` or if `cat.isVehicle()`. If a player mounts a cat via `CAT_MOUNT` and then activates Twine, `travel()` switches from passenger mode to twine mode while the player remains seated, locking inputs.

---

### 1.3 Direct Observations of AI Logic & Doggy Talents Parity (R3)

#### Observation 3.1: AI Goal Priority Inversion
- **File**: `TalentedCat.java:233-286`
- **Table of Existing Priorities**:
  | Priority | Goal | Condition / Issue |
  |---|---|---|
  | 0 | `FloatGoal` | Works properly |
  | 1 | `SitWhenOrderedToGoal` | Bypassed when twined |
  | 2 | `CatFoxPounceGoal` | Multi-phase leap; fails to check POUNCE talent level |
  | 2 | `CatGunAttackGoal` | TacZ ranged attack; continuous moveTo spam |
  | 3 | `MeleeAttackGoal` | Checks sit/incapacitated |
  | 4 | `CatCreeperWardenGoal` | Scans entities every tick with no flags |
  | 5 | `CatRestOnBedGoal` | **Higher priority than FollowOwnerGoal!** |
  | 6 | `CatUseScratcherGoal` | **Higher priority than FollowOwnerGoal!** |
  | 7 | `CatEatFromBowlGoal` | **Higher priority than FollowOwnerGoal!** |
  | 8 | `CatFetchToyGoal` | **Higher priority than FollowOwnerGoal!** |
  | 9 | `CatFisherGoal` | **Higher priority than FollowOwnerGoal!** |
  | 10 | `FollowOwnerGoal` | Priority 10; start distance 10.0 blocks |
  | 11 | `CatMoleMiningGoal` | Priority 11; unthrottled block dig |
  | 11 | `WaterAvoidingRandomStrollGoal` | Priority 11 |
  | 12 | `CatPlaceTorchGoal` | Heavy $O(r^3)$ loop |
- **Impact**: Goals 5–9 (bed resting, scratching, eating from bowl, fetching toys, fishing) possess higher priority than `FollowOwnerGoal` (priority 10). If a cat is injured, slightly hungry (< 96/120), near water, or near a bed, it completely ignores owner movement and abandons the player.

#### Observation 3.2: Critical Vector Normalization NaN Crashes
- **Files**:
  - `CatFoxPounceGoal.java:83-90`: `double dist = Math.sqrt(...); cat.setDeltaMovement(toTarget.x / dist * speed, ...)` -> if `dist == 0`, produces `NaN`.
  - `PacketWhistleCommand.java:128`: `push = m.position().subtract(cat.position()).normalize().scale(1.2D);` -> if positions match, `normalize()` returns `(NaN, NaN, NaN)`.
  - `TalentedCat.java:1738`: `toCat = this.position().subtract(item.position()).normalize().scale(0.35D);` -> NaN on identical coordinates.
  - `TalentedCat.java:1752`: `blast = m.position().subtract(this.position()).normalize().scale(1.8D);` -> NaN on identical coordinates.
  - `TalentedCat.java:1639`: `push = m.position().subtract(this.position()).normalize().scale(...);` -> NaN on identical coordinates.
- **Impact**: Setting NaN velocities in Minecraft causes entities to disappear, triggers server-side `Entity moved wrongly! (NaN)` spam, or crashes the physics engine.

#### Observation 3.3: Infinite Airborne Lock in `CatFoxPounceGoal`
- **File**: `CatFoxPounceGoal.java:61-64, 94-118`
- **Finding**: `canContinueToUse()` returns true while `phase != 0`. Phase 2 transitions to stop only if `distSq <= 4.0D` or `cat.onGround() && deltaMovement.y <= 0`. If the cat lands in water, cobwebs, or misses over a cliff, `cat.onGround()` is false, and it has no tick timeout. The cat becomes permanently locked in airborne state with `MOVE`, `LOOK`, and `JUMP` flags disabled.

#### Observation 3.4: Server Crash on Talent Level >= 10 (`IllegalArgumentException: bound must be positive`)
- **Files**:
  - `CatFisherGoal.java:34`: `cat.getRandom().nextInt(100 - (fisherLevel * 10))` -> at `fisherLevel >= 10`, bound is `<= 0`.
  - `CatMoleMiningGoal.java:40`: `cat.getRandom().nextInt(150 - (moleLvl * 15))` -> at `moleLvl >= 10`, bound is `<= 0`.
- **Finding**: Calling `Random.nextInt(<= 0)` throws an unhandled `IllegalArgumentException`, crashing the dedicated server or singleplayer world.

#### Observation 3.5: Permanent Sit-Lock in `CatRestOnBedGoal`
- **File**: `CatRestOnBedGoal.java:61, 89-93`
- **Finding**: When reaching a bed, line 61 executes `cat.setOrderedToSit(true)`. The `stop()` method (lines 89-93) never clears `orderedToSit`. The cat remains sitting permanently after waking up or healing until manually interacted with.

#### Observation 3.6: Orphaned & Dead AI Goals
- **`CatPounceAttackGoal.java`**: Implemented with leap, claw damage, slowness, and POUNCE talent integration, but NEVER registered in `TalentedCat.registerGoals()`.
- **`UseAbilityGoal.java`**: RPG ability casting goal. Implemented in `com.kittytalents.ai.UseAbilityGoal`, but NEVER registered in `registerGoals()`. Furthermore, line 48 invokes `best.tryCast(cat, null)` inside `canUse()`, violating Minecraft's goal evaluation contract.

#### Observation 3.7: Incomplete & Broken Modes (`CatMode`)
- **File**: `CatMode.java` and `TalentedCat.java:233-359`
- **Finding**:
  - `BERSERKER`: Not included in `NearestAttackableTargetGoal` or `FollowOwnerGoal`. Cats in Berserker mode do not seek enemies and do not follow the owner.
  - `PATROL`: Mapped directly to `WaterAvoidingRandomStrollGoal` (identical to WANDER). Has no waypoint, post, or path logic.
  - `TACTICAL`: Follows owner, but has no tactical positioning or ranged flanking behavior.
  - `STAY`: `setCatMode()` sets `orderedToSit(false)`, leaving the cat standing frozen with no goal.
  - `PROTECT`: Not included in `FollowOwnerGoal` or attack targeting.

#### Observation 3.8: Whistle Command Deficiencies & Absurdities
- **File**: `PacketWhistleCommand.java:64-167`
- **Finding**:
  - `QUIET` (lines 159-161): Intended to silence meowing; actually grants `MobEffects.INVISIBILITY` for 5 minutes (6000 ticks)!
  - `FLY` (lines 155-157): Executes `cat.startRiding(player)`, placing the cat on top of the player instead of enabling mount flight.
  - `STAND` (lines 77-79): Sets `orderedToSit(false)`, but leaves mode in `DOCILE`. The cat immediately sits back down.
  - `EAT`, `FISH`, `MINE` (lines 116, 148, 151): Simply call `setOrderedToSit(false)` without initiating bowl search, fishing, or mining.

#### Observation 3.9: Starvation with Full Backpacks (Zero Auto-Feeding)
- **File**: `TalentedCat.java:1509-1522`
- **Finding**: Starvation damage is inflicted at 0 hunger. The cat never checks its 27-slot `catPack` for food. In Doggy Talents, pets consume food from inventory when hunger < 80 or health is low. In Kitty Talents, cats starve to death while carrying stacks of fish and cat treats.

#### Observation 3.10: Unimplemented Talents
- **`CRITICAL_CLAWS`**: Registered in `Talents.java:28`; never checked or implemented in `doHurtTarget()`.
- **`FELINE_REFLEXES`**: Registered in `Talents.java:27`; never checked in `hurt()`.
- **`HOME_WHISKERS`**: Registered in `Talents.java:36`; never referenced.
- **`CUSHION_PAWS`**: Registered in `Talents.java:52`; never referenced in fall damage or movement.
- **`GATE_PURR`**: Opens fence gates at `TalentedCat:1655` but never closes them, releasing livestock.
- **`KittyFeedingStationBlock`**: Blocks hold food (`KittyFeedingStationBlockEntity:25-38`), but `CatEatFromBowlGoal` only checks `CatFoodBowlBlock`. Cats never eat from feeding stations.

---

## 2. Logic Chain

1. **Premise 1 (R1 Root Cause)**: The camera stutter, rubberbanding, and movement desync reported in R1 are caused by three specific flaws:
   - `ComputeCameraAngles` overrides rendered camera yaw/pitch using raw tick values without `rotLerp(partialTick)`.
   - The vanilla server echoes movement packets back to the controlling client because the player is not marked as a controlling passenger.
   - Dual uncoordinated physics loops in `ClientTwineHandler` and `ForgeEvents` fight over the player's position and velocity.

2. **Premise 2 (R1 Input & Movement Flaws)**: Twine control is unresponsive or broken in common scenarios:
   - In water/fluids, jump input requires `onGround()`, drowning the cat.
   - Advanced movement (`SKY_FELINE`, `FELINE_AGILITY`, `POUNCE`) is executed server-side only with no client-side prediction.
   - Sprinting keybinds are not connected; crouching/sneaking aborts Twine instead of allowing stealth.
   - Custom `MoveControl` and `LookControl` are dead code bypassed by early return in `customServerAiStep()`.
   - Low step-height (0.6) stops the cat at every 1-block terrain elevation.

3. **Premise 3 (R3 AI Priority Inversion)**: Companion cats behave erratically because utility goals (beds, bowls, scratchers, fishing) are assigned priorities 5–9, outranking `FollowOwnerGoal` (priority 10). The moment a cat is slightly hungry or injured, it breaks off from following the owner.

4. **Premise 4 (R3 Stability & Crash Risks)**: Several existing AI routines contain severe bugs:
   - Vector calculations divide by zero or normalize zero-length vectors, producing `NaN` velocities that corrupt entity tracking.
   - Fisher and Mole Mining goals crash the server at talent level >= 10 due to `Random.nextInt(<= 0)`.
   - `CatFoxPounceGoal` lacks a tick timeout, causing infinite lockup when landing in water or cobwebs.
   - `CatRestOnBedGoal` permanently sets sit mode.

5. **Premise 5 (R3 Doggy Talents Parity Gaps)**: To achieve parity with Doggy Talents:
   - Companion modes (`BERSERKER`, `PATROL`, `TACTICAL`, `STAY`, `PROTECT`, `GUARD`, `FOLLOW`, `DOCILE`, `WANDER`, `HUNTING`, `AGGRESSIVE`) must have full AI implementations.
   - Whistle commands must trigger actual behaviors instead of placeholders or joke effects.
   - Autonomous feeding from inventory (`catPack`) and feeding stations must be established.
   - Unimplemented talents (`CRITICAL_CLAWS`, `FELINE_REFLEXES`, `HOME_WHISKERS`, `CUSHION_PAWS`) must be fully realized.

---

## 3. Caveats

- **Network Protocol Constraints**: Modifying entity tracking packet suppression requires careful Forge/Minecraft network direction handling or custom clientbound packet routing to ensure other tracking players still see the cat moving smoothly while suppressing echoes to the controlling player.
- **TacZ Compatibility**: Ranged attack logic in `CatGunAttackGoal` relies on `TacZCompat`. If TacZ is not installed, the compat layer properly stubs out calls, but AI priorities must ensure cats fall back seamlessly to melee or pounce attacks.
- **Dedicated Server Class Safety**: Any client-side camera or keybind references must strictly reside in `@Mod.EventBusSubscriber(Dist.CLIENT)` classes to avoid `NoClassDefFoundError` on dedicated servers (as identified in peer survey reports).

---

## 4. Conclusion

The Twine control system (R1) and AI logic (R3) currently suffer from fundamental design flaws and implementation bugs:
1. **Twine Desyncs & Camera Stutter** stem from lack of partial-tick interpolation in `ComputeCameraAngles`, network echo packet overrides, and dual client/server player simulation.
2. **Twine Movement Flaws** include fluid drowning, lack of client prediction for talents, missing sprint/sneak handling, and step-height obstruction.
3. **AI Logic Overhaul** requires fixing inverted goal priorities, eliminating `NaN` vector corruption, resolving crash bugs in Fisher/Mole goals, finishing incomplete modes, correcting whistle commands, enabling backpack auto-feeding, and implementing missing talents.

### Concrete Recommendations for Implementation:
- **Twine Camera**: In `ClientTwineHandler.onComputeCameraAngles`, interpolate rotation using `Mth.rotLerp(partialTick, cat.yRotO, cat.getYRot())`. Respect third-person front view inversion (flip 180°).
- **Twine Networking**: Implement client-authoritative cat movement or suppress `ServerEntity` position/rotation echoes to the twined player; reconcile only on significant divergence (> 2.0 blocks).
- **Player Dormancy**: In Twine mode, place the player's physical body in a safe, dormant, damage-resistant pose rather than running competing naive pathing simulations on client and server.
- **Twine Movement**: Allow fluid jumping/swimming up; add client-side prediction for pounce, flight, and wall leap; bind sprint key to cat sprinting; rebind sneak to feline crouch/stalk; grant temporary 1.0 block step-assist while twined.
- **AI Goals**: Restructure goal priorities so `FollowOwnerGoal` and combat goals take precedence over utility goals when active; add 60-tick safety timeout to `CatFoxPounceGoal`; fix `Random.nextInt` bounds (`Math.max(1, bound)`); implement inventory auto-feeding; implement all missing talents.

---

## 5. Verification Method

To independently verify these findings:
1. **Compilation Check**:
   ```powershell
   .\gradlew.bat compileJava
   ```
2. **Inspect Twine Camera Code**:
   View `src/main/java/com/kittytalents/client/ClientTwineHandler.java:301-306` to verify raw tick getters without `partialTick`.
3. **Inspect Dual Player Movement**:
   Compare `ClientTwineHandler.java:191-224` with `ForgeEvents.java:221-284`.
4. **Inspect Fluid Jump Check**:
   View `PacketTwineInput.java:104` to verify `cat.onGround()` prevents swimming up in fluids.
5. **Inspect Server Crash Vectors**:
   View `CatFisherGoal.java:34` and `CatMoleMiningGoal.java:40` to confirm `100 - (fisherLevel * 10)` becomes `<= 0` at level >= 10.
6. **Inspect Goal Priorities**:
   View `TalentedCat.java:234-263` to confirm utility goals (5–9) are scheduled above `FollowOwnerGoal` (10).
7. **Invalidation Condition**:
   This survey is invalidated if `ClientTwineHandler` is proven to interpolate partial ticks, if `ServerEntity` is already suppressing packet echoes to twined players, or if `CatFisherGoal` has safety clamps against zero random bounds.
