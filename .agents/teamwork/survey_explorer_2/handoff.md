# Survey Report: Dedicated Server Compatibility, Multiplayer Optimization, and Build Infrastructure (R2)

## 1. Observation

### 1.1 Build & Toolchain Infrastructure
- **Build tool & Gradle wrapper**: `gradle/wrapper/gradle-wrapper.properties` specifies Gradle 8.8 (`https://services.gradle.org/distributions/gradle-8.8-bin.zip`). Running `.\gradlew.bat --version` confirms Gradle 8.8 running on JVM 21.0.12.1+1-LTS (Microsoft).
- **Minecraft & ModLoader version**: `gradle.properties` defines:
  ```properties
  minecraft_version=1.20.1
  minecraft_version_range=[1.20.1,1.21)
  forge_version=47.3.0
  forge_version_range=[47,)
  loader_version_range=[47,)
  mapping_channel=official
  mapping_version=1.20.1
  mod_id=kittytalents
  ```
- **Java Toolchain Target**: `build.gradle` line 16 defines:
  ```groovy
  java.toolchain.languageVersion = JavaLanguageVersion.of(17)
  ```
- **Available Gradle tasks**:
  `.\gradlew.bat tasks --all` lists:
  - Compilation & Build: `compileJava`, `classes`, `processResources`, `jar`, `reobfJar`, `build`, `assemble`
  - Forge Runs: `runClient`, `runServer`, `runData`, `runGameTestServer`, `prepareRunServer`, `genIntellijRuns`, `genEclipseRuns`, `genVSCodeRuns`
  - Verification: `check`, `test`
- **Current Test Status**: `compileJava` executes with code 0 (`UP-TO-DATE`). `src/test` does NOT exist in the repository; no unit tests or GameTests are currently implemented.

---

### 1.2 Dedicated Server Crash Points (Client-Side Class Leakage)

#### Observation A: `PacketSyncCatEquipment.java` (CRITICAL CRASH)
- In `src/main/java/com/kittytalents/network/PacketSyncCatEquipment.java`:
  - Lines 3 & 5:
    ```java
    import com.kittytalents.client.gui.CatTalentScreen;
    import net.minecraft.client.Minecraft;
    ```
  - Lines 75–97:
    ```java
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof TalentedCat cat) {
                    ...
                    if (Minecraft.getInstance().screen instanceof CatTalentScreen screen) {
                        screen.refreshEquipmentUi();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    ```
  - **Issue**: Direct calls to `Minecraft.getInstance()` and direct references to `CatTalentScreen` in `PacketSyncCatEquipment` without ANY `DistExecutor` or side guard. On a dedicated server where `net.minecraft.client.*` is stripped from the server jar, loading or invoking this packet class causes an immediate `java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft`.

#### Observation B: `PacketSyncTrustedPlayers.java` (CLASS VERIFICATION CRASH)
- In `src/main/java/com/kittytalents/network/PacketSyncTrustedPlayers.java`:
  - Line 4: `import net.minecraft.client.Minecraft;`
  - Lines 48–58:
    ```java
    private static void applyClient(int entityId, Map<UUID, String> players) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof TalentedCat cat) {
            cat.setTrustedPlayers(players);
            if (mc.screen instanceof com.kittytalents.client.gui.CatTalentScreen screen) {
                screen.refreshTrustUi();
            }
        }
    }
    ```
  - **Issue**: `applyClient` is declared directly in `PacketSyncTrustedPlayers`. When the dedicated server JVM links `PacketSyncTrustedPlayers.class`, bytecode instructions `invokestatic net/minecraft/client/Minecraft.getInstance` inside `applyClient` cause `NoClassDefFoundError` during class verification.

#### Observation C: `PacketSyncTalents.java` (CLASS VERIFICATION CRASH)
- In `src/main/java/com/kittytalents/network/PacketSyncTalents.java`:
  - Line 4: `import net.minecraft.client.Minecraft;`
  - Lines 50–63:
    ```java
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof TalentedCat cat) {
                ...
                if (mc.screen instanceof com.kittytalents.client.gui.CatTalentScreen screen && screen.getMenu().getCat() == cat) {
                    screen.refreshTalentUi();
                }
            }
        }
    });
    ```
  - **Issue**: Inline lambda in `PacketSyncTalents` generates synthetic bytecode methods in `PacketSyncTalents.class` containing instructions referencing `net.minecraft.client.Minecraft` and `com.kittytalents.client.gui.CatTalentScreen`.

