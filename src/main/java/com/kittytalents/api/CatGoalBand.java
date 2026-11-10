package com.kittytalents.api;

/**
 * The priority bands of a Talented Cat's AI (CatBrain), highest first. Addons add goals to a band through
 * {@link com.kittytalents.api.event.RegisterCatGoalsEvent} instead of guessing raw goal priorities.
 */
public enum CatGoalBand {
    /** Never interrupted: floating, staying safe while downed. */
    SAFETY(com.kittytalents.ai.CatBrain.SAFETY),
    /** Explicit orders: sit, retreat at low health. */
    ORDERS(com.kittytalents.ai.CatBrain.ORDERS),
    /** Ranged and opening moves: stalk, dodge, guns (the pounce itself sits just above this band). */
    COMBAT(com.kittytalents.ai.CatBrain.COMBAT),
    /** Melee and ability use. */
    MELEE(com.kittytalents.ai.CatBrain.MELEE),
    /** The current mode's movement: follow, hold position, return home, fetch. */
    COMMAND(com.kittytalents.ai.CatBrain.COMMAND),
    /** Job work while clocked in. */
    WORK(com.kittytalents.ai.CatBrain.WORK),
    /** Hunger, beds, scratchers, breeding. */
    NEEDS(com.kittytalents.ai.CatBrain.NEEDS),
    /** Idle life: wandering, grooming, looking around. */
    IDLE(com.kittytalents.ai.CatBrain.IDLE);

    private final int priority;

    CatGoalBand(int priority) { this.priority = priority; }

    /** The GoalSelector priority this band uses. */
    public int priority() { return priority; }
}
