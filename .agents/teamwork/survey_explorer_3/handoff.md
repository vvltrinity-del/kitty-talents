# Survey Report: R2 (Performance, TPS & Memory Optimization)

## 1. Observation

A full codebase survey of `KittyTalents` was conducted across entity ticking logic, AI goals, block entities, event listeners, network packets, compatibility layers, and item handlers. The following specific bottlenecks, leak vectors, and bugs were observed:

### Observation 1: Global Entity Tick Hook in `FelineStatusEffects.java`
- **File**: `com/kittytalents/combat/FelineStatusEffects.java:27-36, 96-103`
- **Verbatim Code**:
```java
@SubscribeEvent
public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity.level().isClientSide || !entity.isAlive()) return;
    
    CompoundTag data = entity.getPersistentData();
    if (!data.contains("FelineStatuses")) return;
    
    CompoundTag statuses = data.getCompound("FelineStatuses");
```
and:
```java
    effectData.putInt("Ticks", ticks - 1);
    changed = true;
}
if (changed) {
    data.put("FelineStatuses", statuses);
}
```
- **Context**: Subscribes to Forge's `LivingTickEvent` which fires for **every living entity** in the entire world (players, monsters, animals, ambient bats, water creatures) on every single tick (20 Hz). Performs persistent NBT allocations/lookups on every entity every tick, and mutates persistent NBT tags each tick when statuses are active.

---

### Observation 2: Pathfinding Spikes & Unthrottled Navigation Recalculation
- **File 2a**: `com/kittytalents/entity/ai/CatRestOnBedGoal.java:78-81`
```java
} else {
    cat.getNavigation().moveTo(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D, 1.2D);
}
```
`tick()` executes every single tick; whenever distance to bed is >= 2.5 blocks, it requests a new path calculation (`moveTo`) every 50ms without cooldown.

- **File 2b**: `com/kittytalents/entity/ai/CatPounceAttackGoal.java:84-87`
```java
} else {
    cat.getNavigation().moveTo(target, 1.25D);
}
```
`tick()` executes every single tick; if `distSq > 4.0D` or `attackCooldown > 10`, it invokes `cat.getNavigation().moveTo(target, ...)` every single tick instead of using a standard navigation cooldown timer (`ticksUntilNextPathRecalculation`).

- **File 2c**: `com/kittytalents/entity/ai/CatGunAttackGoal.java:66-70`
```java
if (distSq > maxDistSq) { // Advance
    cat.getNavigation().moveTo(target, speedModifier);
} else if (distSq < minDistSq) { // Back away
    cat.getNavigation().moveTo(cat.getX() - (target.getX() - cat.getX()), cat.getY(), cat.getZ() - (target.getZ() - cat.getZ()), speedModifier * 1.05D);
}
```
Invokes path calculation on every tick for advancing or retreating.

- **File 2d**: `com/kittytalents/entity/ai/CatFetchToyGoal.java:62, 80`
```java
cat.getNavigation().moveTo(owner, 1.35D);
...
cat.getNavigation().moveTo(targetToy, 1.45D);
```
Calls `moveTo()` on every tick in `tick()`.

- **File 2e**: `com/kittytalents/entity/ai/CatEatFromBowlGoal.java:114-118` & `CatPlaceTorchGoal.java:87-89`
```java
if (cat.getNavigation().isDone()) {
    double speed = cat.getHunger() < 40 ? 1.35D : 1.15D;
    cat.getNavigation().moveTo(bowlPos.getX() + 0.5D, bowlPos.getY(), bowlPos.getZ() + 0.5D, speed);
}
```
When path navigation gets stuck or obstructed, `isDone()` returns `true` continuously, resulting in an unthrottled path recalculation loop every single tick.

---

### Observation 3: Heavy World Block Scans in AI Goals
- **File 3a**: `com/kittytalents/entity/ai/CatEatFromBowlGoal.java:41-53`
```java
checkCooldown = 30; // Check every 1.5s
BlockPos catPos = cat.blockPosition();
int searchRadius = cat.getHunger() < 40 ? 18 : 12;

for (BlockPos pos : BlockPos.betweenClosed(catPos.offset(-searchRadius, -2, -searchRadius), catPos.offset(searchRadius, 2, searchRadius))) {
    if (cat.level().getBlockState(pos).getBlock() instanceof CatFoodBowlBlock) {
```
Scans 25 x 5 x 25 = **3,125 blocks** (or 37 x 5 x 37 = **6,845 blocks** when hungry) every 30 ticks (1.5 seconds) per cat. If no food bowl is present, it visits thousands of chunk block states repeatedly.
Furthermore, line 70-71 in `canContinueToUse()` calls `cat.level().getBlockEntity(bowlPos)` and `bowl.getFirstEdibleFood(cat)` on **every single tick**.

