package com.kittytalents.api;

/**
 * The priority bands of a Talented Cat's AI (CatBrain), highest first. Addons add goals to a band through
 * {@link com.kittytalents.api.event.RegisterCatGoalsEvent} instead of guessing raw goal priorities.
 */
public enum CatGoalBand {
    /** Never interrupted: floating, staying safe while downed. */
    SAFETY(0),
    /** Explicit orders: sit, retreat at low health. */
    ORDERS(1),
    /** Ranged and opening moves: pounce, stalk, dodge, guns. */
    COMBAT(2),
    /** Melee and ability use. */
    MELEE(3),
    /** The current mode's movement: follow, hold position, return home, fetch. */
    COMMAND(4),
    /** Job work while clocked in. */
    WORK(5),
    /** Hunger, beds, scratchers, breeding. */
    NEEDS(6),
    /** Idle life: wandering, grooming, looking around. */
    IDLE(7);

    private final int priority;

    CatGoalBand(int priority) { this.priority = priority; }

    /** The GoalSelector priority this band uses. */
    public int priority() { return priority; }
}
