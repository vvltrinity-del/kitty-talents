# 🐱 Kitty Talents (Minecraft 1.20.1 Forge)

A complete, production-ready companion overhaul mod for Minecraft 1.20.1 Forge, expanding vanilla cats into deep, trainable RPG companions with leveling, skill trees, physical fox-style pounces, mounts, controlled flight, autonomous mining, full equipment armor/weapon systems, Dire Treat endgame progression, live 3D GUI, and feline superpowers!

---

## 🌟 Overhauled Systems & Features

### 1. 🎓 True RPG Progression & Skill Points Rebalance
- **Balanced Points Economy**: Cats level from 1 to 100, earning skill points to invest in 16 specialized talents.
- **Rank Scaling Cost**: Higher ranks cost proportionally more points (Rank 1 costs 1 point, Rank 2 costs 2 points, Rank 3 costs 3 points, etc.). Maxing a 5-rank talent costs 15 points.
- **Meaningful Builds**: With 16 talents requiring over 200 total points to max, a level 100 cat can only fully master ~6–7 talents, creating distinct companion builds:
  - *Battle Panther*: Fox Pounce, Cat Armory, Feline Armor, Nine Lives, Agility
  - *Sky Feline Mount*: Cat Mount, Sky Feline Flight, Lucky Purr, Pillow Cat
  - *Cave Delver*: Mole Cat Mining, Kitty Pack, Whisker Radar, Night Prowler
  - *Frenzied Hunter*: Frenzy Focus, Mouser, Fisher Cat, Pounce
- **Catnip Respec Elixir**: Craftable item (`kittytalents:catnip_respec_elixir`) or clicking **Respec** in the GUI refunds 100% of spent skill points so players can freely experiment with builds.

### 2. ⚔️ Fully Functional AI Modes with Real Combat Priorities
Each mode directly drives navigation, attack priorities, and target selection:
1. **Docile (Sitting)**: Cat sits peacefully, cancels all movement and targeting, and never attacks unless hurt.
2. **Follow**: Follows the owner closely and aggressively defends the owner when attacked or when the owner strikes an enemy.
3. **Wander**: Freely explores and strolls within a comfortable area without following the player; only fights back if attacked.
4. **Hunting**: Autonomously searches for and hunts valid prey: pests (silverfish, endermites, bats, cave spiders, rabbits) and small hostiles.
5. **Guard**: Defends a 16-block post/perimeter, automatically charging and attacking any monster that enters the zone.
6. **Aggressive**: Relentlessly scours the area up to 24 blocks away, hunting down and destroying all hostile monsters!

### 3. 🦊 Fox-Like Physical Pounce Attack (`CatFoxPounceGoal`)
- Real physical trajectory, not just an attack animation:
  1. **Crouch & Lock-On**: Cat crouches low, wiggles rear, stalks the target for 12 ticks, and hisses.
  2. **High Aerial Launch**: Launches high into the air with real physical arc trajectory (Y impulse + directed XZ velocity).
  3. **Dive Slam**: Rotates downward and slams onto the target's head.
  4. **Impact Damage**: Deals heavy critical impact slam damage, creates sweep particles, inflicts slowness, and triggers an impact shockwave.
  5. **Melee Follow-up**: Transitions seamlessly into clawing and sword slashes.

### 4. 🐎 Cat Mount (Feline Steed)
- Progressive scaling with talent rank:
  - Rank 0: 1.00x (Normal cat)
  - Rank 1: 1.25x
  - Rank 2: 1.50x
  - Rank 3: 1.80x
  - Rank 4: 2.20x
  - Rank 5: 2.60x (Giant battle feline / tiger size!)
- Right-click with empty hand or saddle to mount and steer with WASD controls, boosted speed, and scaled jump height.
- Proper dynamic hitbox scaling prevents clipping and suffocation.

### 5. ⚔️ Cat Armory (Weapons)
- Dedicated weapon slot supporting tiered progression:
  - Rank 1: Wooden & Stone swords
  - Rank 2: Iron & Golden swords
  - Rank 3: Diamond swords
  - Rank 4: Netherite swords
  - Rank 5: Master Blade (bonus attack speed + fire slash)
