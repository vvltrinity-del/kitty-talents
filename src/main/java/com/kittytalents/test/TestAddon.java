package com.kittytalents.test;

import com.kittytalents.api.KittyTalentsAPI;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.CatJob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stand-in addon for the GameTest server (4.0 Phase 8): one mode and one job, added through
 * {@link KittyTalentsAPI} only, the way a real addon would in its common setup. Never registered in a normal game.
 */
public final class TestAddon {
    private TestAddon() {}

    public static CatMode SCOUT;
    public static CatJob COUNTER;
    /** Ticks the test job has worked, per cat. */
    public static final Map<UUID, Integer> WORKED = new ConcurrentHashMap<>();

    public static void register() {
        if (SCOUT != null) return;
        SCOUT = KittyTalentsAPI.registerMode(CatMode.builder("kttest_scout")
                .followsOwner().followDistance(9.0D, 7.0D)
                .targetPolicy(CatMode.TargetPolicy.SEEK_HOSTILES).retreatsWhenHurt(false).build());
        COUNTER = KittyTalentsAPI.registerJob(CatJob.builder("kttest_counter", "Counter")
                .description("Counts the ticks it works.").minCatLevel(5)
                .icon(() -> new ItemStack(Items.CLOCK)).goal(CounterGoal::new).build());
    }

    /** The simplest possible job: counts while it is the cat's job and the cat is at work. */
    private static final class CounterGoal extends Goal {
        private final TalentedCat cat;

        CounterGoal(TalentedCat cat) { this.cat = cat; }

        @Override
        public boolean canUse() {
            return cat.getCatJob() == COUNTER && cat.isClockedIn() && cat.getCatMode().isWorkMode();
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() { WORKED.merge(cat.getUUID(), 1, Integer::sum); }
    }
}
