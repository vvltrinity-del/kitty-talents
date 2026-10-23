## 2026-10-07T18:17:56Z
You are Agent 4 (Networking, Performance & Server Specialist) for Kitty Talents 3.0 (Batch 2).

Working directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_4_network_perf
Project directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Authoritative request: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md (Read this first, especially the requirements under `## 2026-10-07T18:13:51Z` and R25, R26, R27, R28, R29, R30).
Project specification: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\PROJECT.md

Your Assigned Scope (Batch 2: Infrastructure, Network, Sidedness, Performance, Trust & Persistence):
1. R26 & R27: Dedicated Server & LAN Multiplayer Architecture & Security
   - Perform a whole-codebase client/server separation audit.
   - Guard 100% of client-only classes, rendering code, and screen openings with DistExecutor / `@OnlyIn(Dist.CLIENT)` or client proxies (`ClientPacketHandlers`) so dedicated servers never crash with `ClassNotFoundException` or `NoClassDefFoundError`.
   - Audit every packet in `com.kittytalents.network.*`: Decode -> Validate -> Authenticate -> Authorize -> Apply -> Synchronize. Server must be authoritative for HP, Energy, talent points, inventory, equipment, production, and Twine state.
   - Implement `PlayerEvent.StartTracking` in `ForgeEvents.java` so newly tracking/rendering players receive complete cat equipment, talents, and trust sync data.
2. R29 & R30: Performance, Lag Sweep & Stress Hardening
   - Throttle per-tick scans, livingTick operations (`FelineStatusEffects.onLivingTick`), entity goals, and pathfinding calls.
   - Throttle `CatPlaceTorchGoal`, `CatCreeperWardenGoal`, `CatRestOnBedGoal`, `CatUseScratcherGoal`, `CatBathBlockEntity`. Sleeping/sitting cats must consume near-zero tick time.
   - Optimize `KittyTrackerItem` and `FelineLocatorOrbItem` (replace excessive 2,048-block scan with direct entity lookup).
   - Eliminate memory leaks, repeated NBT allocations, and server particle sidedness issues (`sendParticles` on ServerLevel).
3. R25 & R28: Persistence, NBT Migration & Trust Security
   - Enforce strict multiplayer trust permissions in `TalentedCat`: Owner vs. Trusted Friend vs. Stranger. Strangers cannot untame, steal equipment, command, or Twine another player's cat.
   - Audit all NBT serialization/deserialization (`addAdditionalSaveData` and `readAdditionalSaveData` in `TalentedCat` and related entities) to guarantee all stats, roles, abilities, equipment, energy, carried toys, and production tier/data persist safely across dimension travel, world reloads, and server restarts.

Exclusive Write Ownership:
- `src/main/java/com/kittytalents/network/**`
- `src/main/java/com/kittytalents/client/network/**`
- `src/main/java/com/kittytalents/event/ForgeEvents.java`
- `src/main/java/com/kittytalents/entity/TalentedCat.java` (NBT persistence, trust logic, tick optimizations)
- `src/main/java/com/kittytalents/entity/ai/**` (throttling goals)
- `src/main/java/com/kittytalents/item/KittyTrackerItem.java`, `FelineLocatorOrbItem.java`
Do NOT edit GUI screens, textures, sounds, or `ClientTwineHandler.java` (owned by Agent 7).

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Verification:
- Run `./gradlew compileJava` or `./gradlew.bat compileJava` to verify zero compile errors.
- Maintain `progress.md` with `Last visited: [timestamp]` during your work.
- When complete, write a comprehensive `handoff.md` in your working directory summarizing: changes made, files modified, verification commands and results, and send a message back to the orchestrator.


## 2026-10-07T18:28:12Z
**Context**: Batch 2 implementation (Networking, Performance, Trust & Server)
**Content**: High-priority user directive received: Reference open-source Java resources for mob control and better pet/companion AI (e.g. DoggyTalents/DoggyTalentsNext companion behaviors, standard Forge/Minecraft vanilla companion control algorithms) and ensure robust server-side companion state handling, trust boundaries, and efficient tick processing.
**Action**: Ensure high code quality, clean companion behavior feel, and robust multiplayer/server security aligning with these open-source standards.


## 2026-10-07T18:28:29Z
**Context**: Batch 2 implementation
**Content**: Additional directive: Also reference GitHub open-source repositories (e.g., publicly available Forge companion mods like DoggyTalentsNext, Domestication Innovation, custom HUD/GUI repositories, and particle/sound effect implementations on GitHub) for established architectural patterns, robust companion control logic, and authentic audio-visual design.
**Action**: Apply these established architectural patterns to your implementation.
