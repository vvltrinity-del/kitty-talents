package com.kittytalents.entity;

import com.kittytalents.KittyTalents;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketSyncTwine;
import com.kittytalents.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Twine state: which player is twined with which cat, and the player's dormant body.
 * <p>
 * Every way a session can end goes through {@link #endForPlayer} or {@link #endForCat}, and both always restore the
 * player's body (visibility, invulnerability, gravity, physics). The original body flags are also written into the
 * player's persistent data, so a crash or a missed logout can't leave a player invisible and invulnerable for good:
 * {@link #restoreOnLogin} puts them back the next time the player joins.
 * <p>
 * Edge cases and where they are handled:
 * <ul>
 *   <li>player logs out, dies or changes dimension: ForgeEvents → {@link #endForPlayer}</li>
 *   <li>cat dies: TalentedCat#die → {@link #endForCat}</li>
 *   <li>cat is removed (killed, discarded, chunk unload, dimension change, habitat): TalentedCat#remove → {@link #endForCat}</li>
 *   <li>cat or player goes missing / incapacitated / different level: TalentedCat#tick and ForgeEvents#onPlayerTick</li>
 *   <li>server stops: {@link #clearAll} (after every player has logged out)</li>
 * </ul>
 * Twine moves (dodge, synergy, traversal) are in {@link TwineMoves}.
 */
public final class TwineManager {
    /** Player-data key holding the body flags to restore; present only while a session is (or was) active. */
    public static final String BODY_TAG = "kittytalents_twine_body";

    private static final Map<UUID, UUID> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> REVERSE_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> DORMANT_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, DormantBodyState> DORMANT_BODY_STATES = new ConcurrentHashMap<>();

    private TwineManager() {}

    // ------------------------------------------------------------------ queries

    public static boolean isPlayerTwined(UUID player) {
        return SESSIONS.containsKey(player);
    }

    @Nullable
    public static UUID getCatOf(UUID player) {
        return SESSIONS.get(player);
    }

    @Nullable
    public static UUID getPlayerOf(UUID cat) {
        return REVERSE_SESSIONS.get(cat);
    }

    @Nullable
    public static Vec3 getDormantPosition(UUID player) {
        return DORMANT_POSITIONS.get(player);
    }

    /** True while the player's body flags are overridden by Twine. For GameTests. */
    public static boolean isBodyHidden(UUID player) {
        return DORMANT_BODY_STATES.containsKey(player);
    }

    public static void register(UUID player, UUID cat) {
        SESSIONS.put(player, cat);
        REVERSE_SESSIONS.put(cat, player);
    }

    // ------------------------------------------------------------------ start

    /**
     * Starts a session. The caller has already checked permissions and the cat's state (PacketTwineAction).
     * Any previous session of the player, or of the cat, ends first.
     */
    public static boolean start(ServerPlayer player, TalentedCat cat) {
        if (isPlayerTwined(player.getUUID())) endForPlayer(player, null);
        if (cat.isTwined()) endForCat(cat, null);

        cat.setTwinedPlayer(player); // also registers the session
        register(player.getUUID(), cat.getUUID());
        DORMANT_POSITIONS.put(player.getUUID(), player.position());
        hideDormantBody(player);
        cat.setMaxUpStep(1.0F);

        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncTwine(cat.getId(), true));
        player.playNotifySound(ModSounds.TWINE_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.literal("✦ Twined with " + cat.getName().getString() + "! Controlling companion. ✦").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // ------------------------------------------------------------------ dormant body

    /**
     * A connected ServerPlayer cannot safely be removed from a live level: doing so tears down normal player tracking.
     * Hide and suspend the body instead, keeping the real player session while the cat is the visible form.
     */
    public static void hideDormantBody(ServerPlayer player) {
        DormantBodyState state = DORMANT_BODY_STATES.computeIfAbsent(player.getUUID(), id -> new DormantBodyState(
                player.isInvisible(), player.isInvulnerable(), player.isNoGravity(), player.noPhysics));
        player.getPersistentData().put(BODY_TAG, state.toTag());
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setNoGravity(true);
        player.noPhysics = true;
        player.setDeltaMovement(Vec3.ZERO);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.9D, player.getZ(), 24, 0.25D, 0.45D, 0.25D, 0.08D);
        }
    }

    private static void restoreDormantBody(ServerPlayer player) {
        DormantBodyState state = DORMANT_BODY_STATES.remove(player.getUUID());
        if (state == null && player.getPersistentData().contains(BODY_TAG)) {
            state = DormantBodyState.fromTag(player.getPersistentData().getCompound(BODY_TAG));
        }
        player.getPersistentData().remove(BODY_TAG);
        if (state == null) return;
        state.applyTo(player);
        player.fallDistance = 0.0F;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.9D, player.getZ(), 24, 0.25D, 0.45D, 0.25D, 0.08D);
        }
    }

    /**
     * Login safety net: if the player's saved data still carries Twine body flags (server crash or a missed logout
     * while twined), restore them. noPhysics isn't saved by vanilla, so it is simply reset.
     */
    public static void restoreOnLogin(ServerPlayer player) {
        if (isPlayerTwined(player.getUUID())) return;
        if (player.getPersistentData().contains(BODY_TAG)) {
            KittyTalents.LOGGER.info("[Twine] restoring {}'s body flags left over from an unfinished Twine session", player.getGameProfile().getName());
            DORMANT_BODY_STATES.remove(player.getUUID());
            DormantBodyState.fromTag(player.getPersistentData().getCompound(BODY_TAG)).applyTo(player);
            player.getPersistentData().remove(BODY_TAG);
        }
    }

    // ------------------------------------------------------------------ end

    /** Ends the player's session (if any), restores the body and tells the client. Safe to call when not twined. */
    public static void endForPlayer(ServerPlayer player, @Nullable String reason) {
        boolean hadSession = SESSIONS.containsKey(player.getUUID()) || DORMANT_BODY_STATES.containsKey(player.getUUID());
        UUID catUuid = SESSIONS.remove(player.getUUID());
        DORMANT_POSITIONS.remove(player.getUUID());
        restoreDormantBody(player);
        int catId = -1;
        if (catUuid != null) {
            REVERSE_SESSIONS.remove(catUuid, player.getUUID());
            TalentedCat cat = findCat(player, catUuid);
            if (cat != null) {
                catId = cat.getId();
                if (cat.isTwinedWith(player)) {
                    cat.clearTwineState();
                    cat.setMaxUpStep(0.6F);
                }
            }
        }
        if (hadSession) finish(player, catId, reason);
    }

    /** Ends the session of a cat (death, removal, incapacitation, unload). Safe to call when not twined. */
    public static void endForCat(TalentedCat cat, @Nullable String reason) {
        if (cat.level().isClientSide) return;
        UUID playerUuid = cat.getTwinedPlayerUUID();
        UUID mapped = REVERSE_SESSIONS.remove(cat.getUUID());
        if (playerUuid == null) playerUuid = mapped;
        cat.clearTwineState();
        cat.setMaxUpStep(0.6F);
        if (playerUuid == null) return;

        // Only end the player's session if it is still this cat's (they may have moved on to another cat).
        if (!SESSIONS.remove(playerUuid, cat.getUUID())) return;
        DORMANT_POSITIONS.remove(playerUuid);

        ServerPlayer player = cat.getServer() != null ? cat.getServer().getPlayerList().getPlayer(playerUuid) : null;
        if (player == null && cat.level().getPlayerByUUID(playerUuid) instanceof ServerPlayer inLevel) player = inLevel;
        if (player != null) {
            restoreDormantBody(player);
            finish(player, cat.getId(), reason);
        } else {
            DORMANT_BODY_STATES.remove(playerUuid); // offline: the login safety net restores the saved flags
        }
    }

    /** Server stopping: forget all sessions (players have already been logged out and restored by then). */
    public static void clearAll() {
        SESSIONS.clear();
        REVERSE_SESSIONS.clear();
        DORMANT_POSITIONS.clear();
        DORMANT_BODY_STATES.clear();
        TwineMoves.clearAll();
    }

    @Nullable
    private static TalentedCat findCat(ServerPlayer player, UUID catUuid) {
        if (player.level() instanceof ServerLevel own && own.getEntity(catUuid) instanceof TalentedCat cat) return cat;
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity e = level.getEntity(catUuid);
            if (e instanceof TalentedCat cat) return cat;
        }
        return null;
    }

    private static void finish(ServerPlayer player, int catId, @Nullable String reason) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncTwine(catId, false));
        player.playNotifySound(ModSounds.TWINE_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (reason != null && !reason.isEmpty()) {
            player.displayClientMessage(Component.literal(reason).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private record DormantBodyState(boolean invisible, boolean invulnerable, boolean noGravity, boolean noPhysics) {
        CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            t.putBoolean("Invisible", invisible);
            t.putBoolean("Invulnerable", invulnerable);
            t.putBoolean("NoGravity", noGravity);
            return t;
        }

        static DormantBodyState fromTag(CompoundTag t) {
            return new DormantBodyState(t.getBoolean("Invisible"), t.getBoolean("Invulnerable"), t.getBoolean("NoGravity"), false);
        }

        void applyTo(ServerPlayer player) {
            player.setInvisible(invisible);
            player.setInvulnerable(invulnerable);
            player.setNoGravity(noGravity);
            player.noPhysics = noPhysics;
        }
    }
}
