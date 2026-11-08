package com.kittytalents.client;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketTeamCommand;
import com.kittytalents.network.PacketTwineAction;
import com.kittytalents.network.PacketTwineInput;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Kitty Talents ClientTwineHandler
 * Decouples mouse look and camera from the human player body without wraparound spin
 * or pitch-clamp swirling, integrates with FirstPersonModel to prevent head/neck clipping,
 * and drives responsive client+server feline traversal, abilities, and gunplay.
 */
@Mod.EventBusSubscriber(modid = "kittytalents", value = Dist.CLIENT)
public class ClientTwineHandler {
    public static int activeTwinedCatId = -1;

    public static KeyMapping KEY_TWINE;
    public static KeyMapping KEY_UNTWINE;
    public static KeyMapping KEY_DODGE;
    public static KeyMapping KEY_SYNERGY;
    public static KeyMapping KEY_ABILITY_1;
    public static KeyMapping KEY_ABILITY_2;
    public static KeyMapping KEY_ABILITY_3;
    public static KeyMapping KEY_ULTIMATE;

    // Saved dormant player orientation restored upon untwining
    private static float dormantPlayerYaw = 0.0F;
    private static float dormantPlayerPitch = 0.0F;
    private static float dormantPlayerHeadYaw = 0.0F;
    private static float dormantPlayerBodyYaw = 0.0F;

    // Continuous unwrapped yaw and clamped pitch for the Twined cat camera
    private static float twineYaw = 0.0F;
    private static float twinePitch = 0.0F;
    private static boolean twineAnglesInitialized = false;

    private static boolean cameraBound = false;
    private static boolean firstPersonCompatRegistered = false;

    // Client action cooldowns
    private static int dodgeCooldownClient = 0;
    private static int synergyCooldownClient = 0;
    private static int rmbPounceCooldownClient = 0;

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        KEY_TWINE = new KeyMapping("key.kittytalents.twine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.kittytalents");
        KEY_UNTWINE = new KeyMapping("key.kittytalents.untwine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories.kittytalents");
        KEY_DODGE = new KeyMapping("key.kittytalents.dodge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.kittytalents");
        KEY_SYNERGY = new KeyMapping("key.kittytalents.synergy", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.kittytalents");
        KEY_ABILITY_1 = new KeyMapping("key.kittytalents.ability_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.kittytalents");
        KEY_ABILITY_2 = new KeyMapping("key.kittytalents.ability_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.kittytalents");
        KEY_ABILITY_3 = new KeyMapping("key.kittytalents.ability_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.kittytalents");
        KEY_ULTIMATE = new KeyMapping("key.kittytalents.ultimate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.kittytalents");

        event.register(KEY_TWINE);
        event.register(KEY_UNTWINE);
        event.register(KEY_DODGE);
        event.register(KEY_SYNERGY);
        event.register(KEY_ABILITY_1);
        event.register(KEY_ABILITY_2);
        event.register(KEY_ABILITY_3);
        event.register(KEY_ULTIMATE);

        registerFirstPersonModelGuard();
    }

    /**
     * If FirstPersonModel (tr7zw.firstperson) is installed, register an ActivationHandler
     * that disables FirstPersonModel's body-offset & forced entity render while Twined.
     */
    private static void registerFirstPersonModelGuard() {
        if (firstPersonCompatRegistered) return;
        firstPersonCompatRegistered = true;
        try {
            Class<?> apiClass = Class.forName("dev.tr7zw.firstperson.api.FirstPersonAPI");
            Class<?> handlerInterface = Class.forName("dev.tr7zw.firstperson.api.ActivationHandler");
            Object proxy = Proxy.newProxyInstance(
                    handlerInterface.getClassLoader(),
                    new Class<?>[]{handlerInterface},
                    (Object obj, Method method, Object[] args) -> {
                        if ("preventFirstperson".equals(method.getName())) {
                            return isTwined();
                        }
                        return false;
                    }
            );
            Method registerMethod = apiClass.getMethod("registerPlayerHandler", Object.class);
            registerMethod.invoke(null, proxy);
        } catch (Throwable ignored) {
            // firstperson-forge not installed; safe to ignore
        }
    }

    public static boolean isTwined() {
        return activeTwinedCatId != -1;
    }

