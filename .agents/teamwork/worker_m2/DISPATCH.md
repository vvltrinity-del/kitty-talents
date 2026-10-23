# Dispatch Assignment — worker_m2

## 2026-10-06T21:10:00Z
- **Identity**: You are worker_m2 (teamwork_preview_worker).
- **Working directory**: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\worker_m2
- **Project root**: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
- **Parent**: orchestrator_1 (conversation ID: a549636f-050f-4476-a58e-0c527d96daeb)

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Objective
Implement Milestone 2 (M2): Performance Sweep, TPS Throttling & Memory Optimization.
Conduct a codebase sweep to optimize TPS and memory usage, eliminating all tick-loop lag spikes.

## Inputs & Authoritative Specs
1. Read `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md`.
2. Read `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\PROJECT.md`.
3. Read survey handoffs:
   - `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\survey_explorer_3\handoff.md`
   - `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\survey_explorer_2\handoff.md`

## Exclusive Write Ownership
You exclusively own and may modify:
- `src/main/java/com/kittytalents/combat/FelineStatusEffects.java`
- `src/main/java/com/kittytalents/entity/ai/CatPlaceTorchGoal.java`
- `src/main/java/com/kittytalents/entity/ai/CatCreeperWardenGoal.java`
- `src/main/java/com/kittytalents/block/CatBathBlockEntity.java`
- `src/main/java/com/kittytalents/item/KittyTrackerItem.java`
- `src/main/java/com/kittytalents/item/FelineLocatorOrbItem.java`
- `src/main/java/com/kittytalents/entity/ai/CatUseScratcherGoal.java`

Do NOT modify files owned by other milestones.

## Tasks
1. In `FelineStatusEffects.java`: Optimize `onLivingTick` event hook. Eliminate the global 20Hz NBT allocation across all world entities by checking entity type or maintaining a tracked entities set.
2. In `CatPlaceTorchGoal.java`:
   - Fix the infinite 20Hz loop: set `this.cooldown = 40;` when `targetTorchPos == null`.
   - Reduce search radius and eliminate the nested $O(r^3)$ 885,000-block scan by caching candidate checks.
3. In `CatCreeperWardenGoal.java`: Throttle entity scans to run only every 10 ticks (`if (cat.tickCount % 10 == 0)`) instead of every tick.
4. In `CatBathBlockEntity.java`: Only execute entity search when `cleanCooldown <= 0 || fishCooldown <= 0`.
5. In `KittyTrackerItem.java`: Add a 30-tick cooldown upon use to prevent spam lag.
6. In `FelineLocatorOrbItem.java`: Replace the 2,048-block `getEntitiesOfClass` scan with direct `((ServerLevel)level).getEntity(boundId)`. Add a 30-tick cooldown.
7. In `CatUseScratcherGoal.java` and `CatRestOnBedGoal.java`: Fix particle sidedness. Replace client particle invocations (`isClientSide`) with server-side particle broadcasting via `((ServerLevel)cat.level()).sendParticles(...)`.
8. Run `.\gradlew.bat compileJava --no-daemon` and confirm clean compilation with 0 errors.

## Output Requirements
Produce a comprehensive handoff report in:
`C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\worker_m2\handoff.md`
including verification commands, compile results, and line-by-line summary.
Notify parent via `send_message` when complete.


## 2026-10-06T21:11:18Z
Objective: Implement Milestone 2 (M2): Performance Sweep, TPS Throttling & Memory Optimization.
Optimize FelineStatusEffects.onLivingTick to eliminate 20Hz global NBT allocation across world entities.
Throttle CatPlaceTorchGoal (set 40-tick cooldown on null target, reduce radius, eliminate 885k block scan).
Throttle CatCreeperWardenGoal (entity scan every 10 ticks), CatBathBlockEntity, KittyTrackerItem, and FelineLocatorOrbItem (replace 2,048-block scan with direct ServerLevel.getEntity).
Fix particle sidedness in AI goals via ServerLevel.sendParticles.
Compile with .\gradlew.bat compileJava --no-daemon to verify 0 errors.
Write your handoff report to:
C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\worker_m2\handoff.md
Send a completion message back to parent (conversation ID: a549636f-050f-4476-a58e-0c527d96daeb).
