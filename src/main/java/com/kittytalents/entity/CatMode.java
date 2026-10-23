package com.kittytalents.entity;

import net.minecraft.network.chat.Component;

public enum CatMode {
    DOCILE("mode.kittytalents.docile"),
    FOLLOW("mode.kittytalents.follow"),
    STAY("mode.kittytalents.stay"),
    WANDER("mode.kittytalents.wander"),
    HUNTING("mode.kittytalents.hunt"),
    GUARD("mode.kittytalents.guard"),
    AGGRESSIVE("mode.kittytalents.aggressive"),
    BERSERKER("mode.kittytalents.berserker"),
    PATROL("mode.kittytalents.patrol"),
    TACTICAL("mode.kittytalents.tactical"),
    DEFENSIVE("mode.kittytalents.defensive"),
    ESCORT("mode.kittytalents.escort"),
    FETCH("mode.kittytalents.fetch"),
    MINE("mode.kittytalents.mine"),
    FARM("mode.kittytalents.farm"),
    FISH("mode.kittytalents.fish"),
    SUPPORT("mode.kittytalents.support"),
    PROTECT("mode.kittytalents.protect"),
    RETURN_HOME("mode.kittytalents.return_home"),
    RETREAT("mode.kittytalents.retreat"),
    WORK("mode.kittytalents.work");

    private final String key;

    CatMode(String key) {
        this.key = key;
    }

    public Component getDisplayName() {
        return Component.translatable(key);
    }

    public String getKey() {
        return key;
    }

    public boolean isCombatMode() {
        return this == AGGRESSIVE || this == BERSERKER || this == GUARD ||
               this == DEFENSIVE || this == PROTECT || this == TACTICAL || this == HUNTING;
    }

    public boolean shouldFollowOwner() {
        return this == FOLLOW || this == ESCORT || this == DEFENSIVE ||
               this == SUPPORT || this == PROTECT || this == TACTICAL;
    }

    public boolean isStationary() {
        return this == DOCILE || this == STAY || this == GUARD;
    }

    public boolean isWorkMode() {
        return this == WORK || this == FETCH || this == MINE || this == FARM || this == FISH;
    }

    public boolean canAttack() {
        return this != DOCILE && this != RETREAT && this != STAY && this != WORK;
    }

    public boolean shouldRetreat() {
        return this == RETREAT;
    }

    public CatMode next() {
        CatMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public CatMode previous() {
        CatMode[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static CatMode byId(int id) {
        CatMode[] values = values();
        if (id < 0 || id >= values.length) {
            return DOCILE;
        }
        return values[id];
    }

    public static CatMode byName(String name) {
        for (CatMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return DOCILE;
    }
}
