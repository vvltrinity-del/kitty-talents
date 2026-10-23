# Agent 3 (AI & Companion Behavior Specialist) Dispatch

Authoritative Request Log: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md
Project Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Working Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_3_ai

## Assigned Requirements:
- R11: AI Logic Overhaul (All AI modes operational: Docile, Follow, Stay, Wander, Hunting, Guard, Aggressive, Tactical, Defensive, Escort, Fetch, Mine, Farm, Fish, Support, Protect Owner, Return Home, Retreat)
- R12: AI Targeting and Priorities (Centralized target-selection logic, eliminate contradictory goal behavior)
- R13: Feline AI Behavior (Stalking, pouncing, hunting, resting on bed, scratching, food seeking, toy interaction)
- R14: Ranged / TaCZ AI (Firearm detection, jaw mounting, ammo consumption from catPack, aiming, reloading, graceful fallback when TaCZ absent)

## Mandatory Constraints:
- Read ORIGINAL_REQUEST.md before starting work.
- Do NOT rebuild the mod from scratch. Inspect existing code first (e.g. `entity/ai/*`, `entity/CatMode.java`, `entity/TalentedCat.java`, `compat/TacZCompat.java`), preserve everything that works, and improve/refactor only where necessary.
- Fix known bugs: bed resting sit-lock, Docile loop, downed cat pathing, scratcher timeout, cliff leaping suicidal check, Whistle commands.
- DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
- Test and verify compilation using `./gradlew compileJava` in your working report. Write a detailed `handoff.md` upon completion.


## 2026-10-07T03:11:29Z
You are Agent 3: AI & Companion Behavior Specialist for the Kitty Talents 3.0 overhaul.
Working directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_3_ai
Project directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Authoritative request log: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md
Your dispatch instructions: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_3_ai\DISPATCH.md

You MUST read C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md before starting work.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

CRITICAL CONSTRAINTS:
- Do NOT rebuild the mod from scratch. Do NOT replace working systems unnecessarily. Inspect the current implementation first, preserve everything that already works, and improve/refactor only where necessary.
- Integrity mode: Development.

ASSIGNED REQUIREMENTS:
- R11: AI Logic Overhaul (All AI modes operational: Docile, Follow, Stay, Wander, Hunting, Guard, Aggressive, Tactical, Defensive, Escort, Fetch, Mine, Farm, Fish, Support, Protect Owner, Return Home, Retreat)
- R12: AI Targeting and Priorities (Centralized target-selection logic, eliminate contradictory goal behavior)
- R13: Feline AI Behavior (Stalking, pouncing, hunting, resting on bed, scratching, food seeking, toy interaction)
- R14: Ranged / TaCZ AI (Firearm detection, jaw mounting, ammo consumption from catPack, aiming, reloading, graceful fallback when TaCZ absent)

Your exclusive write files:
- src/main/java/com/kittytalents/entity/ai/*
- src/main/java/com/kittytalents/entity/CatMode.java
- src/main/java/com/kittytalents/compat/TacZCompat.java

Inspect existing code first. Fix known bugs: bed resting sit-lock, Docile loop, downed cat pathing, scratcher timeout, cliff leaping suicidal check, Whistle commands.
Run `./gradlew compileJava` in the project directory to verify that the mod builds without any compilation errors.
When complete, write `handoff.md` and `progress.md` in your working directory and send a message back with your findings and build result.


## 2026-10-07T03:23:09Z
**Context**: "Neko Night" Gameplay Event System
**Content**: NEW USER REQUEST:
"add a special night event called neko night where enemies are stronger and theres a chance nekomata cats and tierd cats spawn"

Please implement the world event logic and spawn hooks for "Neko Night":
1. Create a clean event manager (e.g. `com.kittytalents.event.NekoNightManager` or in `world/`):
   - Periodic night check (e.g., at sunset/night transition, random chance like 10% per full moon/night, plus command toggle `/kittytalents nekonight start/stop`).
   - Network sync or chat announcement ("A mystical Neko Night has begun... The moon glows with feline spirit!").
   - Static query `NekoNightManager.isNekoNight(Level level)`.
2. Wild Cat Spawn Hook:
   - When cats spawn during Neko Night, grant a 25% chance to spawn as Nekomata (`setNekomata(true)`) and a 35% chance to spawn with elevated Production Tiers (e.g., Iron, Gold, Diamond).
**Action**: Implement this event management and spawn hook, verify compilation with `./gradlew compileJava`, and report in your handoff.

## Follow-up — 2026-10-07T03:22:48Z & 03:23:17Z
USER REQUESTS:
1. "add a special night event called neko night where enemies are stronger and theres a chance nekomata cats and tierd cats spawn"
   - Implement `NekoNightManager` with random night trigger / command toggle.
   - Boost wild cat Nekomata and elevated Production Tier spawn chance during Neko Night.
2. "add talented cats as a natural spawn in jungle and villages"
   - Add natural wild spawns in Jungles and Villages via biome modifiers / spawn placements.


## 2026-10-07T03:23:59Z
**Context**: Natural Wild Cat Spawns (Jungles and Villages)
**Content**: USER REQUEST:
"add talented cats as a natural spawn in jungle and villages"

Please ensure Talented Cats spawn naturally in the wild:
1. Register spawn placement rules (`SpawnPlacements.register(ModEntities.TALENTED_CAT.get(), ...)` in CommonSetup or event hook).
2. Add Forge Biome Modifiers or Biome Loading event to add Talented Cat spawns to:
   - Jungle biomes (`#minecraft:is_jungle`)
   - Village areas / plains/forest around villages
3. Ensure natural wild cats spawn un-tamed, with random feline variants/skins, ready to be befriended/tamed by players with Catnip or fish.
**Action**: Implement natural wild spawn hooks and biome modifiers, verify compilation with `./gradlew compileJava`, and report in your handoff.

## Follow-up — 2026-10-07T03:27:31Z
USER REQUEST:
"have them switch from gun to sword from their inventroy if out of ammo or just remove gun"

Implement empty-ammo fallback:
- Check cat inventory/Kitty Pack for a melee weapon (sword/axe) and swap into mainhand.
- If none found, unequip/stow empty gun to fight with claws/bite.


## 2026-10-07T03:27:44Z
**Context**: Empty-Ammo Firearm Fallback Logic (R14)
**Content**: USER REQUEST:
"have them switch from gun to sword from their inventroy if out of ammo or just remove gun"

In your firearm combat AI (`CatGunAttackGoal.java` / `TacZCompat.java` / `TalentedCat.java`):
When a cat is out of ammunition for its equipped firearm:
1. Search its inventory / Kitty Pack for any available melee weapon (e.g. SwordItem / AxeItem). If found, automatically swap/equip it into its main hand and transition cleanly to melee combat goal (`CatMeleeAttackGoal` / `CatPounceAttackGoal`).
2. If no melee weapon exists in inventory, unequip the empty firearm into its inventory (or stow it) so the cat fights with natural claws/bite rather than holding an empty gun in close-quarters melee.
3. If ammunition is replenished later, allow re-equipping the firearm when at range.
**Action**: Implement this empty-ammo weapon swap/stow logic, verify compilation with `./gradlew compileJava`, and report in your handoff.
