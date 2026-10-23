# DISPATCH — worker_m4

- **Agent**: worker_m4
- **Role**: teamwork_preview_worker (AI Logic & DT Parity Worker)
- **Assigned Milestone**: Milestone 4 (M4): AI Logic Overhaul & Doggy Talents Parity
- **Working Directory**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\worker_m4`
- **Project Root**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`
- **Parent Conversation ID**: `a549636f-050f-4476-a58e-0c527d96daeb`

---

## 🔒 Mandatory References
Read these files before writing code:
1. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md`
2. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\PROJECT.md`
3. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\survey_explorer_4\handoff.md`
4. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\survey_explorer_1\handoff.md`

---

## ⚠️ MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

---

## Exclusive Write Scope
You have exclusive write access to:
- `src/main/java/com/kittytalents/entity/TalentedCat.java`
- `src/main/java/com/kittytalents/entity/CatMode.java`
- `src/main/java/com/kittytalents/entity/ai/` (All files in this directory)
- `src/main/java/com/kittytalents/ability/` (`CatAbilities.java`, `CatAbility.java`, etc.)
- `src/main/java/com/kittytalents/talent/` (`Talents.java` and talent classes)

DO NOT modify files owned by M1, M2, M3, M5, M6, or M7.

---

## Assigned Tasks (Features 24–40)

### Task 1: Fix 16 Critical AI Bugs & Broken States (Features 24–30, 33–34, 40)
1. **Bed Resting Sit-Lock**: In `CatRestOnBedGoal.java`, when `stop()` is called, invoke `cat.setOrderedToSit(false)` so cats do not remain permanently sit-locked after resting.
2. **Bed Goal DOCILE Infinite Loop**: In `CatRestOnBedGoal.java`, fix `canContinueToUse()` so cats in DOCILE mode do not stay in bed forever during daylight; require darkness or nighttime.
3. **Downed Cat Bed Pathing**: Ensure incapacitated/downed cats do not pathfind to bed.
4. **Nine Lives Godmode**: In `TalentedCat.java`, replace infinite resurrection with finite daily lives (e.g. max 9 charges, regaining 1 charge per Minecraft day/dawn, or 10-minute cooldown). Trigger soul particle burst and temporary invulnerability on revive.
5. **Fisher & Mole Crash on Level >= 10**: In `CatFisherGoal.java` and `CatMoleMiningGoal.java`, replace `random.nextInt(bound)` with `random.nextInt(Math.max(1, bound))` to prevent `IllegalArgumentException: bound must be positive` when talent levels exceed standard caps.
6. **Scratcher Timeout**: In `CatUseScratcherGoal.java`, add an 80-tick timeout so cats do not freeze if pathfinding to a scratcher fails.
7. **NBT Persistence**: In `TalentedCat.java:addAdditionalSaveData` and `readAdditionalSaveData`, persist `energy`, active ability slots (`abilitySlot1..3`), and `carriedToy`.
8. **Fox Pounce Preconditions**: In `CatFoxPounceGoal.java`, check `cat.getTalentLevel(Talents.POUNCE) > 0` before leaping, and add a 60-tick airborne safety timeout.
9. **Cliff Leaping Safety**: In `TalentedCat.java:customServerAiStep`, add a safe gap/fall check before high-speed leaps to prevent suicidal cliff jumps.
10. **Vector NaN Guards**: Guard all vector calculations (`subtract().normalize()`, distance divisions) against zero-length vectors.

### Task 2: Energy Regeneration & Active Ability Engine (Features 31, 32)
1. In `TalentedCat.java:tick()`: Implement continuous Energy regeneration (+1 Energy every 20 ticks, up to maxEnergy 100, modified by talents/stats).
2. In `CatAbilities.java`: Register all 6 active abilities: `POUNCE`, `FELINE_HISS`, `HEALING_PURR`, `SHADOW_STRIKE`, `UTILITY_INTERACT`, `VOLT_DASH`.
3. In `UseAbilityGoal.java`: Register in `goalSelector` at Priority 4 so cats autonomously trigger equipped abilities during combat/utility when conditions and energy thresholds are met.

### Task 3: Stub Talents Implementation (Feature 36)
Implement genuine gameplay logic for:
1. `CRITICAL_CLAWS`: Chance for critical strikes dealing 1.5x damage with claw crit particles.
2. `FELINE_REFLEXES`: Increased dodge chance and reduced damage taken from falling and projectiles.
3. `HOME_WHISKERS`: Grants the cat a homing sense back to its bed/owner and bonus navigation speed when returning home.
4. `CUSHION_PAWS`: Complete fall damage negation up to 20 blocks and completely silent footsteps (mobs cannot detect via vibration).

### Task 4: Goal Priority Realignment & Doggy Talents Parity Modes (Features 37, 38)
1. **Goal Priorities**: Realign `goalSelector` so:
   - Priority 1: SwimGoal / FloatGoal
   - Priority 2: SitWhenOrderedToGoal
   - Priority 3: Downed / Incapacitated Goal
   - Priority 4: UseAbilityGoal
   - Priority 5: MeleeAttackGoal / Combat Goals
   - Priority 6: FollowOwnerGoal
   - Priority 7+: Utility / Idle Goals (Bed, Scratcher, Fisher, Torch, etc.)
2. **Companion Modes (`CatMode.java`)**:
   - `DOCILE`: Follows owner peacefully, does not attack unless owner is attacked.
   - `WANDERING`: Explores local area near home or current location.
   - `AGGRESSIVE`: Attacks any hostile monsters within 16 blocks.
   - `BERSERKER`: Attacking frenzy — heightened speed and attack frequency against all hostile targets.
   - `PATROL`: Patrols between recorded waypoints or around anchor location.
   - `GUARD`: Anchored to a guard post/block; defends within an 8-block perimeter and returns to post.
   - `TACTICAL`: Stays at a medium tether distance (6–10 blocks), providing flanking and support abilities.
   - `STAY`: Stays at designated location standing alert (unlike sitting).

### Task 5: Whistle Commands & Backpack Auto-Feeding (Features 35, 39)
1. **Whistle Commands**:
   - `QUIET`: Mutes meows/purrs for stealth instead of giving an invisibility potion effect.
   - `ATTACK_TARGET`: Directs all nearby cats to focus attack on the entity the player is pointing at.
   - `FETCH`: Directs cat to fetch dropped items or toys.
   - `STAND`: Commands sitting cats to stand immediately.
2. **Backpack Auto-Feeding**:
   - When cat hunger drops below 60% or health is below 100%, scan `catPack` inventory for valid cat food/fish/treats and consume automatically to heal and satisfy hunger. Also auto-feed from `KittyFeedingStationBlock` when within 4 blocks.

---

## Verification Requirements
1. Run `.\gradlew.bat compileJava --no-daemon` to ensure 0 errors.
2. Check that all 16 AI bugs are eliminated with zero regression.
3. Document all implementation details, code diffs, and verification results in `handoff.md`.
4. Send a completion message to parent (`a549636f-050f-4476-a58e-0c527d96daeb`).
