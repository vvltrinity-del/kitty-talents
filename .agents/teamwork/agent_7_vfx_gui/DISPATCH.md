## 2026-10-07T18:17:56Z
You are Agent 7 (Assets, Audio/VFX & GUI Specialist) for Kitty Talents 3.0 (Batch 2).

Working directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_7_vfx_gui
Project directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Authoritative request: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md (Read this first, especially the requirements under `## 2026-10-07T18:13:51Z` and R24, R31, R32).
Project specification: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\PROJECT.md

Your Assigned Scope (Batch 2: Companion RPG GUI, Twine HUD, VFX/SFX & Asset Polish):
1. R24: Companion RPG GUI & Twine HUD Overhaul
   - Redesign the cat interaction screen (`CatTalentScreen` or companion screen in `client/gui/`) into a modern, compact companion RPG interface where the Minecraft world remains visible in the background.
   - Implement organized tabs: Home, Talents, Gear, Style, Stats, Combat, Twine.
   - Display an unobtrusive Twine & Combat HUD overlay in `ClientTwineHandler.java` (or dedicated HUD overlay): real-time Cat HP, Feline Energy bar, Ultimate meter, active abilities (slots 1-3) with cooldown timers, role tag, and active status effect badges.
2. R31: Asset & Texture Polish Pass
   - Check all texture files in `src/main/resources/assets/kittytalents/textures/**` (items, abilities, armor, collars, GUI icons).
   - Ensure all registered items, abilities, and armor pieces have valid textures so there are zero "Missing texture" warnings.
   - Clean up any broken references or placeholder textures.
3. R32: SFX, VFX & Animation Polish Pass
   - Register high-quality audio events in `ModSounds.java` and `src/main/resources/assets/kittytalents/sounds.json` for: cat claws/swipes, bite, Pounce launch/impact, dodge/perfect dodge, ability casts, Twine enter/exit, and purr/grooming/petting.
   - Add visual particle effects in `CatAttackEffects.java` and relevant ability handlers for elemental statuses (Burn, Frost, Shock, Poison, Bleed) and ability impacts.

Exclusive Write Ownership:
- `src/main/java/com/kittytalents/client/gui/**`
- `src/main/java/com/kittytalents/client/ClientTwineHandler.java` (HUD overlay rendering)
- `src/main/java/com/kittytalents/registry/ModSounds.java` (or sound registries)
- `src/main/resources/assets/kittytalents/sounds.json`
- `src/main/resources/assets/kittytalents/sounds/**`
- `src/main/resources/assets/kittytalents/textures/**`
- `src/main/java/com/kittytalents/combat/CatAttackEffects.java` (VFX particle effects)
Do NOT edit networking, entity NBT, or server AI goals (owned by Agent 4).


## 2026-10-07T18:28:16Z
**Context**: Batch 2 implementation (GUI, Twine HUD, VFX/SFX & Assets)
**Content**: High-priority user directive received: Reference open-source Java resources and utilize open-source Minecraft sound effect references, procedural sound generator patterns, and particle effect designs, including established Java tutorials/implementations.
**Action**: Ensure authentic audio-visual polish, registered sound events matching Minecraft sound standards, vibrant elemental particle effects, and clean, responsive RPG GUI and HUD layout.


## 2026-10-07T18:28:32Z
**Context**: Batch 2 implementation
**Content**: Additional directive: Also reference GitHub open-source repositories (e.g., publicly available Forge companion mods like DoggyTalentsNext, Domestication Innovation, custom HUD/GUI repositories, and particle/sound effect implementations on GitHub) for established architectural patterns, robust companion control logic, and authentic audio-visual design.
**Action**: Apply these established architectural patterns to your implementation.
