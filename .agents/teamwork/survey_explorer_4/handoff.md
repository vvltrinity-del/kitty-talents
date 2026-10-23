# Survey Report: R3 AI Logic Overhaul & Doggy Talents Parity

**Surveyor**: survey_explorer_4  
**Date**: 2026-10-06  
**Project**: Kitty Talents 3.0 Overhaul  
**Target Path**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`

---

## 1. Observation

### 1.1 Complete Inventory of Existing AI Goals & Tasks

#### GoalSelector Registrations (`TalentedCat.java:233-320`)
1. **Priority 0**: `FloatGoal(this)` (`TalentedCat.java:234`)
   - Allows cat to float in water.
2. **Priority 1**: Anonymous `SitWhenOrderedToGoal(this)` (`TalentedCat.java:235-244`)
   - Overrides `canUse()` and `canContinueToUse()` to check `!isTwined()`.
3. **Priority 2**: `BreedGoal(this, 1.0D)` (`TalentedCat.java:245`)
   - Vanilla breeding goal. Does not check `isIncapacitated()` or `isTwined()`.
4. **Priority 2**: `CatFoxPounceGoal(this)` (`TalentedCat.java:246`)
   - Multi-phase leap attack (crouch -> airborne -> land). Fails to check `Talents.POUNCE` level!
5. **Priority 2**: `CatGunAttackGoal(this, 1.25D, 8.0F, 22.0F)` (`TalentedCat.java:247`)
   - TacZ ranged attack goal. Strafe & fire logic. Calls `moveTo()` every single tick.
6. **Priority 3**: Anonymous `MeleeAttackGoal(this, 1.35D, true)` (`TalentedCat.java:248-253`)
   - Checks `!isTwined() && !isIncapacitated() && !isOrderedToSit() && getCatMode() != CatMode.DOCILE && !hasEquippedGun()`.
7. **Priority 4**: `CatCreeperWardenGoal(this)` (`TalentedCat.java:254`)
   - Repels Creepers & Phantoms, grants Night Vision. Runs heavy AABB entity scans every single tick.
8. **Priority 5**: `CatRestOnBedGoal(this)` (`TalentedCat.java:255`)
   - Seeks `homeBedPos`. Permanently sets `isOrderedToSit(true)`, causing permanent sit-lock.
9. **Priority 6**: `CatUseScratcherGoal(this)` (`TalentedCat.java:256`)
   - Navigates to `CatScratcherBlock` for damage boost buff. Scans 1,445 blocks every 100 ticks; lacks path timeout.
10. **Priority 7**: `CatEatFromBowlGoal(this)` (`TalentedCat.java:257`)
    - Scans up to 6,845 blocks every 30 ticks; higher priority than following owner; lacks unreachable-bowl timeout.
11. **Priority 8**: `CatFetchToyGoal(this)` (`TalentedCat.java:258`)
    - Retrieves `ThrownToyEntity`. Stores carried item in ephemeral field (lost on world reload).
12. **Priority 9**: `CatFisherGoal(this)` (`TalentedCat.java:259`)
    - Fishes if water is within 2 blocks. Only sets `Flag.LOOK` (cat wanders away while fishing). Crashes if level >= 10 (`nextInt(<=0)`).
13. **Priority 10**: `CatComfortGoal(this)` (`TalentedCat.java:260`)
    - Passively heals cat when standing on bed/chest. Does not set flags; purrTimer never resets.
14. **Priority 10**: Anonymous `FollowOwnerGoal(this, 1.25D, 10.0F, 2.5F, false)` (`TalentedCat.java:263-286`)
    - Active only if mode is `FOLLOW`, `GUARD`, or `TACTICAL`. Clashes with other priority 10 goals.
15. **Priority 11**: `CatMoleMiningGoal(this)` (`TalentedCat.java:261`)
    - Digs ores out of thin air/glass. No ground/depth checks. Crashes if level >= 10 (`nextInt(<=0)`).
16. **Priority 11**: Anonymous `WaterAvoidingRandomStrollGoal(this, 1.0D)` (`TalentedCat.java:287-299`)
    - Active only if mode is `WANDER` or `PATROL`.
17. **Priority 12**: `CatPlaceTorchGoal(this)` (`TalentedCat.java:262`)
    - Places torches in darkness. Catastrophic $O(r^3)$ block search (up to 885,000 block queries per tick).
18. **Priority 12**: Anonymous `LookAtPlayerGoal(this, Player.class, 8.0F)` (`TalentedCat.java:300-309`)
19. **Priority 12**: Anonymous `RandomLookAroundGoal(this)` (`TalentedCat.java:310-319`)

#### Unregistered / Orphaned Goals
20. `CatPounceAttackGoal(this)` (`com.kittytalents.entity.ai.CatPounceAttackGoal:1-89`)
    - Implemented with leap, claw attack, slowness effect, and `Talents.POUNCE` check, but NEVER registered in `registerGoals()`.
21. `UseAbilityGoal(this)` (`com.kittytalents.ai.UseAbilityGoal:1-54`)
    - AI ability casting goal. NEVER instantiated or registered in `registerGoals()`. Casts abilities directly inside `canUse()`.

#### TargetSelector Registrations (`TalentedCat.java:321-359`)
1. **Priority 1**: Anonymous `OwnerHurtByTargetGoal(this)` (`TalentedCat.java:322-327`)
   - Active if `!isIncapacitated() && getCatMode() != CatMode.DOCILE && getCatMode() != CatMode.WANDER`.
2. **Priority 2**: Anonymous `OwnerHurtTargetGoal(this)` (`TalentedCat.java:328-333`)
   - Same condition as above.
3. **Priority 2**: `TrustedPlayerHurtByTargetGoal(this)` (`TalentedCat.java:334`)
   - Targets whatever attacks trusted players.
4. **Priority 2**: `TrustedPlayerHurtTargetGoal(this)` (`TalentedCat.java:335`)
   - Targets whatever trusted players attack.
5. **Priority 3**: Anonymous `HurtByTargetGoal(this)` (`TalentedCat.java:336-341`)
   - Self-defense revenge targeting (`getCatMode() != CatMode.DOCILE`).
6. **Priority 4**: `NearestAttackableTargetGoal<Monster>` (Guard Mode) (`TalentedCat.java:343-345`)
   - Checks `getCatMode() == CatMode.GUARD && distanceToSqr(mob) <= 256.0D`. Note: Checks distance from CAT, not guard post!
7. **Priority 4**: `NearestAttackableTargetGoal<Monster>` (Aggressive Mode) (`TalentedCat.java:347-349`)
   - Targets all `Monster.class` unconditionally (includes Creepers, which blow up cats).
8. **Priority 4**: `NearestAttackableTargetGoal<Spider>` (Pest Hunter) (`TalentedCat.java:351-353`)
9. **Priority 4**: `NearestAttackableTargetGoal<Silverfish>` (Pest Hunter) (`TalentedCat.java:354-356`)
10. **Priority 5**: `CatMouserGoal(this)` (`TalentedCat.java:358`)
    - Targets Silverfish, Endermite, Bat, CaveSpider, Rabbit if `MOUSER > 0` or mode is `HUNTING`.

---

### 1.2 Comparison with Doggy Talents Feature Set

| Feature / System | Doggy Talents (Next) Implementation | Kitty Talents Current State | Parity Status |
|---|---|---|---|
| **Mode: Docile** | Pacifist, no targeting, can follow/sit peacefully. | Clears targets, forces sitting (`setOrderedToSit(true)`), stops nav. | Partial (Conflates stance with mode) |
| **Mode: Wandering** | Roams within area, defends self, does not follow. | Runs `WaterAvoidingRandomStrollGoal`, excluded from owner defense. | Working (Basic) |
| **Mode: Aggressive** | Actively seeks hostiles within sight radius; follows owner if no targets. | Targets `Monster.class` (including Creepers!); has NO movement goal when idle (freezes in place). | Broken / Defective |
| **Mode: Tactical** | Follows owner; attacks threats; tethers to owner (<12-16 blocks); disengages if owner retreats. | Reuses `FollowOwnerGoal`. Has NO tether distance, NO disengage, NO tactical retreat. Chases mobs indefinitely. | Missing / Stub |
| **Mode: Berserker** | Relentless combat aggression, bonus speed/damage at low HP, pounce frenzy. | Defined in `CatMode.java:12`. 0 references in any AI goal or target selector. Cat freezes and does nothing. | Unimplemented Stub |
| **Mode: Patrol** | Walks between patrol posts/waypoints or orbits guard anchor. | Defined in `CatMode.java:13`. Reuses `WaterAvoidingRandomStrollGoal`. No waypoints, no perimeter. | Unimplemented Stub |
| **Mode: Guard** | Guards fixed post/bed within configured radius (e.g. 16m); attacks trespassers; returns to post. | `FollowOwnerGoal` is active in GUARD mode! Cat follows player instead of guarding! Target check tests distance to cat, not anchor. | Broken / Inverted |
| **Mode: Stay** | Stands/sits at fixed spot; does not wander or follow; remains vigilant. | Defined in `CatMode.java:15`. 0 references in AI goals. Calling `setCatMode(STAY)` un-sits the cat and leaves it standing idle. | Unimplemented Stub |
| **Mode: Protect** | Specialized bodyguard mode focusing on projectile deflection and meat-shielding. | Defined in `CatMode.java:16`. 0 references in codebase. | Unimplemented Stub |
| **Talent: Hunting** | Dog hunts animals/prey, retrieves meat/leather. | `HUNTER_CAT`: Only grants bonus damage against animals in `doHurtTarget`. NO AI goal to target animals! Cats never hunt animals. | Broken / Incomplete |
| **Talent: Fishing** | Dog pathfinds to water, catches fish, deposits in pack. | `FISHER_CAT` (`CatFisherGoal`): Only fishes if already standing $\le$2 blocks from water (no pathfinding to water). Wanders away while fishing. Drops fish at feet. | Broken / Incomplete |
| **Talent: Healing** | Dog applies regen/healing to hurt owner & allies; ambulance mode. | `PURR_MEDIC`: Hardcoded in `tick()`. `RESCUE_PURR`: Hardcoded emergency teleport in `tick()`. Active `HealingPurrAbility` unregistered. | Partial (Bypasses AI goals) |
| **Talent: Guarding** | Interposes to absorb hits; counter-attacks; shield stance. | `GUARDIAN_CAT`: Flat damage reduction in `hurt()`. No AI bodyguard behavior. | Missing AI Behavior |
| **Talent: Fetching** | Dog catches thrown bones/balls, brings to owner, holds in mouth slot. | `CatFetchToyGoal`: Fetches `ThrownToyEntity`. Carried toy is unsaved in memory (lost on chunk reload/restart). No mouth rendering. | Buggy / Fragile |
| **Talent: Mounting** | Player rides dog; steering; speed and jump scaling. | `CAT_MOUNT`: Functional riding in `travel()`. BUT empty-hand right click always mounts the cat, permanently locking out the sit/stand toggle! | Serious Interaction Bug |
| **Talent: Torches** | Dog places torches in dark areas without spamming. | `CatPlaceTorchGoal`: Up to 885,000 block queries per tick! Consumes soul torches but always places regular torches. Gets stuck if spot unreachable. | Major Server Lag Hazard |
| **Talent: Digging / Mining** | Dog digs in stone/dirt underground for ores/bones. | `CatMoleMiningGoal`: Digs ores out of glass/air anywhere. No stone or underground check. Crashes on high level. | Defective / Exploit |
| **Follow Owner** | Intelligent sprint matching, water pathing, obstacle avoidance, safe teleport. | Priority 10 (overridden by eating/scratching/toys). Crudely multiplies speed attribute without resetting. Vanilla teleport can strand cats in blocks/trees. | Poor Priority / Clunky |
| **Sit & Stand** | Clean stance toggle independent of mode. | Conflated with `DOCILE` mode. `CatRestOnBedGoal` locks cat in permanent sitting pose. Whistle `STAND` fails if in `DOCILE`. | Severely Buggy |
| **Sleep & Bed** | Cat rests on bed at night/injured, wakes up and unsits cleanly. | `CatRestOnBedGoal` causes permanent sit-lock. In `DOCILE` mode, runs infinitely. Downed incapacitated cat stands up and runs to bed! | Broken States |
| **Play Behavior** | Plays with toys, chases balls, responds to play commands. | `CatFetchToyGoal` only responds to thrown entity. `WhistleCommand.PLAY` just sets mode to `WANDER`. `CatLaserPointerItem` moveTo is wiped next tick. | Incomplete |
| **Target Avoidance** | Avoids attacking creepers during ignition; avoids hazards (lava, cliffs). | Cat in Aggressive/Guard attacks Creepers, dying to explosions. `customServerAiStep` parkour flings cats off 50-block cliffs into lava. | Hazard / Suicidal |

---

### 1.3 Verbatim Bugs, Broken States, Null Pointer Risks & Stuck Conditions

#### Critical Bug 1: Permanent Sitting Lockout from Bed Resting (`CatRestOnBedGoal.java:61, 88-92`)
```java
// Line 61:
cat.setOrderedToSit(true);

