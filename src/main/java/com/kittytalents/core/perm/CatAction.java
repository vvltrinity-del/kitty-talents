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
    OPEN_PACK(true, 32),
    /** Mode changes, whistle and team commands. */
    COMMAND(true, 64),
    /** Skin, collar colour, gender. */
    STYLE(true, 32),
    /** Equip / unequip gear, toggle cosmetics. */
    EQUIP(true, 32),
    /** Combat loadout (role, element, ability slots). */
    LOADOUT(true, 32),
    /** Job and clock-in. */
    SET_JOB(true, 32),
    /** Start a Twine session. */
    TWINE_START(true, 16),
    /** Twine movement, dodge, synergy, traversal while twined (session-bound, see class doc). */
    TWINE_CONTROL(true, -1),
    /** Spend skill points. Owner only: points can't be refunded without a Respec Elixir. */
    TRAIN(false, 32),
    /** Refund all talents (costs an elixir). */
    RESPEC(false, 32),
    /** Rename the cat. */
    RENAME(false, 32),
    /** Add or remove co-owners. Co-owners may only remove themselves (handled by the packet). */
    MANAGE_TRUST(false, 32);

    private final boolean trustedAllowed;
    private final double maxDistance;

    CatAction(boolean trustedAllowed, double maxDistance) {
        this.trustedAllowed = trustedAllowed;
        this.maxDistance = maxDistance;
    }

    /** Whether a co-owner (trusted player) may do this. The primary owner may always; strangers never. */
    public boolean trustedAllowed() {
        return trustedAllowed;
    }

    /** Maximum distance in blocks between player and cat, or a negative value for "no range check". */
    public double maxDistance() {
        return maxDistance;
    }
}