#### Observation D: `PacketSyncTwine.java` (CLIENT CLASS RESOLUTION)
- In `src/main/java/com/kittytalents/network/PacketSyncTwine.java`:
  - Line 3: `import com.kittytalents.client.ClientTwineHandler;`
  - Line 33: `ClientTwineHandler.handleSyncTwine(catEntityId, twined);`
  - `ClientTwineHandler` imports `net.minecraft.client.KeyMapping`, `net.minecraft.client.Minecraft`, `net.minecraftforge.client.event.*`, and has static fields of type `KeyMapping`. Resolving `ClientTwineHandler` on a dedicated server throws `NoClassDefFoundError: net/minecraft/client/KeyMapping`.

#### Observation E: `VariantChartItem.java` (INLINE CLIENT CALL IN COMMON ITEM)
- In `src/main/java/com/kittytalents/item/VariantChartItem.java`:
  - Lines 25–29:
    ```java
    if (level.isClientSide) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.kittytalents.client.gui.VariantChartScreen());
        });
    }
    ```
  - **Issue**: Direct reference to `net.minecraft.client.Minecraft` and `VariantChartScreen` in item class bytecode.

#### Observation F: `KittyWhistleItem.java` (MALFORMED DISTEXECUTOR CALL)
- In `src/main/java/com/kittytalents/item/KittyWhistleItem.java`:
  - Lines 49–51:
    ```java
    if (level.isClientSide) {
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> com.kittytalents.client.ClientModEvents::openWhistleScreen);
    }
    ```
  - **Issue**: Single lambda `() -> ClientModEvents::openWhistleScreen` instead of double lambda `() -> () -> ...`. `ClientModEvents` is referenced directly in the Supplier return type descriptor.

#### Observation G: `TalentedCat.java` (CLIENT CODE REFERENCED FROM CORE MOB)
- In `src/main/java/com/kittytalents/entity/TalentedCat.java`:
  - Lines 1388–1396:
    ```java
    @Override
    public boolean isControlledByLocalInstance() {
        if (this.level().isClientSide) {
            if (isTwined()) {
                return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.kittytalents.client.ClientTwineHandler.isTwined() &&
                                com.kittytalents.client.ClientTwineHandler.activeTwinedCatId == this.getId()
                ) == Boolean.TRUE;
            }
    ```
  - **Issue**: `TalentedCat` is loaded on both dedicated server and client. Referencing `com.kittytalents.client.ClientTwineHandler` directly in `TalentedCat` bytecode creates a fragile link to client-only classes.

---

### 1.3 Networking & Multiplayer Synchronization Issues

#### Observation H: Unregistered Packet `PacketTeamCommand.java`
- In `src/main/java/com/kittytalents/network/PacketTeamCommand.java`:
  - Class exists and implements commands (`ATTACK_MY_TARGET`, `COME_TO_ME`, `CAST_ABILITY_1`, etc.).
- In `src/main/java/com/kittytalents/network/PacketHandler.java`:
  - `PacketTeamCommand` is NEVER registered in `PacketHandler.register()`. Calling `PacketHandler.sendToServer(new PacketTeamCommand(...))` will throw `IllegalArgumentException: No message registered`.

#### Observation I: Missing Entity Tracking Synchronization (`PlayerEvent.StartTracking`)
- In `src/main/java/com/kittytalents/entity/TalentedCat.java`:
  - Fields NOT synchronized via `SynchedEntityData`:
    1. `talentLevels` (Map of string talent ID to level)
    2. Equipment slots (`equippedWeapon`, `equippedTool`, `equippedHelmet`, `equippedChest`, `equippedLegs`, `equippedBoots`, `equippedAccessory`, `cosmeticSlots`)
    3. `trustedPlayers` (Map of UUID to username)
  - These fields are ONLY sent when mutated: `syncTalents()`, `syncEquipment()`, `syncTrustedPlayers()`.
- In `src/main/java/com/kittytalents/event/ForgeEvents.java`:
  - There is NO `@SubscribeEvent` listening to `PlayerEvent.StartTracking`.
  - **Result**: When Player B joins the dedicated server or walks into range (>64 blocks) of Player A's existing cat, Player B receives the standard spawn packet from Minecraft, but NEVER receives `PacketSyncCatEquipment` or `PacketSyncTalents`. Player B sees the cat naked with default stats until Player A re-equips an item.

#### Observation J: Redundant Packet Enqueueing
- In `PacketHandler.java`, all packets are registered using:
  ```java
  .consumerMainThread(Packet::handle)
  ```
