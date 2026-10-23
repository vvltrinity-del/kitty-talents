package com.kittytalents.entity;

public enum WhistleCommand {
    HEEL("Heel & Teleport", "Commands cats to immediately sprint or teleport to your side."),
    STAY("Stay (Docile)", "Commands cats to sit down and rest."),
    FOLLOW("Follow Me", "Commands cats to follow and assist you."),
    GUARD("Guard Area", "Commands cats to defend a 16-block territory."),
    HUNT("Hunting Mode", "Commands cats to hunt small prey and pests."),
    AGGRESSIVE("Aggressive", "Commands cats to seek and attack hostile mobs."),
    RETURN_HOME("Return Home", "Commands cats with linked beds to return home."),
    STOP_ATTACK("Stop Attacking", "Commands cats to immediately disengage from combat."),
    QUIET("Ssshhh! (Quiet)", "Silences cats from meowing for 5 minutes."),
    COME("Come Here", "Calls cats to your location."),
    STAND("All Stand Up", "Commands all sitting cats to stand up."),
    SIT("All Sit Down", "Commands all standing cats to sit down."),
    ATTACK_TARGET("Attack Focus", "Directs cats to target your currently focused enemy."),
    DISENGAGE("Disengage & Calm", "Commands cats to break combat and calm down."),
    DEFEND("Defend Area", "Commands cats to guard the immediate area."),
    PATROL("Patrol Perimeter", "Commands cats to patrol the perimeter."),
    FETCH("Fetch Items", "Commands cats with Paw Retriever to fetch nearby items."),
    SCATTER("Scatter / Disperse", "Commands cats to disperse outward."),
    REST("Rest on Bed", "Commands cats to rest on the nearest cat bed."),
    EAT("Eat from Bowl", "Commands cats to eat from the nearest food bowl."),
    PURR_BUFF("Purr Buff", "Commands cats with Purr Therapy to provide healing & buffs."),
    ROAR_HISS("Hiss / Intimidate", "Commands cats to hiss and scare creepers and foes."),
    FRENZY("Trigger Frenzy", "Boosts feline attack speed temporarily."),
    LOCATE("Locate Beacon", "Emits beacon particles to locate your cats."),
    PLAY("Play with Toys", "Commands cats to play with nearby toys."),
    FISH("Fish in Water", "Commands cats with Fisherman talent to catch fish."),
    MINE("Mole Cat Dig", "Commands cats with Mole talent to dig underground."),
    FLY("Mount Takeoff", "Mount or takeoff if equipped with flight gear."),
    INVENTORY("Open Cat Pack", "Opens the nearest cat's pack inventory.");

    private final String displayName;
    private final String description;

    WhistleCommand(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTitle() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getDesc() {
        return description;
    }

    public WhistleCommand next() {
        WhistleCommand[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
