package com.kittytalents.talent.effect;

/** Registers every built-in talent effect. 4.0 / addons add theirs with {@link TalentEngine#register}. */
final class TalentEffects {
    private TalentEffects() {}

    static void registerAll() {
        CombatTalents.register();
        DefenseTalents.register();
        SupportTalents.register();
        UtilityTalents.register();
        MobilityAndDireTalents.register();
    }
}