- **File 3b**: `com/kittytalents/entity/ai/CatPlaceTorchGoal.java:47-56, 152-177, 191-199`
```java
// Search for valid legal torch spot
this.targetTorchPos = findValidTorchPos(torchLvl);
return this.targetTorchPos != null;
```
When `targetTorchPos` is `null` (e.g. no legal spot), `cooldown` is **not reset** and remains `0`. On the very next tick, `canUse()` executes `findValidTorchPos` again.
`findValidTorchPos` tests a volume of $(2r+1)^2 \times 5 = 27 \times 27 \times 5 = \mathbf{3,645\text{ blocks}}$ at level 5. For each candidate empty air block, it invokes `hasTorchNear(pos, 4)`:
```java
for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius)))
```
which runs a nested iteration of $9 \times 3 \times 9 = \mathbf{243\text{ blocks}}$. In dark caves without valid placements, this executes tens of thousands of block lookups **every single tick** (50ms).

- **File 3c**: `com/kittytalents/entity/ai/CatUseScratcherGoal.java:35-40`
```java
for (BlockPos pos : BlockPos.betweenClosed(catPos.offset(-8, -2, -8), catPos.offset(8, 2, 8))) {
    if (cat.level().getBlockState(pos).getBlock() instanceof CatScratcherBlock) {
```
Scans $17 \times 5 \times 17 = \mathbf{1,445\text{ blocks}}$ every 100 ticks per cat if no scratcher is found.

- **File 3d**: `com/kittytalents/entity/TalentedCat.java:1649-1659` (`Gate Purr`)
```java
for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
    net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(p);
```
Scans 75 block positions every 20 ticks per cat.

---

### Observation 4: Unthrottled Spatial Queries & Redundant Ticking Calculations
- **File 4a**: `com/kittytalents/entity/TalentedCat.java:1632-1644` (`Gale Purr`)
```java
int galeLvl = getTalentLevel(Talents.GALE_PURR);
if (galeLvl > 0 && galeCooldown <= 0) {
    List<Monster> surrounding = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(3.5D + galeLvl * 0.5D));
    if (surrounding.size() >= 3) {
```
When `galeCooldown <= 0` and there are fewer than 3 monsters, `galeCooldown` remains 0. Therefore, `getEntitiesOfClass(Monster.class, ...)` runs **every single tick**.

- **File 4b**: `com/kittytalents/entity/ai/CatCreeperWardenGoal.java:31-50`
```java
public void tick() {
    int level = cat.getTalentLevel(Talents.NIGHT_PROWLER);
    double radius = 6.0D + (level * 2.0D);
    AABB box = cat.getBoundingBox().inflate(radius);
    List<Creeper> creepers = cat.level().getEntitiesOfClass(Creeper.class, box);
    ...
    List<Phantom> phantoms = cat.level().getEntitiesOfClass(Phantom.class, box);
```
Goal runs continuously with no cooldown once `canUse()` is true. Executes two `getEntitiesOfClass` queries every single tick. At night (lines 61-64), allocates `new MobEffectInstance(MobEffects.NIGHT_VISION, ...)` every tick.

- **File 4c**: `com/kittytalents/block/CatBathBlockEntity.java:67-73`
```java
List<TalentedCat> cats = level.getEntitiesOfClass(
        TalentedCat.class,
        new AABB(pos).inflate(5.0D, 3.0D, 5.0D),
        c -> !c.isIncapacitated()
);
```
Runs on **every single tick** (20 Hz) for every placed Cat Bath block entity, constantly allocating `AABB` instances even while `cleanCooldown` and `fishCooldown` are active.

- **File 4d**: `com/kittytalents/entity/TalentedCat.java:1461-1464`
```java
double deltaMovementLength = Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().z * getDeltaMovement().z);
if (deltaMovementLength > 0.05D) {
    this.distanceTraveled += deltaMovementLength;
}
```
Computes `Math.sqrt` unconditionally on every tick for every cat, even when completely motionless.