// Lines 89-92:
@Override
public void stop() {
    restTicks = 0;
}
```
**Observation**: When resting on a bed, the cat calls `cat.setOrderedToSit(true)`. When the goal stops, it resets `restTicks = 0` but NEVER calls `setOrderedToSit(false)`. The cat is permanently sit-locked. The player must manually find and right-click the cat to stand it back up.

#### Critical Bug 2: Permanent Infinite Goal Execution in DOCILE Mode (`CatRestOnBedGoal.java:84-87`)
```java
@Override
public boolean canContinueToUse() {
    if (cat.getHomeBedPos() == null) return false;
    return (cat.isIncapacitated() || cat.getHealth() < cat.getMaxHealth() || cat.level().isNight() || cat.getCatMode() == CatMode.DOCILE);
}
```
**Observation**: If `getCatMode() == CatMode.DOCILE` and `homeBedPos != null`, `canContinueToUse()` unconditionally returns `true` forever. The cat will never release this goal. Even during full daylight at max health, it remains trapped in the bed resting goal forever.

#### Critical Bug 3: Incapacitated Downed Cat Runs to Bed (`CatRestOnBedGoal.java:35, 49`)
**Observation**: When a cat is downed/incapacitated (`DATA_INCAPACITATED = true`), `CatRestOnBedGoal` matches `cat.isIncapacitated()`, calls `moveTo(bedPos)`, and pathfinds across up to 20 blocks. A cat that is supposed to be unconscious/bleeding out stands up and sprints to a bed.

#### Critical Bug 4: Empty-Hand Right Click Lockout with CAT_MOUNT (`TalentedCat.java:1983-1994`)
```java
// Mounting the cat
if (getTalentLevel(Talents.CAT_MOUNT) > 0 && (isOwnedBy(player) || isTrusted(player.getUUID()) || player.getAbilities().instabuild) && (held.isEmpty() || held.is(Items.SADDLE))) {
    if (!this.level().isClientSide && !this.isVehicle()) {
        player.startRiding(this);
    }
    return InteractionResult.sidedSuccess(this.level().isClientSide);
}

