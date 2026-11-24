package com.kittytalents.item;

import com.kittytalents.block.KittyHabitatBlockEntity;
import com.kittytalents.block.KittyWorkStationBlockEntity;
import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.ProductionTier;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.work.CatJob;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Clipboard 2.0:
 * - Right-click Cat: Select cat worker (Sneak + Right-click selected cat: cycle CatJob & clock in).
 * - Sneak + Right-click Air: Toggle container link mode (Input <-> Output).
 * - Right-click Kitty Work Station: Bind selected cat to station.
 * - Right-click Kitty Production Habitat: House selected cat inside Habitat (or Sneak + Right-click to release housed cat).
 * - Sneak + Right-click Container: Link/Unlink container as Input or Output for selected cat.
 * - Right-click Block: Set Work Area Corner A, then Corner B for selected cat.
 */
public class KittyWorkClipboardItem extends Item {
    public KittyWorkClipboardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getOrCreateTag();
                boolean linkOutput = !tag.getBoolean("LinkOutputMode");
                tag.putBoolean("LinkOutputMode", linkOutput);
                player.displayClientMessage(Component.literal("Clipboard Container Link Mode: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(linkOutput ? "OUTPUT (Deposit)" : "INPUT (Supply)")
                                .withStyle(linkOutput ? ChatFormatting.AQUA : ChatFormatting.GREEN)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TalentedCat cat && com.kittytalents.core.perm.CatPermissions.can(player, cat, com.kittytalents.core.perm.CatAction.SET_JOB)) {
            if (!player.level().isClientSide) {
                CompoundTag tag = stack.getOrCreateTag();
                boolean alreadySelected = tag.hasUUID("CatUUID") && tag.getUUID("CatUUID").equals(cat.getUUID());

                if (player.isShiftKeyDown() && alreadySelected) {
                    // Cycle job on the selected cat
                    CatJob[] jobs = CatJob.values();
                    CatJob next = jobs[(cat.getCatJob().ordinal() + 1) % jobs.length];
                    cat.setCatJob(next);
                    if (next != CatJob.NONE) {
                        cat.setCatMode(CatMode.WORK);
                        cat.setClockedIn(true);
                    }
                    tag.putString("CatJob", next.getDisplayName());
                    cat.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.3F);
                    player.displayClientMessage(Component.literal(cat.getName().getString() + " Job -> ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(next.getDisplayName() + " (" + next.getDescription() + ")")
                                    .withStyle(ChatFormatting.AQUA)), true);
                    return InteractionResult.SUCCESS;
                }

                tag.putUUID("CatUUID", cat.getUUID());
                tag.putString("CatName", cat.getName().getString());
                tag.putString("CatVariant", cat.getCatVariant().getId());
                tag.putString("CatTier", cat.getProductionTier().getId());
                tag.putInt("CatLevel", cat.getCatLevel());
                tag.putBoolean("CatFemale", cat.isFemale());
                tag.putString("CatRole", cat.getCatVariant().getTraitDescription());
                tag.putString("CatMutation", cat.getMutation().name());
                tag.putString("CatJob", cat.getCatJob().getDisplayName());

                cat.playSound(SoundEvents.VILLAGER_WORK_LIBRARIAN, 1.0F, 1.2F);
                if (cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            cat.getX(), cat.getY() + 0.6D, cat.getZ(),
                            5, 0.2D, 0.2D, 0.2D, 0.05D);
                }

                player.displayClientMessage(Component.literal("Selected Worker: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(cat.getName().getString() + " [" + cat.getCatJob().getDisplayName() + " | " + cat.getProductionTier().getDisplayName() + "]").withStyle(ChatFormatting.GREEN)), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        BlockEntity be = level.getBlockEntity(pos);

        // 1. Kitty Production Habitat: house or release cat
        if (be instanceof KittyHabitatBlockEntity habitat) {
            if (!level.isClientSide) {
                if (habitat.hasStoredCat() && (player != null && player.isShiftKeyDown() || !hasSelectedCat(tag))) {
                    String releasedName = habitat.getAssignedCatName();
                    habitat.releaseHousedCat(pos.above());
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Released " + releasedName + " from Production Habitat!").withStyle(ChatFormatting.AQUA), true);
                    }
                    return InteractionResult.SUCCESS;
                }
                TalentedCat activeCat = resolveSelectedCat(level, tag);
                if (activeCat != null) {
                    String catName = activeCat.getName().getString();
                    habitat.houseWorker(activeCat);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.BLOCKS, 1.0F, 1.5F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Housed " + catName + " inside Production Habitat! (Sneak-click with Clipboard to release)").withStyle(ChatFormatting.GREEN), true);
                    }
                    return InteractionResult.SUCCESS;
                } else if (hasSelectedCat(tag)) {
                    // Fallback metadata assignment if cat entity wasn't nearby
                    UUID catUUID = tag.hasUUID("CatUUID") ? tag.getUUID("CatUUID") : UUID.fromString(tag.getString("CatUUID"));
                    String catName = tag.getString("CatName");
                    CatVariant variant = CatVariant.fromId(tag.getString("CatVariant"));
                    ProductionTier tier = tag.contains("CatTier") ? ProductionTier.fromId(tag.getString("CatTier")) : ProductionTier.STANDARD;
                    int catLvl = tag.getInt("CatLevel");
                    boolean female = tag.getBoolean("CatFemale");
                    String role = tag.getString("CatRole");
                    String mutation = tag.contains("CatMutation") ? tag.getString("CatMutation") : "NONE";
                    habitat.assignWorker(catUUID, catName, variant, tier, catLvl, female, role, mutation);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.BLOCKS, 1.0F, 1.5F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Worker " + catName + " assigned to Production Habitat").withStyle(ChatFormatting.GREEN), true);
                    }
                    return InteractionResult.SUCCESS;
                } else if (player != null) {
                    player.displayClientMessage(Component.literal("No cat recorded on clipboard! Right-click an owned Talented Cat first.").withStyle(ChatFormatting.RED), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. Kitty Work Station: bind selected worker cat
        if (be instanceof KittyWorkStationBlockEntity station) {
            if (!level.isClientSide) {
                TalentedCat activeCat = resolveSelectedCat(level, tag);
                if (activeCat != null) {
                    station.bindWorker(activeCat);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Bound " + activeCat.getName().getString() + " to Kitty Work Station at " + pos.toShortString()).withStyle(ChatFormatting.GREEN), true);
                    }
                } else if (player != null) {
                    player.displayClientMessage(Component.literal("Select a nearby Talented Cat with the clipboard first!").withStyle(ChatFormatting.RED), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 3. Sneak + Right-click any container with ITEM_HANDLER: link as Input or Output
        if (player != null && player.isShiftKeyDown() && be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent()) {
            if (!level.isClientSide) {
                TalentedCat activeCat = resolveSelectedCat(level, tag);
                if (activeCat != null) {
                    boolean outputMode = tag.getBoolean("LinkOutputMode");
                    boolean added = outputMode
                            ? activeCat.getJobData().addLinkedOutput(pos)
                            : activeCat.getJobData().addLinkedInput(pos);
                    String kind = outputMode ? "Output" : "Input";
                    player.displayClientMessage(Component.literal((added ? "Linked " : "Unlinked ") + kind + " container at " + pos.toShortString() + " for " + activeCat.getName().getString())
                            .withStyle(added ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7F, added ? 1.5F : 0.8F);
                } else {
                    player.displayClientMessage(Component.literal("Select a nearby Talented Cat first to link containers!").withStyle(ChatFormatting.RED), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 4. Right-click block: Set Work Area Corner A -> Corner B
        if (hasSelectedCat(tag)) {
            if (!level.isClientSide) {
                TalentedCat activeCat = resolveSelectedCat(level, tag);
                if (activeCat == null) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Selected cat must be loaded nearby to set work area.").withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (!tag.contains("CornerA")) {
                    tag.put("CornerA", NbtUtils.writeBlockPos(pos));
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Work Area Corner A set to " + pos.toShortString() + ". Right-click second corner!").withStyle(ChatFormatting.AQUA), true);
                    }
                } else {
                    BlockPos cornerA = NbtUtils.readBlockPos(tag.getCompound("CornerA"));
                    tag.remove("CornerA");
                    int maxR = KittyTalentsConfig.COMMON.maxStationRadius.get();
                    if (Math.abs(pos.getX() - cornerA.getX()) > maxR * 2 ||
                        Math.abs(pos.getZ() - cornerA.getZ()) > maxR * 2) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("Work area exceeds max radius (" + maxR + " blocks)!").withStyle(ChatFormatting.RED), true);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    activeCat.getJobData().setWorkAreaCorners(cornerA, pos);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("Work Area set for " + activeCat.getName().getString() + ": [" + cornerA.toShortString() + " -> " + pos.toShortString() + "]").withStyle(ChatFormatting.GREEN), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    private static boolean hasSelectedCat(@Nullable CompoundTag tag) {
        return tag != null && (tag.hasUUID("CatUUID") || tag.contains("CatUUID"));
    }

    @Nullable
    private static TalentedCat resolveSelectedCat(Level level, @Nullable CompoundTag tag) {
        if (!hasSelectedCat(tag) || !(level instanceof ServerLevel sl)) return null;
        try {
            UUID id = tag.hasUUID("CatUUID") ? tag.getUUID("CatUUID") : UUID.fromString(tag.getString("CatUUID"));
            Entity e = sl.getEntity(id);
            if (e instanceof TalentedCat cat) return cat;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (hasSelectedCat(tag)) {
            tooltip.add(Component.literal("=== SELECTED CAT WORKER ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltip.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tag.getString("CatName")).withStyle(ChatFormatting.WHITE)));

            if (tag.contains("CatJob")) {
                tooltip.add(Component.literal("Active Job: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(tag.getString("CatJob")).withStyle(ChatFormatting.GREEN)));
            }

            CatVariant variant = CatVariant.fromId(tag.getString("CatVariant"));
            tooltip.add(Component.literal("Breed: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(variant.getDisplayName()).withStyle(ChatFormatting.AQUA)));

            ProductionTier tier = tag.contains("CatTier") ? ProductionTier.fromId(tag.getString("CatTier")) : ProductionTier.STANDARD;
            tooltip.add(Component.literal("Production Tier: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tier.getDisplayName()).withStyle(tier.getFormatting())));

            tooltip.add(Component.literal("Level: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(tag.getInt("CatLevel"))).withStyle(ChatFormatting.YELLOW)));

            boolean linkOut = tag.getBoolean("LinkOutputMode");
            tooltip.add(Component.literal("Link Mode: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(linkOut ? "OUTPUT" : "INPUT").withStyle(linkOut ? ChatFormatting.AQUA : ChatFormatting.GREEN)));

            tooltip.add(Component.literal("• Sneak+Click Cat: Cycle Job").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("• Click Work Station / Habitat: Bind or House").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("• Sneak+Click Container: Link Input/Output").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Kitty Work Clipboard").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Right-click an owned Talented Cat to select worker.").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Bind to Work Stations, Habitats, or link chests.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