- **File 4e**: `com/kittytalents/entity/TalentedCat.java:700-705`
```java
public int getTalentLevel(Talent talent) {
    if (talent != null && talent.isStandard()) {
        return Math.max(1, talentLevels.getOrDefault(talent.getId(), 5));
    }
    return talentLevels.getOrDefault(talent.getId(), 0);
}
```
Called 20+ times per tick per cat inside `tick()`, doing `Map<String, Integer>` lookups with string hashing and Integer boxing/unboxing (over 400 lookups/second per cat).

- **File 4f**: `com/kittytalents/event/ForgeEvents.java:231-239, 283`
```java
net.minecraft.world.phys.AABB scan = sp.getBoundingBox().inflate(8.0D);
List<net.minecraft.world.entity.LivingEntity> threats = sp.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, scan, ...);
...
sp.hurtMarked = true;
```
In `onPlayerTick`: for twined players, executes `getEntitiesOfClass(LivingEntity.class, ...)` on the server every tick, loops over all entities, and forces `sp.hurtMarked = true` on every tick, spamming movement synchronization packets.

---

### Observation 5: Massive AABB Searches & Missing Cooldowns on Items
- **File 5a**: `com/kittytalents/item/FelineLocatorOrbItem.java:59-63`
```java
List<TalentedCat> cats = level.getEntitiesOfClass(
        TalentedCat.class,
        new AABB(player.blockPosition()).inflate(1024.0D),
        c -> c.getUUID().equals(boundId)
);
```
Searches a $2048 \times 2048$ block volume ($128 \times 128 = \mathbf{16,384\text{ chunks}}$) using spatial iteration to find an entity by UUID, rather than using `((ServerLevel) level).getEntity(boundId)` ($O(1)$ map lookup). Has no item cooldown.

- **File 5b**: `com/kittytalents/item/KittyTrackerItem.java:33-37`
```java
List<TalentedCat> cats = level.getEntitiesOfClass(
        TalentedCat.class,
        new AABB(player.blockPosition()).inflate(512.0D),
        c -> c.isOwnedBy(player)
);
```
Inflates AABB by 512 blocks ($1024 \times 1024$ blocks = $\mathbf{4,096\text{ chunks}}$) on right-click with no cooldown.

- **File 5c**: `com/kittytalents/compat/tacz/TacZCompat.java:246-252`
```java
for (LivingEntity e : cat.level().getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(64.0D),
        entity -> entity != cat && entity != player && !cat.isAlliedTo(entity))) {
    if (e.getBoundingBox().inflate(0.6D).clip(eye, end).isPresent()) {
        hitTarget = e;
        break;
    }
}
```
Queries an unconstrained 64-block inflated box ($128 \times 128 \times 128$) across multiple chunks and performs Ray/AABB clipping without distance sorting.

- **File 5d**: `com/kittytalents/item/KittyWhistleItem.java:45-125`
Lacks a click cooldown (`player.getCooldowns().addCooldown(...)`), allowing rapid packet spamming of whistle commands.

---

### Observation 6: Dedicated Server Crash Vectors & ClassLoading Violations
- **File 6a**: `com/kittytalents/network/PacketSyncCatEquipment.java:3, 5, 77, 92`
```java
import com.kittytalents.client.gui.CatTalentScreen;
import net.minecraft.client.Minecraft;
...
if (Minecraft.getInstance().level != null) {
```
Directly imports and invokes `net.minecraft.client.Minecraft` and `CatTalentScreen` in common packet code without `DistExecutor` or client-only proxy. Will crash dedicated servers on classload with `NoClassDefFoundError: net/minecraft/client/Minecraft`.

- **File 6b**: `com/kittytalents/network/PacketSyncTwine.java:3`
```java
import com.kittytalents.client.ClientTwineHandler;
```
Direct top-level import of client class in common packet.

- **File 6c**: `com/kittytalents/network/PacketSyncTrustedPlayers.java:4, 49` & `PacketSyncTalents.java:4, 51`
Direct top-level imports of `net.minecraft.client.Minecraft`.

- **File 6d**: `com/kittytalents/entity/TwineManager.java:21`
```java
private static final Map<UUID, UUID> SESSIONS = new ConcurrentHashMap<>();
```
Static map holds session UUIDs and is never cleared on server stop or level unload, causing cross-world state leakage in singleplayer / integrated servers.