// Normal right click toggles sit/stand
if ((isOwnedBy(player) || isTrusted(player.getUUID())) && held.isEmpty()) {
    setCatMode(getCatMode() == CatMode.DOCILE ? CatMode.FOLLOW : CatMode.DOCILE);
    return InteractionResult.sidedSuccess(this.level().isClientSide);
}
```
**Observation**: Once `CAT_MOUNT` is learned, right clicking with an empty hand matches the mounting check first and mounts the cat. Lines 1991-1994 (toggle sit/stand) are NEVER reachable with an empty hand. The player cannot command their cat to sit or stand.

#### Critical Bug 5: Nine Lives Infinite Godmode Exploit (`TalentedCat.java:1304-1312`)
```java
// Nine Lives instant totem revival
int nineLives = getTalentLevel(Talents.NINE_LIVES);
if (nineLives > 0 && !isIncapacitated()) {
    this.setHealth(Math.max(4.0F, this.getMaxHealth() * (0.18F * nineLives)));
    this.level().broadcastEntityEvent(this, (byte) 35);
    this.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
    this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
    this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
    return false;
}
```
**Observation**: `nineLives` is the talent level (1 to 5). It is NEVER decremented, there is no life charge counter, and there is ZERO cooldown. Any cat with Nine Lives Level 1 revives on every single fatal hit indefinitely. It is 100% immune to death.

#### Critical Bug 6: Crash on High Talent Level (`CatFisherGoal.java:34`, `CatMoleMiningGoal.java:40`)
```java
// CatFisherGoal.java:34
if (cat.getRandom().nextInt(100 - (fisherLevel * 10)) != 0) return false;

