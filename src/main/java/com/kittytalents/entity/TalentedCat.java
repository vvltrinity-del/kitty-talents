package com.kittytalents.entity;

import com.kittytalents.core.component.CatCombatState;
import com.kittytalents.core.component.CatEquipment;
import com.kittytalents.core.component.CatStatistics;
import com.kittytalents.core.component.CatTalentLevels;
import com.kittytalents.core.component.CatTrust;
import com.kittytalents.core.persist.CatSchema;

import com.kittytalents.config.KittyTalentsConfig;
import com.kittytalents.entity.ai.*;
import com.kittytalents.inventory.CatPackMenu;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.item.CatTreatItem;
import com.kittytalents.item.WarmMilkItem;
import com.kittytalents.registry.ModEntities;
import com.kittytalents.registry.ModItems;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketSyncCatEquipment;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TalentedCat extends Cat implements PlayerRideableJumping {
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POINTS =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_INCAPACITATED =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SKIN_ID =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DIRE_AWAKENED =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FEMALE =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_COSMETICS_VISIBLE =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_VARIANT =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_RESOURCE_TIER =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_MUTATION =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_TWINED =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_JOB =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_CLOCKED_IN =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_WORK_STATUS =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_LAST_SHOT_TICK =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    /** Active statuses for the HUDs: {TYPE: {End, Power}} (see FelineStatusEffects#syncToClient). */
    private static final EntityDataAccessor<net.minecraft.nbt.CompoundTag> DATA_STATUSES =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> DATA_XP =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY =
            SynchedEntityData.defineId(TalentedCat.class, EntityDataSerializers.INT);

    private boolean isDualStriking = false;

    @Nullable
    private UUID twinedPlayerUUID = null;
    private float twineXxa = 0.0F;
    private float twineZza = 0.0F;

    // ---- Components (Phase 2). The public accessors below are thin delegates kept for API compatibility. ----
    private final CatTalentLevels talents = new CatTalentLevels();
    private final CatEquipment equipment = new CatEquipment();
    private final CatStatistics stats = new CatStatistics();
    private final CatTrust trust = new CatTrust();
    private final CatCombatState combat = new CatCombatState();

    public CatTalentLevels talents() { return talents; }
    public CatEquipment equipment() { return equipment; }
    public CatStatistics stats() { return stats; }
    public CatTrust trust() { return trust; }
    public CatCombatState combat() { return combat; }

    /** Transient per-talent cooldowns ("talentId:key" -> game time ready). See TalentContext#ready. */
    private final java.util.Map<String, Long> talentCooldowns = new java.util.HashMap<>();
    public java.util.Map<String, Long> talentCooldowns() { return talentCooldowns; }

    public boolean isFrenzied() { return frenzyTimer > 0; }

    // ---- One-shot animations (Phase 5). Server broadcasts an entity event; clients time the pose locally. ----
    // IDs 110-113: vanilla claims 60 (death poof) and 63 (handled by ClientPacketListener as the Sniffer dig sound,
    // which threw "TalentedCat cannot be cast to Sniffer" on every groom).
    public static final byte ANIM_POUNCE = 110, ANIM_DODGE = 111, ANIM_CAST = 112, ANIM_GROOM = 113;
    private byte animType = 0;
    private int animStartTick = -10_000;
    public byte getAnimType() { return animType; }
    public int getAnimStartTick() { return animStartTick; }

    /** Server: play a one-shot animation on every client tracking this cat. */
    public void playAnimation(byte anim) {
        if (!this.level().isClientSide) this.level().broadcastEntityEvent(this, anim);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= ANIM_POUNCE && id <= ANIM_GROOM) {
            this.animType = id;
            this.animStartTick = this.tickCount;
            return;
        }
        super.handleEntityEvent(id);
    }

    /** XP towards the next level (Phase 4). Synced for the UI. */
    public net.minecraft.nbt.CompoundTag getSyncedStatuses() { return this.entityData.get(DATA_STATUSES); }
    public void setSyncedStatuses(net.minecraft.nbt.CompoundTag view) { this.entityData.set(DATA_STATUSES, view); }

    public int getCatXp() { return this.entityData.get(DATA_XP); }
    public void setCatXp(int xp) { this.entityData.set(DATA_XP, Math.max(0, xp)); }

    private final com.kittytalents.work.JobData jobData = new com.kittytalents.work.JobData();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> catItemCapability =
            net.minecraftforge.common.util.LazyOptional.of(() -> new com.kittytalents.work.CatAmmoAndPackItemHandler(this));


    @Nullable
    private BlockPos homeBedPos = null;
    private String homeBedDimension = "";

    private int hungerTimer = 0;
    private int frenzyCooldown = 0;
    private int frenzyTimer = 0;
    private int quietUntilTick = 0;


    private int energy = 100;

    public com.kittytalents.combat.CatRole getCatRole() { return combat.getRole(); }
    public void setCatRole(com.kittytalents.combat.CatRole role) { combat.setRole(role); }
    public com.kittytalents.combat.FelineElement getPrimaryElement() { return combat.getPrimaryElement(); }
    public void setPrimaryElement(com.kittytalents.combat.FelineElement element) { combat.setPrimaryElement(element); }
    public com.kittytalents.combat.FelineElement getSecondaryElement() { return combat.getSecondaryElement(); }
    public void setSecondaryElement(com.kittytalents.combat.FelineElement element) { combat.setSecondaryElement(element); }

    public String getAbilitySlot1() { return combat.getSlot1(); }
    public void setAbilitySlot1(String slot) { combat.setSlot1(slot); }
    public String getAbilitySlot2() { return combat.getSlot2(); }
    public void setAbilitySlot2(String slot) { combat.setSlot2(slot); }
    public String getAbilitySlot3() { return combat.getSlot3(); }
    public void setAbilitySlot3(String slot) { combat.setSlot3(slot); }
    public String getAbilityUltimate() { return combat.getUltimate(); }
    public void setAbilityUltimate(String slot) { combat.setUltimate(slot); }

    public int getEnergy() { return this.entityData != null ? this.entityData.get(DATA_ENERGY) : energy; }
    public int getMaxEnergy() { return com.kittytalents.combat.FelineEnergyEngine.getMaxEnergy(this); }
    public void setEnergy(int energy) {
        this.energy = Math.max(0, energy);
        if (this.entityData != null) {
            this.entityData.set(DATA_ENERGY, this.energy);
        }
    }

    public int getOutOfCombatTicks() { return combat.outOfCombatTicks; }
    public void setOutOfCombatTicks(int val) { combat.outOfCombatTicks = val; }
    public void incrementOutOfCombatTicks() { combat.outOfCombatTicks++; }
    public void resetOutOfCombatTicks() { combat.outOfCombatTicks = 0; }

    public long getNineLivesReadyAt() { return combat.getNineLivesReadyAt(); }

    public long getAbilityCooldown(String id) { return combat.getCooldown(id); }
    public void setAbilityCooldown(String id, long gameTime) { combat.setCooldown(id, gameTime); }

    /** Fed Spirit Catnip and not yet bred: the next offspring is guaranteed Nekomata. */
    private boolean spiritCatnipPrimed = false;
    public boolean isSpiritCatnipPrimed() { return spiritCatnipPrimed; }
    public void setSpiritCatnipPrimed(boolean primed) { this.spiritCatnipPrimed = primed; }

    /** Transient (not saved): last Twine right-click pounce, for its 1-second cooldown. */
    public long getLastTwinePounceTick() { return combat.lastTwinePounceTick; }
    public void setLastTwinePounceTick(long tick) { combat.lastTwinePounceTick = tick; }

    public ItemStack getCarriedToy() { return equipment.getCarriedToy(); }
    public void setCarriedToy(ItemStack toy) { equipment.setCarriedToy(toy); }

    public TalentedCat(EntityType<? extends Cat> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new TalentedCatMoveControl(this);
        this.lookControl = new TalentedCatLookControl(this);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.FENCE, 0.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DOOR_OPEN, 0.0F);
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Cat.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
        com.kittytalents.compat.apotheosis.ApotheosisCompat.registerAttributes(builder);
        return builder;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LEVEL, 1);
        this.entityData.define(DATA_POINTS, 0);
        this.entityData.define(DATA_HUNGER, 100);
        this.entityData.define(DATA_MODE, CatMode.DOCILE.ordinal());
        this.entityData.define(DATA_INCAPACITATED, false);
        this.entityData.define(DATA_SKIN_ID, 0);
        this.entityData.define(DATA_DIRE_AWAKENED, false);
        this.entityData.define(DATA_FEMALE, false);
        this.entityData.define(DATA_COSMETICS_VISIBLE, true);
        this.entityData.define(DATA_VARIANT, CatVariant.TABBY.getId());
        this.entityData.define(DATA_RESOURCE_TIER, ProductionTier.STANDARD.getId());
        this.entityData.define(DATA_MUTATION, CatBreedingManager.MutationType.NONE.name());
        this.entityData.define(DATA_TWINED, false);
        this.entityData.define(DATA_JOB, (byte) com.kittytalents.work.CatJob.NONE.ordinal());
        this.entityData.define(DATA_CLOCKED_IN, true);
        this.entityData.define(DATA_WORK_STATUS, "Idle");
        this.entityData.define(DATA_LAST_SHOT_TICK, -100);
        this.entityData.define(DATA_ENERGY, 100);
        this.entityData.define(DATA_XP, 0);
        this.entityData.define(DATA_STATUSES, new net.minecraft.nbt.CompoundTag());
    }

    @Override
    protected void registerGoals() {
        // All AI is assembled in one place, in documented priority bands (Phase 3).
        com.kittytalents.ai.CatBrain.install(this);
    }

    public int getCatLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public void setCatLevel(int level) {
        int maxLvl = KittyTalentsConfig.COMMON.maxCatLevel.get();
        this.entityData.set(DATA_LEVEL, Math.max(1, Math.min(maxLvl, level)));
        updateAttributes();
    }

    public int getSkillPoints() {
        return this.entityData.get(DATA_POINTS);
    }

    public void setSkillPoints(int points) {
        this.entityData.set(DATA_POINTS, Math.max(0, points));
    }

    public int getHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(DATA_HUNGER, Math.max(0, Math.min(120, hunger)));
    }

    public void increaseHunger(int amount) {
        setHunger(getHunger() + amount);
        stats.addFoodConsumed(Math.max(1, amount / 10));
    }

    private float satiety = 0.0F;

    public float getSatiety() {
        return satiety;
    }

    /** Applies hunger, healing and saturation for one consumed food item (server-side). */
    public void applyFood(int hunger, float saturation, float heal) {
        increaseHunger(hunger);
        this.satiety = Math.min(40.0F, this.satiety + Math.max(0.0F, saturation));
        if (heal > 0.0F) this.heal(heal);
    }

    public CatMode getCatMode() {
        return CatMode.byId(this.entityData.get(DATA_MODE));
    }

    /** Where STAY / GUARD hold. Set when such a mode is chosen; saved with the cat. */
    @Nullable private BlockPos modeAnchor = null;
    @Nullable public BlockPos getModeAnchor() { return modeAnchor; }
    public void setModeAnchor(@Nullable BlockPos pos) { this.modeAnchor = pos; }

    /** Transient: true while CatRetreatGoal is running (blocks targeting and combat goals). */
    private boolean retreating = false;
    public boolean isRetreating() { return retreating; }
    public void setRetreating(boolean value) { this.retreating = value; }

    public void setCatMode(CatMode mode) {
        CatMode previous = getCatMode();
        this.entityData.set(DATA_MODE, mode.ordinal());
        // Anchored modes hold where they were ordered; re-anchor when switching into one.
        if (mode.usesAnchor() && (previous != mode || modeAnchor == null)) {
            this.modeAnchor = this.blockPosition();
        }
        // A clocked-in cat is always in WORK mode, so leaving WORK clocks it out.
        if (mode != CatMode.WORK && !this.level().isClientSide && jobData.isClockedIn()) {
            setClockedIn(false);
        }
        if (mode == CatMode.DOCILE) {
            this.setOrderedToSit(true);
            this.setTarget(null);
            this.getNavigation().stop();
        } else {
            this.setOrderedToSit(false);
        }
    }

    public boolean isIncapacitated() {
        return this.entityData.get(DATA_INCAPACITATED);
    }

    public void setIncapacitated(boolean value) {
        this.entityData.set(DATA_INCAPACITATED, value);
        if (value) {
            this.setOrderedToSit(true);
            this.setHealth(1.0F);
            this.setTarget(null);
        }
    }

    public int getSkinId() {
        return this.entityData.get(DATA_SKIN_ID);
    }

    public void setSkinId(int id) {
        this.entityData.set(DATA_SKIN_ID, Math.max(0, Math.min(16, id)));
    }

    public CatVariant getCatVariant() {
        return CatVariant.fromId(this.entityData.get(DATA_VARIANT));
    }

    public void setCatVariant(CatVariant variant) {
        this.entityData.set(DATA_VARIANT, variant != null ? variant.getId() : CatVariant.TABBY.getId());
    }

    public ProductionTier getProductionTier() {
        return ProductionTier.fromId(this.entityData.get(DATA_RESOURCE_TIER));
    }

    public void setProductionTier(ProductionTier tier) {
        this.entityData.set(DATA_RESOURCE_TIER, tier != null ? tier.getId() : ProductionTier.STANDARD.getId());
        if (!this.level().isClientSide) updateAttributes(); // tier scales combat stats
    }

    public CatBreedingManager.MutationType getMutation() {
        return CatBreedingManager.MutationType.fromString(this.entityData.get(DATA_MUTATION));
    }

    public void setMutation(CatBreedingManager.MutationType mutation) {
        String name = mutation != null ? mutation.name() : CatBreedingManager.MutationType.NONE.name();
        this.entityData.set(DATA_MUTATION, name);
    }

    public void setNekomata(boolean nekomata) {
        if (nekomata) {
            setMutation(CatBreedingManager.MutationType.NEKOMATA);
            setDireAwakened(true);
        } else if (getMutation() == CatBreedingManager.MutationType.NEKOMATA) {
            setMutation(CatBreedingManager.MutationType.NONE);
            setDireAwakened(false);
        }
    }

    public boolean isNekomata() {
        return getMutation() == CatBreedingManager.MutationType.NEKOMATA || isDireAwakened();
    }

    public boolean tryPlaceTorchManual() {
        if (this.isIncapacitated()) return false;
        ItemStack torchStack = ItemStack.EMPTY;
        int packSlot = -1;
        boolean inToolSlot = false;
        if (!getEquippedTool().isEmpty() && (getEquippedTool().is(net.minecraft.world.item.Items.TORCH) || getEquippedTool().is(net.minecraft.world.item.Items.SOUL_TORCH))) {
            torchStack = getEquippedTool();
            inToolSlot = true;
        } else {
            for (int i = 0; i < equipment.pack().getContainerSize(); i++) {
                ItemStack st = equipment.pack().getItem(i);
                if (!st.isEmpty() && (st.is(net.minecraft.world.item.Items.TORCH) || st.is(net.minecraft.world.item.Items.SOUL_TORCH))) {
                    torchStack = st;
                    packSlot = i;
                    break;
                }
            }
        }
        if (torchStack.isEmpty()) return false;

        BlockPos pos = this.blockPosition();
        BlockPos targetPos = null;
        if (this.level().isEmptyBlock(pos) && net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState().canSurvive(this.level(), pos)) {
            targetPos = pos;
        } else {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos offset = pos.relative(dir);
                if (this.level().isEmptyBlock(offset) && net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState().canSurvive(this.level(), offset)) {
                    targetPos = offset;
                    break;
                }
            }
        }

        if (targetPos != null) {
            net.minecraft.world.level.block.state.BlockState torchState = torchStack.is(net.minecraft.world.item.Items.SOUL_TORCH)
                    ? net.minecraft.world.level.block.Blocks.SOUL_TORCH.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState();
            this.level().setBlock(targetPos, torchState, 3);
            this.level().playSound(null, targetPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME, targetPos.getX() + 0.5D, targetPos.getY() + 0.6D, targetPos.getZ() + 0.5D, 5, 0.1D, 0.1D, 0.1D, 0.02D);
            }
            if (com.kittytalents.talent.effect.TalentEngine.isMastered(this, Talents.TORCH_KITTY)) {
                // Torch Kitty mastery "Everburning": torches are free.
            } else if (inToolSlot) {
                getEquippedTool().shrink(1);
                if (getEquippedTool().isEmpty()) setEquippedTool(ItemStack.EMPTY);
            } else if (packSlot >= 0) {
                equipment.pack().removeItem(packSlot, 1);
            }
            return true;
        }
        return false;
    }

    public boolean tryUseEquippedTool() {
        if (this.isIncapacitated() || getEquippedTool().isEmpty()) return false;
        BlockPos frontPos = this.blockPosition().relative(this.getDirection());
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(frontPos);
        if (!state.isAir()) {
            if (getEquippedTool().getItem() instanceof net.minecraft.world.item.DiggerItem digger) {
                if (digger.isCorrectToolForDrops(state)) {
                    this.level().destroyBlock(frontPos, true, this);
                    getEquippedTool().hurtAndBreak(1, this, c -> {
                        c.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
                        c.setEquippedTool(ItemStack.EMPTY);
                    });
                    this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.2F);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        Component base = super.getDisplayName();
        ProductionTier tier = getProductionTier();
        return base.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(tier.getColorValue()));
    }

    @Override
    public Component getCustomName() {
        Component base = super.getCustomName();
        if (base != null) {
            ProductionTier tier = getProductionTier();
            return base.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(tier.getColorValue()));
        }
        return null;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.85F;
    }

    public UUID getTwinedPlayerUUID() {
        return twinedPlayerUUID;
    }

    public boolean isTwined() {
        return this.entityData.get(DATA_TWINED) || twinedPlayerUUID != null;
    }

    public boolean isTwinedWith(Player player) {
        return twinedPlayerUUID != null && twinedPlayerUUID.equals(player.getUUID());
    }

    public void setTwinedPlayer(@Nullable Player player) {
        this.twinedPlayerUUID = player != null ? player.getUUID() : null;
        this.entityData.set(DATA_TWINED, player != null);
        if (player != null) {
            this.setOrderedToSit(false);
            this.setInSittingPose(false);
            this.getNavigation().stop();
            this.setTarget(null);
            TwineManager.register(player.getUUID(), this.getUUID());
        } else {
            this.twineXxa = 0.0F;
            this.twineZza = 0.0F;
        }
        if (!this.level().isClientSide) updateControlFlags(); // apply now, not on the next 5-tick refresh
    }

    /**
     * While twined the player is the only driver: AI goals lose MOVE / LOOK / JUMP and target goals lose TARGET,
     * so running goals are stopped and new ones cannot start. (customServerAiStep runs AFTER the goal selectors,
     * so returning early there never stopped goals from pathing, turning or attacking on their own.)
     */
    @Override
    protected void updateControlFlags() {
        if (isTwined()) {
            this.goalSelector.setControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE, false);
            this.goalSelector.setControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP, false);
            this.goalSelector.setControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK, false);
            this.targetSelector.setControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET, false);
            return;
        }
        // Vanilla 1.20.1 never touches TARGET here, so we must re-enable it ourselves after a Twine session.
        this.targetSelector.setControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET, true);
        super.updateControlFlags();
    }

    /** Clears local twine state only (session registry is handled by TwineManager). */
    public void clearTwineState() {
        this.twinedPlayerUUID = null;
        this.entityData.set(DATA_TWINED, false);
        this.twineXxa = 0.0F;
        this.twineZza = 0.0F;
        this.twineJump = false;
        if (!this.level().isClientSide) updateControlFlags(); // hand control back to the AI immediately
    }

    public void setTwineInput(float xxa, float zza) {
        this.twineXxa = xxa;
        this.twineZza = zza;
    }

    /** Jump/Space held while Twined (Sky Feline climb). Set by PacketTwineInput and by the client's own prediction. */
    private boolean twineJump;
    public void setTwineJump(boolean held) { this.twineJump = held; }

    /** A Twined cat can range this far (horizontally) from the player's sleeping body: chunks and tracking follow the body. */
    public static final double TWINE_LEASH = 64.0D;

    @Nullable
    private Vec3 twineBodyPos() {
        if (this.level().isClientSide) {
            return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.kittytalents.client.twine.ClientTwineState.bodyPosition());
        }
        Player body = twinedPlayerUUID == null ? null : this.level().getPlayerByUUID(twinedPlayerUUID);
        return body != null ? body.position() : null;
    }

    /** Stops a Twined cat at the leash instead of letting it leave the loaded area (which ended the session). */
    private void applyTwineLeash() {
        Vec3 body = twineBodyPos();
        if (body == null) return;
        double dx = this.getX() - body.x, dz = this.getZ() - body.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= TWINE_LEASH || dist < 1.0E-4D) return;
        double nx = dx / dist, nz = dz / dist;
        Vec3 m = this.getDeltaMovement();
        double outward = m.x * nx + m.z * nz;
        double pull = Math.min(0.2D, (dist - TWINE_LEASH) * 0.05D);
        double cancel = Math.max(0.0D, outward) + pull;
        this.setDeltaMovement(m.x - nx * cancel, m.y, m.z - nz * cancel);
        Player p = !this.level().isClientSide && this.tickCount % 40 == 0 && twinedPlayerUUID != null
                ? this.level().getPlayerByUUID(twinedPlayerUUID) : null;
        if (p != null) {
            p.displayClientMessage(Component.translatable("message.kittytalents.twine_leash").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    @Override
    protected float getFlyingSpeed() {
        // Twined Sky Feline: real air control (vanilla mobs get almost none in the air).
        if (isTwined() && !this.onGround() && getTalentLevel(Talents.SKY_FELINE) > 0) return SkyFlight.airAcceleration(this);
        return super.getFlyingSpeed();
    }

    public float getTwineXxa() {
        return this.twineXxa;
    }

    public float getTwineZza() {
        return this.twineZza;
    }

    public static class TalentedCatMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private final TalentedCat cat;

        public TalentedCatMoveControl(TalentedCat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public void tick() {
            if (this.cat.isTwined()) {
                float speed = (float) this.cat.getAttributeValue(Attributes.MOVEMENT_SPEED);
                if (this.cat.isSprinting()) speed *= 1.35F;
                this.cat.setSpeed(speed);
                this.cat.setZza(this.cat.twineZza);
                this.cat.setXxa(this.cat.twineXxa);
                return;
            }
            super.tick();
        }
    }

    public static class TalentedCatLookControl extends net.minecraft.world.entity.ai.control.LookControl {
        private final TalentedCat cat;

        public TalentedCatLookControl(TalentedCat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public void tick() {
            if (this.cat.isTwined()) {
                return;
            }
            super.tick();
        }
    }

    @Override
    public void customServerAiStep() {
        if (isTwined()) {
            // Player drives the cat: skip goals/navigation so AI cannot zero or fight the input.
            this.getNavigation().stop();
            this.setOrderedToSit(false);
            this.setInSittingPose(false);
            this.xxa = this.twineXxa;
            this.zza = this.twineZza;
            this.yya = 0.0F;
            float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (this.isSprinting()) speed *= 1.35F;
            this.setSpeed(speed);
            return;
        }
        super.customServerAiStep();

        if (this.onGround() && !this.isOrderedToSit() && !this.isIncapacitated()) {
            if (this.horizontalCollision) {
                this.getJumpControl().jump();
            } else if (this.getDeltaMovement().horizontalDistanceSqr() > 0.03D) {
                BlockPos ahead = this.blockPosition().relative(this.getDirection());
                if (this.level().isEmptyBlock(ahead) && this.level().isEmptyBlock(ahead.below())) {
                    Vec3 dir = this.getLookAngle();
                    this.setDeltaMovement(this.getDeltaMovement().x + dir.x * 0.22D, 0.42D, this.getDeltaMovement().z + dir.z * 0.22D);
                    this.hasImpulse = true;
                }
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide && isTwined()) {
            TwineManager.endForCat(this, "Your cat is gone - untwined.");
        }
        super.remove(reason);
    }

    public void doJump() {
        this.jumpFromGround();
    }

    public boolean isDireAwakened() {
        return this.entityData.get(DATA_DIRE_AWAKENED);
    }

    public void setDireAwakened(boolean val) {
        this.entityData.set(DATA_DIRE_AWAKENED, val);
    }

    /** Standard talents (Talent#isStandard) are free at this rank on every cat. */
    public static final int STANDARD_TALENT_BASELINE = 5;

    public int getTalentLevel(Talent talent) {
        if (talent == null) return 0;
        if (talent.isStandard()) {
            return Math.max(1, talents.getOrDefault(talent.getId(), STANDARD_TALENT_BASELINE));
        }
        return talents.getOrDefault(talent.getId(), 0);
    }

    public void setAllTalentLevels(Map<String, Integer> levels) {
        talents.replaceAll(levels);
    }

    public Map<String, Integer> getAllTalentLevels() {
        return talents.view();
    }

    public void syncTalents(ServerPlayer player) {
        if (!this.level().isClientSide && player != null) {
            com.kittytalents.network.PacketSyncTalents packet =
                    new com.kittytalents.network.PacketSyncTalents(this.getId(), this.getSkillPoints(), talents.view());
            PacketHandler.sendToPlayer(packet, player);
        }
    }

    public void syncTalents() {
        if (!this.level().isClientSide) {
            com.kittytalents.network.PacketSyncTalents packet =
                    new com.kittytalents.network.PacketSyncTalents(this.getId(), this.getSkillPoints(), talents.view());
            PacketHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    packet
            );
        }
    }

    public void setTalentLevel(Talent talent, int level) {
        talents.set(talent.getId(), Math.min(talent.getMaxLevel(), level));
        updateAttributes();
        this.refreshDimensions();
        syncTalents();
    }

    /** Skill-point cost of training {@code talent} up to {@code rank}. The single source for training and respec. */
    public static int costOfRank(Talent talent, int rank) {
        if (talent.requiresDireTreat()) return rank * 3;
        if (talent.getMaxLevel() <= 5) return rank <= 2 ? 1 : 2;
        if (rank <= 2) return 1;
        if (rank <= 5) return 2;
        return 3;
    }

    /** Points needed to take {@code talent} from its free baseline (standard talents start at rank 5) to max. */
    public static int costToMax(Talent talent) {
        int total = 0;
        for (int r = (talent.isStandard() ? STANDARD_TALENT_BASELINE : 0) + 1; r <= talent.getMaxLevel(); r++) {
            total += costOfRank(talent, r);
        }
        return total;
    }

    /** Starting skill points for a newly tamed cat: enough to max any one non-Dire talent (currently 17). */
    public static int startingSkillPoints() {
        int max = 0;
        for (Talent t : Talents.getAll()) {
            if (!t.requiresDireTreat()) max = Math.max(max, costToMax(t));
        }
        return max;
    }

    /** Whether this cat already received its starting skill points (saved). */
    private boolean startingPointsGranted = false;

    private void grantStartingPoints() {
        if (startingPointsGranted || this.level().isClientSide || !this.isTame()) return;
        startingPointsGranted = true;
        setSkillPoints(getSkillPoints() + startingSkillPoints());
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        grantStartingPoints();
    }

    public int getCostForNextRank(Talent talent) {
        int currentLvl = getTalentLevel(talent);
        if (currentLvl >= talent.getMaxLevel()) return 0;
        return costOfRank(talent, currentLvl + 1);
    }

    public boolean canTrainTalent(Talent talent) {
        int currentLvl = getTalentLevel(talent);
        if (currentLvl >= talent.getMaxLevel()) return false;
        if (talent.requiresDireTreat() && !isDireAwakened()) return false;
        int cost = getCostForNextRank(talent);
        return getSkillPoints() >= cost;
    }

    public boolean trainTalent(Talent talent) {
        if (!canTrainTalent(talent)) return false;
        int cost = getCostForNextRank(talent);
        setSkillPoints(getSkillPoints() - cost);
        setTalentLevel(talent, getTalentLevel(talent) + 1);
        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
        return true;
    }

    public int respecAllTalents() {
        int refundedPoints = 0;
        for (Talent talent : Talents.getAll()) {
            int lvl = getTalentLevel(talent);
            // Standard talents are free up to the baseline; refunding those ranks minted free skill points every respec.
            int freeRanks = talent.isStandard() ? STANDARD_TALENT_BASELINE : 0;
            for (int r = freeRanks + 1; r <= lvl; r++) {
                refundedPoints += costOfRank(talent, r);
            }
        }
        talents.clear();
        setSkillPoints(getSkillPoints() + refundedPoints);
        updateAttributes();
        this.refreshDimensions();
        this.playSound(SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.0F);
        syncTalents();
        return refundedPoints;
    }

    public int respecTalents() {
        return respecAllTalents();
    }

    // Equipment, Tools & Accessories
    public ItemStack getEquippedWeapon() { return equipment.get(CatEquipment.Slot.WEAPON); }
    public void setEquippedWeapon(ItemStack stack) {
        equipment.set(CatEquipment.Slot.WEAPON, stack);
        updateAttributes();
        syncEquipment();
        ItemStack weapon = getEquippedWeapon();
        if (!this.level().isClientSide && com.kittytalents.compat.tacz.TacZCompat.isGun(weapon)) {
            com.kittytalents.compat.tacz.TacZCompat.onGunEquipped(this, weapon);
        }
    }
    public void setEquippedWeaponClient(ItemStack stack) { equipment.set(CatEquipment.Slot.WEAPON, stack); updateAttributes(); }

    public ItemStack getEquippedTool() { return equipment.get(CatEquipment.Slot.TOOL); }
    public void setEquippedTool(ItemStack stack) { equipment.set(CatEquipment.Slot.TOOL, stack); syncEquipment(); }
    public void setEquippedToolClient(ItemStack stack) { equipment.set(CatEquipment.Slot.TOOL, stack); }

    /** @param slot 1 = helmet, 2 = chest, 3 = legs, 4 = boots */
    public ItemStack getArmorSlotItem(int slot) { return equipment.get(CatEquipment.Slot.armor(slot)); }
    public void setArmorSlotItem(int slot, ItemStack stack) {
        equipment.set(CatEquipment.Slot.armor(slot), stack);
        updateAttributes();
        syncEquipment();
    }
    public void setArmorSlotItemClient(int slot, ItemStack stack) {
        equipment.set(CatEquipment.Slot.armor(slot), stack);
        updateAttributes();
    }

    public boolean hasEquippedGun() {
        return com.kittytalents.compat.tacz.TacZCompat.isGun(getEquippedWeapon());
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> getEquippedWeapon();
            case OFFHAND -> getEquippedTool();
            case HEAD -> getArmorSlotItem(1);
            case CHEST -> getArmorSlotItem(2);
            case LEGS -> getArmorSlotItem(3);
            case FEET -> getArmorSlotItem(4);
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        switch (slot) {
            case MAINHAND -> setEquippedWeapon(stack);
            case OFFHAND -> setEquippedTool(stack);
            case HEAD -> setArmorSlotItem(1, stack);
            case CHEST -> setArmorSlotItem(2, stack);
            case LEGS -> setArmorSlotItem(3, stack);
            case FEET -> setArmorSlotItem(4, stack);
        }
    }

    public ItemStack getEquippedAccessory() { return equipment.get(CatEquipment.Slot.ACCESSORY); }
    public void setEquippedAccessory(ItemStack stack) { equipment.set(CatEquipment.Slot.ACCESSORY, stack); syncEquipment(); }
    public void setEquippedAccessoryClient(ItemStack stack) { equipment.set(CatEquipment.Slot.ACCESSORY, stack); }

    public ItemStack getCosmeticSlot(int index) { return equipment.getCosmetic(index); }
    public void setCosmeticSlot(int index, ItemStack stack) {
        if (equipment.setCosmetic(index, stack)) syncEquipment();
    }
    public void setCosmeticSlotClient(int index, ItemStack stack) { equipment.setCosmetic(index, stack); }

    public boolean isCosmeticsVisible() {
        return this.entityData.get(DATA_COSMETICS_VISIBLE);
    }
    public void setCosmeticsVisible(boolean visible) {
        this.entityData.set(DATA_COSMETICS_VISIBLE, visible);
        syncEquipment();
    }

    public boolean isFemale() {
        return this.entityData.get(DATA_FEMALE);
    }
    public void setFemale(boolean female) {
        this.entityData.set(DATA_FEMALE, female);
    }

    public void syncEquipment() {
        if (!this.level().isClientSide) {
            PacketSyncCatEquipment packet = new PacketSyncCatEquipment(
                    this.getId(),
                    getEquippedWeapon(),
                    getEquippedTool(),
                    getArmorSlotItem(1),
                    getArmorSlotItem(2),
                    getArmorSlotItem(3),
                    getArmorSlotItem(4),
                    getEquippedAccessory(),
                    equipment.getCosmetic(0),
                    equipment.getCosmetic(1),
                    equipment.getCosmetic(2),
                    this.isCosmeticsVisible()
            );
            PacketHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    packet
            );
        }
    }

    public void syncEquipment(ServerPlayer player) {
        if (!this.level().isClientSide && player != null) {
            PacketSyncCatEquipment packet = new PacketSyncCatEquipment(
                    this.getId(),
                    getEquippedWeapon(),
                    getEquippedTool(),
                    getArmorSlotItem(1),
                    getArmorSlotItem(2),
                    getArmorSlotItem(3),
                    getArmorSlotItem(4),
                    getEquippedAccessory(),
                    equipment.getCosmetic(0),
                    equipment.getCosmetic(1),
                    equipment.getCosmetic(2),
                    this.isCosmeticsVisible()
            );
            PacketHandler.sendToPlayer(packet, player);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        syncEquipment(player);
        syncTalents(player);
        syncLoadout(player);
    }

    /** Send the authoritative combat loadout to everyone tracking this cat. */
    public void syncLoadout() {
        if (!this.level().isClientSide) {
            PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new com.kittytalents.network.PacketSyncCatLoadout(this.getId(), com.kittytalents.core.component.CatLoadout.of(this)));
        }
    }

    public void syncLoadout(ServerPlayer player) {
        if (!this.level().isClientSide && player != null) {
            PacketHandler.sendToPlayer(new com.kittytalents.network.PacketSyncCatLoadout(this.getId(),
                    com.kittytalents.core.component.CatLoadout.of(this)), player);
        }
    }

    public boolean tryAutoEquip(ItemStack stack, Player player) {
        if (stack.isEmpty()) return false;

        // Armor: Helmet, Chestplate, Leggings, Boots
        if (stack.getItem() instanceof ArmorItem armor) {
            int slot = switch (armor.getType()) {
                case HELMET -> 1;
                case CHESTPLATE -> 2;
                case LEGGINGS -> 3;
                case BOOTS -> 4;
            };
            if (slot > 0) {
                ItemStack current = getArmorSlotItem(slot);
                if (current.isEmpty()) {
                    setArmorSlotItem(slot, stack.split(1));
                    updateAttributes();
                    syncEquipment();
                    playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                    return true;
                } else if (current.getItem() instanceof ArmorItem curArmor && armor.getDefense() > curArmor.getDefense()) {
                    ItemStack old = current.copy();
                    setArmorSlotItem(slot, stack.split(1));
                    if (player != null) player.getInventory().add(old);
                    else equipment.pack().addItem(old);
                    updateAttributes();
                    syncEquipment();
                    playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        // Weapon: Guns, Swords or Tridents (Mouth Weapon Slot)
        if (com.kittytalents.compat.tacz.TacZCompat.isGun(stack) ||
            stack.getItem() instanceof net.minecraft.world.item.SwordItem ||
            stack.getItem() instanceof net.minecraft.world.item.TridentItem) {
            if (getEquippedWeapon().isEmpty()) {
                setEquippedWeapon(stack.split(1));
                playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                return true;
            }
        }

        // Tools: Pickaxes, Axes, Shovels, Hoes, Shears, Fishing Rods (Tool Holster Slot)
        if (stack.getItem() instanceof net.minecraft.world.item.DiggerItem ||
            stack.getItem() instanceof net.minecraft.world.item.ShearsItem ||
            stack.getItem() instanceof net.minecraft.world.item.FishingRodItem) {
            if (getEquippedTool().isEmpty()) {
                setEquippedTool(stack.split(1));
                playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                return true;
            }
        }

        // Functional Accessory or Cosmetics
        if (stack.getItem() instanceof com.kittytalents.item.CatAccessoryItem) {
            if (getEquippedAccessory().isEmpty()) {
                setEquippedAccessory(stack.split(1));
                playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
                return true;
            }
            for (int i = 0; i < CatEquipment.COSMETIC_SLOTS; i++) {
                if (getCosmeticSlot(i).isEmpty()) {
                    setCosmeticSlot(i, stack.split(1));
                    playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        // Cosmetics (Collars, Capes, etc.)
        if (stack.getItem() instanceof com.kittytalents.item.CatCollarItem) {
            for (int i = 0; i < CatEquipment.COSMETIC_SLOTS; i++) {
                if (getCosmeticSlot(i).isEmpty()) {
                    setCosmeticSlot(i, stack.split(1));
                    playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        return false;
    }

    // Bed & Home
    @Nullable
    public BlockPos getHomeBedPos() { return homeBedPos; }
    public void setHomeBedPos(@Nullable BlockPos pos) { this.homeBedPos = pos; }
    public String getHomeBedDimension() { return homeBedDimension; }
    public void setHomeBedDimension(String dim) { this.homeBedDimension = dim; }
    public void setQuietUntil(int tick) { this.quietUntilTick = tick; }

    // Player Trust System (shared co-ownership)
    public Map<UUID, String> getTrustedPlayers() { return trust.view(); }
    public boolean isTrusted(UUID id) { return trust.contains(id); }
    public boolean isTrusted(Player player) { return player != null && isTrusted(player.getUUID()); }
    public void addTrustedPlayer(UUID id, String name) { trust.add(id, name); }
    public void removeTrustedPlayer(UUID id) { trust.remove(id); }
    public void setTrustedPlayers(Map<UUID, String> players) {
        trust.replaceAll(players);
    }
    public void syncTrust() {
        if (!this.level().isClientSide) {
            PacketHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new com.kittytalents.network.PacketSyncTrustedPlayers(this.getId(), trust.view())
            );
        }
    }
    public void syncTrust(ServerPlayer player) {
        if (!this.level().isClientSide && player != null) {
            PacketHandler.sendToPlayer(
                    new com.kittytalents.network.PacketSyncTrustedPlayers(this.getId(), trust.view()),
                    player
            );
        }
    }
    public boolean isPrimaryOwner(Player player) {
        return player != null && player.getUUID().equals(this.getOwnerUUID());
    }

    /** Trusted players share ownership permissions (commands, riding, equipment, pack). */
    @Override
    public boolean isOwnedBy(LivingEntity entity) {
        return super.isOwnedBy(entity) || (entity instanceof Player p && isTrusted(p));
    }

    public void resetStatistics() { stats.reset(); }

    public SimpleContainer getCatPack() { return equipment.pack(); }
    public SimpleContainer getAmmoPouch() { return equipment.ammoPouch(); }
    public com.kittytalents.work.JobData getJobData() { return jobData; }

    public com.kittytalents.work.CatJob getCatJob() {
        if (!this.level().isClientSide) {
            return jobData.getJob();
        }
        return com.kittytalents.work.CatJob.byOrdinal(this.entityData.get(DATA_JOB));
    }

    public void setCatJob(com.kittytalents.work.CatJob job) {
        com.kittytalents.work.CatJob safe = job != null ? job : com.kittytalents.work.CatJob.NONE;
        this.jobData.setJob(safe);
        this.entityData.set(DATA_JOB, (byte) safe.ordinal());
    }

    public boolean isClockedIn() {
        if (!this.level().isClientSide) {
            return jobData.isClockedIn();
        }
        return this.entityData.get(DATA_CLOCKED_IN);
    }

    public void setClockedIn(boolean clockedIn) {
        this.jobData.setClockedIn(clockedIn);
        this.entityData.set(DATA_CLOCKED_IN, clockedIn);
        // Invariant: a clocked-in cat with a job is always in WORK mode, whichever caller clocked it in.
        if (clockedIn && getCatJob() != com.kittytalents.work.CatJob.NONE && getCatMode() != CatMode.WORK) {
            setOrderedToSit(false);
            setCatMode(CatMode.WORK);
        }
    }

    public String getWorkStatus() {
        return this.entityData.get(DATA_WORK_STATUS);
    }

    public void setWorkStatus(String status) {
        String safe = status != null ? status : "Idle";
        this.jobData.setStatusMessage(safe);
        this.entityData.set(DATA_WORK_STATUS, safe);
    }

    public int getLastShotTick() {
        return this.entityData.get(DATA_LAST_SHOT_TICK);
    }

    public void setLastShotTick(int tick) {
        this.entityData.set(DATA_LAST_SHOT_TICK, tick);
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.core.Direction facing) {
        if (this.isAlive() && capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return catItemCapability.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        catItemCapability.invalidate();
    }

    public int getPackSlotCount() {
        int packLevel = getTalentLevel(Talents.KITTY_PACK);
        return Math.min(27, packLevel * 6);
    }

    /**
     * Cat Mount size: the cat only grows while someone rides it. (It used to be up to 2.6x bigger permanently, which
     * made mounted-talent cats unable to fit through gaps in forests and get stuck pathing.)
     */
    public float getCatScale() {
        if (!this.isVehicle()) return 1.0F;
        int mountLvl = getTalentLevel(Talents.CAT_MOUNT);
        return switch (mountLvl) {
            case 1 -> 1.25F;
            case 2 -> 1.50F;
            case 3 -> 1.80F;
            case 4 -> 2.20F;
            case 5 -> 2.60F;
            default -> 1.0F;
        };
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getCatScale());
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        this.refreshDimensions(); // grow to steed size
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.refreshDimensions(); // back to normal size
    }

    public void updateAttributes() {
        // Base values; talents add on top through TalentEngine (Cat Armory, Cat Armor, Feline Agility, ...).
        com.kittytalents.talent.effect.AttributeTotals t = new com.kittytalents.talent.effect.AttributeTotals(
                20.0D + getCatLevel() * 0.6D, 0.3D, 4.0D, 2.0D);
        for (int i = 1; i <= 4; i++) {
            if (getArmorSlotItem(i).getItem() instanceof ArmorItem armorItem) {
                t.armor += armorItem.getDefense();
                t.armorToughness += armorItem.getToughness();
            }
        }
        // Production tier: each step up (Standard -> Netherite) adds health, armor, toughness and damage.
        ProductionTier tier = getProductionTier();
        t.maxHealth += tier.bonusHealth();
        t.armor += tier.bonusArmor();
        t.armorToughness += tier.bonusToughness();
        t.attackDamage += tier.bonusDamage();
        com.kittytalents.talent.effect.TalentEngine.applyAttributes(this, t);
        setBase(Attributes.MAX_HEALTH, t.maxHealth);
        setBase(Attributes.MOVEMENT_SPEED, t.movementSpeed);
        setBase(Attributes.ATTACK_DAMAGE, t.attackDamage);
        setBase(Attributes.ARMOR, t.armor);
        setBase(Attributes.ARMOR_TOUGHNESS, t.armorToughness);
        setBase(Attributes.KNOCKBACK_RESISTANCE, Math.min(0.75D, t.knockbackResistance));
    }

    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = this.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        int armoryLvl = getTalentLevel(Talents.CAT_ARMORY);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        if (entity instanceof LivingEntity livingVictim) {
            // Talent damage bonuses (Pest Hunter, Hunter Cat, Berserker, masteries...) - Phase 4 TalentEngine.
            damage = com.kittytalents.talent.effect.TalentEngine.modifyOutgoing(this, livingVictim, damage);
        }

        if (!getEquippedWeapon().isEmpty()) {
            damage += 4.0F;
            if (entity instanceof LivingEntity livingTarget) {
                damage += net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(getEquippedWeapon(), livingTarget.getMobType());
                int fireAspect = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, getEquippedWeapon());
                if (fireAspect > 0) {
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                int knockback = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK, getEquippedWeapon());
                if (knockback > 0) {
                    livingTarget.knockback(knockback * 0.5F, (double) net.minecraft.util.Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), (double) (-net.minecraft.util.Mth.cos(this.getYRot() * ((float) Math.PI / 180F))));
                }
            }
            if (!this.level().isClientSide) {
                getEquippedWeapon().hurtAndBreak(1, this, (cat) -> {
                    this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
                    equipment.set(CatEquipment.Slot.WEAPON, ItemStack.EMPTY);
                    syncEquipment();
                });
            }
        }

        // Apotheosis attack modifiers (crit, elemental damage, life-steal, armor pierce)
        damage = com.kittytalents.compat.apotheosis.ApotheosisCompat.applyAttackModifiers(this, entity, damage);

        boolean hurt = entity.hurt(this.damageSources().mobAttack(this), damage);
        if (hurt) {
            stats.addDamageDealt(damage);
            if (!entity.isAlive()) {
                stats.recordKill(entity instanceof Monster);
            }
            if (entity instanceof LivingEntity livingHit) {
                // On-hit talents (Armory fire, Inferno, Venom, Charm, Alchemy, Catnip...) and role/tier attack effects.
                com.kittytalents.talent.effect.TalentEngine.onHitDealt(this, livingHit, damage);
                com.kittytalents.combat.CatAttackEffects.applyAttackEffects(this, livingHit, damage);
            }

            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.3F);

            // Nekomata Dual Strike Perk
            if (getMutation() == CatBreedingManager.MutationType.NEKOMATA && entity.isAlive() && !this.isDualStriking) {
                this.isDualStriking = true;
                entity.invulnerableTime = 0;
                doHurtTarget(entity);
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.85F, 1.4F);
                this.isDualStriking = false;
            }
        }
        return hurt;
    }

    /**
     * Truly fire-immune (no burning, no flames, lava doesn't set it alight) with Fireproof Paws or a mastered
     * Inferno Cat ("Phoenix"). Damage was already cancelled by the talents, but the cat still caught fire.
     */
    @Override
    public boolean fireImmune() {
        if (talents == null) return super.fireImmune(); // may be asked during construction
        return super.fireImmune() || getTalentLevel(Talents.FIREPROOF_PAWS) > 0
                || com.kittytalents.talent.effect.TalentEngine.isMastered(this, Talents.INFERNO_CAT);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Void / kill damage must always apply, otherwise a downed cat below the world falls forever.
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (isIncapacitated()) {
            return false;
        }

        if (com.kittytalents.compat.apotheosis.ApotheosisCompat.checkDodge(this, source)) {
            return false;
        }

        // Owner friendly fire. Guard against null: untamed cats have no owner, and environmental damage has no entity.
        if (this.getOwner() != null && source.getEntity() == this.getOwner() && !KittyTalentsConfig.COMMON.friendlyFire.get()) {
            return false;
        }

        // Talent defenses (Fireproof Paws, Guardian Cat, Feline Agility dodge, masteries...) - Phase 4 TalentEngine.
        amount = com.kittytalents.talent.effect.TalentEngine.modifyIncoming(this, source, amount);
        if (amount <= 0.0F) return false;
        // Counted after talent dodges so dodged hits are not recorded as damage taken.
        stats.addDamageTaken(amount);

        // Damage armor durability
        if (!this.level().isClientSide && amount > 2.0F) {
            for (int i = 1; i <= 4; i++) {
                ItemStack stack = getArmorSlotItem(i);
                if (!stack.isEmpty()) {
                    final int slot = i;
                    stack.hurtAndBreak(1, this, (cat) -> {
                        cat.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
                        cat.setArmorSlotItem(slot, ItemStack.EMPTY);
                    });
                }
            }
        }

        if (amount >= this.getHealth()) {
            // Nine Lives instant totem revival
            int nineLives = getTalentLevel(Talents.NINE_LIVES);
            long now = this.level().getGameTime();
            if (nineLives > 0 && !isIncapacitated() && now >= combat.getNineLivesReadyAt()) {
                // Instant revive recharges after (10 min - 1 min per rank); otherwise the cat goes down instead.
                boolean tenthLife = com.kittytalents.talent.effect.TalentEngine.isMastered(this, Talents.NINE_LIVES);
                long recharge = Math.max(1200L, 12000L - 1200L * nineLives);
                combat.setNineLivesReadyAt(now + (tenthLife ? recharge / 2 : recharge));
                this.setHealth(tenthLife ? this.getMaxHealth() : Math.max(4.0F, this.getMaxHealth() * Math.min(1.0F, 0.18F * nineLives)));
                if (tenthLife) this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                this.level().broadcastEntityEvent(this, (byte) 35);
                this.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
                return false;
            }

            if (KittyTalentsConfig.COMMON.immortalCats.get() || nineLives > 0) {
                this.setIncapacitated(true);
                this.playSound(SoundEvents.CAT_DEATH, 0.8F, 1.2F);
                if (this.getOwner() instanceof Player player) {
                    player.sendSystemMessage(Component.translatable("message.kittytalents.cat_downed", this.getName()));
                }
                return false;
            }
        }

        return super.hurt(source, amount);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof Player player ? player : null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive()) {
            if (isTwined()) {
                SkyFlight.twined(this, twineJump);
                applyTwineLeash();
                float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                if (this.isSprinting()) {
                    speed *= 1.35F;
                }
                this.setSpeed(speed);
                super.travel(new Vec3(this.twineXxa, travelVector.y, this.twineZza));
                return;
            }
            LivingEntity passenger = this.getControllingPassenger();
            if (this.isVehicle() && passenger instanceof Player player) {
                this.setYRot(player.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(player.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;

                float strafe = player.xxa * 0.5F;
                float forward = player.zza;
                if (forward <= 0.0F) {
                    forward *= 0.3F;
                }

                int mountLvl = getTalentLevel(Talents.CAT_MOUNT);
                double speedMult = KittyTalentsConfig.COMMON.mountSpeedMultiplier.get();
                double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (1.0D + mountLvl * 0.25D) * speedMult;

                // Flight with SKY_FELINE talent
                int skyLvl = getTalentLevel(Talents.SKY_FELINE);
                if (skyLvl > 0) {
                    double flightMult = KittyTalentsConfig.COMMON.flightSpeedMultiplier.get();
                    if (player.getXRot() < -10.0F && forward > 0) {
                        this.setDeltaMovement(this.getDeltaMovement().x, (0.25D + (skyLvl * 0.08D)) * flightMult, this.getDeltaMovement().z);
                        this.hasImpulse = true;
                        this.resetFallDistance();
                    } else if (!this.onGround()) {
                        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.65D, 1.0D));
                        this.resetFallDistance();
                    }
                }

                this.setSpeed((float) speed);
                super.travel(new Vec3(strafe, travelVector.y, forward));
                return;
            }
        }
        super.travel(travelVector);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
        if (this.level().isClientSide && this.isControlledByLocalInstance() && !teleport) {
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, lerpSteps, teleport);
    }

    @Override
    public void lerpHeadTo(float yRot, int steps) {
        if (this.level().isClientSide && this.isControlledByLocalInstance()) {
            return;
        }
        super.lerpHeadTo(yRot, steps);
    }


    /** Set by the stress tests on the cats they spawn, so TickCost times only those (transient, never saved). */
    public boolean tickCostTracked;

    @Override
    public void tick() {
        if (!com.kittytalents.util.TickCost.enabled || !tickCostTracked || this.level().isClientSide) {
            tickCat();
            return;
        }
        long t = System.nanoTime();
        tickCat();
        com.kittytalents.util.TickCost.add(System.nanoTime() - t);
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide && this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.lerpHeadSteps = 0;
        }
        super.aiStep();
    }

    @Override
    public boolean isControlledByLocalInstance() {
        if (this.level().isClientSide) {
            if (isTwined()) {
                return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.kittytalents.client.twine.ClientTwineState.isTwinedWith(this.getId())
                ) == Boolean.TRUE;
            }
            Entity passenger = this.getControllingPassenger();
            if (passenger instanceof Player player) {
                return player.isLocalPlayer();
            }
            return false;
        }
        return super.isControlledByLocalInstance();
    }

    @Override
    public double getPassengersRidingOffset() {
        return (double) this.getDimensions(this.getPose()).height * 0.75D;
    }

    @Override
    public void onPlayerJump(int jumpScale) {
        if (this.onGround()) {
            float power = 0.45F * (1.0F + getTalentLevel(Talents.FELINE_AGILITY) * 0.12F);
            if (jumpScale > 0) {
                power *= (1.0F + (jumpScale / 100.0F) * 0.4F);
            }
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, power, motion.z);
            this.hasImpulse = true;
            this.playSound(SoundEvents.CAT_BEG_FOR_FOOD, 0.6F, 1.4F);
        }
    }

    @Override
    public boolean canJump() {
        return this.onGround() || getTalentLevel(Talents.SKY_FELINE) > 0;
    }

    @Override
    public void handleStartJump(int jumpScale) {}

    @Override
    public void handleStopJump() {}

    /** The cat's tick body; {@link #tick} wraps it with the optional stress-test timer. */
    private void tickCat() {
        super.tick();

        if (!this.level().isClientSide && this.isVehicle()) {
            LivingEntity passenger = this.getControllingPassenger();
            if (passenger instanceof Player player) {
                this.setYRot(player.getYRot());
                this.setXRot(player.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
                this.hasImpulse = true;
            }
        }

        if (!this.level().isClientSide && isTwined()) {
            // Looked up in this level (a player in another dimension isn't found, which also ends the session).
            Player tp = twinedPlayerUUID != null ? this.level().getPlayerByUUID(twinedPlayerUUID) : null;
            if (tp == null || !tp.isAlive() || isIncapacitated() || !this.isAlive()) {
                TwineManager.endForCat(this, "Twine ended.");
            }
        }

        // Track distance
        double deltaMovementLength = Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().z * getDeltaMovement().z);
        if (deltaMovementLength > 0.05D) {
            stats.addDistance(deltaMovementLength);
        }

        if (isIncapacitated()) {
            this.setOrderedToSit(true);
            if (this.level().isClientSide && this.random.nextInt(20) == 0) {
                this.level().addParticle(ParticleTypes.DAMAGE_INDICATOR, this.getX(), this.getY() + 0.3D, this.getZ(), 0, 0.05D, 0);
            }
            return;
        }

        // Sleeping / sitting cats must consume near-zero tick time.
        // Process hunger, energy regeneration, and skip all combat/talent environmental scans.
        if ((this.isOrderedToSit() || this.isSleeping()) && !this.isTwined()) {
            if (!this.level().isClientSide) {
                // Slower hunger drain when resting (3x slower)
                hungerTimer++;
                int happyEater = getTalentLevel(Talents.HAPPY_EATER);
                int rate = (KittyTalentsConfig.COMMON.hungerTickRate.get() + (happyEater * 400)) * 3;
                if (hungerTimer >= rate) {
                    hungerTimer = 0;
                    if (this.satiety > 0.0F) {
                        this.satiety = Math.max(0.0F, this.satiety - 1.0F);
                    } else if (getHunger() > 0) {
                        setHunger(getHunger() - 1);
                    }
                }
                // Passive talents (owner auras, healing, self-repair) keep working while the cat rests.
                com.kittytalents.talent.effect.TalentEngine.tick(this, true);
            }
            return;
        }

        // Torch Kitty bioluminescent glow
        int torchLvl = getTalentLevel(Talents.TORCH_KITTY);
        if (torchLvl > 0 && this.level().isClientSide && this.level().getBrightness(LightLayer.BLOCK, this.blockPosition()) < 4) {
            if (this.random.nextInt(5) == 0) {
                this.level().addParticle(ParticleTypes.GLOW, this.getRandomX(0.5D), this.getY() + 0.4D, this.getRandomZ(0.5D), 0, 0.02D, 0);
            }
        }

        // Sky Feline flight particles
        if (getTalentLevel(Talents.SKY_FELINE) > 0 && !this.onGround()) {
            if (this.level().isClientSide && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.5D), this.getY() + 0.3D, this.getRandomZ(0.5D), 0, 0.02D, 0);
            }
        }

        // Frenzy Aura particles
        if (frenzyTimer > 0) {
            frenzyTimer--;
            if (this.level().isClientSide && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.6D), this.getY() + 0.2D, this.getRandomZ(0.6D), 0, 0.05D, 0);
            }
        }

        if (frenzyCooldown > 0) {
            frenzyCooldown--;
        }

        if (!this.level().isClientSide) {

            // Hunger drain with Happy Eater reduction
            hungerTimer++;
            int happyEater = getTalentLevel(Talents.HAPPY_EATER);
            int rate = KittyTalentsConfig.COMMON.hungerTickRate.get() + (happyEater * 400);
            if (hungerTimer >= rate) {
                hungerTimer = 0;
                if (this.satiety > 0.0F) {
                    this.satiety = Math.max(0.0F, this.satiety - 1.0F);
                } else if (getHunger() > 0) {
                    setHunger(getHunger() - 1);
                } else {
                    this.hurt(this.damageSources().starve(), 1.0F);
                }
            }

            // Every talent's periodic behavior (and mastery) - Phase 4 TalentEngine; staggered per cat.
            com.kittytalents.talent.effect.TalentEngine.tick(this);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.tickCount < quietUntilTick) return null;
        return super.getAmbientSound();
    }

    public boolean canConsumeFrenzyMilk() {
        return frenzyCooldown <= 0;
    }

    public void triggerFrenzy(int durationTicks) {
        this.frenzyTimer = durationTicks;
        this.frenzyCooldown = durationTicks + 2400;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = com.kittytalents.entity.interact.CatInteractions.handle(this, player, hand);
        return result != null ? result : super.mobInteract(player, hand);
    }

    public void openTalentScreen(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CatTalentMenu(containerId, playerInventory, this),
                this.getName()
        ), buf -> buf.writeInt(this.getId()));
        com.kittytalents.network.PacketHandler.sendToPlayer(
                new com.kittytalents.network.PacketSyncTrustedPlayers(this.getId(), trust.view()), player);
        syncEquipment(player);
        syncTalents(player);
        syncLoadout(player);
    }

    public void openPackScreen(ServerPlayer player) {
        if (getPackSlotCount() <= 0) {
            player.sendSystemMessage(Component.translatable("message.kittytalents.no_pack"));
            return;
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CatPackMenu(containerId, playerInventory, this),
                Component.translatable("container.kittytalents.pack", this.getName())
        ), buf -> buf.writeInt(this.getId()));
    }

    // Statistics getters & helpers
    public int getTotalKills() { return stats.getTotalKills(); }
    public int getHostileKills() { return stats.getHostileKills(); }
    public int getPassiveKills() { return stats.getPassiveKills(); }
    public float getDamageDealt() { return stats.getDamageDealt(); }
    public float getDamageTaken() { return stats.getDamageTaken(); }
    public double getDistanceTraveled() { return stats.getDistanceTraveled(); }
    public int getFoodConsumed() { return stats.getFoodConsumed(); }
    public int getBlocksMined() { return stats.getBlocksMined(); }
    public void incrementBlocksMined() { stats.incrementBlocksMined(); }
    public int getPouncesPerformed() { return stats.getPouncesPerformed(); }
    public void incrementPounces() { stats.incrementPounces(); }
    public int getItemsRetrieved() { return stats.getItemsRetrieved(); }
    public void incrementItemsRetrieved() { stats.incrementItemsRetrieved(); }
    public int getCreepersScared() { return stats.getCreepersScared(); }
    public void incrementCreepersScared() { stats.incrementCreepersScared(); }
    public int getHealingProvided() { return stats.getHealingProvided(); }
    public void addHealingProvided(int amount) { stats.addHealingProvided(amount); }
    public int getTimesPet() { return stats.getTimesPet(); }
    public void incrementTimesPet() { stats.incrementTimesPet(); }
    public int getFrenzyActivations() { return stats.getFrenzyActivations(); }
    public void incrementFrenzyActivations() { stats.incrementFrenzyActivations(); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CatLevel", getCatLevel());
        tag.putInt("CatXp", getCatXp());
        tag.putBoolean("StartingPointsGranted", startingPointsGranted);
        tag.putInt("SkillPoints", getSkillPoints());
        tag.putInt("Hunger", getHunger());
        tag.putFloat("Satiety", satiety);
        tag.putInt(CatSchema.KEY_SCHEMA, CatSchema.CURRENT);
        tag.putString(CatSchema.KEY_MODE_NAME, getCatMode().name());
        if (modeAnchor != null) tag.put("ModeAnchor", NbtUtils.writeBlockPos(modeAnchor));
        if (spiritCatnipPrimed) tag.putBoolean(CatSchema.KEY_SPIRIT_PRIMED, true);
        tag.putBoolean("Incapacitated", isIncapacitated());
        tag.putInt("SkinId", getSkinId());
        tag.putBoolean("DireAwakened", isDireAwakened());
        tag.putBoolean("Female", isFemale());
        tag.putBoolean("CosmeticsVisible", isCosmeticsVisible());
        tag.putString("CatVariant", getCatVariant().getId());
        tag.putString("VariantID", getCatVariant().getId());
        tag.putString("ProductionTier", getProductionTier().getId());
        tag.putString("MutationType", getMutation().name());
        tag.putInt("FrenzyTimer", frenzyTimer);
        tag.putInt("FrenzyCooldown", frenzyCooldown);

        if (homeBedPos != null) tag.put("HomeBedPos", NbtUtils.writeBlockPos(homeBedPos));
        if (homeBedDimension != null && !homeBedDimension.isEmpty()) tag.putString("HomeBedDim", homeBedDimension);

        stats.save(tag);

        equipment.save(tag);
        combat.save(tag);
        tag.putInt("Energy", energy);

        trust.save(tag);
        talents.save(tag);
        tag.put("JobData", jobData.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Upgrade old saves in place before reading any field (ForgeData is already loaded into getPersistentData()).
        CatSchema.migrate(tag, getPersistentData());
        if (tag.contains("CatLevel")) setCatLevel(tag.getInt("CatLevel"));
        setCatXp(tag.getInt("CatXp"));
        this.startingPointsGranted = tag.getBoolean("StartingPointsGranted");
        if (tag.contains("SkillPoints")) setSkillPoints(tag.getInt("SkillPoints"));
        if (tag.contains("Hunger")) setHunger(tag.getInt("Hunger"));
        this.satiety = tag.getFloat("Satiety");
        String savedMode = tag.getString(CatSchema.KEY_MODE_NAME);
        if (tag.contains(CatSchema.KEY_MODE_NAME)) setCatMode(CatMode.fromSavedName(savedMode));
        // Anchor after the mode: setCatMode would otherwise re-anchor to the load position.
        if (tag.contains("ModeAnchor")) this.modeAnchor = NbtUtils.readBlockPos(tag.getCompound("ModeAnchor"));
        this.spiritCatnipPrimed = tag.getBoolean(CatSchema.KEY_SPIRIT_PRIMED);
        if (tag.contains("Incapacitated")) setIncapacitated(tag.getBoolean("Incapacitated"));
        if (tag.contains("SkinId")) setSkinId(tag.getInt("SkinId"));
        if (tag.contains("DireAwakened")) setDireAwakened(tag.getBoolean("DireAwakened"));
        if (tag.contains("Female")) setFemale(tag.getBoolean("Female"));
        if (tag.contains("CosmeticsVisible")) setCosmeticsVisible(tag.getBoolean("CosmeticsVisible"));
        if (tag.contains("VariantID")) setCatVariant(CatVariant.fromId(tag.getString("VariantID")));
        else if (tag.contains("CatVariant")) setCatVariant(CatVariant.fromId(tag.getString("CatVariant")));
        if (tag.contains("ProductionTier")) setProductionTier(ProductionTier.fromId(tag.getString("ProductionTier")));
        else if (tag.contains("ResourceType")) setProductionTier(ProductionTier.fromId(tag.getString("ResourceType")));
        if (tag.contains("MutationType")) setMutation(CatBreedingManager.MutationType.fromString(tag.getString("MutationType")));
        if (tag.contains("FrenzyTimer")) this.frenzyTimer = tag.getInt("FrenzyTimer");
        if (tag.contains("FrenzyCooldown")) this.frenzyCooldown = tag.getInt("FrenzyCooldown");

        if (tag.contains("HomeBedPos")) this.homeBedPos = NbtUtils.readBlockPos(tag.getCompound("HomeBedPos"));
        if (tag.contains("HomeBedDim")) this.homeBedDimension = tag.getString("HomeBedDim");

        stats.load(tag);

        equipment.load(tag);
        combat.load(tag);
        if (tag.contains("Energy")) setEnergy(tag.getInt("Energy"));

        trust.load(tag);
        talents.load(tag);
        if (tag.contains("JobData", Tag.TAG_COMPOUND)) {
            jobData.load(tag.getCompound("JobData"));
        }
        // Migrate legacy FARM / MINE / FISH modes to WORK + corresponding CatJob
        com.kittytalents.work.CatJob legacyJob = switch (savedMode.toUpperCase(java.util.Locale.ROOT)) {
            case "FARM" -> com.kittytalents.work.CatJob.FARMER;
            case "MINE" -> com.kittytalents.work.CatJob.MINER;
            case "FISH" -> com.kittytalents.work.CatJob.FISHER;
            default -> null;
        };
        if (legacyJob != null) {
            setCatMode(CatMode.WORK);
            setCatJob(legacyJob);
        } else {
            this.entityData.set(DATA_JOB, (byte) jobData.getJob().ordinal());
            this.entityData.set(DATA_CLOCKED_IN, jobData.isClockedIn());
            this.entityData.set(DATA_WORK_STATUS, jobData.getStatusMessage());
        }
        updateAttributes();
        this.refreshDimensions();
        // Cats tamed before the starting pool existed get it once (the flag is saved afterwards).
        grantStartingPoints();
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide) {
            if (isTwined()) TwineManager.endForCat(this, "Your cat has fallen - untwined.");
            Containers.dropContents(this.level(), this, equipment.pack());
            Containers.dropContents(this.level(), this, equipment.ammoPouch());
            if (!getEquippedWeapon().isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getEquippedWeapon());
            if (!getEquippedTool().isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getEquippedTool());
            if (!getArmorSlotItem(1).isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getArmorSlotItem(1));
            if (!getArmorSlotItem(2).isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getArmorSlotItem(2));
            if (!getArmorSlotItem(3).isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getArmorSlotItem(3));
            if (!getArmorSlotItem(4).isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getArmorSlotItem(4));
            if (!getEquippedAccessory().isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getEquippedAccessory());
            if (!getCarriedToy().isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), getCarriedToy());
            for (int ci = 0; ci < CatEquipment.COSMETIC_SLOTS; ci++) {
                ItemStack cosmetic = getCosmeticSlot(ci);
                if (!cosmetic.isEmpty()) {
                    Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), cosmetic);
                }
            }
        }
        super.die(cause);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.BREEDING_CATNIP.get()) || stack.is(ModItems.SPIRIT_CATNIP.get());
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal otherAnimal) {
        if (otherAnimal == this) return false;
        if (!(otherAnimal instanceof TalentedCat otherCat)) return false;
        if (!this.isInLove() || !otherCat.isInLove()) return false;
        return !this.isOrderedToSit() && !otherCat.isOrderedToSit();
    }

    @Nullable
    @Override
    public Cat getBreedOffspring(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob otherParent) {
        TalentedCat child = com.kittytalents.registry.ModEntities.TALENTED_CAT.get().create(level);
        if (child != null) {
            UUID owner = this.getOwnerUUID();
            if (owner != null) {
                child.setOwnerUUID(owner);
                child.setTame(true);
            }
            boolean spiritUsed = this.isSpiritCatnipPrimed() ||
                    (otherParent instanceof TalentedCat o && o.isSpiritCatnipPrimed());
            this.setSpiritCatnipPrimed(false);
            if (otherParent instanceof TalentedCat o) {
                o.setSpiritCatnipPrimed(false);
            }

            if (otherParent instanceof TalentedCat otherCat) {
                ProductionTier childTier = CatBreedingManager.getChildTier(this.getProductionTier(), otherCat.getProductionTier());
                CatVariant childVariant = CatBreedingManager.getChildVariant(this.getCatVariant(), otherCat.getCatVariant(), childTier);
                child.setCatVariant(childVariant);
                child.setProductionTier(childTier);
                child.setSkinId(this.random.nextBoolean() ? this.getSkinId() : otherCat.getSkinId());
                child.setFemale(this.random.nextBoolean());
                if (spiritUsed) {
                    child.setMutation(CatBreedingManager.MutationType.NEKOMATA);
                } else {
                    CatBreedingManager.MutationType mutation = CatBreedingManager.calculateMutation(this, otherCat, false);
                    child.setMutation(mutation);
                }
            } else {
                child.setCatVariant(this.getCatVariant());
                child.setProductionTier(this.getProductionTier());
                child.setSkinId(this.getSkinId());
                if (spiritUsed) child.setMutation(CatBreedingManager.MutationType.NEKOMATA);
            }
        }
        return child;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        // Randomize gender
        this.setFemale(this.random.nextBoolean());

        // Randomize appearance: pick from CatVariant or custom skins
        if (this.random.nextFloat() < 0.70F) {
            int vanillaSkin = this.random.nextInt(11);
            this.setSkinId(vanillaSkin);
            this.setCatVariant(CatVariant.values()[vanillaSkin]);
        } else if (this.random.nextFloat() < 0.85F) {
            int specialSkin = 11 + this.random.nextInt(6); // 11..16
            this.setSkinId(specialSkin);
            if (specialSkin == 11) this.setCatVariant(CatVariant.TIGER);
            else if (specialSkin == 12) this.setCatVariant(CatVariant.SNOW_LEOPARD);
            else if (specialSkin == 13) this.setCatVariant(CatVariant.VOID_ENDER);
            else if (specialSkin == 14) this.setCatVariant(CatVariant.GOLDEN_MANEKI);
            else if (specialSkin == 15) this.setCatVariant(CatVariant.GHOST_PHANTOM);
            else if (specialSkin == 16) this.setCatVariant(CatVariant.PANTHER);
        } else {
            CatVariant[] allVariants = CatVariant.values();
            this.setCatVariant(allVariants[this.random.nextInt(allVariants.length)]);
        }

        // Untamed cats default to wander mode
        if (!this.isTame()) {
            this.setCatMode(CatMode.WANDER);
            this.setOrderedToSit(false);
        }

        // Apply Neko Night bonuses if active
        com.kittytalents.event.NekoNightManager.applyNekoNightBonuses(this);

        return spawnData;
    }
}
