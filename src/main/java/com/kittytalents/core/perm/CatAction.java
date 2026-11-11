package com.kittytalents.core.perm;

/**
 * Everything a player can ask the server to do to one cat. Each action says who may do it
 * ({@link CatPermissions#allows}) and how close the player must be.
 * <p>
 * Screen actions use 32 blocks: the companion screen stays open while a cat walks around, and the Twine body is kept
 * within 32 blocks of its cat. Starting Twine needs the cat under the crosshair, so 16 blocks is already generous.
 * {@link #TWINE_CONTROL} has no range: it is only valid for the cat the sender is twined with right now.
 */
public enum CatAction {
    /** Open the cat's pack inventory. */
    OPEN_PACK(TrustRole.FULL, 32),
    /** Mode changes, whistle and team commands. */
    COMMAND(TrustRole.HANDLER, 64),
    /** Skin, collar colour, gender. */
    STYLE(TrustRole.FULL, 32),
    /** Equip / unequip gear, toggle cosmetics. */
    EQUIP(TrustRole.FULL, 32),
    /** Combat loadout (role, element, ability slots). */
    LOADOUT(TrustRole.FULL, 32),
    /** Job and clock-in. */
    SET_JOB(TrustRole.FULL, 32),
    /** Start a Twine session. */
    TWINE_START(TrustRole.HANDLER, 16),
    /** Twine movement, dodge, synergy, traversal while twined (session-bound, see class doc). */
    TWINE_CONTROL(TrustRole.HANDLER, -1),
    /** Spend skill points. Owner only: points can't be refunded without a Respec Elixir. */
    TRAIN(null, 32),
    /** Refund all talents (costs an elixir). */
    RESPEC(null, 32),
    /** Rename the cat. */
    RENAME(null, 32),
    /** Add or remove co-owners. Co-owners may only remove themselves (handled by the packet). */
    MANAGE_TRUST(null, 32),
    /** Ride the cat (4.0): the one thing a Rider co-owner may do. */
    RIDE(TrustRole.RIDER, 8);

    @javax.annotation.Nullable
    private final TrustRole minRole;
    private final double maxDistance;

    CatAction(@javax.annotation.Nullable TrustRole minRole, double maxDistance) {
        this.minRole = minRole;
        this.maxDistance = maxDistance;
    }

    /** Whether a co-owner (trusted player) may do this. The primary owner may always; strangers never. */
    public boolean trustedAllowed() {
        return minRole != null;
    }

    /** The lowest co-owner role that may do this, or null when only the owner may. */
    @javax.annotation.Nullable
    public TrustRole minRole() {
        return minRole;
    }

    /** Maximum distance in blocks between player and cat, or a negative value for "no range check". */
    public double maxDistance() {
        return maxDistance;
    }
}
