# Kitty Talents 5.0

> **Minecraft Forge 1.20.1** · Update the server and every client together. 4.0 worlds load safely, and skill points for reworked talents are refunded automatically.

## ⚡ Performance
- **Cats no longer cost FPS.** The companion cards in the top-left corner drew their wood and leather background one tile at a time, about 70 GPU draw calls per cat per frame. They now draw in a few. Four cats went from about 0.5 ms per frame to no measurable cost; 20 cats cost about 0.3 ms.
- Following cats only recalculate their path when you move, not twice a second while you stand still.
- Searches for food bowls, Cat Trees and scratchers skip empty parts of the world instead of checking thousands of blocks.

## 🐛 Fixes
- **Cat Trees were only ever half a tree.** Placing one now builds the top (the perch), so cats can actually climb up and sit there. Break and replace old trees to get their top. Perched cats no longer spin.
- **Riding a Sky Feline off a cliff** no longer sinks you halfway into the cat.
- **Night Prowler's night vision** no longer flickers.
- **All 12 roles** are on the Combat tab (Assassin, Support, Controller, Breaker, Mobility and Hybrid were hidden).
- The Energy shown in a Habitat or Treat Oven no longer keeps growing on screen.
- Cats no longer chase **fish** or **bats**.
- Cats don't get **hungry while their owner is offline** (servers), including cats working in a Habitat.

## 🎓 Easier to learn
- A **Kitty Talents** tab in the Advancements screen walks you through one thing at a time: tame, Follow, first talent, bowl or bed, a job, a combat role, then Twine.
- Every talent description now gives real numbers ("18% crit chance at rank 1, +8% per rank").

## ⚖️ Design changes
- **Any cat can wear basic gear.** Leather to iron armor and basic swords need no talent. Diamond needs Feline Armor or Cat Armory rank 1, netherite rank 3. Each rank still adds armor or damage.
- **Kitty Tools:** +5% job speed per rank while holding a tool, and rank 1 unlocks the **Miner** job (it used to need the Dire talent Mole Cat).
- **Feline Reflexes** now parries melee hits only; **Feline Agility** dodges projectiles, and its mastery Ghost Step gives Speed II after a dodge. Points spent on either are refunded.
- Renamed: the Feline Hiss talent is **Sonic Hiss**; Pest Hunter's mastery is **Bug Bane**.
- **Deep Fried Chicken** is now a mid meal (+27 cat hunger); the new **Chicken and Rice** (Deep Fried Chicken + Steamed Rice Bowl) is the top meal (+50).
- **Condensed Ore Essence** drops from copper to netherite Habitat cats and crafts the **Resource Catalyst** and a cheaper Netherite Cat Treat. The **Super Catnip Catalyst** has a recipe (catnip, bone meal, glowstone).

## ✨ New
- **Personality in the Habitat.** A housed cat's trait matters: Shy cats work faster alone, Brave cats are foremen for their neighbours, Lazy cats are slower but never hungry, Curious cats find bonus items, Playful cats like a playmate, Greedy cats make more and eat more. Bond adds up to +10% while you're near. The Habitat screen shows the total next to the cycle count.
- **Captain cat.** At bond level 10, make one cat your Captain (★ on its Home tab). It wears a crown, spots monsters, and from a Cat Tree (or leading a patrol) rallies your other cats (+10% damage) and nearby Habitats (+5% speed).
- **Nekomata spirit fire.** Nekomata keep up to 3 blue wisps circling them and send them at enemies; a housed Nekomata keeps monsters away from its Habitat.
- **Base Pin.** Mark out your bases (several per player; rename, resize, merge or delete them from its menu). Give a cat a **guard post** (a Cat Tree post keeps it perched on top for good). Guard cats in a base without a post **patrol** it in teams of up to 3.
- **Role promotions.** A max-level cat has mastered its role and can be promoted: Hyper Carry, Crusader, Guardian of Light, Warlock, Pathfinder, Juggernaut, Phantom Blade, Commander, Archon, Siegebreaker, Windrunner and Paragon, each with a signature perk.
- **Game rules:** `kittyTalentsCatDeath` (off: a downed cat dies if not revived in time), `kittyTalentsBleedOutSeconds` (60), `kittyTalentsFollowerLimit` (4 cats can follow you at once).
- **JEI:** pages for every Habitat tier's output and every breeding outcome.
- **Farmer's Delight:** cats eat its fish slices, chicken cuts and fish meals; its rice works for Steamed Rice Bowls and Chicken and Rice (also in the Cooking Pot).
- **Safe removal:** `/kittytalents uninstall` turns your Talented Cats into vanilla cats before you remove the mod, so they don't vanish; `/kittytalents reinstall` brings them back with everything they had.

*This mod was created with AI.*
