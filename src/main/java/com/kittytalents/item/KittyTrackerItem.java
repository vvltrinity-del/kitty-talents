package com.kittytalents.item;

import com.kittytalents.entity.TalentedCat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KittyTrackerItem extends Item {
    public KittyTrackerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(held);
        }
        player.getCooldowns().addCooldown(this, 60);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.8F, 1.3F);

        if (!level.isClientSide) {
            // Find all loaded owned or trusted cats within 256 blocks
            List<TalentedCat> cats = level.getEntitiesOfClass(
                    TalentedCat.class,
                    new AABB(player.blockPosition()).inflate(256.0D),
                    c -> c.isOwnedBy(player) || c.isTrusted(player)
            );

            player.sendSystemMessage(Component.literal("=== [ KITTY TRACKER RADAR ] ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            if (cats.isEmpty()) {
                player.sendSystemMessage(Component.literal("No companion cats detected within radar range (256 blocks).").withStyle(ChatFormatting.GRAY));
            } else {
                for (TalentedCat cat : cats) {
                    double dist = Math.sqrt(cat.distanceToSqr(player));
                    String dirStr = getDirectionString(player.getX(), player.getZ(), cat.getX(), cat.getZ());
                    String status = cat.isIncapacitated() ? (ChatFormatting.RED + "[DOWNED]") :
                            (cat.isOrderedToSit() ? (ChatFormatting.YELLOW + "[SITTING]") : (ChatFormatting.GREEN + "[ACTIVE]"));

                    Component line = Component.literal(cat.getName().getString()).withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(" - " + status + " ").withStyle(ChatFormatting.RESET))
                            .append(Component.literal(String.format("%.0fm %s ", dist, dirStr)).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(String.format("HP:%d/%d ", (int) cat.getHealth(), (int) cat.getMaxHealth())).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("Mode:" + cat.getCatMode().name()).withStyle(ChatFormatting.DARK_AQUA));

                    player.sendSystemMessage(line);
                }
            }
            player.sendSystemMessage(Component.literal("===============================").withStyle(ChatFormatting.GOLD));
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    private String getDirectionString(double px, double pz, double cx, double cz) {
        double dx = cx - px;
        double dz = cz - pz;
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        if (angle < 0) angle += 360.0D;

        if (angle >= 337.5 || angle < 22.5) return "North (↑)";
        if (angle >= 22.5 && angle < 67.5) return "North-East (↗)";
        if (angle >= 67.5 && angle < 112.5) return "East (→)";
        if (angle >= 112.5 && angle < 157.5) return "South-East (↘)";
        if (angle >= 157.5 && angle < 202.5) return "South (↓)";
        if (angle >= 202.5 && angle < 247.5) return "South-West (↙)";
        if (angle >= 247.5 && angle < 292.5) return "West (←)";
        return "North-West (↖)";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Detects and locates your owned cats within 256 blocks.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-Click: Scan nearby cats & coordinates").withStyle(ChatFormatting.AQUA));
    }
}
