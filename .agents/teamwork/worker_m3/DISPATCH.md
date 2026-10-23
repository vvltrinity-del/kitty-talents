# DISPATCH — worker_m3

- **Agent**: worker_m3
- **Role**: teamwork_preview_worker (Twine & Combat Worker)
- **Assigned Milestone**: Milestone 3 (M3): Twine System Debugging & Real-Time Controls
- **Working Directory**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\worker_m3`
- **Project Root**: `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents`
- **Parent Conversation ID**: `a549636f-050f-4476-a58e-0c527d96daeb`

---

## 🔒 Mandatory References
Read these files before writing code:
1. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\ORIGINAL_REQUEST.md`
2. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\PROJECT.md`
3. `C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\.agents\teamwork\survey_explorer_1\handoff.md`

---

## ⚠️ MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

---

## Exclusive Write Scope
You have exclusive write access to:
- `src/main/java/com/kittytalents/client/ClientTwineHandler.java`
- `src/main/java/com/kittytalents/entity/TwineManager.java`
- `src/main/java/com/kittytalents/network/PacketTwineInput.java`
- `src/main/java/com/kittytalents/combat/` (`FelineCombatHandler.java`, `DodgeHandler.java`, `PounceAction.java`, etc.)
- Any new Twine/combat packet classes needed in `com.kittytalents.network`

DO NOT modify files owned by M1, M2, M4, M5, M6, or M7.

---

## Assigned Tasks (Features 12–23)

### Task 1: Twine Camera Smoothing & Alignment (Features 12, 13)
- In `ClientTwineHandler.java` / `ComputeCameraAngles`:
  - Interpolate yaw and pitch using `Mth.rotLerp(partialTick, cat.yRotO, cat.getYRot())` and `Mth.rotLerp(partialTick, cat.xRotO, cat.getXRot())` to eliminate 20Hz micro-jitter.
  - Fix front-camera view: When in third-person front view (`cameraType.isMirrored()`), apply the 180° rotation inversion so the camera faces the cat properly.

### Task 2: Network Echo Suppression & Physics Jitter Elimination (Feature 14)
- When the twined player sends movement packets from the client, suppress the server echoing back position/rotation corrections to the controlling player that cause jitter/snapping.
- Ensure the controlling client is the authority for feline client-side movement and prediction.

### Task 3: Dormant Player Body Pose & Safety (Feature 15)
- In `TwineManager.java`:
  - When twining, put the player body in a safe, dormant, damage-resistant pose (e.g. meditating/sleeping pose, high resistance, invulnerable or shielded from mob aggro).
  - Stop player physics simulation from competing with the cat's physics.
  - When untwining, cleanly restore player control without falling through the floor or teleporting into suffocation blocks.

### Task 4: Fluid Swimming & Step-Height Assist (Features 16, 19)
- In `ClientTwineHandler.java` / `TwineManager.java`:
  - Fluid swimming: When cat is in water or lava, allow the jump key to ascend/swim up continuously (do NOT require `onGround()`).
  - Step-height: Grant a temporary 1.0 block step-height while twined (`cat.setMaxUpStep(1.0F)`) so the player can smoothly traverse 1-block terrain steps without constant jumping. Restore default step-height upon untwining.

### Task 5: Sprint & Crouch Rebinding & Mount Conflict Fix (Features 18, 20)
- In `ClientTwineHandler.java` & `TalentedCat.java`:
  - Connect the sprint key to trigger feline sprinting (+40% speed, increased FOV).
  - Rebind sneak: Sneak key triggers feline crouching/stalking (reduced hitbox, stealth, silent footsteps) instead of immediately untwining!
  - Untwine key: Assign untwine to a dedicated key or hold sneak / double-tap sneak / whistle, preventing accidental premature untwining.
  - Mount vs Sit conflict: Empty-hand right-click on the cat strictly toggles sit/stand. Mounting requires sneak + right-click or specific riding interaction.

### Task 6: Feline 4-Way Dodge & Perfect Dodge (Features 21, 22)
- Implement `DodgeHandler.java` in `com.kittytalents.combat`:
  - 4-way dodging: Double-tap or dodge key + Direction (W/A/S/D) initiates rapid feline dart in that direction with brief invulnerability frames (i-frames: 8-10 ticks) and Energy cost (e.g., 15 Energy).
  - Perfect Dodge: If a dodge is executed within 4 ticks before an incoming hostile attack connects:
    - Negates 100% of the damage.
    - Applies brief slowness (Slow II for 30 ticks) to the attacker.
    - Refunds 100% of the Energy cost.
    - Plays a crisp dodge sound and subtle visual effect.

### Task 7: Pounce 2.0 Real-Time Action (Feature 23)
- Implement `PounceAction.java` in `com.kittytalents.combat`:
  - Multi-stage real-time action:
    1. Aim / Target trajectory: Calculate trajectory arc based on cat look vector.
    2. Crouch & Charge: Hold attack/pounce key to charge power.
    3. Launch: Cat leaps in high-speed parabolic arc with client prediction.
    4. Impact: On collision with entity or ground, deal kinetic/pounce damage, knockback, and brief knockdown/stun to target.

---

## Verification Requirements
1. Run `.\gradlew.bat compileJava --no-daemon` to ensure 0 errors.
2. Verify all class files compile and interface contracts in `PROJECT.md` are satisfied.
3. Document all implementation details, code diffs, and verification results in `handoff.md`.
4. Send a completion message to parent (`a549636f-050f-4476-a58e-0c527d96daeb`).
