package com.kittytalents.core.perm;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * The one permission matrix for cats: who (owner / co-owner / stranger) may do which {@link CatAction}.
 * Every client-to-server packet goes through {@link #check} so no packet has its own ad-hoc rules.
 *
 * <pre>
 *                 OWNER  TRUSTED  STRANGER
 *  OPEN_PACK        x       x
 *  COMMAND          x       x
 *  STYLE            x       x
 *  EQUIP            x       x
 *  LOADOUT          x       x
 *  SET_JOB          x       x
 *  TWINE_START      x       x
 *  TWINE_CONTROL    x       x      (and only the cat the sender is twined with)
 *  TRAIN            x
 *  RESPEC           x
 *  RENAME           x
 *  MANAGE_TRUST     x              (co-owners may remove themselves)
 * </pre>
 * Co-owners are added only while they are online real players, and only the owner can remove them (decided 2026-11-07).
 */
public final class CatPermissions {
    private CatPermissions() {}

    public enum Role { OWNER, TRUSTED, STRANGER }

    /** Why a request was refused. {@code null} in {@link Access#denial()} means allowed. */
    public enum Denial { NO_ENTITY, NOT_A_CAT, DEAD, TOO_FAR, NOT_ALLOWED, NOT_TWINED }

    /** Result of {@link #check}: the cat (when the entity is a cat at all) and the denial reason, if any. */
    public record Access(@Nullable TalentedCat cat, @Nullable Denial denial) {
        public boolean allowed() {
            return denial == null && cat != null;
        }
    }

    public static Role roleOf(TalentedCat cat, @Nullable Player player) {
        if (player == null) return Role.STRANGER;
        if (cat.isPrimaryOwner(player)) return Role.OWNER;
        if (cat.isTrusted(player)) return Role.TRUSTED;
        return Role.STRANGER;
    }

    /** Matrix lookup only (no entity, range or session checks). */
    public static boolean allows(Role role, CatAction action) {
        return switch (role) {
            case OWNER -> true;
            case TRUSTED -> action.trustedAllowed();
            case STRANGER -> false;
        };
    }

    public static boolean can(@Nullable Player player, TalentedCat cat, CatAction action) {
        return allows(roleOf(cat, player), action);
    }

    /**
     * Full server-side check for a packet that names a cat by entity id: the entity exists in the sender's level, is a
     * living TalentedCat, is in range for the action, and the sender is allowed to do the action. For
     * {@link CatAction#TWINE_CONTROL} the sender must also be twined with exactly this cat.
     */
    public static Access check(ServerPlayer sender, int entityId, CatAction action) {
        Entity e = sender.level().getEntity(entityId);
        if (e == null) return new Access(null, Denial.NO_ENTITY);
        if (!(e instanceof TalentedCat cat)) return new Access(null, Denial.NOT_A_CAT);
        return check(sender, cat, action);
    }

    /** As {@link #check(ServerPlayer, int, CatAction)} for a cat already in hand (whistle / team scans). */
    public static Access check(ServerPlayer sender, TalentedCat cat, CatAction action) {
        if (!cat.isAlive() || cat.isRemoved() || cat.level() != sender.level()) return new Access(cat, Denial.DEAD);
        if (!can(sender, cat, action)) return new Access(cat, Denial.NOT_ALLOWED);
        if (action == CatAction.TWINE_CONTROL) {
            if (!cat.isTwinedWith(sender) || !cat.getUUID().equals(com.kittytalents.entity.TwineManager.getCatOf(sender.getUUID()))) {
                return new Access(cat, Denial.NOT_TWINED);
            }
        } else if (action.maxDistance() >= 0) {
            double max = action.maxDistance();
            if (cat.distanceToSqr(sender) > max * max) return new Access(cat, Denial.TOO_FAR);
        }
        return new Access(cat, null);
    }
}