---

### Observation 7: Object Allocation Churn
- **File 7a**: `com/kittytalents/compat/apotheosis/ApotheosisCompat.java:52, 63, 65, 74, 81, 89, 99`
```java
Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", attributeName));
```
Called 6+ times per attack; allocates `new ResourceLocation("attributeslib", ...)` and queries the registry dynamically instead of caching `Attribute` references statically.

- **File 7b**: `com/kittytalents/client/renderer/TalentedCatArmorLayer.java:113-140`
```java
return new ResourceLocation("kittytalents", "textures/models/armor/cat_armor_....png");
```
Allocates new `ResourceLocation` objects on every frame for all 4 armor slots.

- **File 7c**: `com/kittytalents/block/CatRiceMillBlockEntity.java:68` & `KittyHabitatBlockEntity.java:241`
Allocates `ItemStack` copies every tick during processing.

---

## 2. Logic Chain

1. **Global Tick Inefficiencies**:
   - From Observation 1, `FelineStatusEffects.onLivingTick` executes on `LivingEvent.LivingTickEvent` on the Forge event bus.
   - Because `LivingTickEvent` runs on every living entity in loaded chunks, having 500 mobs in loaded chunks results in 10,000 NBT inspections and compound queries per second.
   - This creates an unnecessary global baseline TPS degradation regardless of whether any cat is nearby.

2. **AI Goal Block & Entity Scan Overload**:
   - From Observation 3a, 3b, and 3c, `CatEatFromBowlGoal` and `CatPlaceTorchGoal` inspect 3,000 to 6,800 blocks per check.
   - Because `CatPlaceTorchGoal` fails to reset `cooldown` when no legal spot is found, it runs this scan every 50ms, resulting in tens of thousands of block lookups per second.
   - From Observation 4a, 4b, and 4c, `Gale Purr`, `CatCreeperWardenGoal`, and `CatBathBlockEntity` run spatial entity searches (`getEntitiesOfClass`) every tick without throttling.
   - Combined with path recalculation in Observation 2 running on every tick (`moveTo` called unconditionally), cats with these goals cause severe tick lag spikes and pathfinder saturation.

3. **Multiplayer & Dedicated Server Crashes**:
   - From Observation 6a, 6b, 6c, packet classes (`PacketSyncCatEquipment`, `PacketSyncTwine`, `PacketSyncTrustedPlayers`, `PacketSyncTalents`) directly import `net.minecraft.client.Minecraft` and client GUI classes.
   - When a dedicated server runs, the JVM verifies class bytecode upon loading. Classes referencing `net/minecraft/client/Minecraft` or client GUIs fail verification, immediately crashing the server with `NoClassDefFoundError`.
   - This violates Requirement R2 and the Multiplayer Verification acceptance criterion.

4. **Algorithmic Suboptimality in Items**:
   - From Observation 5a, `FelineLocatorOrbItem` does an AABB search across 16,384 chunks to find a single cat by UUID, when `ServerLevel.getEntity(UUID)` can resolve it in $O(1)$ time.
   - Together with the absence of item use cooldowns in `KittyTrackerItem` and `KittyWhistleItem`, rapid player clicks can freeze the server thread.

---

## 3. Caveats

- **Profiling Numbers**: Exact millisecond timings vary depending on server CPU hardware, world seed, and number of spawned mobs/entities in loaded chunks.
- **TacZ Compatibility**: If Timeless and Classics Zero (`tacz`) is not installed, `TacZCompat` methods return early via `isLoaded()` checks, so firearm performance costs only trigger when TacZ is active.
- **Apotheosis Compatibility**: Similarly, `ApotheosisCompat` only executes its registry lookups when Apotheosis / AttributesLib is loaded.
- **No other caveats**: All observations are based on direct source inspection and validated against compilation.

---

## 4. Conclusion & Actionable Optimization Plan

To achieve the requirements for R2 (TPS, memory optimization, multiplayer/dedicated server stability), the following architectural changes must be implemented:

### Action 1: Replace Global `LivingTickEvent` with Native Effect or Tag Management
- Remove `LivingEvent.LivingTickEvent` from `FelineStatusEffects.java`.
- Instead of inspecting every entity in the world every tick, track afflicted entities in a dedicated, active `Set<UUID>` / `Map<UUID, StatusData>` on the server, or attach standard vanilla `MobEffect` instances.

