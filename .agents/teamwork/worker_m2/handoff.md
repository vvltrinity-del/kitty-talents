# Handoff Report — Milestone 2 (M2): Performance Sweep, TPS Throttling & Memory Optimization

- **Worker**: worker_m2
- **Project Root**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`
- **Working Directory**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\worker_m2`
- **Parent Conversation ID**: `a549636f-050f-4476-a58e-0c527d96daeb`
- **Build Status**: PASS (`.\gradlew.bat compileJava --no-daemon` exit code 0)

---

## 1. Observation

Direct code inspections and survey findings identified critical tick-loop bottlenecks, unthrottled spatial queries, memory allocations, and particle sidedness defects across multiple systems:

1. **Global Entity Tick Hook in `FelineStatusEffects.java:27-36, 101`**:
   `onLivingTick` subscribed to `LivingEvent.LivingTickEvent` on the Forge event bus, executing for every living mob/animal in loaded chunks every tick (20 Hz). It invoked `entity.getPersistentData()` and `data.contains("FelineStatuses")` unconditionally. On worlds with 300–500 loaded entities, this caused 6,000–10,000 NBT lookups/allocations every second.

2. **Unthrottled 20Hz Loop & $O(r^3)$ Block Iteration in `CatPlaceTorchGoal.java:48-56, 152-177, 191-199`**:
   - In `canUse()`, when `findValidTorchPos` returned `null` or when `hasTorches()` was false, `cooldown` was not reset and remained `0`, causing `findValidTorchPos` to execute on every single tick (20 Hz).
   - In `findValidTorchPos`, `radius` scaled up to `3 + torchLvl * 2` (13 blocks at level 5), inspecting $27 \times 27 \times 5 = 3,645$ blocks.
   - For every candidate block, `hasTorchNear` iterated an unthrottled $9 \times 3 \times 9 = 243$ block cube, resulting in up to $3,645 \times 243 = 885,735$ block state lookups per check.
   - In `tick()`, `cat.getNavigation().moveTo(...)` was triggered every tick whenever `distSq > 4.0D` and `isDone()` was true.

3. **Continuous 20Hz Entity Queries in `CatCreeperWardenGoal.java:31-50`**:
   - `tick()` ran every tick and executed two unthrottled `getEntitiesOfClass` queries (`Creeper.class` and `Phantom.class`) over a 16-block radius.
   - It invoked `player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ...))` every single tick at night, allocating new objects 20 times per second.
   - Vector normalization on flee direction lacked zero-length division guards (`subtract().normalize()`).

4. **Continuous Entity Queries in `CatBathBlockEntity.java:68-73`**:
   - `tick()` executed `level.getEntitiesOfClass(TalentedCat.class, new AABB(pos).inflate(5.0D, 3.0D, 5.0D), ...)` on every tick (20 Hz) regardless of whether `cleanCooldown` or `fishCooldown` were active.

5. **Spam Lag in `KittyTrackerItem.java:27-63`**:
   - `use()` inflated an AABB by 512 blocks (scanning a $1024 \times 1024$ block volume across thousands of chunks) with 0 item cooldown, allowing rapid right-click spamming.

6. **Massive AABB Spatial Scan in `FelineLocatorOrbItem.java:59-63`**:
   - `use()` executed `level.getEntitiesOfClass(TalentedCat.class, new AABB(player.blockPosition()).inflate(1024.0D), c -> c.getUUID().equals(boundId))` to find an entity by UUID across a 2,048-block cube with 0 cooldown, rather than querying the level's entity map directly.

7. **Client Particle Invocations in Server AI Goals**:
   - `CatUseScratcherGoal.java:63-65, 72-74` and `CatRestOnBedGoal.java:68-70` checked `if (cat.level().isClientSide) cat.level().addParticle(...)`. Because AI goals run exclusively on the logical server, `isClientSide` is always `false`, completely preventing scratcher crit/villager particles and bed heart healing particles from spawning.

---

## 2. Logic Chain

1. **`FelineStatusEffects`**:
   - By creating a thread-safe `TRACKED_ENTITIES` set (`ConcurrentHashMap.newKeySet()`), entities afflicted with feline statuses are registered when `applyElementStatus` is called or when joining the level with saved status data.
   - In `onLivingTick`, checking `if (TRACKED_ENTITIES.isEmpty() || !TRACKED_ENTITIES.contains(entity.getUUID())) return;` performs an immediate $O(1)$ memory lookup, eliminating 100% of persistent NBT calls for unaffected entities.
   - When an entity's status effects expire or the entity dies, it is removed from `TRACKED_ENTITIES`, preventing memory leaks.

2. **`CatPlaceTorchGoal`**:
   - Setting `this.cooldown = 40;` when `targetTorchPos == null`, `!hasTorches()`, or brightness > 6 eliminates the fast 20 Hz failure loop and guarantees a minimum 2-second backoff between scans.
   - Capping search radius to `Math.min(6, 2 + torchLvl)` reduces candidate volume from 3,645 blocks to at most 845 blocks.
   - Caching discovered torches in `cachedNearbyTorch` enables an $O(1)$ Euclidean distance check for subsequent candidate blocks within the cached torch's radius, eliminating the nested $O(r^3)$ 243-block scan.
   - Guarding `cat.getNavigation().moveTo` with `cat.tickCount % 10 == 0` prevents pathfinder thrashing.

