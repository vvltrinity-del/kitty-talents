# Agent 2 (Combat & Abilities Specialist) Dispatch

Authoritative Request Log: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md
Project Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Working Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_2_combat

## Assigned Requirements:
- R2: Pokémon/Aniimo-Style Creature RPG Combat (Real-time creature roles, affinities, active abilities, builds)
- R3: Cat Combat Roles (DPS, Tank, Support, Scout, Assassin, etc. emerging from talents and equipment)
- R4: Active Ability System (Basic attack, 3 skills, 1 ultimate, passive/innate)
- R5: Feline Energy (Energy pool, regen engine, usage for dodge/pounce/skills, parity across AI and Twine)
- R6: Real-Time Combat Feel (Combos, 4-way dodge, perfect dodge with slowdown/sound/refund, Pounce 2.0 physics arc)
- R7: Elements & Status Effects (Affinities, status effects: Burn, Poison, Bleed, Freeze, Haste, Fortified, etc.)
- R8: Talents -> Real Gameplay (Audit registered talents, ensure real gameplay logic for critical claws, agility, cushion paws, berserker, etc.)

## Mandatory Constraints:
- Read ORIGINAL_REQUEST.md before starting work.
- Do NOT rebuild the mod from scratch. Inspect existing code first (e.g. `combat/*`, `ability/*`, `talent/*`), preserve everything that works, and improve/refactor only where necessary.
- DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
- Test and verify compilation using `./gradlew compileJava` in your working report. Write a detailed `handoff.md` upon completion.


## 2026-10-07T03:11:29Z
Received dispatch from parent (79e8da1a-3c50-454a-a794-b13066ada7ed):
Role: Agent 2: Combat & Abilities Specialist for Kitty Talents 3.0 overhaul.
Requirements: R2, R3, R4, R5, R6, R7, R8.
Exclusive write files:
- src/main/java/com/kittytalents/combat/*
- src/main/java/com/kittytalents/ability/*
- src/main/java/com/kittytalents/talent/*


## 2026-10-07T03:23:31Z
Received update from parent (79e8da1a-3c50-454a-a794-b13066ada7ed):
Neko Night Combat Scaling:
- When Neko Night is active, hostile mobs encountering or fighting cats receive buffed combat stats (+30% attack damage, +25% resistance buff, eerie spirit particles).
- Cats fighting during Neko Night earn increased combat XP / Energy flow (+50% energy generation and bonus combat rewards).
- Integrate this combat scaling into combat/* event handlers using soft reflection or clean checking of NekoNightManager if present.

## Follow-up — 2026-10-07T03:22:48Z
USER REQUEST:
"add a special night event called neko night where enemies are stronger and theres a chance nekomata cats and tierd cats spawn"

Integrate combat difficulty scaling during Neko Night:
- Query `NekoNightManager.isNekoNight(level)` during combat calculations.
- Hostile mobs receive buffed combat stats (+damage, +health/resistance) and eerie visual aura.
- Cats fighting during Neko Night earn bonus combat XP / Energy flow.