### Action 2: Pathfinding Navigation Throttling
- Add `int ticksUntilNextPathRecalculation` cooldown (10–20 ticks) to:
  - `CatRestOnBedGoal`
  - `CatPounceAttackGoal`
  - `CatGunAttackGoal`
  - `CatFetchToyGoal`
- In `CatEatFromBowlGoal` and `CatPlaceTorchGoal`, when `navigation.isDone()`, apply a 20–40 tick backoff timer before attempting path recalculation.

### Action 3: Throttle and Cache Block/Entity Searches
- **`CatPlaceTorchGoal`**: Ensure `cooldown` is always set to at least 40–60 ticks if `findValidTorchPos` returns `null`. Cache candidate search positions and avoid nested 243-block checks.
- **`CatEatFromBowlGoal`**: Cache discovered bowl positions in `TalentedCat`, or index bowls via a lightweight coordinate set, rather than brute-force iterating 6,800 blocks every 1.5s.
- **`TalentedCat.tick()`**:
  - `Gale Purr`: Check `this.tickCount % 10 == 0` instead of every tick.
  - `CatCreeperWardenGoal`: Throttle `getEntitiesOfClass` to run every 10 ticks.
  - `CatBathBlockEntity`: Check `level.getGameTime() % 20 == 0` before querying cats.
  - `Distance traveled`: Use `getDeltaMovement().horizontalDistanceSqr() > 0.0025D` before invoking `Math.sqrt`.
  - `getTalentLevel`: Cache talent levels in a direct array or integer table to avoid 400+ `Map.getOrDefault` hash lookups per second.

### Action 4: Fix Dedicated Server ClassLoading Crashing
- Isolate all client-specific code in `PacketSyncCatEquipment`, `PacketSyncTwine`, `PacketSyncTrustedPlayers`, and `PacketSyncTalents` into a dedicated client handler class (e.g., `com.kittytalents.client.ClientPacketHandler`), invoked strictly via `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ...)`.
- Remove all `import net.minecraft.client.*` and `import com.kittytalents.client.*` from common packet classes.

### Action 5: Optimize Item Lookups & Add Cooldowns
- **`FelineLocatorOrbItem`**: Replace the 1,024-block AABB entity query with `((ServerLevel) level).getEntity(boundId)` ($O(1)$). Add a 20-tick item cooldown.
- **`KittyTrackerItem`**: Restrict search radius and add a 20-tick cooldown.
- **`KittyWhistleItem`**: Add a 10-tick item cooldown to prevent network spam.
- **`TacZCompat`**: Use ray-box intersection against a constrained frustum/ray corridor rather than inflating a 64-block cube.

### Action 6: Eliminate ResourceLocation & ItemStack Allocation Churn
- Statically instantiate `ResourceLocation` constants in `TalentedCatArmorLayer` and `TalentedCatRenderer`.
- Statically resolve `Attribute` objects in `ApotheosisCompat` once at mod load.
- In `KittyHabitatBlockEntity` and `CatRiceMillBlockEntity`, avoid calling `copy()` and allocating new item stacks every single tick.

---

## 5. Verification Method

1. **Compilation Verification**:
   - Run: `./gradlew.bat compileJava --stacktrace`
   - Must exit with code 0.
2. **Dedicated Server ClassLoading Verification**:
   - Run: `./gradlew.bat runServer` or inspect compiled classes with bytecode analysis to verify zero references to `net/minecraft/client` in common packets.
3. **Unit / Integration Tests**:
   - Run: `./gradlew.bat test`
4. **Code Inspection Checkpoints**:
   - Confirm `FelineStatusEffects.java` no longer subscribes to `LivingEvent.LivingTickEvent`.
   - Confirm `CatPlaceTorchGoal` and `CatEatFromBowlGoal` do not run unthrottled block scans.
   - Confirm `moveTo` in all AI goals is guarded by cooldown intervals.
   - Confirm `FelineLocatorOrbItem` uses `getEntity(UUID)` rather than 1024-block AABB inflation.
   - Confirm all packet classes in `com.kittytalents.network` contain zero client imports.

5. **Invalidation Conditions**:
   - Any modification that re-introduces client imports into network packets.
   - Any AI goal tick loop that calls `navigation.moveTo()` on every tick without a ticker guard.
   - Any unthrottled `getEntitiesOfClass` running at 20 Hz without cooldown checks.