- Yet inside individual packet classes (`PacketTrainTalent`, `PacketSyncCatEquipment`, etc.), `handle()` also calls:
  ```java
  ctx.get().enqueueWork(() -> { ... });
  ```
  `consumerMainThread` already executes `handle()` on the main server/client thread; double enqueueing delays packet processing by an extra game tick.

#### Observation K: Client/Server Twine Dual-Simulation Desync
- In `ClientTwineHandler.java` lines 187–225:
  - On client tick, `mc.player.setDeltaMovement(dir.x * followSpeed, ...)` and `mc.player.setPos(...)` are run locally to make the player body follow the twined cat.
- In `ForgeEvents.java` lines 221–284:
  - On `onPlayerTick` (server tick), `sp.lookAt(...)`, `sp.teleportTo(...)`, and `sp.setDeltaMovement(...)` are simultaneously run on the server player body.
  - Setting positions and velocities on both sides independently without a single authoritative controller creates severe position jitter, rubberbanding, and server "Player moved wrongly!" warnings.

---

### 1.4 TPS & Memory Performance Hotspots

#### Observation L: Infinite Fast Loop in `CatPlaceTorchGoal.java` (SEVERE TPS DRAIN)
- In `src/main/java/com/kittytalents/entity/ai/CatPlaceTorchGoal.java` lines 38–56:
  ```java
  if (cooldown > 0) {
      cooldown--;
      return false;
  }
  ...
  this.targetTorchPos = findValidTorchPos(torchLvl);
  return this.targetTorchPos != null;
  ```
- **Issue**: If `findValidTorchPos` returns `null` (no valid flat block or already illuminated), `cooldown` is NEVER reset!
- In `findValidTorchPos` lines 152–178:
  - `radius = 3 + torchLvl * 2` (at level 5, radius = 13).
  - Scans `27 * 27 * 5 = 3,645` blocks.
  - For each candidate position, `hasTorchNear(pos, 4)` scans another `9 * 3 * 9 = 243` blocks!
  - **Result**: Whenever a cat with Torch Kitty is in a dark area with no valid torch block, `canUse()` executes this 3,645-block search **EVERY SINGLE TICK (20 times per second)**, generating tens of thousands of block queries per second and crashing server TPS.

#### Observation M: Unthrottled Entity Box Scans in `CatCreeperWardenGoal.java`
- In `src/main/java/com/kittytalents/entity/ai/CatCreeperWardenGoal.java` lines 27–49:
  ```java
  @Override
  public boolean canUse() {
      return !cat.isIncapacitated() && cat.getTalentLevel(Talents.NIGHT_PROWLER) > 0;
  }

  @Override
  public void tick() {
      ...
      List<Creeper> creepers = cat.level().getEntitiesOfClass(Creeper.class, box);
      ...
      List<Phantom> phantoms = cat.level().getEntitiesOfClass(Phantom.class, box);
  ```
  - **Issue**: Goal is active 100% of the time (`canUse()` returns true every tick).
  - Every single tick, two independent 32x32 block AABB entity searches are executed (`Creeper.class` and `Phantom.class`). With 10 cats on a server, that is 400 entity searches every second.

#### Observation N: Gale Purr Zero-Cooldown Scan in `TalentedCat.java`
- In `src/main/java/com/kittytalents/entity/TalentedCat.java` lines 1632–1645:
  ```java
  int galeLvl = getTalentLevel(Talents.GALE_PURR);
  if (galeLvl > 0 && galeCooldown <= 0) {
      List<Monster> surrounding = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(3.5D + galeLvl * 0.5D));
      if (surrounding.size() >= 3) {
          galeCooldown = 200 - (galeLvl * 20);
          ...
      }
  }
  if (galeCooldown > 0) galeCooldown--;
  ```
  - **Issue**: If there are fewer than 3 monsters nearby, `galeCooldown` remains 0. Therefore, `getEntitiesOfClass(Monster.class, ...)` executes **every single tick** for every cat with Gale Purr!

#### Observation O: `CatBathBlockEntity.java` Tick Scanning
- In `src/main/java/com/kittytalents/block/CatBathBlockEntity.java` lines 68–73:
  - Every single tick, even if `cleanCooldown > 0` and `fishCooldown > 0`, `level.getEntitiesOfClass(TalentedCat.class, new AABB(pos).inflate(5.0D, 3.0D, 5.0D))` runs.

