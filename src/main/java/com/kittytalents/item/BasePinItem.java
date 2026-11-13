package com.kittytalents.item;

import com.kittytalents.core.base.BaseAreas;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketSyncBaseAreas;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Base Pin (5.0): marks out bases and gives cats guard posts.
 * <ul>
 * <li>Right-click two blocks: the corners of a new base area (or of the area picked for resizing in the menu).</li>
 * <li>Sneak + right-click your cat: select it. Then sneak + right-click a block: that block is its guard post (a Cat
 * Tree post keeps it perched on top). Sneak + right-click the selected cat again to clear its post.</li>
 * <li>Right-click the air: the Base Pin menu (rename, resize, merge, delete areas).</li>
 * </ul>
 * Holding the pin shows the borders of your areas.
 */
public class BasePinItem extends Item {
    public BasePinItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) PacketSyncBaseAreas.send(sp, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof TalentedCat cat) || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!cat.isOwnedBy(player)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        CompoundTag tag = held.getOrCreateTag();
        if (tag.hasUUID("Cat") && tag.getUUID("Cat").equals(cat.getUUID())) {
            tag.remove("Cat");
            if (cat.getGuardPost() != null) {
                cat.setGuardPost(null);
                player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.post_cleared", cat.getName()).withStyle(ChatFormatting.YELLOW), true);
            } else {
                player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.unselected", cat.getName()).withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.CONSUME;
        }
        tag.putUUID("Cat", cat.getUUID());
        tag.putString("CatName", cat.getName().getString());
        player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.selected", cat.getName()).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        BlockPos pos = context.getClickedPos();

        // Sneak + block with a selected cat: guard post.
        if (player.isShiftKeyDown() && tag.hasUUID("Cat")) {
            UUID id = tag.getUUID("Cat");
            Entity e = ((ServerLevel) level).getEntity(id);
            if (e instanceof TalentedCat cat && cat.isOwnedBy(player)) {
                cat.setGuardPost(pos);
                player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.post_set", cat.getName(), pos.toShortString()).withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.cat_gone", tag.getString("CatName")).withStyle(ChatFormatting.RED), true);
            }
            tag.remove("Cat");
            return InteractionResult.CONSUME;
        }

        // Corners: first click stores A, second makes (or resizes) the area.
        if (!tag.contains("CornerA")) {
            tag.put("CornerA", NbtUtils.writeBlockPos(pos));
            player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.corner_a", pos.toShortString()).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }
        BlockPos a = NbtUtils.readBlockPos(tag.getCompound("CornerA"));
        tag.remove("CornerA");
        BaseAreas bases = BaseAreas.get(sp.server);
        String dim = level.dimension().location().toString();
        if (tag.contains("ResizeId")) {
            int id = tag.getInt("ResizeId");
            tag.remove("ResizeId");
            boolean ok = bases.resize(player.getUUID(), id, dim, a, pos);
            player.displayClientMessage(Component.translatable(ok ? "message.kittytalents.base_pin.resized" : "message.kittytalents.base_pin.too_big").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        } else {
            BaseAreas.Area area = bases.add(player.getUUID(), dim, a, pos);
            if (area == null) {
                player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.too_big").withStyle(ChatFormatting.RED), true);
            } else {
                player.displayClientMessage(Component.translatable("message.kittytalents.base_pin.added", area.name(), area.sizeX(), area.sizeZ()).withStyle(ChatFormatting.GREEN), true);
            }
        }
        PacketSyncBaseAreas.send(sp, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide && selected && entity instanceof Player p && p.tickCount % 8 == 0) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.kittytalents.client.ClientBaseAreas.drawBorders(level, p));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kittytalents.base_pin.tip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.kittytalents.base_pin.tip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.kittytalents.base_pin.tip3").withStyle(ChatFormatting.GRAY));
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CatName")) {
            tooltip.add(Component.translatable("item.kittytalents.base_pin.selected", tag.getString("CatName")).withStyle(ChatFormatting.AQUA));
        }
    }
}
