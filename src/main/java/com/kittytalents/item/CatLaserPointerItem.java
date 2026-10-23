package com.kittytalents.item;

import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.TalentedCat;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class CatLaserPointerItem extends Item {
    public CatLaserPointerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Raytrace to find target position
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(32.0D));
        BlockHitResult hit = level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 targetPos = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation() : endPos;

        // Spawn red laser dot particles
        if (level.isClientSide) {
            level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.5F),
                    targetPos.x, targetPos.y + 0.1D, targetPos.z, 0, 0, 0);
        }

        // Find nearby cats owned by this player
        List<TalentedCat> cats = level.getEntitiesOfClass(
                TalentedCat.class,
                player.getBoundingBox().inflate(32.0D),
                cat -> cat.isOwnedBy(player) && !cat.isIncapacitated()
        );

        if (player.isShiftKeyDown()) {
            // Shift + click cycles AI mode for all nearby cats
            if (!level.isClientSide && !cats.isEmpty()) {
                CatMode nextMode = cats.get(0).getCatMode().next();
                for (TalentedCat cat : cats) {
                    cat.setCatMode(nextMode);
                }
                player.sendSystemMessage(Component.translatable("message.kittytalents.mode_switched", nextMode.getDisplayName()));
                player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.6F, 1.2F);
            }
        } else {
            // Normal click directs cats to navigate to the laser dot!
            for (TalentedCat cat : cats) {
                if (cat.getCatMode() != CatMode.DOCILE) {
                    cat.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.3D);
                }
            }
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4F, 1.8F);
        }

        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