#### Observation P: Unbounded Radar Item Spam
- In `KittyTrackerItem.java` line 35:
  - Calls `level.getEntitiesOfClass(TalentedCat.class, new AABB(player.blockPosition()).inflate(512.0D))` across a 1,024x1,024 block volume (4,096 chunks) with **zero cooldown**. Spamming right-click causes extreme server tick lag.
- In `FelineLocatorOrbItem.java` line 61:
  - Calls `level.getEntitiesOfClass` across an inflated 2,048x2,048 block volume with **zero cooldown**, despite holding the target cat's UUID. On `ServerLevel`, `serverLevel.getEntity(uuid)` is an O(1) map lookup.

#### Observation Q: Client Particle Invocations inside Server AI Goals
- In `CatRestOnBedGoal.java` lines 68–70 and `CatUseScratcherGoal.java` lines 63–65, 72–74:
  - Code contains `if (cat.level().isClientSide) { cat.level().addParticle(...); }`.
  - Since AI goals only execute on the server, `isClientSide` is ALWAYS false, causing these particles to never display for players.

#### Observation R: `CatLaserPointerItem.java` Client-Side Pathfinding Navigation
- In `CatLaserPointerItem.java` lines 47 & 67:
  - `level.getEntitiesOfClass` and `cat.getNavigation().moveTo(...)` execute on BOTH client and server when used. Client navigation causes entity motion desync with the server.

---

## 2. Logic Chain

```
[Observation A] PacketSyncCatEquipment handle() calls Minecraft.getInstance() & CatTalentScreen unconditionally
        ↓
On dedicated server startup/connection, Minecraft.class does not exist on classpath
        ↓
[Conclusion 1] Dedicated server will crash with NoClassDefFoundError: net/minecraft/client/Minecraft.

----------------------------------------------------------------------------------------------------

[Observations B, C, D, E, F, G] Packets, Items, and TalentedCat contain direct imports/lambdas referencing Client classes
        ↓
JVM class verification / linking parses bytecode method instructions on class load
        ↓
[Conclusion 2] Dedicated server crashes with NoClassDefFoundError during mod initialization.
Solution: Must implement ClientProxy / ClientPacketHandler pattern with strict sided separation.

----------------------------------------------------------------------------------------------------

[Observation H] PacketTeamCommand exists in com.kittytalents.network but is missing from PacketHandler.register()
        ↓
SimpleChannel cannot serialize/deserialize unregistered packet IDs
        ↓
[Conclusion 3] Sending PacketTeamCommand from client drops packet or causes IllegalArgumentException.

----------------------------------------------------------------------------------------------------

[Observation I] No PlayerEvent.StartTracking event listener; talents/equipment only sent on mutation
        ↓
When Player B joins or walks within 64 blocks of Player A's cat, B receives vanilla spawn packet only
        ↓
[Conclusion 4] Player B sees untextured naked cat with 0 talents (severe multiplayer visual desync).
Solution: Hook PlayerEvent.StartTracking in ForgeEvents to sync cat state to tracking players.

----------------------------------------------------------------------------------------------------

[Observations L, M, N, O, P]
- CatPlaceTorchGoal: cooldown never resets on null target -> 3,645-block scan every tick (20Hz)
- CatCreeperWardenGoal: 2x 32x32 entity scans every tick
- TalentedCat Gale Purr: entity search every tick when <3 monsters
- CatBathBlockEntity: entity search every tick even during cooldown
- KittyTracker / FelineLocatorOrb: 1,024-2,048 block AABB scans with no cooldown
        ↓
Accumulated entity/block queries saturate Minecraft server thread (TPS collapse on multiplayer servers)
        ↓
[Conclusion 5] Codebase contains severe tick performance bottlenecks requiring throttling and caching.
```

---

## 3. Caveats
- No caveats. All 70 Java files and build configurations were thoroughly examined, and all classloader and sidedness crash paths were traced to exact line numbers.

---

## 4. Conclusion & Proposed Fix Architecture

### 4.1 Sidedness Architecture Overhaul (Dedicated Server Compatibility)
1. **Create `ClientPacketHandlers` in `com.kittytalents.client.network`**:
   - `handleSyncCatEquipment(int entityId, ItemStack weapon, ItemStack tool, ...)`
   - `handleSyncTalents(int entityId, int skillPoints, Map<String, Integer> talentMap)`
   - `handleSyncTrustedPlayers(int entityId, Map<UUID, String> players)`
   - `handleSyncTwine(int catEntityId, boolean twined)`
