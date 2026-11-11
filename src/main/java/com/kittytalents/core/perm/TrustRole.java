package com.kittytalents.core.perm;

import net.minecraft.network.chat.Component;

/**
 * What a co-owner may do with a cat (4.0 Phase 7); the owner picks one per co-owner. Each role includes the ones
 * before it. Co-owners from before 4.0 are {@link #FULL}, which is exactly what a co-owner could do in 3.0.
 */
public enum TrustRole {
    RIDER("Rider", "May ride the cat and feed it."),
    HANDLER("Handler", "Rider, plus commands: modes, sit, whistle, Twine."),
    FULL("Full", "Handler, plus gear, pack, style, loadout and jobs.");

    public final String displayName, description;

    TrustRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean atLeast(TrustRole other) {
        return other != null && ordinal() >= other.ordinal();
    }

    public TrustRole next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component title() {
        return Component.translatableWithFallback("trust_role.kittytalents." + name().toLowerCase(), displayName);
    }

    public Component text() {
        return Component.translatableWithFallback("trust_role.kittytalents." + name().toLowerCase() + ".desc", description);
    }

    public static TrustRole byName(String name) {
        for (TrustRole r : values()) if (r.name().equalsIgnoreCase(name)) return r;
        return FULL;
    }
}
