package com.kittytalents.client.debug;

import com.kittytalents.KittyTalents;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Developer harness (5.0 performance pass): in a singleplayer world, measures frame time with no cats, then with
 * {@code kittytalents.perfCats} geared cats following the player, logs both and quits. Inactive unless the JVM is
 * started with {@code -Dkittytalents.perfScene=true} (Gradle task {@code runPerfScene}, tools/run_perf_scene.sh, which
 * also records a Java Flight Recorder profile).
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PerfScene {
    private static final boolean ON = Boolean.getBoolean("kittytalents.perfScene");
    private static final int CATS = Integer.getInteger("kittytalents.perfCats", 4);
    private static final int WARMUP = 200, MEASURE = 400; // client ticks

    private static int tick;
    private static long frames, frameNanos, lastFrame;
    private static boolean measuring;
    private static double baseMs;

    private PerfScene() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        tick++;
        int t1 = WARMUP, t2 = t1 + MEASURE, t3 = t2 + WARMUP, t4 = t3 + MEASURE;
        if (tick == t1 || tick == t3) start();
        else if (tick == t2) {
            baseMs = stop("no cats");
            spawn(mc);
        } else if (tick == t4) {
            double catMs = stop(CATS + " cats");
            KittyTalents.LOGGER.info("[PerfScene] result: no cats {} ms/frame ({} fps), {} cats {} ms/frame ({} fps), {} ms per cat",
                    f(baseMs), f(1000 / baseMs), CATS, f(catMs), f(1000 / catMs), f((catMs - baseMs) / CATS));
            // Remove the test cats so they are not saved into the shared render_world.
            var server = mc.getSingleplayerServer();
            server.execute(() -> server.getAllLevels().forEach(l -> l.getEntities(ModEntities.TALENTED_CAT.get(), c -> true)
                    .forEach(c -> c.discard())));
        } else if (tick == t4 + 20) {
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        long now = System.nanoTime();
        if (measuring && lastFrame != 0) { frames++; frameNanos += now - lastFrame; }
        lastFrame = now;
    }

    private static void start() { frames = 0; frameNanos = 0; lastFrame = 0; measuring = true; }

    private static double stop(String label) {
        measuring = false;
        double ms = frames == 0 ? 0 : frameNanos / 1.0e6 / frames;
        KittyTalents.LOGGER.info("[PerfScene] {}: {} frames, {} ms/frame", label, frames, f(ms));
        return ms;
    }

    private static void spawn(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p == null) return;
            ServerLevel level = p.serverLevel();
            for (int i = 0; i < CATS; i++) {
                TalentedCat c = ModEntities.TALENTED_CAT.get().create(level);
                if (c == null) return;
                double a = i * Math.PI * 2 / CATS;
                c.moveTo(p.getX() + Math.cos(a) * 3, p.getY(), p.getZ() + Math.sin(a) * 3, 0, 0);
                c.finalizeSpawn(level, level.getCurrentDifficultyAt(c.blockPosition()), MobSpawnType.COMMAND, null, null);
                c.tame(p);
                c.setOrderedToSit(false);
                c.setCatMode(CatMode.FOLLOW);
                c.setCatLevel(30);
                c.setEquippedWeapon(new ItemStack(Items.DIAMOND_SWORD));
                c.setArmorSlotItem(1, new ItemStack(Items.IRON_HELMET));
                c.setArmorSlotItem(2, new ItemStack(Items.DIAMOND_CHESTPLATE));
                c.setCosmeticSlot(0, new ItemStack(ModItems.HERO_CAPE.get()));
                level.addFreshEntity(c);
            }
        });
    }

    private static String f(double v) { return String.format("%.2f", v); }
}
