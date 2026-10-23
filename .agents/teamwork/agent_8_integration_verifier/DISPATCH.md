# Agent 8 (Systems Integration, Persistence & Verification Specialist) Dispatch

Authoritative Request Log: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md
Project Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents
Working Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\agent_8_integration_verifier
Compatibility Mod Directory: C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalentsCompat

## Assigned Requirements:
- R28: Persistence & NBT Migration (Data integrity across chunk load, dimension change, death/revival, older NBT migrations)
- R33: Optional Mod Compatibility (TaCZ, Apotheosis, AttributesLib, Embeddium, Oculus, FirstPersonModel, JEI)
- R34: Full Feature Audit (`/kittytalents audit` command reporting PASS/PARTIAL/FAIL)
- R35: Test World / In-Game Verification
- R36: Regression Testing (Taming, naming, collars, food bowl, revival, backpack, weapons, trust, habitats)
- R37: Save/Load Extreme Test (Fully progressed test cat save/reload integrity)
- R38: Multiplayer Exploit Testing (Invalid UUIDs, packets, negative values, ownership boundaries)
- R40: Definition of Done & Final Victory Verification
- NEW DIRECTIVE: Update and verify `KittyTalentsCompat` mod (`C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalentsCompat`) against 3.0 API changes, resolve imports/signatures, and verify that both projects build cleanly.

## Mandatory Constraints:
- Read ORIGINAL_REQUEST.md before starting work.
- Do NOT rebuild the mod from scratch. Inspect existing code first, preserve everything that works, and improve/refactor only where necessary.
- DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
- Test and verify compilation using `./gradlew compileJava` in both KittyTalents and KittyTalentsCompat.
- Write a detailed `handoff.md` upon completion.
