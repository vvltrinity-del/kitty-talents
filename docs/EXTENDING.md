# Extending Kitty Talents

How to add a talent, ability, skin, mode or job, either inside this mod or from an addon. Developer setup and test
commands are in `PROJECT.md`.

| Thing | From an addon? | How |
|---|---|---|
| Talent | yes | `KittyTalentsAPI.registerTalent` |
| Ability | yes | `KittyTalentsAPI.registerAbility` |
| AI behaviour | yes | `RegisterCatGoalsEvent` |
| Reacting to gifts | yes | `CatRewardEvent` |
| Skin | resources only, inside this mod | a JSON file + texture, plus two code lines (below) |
| Mode | yes (API 2.0) | `KittyTalentsAPI.registerMode` |
| Job | yes (API 2.0) | `KittyTalentsAPI.registerJob` |

## API stability

`com.kittytalents.api` is versioned by `KittyTalentsAPI.API_VERSION` (currently **2.0.0**) using semantic versioning:

- **Major** bump: something an addon may use was removed or changed incompatibly.
- **Minor** bump: something was added (new method, event, band).
- **Patch** bump: fixes only.

Stable (covered by the version): everything in `com.kittytalents.api`, plus these types addons need to use it:

- `talent.Talent`: its constructors and getters.
- `talent.TalentCategory`.
- `talent.effect.TalentEffect` (every hook method) and `talent.effect.TalentContext` (its public fields and methods).
- `ability.CatAbility`: its constructors, `Category`, `execute`, `getAiPriority` and the getters.
- `entity.CatMode`: the built-in constants, `builder`, `TargetPolicy` and the getters.
- `work.CatJob`: the built-in constants, `builder` and the getters; `work.AbstractCatJobGoal`: its constructor and the
  three methods a job implements.
- `entity.TalentedCat`: only `getTalentLevel`, `getOwner`, `isOwnedBy`, `getCatMode`, `getCatLevel`, `getEnergy`,
  plus the vanilla `Mob` / `TamableAnimal` methods it inherits (such as `getTarget`).

**2.0.0 (mod 4.0):** `CatMode` and `CatJob` were enums and are now registries (final classes with the same constants,
`values()`, `ordinal()` and `name()`). Code that only reads them keeps working after a recompile; a `switch` over
either one has to become `if` / `==` checks.

Everything else is internal and can change in any release. If an addon needs something internal, ask for it to be
added to the API instead of reaching in.

## Adding a talent

From an addon, during `FMLCommonSetupEvent`:

```java
event.enqueueWork(() -> KittyTalentsAPI.registerTalent(
        new Talent("mymod_iron_whiskers", 5, () -> new ItemStack(Items.IRON_INGOT), false, false, TalentCategory.DEFENSE),
        new TalentEffect() {
            @Override public float modifyIncomingDamage(TalentContext ctx, DamageSource src, float amount) {
                return amount * (1.0F - 0.05F * ctx.rank);            // 5% less damage per rank
            }
            @Override public int tickInterval() { return 40; }       // onTick every 2 s
            @Override public void onTick(TalentContext ctx) {
                if (ctx.mastered() && ctx.ownerWithin(8) != null) { /* mastery perk */ }
            }
            @Override public boolean worksWhileResting() { return true; } // keep ticking while the cat sits/sleeps
        }));
```

- **Constructor:** `Talent(id, maxRank, icon, needsDireTreat, standard, category)`. A *standard* talent starts at rank 1
  for free.
- **Lang keys** go in your own `en_us.json`:
  - `talent.kittytalents.<id>` (name)
  - `talent.kittytalents.<id>.desc`
  - `talent.kittytalents.<id>.mastery`
- **Hooks** (all optional):
  - `onTick`
  - `modifyOutgoingDamage`, `onHitDealt`
  - `modifyIncomingDamage`
  - `applyAttributes` (health, armor, damage and speed totals; must be idempotent)
  - `onKill`
  - `onOwnerHurt`, `onOwnerFall`
- **Mastery** is reached at max rank. Check it with `ctx.mastered()`.
- **Ids are saved** in cat data. Never rename one after release, and prefix with your mod id.

**Inside this mod:**
1. Add a `Talents.register(new Talent(...))` line.
2. Add an effect class in the matching `talent/effect/*Talents.java` and register it in that file's `register()`.
3. Add the three lang keys to `en_us.json`.
4. Add a GameTest showing that rank 1 changes gameplay.

`LangCompletenessTest` fails if a lang key is missing.

## Adding an ability

