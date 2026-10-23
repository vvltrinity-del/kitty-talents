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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
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

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!isTwined()) return;
        // Render only once per frame on the hotbar/all post pass to avoid duplicate drawing
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(activeTwinedCatId);
        if (!(entity instanceof TalentedCat cat)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        long gameTime = cat.level().getGameTime();

        // Render tactical crosshair when a TacZ firearm is equipped in first-person
        if (cat.hasEquippedGun() && mc.options.getCameraType().isFirstPerson()) {
            renderTacticalGunCrosshair(graphics, screenWidth / 2, screenHeight / 2, cat);
        }

        // Position HUD at top-left (x = 8, y = 8) so it never collides with Jade (top-center) or Xaero's Minimap (top-right)
        int hudW = 350;
        int hudH = 58;
        int hudX = 8;
        int hudY = 8;

        // Translucent glassmorphic backplate
        graphics.fill(hudX - 1, hudY - 1, hudX + hudW + 1, hudY + hudH + 1, 0x55000000);
        graphics.fill(hudX, hudY, hudX + hudW, hudY + hudH, 0x990F172A);
        // Accent indigo top line
        graphics.fill(hudX, hudY, hudX + hudW, hudY + 2, 0xFF6366F1);

        // 1. Header Row (Name, Level, Role Tag, Primary Element, Status Badges)
        int headerY = hudY + 5;
        String catName = cat.getName().getString();
        int tierColor = cat.getProductionTier().getColorValue() | 0xFF000000;
        graphics.drawString(font, catName, hudX + 6, headerY, tierColor, false);
        int nameW = font.width(catName);

        String levelText = "Lv." + cat.getCatLevel();
        graphics.drawString(font, levelText, hudX + 10 + nameW, headerY, 0xFF94A3B8, false);
        int lvlW = font.width(levelText);

        // Role Tag
        com.kittytalents.combat.CatRole role = cat.getCatRole();
        String roleStr = "[" + role.getDisplayName().toUpperCase() + "]";
        int roleColor = getRoleBadgeColor(role);
        graphics.drawString(font, roleStr, hudX + 16 + nameW + lvlW, headerY, roleColor, false);
        int roleW = font.width(roleStr);

        // Primary Element Badge
        String elemStr = "<" + cat.getPrimaryElement().name() + ">";
        graphics.drawString(font, elemStr, hudX + 20 + nameW + lvlW + roleW, headerY, 0xFF38BDF8, false);

        // Active Status Effect Badges (Right side of header)
        renderActiveStatusBadges(graphics, font, cat, hudX + hudW - 6, headerY);

        // 2. Gauges Row: HP Bar (left), Energy Bar (mid), Ultimate Meter (right)
        int gaugeY = hudY + 18;

        // Health Bar (95px)
        graphics.drawString(font, "HP", hudX + 6, gaugeY, 0xFFEF4444, false);
        int hpBarX = hudX + 22;
        int hpBarW = 95;
        float hpFrac = Math.max(0.0F, Math.min(1.0F, cat.getHealth() / cat.getMaxHealth()));
        int hpColor = hpFrac > 0.5F ? 0xFF22C55E : (hpFrac > 0.25F ? 0xFFF59E0B : 0xFFEF4444);
        graphics.fill(hpBarX, gaugeY + 1, hpBarX + hpBarW, gaugeY + 9, 0xFF1E293B);
        graphics.fill(hpBarX, gaugeY + 1, hpBarX + (int)(hpBarW * hpFrac), gaugeY + 9, hpColor);
        String hpText = String.format("%.0f/%.0f", cat.getHealth(), cat.getMaxHealth());
        graphics.drawCenteredString(font, hpText, hpBarX + hpBarW / 2, gaugeY, 0xFFFFFFFF);

        // Energy Bar (95px)
        graphics.drawString(font, "ENG", hudX + 124, gaugeY, 0xFFFBBF24, false);
        int engBarX = hudX + 146;
        int engBarW = 95;
        int maxEnergy = Math.max(100, cat.getMaxEnergy());
        float engFrac = Math.max(0.0F, Math.min(1.0F, (float) cat.getEnergy() / maxEnergy));
        graphics.fill(engBarX, gaugeY + 1, engBarX + engBarW, gaugeY + 9, 0xFF1E293B);
        graphics.fill(engBarX, gaugeY + 1, engBarX + (int)(engBarW * engFrac), gaugeY + 9, 0xFF0284C7);
        String engText = cat.getEnergy() + "/" + maxEnergy;
        graphics.drawCenteredString(font, engText, engBarX + engBarW / 2, gaugeY, 0xFFFFFFFF);

        // Ultimate Meter (72px)
        graphics.drawString(font, "ULT", hudX + 248, gaugeY, 0xFFA855F7, false);
        int ultBarX = hudX + 270;
        int ultBarW = 72;
        String ultName = cat.getAbilityUltimate();
        long ultLast = cat.getAbilityCooldown(ultName);
        long ultCd = getAbilityCooldownTicks(ultName, 450);
        float ultFrac = (ultLast <= 0 || gameTime >= ultLast + ultCd) ? 1.0F : Math.max(0.0F, (float)(gameTime - ultLast) / ultCd);
        graphics.fill(ultBarX, gaugeY + 1, ultBarX + ultBarW, gaugeY + 9, 0xFF1E293B);
        graphics.fill(ultBarX, gaugeY + 1, ultBarX + (int)(ultBarW * ultFrac), gaugeY + 9, 0xFF9333EA);
        String ultText = ultFrac >= 1.0F ? "READY!" : (int)(ultFrac * 100) + "%";
        graphics.drawCenteredString(font, ultText, ultBarX + ultBarW / 2, gaugeY, ultFrac >= 1.0F ? 0xFFFDE047 : 0xFFFFFFFF);

        // 3. Ability Hotbar Cards (Slots 1-3 + Ultimate)
        int cardY = hudY + 31;
        int cardW = 81;
        int cardH = 20;

        drawAbilityCard(graphics, font, hudX + 5, cardY, cardW, cardH, "V", cat.getAbilitySlot1(), cat, gameTime);
        drawAbilityCard(graphics, font, hudX + 5 + 86, cardY, cardW, cardH, "G", cat.getAbilitySlot2(), cat, gameTime);
        drawAbilityCard(graphics, font, hudX + 5 + 172, cardY, cardW, cardH, "B", cat.getAbilitySlot3(), cat, gameTime);

        String ultKeyHint = cat.hasEquippedGun() ? "Sh+R" : "R";
        drawAbilityCard(graphics, font, hudX + 5 + 258, cardY, cardW, cardH, ultKeyHint, ultName, cat, gameTime);

        // 4. Compact Controls Footer & Gun Ammo HUD Pill
        if (cat.hasEquippedGun()) {
            net.minecraft.world.item.ItemStack gun = cat.getEquippedWeapon();
            int curAmmo = TacZCompat.getAmmoCount(gun);
            int maxAmmo = TacZCompat.getMaxAmmoCount(gun);
            boolean reloading = TacZCompat.isReloading(cat);
            String gunStatus = reloading
                    ? "🔫 " + gun.getHoverName().getString() + " [RELOADING...]"
                    : "🔫 " + gun.getHoverName().getString() + " [" + curAmmo + "/" + maxAmmo + "] • [LMB] Fire • [RMB] Aim • [R] Reload • [Shift+R] Ult";
            graphics.drawString(font, gunStatus, hudX + 4, hudY + hudH + 3, reloading ? 0xFFF59E0B : 0xFF38BDF8, true);
        } else {
            String footer = "[LMB] Attack  •  [RMB] Pounce  •  [Alt] Dodge  •  [X] Synergy  •  [U] Untwine";
            graphics.drawString(font, footer, hudX + 4, hudY + hudH + 3, 0xCC94A3B8, true);
        }
    }

    private static void renderTacticalGunCrosshair(GuiGraphics graphics, int cx, int cy, TalentedCat cat) {
        boolean aiming = Minecraft.getInstance().options.keyUse.isDown();
        boolean reloading = TacZCompat.isReloading(cat);
        int gap = aiming ? 3 : 6;
        int len = aiming ? 4 : 5;
        int color = reloading ? 0xCCF59E0B : (aiming ? 0xEE22C55E : 0xDD38BDF8);

        // 4-way tactical reticle brackets
        graphics.fill(cx - gap - len, cy, cx - gap, cy + 1, color);
        graphics.fill(cx + gap + 1, cy, cx + gap + 1 + len, cy + 1, color);
        graphics.fill(cx, cy - gap - len, cx + 1, cy - gap, color);
        graphics.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + len, color);
    }

    private static void drawAbilityCard(GuiGraphics graphics, Font font, int x, int y, int w, int h, String keyHint, String abilityId, TalentedCat cat, long gameTime) {
        graphics.fill(x, y, x + w, y + h, 0xCC1E293B);
        graphics.fill(x, y, x + w, y + 1, 0xFF475569);

        CatAbility abil = CatAbilities.get(abilityId);
        String name = (abil != null) ? abil.getDisplayName() : abilityId.replace('_', ' ');
        int keyW = font.width("[" + keyHint + "]");
        int maxNameChars = keyHint.length() > 1 ? 6 : 9;
        if (name.length() > maxNameChars) name = name.substring(0, maxNameChars - 1) + ".";

        long lastCast = cat.getAbilityCooldown(abilityId);
        long cd = (abil != null) ? abil.getCooldownTicks() : 40;
        long diff = (lastCast <= 0) ? 0 : (lastCast + cd) - gameTime;

        graphics.drawString(font, "[" + keyHint + "]", x + 3, y + 2, 0xFFFCD34D, false);
        graphics.drawString(font, name, x + 5 + keyW, y + 2, 0xFFFFFFFF, false);

        if (diff > 0) {
            String cdStr = String.format("%.1fs", diff / 20.0F);
            graphics.drawString(font, cdStr, x + 4, y + 11, 0xFFEF4444, false);
        } else {
            graphics.drawString(font, "READY", x + 4, y + 11, 0xFF22C55E, false);
        }

        int cost = (abil != null) ? abil.getEnergyCost() : 0;
        if (cost > 0) {
            String costStr = cost + "⚡";
            graphics.drawString(font, costStr, x + w - font.width(costStr) - 3, y + 11, 0xFF38BDF8, false);
        }
    }

    private static void renderActiveStatusBadges(GuiGraphics graphics, Font font, TalentedCat cat, int rightX, int y) {
        net.minecraft.nbt.CompoundTag pData = cat.getPersistentData();
        if (!pData.contains("FelineStatuses")) {
            graphics.drawString(font, "[U to Untwine]", rightX - font.width("[U to Untwine]"), y, 0xFF64748B, false);
            return;
        }

        net.minecraft.nbt.CompoundTag statuses = pData.getCompound("FelineStatuses");
        int curX = rightX;
        for (String key : statuses.getAllKeys()) {
            net.minecraft.nbt.CompoundTag eff = statuses.getCompound(key);
            int ticks = eff.getInt("Ticks");
            if (ticks <= 0) continue;

            int sec = (ticks + 19) / 20;
            String badge = getStatusEmoji(key) + " " + sec + "s";
            int badgeW = font.width(badge) + 6;
            curX -= badgeW + 3;

            int badgeColor = getStatusColor(key);
            graphics.fill(curX, y - 1, curX + badgeW, y + 9, badgeColor | 0xAA000000);
            graphics.drawString(font, badge, curX + 3, y, 0xFFFFFFFF, false);
        }

        if (curX == rightX) {
            graphics.drawString(font, "[U to Untwine]", rightX - font.width("[U to Untwine]"), y, 0xFF64748B, false);
        }
    }

    private static String getStatusEmoji(String key) {
        return switch (key.toUpperCase()) {
            case "BURN" -> "🔥";
            case "FREEZE" -> "❄";
            case "SHOCK" -> "⚡";
            case "POISON" -> "☠";
            case "BLEED" -> "🩸";
            case "MARKED" -> "🎯";
            case "HASTE" -> "👟";
            case "FORTIFIED" -> "🛡";
            case "REGENERATION" -> "💖";
            default -> "✦";
        };
    }

    private static int getStatusColor(String key) {
        return switch (key.toUpperCase()) {
            case "BURN" -> 0xDC2626;
            case "FREEZE" -> 0x0284C7;
            case "SHOCK" -> 0xD97706;
            case "POISON" -> 0x16A34A;
            case "BLEED" -> 0x991B1B;
            case "MARKED" -> 0x7C3AED;
            case "HASTE" -> 0x0D9488;
            case "FORTIFIED" -> 0x2563EB;
            case "REGENERATION" -> 0xDB2777;
            default -> 0x475569;
        };
    }

    private static int getRoleBadgeColor(com.kittytalents.combat.CatRole role) {
        if (role == null) return 0xFF94A3B8;
        return switch (role) {
            case DPS, ASSASSIN -> 0xFFEF4444;
            case TANK, BRUISER -> 0xFF3B82F6;
            case HEALER, SUPPORT -> 0xFF10B981;
            case MAGE -> 0xFFA855F7;
            case SCOUT, MOBILITY -> 0xFFF59E0B;
            case CONTROLLER, BREAKER -> 0xFFEC4899;
            case HYBRID -> 0xFF6366F1;
            default -> 0xFF94A3B8;
        };
    }

    private static long getAbilityCooldownTicks(String abilityId, long defaultTicks) {
        CatAbility abil = CatAbilities.get(abilityId);
        return (abil != null) ? abil.getCooldownTicks() : defaultTicks;
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
