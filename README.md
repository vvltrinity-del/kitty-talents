# Kitty Talents 4.0 (Minecraft 1.20.1, Forge 47)

Kitty Talents turns cats into trainable companions. They level up, learn talents, wear gear, fight with their own
moves, work jobs, breed into stronger production tiers, and you can take direct control of one with **Twine**.
Kittens grow up, every cat has a personality, and the bond between you and a cat grows as you spend time together.

## Getting started

1. **Tame** a wild Talented Cat with a **Catnip Treat** (always works), Catnip (1 in 2) or raw fish (1 in 3).
   A newly tamed cat starts with enough skill points to max out any one regular talent.
2. Right-click your cat with a **Stick** or the **Laser Pointer** to open its screen.
3. Right-click with an empty hand to make it sit or stand. Sneak + empty hand to pet it.

## The cat screen

Seven tabs, in a wood-and-leather style. Switch tabs with the mouse, **Ctrl+Tab** or the keys **1-7**.

| Tab | What it does |
|---|---|
| **Home** | Portrait with level, HP, XP, Energy and the bond bar. Personality, behaviour mode and job, Sit, Kitty Pack, Whistle, TWINE. Rename and co-owners. |
| **Talents** | Every talent with rank pips, its description, its mastery perk and the TRAIN button. Respec. |
| **Gear** | Weapon, accessory, tool, four armor pieces, three cosmetics, with your inventory underneath. Drag items in and out, or shift-click to move one. |
| **Style** | Collar colour, skin (built-in and from packs), and the breeding codex. |
| **Stats** | Lifetime records: kills, damage, distance, food, blocks mined and more. Sneak + right-click with Coal to wipe them. |
| **Combat** | Role, element, the four Twine ability slots and loadout presets. |
| **Twine** | Start Twining, plus the controls and what the cat's body can do. |

Co-owners see the same screen without the owner-only controls (training, respec, renaming, managing co-owners).

## Progression

- **XP and levels:** cats earn XP from kills and level up to the configured cap (default 100), gaining skill points.
  Treats also level a cat directly.
- **51 talents** in combat, utility, mobility and support. A rank costs more points the higher it goes.
  Four talents (Nine Lives, Mouser, Fisher Cat, Pillow Cat) start at rank 1 for free.
- **Mastery:** maxing a talent unlocks a mastery perk. For example, maxed Feline Armor repairs the cat's armor like
  Mending, and maxed Alchemy Kitty moves your debuffs onto the enemy your cat hits.
- **Dire talents** (Sky Feline flight, Mole Cat mining, Frenzy Focus, Alchemy Kitty, Catnip Catastrophe) need one
  **Dire Treat** (Master Catnip Treat + Ghast Tear + Diamond) at the required level.
- **Respec** with a Catnip Respec Elixir refunds every paid rank.
- **22 abilities** for the Twine slots and AI cats, including ice, poison and arcane ones added in 4.0.

## Behaviour modes

Docile, Stay, Follow, Wander, Guard, Hunting, Aggressive, Tactical, Support, Fetch, Work and Return Home.
Each mode decides whether the cat follows you, holds a spot, and what it is allowed to attack. Every cat **stalks and
pounces** as its opening move, dodges arrows and creeper blasts, and retreats when badly hurt (except in Aggressive).
The **Kitty Whistle** commands all your cats nearby at once. Switch it between "mine only" and "mine + co-owned",
and right-click a command to pin it to the favourites row.

## Jobs and production

- **Jobs** (Work mode): Farmer, Lumberjack, Miner, Rancher, Courier, Sorter, Fisher, Collector and Gunsmith (TaCZ).
  A job only runs while the cat is clocked in.
- **Production tiers:** Standard, Coal, Copper, Iron, Gold, Lapis, Redstone, Emerald, Diamond and Netherite. Breeding two
  cats of the same tier gives the next tier. Higher tiers are tougher and hit harder.
- **Kitty Habitat:** house a cat and it produces its tier's resource over time.
- **Nekomata:** feed Spirit Catnip to a parent before breeding for a guaranteed two-tailed Nekomata kitten.

## Living cats (4.0)

- **Growing up:** kittens go through Newborn, Kitten and Junior before they are adults. Young cats are smaller and
  can't be ridden yet. Feeding speeds growth up.
- **Personality:** each cat has one trait with a bonus and a quirk: Brave, Lazy, Curious, Playful, Shy or Greedy.
  Kittens usually inherit a parent's trait.
- **Bond:** time together, petting, feeding, play and fighting side by side raise the bond (levels 1 to 10).
  Perks unlock at levels 2, 3, 5, 7 and 10, from Regeneration when you pet the cat to bonus damage near you.
- **Cat Tree:** a two-block perch. Guard and Wander cats rest on it and mark nearby monsters with Glowing.

## Care

Cats get hungry. Feed them fish by hand, or fill a **Cat Food Bowl** and they eat by themselves. They sleep on
**Cat Beds** at night and use scratchers. A cat that drops to 0 HP is **downed**, not dead. Revive it with a Golden Apple,
Milk or Warm Milk. Nine Lives can bring it back on its own, with a cooldown.

## Twine

Press **TWINE** to control your cat directly while your body sleeps safely where you left it. You move, jump, claw,
pounce and dodge as the cat, and use its four ability slots (V, G, B and R for the ultimate). Press **U** to return.
A HUD shows HP, Energy, the ultimate meter, cooldowns and status effects. Your body is always restored if the cat dies,
you die, log out, change dimension or the game crashes.

## HUD

When you're not Twining, small cards show your nearby cats that are following you, hurt or downed. Turn them off or
change how many show in `config/kittytalents-client.toml`.

## Co-owners

Add another online player as a co-owner from the Home tab, then click their chip to set a role:

- **Rider:** may ride the cat.
- **Handler:** also commands it (modes, whistle, jobs) and Twines.
- **Full:** also changes gear and style.

Only the owner can train, respec, rename or manage co-owners. A co-owner can leave on their own. Cats that aren't
yours show their owner's name on the HUD, and other players see a label over a Twined cat and its resting body.

## Packs and addons

- **Skins** come from datapacks plus a resource pack for the texture (`docs/examples/sunset_skin_pack`).
- **Balance** is tunable from a datapack: talent, ability, trait, growth and bond numbers
  (`/kittytalents tuning export` writes every default).
- **Addons** can add talents, abilities, modes, jobs and AI goals through the API (`docs/EXTENDING.md`).

## Mod compatibility

Built-in: **TaCZ** (cats use guns, ammo pouch, Gunsmith job), **Apotheosis**, **Curios**, **Jade** and **JEI**. The separate
**KittyTalentsCompat** addon adds Lootr, PlayerRevive and Sophisticated Backpacks support.

## Installing

Build with `./gradlew build` (JDK 17) and put `build/libs/kittytalents-5.0.2.jar` in your `mods` folder with
Forge 1.20.1 (47.x). Server and clients must run the same build. Worlds from 1.0, 2.0 and 3.0 load as they are.
"# kitty-talents" 
