# Original User Request

## Initial Request — 2026-10-06T20:46:08Z

Conduct a major 3.0 overhaul of the Kitty Talents Minecraft mod. The scope includes debugging the twine system, optimizing the codebase for server/LAN performance, overhauling the AI to mirror Doggy Talents, and refining all assets (VFX, SFX, textures).

Working directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Integrity mode: development

## Requirements

### R1. Twine System Debugging
Fix all existing bugs and edge cases within the Twine (control) system so the player can smoothly control the cat.

### R2. Performance & Multiplayer Optimization
Perform a full codebase sweep to optimize TPS and memory usage. Ensure 100% compatibility with dedicated servers and LAN multiplayer (no client-side code running on the server).

### R3. AI Logic Overhaul
Rework the cat AI logic to ensure all features are fully finished, robust, and operate reliably, drawing inspiration from Doggy Talents.

### R4. Asset Polish
Refine and update textures, visual effects (VFX), graphical effects (GFX), and sound effects (SFX) for the cats to ensure high quality and no missing asset errors.

## Acceptance Criteria

### Twine & AI Verification (Agent-as-Judge)
- [ ] An independent agent must launch the provided test world, initiate the Twine system, and confirm smooth control without camera or input desyncs.
- [ ] An independent agent must trigger at least 3 distinct overhauled AI behaviors in the test world and confirm they execute completely without getting stuck or throwing errors.

### Multiplayer & Performance Verification
- [ ] The mod must successfully compile and launch a Dedicated Server instance without crashing due to client-side class loading errors (e.g. `NoClassDefFoundError` for rendering classes).
- [ ] The client must successfully connect to the local Dedicated Server instance, spawn a cat, and interact with it without generating server-side console errors or desyncs.

### Asset Verification
- [ ] The Minecraft client log (`latest.log`) must be scanned after loading into the test world to ensure there are exactly zero `Missing texture` or `Missing sound` warnings related to the mod's namespace.


## Follow-up — 2026-10-06T20:49:03Z

The user has explicitly requested to scale up the agent workforce to a "swarm" level. Please use a very large team of agents / maximum swarm scaling to explore, fix, and optimize the codebase in parallel to the greatest extent possible.


## Follow-up — 2026-10-06T20:57:34Z