- Sword is visibly held in the cat's mouth in 3D!
- Weapon damage directly adds to melee attacks, and durability is properly consumed and preserved.

### 6. 🛡️ Feline Armor (Equipment System)
- Equips Helmet, Chestplate, Leggings, and Boots through the dedicated Equipment GUI tab!
- Provides actual Armor points and Armor Toughness, greatly increasing survivability.
- Rank-gated equipment prevents equipping diamond/netherite armor before achieving the required talent rank.

### 7. 👑 End-Game Tier: Dire Treat Awakening
- **Dire Treat Gating**: Every end-game talent (**Sky Feline**, **Mole Cat**, **Frenzy Focus**) is strictly locked until the cat reaches Level 60+ and consumes a **Dire Treat** to achieve **DIRE AWAKENING**!
- Displayed with a clear `[LOCKED - REQUIRES: DIRE TREAT]` banner in the GUI.
- Server-validated against any command, packet, or NBT bypass exploits.

### 8. 🪽 End-Game: Sky Feline (Flight)
- Cat sprouts glowing celestial Elytra wings!
- When mounted: Pitch view up while moving forward or press Jump to ascend and soar through the air; looking level or down smoothly glides.
- Cat is completely immune to fall damage and suffocation while flying.
- When dismounted: Flies alongside the player over ravines and oceans, and takes flight to attack aerial mobs (Phantoms, Blazes, Ghasts).

### 9. ⛏️ End-Game: Mole Cat (Mining)
- When underground or in caves, cat paws at the ground and detects ore deposits!
- Breaks stone and ores with block particles and mining sounds.
- Ranks unlock: Coal/Copper -> Iron/Lapis -> Gold/Redstone -> Diamond/Emerald -> Ancient Debris!
- Automatically deposits mined ores straight into the **Kitty Pack** inventory (or at feet if full).

### 10. 🥛 Enchanted Frenzy Milk Bucket & Cow System
- Craft a **Catnip-Infused Bucket** (`kittytalents:catnip_bucket`) and right-click any Cow to milk **Bucket of Frenzy Cat Milk** (`kittytalents:catnip_milk_bucket`).
- Feed it to your cat to trigger **SUPER FELINE FRENZY**:
  - +20 Absorption Hearts
  - +15 Attack Damage
  - Speed III (hyper zoomies!)
  - Regeneration II, Resistance II, Jump Boost III
  - Glowing soul flame particle aura
- Tracked by an active duration timer and cooldown to prevent infinite spam.

### 11. 🎨 Colorable Collars
- Right-click cat with **any of the 16 Minecraft dyes** to change its collar color instantly.
- Preserves all stats, equipment, and talents; displayed in the live 3D GUI.

### 12. 🏷️ Naming Without Name Tags
- Naming text box built directly into the Kitty Talents GUI with validation (1–32 characters).
- Enter any name and click **Rename**; persists across reloads, dimension travel, and deaths.

### 13. 🐱 16 Custom Skins with 3D Preview
- Cycle between 11 vanilla coats and 5 custom high-tier coats using `◀` and `▶`:
  1. Tiger (Orange/black with vivid emerald eyes)
  2. Snow Leopard / Cheetah
  3. Void Ender Cat (Cosmic purple with glowing eyes)
  4. Golden Maneki-Neko (Lucky golden feline)
  5. Ghost Phantom Cat (Ethereal spectral cyan)
- Cosmetic-only; never resets attributes or progress.

### 14. 🖥️ Polished RPG Companion Interface
- **Live 3D Cat Model Preview**: Renders the cat in 3D right in the interface, tracking the player's cursor!
- **Talents Tab**: Shows rank progress bars (`[███░░] 3/5`), point costs, effects, and Dire Treat status.
- **Equipment Tab**: Equips Weapon, Helmet, Chestplate, Leggings, Boots, and quick-opens the Kitty Pack.
- **Respec Button**: One-click talent reset when holding a Catnip Respec Elixir.

---

## 📦 File Location & Installation
Built and ready to play:
```
C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\build\libs\kittytalents-1.0.0.jar
```
Drop this `.jar` into your `.minecraft/mods` folder with Forge 1.20.1 installed.