3. **`CatCreeperWardenGoal`**:
   - Throttling `tick()` scans to `if (cat.tickCount % 10 != 0) return;` reduces entity queries by 90% (from 20 Hz to 2 Hz) while keeping `hissTimer` ticking down every game tick.
   - Guarding flee direction vectors against zero length (`diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(1, 0, 0)`) prevents NaN movement velocity glitches.
   - Throttling night vision effect re-application eliminates object allocation churn.

4. **`CatBathBlockEntity`**:
   - Guarding `getEntitiesOfClass` behind `if ((bath.cleanCooldown <= 0 || bath.fishCooldown <= 0) && level.getGameTime() % 10 == 0)` ensures that during the 80 ticks of cleaning cooldown and 300 ticks of fishing cooldown, zero entity queries or AABB allocations occur.

5. **`KittyTrackerItem` & `FelineLocatorOrbItem`**:
   - Adding `player.getCooldowns().addCooldown(this, 30)` prevents spamming of radar scans.
   - In `FelineLocatorOrbItem`, replacing `level.getEntitiesOfClass` with `((ServerLevel) level).getEntity(boundId)` changes an $O(\text{volume})$ chunk entity scan into an $O(1)$ direct hash map lookup.

6. **Particle Sidedness**:
   - AI goals execute on the server. Replacing `isClientSide` / `addParticle` with `((ServerLevel) cat.level()).sendParticles(...)` ensures server-side particle packets are broadcast to all tracking clients.

---

## 3. Caveats

- **Active Afflicted Entities**: If thousands of mobs are simultaneously afflicted with feline elemental statuses, `TRACKED_ENTITIES` will track each mob's UUID until the effect expires or the mob dies; under normal gameplay, only entities engaged in combat with cats are tracked.
- **Server Distance Resolution**: `ServerLevel.getEntity(UUID)` resolves entities currently loaded in memory. If the bound cat is in an unloaded chunk, the locator orb cleanly reports that the cat is currently offline or in an unloaded chunk/dimension, without forcing synchronous chunk generation or freezing the server thread.
- **No other caveats**: All changes strictly respect exclusive write ownership and project conventions.

---

## 4. Conclusion

Milestone 2 (M2) is fully implemented:
- Global 20Hz NBT allocation across all world entities is completely eliminated.
- `CatPlaceTorchGoal` 20Hz loop is fixed with a 40-tick cooldown on null target, radius capped at 6, and candidate torch caching eliminating the nested 885k block scan.
- `CatCreeperWardenGoal` entity queries throttled from 20 Hz to 2 Hz with NaN vector safety.
- `CatBathBlockEntity` queries guarded by cooldowns and throttled to 2 Hz.
- `KittyTrackerItem` and `FelineLocatorOrbItem` received 30-tick cooldowns.
- `FelineLocatorOrbItem` 2,048-block scan replaced with direct $O(1)$ `ServerLevel.getEntity(boundId)`.
- Particle sidedness resolved in `CatUseScratcherGoal` and `CatRestOnBedGoal` via `ServerLevel.sendParticles`.
- Navigation pathfinder thrashing in `CatPlaceTorchGoal` and `CatRestOnBedGoal` mitigated with 10-tick interval guards.

---

## 5. Verification Method

### Automated Compilation Verification
Run from project root:
```powershell
.\gradlew.bat compileJava --no-daemon
```
**Result**: `BUILD SUCCESSFUL in 9s` with 0 errors (Exit code 0).

### Code Inspection Points
- `src/main/java/com/kittytalents/combat/FelineStatusEffects.java`:
  - `TRACKED_ENTITIES` set filters out all un-afflicted entities before NBT is accessed.
  - `onEntityJoinLevel` and `onLivingDeath` maintain set lifecycle.
- `src/main/java/com/kittytalents/entity/ai/CatPlaceTorchGoal.java`:
  - Line 49, 53: `this.cooldown = 40;` when light > 6 or target is null.
  - `radius = Math.min(6, 2 + torchLvl);`
  - `cachedNearbyTorch` caches found torches.
- `src/main/java/com/kittytalents/entity/ai/CatCreeperWardenGoal.java`:
  - Line 37: `if (cat.tickCount % 10 != 0) return;`
- `src/main/java/com/kittytalents/block/CatBathBlockEntity.java`:
  - Line 67: `if ((bath.cleanCooldown <= 0 || bath.fishCooldown <= 0) && level.getGameTime() % 10 == 0)`
- `src/main/java/com/kittytalents/item/KittyTrackerItem.java`:
  - Line 29: `player.getCooldowns().addCooldown(this, 30);`
- `src/main/java/com/kittytalents/item/FelineLocatorOrbItem.java`:
  - Line 49: `player.getCooldowns().addCooldown(this, 30);`
  - Line 63: `Entity entity = serverLevel.getEntity(boundId);`
- `src/main/java/com/kittytalents/entity/ai/CatUseScratcherGoal.java` & `CatRestOnBedGoal.java`:
  - Use `((ServerLevel) cat.level()).sendParticles(...)`.