    public static void handleSyncTwine(int catEntityId, boolean twined) {
        registerFirstPersonModelGuard();
        Minecraft mc = Minecraft.getInstance();
        if (twined) {
            if (mc.player != null && !isTwined()) {
                dormantPlayerYaw = Mth.wrapDegrees(mc.player.getYRot());
                dormantPlayerPitch = Mth.clamp(mc.player.getXRot(), -90.0F, 90.0F);
                dormantPlayerHeadYaw = Mth.wrapDegrees(mc.player.getYHeadRot());
                dormantPlayerBodyYaw = Mth.wrapDegrees(mc.player.yBodyRot);
            }
            activeTwinedCatId = catEntityId;
            twineAnglesInitialized = false;
            cameraBound = false;

            if (mc.level != null) {
                Entity catEntity = mc.level.getEntity(catEntityId);
                if (catEntity instanceof TalentedCat cat) {
                    bindCamera(mc, cat);
                    cat.setMaxUpStep(1.0F);
                    cat.playSound(com.kittytalents.registry.ModSounds.TWINE_ENTER.get(), 1.0F, 1.0F);
                }
            }
            if (mc.player != null) {
                resetPlayerMouseBaseline(mc);
            }
        } else {
            // Only end session if this packet is for our active twined cat or a global stop (-1)
            if (activeTwinedCatId != -1 && (catEntityId == -1 || catEntityId == activeTwinedCatId)) {
                if (mc.level != null) {
                    Entity catEntity = mc.level.getEntity(activeTwinedCatId);
                    if (catEntity instanceof TalentedCat cat) {
                        cat.setMaxUpStep(0.6F);
                        cat.setTwineInput(0.0F, 0.0F);
                        cat.playSound(com.kittytalents.registry.ModSounds.TWINE_EXIT.get(), 0.9F, 1.0F);
                    }
                }
                endClientSession(mc);
            }
        }
    }

    private static void bindCamera(Minecraft mc, TalentedCat cat) {
        if (!twineAnglesInitialized) {
            twineYaw = cat.getYRot();
            twinePitch = Mth.clamp(cat.getXRot(), -89.0F, 89.0F);
            syncCatRotation(cat, twineYaw, twinePitch);
            twineAnglesInitialized = true;
        }
        if (!cameraBound || mc.getCameraEntity() != cat) {
            mc.setCameraEntity(cat);
            cameraBound = true;
        }
    }

    private static void syncCatRotation(TalentedCat cat, float yaw, float pitch) {
        cat.setYRot(yaw);
        cat.yRotO = yaw;
        cat.setXRot(pitch);
        cat.xRotO = pitch;
        cat.setYHeadRot(yaw);
        cat.yHeadRotO = yaw;
        cat.setYBodyRot(yaw);
        cat.yBodyRotO = yaw;
    }

    private static void resetPlayerMouseBaseline(Minecraft mc) {
        if (mc.player != null) {
            mc.player.setYRot(0.0F);
            mc.player.yRotO = 0.0F;
            mc.player.setXRot(0.0F);
            mc.player.xRotO = 0.0F;
            mc.player.yHeadRot = dormantPlayerHeadYaw;
            mc.player.yHeadRotO = dormantPlayerHeadYaw;
            mc.player.yBodyRot = dormantPlayerBodyYaw;
            mc.player.yBodyRotO = dormantPlayerBodyYaw;
        }
    }

