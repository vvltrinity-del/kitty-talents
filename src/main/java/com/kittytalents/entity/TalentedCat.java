package com.kittytalents.entity;

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

    private final Map<String, Integer> talentLevels = new HashMap<>();
    private final SimpleContainer catPack = new SimpleContainer(27);
    private final SimpleContainer ammoPouch = new SimpleContainer(9);
    private final com.kittytalents.work.JobData jobData = new com.kittytalents.work.JobData();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> catItemCapability =
            net.minecraftforge.common.util.LazyOptional.of(() -> new com.kittytalents.work.CatAmmoAndPackItemHandler(this));

    private ItemStack equippedWeapon = ItemStack.EMPTY;
    private ItemStack equippedTool = ItemStack.EMPTY;
    private ItemStack equippedHelmet = ItemStack.EMPTY;
    private ItemStack equippedChest = ItemStack.EMPTY;
    private ItemStack equippedLegs = ItemStack.EMPTY;
    private ItemStack equippedBoots = ItemStack.EMPTY;
    private ItemStack equippedAccessory = ItemStack.EMPTY;
    private final ItemStack[] cosmeticSlots = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

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
    private final Map<UUID, String> trustedPlayers = new LinkedHashMap<>();

    // Companion Statistics
    private int totalKills = 0;
    private int hostileKills = 0;
    private int passiveKills = 0;
    private float damageDealt = 0.0F;
    private float damageTaken = 0.0F;
    private double distanceTraveled = 0.0D;
    private int foodConsumed = 0;

    // RPG System
    private com.kittytalents.combat.CatRole catRole = com.kittytalents.combat.CatRole.NONE;
    private com.kittytalents.combat.FelineElement primaryElement = com.kittytalents.combat.FelineElement.PHYSICAL;
    private com.kittytalents.combat.FelineElement secondaryElement = com.kittytalents.combat.FelineElement.PHYSICAL;
    private String abilitySlot1 = "pounce";
    private String abilitySlot2 = "feline_hiss";
    private String abilitySlot3 = "healing_purr";
    private String abilityUltimate = "catnip_frenzy";
    private int energy = 100;

    public com.kittytalents.combat.CatRole getCatRole() { return catRole; }
    public void setCatRole(com.kittytalents.combat.CatRole role) { this.catRole = role; }
    
    public com.kittytalents.combat.FelineElement getPrimaryElement() { return primaryElement; }
    public void setPrimaryElement(com.kittytalents.combat.FelineElement element) { this.primaryElement = element; }
    
    public com.kittytalents.combat.FelineElement getSecondaryElement() { return secondaryElement; }
    public void setSecondaryElement(com.kittytalents.combat.FelineElement element) { this.secondaryElement = element; }

    public String getAbilitySlot1() { return (abilitySlot1 == null || abilitySlot1.isEmpty()) ? "pounce" : abilitySlot1; }
    public void setAbilitySlot1(String slot) { this.abilitySlot1 = (slot == null || slot.isEmpty()) ? "pounce" : slot; }
    public String getAbilitySlot2() { return (abilitySlot2 == null || abilitySlot2.isEmpty()) ? "feline_hiss" : abilitySlot2; }
    public void setAbilitySlot2(String slot) { this.abilitySlot2 = (slot == null || slot.isEmpty()) ? "feline_hiss" : slot; }
    public String getAbilitySlot3() { return (abilitySlot3 == null || abilitySlot3.isEmpty()) ? "healing_purr" : abilitySlot3; }
    public void setAbilitySlot3(String slot) { this.abilitySlot3 = (slot == null || slot.isEmpty()) ? "healing_purr" : slot; }
    public String getAbilityUltimate() { return (abilityUltimate == null || abilityUltimate.isEmpty()) ? "catnip_frenzy" : abilityUltimate; }
    public void setAbilityUltimate(String slot) { this.abilityUltimate = (slot == null || slot.isEmpty()) ? "catnip_frenzy" : slot; }

    public int getEnergy() { return this.entityData != null ? this.entityData.get(DATA_ENERGY) : energy; }
    public int getMaxEnergy() { return com.kittytalents.combat.FelineEnergyEngine.getMaxEnergy(this); }
    public void setEnergy(int energy) {
        this.energy = Math.max(0, energy);
        if (this.entityData != null) {
            this.entityData.set(DATA_ENERGY, this.energy);
        }
    }

    private int outOfCombatTicks = 100;
    public int getOutOfCombatTicks() { return outOfCombatTicks; }
    public void setOutOfCombatTicks(int val) { this.outOfCombatTicks = val; }
    public void incrementOutOfCombatTicks() { this.outOfCombatTicks++; }
    public void resetOutOfCombatTicks() { this.outOfCombatTicks = 0; }

    /** Game time at which the Nine Lives instant revive is available again. */
    private long nineLivesReadyAt = 0L;
    public long getNineLivesReadyAt() { return nineLivesReadyAt; }

    private final Map<String, Long> abilityCooldowns = new HashMap<>();
    public long getAbilityCooldown(String id) {
        if (id == null) return 0L;
        Long cd = abilityCooldowns.get(id.toLowerCase());
        if (cd != null) return cd;
        long pCd = getPersistentData().getLong("cooldown_" + id.toLowerCase());
        abilityCooldowns.put(id.toLowerCase(), pCd);
        return pCd;
    }
    public void setAbilityCooldown(String id, long gameTime) {
        if (id != null) {
            abilityCooldowns.put(id.toLowerCase(), gameTime);
            getPersistentData().putLong("cooldown_" + id.toLowerCase(), gameTime);
        }
    }

    private ItemStack carriedToy = ItemStack.EMPTY;
    public ItemStack getCarriedToy() { return carriedToy; }
    public void setCarriedToy(ItemStack toy) { this.carriedToy = toy != null ? toy.copy() : ItemStack.EMPTY; }
    private int blocksMined = 0;
    private int pouncesPerformed = 0;
    private int itemsRetrieved = 0;
    private int creepersScared = 0;
    private int healingProvided = 0;
    private int timesPet = 0;
    private int frenzyActivations = 0;

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
        CatAIInitializer.initializeGoals(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isTwined();
            }
            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !isTwined();
            }
        });
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new CatFoxPounceGoal(this));
        this.goalSelector.addGoal(2, new com.kittytalents.entity.ai.CatGunAttackGoal(this, 1.25D, 8.0F, 22.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.35D, true) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isTwined() && !isIncapacitated() && !isOrderedToSit() && getCatMode() != CatMode.DOCILE && !hasEquippedGun();
            }
        });
        this.goalSelector.addGoal(4, new CatCreeperWardenGoal(this));
        this.goalSelector.addGoal(5, new CatRestOnBedGoal(this));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.25D, 10.0F, 2.5F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isTwined() && !isIncapacitated() && !isOrderedToSit() &&
                        (getCatMode() == CatMode.FOLLOW || getCatMode() == CatMode.GUARD || getCatMode() == CatMode.TACTICAL);
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !isTwined() && !isIncapacitated() && !isOrderedToSit() &&
                        (getCatMode() == CatMode.FOLLOW || getCatMode() == CatMode.GUARD || getCatMode() == CatMode.TACTICAL);
            }

            @Override
            public void tick() {
                super.tick();
                LivingEntity owner = getOwner();
                if (owner instanceof Player p && p.isSprinting()) {
                    if (getNavigation().getPath() != null) {
                        setSpeed((float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6D));
                    }
                }
            }
        });
        this.goalSelector.addGoal(7, new CatUseScratcherGoal(this));
        this.goalSelector.addGoal(8, new CatEatFromBowlGoal(this));
        this.goalSelector.addGoal(9, new CatFetchToyGoal(this));
        this.goalSelector.addGoal(11, new CatComfortGoal(this));
        this.goalSelector.addGoal(13, new com.kittytalents.entity.ai.CatPlaceTorchGoal(this));
        this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isTwined() && !isIncapacitated() && !isOrderedToSit() &&
                        (getCatMode() == CatMode.WANDER || getCatMode() == CatMode.PATROL);
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !isTwined() && !isIncapacitated() && !isOrderedToSit() &&
                        (getCatMode() == CatMode.WANDER || getCatMode() == CatMode.PATROL);
            }
        });
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isTwined();
            }
            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !isTwined();
            }
        });
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isTwined();
            }
            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !isTwined();
            }
        });

        // Target Selectors
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isIncapacitated() && getCatMode() != CatMode.DOCILE && getCatMode() != CatMode.WANDER;
            }
        });
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isIncapacitated() && getCatMode() != CatMode.DOCILE && getCatMode() != CatMode.WANDER;
            }
        });
        this.targetSelector.addGoal(2, new com.kittytalents.entity.ai.TrustedPlayerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.kittytalents.entity.ai.TrustedPlayerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isIncapacitated() && getCatMode() != CatMode.DOCILE;
            }
        });
        // Guard Mode: defends a 16 block area against monsters
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (mob) -> {
            return !isIncapacitated() && getCatMode() == CatMode.GUARD && distanceToSqr(mob) <= 256.0D;
        }));
        // Aggressive Mode: actively searches out monsters
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (mob) -> {
            return !isIncapacitated() && getCatMode() == CatMode.AGGRESSIVE;
        }));
        // Pest Hunter goal: attacks spiders, silverfish, endermites, phantoms
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Spider.class, 10, true, false, (mob) -> {
            return !isIncapacitated() && getTalentLevel(Talents.PEST_HUNTER) > 0 && getCatMode() != CatMode.DOCILE;
        }));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Silverfish.class, 10, true, false, (mob) -> {
            return !isIncapacitated() && getTalentLevel(Talents.PEST_HUNTER) > 0 && getCatMode() != CatMode.DOCILE;
        }));
        // Hunting Mode: hunts small pests and hostile prey
        this.targetSelector.addGoal(5, new CatMouserGoal(this));
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
        this.foodConsumed += Math.max(1, amount / 10);
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

    public void setCatMode(CatMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
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
        this.getPersistentData().putString("MutationType", name);
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
        if (!equippedTool.isEmpty() && (equippedTool.is(net.minecraft.world.item.Items.TORCH) || equippedTool.is(net.minecraft.world.item.Items.SOUL_TORCH))) {
            torchStack = equippedTool;
            inToolSlot = true;
        } else {
            for (int i = 0; i < catPack.getContainerSize(); i++) {
                ItemStack st = catPack.getItem(i);
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
                equippedTool.shrink(1);
                if (equippedTool.isEmpty()) setEquippedTool(ItemStack.EMPTY);
            } else if (packSlot >= 0) {
                catPack.removeItem(packSlot, 1);
            }
            return true;
        }
        return false;
    }

    public boolean tryUseEquippedTool() {
        if (this.isIncapacitated() || equippedTool.isEmpty()) return false;
        BlockPos frontPos = this.blockPosition().relative(this.getDirection());
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(frontPos);
        if (!state.isAir()) {
            if (equippedTool.getItem() instanceof net.minecraft.world.item.DiggerItem digger) {
                if (digger.isCorrectToolForDrops(state)) {
                    this.level().destroyBlock(frontPos, true, this);
                    equippedTool.hurtAndBreak(1, this, c -> {
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
            TwineManager.register(player.getUUID(), this.getUUID());
        } else {
            this.twineXxa = 0.0F;
            this.twineZza = 0.0F;
        }
    }

    /** Clears local twine state only (session registry is handled by TwineManager). */
    public void clearTwineState() {
        this.twinedPlayerUUID = null;
        this.entityData.set(DATA_TWINED, false);
        this.twineXxa = 0.0F;
        this.twineZza = 0.0F;
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

    public int getTalentLevel(Talent talent) {
        if (talent != null && talent.isStandard()) {
            return Math.max(1, talentLevels.getOrDefault(talent.getId(), 5));
        }
        return talentLevels.getOrDefault(talent.getId(), 0);
    }

    public void setAllTalentLevels(Map<String, Integer> levels) {
        this.talentLevels.clear();
        if (levels != null) {
            this.talentLevels.putAll(levels);
        }
    }

    public Map<String, Integer> getAllTalentLevels() {
        return Collections.unmodifiableMap(this.talentLevels);
    }

    public void syncTalents(ServerPlayer player) {
        if (!this.level().isClientSide && player != null) {
            com.kittytalents.network.PacketSyncTalents packet =
                    new com.kittytalents.network.PacketSyncTalents(this.getId(), this.getSkillPoints(), this.talentLevels);
            PacketHandler.sendToPlayer(packet, player);
        }
    }

    public void syncTalents() {
        if (!this.level().isClientSide) {
            com.kittytalents.network.PacketSyncTalents packet =
                    new com.kittytalents.network.PacketSyncTalents(this.getId(), this.getSkillPoints(), this.talentLevels);
            PacketHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    packet
            );
        }
    }

    public void setTalentLevel(Talent talent, int level) {
        talentLevels.put(talent.getId(), Math.max(0, Math.min(talent.getMaxLevel(), level)));
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
            for (int r = 1; r <= lvl; r++) {
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
        talentLevels.clear();
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
    public ItemStack getEquippedWeapon() { return equippedWeapon; }
    public void setEquippedWeapon(ItemStack stack) {
        this.equippedWeapon = stack.copy();
        updateAttributes();
        syncEquipment();
        if (!this.level().isClientSide && com.kittytalents.compat.tacz.TacZCompat.isGun(this.equippedWeapon)) {
            com.kittytalents.compat.tacz.TacZCompat.onGunEquipped(this, this.equippedWeapon);
        }
    }
    public void setEquippedWeaponClient(ItemStack stack) { this.equippedWeapon = stack.copy(); updateAttributes(); }

    public ItemStack getEquippedTool() { return equippedTool; }
    public void setEquippedTool(ItemStack stack) { this.equippedTool = stack.copy(); syncEquipment(); }
    public void setEquippedToolClient(ItemStack stack) { this.equippedTool = stack.copy(); }

    public ItemStack getArmorSlotItem(int slot) {
        return switch (slot) {
            case 1 -> equippedHelmet;
            case 2 -> equippedChest;
            case 3 -> equippedLegs;
            case 4 -> equippedBoots;
            default -> ItemStack.EMPTY;
        };
    }
    public void setArmorSlotItem(int slot, ItemStack stack) {
        switch (slot) {
            case 1 -> equippedHelmet = stack.copy();
            case 2 -> equippedChest = stack.copy();
            case 3 -> equippedLegs = stack.copy();
            case 4 -> equippedBoots = stack.copy();
        }
        updateAttributes();
        syncEquipment();
    }
    public void setArmorSlotItemClient(int slot, ItemStack stack) {
        switch (slot) {
            case 1 -> equippedHelmet = stack.copy();
            case 2 -> equippedChest = stack.copy();
            case 3 -> equippedLegs = stack.copy();
            case 4 -> equippedBoots = stack.copy();
        }
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

    public ItemStack getEquippedAccessory() { return equippedAccessory; }
    public void setEquippedAccessory(ItemStack stack) { this.equippedAccessory = stack.copy(); syncEquipment(); }
    public void setEquippedAccessoryClient(ItemStack stack) { this.equippedAccessory = stack.copy(); }

    public ItemStack getCosmeticSlot(int index) {
        if (index >= 0 && index < cosmeticSlots.length) return cosmeticSlots[index];
        return ItemStack.EMPTY;
    }
    public void setCosmeticSlot(int index, ItemStack stack) {
        if (index >= 0 && index < cosmeticSlots.length) {
            cosmeticSlots[index] = stack.copy();
            syncEquipment();
        }
    }
    public void setCosmeticSlotClient(int index, ItemStack stack) {
        if (index >= 0 && index < cosmeticSlots.length) {
            cosmeticSlots[index] = stack.copy();
        }
    }

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
                    this.equippedWeapon,
                    this.equippedTool,
                    this.equippedHelmet,
                    this.equippedChest,
                    this.equippedLegs,
                    this.equippedBoots,
                    this.equippedAccessory,
                    this.cosmeticSlots[0],
                    this.cosmeticSlots[1],
                    this.cosmeticSlots[2],
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
                    this.equippedWeapon,
                    this.equippedTool,
                    this.equippedHelmet,
                    this.equippedChest,
                    this.equippedLegs,
                    this.equippedBoots,
                    this.equippedAccessory,
                    this.cosmeticSlots[0],
                    this.cosmeticSlots[1],
                    this.cosmeticSlots[2],
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
                    else catPack.addItem(old);
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
            for (int i = 0; i < cosmeticSlots.length; i++) {
                if (getCosmeticSlot(i).isEmpty()) {
                    setCosmeticSlot(i, stack.split(1));
                    playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        // Cosmetics (Collars, Capes, etc.)
        if (stack.getItem() instanceof com.kittytalents.item.CatCollarItem) {
            for (int i = 0; i < cosmeticSlots.length; i++) {
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
    public Map<UUID, String> getTrustedPlayers() { return trustedPlayers; }
    public boolean isTrusted(UUID id) { return id != null && trustedPlayers.containsKey(id); }
    public boolean isTrusted(Player player) { return player != null && isTrusted(player.getUUID()); }
    public void addTrustedPlayer(UUID id, String name) { trustedPlayers.put(id, name); }
    public void removeTrustedPlayer(UUID id) { trustedPlayers.remove(id); }
    public void setTrustedPlayers(Map<UUID, String> players) {
        trustedPlayers.clear();
        trustedPlayers.putAll(players);
    }
    public void syncTrust() {
        if (!this.level().isClientSide) {
            PacketHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new com.kittytalents.network.PacketSyncTrustedPlayers(this.getId(), this.trustedPlayers)
            );
        }
    }
    public void syncTrust(ServerPlayer player) {
        if (!this.level().isClientSide && player != null) {
            PacketHandler.sendToPlayer(
                    new com.kittytalents.network.PacketSyncTrustedPlayers(this.getId(), this.trustedPlayers),
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

    public void resetStatistics() {
        this.totalKills = 0;
        this.hostileKills = 0;
        this.passiveKills = 0;
        this.damageDealt = 0.0F;
        this.damageTaken = 0.0F;
        this.distanceTraveled = 0.0D;
        this.foodConsumed = 0;
        this.blocksMined = 0;
        this.pouncesPerformed = 0;
        this.itemsRetrieved = 0;
        this.creepersScared = 0;
        this.healingProvided = 0;
        this.timesPet = 0;
        this.frenzyActivations = 0;
    }

    public SimpleContainer getCatPack() { return catPack; }
    public SimpleContainer getAmmoPouch() { return ammoPouch; }
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

        if (!equippedWeapon.isEmpty()) {
            damage += 4.0F;
            if (entity instanceof LivingEntity livingTarget) {
                damage += net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(equippedWeapon, livingTarget.getMobType());
                int fireAspect = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, equippedWeapon);
                if (fireAspect > 0) {
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                int knockback = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK, equippedWeapon);
                if (knockback > 0) {
                    livingTarget.knockback(knockback * 0.5F, (double) net.minecraft.util.Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), (double) (-net.minecraft.util.Mth.cos(this.getYRot() * ((float) Math.PI / 180F))));
                }
            }
            if (!this.level().isClientSide) {
                equippedWeapon.hurtAndBreak(1, this, (cat) -> {
                    this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
                    this.equippedWeapon = ItemStack.EMPTY;
                    syncEquipment();
                });
            }
        }

        // Apotheosis attack modifiers (crit, elemental damage, life-steal, armor pierce)
        damage = com.kittytalents.compat.apotheosis.ApotheosisCompat.applyAttackModifiers(this, entity, damage);

        boolean hurt = entity.hurt(this.damageSources().mobAttack(this), damage);
        if (hurt) {
            this.damageDealt += damage;
            if (!entity.isAlive()) {
                this.totalKills++;
                if (entity instanceof Monster) {
                    this.hostileKills++;
                } else {
                    this.passiveKills++;
                }
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

        this.damageTaken += amount;

        int agility = getTalentLevel(Talents.FELINE_AGILITY);
        if (source.isIndirect() && agility > 0) {
            if (this.random.nextFloat() < (agility * 0.10F)) {
                this.playSound(SoundEvents.CAT_PURREOW, 0.7F, 1.5F);
                return false;
            }
        }

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
            if (nineLives > 0 && !isIncapacitated() && now >= nineLivesReadyAt) {
                // Instant revive recharges after (10 min - 1 min per rank); otherwise the cat goes down instead.
                this.nineLivesReadyAt = now + Math.max(1200L, 12000L - 1200L * nineLives);
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
            this.distanceTraveled += deltaMovementLength;
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
        ItemStack held = player.getItemInHand(hand);

        // Taming untamed Talented Cat
        if (!this.isTame() || this.getOwnerUUID() == null) {
            if (held.is(ModItems.CATNIP_TREAT.get())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!this.level().isClientSide) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7); // Heart particles
                    player.sendSystemMessage(Component.literal("You tamed " + this.getName().getString() + "!").withStyle(ChatFormatting.GREEN));
                }
                this.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else if (held.is(Items.COD) || held.is(Items.SALMON)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!this.level().isClientSide) {
                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.setOrderedToSit(true);
                        this.level().broadcastEntityEvent(this, (byte) 7); // Heart particles
                        player.sendSystemMessage(Component.literal("You tamed " + this.getName().getString() + "!").withStyle(ChatFormatting.GREEN));
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6); // Smoke particles
                    }
                }
                this.playSound(SoundEvents.CAT_EAT, 0.8F, 1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else if (held.is(ModItems.CATNIP.get())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!this.level().isClientSide) {
                    if (this.random.nextInt(2) == 0) {
                        this.tame(player);
                        this.setOrderedToSit(true);
                        this.level().broadcastEntityEvent(this, (byte) 7); // Heart particles
                        player.sendSystemMessage(Component.literal("You tamed " + this.getName().getString() + " with Catnip!").withStyle(ChatFormatting.GREEN));
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6); // Smoke particles
                    }
                }
                this.playSound(SoundEvents.CAT_PURREOW, 0.9F, 1.1F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Storing Gun Ammo into ammo pouch / backpack by right-clicking with ammo
        if (com.kittytalents.compat.tacz.TacZCompat.isAmmo(held) && (isOwnedBy(player) || isTrusted(player.getUUID()))) {
            ItemStack remaining = this.ammoPouch.addItem(held.copy());
            if (!remaining.isEmpty() && getPackSlotCount() > 0) {
                remaining = this.catPack.addItem(remaining);
            }
            int added = held.getCount() - remaining.getCount();
            if (added > 0) {
                if (!player.getAbilities().instabuild) {
                    held.setCount(remaining.getCount());
                }
                this.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.2F);
                if (!this.level().isClientSide) {
                    player.sendSystemMessage(Component.literal("Stored " + added + " ammo in " + this.getName().getString() + "'s ammo pouch!").withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Direct equip by right-clicking with gun or weapon
        if ((isOwnedBy(player) || isTrusted(player.getUUID())) && (com.kittytalents.compat.tacz.TacZCompat.isGun(held) || held.getItem() instanceof net.minecraft.world.item.SwordItem)) {
            if (tryAutoEquip(held, player)) {
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Right-clicking with Stick or Laser Pointer opens companion talent screen
        if ((isOwnedBy(player) || isTrusted(player.getUUID())) && (held.is(Items.STICK) || held.is(ModItems.CAT_LASER_POINTER.get()))) {
            if (player instanceof ServerPlayer serverPlayer) {
                openTalentScreen(serverPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Coal Stats Reset Protection (Sneak + Right-Click with Coal or Charcoal)
        if (player.isShiftKeyDown() && (isPrimaryOwner(player) || player.getAbilities().instabuild) && (held.is(Items.COAL) || held.is(Items.CHARCOAL))) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            resetStatistics();
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.8F);
            if (this.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 15; i++) {
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.3D, this.getZ(),
                            1, (serverLevel.random.nextDouble() - 0.5D) * 0.3D, 0.1D, (serverLevel.random.nextDouble() - 0.5D) * 0.3D, 0.02D);
                }
            }
            player.displayClientMessage(Component.literal(this.getName().getString() + "'s stats and memories have been wiped clean.").withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Petting with sneak + bare hand!
        if (player.isShiftKeyDown() && isOwnedBy(player) && held.isEmpty()) {
            this.playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
            this.playSound(SoundEvents.CAT_PURREOW, 0.9F, 1.2F);
            if (this.level().isClientSide) {
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(ParticleTypes.HEART, this.getRandomX(0.5D), this.getY() + 0.4D, this.getRandomZ(0.5D), 0, 0.05D, 0);
                }
            } else {
                this.heal(2.0F);
                this.increaseHunger(5);
                String[] petStyles = {"Head Scratch", "Chin Scratch", "Belly Rub", "Back Scratch"};
                String style = petStyles[this.random.nextInt(petStyles.length)];
                player.displayClientMessage(Component.literal("You gave " + this.getName().getString() + " a loving " + style + "! ❤").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Colorable Collar with Dyes
        if (held.getItem() instanceof DyeItem dye && isOwnedBy(player)) {
            DyeColor dyeColor = dye.getDyeColor();
            if (dyeColor != this.getCollarColor()) {
                this.setCollarColor(dyeColor);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                this.playSound(SoundEvents.DYE_USE, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Dire Treat Awakening
        if (held.is(ModItems.DIRE_TREAT.get()) && isOwnedBy(player)) {
            if (!isDireAwakened()) {
                int reqLvl = KittyTalentsConfig.COMMON.direLevelRequirement.get();
                if (getCatLevel() >= reqLvl) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    setDireAwakened(true);
                    this.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
                    this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.8F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 30; i++) {
                            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 0.5D, this.getZ(),
                                    1, (serverLevel.random.nextDouble() - 0.5D), 0.5D, (serverLevel.random.nextDouble() - 0.5D), 0.1D);
                        }
                    }
                    player.sendSystemMessage(Component.translatable("message.kittytalents.dire_awakened", this.getName()));
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                } else {
                    if (!this.level().isClientSide) {
                        player.sendSystemMessage(Component.translatable("message.kittytalents.dire_level_req", reqLvl));
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            }
        }

        // Reviving with Warm Milk or Golden Apple
        if (isIncapacitated()) {
            if (held.getItem() instanceof WarmMilkItem || held.is(Items.MILK_BUCKET) || held.is(Items.GOLDEN_APPLE)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.is(Items.MILK_BUCKET)) {
                        player.addItem(new ItemStack(Items.BUCKET));
                    }
                }
                this.setIncapacitated(false);
                this.heal(12.0F);
                this.increaseHunger(50);
                this.playSound(SoundEvents.PLAYER_BURP, 0.8F, 1.2F);
                this.playSound(SoundEvents.CAT_PURREOW, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                if (!this.level().isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.kittytalents.needs_milk"));
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Treats for leveling
        if (held.getItem() instanceof CatTreatItem treatItem && isOwnedBy(player)) {
            if (treatItem.canLevelUp(this)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                setCatLevel(getCatLevel() + 1);
                setSkillPoints(getSkillPoints() + 1);
                increaseHunger(20);
                this.heal(4.0F);
                this.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.3F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                if (!this.level().isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.kittytalents.treat_too_low"));
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Fish & meats for food & healing
        if (held.is(Items.COD) || held.is(Items.COOKED_COD) || held.is(Items.SALMON) || held.is(Items.COOKED_SALMON) ||
                (getTalentLevel(Talents.HAPPY_EATER) > 0 && (held.is(Items.CHICKEN) || held.is(Items.COOKED_CHICKEN) || held.is(Items.BEEF) || held.is(Items.PORKCHOP)))) {
            if (getHunger() < 120 || getHealth() < getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                increaseHunger(25);
                heal(6.0F);
                playSound(SoundEvents.CAT_EAT, 0.8F, 1.0F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // Mounting the cat
        if (getTalentLevel(Talents.CAT_MOUNT) > 0 && (isOwnedBy(player) || isTrusted(player.getUUID()) || player.getAbilities().instabuild) && (held.isEmpty() || held.is(Items.SADDLE))) {
            if (!this.level().isClientSide && !this.isVehicle()) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Normal right click toggles sit/stand
        if ((isOwnedBy(player) || isTrusted(player.getUUID())) && held.isEmpty()) {
            setCatMode(getCatMode() == CatMode.DOCILE ? CatMode.FOLLOW : CatMode.DOCILE);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    public void openTalentScreen(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CatTalentMenu(containerId, playerInventory, this),
                this.getName()
        ), buf -> buf.writeInt(this.getId()));
        com.kittytalents.network.PacketHandler.sendToPlayer(
                new com.kittytalents.network.PacketSyncTrustedPlayers(this.getId(), this.trustedPlayers), player);
        syncEquipment(player);
        syncTalents(player);
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
    public int getTotalKills() { return totalKills; }
    public int getHostileKills() { return hostileKills; }
    public int getPassiveKills() { return passiveKills; }
    public float getDamageDealt() { return damageDealt; }
    public float getDamageTaken() { return damageTaken; }
    public double getDistanceTraveled() { return distanceTraveled; }
    public int getFoodConsumed() { return foodConsumed; }
    public int getBlocksMined() { return blocksMined; }
    public void incrementBlocksMined() { blocksMined++; }
    public int getPouncesPerformed() { return pouncesPerformed; }
    public void incrementPounces() { pouncesPerformed++; }
    public int getItemsRetrieved() { return itemsRetrieved; }
    public void incrementItemsRetrieved() { itemsRetrieved++; }
    public int getCreepersScared() { return creepersScared; }
    public void incrementCreepersScared() { creepersScared++; }
    public int getHealingProvided() { return healingProvided; }
    public void addHealingProvided(int amount) { healingProvided += amount; }
    public int getTimesPet() { return timesPet; }
    public void incrementTimesPet() { timesPet++; }
    public int getFrenzyActivations() { return frenzyActivations; }
    public void incrementFrenzyActivations() { frenzyActivations++; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CatLevel", getCatLevel());
        tag.putInt("SkillPoints", getSkillPoints());
        tag.putInt("Hunger", getHunger());
        tag.putFloat("Satiety", satiety);
        tag.putInt("CatMode", getCatMode().ordinal());
        tag.putBoolean("Incapacitated", isIncapacitated());
        tag.putLong("NineLivesReadyAt", nineLivesReadyAt);
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

        tag.putInt("TotalKills", totalKills);
        tag.putInt("HostileKills", hostileKills);
        tag.putInt("PassiveKills", passiveKills);
        tag.putFloat("DamageDealt", damageDealt);
        tag.putFloat("DamageTaken", damageTaken);
        tag.putDouble("DistanceTraveled", distanceTraveled);
        tag.putInt("FoodConsumed", foodConsumed);
        tag.putInt("BlocksMined", blocksMined);
        tag.putInt("PouncesPerformed", pouncesPerformed);
        tag.putInt("ItemsRetrieved", itemsRetrieved);
        tag.putInt("CreepersScared", creepersScared);
        tag.putInt("HealingProvided", healingProvided);
        tag.putInt("TimesPet", timesPet);
        tag.putInt("FrenzyActivations", frenzyActivations);

        if (!equippedWeapon.isEmpty()) tag.put("EquippedWeapon", equippedWeapon.save(new CompoundTag()));
        if (!equippedTool.isEmpty()) tag.put("EquippedTool", equippedTool.save(new CompoundTag()));
        if (!equippedHelmet.isEmpty()) tag.put("EquippedHelmet", equippedHelmet.save(new CompoundTag()));
        if (!equippedChest.isEmpty()) tag.put("EquippedChest", equippedChest.save(new CompoundTag()));
        if (!equippedLegs.isEmpty()) tag.put("EquippedLegs", equippedLegs.save(new CompoundTag()));
        if (!equippedBoots.isEmpty()) tag.put("EquippedBoots", equippedBoots.save(new CompoundTag()));
        if (!equippedAccessory.isEmpty()) tag.put("EquippedAccessory", equippedAccessory.save(new CompoundTag()));
        for (int i = 0; i < cosmeticSlots.length; i++) {
            if (!cosmeticSlots[i].isEmpty()) {
                tag.put("CosmeticSlot" + i, cosmeticSlots[i].save(new CompoundTag()));
            }
        }
        if (!carriedToy.isEmpty()) tag.put("CarriedToy", carriedToy.save(new CompoundTag()));
        tag.putString("CatRole", catRole.name());
        tag.putString("PrimaryElement", primaryElement.name());
        tag.putString("SecondaryElement", secondaryElement.name());
        tag.putString("AbilitySlot1", abilitySlot1 != null ? abilitySlot1 : "");
        tag.putString("AbilitySlot2", abilitySlot2 != null ? abilitySlot2 : "");
        tag.putString("AbilitySlot3", abilitySlot3 != null ? abilitySlot3 : "");
        tag.putString("AbilityUltimate", abilityUltimate != null ? abilityUltimate : "");
        tag.putInt("Energy", energy);

        ListTag trustList = new ListTag();
        for (Map.Entry<UUID, String> e : trustedPlayers.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.getKey());
            t.putString("Name", e.getValue());
            trustList.add(t);
        }
        tag.put("TrustedPlayers", trustList);

        CompoundTag talentsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : talentLevels.entrySet()) {
            talentsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Talents", talentsTag);

        ListTag invTag = new ListTag();
        for (int i = 0; i < catPack.getContainerSize(); i++) {
            ItemStack stack = catPack.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                invTag.add(itemTag);
            }
        }
        tag.put("CatPack", invTag);

        ListTag ammoTag = new ListTag();
        for (int i = 0; i < ammoPouch.getContainerSize(); i++) {
            ItemStack stack = ammoPouch.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                ammoTag.add(itemTag);
            }
        }
        tag.put("AmmoPouch", ammoTag);
        tag.put("JobData", jobData.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CatLevel")) setCatLevel(tag.getInt("CatLevel"));
        if (tag.contains("SkillPoints")) setSkillPoints(tag.getInt("SkillPoints"));
        if (tag.contains("Hunger")) setHunger(tag.getInt("Hunger"));
        this.satiety = tag.getFloat("Satiety");
        if (tag.contains("CatMode")) setCatMode(CatMode.byId(tag.getInt("CatMode")));
        if (tag.contains("Incapacitated")) setIncapacitated(tag.getBoolean("Incapacitated"));
        this.nineLivesReadyAt = tag.getLong("NineLivesReadyAt");
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

        if (tag.contains("TotalKills")) this.totalKills = tag.getInt("TotalKills");
        if (tag.contains("HostileKills")) this.hostileKills = tag.getInt("HostileKills");
        if (tag.contains("PassiveKills")) this.passiveKills = tag.getInt("PassiveKills");
        if (tag.contains("DamageDealt")) this.damageDealt = tag.getFloat("DamageDealt");
        if (tag.contains("DamageTaken")) this.damageTaken = tag.getFloat("DamageTaken");
        if (tag.contains("DistanceTraveled")) this.distanceTraveled = tag.getDouble("DistanceTraveled");
        if (tag.contains("FoodConsumed")) this.foodConsumed = tag.getInt("FoodConsumed");
        if (tag.contains("BlocksMined")) this.blocksMined = tag.getInt("BlocksMined");
        if (tag.contains("PouncesPerformed")) this.pouncesPerformed = tag.getInt("PouncesPerformed");
        if (tag.contains("ItemsRetrieved")) this.itemsRetrieved = tag.getInt("ItemsRetrieved");
        if (tag.contains("CreepersScared")) this.creepersScared = tag.getInt("CreepersScared");
        if (tag.contains("HealingProvided")) this.healingProvided = tag.getInt("HealingProvided");
        if (tag.contains("TimesPet")) this.timesPet = tag.getInt("TimesPet");
        if (tag.contains("FrenzyActivations")) this.frenzyActivations = tag.getInt("FrenzyActivations");

        if (tag.contains("EquippedWeapon")) equippedWeapon = ItemStack.of(tag.getCompound("EquippedWeapon"));
        if (tag.contains("EquippedTool")) equippedTool = ItemStack.of(tag.getCompound("EquippedTool"));
        if (tag.contains("EquippedHelmet")) equippedHelmet = ItemStack.of(tag.getCompound("EquippedHelmet"));
        if (tag.contains("EquippedChest")) equippedChest = ItemStack.of(tag.getCompound("EquippedChest"));
        if (tag.contains("EquippedLegs")) equippedLegs = ItemStack.of(tag.getCompound("EquippedLegs"));
        if (tag.contains("EquippedBoots")) equippedBoots = ItemStack.of(tag.getCompound("EquippedBoots"));
        if (tag.contains("EquippedAccessory")) equippedAccessory = ItemStack.of(tag.getCompound("EquippedAccessory"));
        for (int i = 0; i < cosmeticSlots.length; i++) {
            if (tag.contains("CosmeticSlot" + i)) {
                cosmeticSlots[i] = ItemStack.of(tag.getCompound("CosmeticSlot" + i));
            } else {
                cosmeticSlots[i] = ItemStack.EMPTY;
            }
        }

        if (tag.contains("CarriedToy")) {
            this.carriedToy = ItemStack.of(tag.getCompound("CarriedToy"));
        } else {
            this.carriedToy = ItemStack.EMPTY;
        }

        if (tag.contains("CatRole")) {
            try {
                this.catRole = com.kittytalents.combat.CatRole.valueOf(tag.getString("CatRole"));
            } catch (Exception ignored) {
                this.catRole = com.kittytalents.combat.CatRole.NONE;
            }
        }
        if (tag.contains("PrimaryElement")) {
            try {
                this.primaryElement = com.kittytalents.combat.FelineElement.valueOf(tag.getString("PrimaryElement"));
            } catch (Exception ignored) {
                this.primaryElement = com.kittytalents.combat.FelineElement.PHYSICAL;
            }
        }
        if (tag.contains("SecondaryElement")) {
            try {
                this.secondaryElement = com.kittytalents.combat.FelineElement.valueOf(tag.getString("SecondaryElement"));
            } catch (Exception ignored) {
                this.secondaryElement = com.kittytalents.combat.FelineElement.PHYSICAL;
            }
        }
        if (tag.contains("AbilitySlot1")) setAbilitySlot1(tag.getString("AbilitySlot1"));
        if (tag.contains("AbilitySlot2")) setAbilitySlot2(tag.getString("AbilitySlot2"));
        if (tag.contains("AbilitySlot3")) setAbilitySlot3(tag.getString("AbilitySlot3"));
        if (tag.contains("AbilityUltimate")) setAbilityUltimate(tag.getString("AbilityUltimate"));
        if (tag.contains("Energy")) setEnergy(tag.getInt("Energy"));

        trustedPlayers.clear();
        if (tag.contains("TrustedPlayers", Tag.TAG_LIST)) {
            ListTag trustList = tag.getList("TrustedPlayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < trustList.size(); i++) {
                CompoundTag t = trustList.getCompound(i);
                if (t.hasUUID("Id")) {
                    trustedPlayers.put(t.getUUID("Id"), t.getString("Name"));
                }
            }
        }

        if (tag.contains("Talents", Tag.TAG_COMPOUND)) {
            CompoundTag talentsTag = tag.getCompound("Talents");
            talentLevels.clear();
            for (String key : talentsTag.getAllKeys()) {
                talentLevels.put(key, talentsTag.getInt(key));
            }
        }

        if (tag.contains("CatPack", Tag.TAG_LIST)) {
            ListTag invTag = tag.getList("CatPack", Tag.TAG_COMPOUND);
            catPack.clearContent();
            for (int i = 0; i < invTag.size(); i++) {
                CompoundTag itemTag = invTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < catPack.getContainerSize()) {
                    catPack.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
        if (tag.contains("AmmoPouch", Tag.TAG_LIST)) {
            ListTag ammoTag = tag.getList("AmmoPouch", Tag.TAG_COMPOUND);
            ammoPouch.clearContent();
            for (int i = 0; i < ammoTag.size(); i++) {
                CompoundTag itemTag = ammoTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < ammoPouch.getContainerSize()) {
                    ammoPouch.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
        if (tag.contains("JobData", Tag.TAG_COMPOUND)) {
            jobData.load(tag.getCompound("JobData"));
        }
        // Migrate legacy FARM / MINE / FISH modes to WORK + corresponding CatJob
        CatMode currentMode = getCatMode();
        if (currentMode == CatMode.FARM) {
            setCatMode(CatMode.WORK);
            setCatJob(com.kittytalents.work.CatJob.FARMER);
        } else if (currentMode == CatMode.MINE) {
            setCatMode(CatMode.WORK);
            setCatJob(com.kittytalents.work.CatJob.MINER);
        } else if (currentMode == CatMode.FISH) {
            setCatMode(CatMode.WORK);
            setCatJob(com.kittytalents.work.CatJob.FISHER);
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
            Containers.dropContents(this.level(), this, this.catPack);
            Containers.dropContents(this.level(), this, this.ammoPouch);
            if (!equippedWeapon.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedWeapon);
            if (!equippedTool.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedTool);
            if (!equippedHelmet.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedHelmet);
            if (!equippedChest.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedChest);
            if (!equippedLegs.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedLegs);
            if (!equippedBoots.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedBoots);
            if (!equippedAccessory.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), equippedAccessory);
            if (!carriedToy.isEmpty()) Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), carriedToy);
            for (ItemStack cosmetic : cosmeticSlots) {
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
            boolean spiritUsed = this.getPersistentData().getBoolean("SpiritCatnipUsed") ||
                    (otherParent instanceof TalentedCat o && o.getPersistentData().getBoolean("SpiritCatnipUsed"));
            this.getPersistentData().remove("SpiritCatnipUsed");
            if (otherParent instanceof TalentedCat o) {
                o.getPersistentData().remove("SpiritCatnipUsed");
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