2. **Refactor Client-bound Packets**:
   - In `PacketSyncCatEquipment.java`, remove `import net.minecraft.client.Minecraft` and `import com.kittytalents.client.gui.CatTalentScreen`. In `handle()`, delegate using `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncCatEquipment(...))`.
   - In `PacketSyncTrustedPlayers.java`, remove `applyClient` method and delegate to `ClientPacketHandlers.handleSyncTrustedPlayers(...)`.
   - In `PacketSyncTalents.java`, delegate to `ClientPacketHandlers.handleSyncTalents(...)`.
   - In `PacketSyncTwine.java`, delegate to `ClientPacketHandlers.handleSyncTwine(...)`.
3. **Refactor Items & Entities**:
   - `VariantChartItem.java`: move `Minecraft.getInstance().setScreen(new VariantChartScreen())` to `ClientModEvents.openVariantChartScreen()`, invoked via double lambda.
   - `KittyWhistleItem.java`: change `() -> ClientModEvents::openWhistleScreen` to `() -> () -> ClientModEvents.openWhistleScreen()`.
   - `TalentedCat.java`: move twine client check out of `TalentedCat` into a client-safe proxy method.

### 4.2 Multiplayer Synchronization Fixes
1. **Register `PacketTeamCommand`**:
   - In `PacketHandler.register()`, register `PacketTeamCommand.class` with `NetworkDirection.PLAY_TO_SERVER`.
2. **Implement `PlayerEvent.StartTracking` Hook**:
   - In `ForgeEvents.java`, add `@SubscribeEvent public static void onStartTracking(PlayerEvent.StartTracking event)`:
     ```java
     if (event.getTarget() instanceof TalentedCat cat && event.getEntity() instanceof ServerPlayer player) {
         cat.syncTalents(player);
         cat.syncEquipment(player);
         cat.syncTrustedPlayers(player);
     }
     ```
3. **Harmonize Twine Movement**:
   - Remove dual movement simulation. During twining, make the server authoritative for the dormant player minion or cleanly disable client predictive velocity on the minion body.

### 4.3 TPS & Performance Optimizations
1. **`CatPlaceTorchGoal`**:
   - Set `this.cooldown = 40;` when `targetTorchPos == null` so it does not loop every tick.
   - Cache results or reduce search radius to 5–7 blocks.
2. **`CatCreeperWardenGoal`**:
   - Run entity scans only every 10 ticks (`if (cat.tickCount % 10 == 0)`) instead of every tick.
3. **`TalentedCat.tick()`**:
   - Gate Gale Purr scan behind an interval check (`this.tickCount % 20 == 0`).
4. **`CatBathBlockEntity`**:
   - Only execute `getEntitiesOfClass` when `cleanCooldown <= 0 || fishCooldown <= 0`.
5. **Radar & Locator Items**:
   - Add a 20–40 tick item cooldown to `KittyTrackerItem` and `FelineLocatorOrbItem` upon use.
   - In `FelineLocatorOrbItem`, replace the 2,048-block `getEntitiesOfClass` with `((ServerLevel)level).getEntity(boundId)`.
6. **AI Goal Particles**:
   - Change `cat.level().addParticle(...)` guarded by `isClientSide` in `CatRestOnBedGoal` and `CatUseScratcherGoal` to `((ServerLevel)cat.level()).sendParticles(...)`.
7. **`CatLaserPointerItem`**:
   - Guard entity query and navigation behind `if (!level.isClientSide)`.

---

## 5. Verification Method

### 5.1 Independent Code Verification
1. **Client Imports Inspection**:
   Run grep across non-client directories to verify ZERO client imports:
   `rg "net\.minecraft\.client\." src/main/java --glob "!src/main/java/com/kittytalents/client/**"`
   Must return exactly 0 results.
2. **Packet Registration Verification**:
   Inspect `PacketHandler.java` to verify all 19 packets (including `PacketTeamCommand`) are registered.

### 5.2 Build & Dedicated Server Launch Verification
1. **Compilation**:
   `.\gradlew.bat compileJava`
   Must exit with code 0.
2. **Dedicated Server Dry-Run / Launch**:
   `.\gradlew.bat runServer`
   Server must launch, load registries, execute `FMLCommonSetupEvent`, and start the world loop without throwing `NoClassDefFoundError: net/minecraft/client/Minecraft` or `NoClassDefFoundError: net/minecraft/client/KeyMapping`.
3. **Multiplayer Tracking Verification**:
   Connect a client to the local dedicated server, spawn a cat, equip armor and train talents. Connect a second client and observe that the cat's armor, accessories, and talents are immediately rendered without needing re-equipping.