    private static void endClientSession(Minecraft mc) {
        activeTwinedCatId = -1;
        cameraBound = false;
        twineAnglesInitialized = false;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.setYRot(dormantPlayerYaw);
            mc.player.yRotO = dormantPlayerYaw;
            mc.player.setXRot(dormantPlayerPitch);
            mc.player.xRotO = dormantPlayerPitch;
            mc.player.yHeadRot = dormantPlayerHeadYaw;
            mc.player.yHeadRotO = dormantPlayerHeadYaw;
            mc.player.yBodyRot = dormantPlayerBodyYaw;
            mc.player.yBodyRotO = dormantPlayerBodyYaw;
        }
    }

    public static void requestUntwine() {
        if (activeTwinedCatId != -1) {
            PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.TWINE_STOP, 0.0F));
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (isTwined()) {
            Input input = event.getInput();
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (activeTwinedCatId != -1) {
                endClientSession(mc);
            }
            return;
        }

        if (!isTwined() && KEY_TWINE != null && KEY_TWINE.consumeClick()) {
            if (mc.crosshairPickEntity instanceof TalentedCat targetCat) {
                PacketHandler.sendToServer(new PacketTwineAction(targetCat.getId(), PacketTwineAction.ActionType.TWINE_START, 0.0F));
            } else {
                mc.player.displayClientMessage(Component.literal("Look directly at an owned or trusted Talented Cat to Twine!").withStyle(ChatFormatting.YELLOW), true);
            }
        }

        if (activeTwinedCatId != -1) {
            Entity entity = mc.level.getEntity(activeTwinedCatId);
            if (!(entity instanceof TalentedCat cat) || cat.isRemoved() || cat.isIncapacitated()) {
                PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.TWINE_STOP, 0.0F));
                endClientSession(mc);
                return;
            }

            bindCamera(mc, cat);
            syncCatRotation(cat, twineYaw, twinePitch);

            if ((KEY_TWINE != null && KEY_TWINE.consumeClick()) ||
                    (KEY_UNTWINE != null && KEY_UNTWINE.consumeClick())) {
                requestUntwine();
                return;
            }

            if (dodgeCooldownClient > 0) dodgeCooldownClient--;
            if (synergyCooldownClient > 0) synergyCooldownClient--;
            if (rmbPounceCooldownClient > 0) rmbPounceCooldownClient--;

            float forward = 0.0F;
            float strafe = 0.0F;
            if (mc.options.keyUp.isDown()) forward += 1.0F;
            if (mc.options.keyDown.isDown()) forward -= 1.0F;
            if (mc.options.keyLeft.isDown()) strafe += 1.0F;
            if (mc.options.keyRight.isDown()) strafe -= 1.0F;

            boolean jump = mc.options.keyJump.isDown();
            boolean attack = mc.options.keyAttack.isDown();
            boolean pounce = mc.options.keyUse.isDown();
            boolean ability1 = (KEY_ABILITY_1 != null && KEY_ABILITY_1.consumeClick());
            boolean ability2 = (KEY_ABILITY_2 != null && KEY_ABILITY_2.consumeClick());
            boolean ability3 = (KEY_ABILITY_3 != null && KEY_ABILITY_3.consumeClick());
            boolean ultimate = (KEY_ULTIMATE != null && KEY_ULTIMATE.consumeClick());

            if (ability1) {
                triggerClientAbilityCast(cat, cat.getAbilitySlot1(), PacketTeamCommand.CommandType.CAST_ABILITY_1);
            }
            if (ability2) {
                triggerClientAbilityCast(cat, cat.getAbilitySlot2(), PacketTeamCommand.CommandType.CAST_ABILITY_2);
            }
            if (ability3) {
                triggerClientAbilityCast(cat, cat.getAbilitySlot3(), PacketTeamCommand.CommandType.CAST_ABILITY_3);
            }
            if (ultimate) {
                if (cat.hasEquippedGun() && !mc.options.keyShift.isDown()) {
                    net.minecraft.world.item.ItemStack gun = cat.getEquippedWeapon();
                    int curAmmo = TacZCompat.getAmmoCount(gun);
                    int maxAmmo = TacZCompat.getMaxAmmoCount(gun);
                    if (curAmmo < maxAmmo) {
                        PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.RELOAD_GUN, 0.0F));
                    } else {
                        triggerClientAbilityCast(cat, cat.getAbilityUltimate(), PacketTeamCommand.CommandType.CAST_ULTIMATE);
                    }
                } else {
                    triggerClientAbilityCast(cat, cat.getAbilityUltimate(), PacketTeamCommand.CommandType.CAST_ULTIMATE);
                }
            }

            boolean sneak = mc.options.keyShift.isDown();
            cat.setShiftKeyDown(sneak);

            boolean sprint = mc.options.keySprint.isDown();
            cat.setSprinting(sprint);

            cat.setTwineInput(strafe, forward);
            cat.xxa = strafe;
            cat.zza = forward;

            // Local client jump/flight/wall-leap prediction (matches PacketTwineInput on server)
            if (jump) {
                if (cat.getTalentLevel(Talents.SKY_FELINE) > 0) {
                    Vec3 motion = cat.getDeltaMovement();
                    cat.setDeltaMovement(motion.x, Math.min(0.65D, motion.y + 0.18D), motion.z);
                    cat.hasImpulse = true;
                    cat.resetFallDistance();
                } else if (cat.getTalentLevel(Talents.FELINE_AGILITY) >= 3 && !cat.onGround() && cat.horizontalCollision) {
                    Vec3 motion = cat.getDeltaMovement();
                    cat.setDeltaMovement(-motion.x * 0.5D, 0.45D, -motion.z * 0.5D);
                    cat.hasImpulse = true;
                } else if (cat.onGround()) {
                    cat.doJump();
                }
            }

            // Local client right-click pounce prediction when no gun is equipped
            if (pounce && !cat.hasEquippedGun() && cat.onGround() && rmbPounceCooldownClient <= 0) {
                rmbPounceCooldownClient = 20;
                Vec3 look = cat.getLookAngle();
                double pounceSpeed = 1.25D + cat.getTalentLevel(Talents.POUNCE) * 0.10D;
                cat.setDeltaMovement(look.x * pounceSpeed, 0.48D, look.z * pounceSpeed);
                cat.hasImpulse = true;
                cat.playSound(SoundEvents.FOX_BITE, 1.0F, 1.2F);
            }

            // R1: 4-Way Dodge (local + server impulse)
            if (KEY_DODGE != null && KEY_DODGE.consumeClick() && dodgeCooldownClient <= 0) {
                if (cat.getEnergy() >= 15) {
                    dodgeCooldownClient = 15;
                    int dodgeDir;
                    if (forward < 0) dodgeDir = 1;
                    else if (strafe > 0) dodgeDir = 2;
                    else if (strafe < 0) dodgeDir = 3;
                    else if (forward > 0) dodgeDir = 0;
                    else dodgeDir = 1;

                    float yaw = Mth.wrapDegrees(twineYaw);
                    float rad = (float) Math.toRadians(yaw);
                    Vec3 forwardVec = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
                    Vec3 strafeVec = new Vec3(Math.cos(rad), 0, Math.sin(rad));
                    Vec3 dirVec = switch (dodgeDir) {
                        case 0 -> forwardVec;
                        case 1 -> forwardVec.scale(-1.0D);
                        case 2 -> strafeVec;
                        case 3 -> strafeVec.scale(-1.0D);
                        default -> forwardVec.scale(-1.0D);
                    };
                    double dodgeSpeed = 1.15D + (cat.getTalentLevel(Talents.FELINE_AGILITY) * 0.05D);
                    cat.setDeltaMovement(dirVec.x * dodgeSpeed, 0.22D, dirVec.z * dodgeSpeed);
                    cat.hasImpulse = true;

                    PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.DODGE, dodgeDir));
                    cat.playSound(com.kittytalents.registry.ModSounds.CAT_DODGE.get(), 0.9F, 1.4F);
                } else {
                    mc.player.displayClientMessage(Component.literal("Not enough Energy to dodge! (Need 15⚡)").withStyle(ChatFormatting.RED), true);
                }
            }

            // R10: Team Synergy Trigger
            if (KEY_SYNERGY != null && KEY_SYNERGY.consumeClick() && synergyCooldownClient <= 0) {
                synergyCooldownClient = 30;
                PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.TEAM_SYNERGY, 0.0F));
            }

            // Passive Talents Client Buffs
            if (cat.getTalentLevel(Talents.NIGHT_PROWLER) > 0) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));
            }
            if (cat.getTalentLevel(Talents.AQUA_KITTY) > 0 && cat.isInWater()) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false, false));
                mc.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false, false));
            }
            if (cat.getTalentLevel(Talents.PILLOW_CAT) > 0 || cat.getTalentLevel(Talents.CUSHION_PAWS) > 0) {
                cat.resetFallDistance();
                mc.player.resetFallDistance();
            }

            // Send wrapped yaw and clamped pitch to server
            PacketHandler.sendToServer(new PacketTwineInput(
                    activeTwinedCatId, strafe, forward, jump, sprint, sneak,
                    attack, pounce, ability1, ability2,
                    Mth.wrapDegrees(twineYaw), twinePitch
            ));

            // Keep dormant player motionless and reset mouse baseline to (0, 0)
            mc.player.setDeltaMovement(0.0D, mc.player.getDeltaMovement().y, 0.0D);
            mc.player.xxa = 0.0F;
            mc.player.zza = 0.0F;
            resetPlayerMouseBaseline(mc);
        }
    }

    private static void triggerClientAbilityCast(TalentedCat cat, String abilityId, PacketTeamCommand.CommandType cmdType) {
        PacketHandler.sendToServer(new PacketTeamCommand(cmdType));
        if (abilityId == null || abilityId.isEmpty()) return;
        CatAbility abil = CatAbilities.get(abilityId);
        if (abil == null) return;

        long now = cat.level().getGameTime();
        long last = cat.getAbilityCooldown(abil.getId());
        if (now >= last + abil.getCooldownTicks() && cat.getEnergy() >= abil.getEnergyCost()) {
            cat.setAbilityCooldown(abil.getId(), now);
            // Local pounce / dash velocity prediction so the client cat leaps immediately
            if ("pounce".equalsIgnoreCase(abil.getId()) && (cat.onGround() || cat.isInWater())) {
                int pounceTalent = Math.max(1, cat.getTalentLevel(Talents.POUNCE));
                Vec3 dir = cat.getLookAngle().scale(8.0D);
                double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                double horizSpeed = Math.min(2.0D, Math.max(0.8D, horizDist * 0.22D)) * (1.1D + pounceTalent * 0.06D);
                double vertSpeed = 0.52D + (pounceTalent * 0.04D);
                Vec3 horizNorm = new Vec3(dir.x, 0.0D, dir.z).normalize();
                cat.setDeltaMovement(horizNorm.x * horizSpeed, vertSpeed, horizNorm.z * horizSpeed);
                cat.hasImpulse = true;
            } else if ("feline_dash".equalsIgnoreCase(abil.getId()) || "volt_dash".equalsIgnoreCase(abil.getId())) {
                Vec3 look = cat.getLookAngle();
                cat.setDeltaMovement(look.x * 1.45D, 0.25D, look.z * 1.45D);
                cat.hasImpulse = true;
            }
        }
    }

    /**
     * While twined, the camera is the cat, so the crosshair can land on the player's own (invisible, dormant) body.
     * A vanilla attack on yourself gets you kicked ("attempted to attack an invalid entity"), and a vanilla attack on
     * anything else would be the far-away player body swinging its held item. The cat's own attack goes through
     * PacketTwineInput, so vanilla entity attacks are dropped entirely, and clicks on the body or the cat are ignored.
     */
    @SubscribeEvent
    public static void onInteractionKey(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (!isTwined()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        boolean self = target == mc.player || target.getId() == activeTwinedCatId;
        if (self || event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /**
     * Transfers raw mouse delta from the local player baseline (0, 0) to continuous
     * unwrapped twineYaw and clamped twinePitch, keeping cat.yRot and cat.yRotO
     * locked together so Camera.setup() never spins across -180/+180 or rubberbands.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isTwined() || mc.level == null || mc.player == null) return;

        Entity entity = mc.level.getEntity(activeTwinedCatId);
        if (!(entity instanceof TalentedCat cat)) return;

        bindCamera(mc, cat);

        float yawDelta = mc.player.getYRot();
        float pitchDelta = mc.player.getXRot();

        // Guard against any external packet setting player rotation to a non-delta angle
        if (Float.isFinite(yawDelta) && Float.isFinite(pitchDelta)
                && Math.abs(yawDelta) < 120.0F && Math.abs(pitchDelta) < 120.0F) {
            if (Math.abs(yawDelta) > 1.0E-5F || Math.abs(pitchDelta) > 1.0E-5F) {
                twineYaw += yawDelta;
                twinePitch = Mth.clamp(twinePitch + pitchDelta, -89.0F, 89.0F);
            }
        }

        syncCatRotation(cat, twineYaw, twinePitch);
        resetPlayerMouseBaseline(mc);

        event.setYaw(twineYaw);
        event.setPitch(twinePitch);
        event.setRoll(0.0F);
    }
}