```java
KittyTalentsAPI.registerAbility(new CatAbility("mymod_tail_whip", "Tail Whip", "Knocks back nearby foes.",
        25 /* energy */, 120 /* cooldown ticks */, FelineElement.PHYSICAL, CatAbility.Category.SKILL) {
    @Override protected boolean execute(TalentedCat cat, Player player) {
        // player is the Twined player, or null when the AI casts it. Return false if nothing happened.
        return true;
    }
    @Override public float getAiPriority(TalentedCat cat) {
        return cat.getTarget() != null && cat.distanceTo(cat.getTarget()) < 4 ? 3.0F : 0.0F;
    }
});
```

- **When the AI casts it:** an AI cat casts whichever slotted ability scores highest, but only if that score is above
  **1.0**. The default `getAiPriority` returns 1.0, so the AI never casts an ability that doesn't override it. Return
  more than 1.0 when the situation suits the ability.

- The ability appears in the Combat tab's slot cycling (as a skill or an ultimate) and in Twine.
- **Icon:** a 16×16 PNG at `assets/kittytalents/textures/ability/<id>.png`. Ship it in your addon's resources; a star
  shows if it's missing.
- **Only harm valid targets:** use `CatTargeting.canHarm(cat, entity)` so the ability never hurts players, pets or villagers.

## Adding AI behaviour

```java
MinecraftForge.EVENT_BUS.addListener((RegisterCatGoalsEvent e) -> {
    if (!e.getCat().level().isClientSide()) e.addGoal(CatGoalBand.NEEDS, new MyGoal(e.getCat()));
});
```

Bands, highest priority first: SAFETY, ORDERS, POUNCE, COMBAT, MELEE, COMMAND, WORK, NEEDS, IDLE. A goal only runs when
no higher band is using the same control flags (MOVE, LOOK, JUMP). Don't add goals in `EntityJoinLevelEvent`: it fires
on every chunk reload and stacks duplicates.

## Adding a skin

Skins are data. No code is needed, and a server can add skins with a datapack.

1. **Texture (resource pack):** a 64×32 PNG in the vanilla cat layout, e.g.
   `assets/mypack/textures/entity/cat/sunset.png`.
