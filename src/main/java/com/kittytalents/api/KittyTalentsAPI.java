package com.kittytalents.api;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.entity.CatMode;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import com.kittytalents.talent.effect.TalentEffect;
import com.kittytalents.talent.effect.TalentEngine;
import com.kittytalents.work.CatJob;

import javax.annotation.Nullable;

/**
 * The supported way for addons to extend Kitty Talents. Call these from your mod's FMLCommonSetupEvent
 * ({@code event.enqueueWork(...)}), on both sides. See docs/EXTENDING.md for full examples and the stability rules.
 *
 * <p>Talents and abilities are stored by id in saves and packets, so ids must stay the same between versions of your
 * addon. Prefix them with your mod id (for example {@code "mymod_tail_whip"}) to avoid clashes.</p>
 */
public final class KittyTalentsAPI {
    /**
     * Semantic version of this API (package {@code com.kittytalents.api} plus the types listed in docs/EXTENDING.md).
     * Major: something was removed or changed incompatibly. Minor: something was added. Patch: fixes only.
     */
    public static final String API_VERSION = "2.0.0";

    private KittyTalentsAPI() {}

    /**
     * Adds a talent. It appears in the Talents tab under its {@link com.kittytalents.talent.TalentCategory}, can be
     * trained, respecced and saved like a built-in one. Name, description and mastery text come from the lang keys
     * {@code talent.kittytalents.<id>}, {@code .desc} and {@code .mastery} (put them in your own lang file).
     *
     * @param effect what the talent does (hooks: tick, hits, damage taken, attributes, kills, owner hurt or falling);
     *               null for a talent that other code only checks the rank of
     * @throws IllegalArgumentException if a talent with this id already exists
     */
    public static Talent registerTalent(Talent talent, @Nullable TalentEffect effect) {
        if (Talents.get(talent.getId()) != null) {
            throw new IllegalArgumentException("Kitty Talents: a talent with id '" + talent.getId() + "' already exists");
        }
        Talents.register(talent);
        if (effect != null) TalentEngine.register(talent, effect);
        return talent;
    }

    /**
     * Adds an active ability (a skill or an ultimate, per its {@link CatAbility.Category}). It can be put in a Twine
     * slot from the Combat tab, and AI cats cast it according to {@link CatAbility#getAiPriority}. The slot icon is
     * {@code kittytalents:textures/ability/<id>.png} (16x16; ship it in your addon's assets).
     *
     * @throws IllegalArgumentException if an ability with this id already exists
     */
    public static CatAbility registerAbility(CatAbility ability) {
        if (CatAbilities.get(ability.getId()) != null) {
            throw new IllegalArgumentException("Kitty Talents: an ability with id '" + ability.getId() + "' already exists");
        }
        return CatAbilities.register(ability);
    }

    /**
     * Adds a behaviour mode (API 2.0). It joins the mode cycle on the Home tab after the built-in modes and is saved
     * by name. What the cat does in it comes from the builder (follow, anchor, target policy, retreat); for anything
     * more, add a goal in {@link com.kittytalents.api.event.RegisterCatGoalsEvent} that checks
     * {@code cat.getCatMode() == yourMode}. The display name is the lang key {@code mode.kittytalents.<id>}.
     *
     * <p>Register on both sides, in common setup, before any world loads. A cat saved in an addon mode goes to Follow
     * if the addon is removed.</p>
     *
     * @throws IllegalArgumentException if a mode with this name already exists
     */
    public static CatMode registerMode(CatMode mode) {
        return CatMode.register(mode);
    }

    /**
     * Adds a job (API 2.0). It joins the job picker on the Home tab and the Work Clipboard, is saved by id, and its
     * goal runs in the WORK band while the cat is clocked in to it. The title is the lang key
     * {@code job.kittytalents.<id>}.
     *
     * <p>Register on both sides, in common setup, before any world loads. A cat whose job comes from an addon that is
     * removed becomes unemployed.</p>
     *
     * @throws IllegalArgumentException if a job with this id already exists
     */
    public static CatJob registerJob(CatJob job) {
        return CatJob.register(job);
    }
}
