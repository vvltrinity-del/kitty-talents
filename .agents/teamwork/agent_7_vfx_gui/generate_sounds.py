import os
import numpy as np
import soundfile as sf

sr = 44100

def ensure_dir(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)

def write_sound(path, samples):
    ensure_dir(path)
    # Normalize peak to 0.85
    peak = np.max(np.abs(samples))
    if peak > 1e-4:
        samples = (samples / peak) * 0.85
    sf.write(path, samples.astype(np.float32), sr, format='OGG', subtype='VORBIS')
    print(f"Generated {path} ({len(samples)} samples)")

base_dir = r"C:\Users\VivianV\.gemini\antigravity\scratch\KittyTalents\src\main\resources\assets\kittytalents\sounds"

# 1. combat/cat_claw_swipe.ogg
dur = 0.16
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 1.8
noise = np.random.normal(0, 1, len(t))
# Simple bandpass filtering via moving average and diff
sweep = np.sin(2 * np.pi * (1200 + 1600 * (1.0 - t/dur)) * t)
swipe = (noise * 0.7 + sweep * 0.3) * env
write_sound(os.path.join(base_dir, "combat", "cat_claw_swipe.ogg"), swipe)

# 2. combat/cat_claw_hit.ogg
dur = 0.20
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.exp(-t * 28.0)
thump = np.sin(2 * np.pi * 180 * np.exp(-t * 20.0) * t)
click = np.random.normal(0, 1, len(t)) * np.exp(-t * 60.0)
hit = thump * 0.6 + click * 0.5
write_sound(os.path.join(base_dir, "combat", "cat_claw_hit.ogg"), hit * env)

# 3. combat/cat_bite.ogg
dur = 0.22
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
snap1 = np.exp(-np.abs(t - 0.02) * 80.0) * np.sin(2 * np.pi * 320 * t)
snap2 = np.exp(-np.abs(t - 0.06) * 90.0) * np.sin(2 * np.pi * 220 * t)
noise = np.random.normal(0, 1, len(t)) * np.exp(-t * 25.0)
bite = snap1 * 0.6 + snap2 * 0.7 + noise * 0.4
write_sound(os.path.join(base_dir, "combat", "cat_bite.ogg"), bite)

# 4. combat/cat_pounce_launch.ogg
dur = 0.28
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 1.5
freq = 160 + 540 * (t / dur) ** 2
leap = np.sin(2 * np.pi * freq * t) * 0.5 + np.random.normal(0, 0.4, len(t)) * (t / dur)
write_sound(os.path.join(base_dir, "combat", "cat_pounce_launch.ogg"), leap * env)

# 5. combat/cat_pounce_impact.ogg
dur = 0.35
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.exp(-t * 12.0)
boom = np.sin(2 * np.pi * 75 * np.exp(-t * 10.0) * t)
crunch = np.random.normal(0, 1, len(t)) * np.exp(-t * 22.0)
impact = boom * 0.7 + crunch * 0.5
write_sound(os.path.join(base_dir, "combat", "cat_pounce_impact.ogg"), impact * env)

# 6. combat/cat_dodge.ogg
dur = 0.18
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 2
dash = np.random.normal(0, 1, len(t)) * np.sin(2 * np.pi * 600 * t) * env
write_sound(os.path.join(base_dir, "combat", "cat_dodge.ogg"), dash)

# 7. combat/cat_perfect_dodge.ogg
dur = 0.65
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.exp(-t * 6.5)
c1 = np.sin(2 * np.pi * 880 * t)
c2 = np.sin(2 * np.pi * 1320 * t) * 0.6
c3 = np.sin(2 * np.pi * 1760 * t) * 0.4
c4 = np.sin(2 * np.pi * 2640 * t) * 0.2
perfect = (c1 + c2 + c3 + c4) * env
write_sound(os.path.join(base_dir, "combat", "cat_perfect_dodge.ogg"), perfect)

# 8. ability/ability_cast.ogg
dur = 0.45
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 1.2
f = 320 + 480 * (t / dur)
cast = np.sin(2 * np.pi * f * t) * 0.6 + np.sin(2 * np.pi * f * 2.0 * t) * 0.3 + np.random.normal(0, 0.2, len(t))
write_sound(os.path.join(base_dir, "ability", "ability_cast.ogg"), cast * env)

