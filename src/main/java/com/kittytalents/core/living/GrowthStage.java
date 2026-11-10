package com.kittytalents.core.living;

import com.kittytalents.core.tuning.Tuning;
import net.minecraft.network.chat.Component;

/**
 * Kitten growth (4.0 Phase 6). A newborn takes the vanilla 20 minutes (age -24000 .. 0) to grow up and passes through
 * three smaller stages on the way, each with its own size and proportions (bigger head, shorter legs).
 * Food and treats speed it up. Adults (and every 3.0 cat) are {@link #ADULT}.
 */
public enum GrowthStage {
    NEWBORN("newborn", "Newborn", 0.42, 1.40),
    KITTEN("kitten", "Kitten", 0.58, 1.28),
    JUNIOR("junior", "Junior", 0.78, 1.12),
    ADULT("adult", "Adult", 1.0, 1.0);

    public static final String SCOPE = "growth";
    /** Vanilla baby age. */
    public static final int BABY_AGE = -24000;

    public static final Tuning.Key FOOD_SPEEDUP = Tuning.define(SCOPE, "feeding", "food_fraction", 0.10, 0, 1, "Remaining growth skipped per food");
    public static final Tuning.Key TREAT_SPEEDUP = Tuning.define(SCOPE, "feeding", "treat_fraction", 0.25, 0, 1, "Remaining growth skipped per treat");

    public final String id, displayName;
    private final Tuning.Key scale, head;

    GrowthStage(String id, String displayName, double scale, double head) {
        this.id = id;
        this.displayName = displayName;
        this.scale = Tuning.define(SCOPE, id, "scale", scale, 0.2, 1.5, "Body size");
        this.head = Tuning.define(SCOPE, id, "head_scale", head, 0.5, 2.0, "Head size");
    }

    /** Whole-body size relative to an adult. */
    public float scale() {
        return scale.f();
    }

    /** Head size relative to the body (kittens have big heads). */
    public float headScale() {
        return head.f();
    }

    public Component title() {
        return Component.translatableWithFallback("growth.kittytalents." + id, displayName);
    }

    /** The stage for a vanilla age value (negative = still growing). */
    public static GrowthStage ofAge(int age) {
        if (age >= 0) return ADULT;
        if (age < BABY_AGE * 2 / 3) return NEWBORN;
        if (age < BABY_AGE / 3) return KITTEN;
        return JUNIOR;
    }

    public static GrowthStage byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : ADULT;
    }

    public static void init() {}
}
