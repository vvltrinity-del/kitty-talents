package com.kittytalents.core;

import com.kittytalents.KittyTalents;
import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Safe removal (5.0). Minecraft deletes the entities of a removed mod, so Talented Cats would vanish.
 * <ul>
 * <li>{@code /kittytalents uninstall} turns removal mode on: every Talented Cat that is (or gets) loaded becomes a
 * vanilla tamed cat with the same owner, breed, collar, name, age and sitting state. Its Kitty Talents data rides
 * along hidden in the vanilla cat's NBT; its gear is dropped next to it. Cats housed in loaded Habitats are released
 * first, and the chunks of every cat in the cat directory are force-loaded for a few seconds so far-away cats convert too.</li>
 * <li>{@code /kittytalents reinstall} turns it off: vanilla cats carrying that data turn back into the same Talented
 * Cats (same UUID, talents, level, bond) as they load.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KittyTalents.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VanillaRevert {
    private VanillaRevert() {}

    public static final String TAG = "KittyTalentsRestore";
    private static final int FORCE_TICKS = 200;

    /** World flag: removal mode. */
    public static final class State extends SavedData {
        boolean removing;
        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(t -> {
                State s = new State();
                s.removing = t.getBoolean("Removing");
                return s;
            }, State::new, "kittytalents_removal");
        }
        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Removing", removing);
            return tag;
        }
    }

    private record Forced(ServerLevel level, ChunkPos chunk, long until) {}
    private static final List<Forced> FORCED = new ArrayList<>();

    public static boolean removing(MinecraftServer server) {
        return server != null && State.get(server).removing;
    }

    /** @return how many cats were converted right away (more follow as their chunks load). */
    public static int uninstall(MinecraftServer server) {
        State st = State.get(server);
        st.removing = true;
        st.setDirty();
        int n = 0;
        for (ServerLevel level : server.getAllLevels()) {
            // Housed cats first: a Habitat's stored cat would vanish with the block.
            for (BlockPos pos : com.kittytalents.core.living.HabitatModifiers.loadedIn(level)) {
                if (level.getBlockEntity(pos) instanceof KittyHabitatBlockEntity h && h.hasStoredCat()) h.releaseHousedCat(pos.above());
            }
            List<TalentedCat> cats = new ArrayList<>();
            level.getEntities(ModEntities.TALENTED_CAT.get(), c -> true).forEach(cats::add);
            for (TalentedCat c : cats) if (toVanilla(c)) n++;
        }
        // Far-away cats: load their chunks for a moment; they convert as they join.
        for (CatDirectory.Entry e : CatDirectory.get(server.overworld()).all()) {
            for (ServerLevel level : server.getAllLevels()) {
                if (!level.dimension().location().toString().equals(e.dimension) || e.pos == null) continue;
                ChunkPos cp = new ChunkPos(e.pos);
                if (level.setChunkForced(cp.x, cp.z, true)) FORCED.add(new Forced(level, cp, server.getTickCount() + FORCE_TICKS));
            }
        }
        return n;
    }

    /** @return how many loaded vanilla cats were turned back into Talented Cats. */
    public static int reinstall(MinecraftServer server) {
        State st = State.get(server);
        st.removing = false;
        st.setDirty();
        int n = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Cat> marked = new ArrayList<>();
            level.getEntities(EntityType.CAT, c -> c.getPersistentData().contains(TAG)).forEach(marked::add);
            for (Cat c : marked) if (fromVanilla(c)) n++;
        }
        return n;
    }

    /** Replaces a Talented Cat with a vanilla tamed cat carrying its data. */
    public static boolean toVanilla(TalentedCat cat) {
        if (!(cat.level() instanceof ServerLevel level) || cat.isRemoved()) return false;
        if (cat.isTwined()) TwineManager.endForCat(cat, null);
        cat.dropAllGear(true);
        if (cat.isCaptain()) cat.demoteCaptain();
        CompoundTag data = cat.saveWithoutId(new CompoundTag());
        Cat vanilla = EntityType.CAT.create(level);
        if (vanilla == null) return false;
        vanilla.moveTo(cat.getX(), cat.getY(), cat.getZ(), cat.getYRot(), cat.getXRot());
        vanilla.setVariant(cat.getVariant());
        vanilla.setCollarColor(cat.getCollarColor());
        if (cat.isTame() && cat.getOwnerUUID() != null) {
            vanilla.setTame(true);
            vanilla.setOwnerUUID(cat.getOwnerUUID());
        }
        vanilla.setOrderedToSit(cat.isOrderedToSit());
        vanilla.setInSittingPose(cat.isOrderedToSit());
        vanilla.setAge(cat.getAge());
        if (cat.hasCustomName()) vanilla.setCustomName(cat.getCustomName());
        vanilla.setPersistenceRequired();
        vanilla.getPersistentData().put(TAG, data);
        cat.discard();
        level.addFreshEntity(vanilla);
        return true;
    }

    /** Turns a marked vanilla cat back into the Talented Cat it was. */
    public static boolean fromVanilla(Cat vanilla) {
        if (!(vanilla.level() instanceof ServerLevel level) || vanilla instanceof TalentedCat || vanilla.isRemoved()) return false;
        CompoundTag data = vanilla.getPersistentData().getCompound(TAG);
        if (data.isEmpty()) return false;
        TalentedCat cat = ModEntities.TALENTED_CAT.get().create(level);
        if (cat == null) return false;
        cat.load(data);
        cat.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        if (vanilla.hasCustomName()) cat.setCustomName(vanilla.getCustomName());
        vanilla.discard();
        level.addFreshEntity(cat);
        return true;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        boolean removing = removing(level.getServer());
        // Queued for the end of the tick: entities must not be added or removed while one is joining.
        if (event.getEntity() instanceof TalentedCat cat && removing) {
            PENDING.add(() -> toVanilla(cat));
        } else if (!removing && event.getEntity() instanceof Cat vanilla && !(vanilla instanceof TalentedCat)
                && vanilla.getPersistentData().contains(TAG)) {
            PENDING.add(() -> fromVanilla(vanilla));
        }
    }

    private static final List<Runnable> PENDING = new ArrayList<>();

    /** Runs queued conversions (also called by tests). */
    public static void runPending() {
        List<Runnable> now = new ArrayList<>(PENDING);
        PENDING.clear();
        now.forEach(Runnable::run);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!PENDING.isEmpty()) runPending();
        if (FORCED.isEmpty()) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        FORCED.removeIf(f -> {
            if (server.getTickCount() < f.until()) return false;
            f.level().setChunkForced(f.chunk().x, f.chunk().z, false);
            return true;
        });
    }
}