2. **Skin entry (datapack):** `data/mypack/kittytalents/skins/sunset.json`:
   ```json
   { "name": "skin.mypack.sunset", "texture": "mypack:textures/entity/cat/sunset.png", "order": 500,
     "breed_default_for": ["tiger"] }
   ```
   - The skin's id is the file name with its namespace: `mypack:sunset`.
   - `name` is a lang key (add it to your resource pack's lang file) or plain text.
   - `order` sorts the Style tab list. The built-in skins use 0–160.
   - `breed_default_for` is optional. Those breeds use this texture when the cat's skin is "Default".

The server sends its skin list to every player on join and after `/reload`, and it refuses skin ids it hasn't
loaded. Cats save the skin by id. If a pack is removed, its cats show their default look, and they get the skin
back when the pack returns. A pack can override a built-in skin by shipping a file at the same path
(`data/kittytalents/kittytalents/skins/tiger.json`).

There's a complete example in `docs/examples/sunset_skin_pack/`.

## Tuning numbers (datapack)

Every talent and ability number can be changed by a datapack, with no code. This covers per-rank values, cooldowns,
ranges, Energy costs, max rank and skill-point cost. Put one or more files in `data/<namespace>/kittytalents/tuning/`:

```json
{ "talents":   { "hunter_cat": { "damage_per_rank": 3.5, "max_rank": 6 } },
  "abilities": { "frost_nova": { "cooldown_ticks": 120, "radius": 6 } } }
```

- Run `/kittytalents tuning export` to write every value with its default to `kittytalents_tuning_defaults.json` in
  the world folder. Copy the values you want to change from there.
- `/kittytalents tuning changed` lists what the loaded packs changed.
- Changes apply on `/reload`, and players get the new numbers immediately. The Talents tab shows them when you hover
  over a description, and the Combat tab shows them under the ability you hover.
- Each talent has `max_rank` (1–10) and `cost_scale` (a multiplier on each rank's point cost). Each ability has
  `energy_cost` and `cooldown_ticks`.
- Values are clamped to 0..10× their default. Unknown names are logged and ignored. Files are read in name order, and
  a later file wins for the same value.
- Lowering `max_rank` caps cats that already have more ranks. Respec still refunds every rank they paid for.
- With no tuning pack, every number is the built-in value.

The same files can also tune living-cat numbers (4.0), in three more sections:

```json
{ "traits": { "brave": { "damage_mult": 1.2 }, "inheritance": { "parent_chance": 0.5 } },
  "growth": { "kitten": { "scale": 0.65, "head_scale": 1.2 }, "feeding": { "treat_fraction": 0.5 } },
  "bond":   { "levels": { "points_step": 15 }, "gain": { "petting": 3, "cooldown_ticks": 300 } } }
```

- `traits`: each personality's bonus and quirk numbers, and how often a kitten takes a parent's personality.
- `growth`: body and head size for `newborn`, `kitten` and `junior`, and how much growing time food and treats skip.
- `bond`: points per level, points per kind of moment, the cooldown between gains of one kind, and the perk numbers.

`/kittytalents tuning export` lists every name in all five sections.

There's an example in `docs/examples/tuning_pack/`. In code, declare new numbers with `Tuning.talent(...)` or
`Tuning.ability(...)` as a `static final` field, and read them with `.get()`, `.f()` or `.i()`.

## Adding a mode

A mode is data: who the cat follows, where it holds, what it may attack. From an addon (or inside this mod, where the
built-in ones are constants in `entity/CatMode`):

```java
public static CatMode SCOUT;

event.enqueueWork(() -> SCOUT = KittyTalentsAPI.registerMode(
        CatMode.builder("mymod_scout")              // id: a-z, 0-9, _  (saved as MYMOD_SCOUT)
                .followsOwner()                     // or .anchorRadius(8) to hold a spot; not both
                .followDistance(9.0, 7.0)           // start walking beyond 9 blocks, stop within 7
                .targetPolicy(CatMode.TargetPolicy.SEEK_HOSTILES)
                .retreatsWhenHurt(false)
                .build()));
```

- Defaults: does not follow, no anchor, `SELF_DEFENSE`, retreats when hurt. `.stationary()` makes the cat sit.
- `TargetPolicy`: NONE, SELF_DEFENSE, DEFEND_OWNER, AREA_DEFENSE, HUNT or SEEK_HOSTILES.
- Add the lang key `mode.kittytalents.mymod_scout`.
- For behaviour the data doesn't cover, add a goal in `RegisterCatGoalsEvent` that checks `cat.getCatMode() == SCOUT`.
- The mode joins the Home tab's mode cycle after the built-in modes. The whistle wheel only has the built-in commands.

Rules:

- Register in common setup, on **both** client and server. Mode numbers in packets come from the list order (built-in
  modes first, then addon modes by name), so both sides need the same addons.
- Modes are saved **by name**. Never rename one. A cat saved in a mode whose addon was removed loads in Follow.
- Inside this mod, a removed mode's old name goes in `CatMode.LegacyNames`.
- Add a GameTest showing the mode behaves differently from the others.

## Adding a job

A job is an entry in the `CatJob` registry plus a goal that does the work:

```java
public static CatJob BEEKEEPER;

event.enqueueWork(() -> BEEKEEPER = KittyTalentsAPI.registerJob(
        CatJob.builder("mymod_beekeeper", "Beekeeper")   // id is saved; don't change it
                .description("Collects honeycomb from hives in the work area.")
                .requiresTalent(Talents.GREEN_PAW, 2)    // optional
                .minCatLevel(10)                         // optional, default 1
                .icon(() -> new ItemStack(Items.HONEYCOMB))
                .goal(BeekeeperGoal::new)                // made once per cat
                .build()));
```

The goal extends `work/AbstractCatJobGoal` (`super(cat, BEEKEEPER)`) and implements:

- `findNextTask(scanBudget)`: the next block to work on, or null.
- `isTaskStillValid(pos)`
- `executeTask(pos)`: do one unit of work; return true if it did something.

The base class handles walking, the work area, reach, the blacklist, the pack and the "only while clocked in" check.
A plain `Goal` also works if it checks `cat.getCatJob() == BEEKEEPER && cat.isClockedIn()` itself.

- Add the lang key `job.kittytalents.mymod_beekeeper`.
- The job joins the job picker on the Home tab and the Work Clipboard, after the built-in jobs.
- Same rules as modes: register in common setup on both sides. A cat whose job's addon was removed becomes unemployed.
- Add a GameTest that assigns the job and checks one unit of work gets done.

`test/TestAddon` is a complete, minimal example of both (it is only registered on the GameTest server).

## Before you ship

```bash
python tools/gen_screen_lang.py   # if you added screen text
./gradlew build                   # JUnit, including lang completeness
bash tools/run_gametests.sh       # must print "All N required tests passed"
```