The user has requested an additional specific fix for the 3.0 overhaul (R4 Asset Polish / Rendering): The cape accessory is currently rendering a bit too low on the cat (it's appearing under the cat instead of above it). Please ensure the rendering layer/model for the cape is adjusted so it sits properly on the cat's back.


## Follow-up — 2026-10-06T20:58:51Z

The user has requested additional specific fixes for the 3.0 overhaul (R4 Asset Polish / Rendering): 
1. The cat armors currently look "goofy" and need to be refined/remodeled so they fit and look much better on the cats.
2. The black panther skin should be updated to have a proper panther texture with blue eyes. 
Please ensure these are added to the R4 Asset and Rendering milestones.


## Follow-up — 2026-10-06T21:01:47Z

URGENT: The user has just provided the comprehensive "KITTY TALENTS 3.0 MASTER DIRECTIVE". I have saved this exact document to the following artifact path: C:\Users\VivianV\.gemini\antigravity\brain\8316eb53-acda-48ee-979a-5c7154d00eb8\KITTY_TALENTS_3_MASTER_DIRECTIVE.md
It details the exact scope, gameplay layers, Aniimo-style Twine rules, real-time combo systems, active ability requirements, and stat pipelines required for 3.0. Please read this file immediately and inject its requirements deeply into the `PROJECT.md` blueprint before you launch the Implementation Swarms!


## Follow-up — 2026-10-06T21:03:43Z

The user has requested another addition to the 3.0 mechanics (R3/AI & Combat overhaul):
Please add visual effects and damage/support/defense bonuses and debuffs to the cats' attacks. These bonuses/debuffs/VFX should scale or differ based on the cat's specific "type" (element/role) and "tier" (production tier). Ensure this logic is integrated into the real-time combat and stat engine pipelines currently being planned in `PROJECT.md`.


## Follow-up — 2026-10-06T21:24:20Z

URGENT: The user is running low on tokens and has requested that you accelerate and WRAP UP the 3.0 overhaul immediately. Please finalize the active Batch 1 implementations, skip or greatly condense the remaining Batch 2 milestones (Twine, AI, Combat VFX) into a stable, committable state, run your final verifications against the Acceptance Criteria, and deliver the final report as quickly as possible without starting new major tasks.


## 2026-10-07T03:06:12Z

Do not create one agent per requirement. Use a fixed team of 8 specialized agents, and have the orchestrator distribute the requirements among them. Agents may work sequentially or in parallel only when their changes do not conflict.

# Teamwork Project Prompt — Kitty Talents 3.0

> **Status:** Ready for Teamwork execution
> **Requested team:** Full Team
> **Integrity mode:** Development
> **Goal:** Major refinement, debugging, optimization, gameplay expansion, and asset-quality overhaul of the existing Kitty Talents project.
> **Working directory:** C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents

---

# PROJECT MISSION

Perform a major **Kitty Talents 3.0** overhaul on the existing mod.

This is an existing, feature-heavy Minecraft 1.20.1 Forge project.

**Do NOT rebuild the mod from scratch.**

**Do NOT replace working systems unnecessarily.**

Inspect the current implementation first, preserve everything that already works, and improve/refactor only where necessary.

The goal is to turn Kitty Talents into a polished **feline companion RPG** with:

* deep companion progression
* strong feline AI
* responsive combat
* direct player-controlled Twining
* creature-style abilities and builds
* exploration/traversal abilities
* reliable LAN/dedicated-server behavior
* excellent performance
* polished cat models/armor/cosmetics
* professional VFX/SFX/GFX
* complete persistence
* no fake or unfinished features

The intended gameplay feeling combines:

**Pokémon-inspired creature RPG depth**
with
**Aniimo-inspired direct creature control/Twining**
with
**Doggy Talents / Doggy Talents Next-style companion depth**
with
**Minecraft real-time sandbox gameplay**.

Do not copy proprietary code, assets, UI, sounds, exact creature designs, moves, or terminology. Use those projects only as high-level functional/design references.

---

# R1. TWINE SYSTEM — MAJOR OVERHAUL

Twine is one of the most important systems in Kitty Talents 3.0.

Fix all existing Twine bugs, synchronization problems, camera issues, movement problems, control conflicts, rendering problems, and edge cases.

Twine must feel like:

> **"I am now my cat."**

not:

> **"My camera is attached to my cat."**

When Twined, the player must directly control the cat's:

* movement
* look direction
* sprinting
* jumping
* combat
* abilities
* Energy
* dodge
* pounce
* equipment
* traversal abilities
* environmental interactions

Twine must use the cat's real gameplay state rather than creating a fake second combat system.

### Twine controls should support

* WASD movement
* mouse look
* sprint
* jump
* dodge
* left-click basic attack
* right-click Pounce
* active abilities
* Ultimate
* configurable Untwine key

All controls must be configurable and responsive.

### Twine camera

Fix:

* mouse lag
* camera snapping
* yaw desynchronization
* pitch problems
* camera jitter
* incorrect eye height
* model clipping
* head/ear obstruction
* muzzle obstruction
* mouth-weapon obstruction
* bad pounce camera behavior
* incorrect camera behavior while sitting, swimming, flying, mounting, etc.

Use the cat's actual dimensions and eye position.

Hide only first-person geometry that obstructs the view. Do not hide the cat model from other players.

### Twine state management

Twine must safely handle:

* entering
* exiting
* death
* incapacitation
* disconnect
* reconnect
* chunk unloading
* dimension changes
* teleportation
* cat removal
* player death
* server restart

Never leave:

* orphaned Twine sessions
* permanently locked cats
* detached cameras
* duplicated player state
* broken AI
* lost inventory
* duplicated entities

Only one player may Twine a cat at a time.

---

# R2. POKÉMON/ANIIMO-STYLE CREATURE RPG COMBAT

Expand combat into a genuine **real-time creature RPG system**.

Do NOT make Minecraft turn-based.

Combat remains real-time but gains:

* creature roles
* stats
* elemental affinities
* strengths/weaknesses
* active abilities
* passive talents
* status effects
* cooldowns
* Energy
* Ultimates
* builds
* team synergy

The player should be able to build two cats of the same visual breed into completely different combat characters.

---

# R3. CAT COMBAT ROLES

Support roles such as:

* DPS
* Assassin
* Tank
* Bruiser
* Support
* Healer
* Controller
* Breaker
* Scout
* Mobility
* Hybrid

Roles should primarily emerge from:

* talents
* abilities
* equipment
* stats

Breed/skin should not rigidly lock a cat into one role.

---

# R4. ACTIVE ABILITY SYSTEM

Create/refine a real ability system.

Recommended structure:

* Basic Attack
* Skill 1
* Skill 2
* Skill 3
* Ultimate
* Passive/Innate ability

Abilities must have real gameplay effects.

Each ability should support, where applicable:

* damage
* healing
* buffs
* debuffs
* movement
* crowd control
* elemental effects
* Energy cost
* cooldown
* targeting
* range
* animation
* particles
* sound

The ability system should be data-driven where practical.

---

# R5. FELINE ENERGY

Add/refine a shared **Feline Energy** system.

Energy is used for things such as:

* active abilities
* dodge
* advanced movement
* Pounce
* special attacks

Energy should recover through normal gameplay, talents, attacks, resting, or other designed mechanics.

The system must work equally in:

* AI combat
* Command mode
* Twine mode

---

# R6. REAL-TIME COMBAT FEEL

Improve combat so it feels responsive and intentional.

Support:

* proper attack timing
* combo chains
* attack recovery
* hit detection
* critical hits
* knockback
* status application
* dodge windows
* perfect dodge
* hit feedback

Pounce should remain a signature Kitty Talents mechanic and should physically launch the cat rather than teleport it.

---

# R7. ELEMENTS AND STATUS EFFECTS

Create/refine an original elemental system inspired by creature RPG design.

Possible affinities include:

* Physical
* Fire
* Water
* Frost
* Poison
* Electric
* Nature
* Shadow
* Arcane

Create a balanced and configurable matchup system.

Add meaningful statuses such as:

* Burn
* Poison
* Bleed
* Slow
* Shock
* Freeze
* Mark
* Vulnerable
* Weakness
* Haste
* Regeneration
* Fortified

Prevent infinite stacking and clearly define duration, intensity, resistance, and immunity.

---

# R8. TALENTS → REAL GAMEPLAY

Audit every existing talent.

Every registered talent must actually work.

Do not allow talent buttons to modify a GUI number without changing gameplay.

Talents should support:

* passive stat bonuses
* active skills
* combat specialization
* exploration
* traversal
* support
* utility
* survival

Examples already associated with Kitty Talents include:

* Pounce
* Critical Claws
* Cat Armory
* Cat Armor
* Inferno Cat
* Venom Claws
* Feline Berserker
* Guardian Cat
* Feline Agility
* Cushion Paws
* Aqua Kitty
* Cat Mount
* Sky Feline
* Kitty Pack
* Torch Kitty
* Paw Retriever
* Sniffer Whiskers
* Purr Medic
* Alchemy Kitty
* Catnip Catastrophe

Audit all additional talents present in the actual codebase.

---

# R9. TWINE TRAVERSAL / EXPLORATION

Twine should matter outside combat.

Support feline-specific movement and exploration abilities where unlocked:

* climbing
* wall jumping
* high jumping
* swimming
* diving
* digging
* flying
* gliding
* sprinting
* sniffing
* retrieving
* torch placement
* cat-sized passage access

Different cat builds should provide different exploration capabilities.

Create a clean capability system so future content can query things such as:

```text
CAN_FLY
CAN_DIG
CAN_SWIM
CAN_CLIMB
CAN_RETRIEVE
CAN_LIGHT
CAN_SNIFF
```

---

# R10. TWINE TEAM SYNERGY

Add an original feline team-synergy system.

When appropriate cats are nearby, the Twined player may trigger a combined attack/ability.

Examples:

* Panther + Fire build → Inferno Hunt
* Aqua + Snow build → Frozen Tide
* Nekomata + Shadow build → Twin Soul Assault

The system should use actual nearby companion states and abilities.

Do not copy another game's exact implementation.

---

# R11. AI LOGIC OVERHAUL

Rework the entire cat AI architecture.

The goal is mature companion behavior inspired by Doggy Talents / Doggy Talents Next while remaining distinctly feline.

AI modes should actually change behavior.

Audit/refine modes such as:

* Docile
* Follow
* Stay
* Wander
* Hunting
* Guard
* Aggressive
* Tactical
* Defensive
* Escort
* Fetch
* Mine
* Farm
* Fish
* Support
* Protect Owner
* Return Home
* Retreat

Every mode must be demonstrably functional.

---

# R12. AI TARGETING AND PRIORITIES

Create centralized target-selection logic rather than letting individual goals independently make contradictory decisions.

Target priority should consider:

* current AI mode
* owner threat
* ally threat
* target distance
* visibility
* enemy category
* health
* weapon range
* ammunition
* danger
* talents
* current task
* navigation validity

AI should stop goals from fighting each other.

---

# R13. FELINE AI BEHAVIOR

Cats should feel like cats, not renamed dogs.

Improve:

* stalking
* pouncing
* hunting
* playful wandering
* retreat behavior
* climbing
* cautious movement
* prey selection
* grooming
* resting
* scratching
* toy interaction
* food seeking
* bed seeking

Use feline behavior as part of gameplay identity.

---

# R14. RANGED / TaCZ AI

When TaCZ is installed:

Cats must be able to properly use supported firearms.

Verify:

* correct weapon detection
* correct mouth/jaw mounting
* proper aiming
* ammo detection
* ammo consumption
* magazine handling
* reload
* recoil
* muzzle effects
* tactical positioning
* strafing
* target range management
* empty-ammo behavior

Ammo must come from the cat's actual backpack/inventory.

No infinite ammunition.

No fake shooting.

Twined firearm use must work through the same real weapon logic.

When TaCZ is absent, Kitty Talents must still run normally.

---

# R15. EQUIPMENT / ARMOR / COSMETICS

Refine the cat inventory and equipment system so equipment is practical and visible.

Canonical equipment should include:

* weapon
* helmet
* chest
* leggings
* boots
* tool
* functional accessory
* cosmetic 1
* cosmetic 2
* cosmetic 3
* Kitty Pack/backpack

Items placed into valid equipment slots should actually equip.

Invalid items should be rejected safely.

---

# R16. CAT ARMOR VISUAL REWORK

The armor currently needs further polish.

Redesign armor so it looks like **Minecraft-style wolf armor adapted specifically for feline anatomy**.

Armor must conform to:

* back
* chest
* shoulders
* legs
* paws

Avoid:

* floating geometry
* giant boxes
* disconnected plates
* clipping
* distorted proportions

Test all supported variants including:

* Leather
* Chainmail
* Iron
* Gold
* Diamond
* Netherite
* Turtle
* other supported armor

---

# R17. COSMETICS AND CAPE

Support up to three simultaneous cosmetics.

All three must render simultaneously.

Fix the cape so it is properly attached to the cat's back rather than hanging underneath the body.

Test:

* standing
* sitting
* walking
* running
* jumping
* mounting
* Twine

Fix clipping, z-fighting, layer ordering and transform errors.

---

# R18. PANTHER / SNOW LEOPARD / NEKOMATA

### Panther

Preserve/fix the intended Panther appearance:

* black Panther texture
* blue eyes
* correct feline proportions
* complete backside
* correct tail
* correct model/texture mapping

### Snow Leopard

Fix:

* eyes
* pupils
* symmetry
* UV mapping
* face proportions
* texture distortion

### Nekomata

The second tail must be:

* physically attached to the cat's rear/tail pivot
* symmetrical with the original tail
* correctly posed while standing
* correctly posed while sitting
* correct during animation
* visible in UI and third-person rendering

Never let it float beside the existing tail.

Do not break already-working custom skins while fixing these assets.

---

# R19. BREED / SKIN / PRODUCTION SEPARATION

Maintain strict separation between:

### Breed / Feline Identity

Visual feline identity.

### Skin

Cosmetic visual override.

### Production Tier

Independent resource progression.

### Production Type

Actual resource generated.

### Gender

Separate persistent property.

A Panther may be Iron, Diamond, Netherite, etc.

Changing a skin must not change production.

Production evolution must not arbitrarily recolor the cat.

Gender must remain protected and only change through the intended gender-change item.

---

# R20. DETERMINISTIC BREEDING / PRODUCTION

Production tier evolution must remain deterministic:

```text
Standard + Standard → Coal
Coal + Coal → Copper
Copper + Copper → Iron
Iron + Iron → Gold
Gold + Gold → Lapis
Lapis + Lapis → Redstone
Redstone + Redstone → Emerald
Emerald + Emerald → Diamond
Diamond + Diamond → Netherite
```

The breeding/production systems must not use hidden random outcomes.

Breeding Catnip is the normal breeding item.

Spirit Catnip:

> If either parent receives Spirit Catnip, the offspring is guaranteed Nekomata.

Normal fish should not activate breeding.

---

# R21. PRODUCTION HABITAT

Fix and verify the entire worker assignment system.

The Habitat must store the actual worker Cat UUID.

Workflow:

```text
Right-click cat with Work Clipboard
→ Clipboard records cat UUID
→ Right-click Habitat
→ Server validates
→ Habitat assigns UUID
→ Actual cat remains in world
```

The Habitat must not consume or replace the physical cat.

Production output must be determined by the assigned cat's actual Production Tier/Type.

Eliminate hardcoded Mint-only behavior.

---

# R22. FOOD / HUNGER / WELLBEING

Use one authoritative hunger/food system.

Audit:

* hunger
* satiety
* starvation
* food values
* bowl feeding
* direct feeding
* production hunger
* healing
* rest
* beds
* scratching
* petting

The Food Bowl must contain exactly five food stacks and cats must actually navigate to it, consume actual items and restore hunger.

No hidden magical hunger regeneration.

---

# R23. DEEP FRIED CHICKEN

Preserve/add:

```text
Raw Chicken
→ Cooked Chicken
→ Deep Fried Chicken
```

Deep Fried Chicken:

* usable by players
* usable by cats
* works in Food Bowls
* nutrition = 15
* saturation = 15

Ensure recipes, texture, localization, JEI and behavior are all present.

---

# R24. GUI / HUD

Refine the interface into a compact companion RPG UI.

The world should remain visible behind menus.

Avoid giant generic Minecraft inventory screens with excessive empty space.

Use tabs such as:

* Home
* Talents
* Gear
* Style
* Stats
* Combat
* Twine

The cat should remain the visual centerpiece.

Combat/Twine HUD should be compact and show:

* HP
* Energy
* Ultimate
* abilities
* cooldowns
* name
* role
* relevant status

---

# R25. TRUST / MULTIPLAYER

Fully audit ownership and trust.

Differentiate:

* owner
* trusted player
* untrusted player

Verify permissions for:

* commands
* riding
* feeding
* equipment
* backpack
* Twine
* breeding
* Habitat assignment
* irreversible changes

Do not allow a trusted player to automatically perform every owner-only action.

---

# R26. MULTIPLAYER / LAN / DEDICATED SERVER

Perform a complete client/server architecture audit.

The mod must work correctly in:

* single player
* LAN
* dedicated server
* multiple simultaneous players

Eliminate:

* client-only imports in common/server code
* client-only classes referenced by server logic
* unsafe optional integrations
* race conditions
* packet desync
* stale sessions

Server must be authoritative for all important gameplay state.

---

# R27. NETWORK SECURITY

Audit every custom packet.

Every packet should follow:

```text
Decode
→ Validate
→ Authenticate
→ Authorize
→ Apply
→ Synchronize
```

Never trust the client for:

* damage
* Energy
* talent points
* inventory
* equipment
* breeding
* gender
* production
* ownership
* target selection
* Twine state

Test malicious/invalid packets.

---

# R28. PERSISTENCE

Verify all important cat data survives:

* save/load
* relogging
* chunk unload
* server restart
* dimension change
* incapacitation
* revival
* Twine
* equipment changes

Preserve older NBT formats and add migration where necessary.

Never silently reset existing cats.

---

# R29. PERFORMANCE / LAG SWEEP

Perform a whole-codebase performance audit.

Look for:

* expensive per-tick scans
* excessive pathfinding
* repeated NBT access
* excessive allocation
* redundant calculations
* packet spam
* repeated model creation
* particle spam
* event/listener leaks
* unbounded caches
* unnecessary synchronization
* expensive production simulation

Optimize both:

### Server

TPS/MSPT/memory/entity tick cost.

### Client

FPS/rendering/model/VFX/UI cost.

Production systems should use efficient simulation rather than forcing hundreds of fully active AI entities to tick unnecessarily.

---

# R30. PERFORMANCE STRESS TEST

Stress-test with at least:

* 10 cats
* 50 cats
* 100 cats
* large numbers of production workers
* multiple active combat cats
* multiple players
* many entities in the same area

Measure and investigate severe TPS/FPS degradation.

Do not declare performance good without measurement.

---

# R31. ASSET / GRAPHICS PASS

Perform a complete asset sweep for:

* cat textures
* skins
* armor
* weapons
* tools
* collars
* cosmetics
* GUI icons
* particles
* models
* animations

Fix:

* missing textures
* placeholder assets
* black/purple textures
* broken UVs
* invisible geometry
* clipping
* z-fighting
* incorrect transforms
* bad scaling

Existing working skins should be preserved unless testing demonstrates a real defect.

---

# R32. SFX / VFX / ANIMATION PASS

Improve overall game feel.

Add/refine original sounds and visuals for:

* attacks
* claws
* bite
* Pounce
* dodge
* Perfect Dodge
* abilities
* Ultimates
* status effects
* Twine start/end
* weapon fire
* reload
* eating
* grooming
* scratching
* petting
* breeding
* production
* revival
* victory/defeat

Animations should cover:

* idle
* walk
* run
* sprint
* attack
* Pounce
* dodge
* jump
* landing
* hurt
* incapacitated
* revive
* sleep
* eat
* scratch
* groom
* fetch
* fish
* mine
* fly
* swim
* mount
* ability cast
* Ultimate
* Twine transition

---

# R33. OPTIONAL MOD COMPATIBILITY

Audit integrations with:

* TaCZ
* Apotheosis
* AttributesLib
* Embeddium
* Oculus
* FirstPersonModel
* JEI
* Xaero
* other relevant installed mods

Optional integrations must soft-fail.

Kitty Talents must not crash because an optional compatibility mod is missing.

Third-party malformed mixins or broken external compatibility layers should be isolated and clearly distinguished from Kitty Talents failures.

---

# R34. FULL FEATURE AUDIT

Create/maintain:

```text
/kittytalents audit
```

Audit every:

* item
* block
* entity
* AI goal
* talent
* ability
* packet
* menu
* screen
* renderer
* skin
* production type
* production tier
* breeding rule
* command
* config option
* compatibility integration

Each feature must report:

```text
PASS
PARTIAL
FAIL
NOT TESTED
```

A class existing does NOT qualify as PASS.

---

# R35. TEST WORLD / IN-GAME VERIFICATION

Use a dedicated test environment/world to verify the systems in actual Minecraft.

Test:

* combat
* AI
* Twine
* abilities
* equipment
* armor
* cosmetics
* food
* bowls
* beds
* production
* breeding
* Nekomata
* mounting
* navigation
* traversal
* rendering
* multiplayer

Do not rely solely on Gradle compilation.

A successful build only proves that the code compiled.

---

# R36. REGRESSION TESTING

All existing working Kitty Talents systems must be regression-tested after the overhaul.

At minimum verify:

* taming
* naming
* collars
* skins
* talents
* leveling
* XP
* hunger
* food
* Food Bowl
* beds
* revival
* mounting
* backpack
* weapons
* armor
* tools
* accessories
* cosmetics
* whistle
* trust
* statistics
* breeding
* production
* Habitat
* Nekomata
* Twine
* TaCZ
* Apotheosis compatibility

---

# R37. SAVE / LOAD EXTREME TEST

Create a heavily progressed test cat containing:

* high level
* talents
* active abilities
* production tier
* Panther/Snow Leopard/custom skin
* gender
* equipment
* weapon
* tool
* backpack
* cosmetics
* trust
* Nekomata state
* statistics
* hunger

Save, restart, reload and verify that no data is lost or mutated incorrectly.

---

# R38. MULTIPLAYER EXPLOIT TESTING

Attempt to break:

* ownership
* trust
* equipment
* inventories
* talent points
* breeding
* production
* Habitat assignments
* Twine
* packets
* persistence

Test invalid:

* UUIDs
* entity IDs
* item counts
* ranks
* tiers
* values
* packets
* NBT

Everything must fail safely.

---

# R39. TEAMWORK EXECUTION

Split work between agents by specialty but maintain one shared architecture.

Suggested roles:

### Agent 1

Twine, camera, movement, control synchronization.

### Agent 2

Combat, abilities, Energy, status effects.

### Agent 3

AI, navigation, command modes.

### Agent 4

Performance, dedicated server, LAN, networking.

### Agent 5

Rendering, armor, weapons, Nekomata, cat models.

### Agent 6

Textures, SFX, VFX, animations.

### Agent 7

GUI/HUD/UX.

### Agent 8

Food, breeding, production, Habitat.

### Agent 9

Persistence, security, migration, regression.

### Agent 10

Integration testing and final verification.

Agents must inspect existing code before modifying shared systems.

Do not allow parallel agents to overwrite each other's architecture blindly.

---

# R40. DEFINITION OF DONE

A feature is considered complete only when it is:

```text
Implemented
+
Reachable
+
Actually Functional
+
Server Authoritative
+
Persistent
+
Rendered Correctly
+
Multiplayer Safe
+
Performance Tested
+
Regression Tested
+
Verified In-Game
```

Anything else is:

**PARTIAL**

not complete.

---

# ACCEPTANCE CRITERIA

## Twine

* [ ] Smooth camera and mouse control
* [ ] Reliable movement
* [ ] Combat works
* [ ] Abilities works
* [ ] Energy works
* [ ] Dodge works
* [ ] Pounce works
* [ ] Traversal abilities work
* [ ] Safe Untwine
* [ ] Multiplayer-safe
* [ ] No camera/body corruption

## Combat

* [ ] Real-time combat
* [ ] Combos
* [ ] Criticals
* [ ] Dodge/Perfect Dodge
* [ ] Elements
* [ ] Status effects
* [ ] Active abilities
* [ ] Ultimate
* [ ] Distinct builds
* [ ] Correct AI/Twine parity

## AI

* [ ] All modes produce real behavior
* [ ] Intelligent target selection
* [ ] Navigation is reliable
* [ ] Feline behavior feels natural
* [ ] Ranged combat works
* [ ] Tactical behavior works
* [ ] No AI goal conflicts

## Multiplayer

* [ ] Single player
* [ ] LAN
* [ ] Dedicated server
* [ ] Ownership works
* [ ] Trust works
* [ ] Twine locking works
* [ ] No desync
* [ ] No client-only server crashes

## Performance

* [ ] TPS measured
* [ ] MSPT measured
* [ ] Memory checked
* [ ] AI optimized
* [ ] Production optimized
* [ ] Rendering optimized
* [ ] Packet traffic audited

## Equipment

* [ ] Weapon
* [ ] Armor
* [ ] Tool
* [ ] Accessory
* [ ] 3 simultaneous cosmetics
* [ ] Correct rendering
* [ ] Correct gameplay effects

## Breeding / Production

* [ ] Deterministic tier evolution
* [ ] Breed and production remain independent
* [ ] Spirit Catnip guarantees Nekomata
* [ ] Normal fish do not breed
* [ ] Habitat worker assignment works
* [ ] Production output is correct
* [ ] No Mint-only behavior

## Visuals

* [ ] Panther fixed
* [ ] Snow Leopard eyes fixed
* [ ] Nekomata tails attached and symmetrical
* [ ] Sitting Nekomata correct
* [ ] Armor redesigned
* [ ] Cape correctly positioned
* [ ] Working skins preserved
* [ ] No missing textures/models

## Food / Companion Systems

* [ ] Food Bowl actually consumes food
* [ ] Five-stack capacity
* [ ] Hunger/satiety correct
* [ ] Deep Fried Chicken works
* [ ] Beds work
* [ ] Revival works
* [ ] Petting/grooming works
* [ ] Whistle commands work

---

# FINAL REPORTING RULE

Do not report success just because compilation succeeded.

For every major milestone report:

```text
Implementation: PASS / PARTIAL / FAIL
In-Game Test: PASS / FAIL
Multiplayer: PASS / FAIL
Persistence: PASS / FAIL
Performance: PASS / FAIL
Rendering: PASS / FAIL

Known Issues:
...
```

Do not hide unresolved bugs behind "mostly working."

Do not remove a feature simply because it is difficult to repair unless it is demonstrably unsafe or impossible to maintain.

---

# FINAL GOAL

Kitty Talents 3.0 should feel like:

> **A complete feline creature RPG inside Minecraft.**

The player should be able to raise a cat, build it, equip it, teach it abilities, specialize it, command it, fight with it, explore with it, breed it, use it for production, and finally:

> **Twine into it and actually become the creature.**

The final experience should feel inspired by the *design strengths* of Pokémon, Aniimo, Doggy Talents and Doggy Talents Next while remaining unmistakably original to **Kitty Talents by The Fortunate Fox Studios**.

**Do not ship a collection of unfinished systems.**

**Finish the systems. Connect the systems. Test the systems. Optimize the systems. Polish the systems. Then verify them in actual Minecraft.**


## 2026-10-07T03:19:20Z

URGENT FEEDBACK FOR AGENT 1 (Twine Specialist) from the user:
"the twine cat mode still moves the player head and bugs out the cats camera"

Agent 1 is currently overhauling R1. Ensure Agent 1 is explicitly instructed to intercept and disable the underlying player's head rotation/pitch/yaw while Twined, and fix the camera snapping/bugging out. The player's actual human body should not be moving its head while the player is piloting the cat, and the cat's camera must be completely decoupled from the human's physical body rotation.


## 2026-10-07T03:19:41Z

FEEDBACK FOR AGENT 6 (Models, Rendering & Cosmetics Specialist) from the user:
"give the armor better looking goggles on the helmet as well"

Agent 6 is currently remodeling the cat armors (R16). Ensure Agent 6 explicitly updates the armor textures/models to feature prominent, high-quality "goggles" on the cat's helmet piece as part of the visual polish pass.


## 2026-10-07T03:20:10Z

FEEDBACK FOR ORCHESTRATOR / AGENT 6:
The user has requested to "add leather and dyeable armor".
Ensure that Leather Cat Armor is implemented, and that the armor is fully dyeable using standard Minecraft dye mechanics. The rendering agent (Agent 6) must support colored tinting for the leather armor layers.


## 2026-10-07T03:20:23Z

CLARIFICATION ON PREVIOUS FEEDBACK FOR AGENT 6:
The user specifically requested "leather accents btw" regarding the armor designs. Please ensure that the cat armor models/textures feature dyeable leather accents (e.g., straps, padding underneath the plating) across the armor sets, or that the leather armor specifically has nice accents.


## 2026-10-07T03:21:33Z

FEEDBACK FOR ORCHESTRATOR / AGENT 5 (Genetics & Production Specialist):
The user has requested to "add craftable items to tier up cats". 
While maintaining the deterministic breeding paths, please also implement specific craftable consumable items that can be fed to a cat to directly upgrade its Production Tier (e.g., from Standard to Coal, Coal to Iron, etc.).


## 2026-10-07T03:22:37Z

FEEDBACK FOR ORCHESTRATOR:
The user has requested a brand new gameplay feature for the 3.0 overhaul: 
"add a special night event called neko night where enemies are stronger and theres a chance nekomata cats and tierd cats spawn"
Please instruct the relevant agents (likely Agent 2 for Combat scaling and Agent 3 for world spawning/events) to implement the "Neko Night" event. During this random or triggered night event, hostile mob difficulty/stats should be buffed, and wild cats should have a chance to spawn as Nekomata or higher Production Tiers!


## 2026-10-07T03:22:56Z

FEEDBACK FOR ORCHESTRATOR / AGENT 3:
The user has requested to "add talented cats as a natural spawn in jungle and villages".
Please ensure that Talented Cats are added to the biome/structure spawning registries so they spawn naturally in Jungle biomes and Village structures in the wild.


## 2026-10-07T03:27:21Z

FEEDBACK FOR AGENT 3 (AI & Companion Behavior):
The user has requested a specific behavior for the TaCZ empty-ammo fallback: "have them switch from gun to sword from their inventroy if out of ammo or just remove gun".
Please ensure that when a cat runs out of ammo for its equipped firearm, its AI explicitly searches its internal inventory/Kitty Pack for a melee weapon (like a sword) and automatically swaps it into the main hand. If no melee weapon is available, the cat should unequip/remove the gun from its mouth/hand to use its standard claw/bite melee attacks, rather than holding an empty gun while fighting in melee.


## 2026-10-07T03:28:37Z

FEEDBACK FOR ORCHESTRATOR / INTEGRATION AGENT:
The user has requested to "update the kitty talents compat mod as well".
The source code for the compatibility mod is located at `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalentsCompat`. Please ensure that your Integration/Test agent (e.g., Agent 8 in Batch 3) also audits and updates the `KittyTalentsCompat` project so that it successfully compiles and runs against all the massive 3.0 API changes and new events introduced in the main KittyTalents mod.


## 2026-10-07T03:29:52Z

FEEDBACK FOR ORCHESTRATOR:
The user has requested to "build the 3.0 jar before you do batch 3".
Please ensure that after Batch 1 and Batch 2 are completed (and before Batch 3 / Integration begins), you execute a full `gradlew build` on the main KittyTalents codebase to generate an intermediate "KittyTalents 3.0" JAR. This will allow the base mod to be packaged and ready before the final integration/compat tasks in Batch 3.


## 2026-10-07T03:32:35Z

FEEDBACK FOR ORCHESTRATOR:
The user has requested to "build a jar after batch 1 as well".
Please execute a full `./gradlew build` to produce a compiled test JAR immediately upon the completion of Batch 1 (before starting Batch 2). This means you will do a build after Batch 1, and another build after Batch 2 (before Batch 3).


## 2026-10-07T03:45:30Z

URGENT FEEDBACK FOR ORCHESTRATOR:
The user has requested to "pause". 
Please PAUSE all operations immediately. Finish generating the current Batch 1 JAR artifact, but DO NOT dispatch Batch 2 or proceed further. Suspend the pipeline and await further explicit instructions from the user before resuming any work.


## 2026-10-07T18:13:51Z

# Teamwork Project Prompt — Kitty Talents 3.0 (Batch 2)

> **Status:** Launched
> **Goal:** Execute Batch 2 of the Kitty Talents 3.0 overhaul (Network, Performance, GUI/HUD, VFX/SFX, Persistence & Security).
> **Requested team:** Full Team (8 specialized agents orchestrated systematically)
> **Integrity mode:** development
> **Working directory:** C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents

---

# PROJECT MISSION — BATCH 2 OVERVIEW

Building upon the successful Batch 1 core systems (Twine camera/controls, Emergent Combat/Energy, AI modes & TaCZ weapon swapping, Deterministic Treats & Habitat, and Feline Armor/Models), **Batch 2** executes the infrastructure, UI, and audio-visual polish pass for **Kitty Talents 3.0**.

Do NOT break or rewrite working Batch 1 systems. Maintain the fixed 8-agent architecture.

---

## Requirements

### R1. Multiplayer, Dedicated Server & LAN Architecture (R26, R27)
- Perform a whole-codebase client/server separation audit.
- Ensure 100% of client-only classes, rendering code, and sounds are guarded with `@OnlyIn(Dist.CLIENT)` or client proxies so dedicated servers never crash with `ClassNotFoundException` or `NoClassDefFoundError`.
- Audit every custom packet in `PacketHandler` (`Decode -> Validate -> Authenticate -> Authorize -> Apply -> Synchronize`).
- Server must be authoritative for HP, Energy, talent points, inventory, equipment, production, and Twine state.

### R2. Performance, Lag Sweep & Stress Hardening (R29, R30)
- Audit per-tick scans, entity goals, and pathfinding calls. Throttle distant cat AI tasks and ensure sleeping/sitting cats consume near-zero tick time.
- Eliminate memory leaks, repeated NBT lookups, and particle spam.
- Implement efficient Habitat worker simulation so working cats do not overload server TPS.

### R3. Companion RPG GUI & Twine HUD Overhaul (R24)
- Redesign the cat interaction screen into a compact, modern companion RPG interface where the world remains visible in the background.
- Include organized tabs: **Home**, **Talents**, **Gear**, **Style**, **Stats**, **Combat**, and **Twine**.
- Display an unobtrusive Twine & Combat HUD showing real-time Cat HP, Feline Energy bar, Ultimate meter, active abilities with cooldown timers, role tag, and active status effect badges.

### R4. VFX, SFX & Animation Polish Pass (R31, R32)
- Register high-quality audio events for cat claws, bite, Pounce, dodge/perfect dodge, ability casts, Twine enter/exit, and purr/grooming.
- Add visual particle effects for elemental statuses (Burn, Frost, Shock, Poison, Bleed) and ability impacts.
- Ensure all cosmetic items, collars, and armors render without missing texture checkers (`assets/kittytalents/textures`).

### R5. Persistence, NBT Migration & Trust Security (R25, R28)
- Audit all NBT read/write methods (`addAdditionalSaveData` / `readAdditionalSaveData`) to guarantee all stats, roles, abilities, equipment, and production data persist safely across dimension travel, world reloads, and server restarts.
- Enforce strict multiplayer trust permissions: Owner vs. Trusted Friend vs. Stranger (strangers cannot untame, steal equipment, or Twine another player's cat).

---

## Acceptance Criteria

### Server & Multiplayer Verification
- [ ] `./gradlew.bat build` succeeds cleanly with zero compile errors.
- [ ] Dedicated server environment check: No client-side classes loaded on dedicated server startup.
- [ ] All network packets reject unauthorized or forged player actions on the server.

### UI & HUD Verification
- [ ] Companion GUI opens cleanly with responsive navigation across all 7 tabs (Home, Talents, Gear, Style, Stats, Combat, Twine).
- [ ] Twine HUD displays live cat health, energy, ability cooldowns, and status indicators without obstructing the player's view.

### Audio & Visuals Verification
- [ ] All sound events register properly in `sounds.json` and play without console errors.
- [ ] Zero missing texture warnings in the client log for all registered items, abilities, and armor pieces.

### Persistence Verification
- [ ] A fully geared, leveled cat retains 100% of its talents, active skills, inventory, and role after world save & reload.


## 2026-10-07T18:27:34Z

User Directive from Parent:
Please instruct the orchestrator and active agents (Agent 4, Agent 7, and subsequent agents) to reference open-source Java resources for mob control and better pet/companion AI (e.g. DoggyTalents/DoggyTalentsNext companion behaviors, standard Forge/Minecraft vanilla companion control algorithms) and utilize open-source Minecraft sound effect references, procedural sound generator patterns, and particle effect designs, including established Java tutorials/implementations. Ensure high code quality, clean companion behavior feel, and authentic audio-visual polish.


## 2026-10-07T18:27:52Z

User Directive Update:
Also reference GitHub open-source repositories (e.g., publicly available Forge companion mods like DoggyTalentsNext, Domestication Innovation, custom HUD/GUI repositories, and particle/sound effect implementations on GitHub) for established architectural patterns, robust companion control logic, and authentic audio-visual design.


## 2026-10-07T18:51:41Z

User Visual Update Implemented:
TalentedCatArmorModel and armor textures (Leather, Iron, Golden, Diamond, Netherite, Chainmail, Turtle) have been updated to match the user's reference image:
- Clean skull cap (removed bulky goggles) conforming cleanly to feline skull/ears.
- Armadillo/wolf scute style contoured back carapace barding and flank guards that seamlessly follow the feline body slope while sitting and standing.
- Foreleg and hindleg armor cuffs/bands wrapping around the legs.
- Tail armor sleeve added to tail1 and lower tail2 with the tail tip exposed.
The changes have been compiled and verified into the build pipeline.


## 2026-10-07T19:04:17Z

Server restart completed. Please resume monitoring and execution of Batch 2 (check status of Orchestrator 3 and active agents, restart crons/tasks as needed, and continue verification towards victory claim).