// CatMoleMiningGoal.java:40
return cat.getRandom().nextInt(150 - (moleLvl * 15)) == 0 && cat.onGround();
```
**Observation**: If `fisherLevel >= 10`, `100 - (10 * 10) = 0`. Passing $\le 0$ to `Random.nextInt()` throws an immediate `IllegalArgumentException: bound must be positive`, crashing the Minecraft server. The exact same vulnerability exists in `CatMoleMiningGoal` if `moleLvl >= 10`.

#### Critical Bug 7: Infinite Stuck Loop on Unreachable Scratcher (`CatUseScratcherGoal.java:58, 83`)
```java
// Lines 58, 83:
if (cat.distanceToSqr(...) < 3.5D) { scratchTicks++; ... }

@Override
public boolean canContinueToUse() {
    return scratcherPos != null && scratchTicks < 60 && !cat.isIncapacitated() && !cat.isOrderedToSit();
}
```
**Observation**: If the scratcher is separated by a fence, wall, or door, `distanceToSqr` never drops below 3.5. Therefore `scratchTicks` never reaches 60. `canContinueToUse()` returns `true` indefinitely. The cat stays permanently stuck trying to pathfind to the unreachable scratcher, ignoring all other tasks.

#### Critical Bug 8: Catastrophic 885,000 Block Search Lag (`CatPlaceTorchGoal.java:152-199`)
**Observation**: At level 5 (`radius = 13`), `findValidTorchPos` iterates $(27 \times 27 \times 5) = 3,645$ block positions. For every position matching `isValidPlacementLocation`, it calls `hasTorchNear(pos, 4)` which scans $(9 \times 3 \times 9) = 243$ blocks. This results in up to $3,645 \times 243 = 885,735$ block state reads during a single tick in `canUse()`. With multiple cats, this will freeze server TPS.

#### Critical Bug 9: Data Loss on Chunk Unload / Restart (`CatFetchToyGoal.java:20`, `TalentedCat.java:156-181`)
**Observation**:
- `CatFetchToyGoal.carriedToy`: Carried items are stored solely in a non-persistent field `private ItemStack carriedToy`. If the chunk unloads or server restarts while the cat is carrying a toy, the item is deleted forever.
- `TalentedCat`: `abilitySlot1`, `abilitySlot2`, `abilitySlot3`, `abilityUltimate`, and `energy` are private in-memory fields with no serialization in `addAdditionalSaveData()` or `readAdditionalSaveData()`. Every chunk reload or server reboot completely wipes all player-assigned abilities and resets energy.

#### Critical Bug 10: Energy Never Regenerates (`TalentedCat.java:180-181`)
**Observation**: `energy` is initialized to 100. Casting abilities calls `cat.setEnergy(cat.getEnergy() - energyCost)`. There is zero energy regeneration logic in `tick()`. Once a cat spends its energy, it can never cast an ability again for the rest of its existence.

#### Critical Bug 11: 5 out of 6 Active Abilities Unregistered (`CatAbilities.java:7-19`)
```java
public class CatAbilities {
    private static final Map<String, CatAbility> REGISTRY = new HashMap<>();
    public static final CatAbility POUNCE = register(new PounceAbility());
...
```
**Observation**: Only `POUNCE` is registered. `FelineHissAbility`, `HealingPurrAbility`, `ShadowStrikeAbility`, `UtilityInteractAbility`, and `VoltDashAbility` are completely omitted. Calling `CatAbilities.get("feline_hiss")` returns `null`.

#### Critical Bug 12: `UseAbilityGoal` Never Instantiated or Registered (`UseAbilityGoal.java:9-53`)
**Observation**: `UseAbilityGoal` exists in `com.kittytalents.ai`, but is never added to `goalSelector` in `TalentedCat.registerGoals()`. AI cats NEVER cast active abilities autonomously. Moreover, line 48 calls `best.tryCast(cat, null)` inside `canUse()`, violating Minecraft AI contract by mutating game state during selector evaluation.

#### Critical Bug 13: `CatFoxPounceGoal` Completely Ignores Talent Level (`CatFoxPounceGoal.java:30-48`)
**Observation**: `canUse()` does NOT check `cat.getTalentLevel(Talents.POUNCE) > 0`. Any cat with 0 talent points leap-attacks like a fox. Furthermore, `CatPounceAttackGoal` (which does check talent level) is completely orphaned.

#### Critical Bug 14: Suicidal Cliff-Leaping in `customServerAiStep` (`TalentedCat.java:669-676`)
```java
BlockPos ahead = this.blockPosition().relative(this.getDirection());
if (this.level().isEmptyBlock(ahead) && this.level().isEmptyBlock(ahead.below())) {
    Vec3 dir = this.getLookAngle();
    this.setDeltaMovement(this.getDeltaMovement().x + dir.x * 0.22D, 0.42D, this.getDeltaMovement().z + dir.z * 0.22D);
    this.hasImpulse = true;
}
```
**Observation**: The custom gap-jumping code checks if `ahead` and `ahead.below()` are air. If so, it injects vertical and forward velocity. If the cat is walking near a cliff, mountain, ravine, or balcony, it flings itself into the abyss or lava.

#### Critical Bug 15: Whistle Command Discrepancies (`PacketWhistleCommand.java:64-167`)
**Observation**:
- `WhistleCommand.QUIET`: Instead of calling `cat.setQuietUntil(cat.tickCount + 6000)` (which already exists in `TalentedCat.java:1046`), line 160 applies `MobEffects.INVISIBILITY` for 5 minutes!
- `WhistleCommand.ATTACK_TARGET`: Missing from `switch (cmd)`! Hits `default -> {}` and does nothing.
- `WhistleCommand.FETCH`: Missing from `switch (cmd)`! Hits `default -> {}` and does nothing.
- `WhistleCommand.STAND`: Calls `setOrderedToSit(false)`, but does NOT change `CatMode.DOCILE`. Because the mode remains `DOCILE`, the cat immediately refuses to move or attack.
- `WhistleCommand.FISH`, `MINE`, `EAT`: Empty stubs that only call `setOrderedToSit(false)`.

#### Critical Bug 16: Zero-Talent Stub Catalog
- `CRITICAL_CLAWS`: 0 references in entity/combat/AI. Pure stub talent.
- `FELINE_REFLEXES`: 0 references in entity/combat/AI. Pure stub talent.
- `CUSHION_PAWS`: 0 references. Pure stub talent (vanilla Cat already takes 0 fall damage).
- `HOME_WHISKERS`: 0 references in entity/combat/AI. Pure stub talent.

---

## 2. Logic Chain

1. **Premise 1 (AI Goal Invocations)**: AI behaviors in Minecraft Forge depend strictly on `goalSelector` and `targetSelector` priorities and `canUse()` / `canContinueToUse()` preconditions.
2. **Premise 2 (Mode Segregation)**: In Doggy Talents, mode dictates which goals are eligible to run, while stance (sit/stand) is orthogonal. In Kitty Talents, `CatMode` is conflated with stance (`DOCILE` = sit, others = stand). Modes like `BERSERKER`, `PATROL`, `STAY`, and `PROTECT` have no goal backing, leading to frozen entities.
3. **Premise 3 (Stuck State Perpetuation)**: When goals lack timeouts (`CatUseScratcherGoal`, `CatRestOnBedGoal`, `CatEatFromBowlGoal`, `CatFetchToyGoal`, `CatFoxPounceGoal`), unreachable targets cause infinite loops where the goal never finishes or yields to lower-priority goals like `FollowOwnerGoal`.
4. **Premise 4 (Performance Degradation)**: Every-tick AABB queries (`CatCreeperWardenGoal`), tick-by-tick `moveTo()` calls (`CatGunAttackGoal`, `CatFetchToyGoal`), and massive $(3,645 \times 243)$ block scans (`CatPlaceTorchGoal`) degrade server TPS proportionally to the number of cats in active chunks.
5. **Premise 5 (Memory/Persistence Fragility)**: Non-NBT state fields (`carriedToy`, `abilitySlot1..3`, `energy`) cause catastrophic state loss across server restarts and chunk unloads.
6. **Deduction**: A comprehensive AI overhaul must:
   - Decouple stance from mode.
   - Implement missing mode goals (`BERSERKER`, `PATROL`, `TACTICAL`, `GUARD`, `STAY`).
   - Add timeout, stuck detection, and fail cooldowns to all environmental interaction goals.
   - Throttle block and entity searches.
   - Persist all transient state to NBT.
   - Re-architect goal priorities to place `FollowOwnerGoal` above optional activities.

---

## 3. Concrete AI Overhaul Plan

### 3.1 Proposed Goal Priority Architecture

```
GoalSelector:
Priority 0: FloatGoal (Emergency survival in water)
Priority 1: SitWhenOrderedToGoal (Stance control; inactive when twined)
Priority 1: TwinedControlGoal (Player driving via Twine)
Priority 2: IncapacitatedGoal (Lying down, crying out, awaiting revival)
Priority 3: TacticalDisengageGoal (TACTICAL mode: tether return when >12 blocks from owner)
Priority 3: GuardReturnAnchorGoal (GUARD mode: return to guard post when threats cleared)
Priority 4: CatGunAttackGoal (Equipped gun ranged combat with burst cooldowns)
Priority 4: CatFoxPounceGoal (Pounce leap combat; requires POUNCE talent; max 60t airborne timeout)
Priority 4: UseAbilityGoal (Casts equipped abilities when energy & cooldown permit)
Priority 5: MeleeAttackGoal (Normal melee combat; disengages from igniting creepers)
Priority 6: FollowOwnerGoal (FOLLOW, TACTICAL modes; priority 6 ensures it overrides eating/toys when owner moves away)
Priority 7: PatrolPerimeterGoal (PATROL mode: patrols between waypoints or around bed anchor)
Priority 8: CatEatFromBowlGoal (Eats when hunger < 60% or injured; 100t stuck timeout)
Priority 8: CatRestOnBedGoal (Rests on bed at night/injured; clears sit on exit; 120t timeout)
Priority 9: CatFetchToyGoal (Fetches thrown toy/ball; persists carried item in NBT)
Priority 9: CatFisherGoal (Pathfinds to water if FISHER_CAT > 0; Flag.MOVE + LOOK; safe random bound)
Priority 9: CatMoleMiningGoal (Digs in underground stone/deepslate; requires solid ground; safe random bound)
Priority 9: CatPlaceTorchGoal (Places torches; throttled 1 probe/tick; supports Soul/Redstone torches)
Priority 9: CatUseScratcherGoal (Scratching post buff; 80t path timeout)
Priority 10: WaterAvoidingRandomStrollGoal (WANDER mode idle roam)
Priority 11: LookAtPlayerGoal
Priority 12: RandomLookAroundGoal

TargetSelector:
Priority 1: OwnerHurtByTargetGoal (Defends owner; filters pets & allies)
Priority 2: OwnerHurtTargetGoal (Attacks owner's target; filters pets & allies)
Priority 3: TrustedPlayerHurtByTargetGoal
Priority 4: TrustedPlayerHurtTargetGoal
Priority 5: HurtByTargetGoal (Self-defense revenge)
Priority 6: TacticalTargetGoal (TACTICAL mode: only targets within 12 blocks of owner)
Priority 7: GuardTargetGoal (GUARD mode: targets monsters within 16 blocks of guard post)
Priority 7: AggressiveTargetGoal (AGGRESSIVE / BERSERKER modes: targets hostile mobs; avoids igniting creepers)
Priority 8: HunterPreyGoal (HUNTING mode or HUNTER_CAT talent: targets wild prey/livestock)
Priority 8: PestHunterTargetGoal (PEST_HUNTER / MOUSER talents: targets spiders, silverfish, endermites, bats, rabbits)
```

### 3.2 Mode State Transition Machine

```
              ┌───────────────────────────┐
              │          DOCILE           │  (Passive companion, sits/follows peacefully,
              └─────────────┬─────────────┘   zero combat targeting)
                            │
              ┌─────────────┴─────────────┐
              │          FOLLOW           │  (Standard companion: defends owner, sprints,
              └─────────────┬─────────────┘   follows closely)
                            │
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
┌─────────────────┐┌─────────────────┐┌─────────────────┐
│     WANDER      ││      GUARD      ││    TACTICAL     │
│(Roams territory;││(Guards anchor;  ││(Tethered to     │
│ defends self)   ││ does not follow;││ owner <12 blocks;│
└─────────────────┘│ returns to post)││ tactical retreat)│
                   └─────────────────┘└─────────────────┘
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
┌─────────────────┐┌─────────────────┐┌─────────────────┐
│   AGGRESSIVE    ││    BERSERKER    ││     PATROL      │
│(Seeks hostiles; ││(Enraged melee,  ││(Patrols between │
│ idle follows)   ││ leap frenzy,    ││ waypoints/posts)│
└─────────────────┘│ death defiance) │└─────────────────┘
                   └─────────────────┘
```

- **Stance Orthogonality**: Stance (`isOrderedToSit()`) must be a separate boolean from `CatMode`. Right-clicking toggles sit/stand regardless of mode (except in Twine or Incapacitated).
- **Guard Anchor**: When entering `GUARD` mode, the cat records its current position as `guardAnchorPos`. In combat, it only pursues targets within 16 blocks of `guardAnchorPos`. When targets are eliminated or exceed 20 blocks, `GuardReturnAnchorGoal` navigates back to `guardAnchorPos`.
- **Tactical Tether**: In `TACTICAL` mode, if `cat.distanceToSqr(owner) > 144.0D` (12 blocks) or target is > 16 blocks from owner, combat is broken, target is set to `null`, and cat returns to owner.
- **Berserker Combat**: In `BERSERKER` mode, cat gains 20% speed bonus, attacks with increased aggression, ignores damage flinch, and triggers enrage claw strikes at <40% HP.

### 3.3 Event Hooks and System Interconnects

1. **Talent Activation Event Hooks**:
   - `onLivingHurt`: Apply `GUARDIAN_CAT` damage reduction, `FELINE_REFLEXES` dodge chance (fix stub), `CRITICAL_CLAWS` counter-attack (fix stub), `FIREPROOF_PAWS` immunity.
   - `onAttackTarget`: Apply `CRITICAL_CLAWS` crit multipliers, `VENOM_CLAWS` status effects, `INFERNO_CAT` fire.
   - `onFatalDamage`: Fix `NINE_LIVES` — implement 9 finite lives per day or 10-minute revival cooldown; consume life token and broadcast particle burst.
2. **Abilities System Hookup**:
   - Register `FelineHissAbility`, `HealingPurrAbility`, `ShadowStrikeAbility`, `UtilityInteractAbility`, `VoltDashAbility` in `CatAbilities.REGISTRY`.
   - Add `energy` regeneration in `TalentedCat.tick()`: $+1$ energy per 20 ticks (1/sec), boosted by `CatRole.MAGE` and `LUCKY_PURR`.
   - Serialize `abilitySlot1..3`, `abilityUltimate`, and `energy` in NBT.
   - Register `UseAbilityGoal` in `goalSelector` at Priority 4.
3. **Whistle Command System Fixes**:
   - Implement `case ATTACK_TARGET`: Raytrace player target up to 32 blocks, set as `cat.setTarget(target)`.
   - Implement `case FETCH`: Trigger item retrieval routine.
   - Fix `case QUIET`: Call `cat.setQuietUntil(cat.tickCount + 6000)` instead of granting invisibility.
   - Fix `case STAND`: Clear sitting pose and ensure mode transitions cleanly.
   - Implement `case FISH`, `MINE`, `EAT`: Actively force navigation to nearest water, valid stone block, or food bowl.

---

## 4. Caveats

1. **Client Rendering Layers**: This survey focused on AI goals, targeting logic, entity states, and talents. It did not audit client-side rendering layer performance (e.g. `TalentedCatModel` or `TalentedCatWingLayer`).
2. **Third-Party Mods**: TacZ and Apotheosis compat classes were verified for reflection safety. However, dynamic AI interactions when TacZ guns are fired depend on TacZ's runtime bytecode which was analyzed via decompiled signatures.
3. **Doggy Talents Architecture Differences**: Doggy Talents uses an extensive capability-based modular talent addon structure. Directly rewriting Kitty Talents into an identical capability architecture would require changing hundreds of classes; an encapsulated goal/handler model inside the existing package layout is recommended for R3.

---

## 5. Conclusion

The current Kitty Talents AI system is heavily compromised by:
1. **Broken/Stub Modes**: Half of the modes (`BERSERKER`, `PATROL`, `STAY`, `PROTECT`) are unimplemented stubs, while `GUARD` inverted its logic by following the owner instead of guarding an anchor.
2. **Critical Game-Breaking Bugs**: Permanent sitting lock from bed resting, empty-hand lockout with `CAT_MOUNT`, Nine Lives infinite invulnerability exploit, and server crash bugs on high talent levels.
3. **Severe Performance Bottlenecks**: Single-tick 885,000 block queries in torch placement, 6,800 block scans in bowl eating, and every-tick entity/pathfinding executions.
4. **Orphaned Systems**: All abilities (except Pounce) are unregistered; `UseAbilityGoal` is completely uncalled; multiple talents are 0-reference stubs.

The proposed AI Overhaul Plan resolves every identified bug, achieves complete feature parity with Doggy Talents, introduces robust goal priorities with stuck/timeout protections, and establishes a production-grade companion AI foundation.

---

## 6. Verification Method

### 6.1 Independent Code Verification
1. Inspect `CatRestOnBedGoal.java:61, 88-92` to verify permanent sit-lock.
2. Inspect `TalentedCat.java:1983-1994` to verify empty-hand right-click lockout.
3. Inspect `TalentedCat.java:1304-1312` to verify Nine Lives cooldown/charge omission.
4. Inspect `CatFisherGoal.java:34` and `CatMoleMiningGoal.java:40` to verify `nextInt(<=0)` crash.
5. Inspect `CatPlaceTorchGoal.java:152-199` to verify $O(r^3)$ nested search volume.
6. Inspect `PacketWhistleCommand.java:64-167` to verify missing `ATTACK_TARGET`, missing `FETCH`, and invisibility bug in `QUIET`.
7. Inspect `CatAbilities.java:7-19` to verify 5 unregistered abilities.

### 6.2 Test World Behavior Verification
1. **Behavior 1 (Mode Cycling & Movement)**:
   - Command cat into `BERSERKER`, `HUNTING`, and `AGGRESSIVE` modes with no mobs nearby. Observe that the cat currently freezes as a statue due to missing idle goals.
2. **Behavior 2 (Bed Resting & Permanent Sit Lock)**:
   - Link cat to bed (`CatBedBlock`). Let cat rest until fully healed. Observe that cat remains permanently sitting and never follows until manually clicked.
3. **Behavior 3 (Mount vs Sit Conflict)**:
   - Level `CAT_MOUNT` to 1. Attempt to right click cat with empty hand to toggle sit/stand. Observe that player mounts the cat instead of toggling sit.
4. **Behavior 4 (Whistle Command Verification)**:
   - Whistle `Quiet`. Observe that cats become invisible (`INVISIBILITY` effect) instead of muting meows.
   - Whistle `Attack Focus`. Observe that no cat responds or targets the aimed mob.
