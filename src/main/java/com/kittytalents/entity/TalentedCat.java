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

    private final com.kittytalents.work.JobData jobData = new com.kittytalents.work.JobData();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> catItemCapability =
            net.minecraftforge.common.util.LazyOptional.of(() -> new com.kittytalents.work.CatAmmoAndPackItemHandler(this));


    @Nullable
    private BlockPos homeBedPos = null;
    private String homeBedDimension = "";

    private int hungerTimer = 0;
    private int purrTimer = 0;
    private int radarTimer = 0;
    private int frenzyCooldown = 0;
    private int frenzyTimer = 0;
    private int hissCooldown = 0;
    private int medicCooldown = 0;
    private int rescueCooldown = 0;
    private int galeCooldown = 0;
    private int catastropheCooldown = 0;
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
            if (inToolSlot) {
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
        if (!this.level().isClientSide) updateControlFlags(); // hand control back to the AI immediately
    }

    public void setTwineInput(float xxa, float zza) {
        this.twineXxa = xxa;
        this.twineZza = zza;
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

    public int getCostForNextRank(Talent talent) {
        int currentLvl = getTalentLevel(talent);
        if (currentLvl >= talent.getMaxLevel()) return 0;
        int nextRank = currentLvl + 1;
        if (talent.requiresDireTreat()) {
            return nextRank * 3;
        } else if (talent.getMaxLevel() <= 5) {
            return nextRank <= 2 ? 1 : 2;
        } else {
            if (nextRank <= 2) return 1;
            else if (nextRank <= 5) return 2;
            else return 3;
        }
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
                if (talent.requiresDireTreat()) {
                    refundedPoints += r * 3;
                } else if (talent.getMaxLevel() <= 5) {
                    refundedPoints += (r <= 2 ? 1 : 2);
                } else {
                    if (r <= 2) refundedPoints += 1;
                    else if (r <= 5) refundedPoints += 2;
                    else refundedPoints += 3;
                }
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

    public float getCatScale() {
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

    public void updateAttributes() {
        double maxHealth = 20.0D + (getCatLevel() * 0.6D);
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        }
        int agility = getTalentLevel(Talents.FELINE_AGILITY);
        double speed = 0.3D + (agility * 0.03D);
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        int armory = getTalentLevel(Talents.CAT_ARMORY);
        double damage = 4.0D + (armory * 2.5D);
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        }
        int armor = getTalentLevel(Talents.CAT_ARMOR);
        double armorVal = 2.0D + (armor * 3.0D);
        double toughness = armor * 1.5D;

        for (int i = 1; i <= 4; i++) {
            ItemStack stack = getArmorSlotItem(i);
            if (stack.getItem() instanceof ArmorItem armorItem) {
                armorVal += armorItem.getDefense();
                toughness += armorItem.getToughness();
            }
        }

        if (this.getAttribute(Attributes.ARMOR) != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(armorVal);
        }
        if (this.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(toughness);
        }
        if (this.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(Math.min(0.75D, armor * 0.15D));
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        int armoryLvl = getTalentLevel(Talents.CAT_ARMORY);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        int pestLvl = getTalentLevel(Talents.PEST_HUNTER);
        if (pestLvl > 0 && (entity instanceof Spider || entity instanceof Silverfish ||
                entity instanceof Endermite || entity instanceof Bat || entity instanceof Phantom)) {
            damage *= (1.5F + pestLvl * 0.4F);
        }

        int hunterLvl = getTalentLevel(Talents.HUNTER_CAT);
        if (hunterLvl > 0 && entity instanceof Animal) {
            damage += (hunterLvl * 2.5F);
        }

        int berserkLvl = getTalentLevel(Talents.FELINE_BERSERKER);
        if (berserkLvl > 0 && (this.getHealth() < this.getMaxHealth() * 0.4F || frenzyTimer > 0)) {
            damage += (3.0F + berserkLvl * 2.0F);
            this.playSound(SoundEvents.RAVAGER_ATTACK, 0.6F, 1.6F);
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
            if (armoryLvl >= 3) {
                entity.setSecondsOnFire(4 * (armoryLvl - 2));
            }

            int infernoLvl = getTalentLevel(Talents.INFERNO_CAT);
            if (infernoLvl > 0) {
                entity.setSecondsOnFire(infernoLvl * 3);
            }

            int venomLvl = getTalentLevel(Talents.VENOM_CLAWS);
            if (venomLvl > 0 && entity instanceof LivingEntity living) {
                if (venomLvl >= 5) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                } else {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + venomLvl * 20, Math.max(0, venomLvl - 1)));
                }
            }

            int charmLvl = getTalentLevel(Talents.CHARM_WHISKERS);
            if (charmLvl > 0 && entity instanceof LivingEntity living && this.random.nextFloat() < (0.15F * charmLvl)) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, Math.max(0, charmLvl - 1)));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
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

        // Fireproof Paws immunity to fire, hot floor and lava
        if (getTalentLevel(Talents.FIREPROOF_PAWS) > 0 &&
                (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
                 source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR) ||
                 source.is(net.minecraft.world.damagesource.DamageTypes.LAVA) ||
                 source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) ||
                 source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE))) {
            this.clearFire();
            return false;
        }

        // Guardian Cat damage reduction
        int guardianLvl = getTalentLevel(Talents.GUARDIAN_CAT);
        if (guardianLvl > 0 && amount > 1.0F) {
            amount *= Math.max(0.4F, 1.0F - (guardianLvl * 0.12F));
        }


        int agility = getTalentLevel(Talents.FELINE_AGILITY);
        if (source.isIndirect() && agility > 0) {
            if (this.random.nextFloat() < (agility * 0.10F)) {
                this.playSound(SoundEvents.CAT_PURREOW, 0.7F, 1.5F);
                return false;
            }
        }
        // Counted after the dodge roll so dodged hits are not recorded as damage taken.
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
                combat.setNineLivesReadyAt(now + Math.max(1200L, 12000L - 1200L * nineLives));
                this.setHealth(Math.max(4.0F, this.getMaxHealth() * (0.18F * nineLives)));
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
                        () -> () -> com.kittytalents.client.ClientTwineHandler.isTwined() &&
                                com.kittytalents.client.ClientTwineHandler.activeTwinedCatId == this.getId()
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

    @Override
    public void tick() {
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
            net.minecraft.server.level.ServerPlayer tp = this.getServer() != null
                    ? this.getServer().getPlayerList().getPlayer(twinedPlayerUUID) : null;
            if (tp == null || !tp.isAlive() || tp.level() != this.level() || isIncapacitated() || !this.isAlive()) {
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
                // Energy regeneration (+3/sec when resting)
                if (this.tickCount % 20 == 0) {
                    this.energy = Math.min(100, this.energy + 3);
                }
            }
            return;
        }

        // Aqua Kitty water breathing and swim boost
        int aquaLvl = getTalentLevel(Talents.AQUA_KITTY);
        if (aquaLvl > 0 && this.isInWater()) {
            this.setAirSupply(this.getMaxAirSupply());
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.25D + (aquaLvl * 0.08D), 1.08D, 1.25D + (aquaLvl * 0.08D)));
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
            // Energy regeneration (+1/sec when active)
            if (this.tickCount % 20 == 0) {
                this.energy = Math.min(100, this.energy + 1);
            }

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

            // Purr Medic healing aura
            int medicLvl = getTalentLevel(Talents.PURR_MEDIC);
            if (medicLvl > 0) {
                medicCooldown++;
                if (medicCooldown >= 100) {
                    medicCooldown = 0;
                    if (this.getOwner() instanceof Player owner && this.distanceToSqr(owner) < 64.0D) {
                        if (owner.getHealth() < owner.getMaxHealth()) {
                            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80 + (medicLvl * 20), 1, false, false, true));
                            if (medicLvl >= 3) {
                                owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, false, false, true));
                            }
                            this.playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
                        }
                    }
                }
            }

            // Feline Hiss ranged combat
            int hissLvl = getTalentLevel(Talents.FELINE_HISS);
            if (hissLvl > 0 && this.getTarget() != null) {
                LivingEntity target = this.getTarget();
                double distSqr = this.distanceToSqr(target);
                if (distSqr >= 16.0D && distSqr <= 256.0D) {
                    hissCooldown++;
                    if (hissCooldown >= Math.max(30, 80 - hissLvl * 10)) {
                        hissCooldown = 0;
                        this.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
                        FelineHissProjectileEntity projectile = new FelineHissProjectileEntity(this.level(), this, 4.0F + (hissLvl * 2.5F));
                        double dx = target.getX() - this.getX();
                        double dy = target.getY(0.5D) - this.getY(0.5D);
                        double dz = target.getZ() - this.getZ();
                        projectile.shoot(dx, dy, dz, 1.3F, 1.0F);
                        this.level().addFreshEntity(projectile);
                    }
                }
            }

            int luckyPurr = getTalentLevel(Talents.LUCKY_PURR);
            if (luckyPurr > 0) {
                purrTimer++;
                if (purrTimer >= 100) {
                    purrTimer = 0;
                    List<Player> nearby = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(6.0D + luckyPurr * 2.0D));
                    for (Player p : nearby) {
                        p.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, luckyPurr - 1, false, false, true));
                        if (luckyPurr >= 3) {
                            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false, true));
                        }
                    }
                }
            }

            int radar = getTalentLevel(Talents.WHISKER_RADAR);
            if (radar > 0) {
                radarTimer++;
                if (radarTimer >= 160) {
                    radarTimer = 0;
                    double radarDist = 8.0D + (radar * 4.0D);
                    List<Monster> monsters = this.level().getEntitiesOfClass(
                            Monster.class,
                            this.getBoundingBox().inflate(radarDist)
                    );
                    if (!monsters.isEmpty()) {
                        this.playSound(SoundEvents.CAT_BEG_FOR_FOOD, 0.8F, 1.2F);
                    }
                }
            }

            // Shadow Pelt - stealth in low light
            int shadowLvl = getTalentLevel(Talents.SHADOW_PELT);
            if (shadowLvl > 0) {
                int light = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());
                if (light <= 4 || this.isCrouching()) {
                    this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
                }
            }

            // Water Pelt - extinguishes burning owner & grants water breathing
            int waterPeltLvl = getTalentLevel(Talents.WATER_PELT);
            if (waterPeltLvl > 0 && this.getOwner() instanceof Player owner && this.distanceToSqr(owner) < 64.0D) {
                if (owner.isOnFire()) {
                    owner.clearFire();
                    this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.7F, 1.0F);
                }
                if (owner.isInWater() || this.isInWater()) {
                    owner.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false, true));
                }
            }

            // Rescue Purr - saves low health owner
            int rescueLvl = getTalentLevel(Talents.RESCUE_PURR);
            if (rescueLvl > 0 && this.getOwner() instanceof Player owner && !this.isIncapacitated()) {
                if (owner.getHealth() <= owner.getMaxHealth() * 0.3F && this.distanceToSqr(owner) < 1024.0D) {
                    if (this.rescueCooldown <= 0) {
                        this.rescueCooldown = 600 - (rescueLvl * 60);
                        this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                        owner.heal(4.0F + (rescueLvl * 2.0F));
                        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
                        owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                        this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
                        this.addHealingProvided(6 + rescueLvl * 2);
                    }
                }
            }
            if (rescueCooldown > 0) rescueCooldown--;

            // Gale Purr - pushes back surrounding hostiles (throttled to 20-tick interval)
            int galeLvl = getTalentLevel(Talents.GALE_PURR);
            if (galeLvl > 0 && galeCooldown <= 0 && this.tickCount % 20 == 0) {
                List<Monster> surrounding = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(3.5D + galeLvl * 0.5D));
                if (surrounding.size() >= 3) {
                    galeCooldown = 200 - (galeLvl * 20);
                    this.playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 1.4F);
                    for (Monster m : surrounding) {
                        Vec3 diff = m.position().subtract(this.position());
                        Vec3 push = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(1.2D + galeLvl * 0.2D) : new Vec3(0, 0.5D, 0);
                        m.push(push.x, 0.35D, push.z);
                        m.hurt(this.damageSources().mobAttack(this), 2.0F + galeLvl);
                    }
                }
            }
            if (galeCooldown > 0) galeCooldown--;

            // Gate Purr - opens nearby doors/gates for owner
            int gateLvl = getTalentLevel(Talents.GATE_PURR);
            if (gateLvl > 0 && this.tickCount % 20 == 0) {
                BlockPos pos = this.blockPosition();
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
                    net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(p);
                    if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                        if (!state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
                            this.level().setBlock(p, state.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true), 3);
                            this.playSound(SoundEvents.FENCE_GATE_OPEN, 0.8F, 1.2F);
                        }
                    }
                }
            }

            // Charm Whiskers - Charms villagers and grants Hero of the Village effect
            int charmWhiskersLvl = getTalentLevel(Talents.CHARM_WHISKERS);
            if (charmWhiskersLvl > 0 && this.tickCount % 40 == 0) {
                double charmRadius = 8.0D + charmWhiskersLvl * 2.0D;
                List<net.minecraft.world.entity.npc.Villager> villagers = this.level().getEntitiesOfClass(
                        net.minecraft.world.entity.npc.Villager.class,
                        this.getBoundingBox().inflate(charmRadius),
                        LivingEntity::isAlive
                );
                if (!villagers.isEmpty()) {
                    int amp = Math.min(4, Math.max(0, charmWhiskersLvl - 1));
                    if (this.getOwner() instanceof Player owner && this.distanceToSqr(owner) <= (charmRadius * charmRadius)) {
                        owner.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 120, amp, false, false, true));
                    }
                    if (this.isTwined() && this.twinedPlayerUUID != null && this.getServer() != null) {
                        ServerPlayer twinedP = this.getServer().getPlayerList().getPlayer(this.twinedPlayerUUID);
                        if (twinedP != null) {
                            twinedP.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 120, amp, false, false, true));
                        }
                    }
                    if (this.tickCount % 80 == 0 && this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.6D, this.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
                        for (net.minecraft.world.entity.npc.Villager v : villagers) {
                            if (this.random.nextFloat() < 0.3F) {
                                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.2D, v.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    }
                }
            }

            // Sniffer Whiskers - digs up items from ground
            int snifferLvl = getTalentLevel(Talents.SNIFFER_WHISKERS);
            if (snifferLvl > 0 && this.tickCount % 1200 == 0 && this.onGround()) {
                net.minecraft.world.level.block.state.BlockState ground = this.level().getBlockState(this.blockPosition().below());
                if (ground.is(net.minecraft.tags.BlockTags.DIRT)) {
                    this.playSound(SoundEvents.SNIFFER_DIGGING, 0.8F, 1.2F);
                    ItemStack[] drops = { new ItemStack(Items.WHEAT_SEEDS, 2), new ItemStack(Items.PUMPKIN_SEEDS), new ItemStack(Items.MELON_SEEDS), new ItemStack(Items.BEETROOT_SEEDS), new ItemStack(Items.BONE), new ItemStack(Items.IRON_NUGGET, 2), new ItemStack(ModItems.CATNIP.get()) };
                    ItemStack reward = drops[this.random.nextInt(drops.length)];
                    Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), reward);
                }
            }

            // Alchemy Kitty - gives beneficial status effects
            int alchemyLvl = getTalentLevel(Talents.ALCHEMY_KITTY);
            if (alchemyLvl > 0 && this.tickCount % 1600 == 0) {
                if (this.getOwner() instanceof Player owner && this.distanceToSqr(owner) < 64.0D) {
                    MobEffectInstance[] effects = {
                        new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1),
                        new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0),
                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0),
                        new MobEffectInstance(MobEffects.DIG_SPEED, 400, 1)
                    };
                    MobEffectInstance selected = effects[this.random.nextInt(effects.length)];
                    owner.addEffect(selected);
                    this.playSound(SoundEvents.BREWING_STAND_BREW, 0.8F, 1.2F);
                }
            }

            // Herding Kitty - attracts livestock
            int herdingLvl = getTalentLevel(Talents.HERDING_KITTY);
            if (herdingLvl > 0 && this.tickCount % 40 == 0) {
                List<Animal> animals = this.level().getEntitiesOfClass(Animal.class, this.getBoundingBox().inflate(6.0D + herdingLvl * 2.0D), a -> !(a instanceof Cat));
                for (Animal a : animals) {
                    if (a.getTarget() == null && this.random.nextFloat() < 0.3F) {
                        a.getNavigation().moveTo(this, 1.0D);
                    }
                }
            }

            // Paw Retriever - attracts dropped items
            int retrieverLvl = getTalentLevel(Talents.PAW_RETRIEVER);
            if (retrieverLvl > 0 && this.tickCount % 10 == 0) {
                List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(4.0D + retrieverLvl * 2.0D));
                for (ItemEntity item : items) {
                    if (item.isAlive() && !item.hasPickUpDelay()) {
                        Vec3 diff = this.position().subtract(item.position());
                        Vec3 toCat = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(0.35D) : new Vec3(0, 0, 0);
                        item.setDeltaMovement(toCat.x, 0.1D, toCat.z);
                        this.incrementItemsRetrieved();
                    }
                }
            }

            // Catnip Catastrophe - emergency shockwave when low health
            int catastropheLvl = getTalentLevel(Talents.CATNIP_CATASTROPHE);
            if (catastropheLvl > 0 && this.getHealth() < this.getMaxHealth() * 0.25F && catastropheCooldown <= 0) {
                catastropheCooldown = 400;
                this.playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.5F);
                List<Monster> nearby = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(6.0D));
                for (Monster m : nearby) {
                    Vec3 diff = m.position().subtract(this.position());
                    Vec3 blast = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(1.8D) : new Vec3(0, 0.5D, 0);
                    m.push(blast.x, 0.5D, blast.z);
                    m.hurt(this.damageSources().mobAttack(this), 8.0F + catastropheLvl * 3.0F);
                }
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0, 0, 0, 0);
                }
            }
            if (catastropheCooldown > 0) catastropheCooldown--;
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
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide) {
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