# 9. ability/ability_hiss.ogg
dur = 0.55
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = (np.sin(np.pi * (t / dur)) ** 1.3) * (1.0 + 0.3 * np.sin(2 * np.pi * 18 * t))
noise = np.random.normal(0, 1, len(t))
hiss = noise * env
write_sound(os.path.join(base_dir, "ability", "ability_hiss.ogg"), hiss)

# 10. ability/ability_purr.ogg
dur = 0.85
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = 0.5 * (1.0 - np.cos(2 * np.pi * t / dur))
purr_mod = (0.5 + 0.5 * np.sin(2 * np.pi * 26 * t)) ** 2
purr_carrier = np.sin(2 * np.pi * 95 * t) + 0.4 * np.sin(2 * np.pi * 190 * t)
purr = purr_carrier * purr_mod * env
write_sound(os.path.join(base_dir, "ability", "ability_purr.ogg"), purr)

# 11. ability/ability_elemental.ogg
dur = 0.50
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur))
crackle = np.random.normal(0, 1, len(t)) * (np.random.random(len(t)) > 0.85)
surge = np.sin(2 * np.pi * (240 + 600 * (t / dur)) * t) * 0.5
elem = (crackle * 0.6 + surge * 0.5) * env
write_sound(os.path.join(base_dir, "ability", "ability_elemental.ogg"), elem)

# 12. ability/ability_ultimate.ogg
dur = 0.95
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 1.1
chord1 = np.sin(2 * np.pi * 220 * t)
chord2 = np.sin(2 * np.pi * 277.18 * t)
chord3 = np.sin(2 * np.pi * 329.63 * t)
chord4 = np.sin(2 * np.pi * 440 * (1.0 + 0.05 * np.sin(2 * np.pi * 5 * t)) * t) * 0.8
sparkle = np.sin(2 * np.pi * (880 + 800 * (t / dur)) * t) * 0.3
ult = (chord1 + chord2 + chord3 + chord4 + sparkle) * env
write_sound(os.path.join(base_dir, "ability", "ability_ultimate.ogg"), ult)

# 13. twine/twine_enter.ogg
dur = 0.65
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 1.2
f_rise = 400 + 800 * (t / dur) ** 1.5
warp = np.sin(2 * np.pi * f_rise * t) * 0.6 + np.sin(2 * np.pi * (f_rise * 1.5) * t) * 0.4
tw_enter = warp * env
write_sound(os.path.join(base_dir, "twine", "twine_enter.ogg"), tw_enter)

# 14. twine/twine_exit.ogg
dur = 0.55
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.exp(-t * 5.0)
f_fall = 1000 - 650 * (t / dur)
tw_exit = (np.sin(2 * np.pi * f_fall * t) * 0.6 + np.random.normal(0, 0.2, len(t))) * env
write_sound(os.path.join(base_dir, "twine", "twine_exit.ogg"), tw_exit)

# 15. companion/cat_purr_deep.ogg
dur = 1.10
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = 0.5 * (1.0 - np.cos(2 * np.pi * t / dur))
purr_mod = (0.5 + 0.5 * np.sin(2 * np.pi * 23 * t)) ** 2
purr_carrier = np.sin(2 * np.pi * 78 * t) + 0.5 * np.sin(2 * np.pi * 156 * t)
deep_purr = purr_carrier * purr_mod * env
write_sound(os.path.join(base_dir, "companion", "cat_purr_deep.ogg"), deep_purr)

# 16. companion/cat_grooming.ogg
dur = 0.45
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.sin(np.pi * (t / dur)) ** 2
brush = np.random.normal(0, 1, len(t)) * (np.sin(2 * np.pi * 8 * t) ** 2) * env * 0.7
write_sound(os.path.join(base_dir, "companion", "cat_grooming.ogg"), brush)

# 17. companion/cat_petting.ogg
dur = 0.50
t = np.linspace(0, dur, int(sr * dur), endpoint=False)
env = np.exp(-t * 6.0)
# Sweet harmonic chirp / purr trill
chirp = np.sin(2 * np.pi * (520 + 350 * np.sin(np.pi * t / dur)) * t) * 0.6
purr_t = (0.5 + 0.5 * np.sin(2 * np.pi * 28 * t)) * 0.4
pet = (chirp + purr_t) * env
write_sound(os.path.join(base_dir, "companion", "cat_petting.ogg"), pet)

print("All 17 sound files successfully synthesized!")
