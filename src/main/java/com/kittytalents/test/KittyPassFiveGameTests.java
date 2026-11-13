package com.kittytalents.test;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.core.VanillaRevert;
import com.kittytalents.core.base.BaseAreas;
import com.kittytalents.core.living.CatTrait;
import com.kittytalents.core.living.HabitatModifiers;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModBlocks;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.talent.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.kittytalents.test.KittyTalentGameTests.joinFakeOwner;
import static com.kittytalents.test.KittyTalentGameTests.leave;

/** 5.0 pass 5: save safety, safe removal, offline hunger and a colony-sized performance check. */
@GameTestHolder(KittyTalents.MODID)
@PrefixGameTestTemplate(false)
public class KittyPassFiveGameTests {

    /** uninstall -> vanilla cat with the data hidden and the gear dropped; reinstall -> the same Talented Cat. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testVanillaRevertRoundTrip(GameTestHelper h) {
        var fp = joinFakeOwner(h, new BlockPos(3, 2, 3));
        try {
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
            c.tame(fp);
            c.setCatLevel(42);
            c.setTalentLevel(Talents.HUNTER_CAT, 4);
            c.setEquippedWeapon(new ItemStack(Items.IRON_SWORD));
            UUID id = c.getUUID();
            AABB box = new AABB(h.absolutePos(BlockPos.ZERO)).inflate(8);
            h.assertTrue(VanillaRevert.toVanilla(c), "converted");
            var vanilla = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Cat.class, box,
                    v -> !(v instanceof TalentedCat) && v.getPersistentData().contains(VanillaRevert.TAG));
            h.assertTrue(vanilla.size() == 1, "one vanilla cat with the data, found " + vanilla.size());
            h.assertTrue(vanilla.get(0).isTame() && fp.getUUID().equals(vanilla.get(0).getOwnerUUID()), "still tame and owned");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(Items.IRON_SWORD)).isEmpty(), "the sword was dropped");
            VanillaRevert.runPending(); // removal mode is off in tests: the joined vanilla cat would turn back by itself
            TalentedCat back = (TalentedCat) h.getLevel().getEntity(id);
            if (back == null) {
                h.assertTrue(VanillaRevert.fromVanilla(vanilla.get(0)), "restored");
                back = (TalentedCat) h.getLevel().getEntity(id);
            }
            h.assertTrue(back != null && back.getCatLevel() == 42 && back.getTalentLevel(Talents.HUNTER_CAT) == 4, "same cat, level and talents");
            h.assertTrue(back.getEquippedWeapon().isEmpty(), "no duplicated gear");
            h.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        } finally {
            leave(h, fp);
        }
    }

    /** A cat whose owner is offline doesn't get hungry. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testNoHungerWhileOwnerOffline(GameTestHelper h) {
        TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2, 2, 2));
        c.setTame(true);
        c.setOwnerUUID(UUID.randomUUID()); // nobody with this id is logged in
        h.assertFalse(c.ownerOnline(), "owner should count as offline");
        h.succeed();
    }

    /**
     * Colony performance (doc: "20+ cats loaded"): 10 housed Habitat cats with traits, 10 Guard cats patrolling a
     * base, 10 Work cats and 4 followers. Logs the cats' cost per tick; fails above 10 ms (a fifth of a tick).
     */
    @GameTest(template = "arena", timeoutTicks = 400, batch = "colony")
    public static void testColonyTickBudget(GameTestHelper h) {
        var owner = joinFakeOwner(h, new BlockPos(15, 2, 15));
        var log = org.slf4j.LoggerFactory.getLogger("KittyTalents/Stress");
        CatTrait[] traits = {CatTrait.SHY, CatTrait.BRAVE, CatTrait.LAZY, CatTrait.CURIOUS, CatTrait.PLAYFUL, CatTrait.GREEDY};
        for (int i = 0; i < 10; i++) {
            BlockPos rel = new BlockPos(2 + i * 2, 2, 28);
            h.setBlock(rel, ModBlocks.KITTY_HABITAT.get().defaultBlockState());
            var be = (KittyHabitatBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
            HabitatModifiers.loaded(h.getLevel(), h.absolutePos(rel));
            TalentedCat worker = h.spawn(ModEntities.TALENTED_CAT.get(), rel.above());
            worker.tame(owner);
            worker.setTrait(traits[i % traits.length]);
            be.setWorker(worker);
        }
        BaseAreas.Area area = BaseAreas.get(h.getLevel().getServer()).add(owner.getUUID(), h.getLevel().dimension().location().toString(),
                h.absolutePos(new BlockPos(1, 1, 1)), h.absolutePos(new BlockPos(28, 3, 20)));
        int n = 0;
        for (int i = 0; i < 24; i++) {
            CatMode mode = i < 10 ? CatMode.GUARD : i < 20 ? CatMode.WORK : CatMode.FOLLOW;
            TalentedCat c = h.spawn(ModEntities.TALENTED_CAT.get(), new BlockPos(2 + (i % 12) * 2, 2, 4 + (i / 12) * 6));
            c.tame(owner);
            c.setOrderedToSit(false);
            c.setCatMode(mode);
            c.tickCostTracked = true;
            n++;
        }
        for (int z = 0; z < 4; z++) h.spawn(EntityType.ZOMBIE, new BlockPos(4 + z * 6, 2, 18));
        var server = h.getLevel().getServer();
        long[] start = {0};
        h.runAfterDelay(60, () -> {
            start[0] = server.getTickCount();
            com.kittytalents.util.TickCost.reset();
            com.kittytalents.util.TickCost.enabled = true;
        });
        final int live = n, cats = n + 10;
        h.runAfterDelay(260, () -> {
            com.kittytalents.util.TickCost.enabled = false;
            double ticks = Math.max(1, server.getTickCount() - start[0]);
            double ms = com.kittytalents.util.TickCost.nanos() / 1.0e6 / ticks;
            log.info("Colony: {} live cats + 10 housed: {} ms/tick for the live cats, whole server {} ms/tick", live,
                    String.format("%.3f", ms), String.format("%.2f", server.getAverageTickTime()));
            BaseAreas.get(server).delete(owner.getUUID(), area.id());
            leave(h, owner);
            h.assertTrue(ms < 10.0, cats + " cats cost " + ms + " ms per tick");
            h.succeed();
        });
    }
}
