package com.kittytalents.client.twine;

import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.compat.tacz.TacZCompat;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.network.PacketHandler;
import com.kittytalents.network.PacketTeamCommand;
import com.kittytalents.network.PacketTwineAction;
import com.kittytalents.network.PacketTwineInput;
import com.kittytalents.registry.ModSounds;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Twine input: key mappings, and each client tick while twined, reading movement/combat keys, predicting the cat's
 * jump / pounce / dodge locally, and sending {@link PacketTwineInput} to the server (which is authoritative).
 * The player's own movement is suppressed and vanilla entity attacks are cancelled while twined.
 */
@Mod.EventBusSubscriber(modid = "kittytalents", value = Dist.CLIENT)
public final class TwineInput {
    public static KeyMapping KEY_TWINE;
    public static KeyMapping KEY_UNTWINE;
    public static KeyMapping KEY_DODGE;
    public static KeyMapping KEY_SYNERGY;
    public static KeyMapping KEY_ABILITY_1;
    public static KeyMapping KEY_ABILITY_2;
    public static KeyMapping KEY_ABILITY_3;
    public static KeyMapping KEY_ULTIMATE;

    // Client-side repeat guards (the server has its own cooldowns)
    private static int dodgeCooldown = 0;
    private static int synergyCooldown = 0;
    private static int pounceCooldown = 0;

    private TwineInput() {}

    static void reset() {
        dodgeCooldown = 0;
        synergyCooldown = 0;
        pounceCooldown = 0;
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        KEY_TWINE = key("twine", GLFW.GLFW_KEY_Z);
        KEY_UNTWINE = key("untwine", GLFW.GLFW_KEY_U);
        KEY_DODGE = key("dodge", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_SYNERGY = key("synergy", GLFW.GLFW_KEY_X);
        KEY_ABILITY_1 = key("ability_1", GLFW.GLFW_KEY_V);
        KEY_ABILITY_2 = key("ability_2", GLFW.GLFW_KEY_G);
        KEY_ABILITY_3 = key("ability_3", GLFW.GLFW_KEY_B);
        KEY_ULTIMATE = key("ultimate", GLFW.GLFW_KEY_R);
        for (KeyMapping k : new KeyMapping[]{KEY_TWINE, KEY_UNTWINE, KEY_DODGE, KEY_SYNERGY, KEY_ABILITY_1, KEY_ABILITY_2, KEY_ABILITY_3, KEY_ULTIMATE}) {
            event.register(k);
        }
        TwineCamera.registerFirstPersonModelGuard();
    }

    private static KeyMapping key(String name, int glfw) {
        return new KeyMapping("key.kittytalents." + name, InputConstants.Type.KEYSYM, glfw, "key.categories.kittytalents");
    }

    private static boolean pressed(KeyMapping key) {
        return key != null && key.consumeClick();
    }

    /** Leaving a server or world mid-session: drop the session so the next world starts clean. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTwineState.endLocally(Minecraft.getInstance(), false);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientTwineState.isTwined()) return;
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            ClientTwineState.endLocally(mc, false);
            return;
        }

        if (!ClientTwineState.isTwined()) {
            if (pressed(KEY_TWINE)) {
                if (mc.crosshairPickEntity instanceof TalentedCat targetCat) {
                    PacketHandler.sendToServer(new PacketTwineAction(targetCat.getId(), PacketTwineAction.ActionType.TWINE_START, 0.0F));
                } else {
                    mc.player.displayClientMessage(Component.literal("Look directly at an owned or trusted Talented Cat to Twine!").withStyle(ChatFormatting.YELLOW), true);
                }
            }
            return;
        }

        TalentedCat cat = ClientTwineState.validate(mc);
        if (cat == null) return;
        int catId = cat.getId();

        TwineCamera.bind(mc, cat);
        TwineCamera.syncCatRotation(cat, TwineCamera.yaw(), TwineCamera.pitch());

        if (pressed(KEY_TWINE) || pressed(KEY_UNTWINE)) {
            ClientTwineState.requestUntwine();
            return;
        }

        if (dodgeCooldown > 0) dodgeCooldown--;
        if (synergyCooldown > 0) synergyCooldown--;
        if (pounceCooldown > 0) pounceCooldown--;

        float forward = 0.0F;
        float strafe = 0.0F;
        if (mc.options.keyUp.isDown()) forward += 1.0F;
        if (mc.options.keyDown.isDown()) forward -= 1.0F;
        if (mc.options.keyLeft.isDown()) strafe += 1.0F;
        if (mc.options.keyRight.isDown()) strafe -= 1.0F;

        boolean jump = mc.options.keyJump.isDown();
        boolean attack = mc.options.keyAttack.isDown();
        boolean pounce = mc.options.keyUse.isDown();
        boolean ability1 = pressed(KEY_ABILITY_1);
        boolean ability2 = pressed(KEY_ABILITY_2);
        boolean ability3 = pressed(KEY_ABILITY_3);
        boolean ultimate = pressed(KEY_ULTIMATE);

        if (ability1) castPredicted(cat, cat.getAbilitySlot1(), PacketTeamCommand.CommandType.CAST_ABILITY_1);
        if (ability2) castPredicted(cat, cat.getAbilitySlot2(), PacketTeamCommand.CommandType.CAST_ABILITY_2);
        if (ability3) castPredicted(cat, cat.getAbilitySlot3(), PacketTeamCommand.CommandType.CAST_ABILITY_3);
        if (ultimate) {
            // With a gun, R reloads when the magazine isn't full (Shift+R always casts the ultimate).
            if (cat.hasEquippedGun() && !mc.options.keyShift.isDown()
                    && TacZCompat.getAmmoCount(cat.getEquippedWeapon()) < TacZCompat.getMaxAmmoCount(cat.getEquippedWeapon())) {
                PacketHandler.sendToServer(new PacketTwineAction(catId, PacketTwineAction.ActionType.RELOAD_GUN, 0.0F));
            } else {
                castPredicted(cat, cat.getAbilityUltimate(), PacketTeamCommand.CommandType.CAST_ULTIMATE);
            }
        }

        boolean sneak = mc.options.keyShift.isDown();
        boolean sprint = mc.options.keySprint.isDown();
        cat.setShiftKeyDown(sneak);
        cat.setSprinting(sprint);
        cat.setTwineInput(strafe, forward);
        cat.xxa = strafe;
        cat.zza = forward;

        predictJump(cat, jump);
        predictPounce(cat, pounce);
        handleDodge(mc, cat, forward, strafe);

        if (pressed(KEY_SYNERGY) && synergyCooldown <= 0) {
            synergyCooldown = 30;
            PacketHandler.sendToServer(new PacketTwineAction(catId, PacketTwineAction.ActionType.TEAM_SYNERGY, 0.0F));
        }

        applyPassiveViewBuffs(mc, cat);

        PacketHandler.sendToServer(new PacketTwineInput(catId, strafe, forward, jump, sprint, sneak,
                attack, pounce, ability1, ability2, Mth.wrapDegrees(TwineCamera.yaw()), TwineCamera.pitch()));

        // Keep the dormant player motionless and the mouse baseline at (0, 0).
        mc.player.setDeltaMovement(0.0D, mc.player.getDeltaMovement().y, 0.0D);
        mc.player.xxa = 0.0F;
        mc.player.zza = 0.0F;
        TwineCamera.resetPlayerMouseBaseline(mc);
    }

    /** Local jump / flight / wall-leap prediction (mirrors PacketTwineInput on the server). */
    private static void predictJump(TalentedCat cat, boolean jump) {
        cat.setTwineJump(jump); // Sky Feline climb/glide runs in TalentedCat#travel, same as on the server
        if (!jump || cat.getTalentLevel(Talents.SKY_FELINE) > 0) return;
        if (cat.getTalentLevel(Talents.FELINE_AGILITY) >= 3 && !cat.onGround() && cat.horizontalCollision) {
            Vec3 motion = cat.getDeltaMovement();
            cat.setDeltaMovement(-motion.x * 0.5D, 0.45D, -motion.z * 0.5D);
            cat.hasImpulse = true;
        } else if (cat.onGround()) {
            cat.doJump();
        }
    }

    /** Local right-click pounce prediction when no gun is equipped. */
    private static void predictPounce(TalentedCat cat, boolean pounce) {
        if (!pounce || cat.hasEquippedGun() || !cat.onGround() || pounceCooldown > 0) return;
        pounceCooldown = 20;
        Vec3 look = cat.getLookAngle();
        double speed = 1.25D + cat.getTalentLevel(Talents.POUNCE) * 0.10D;
        cat.setDeltaMovement(look.x * speed, 0.48D, look.z * speed);
        cat.hasImpulse = true;
        cat.playSound(SoundEvents.FOX_BITE, 1.0F, 1.2F);
    }

    /** R1: 4-way dodge, predicted locally and confirmed by the server. */
    private static void handleDodge(Minecraft mc, TalentedCat cat, float forward, float strafe) {
        if (!pressed(KEY_DODGE) || dodgeCooldown > 0) return;
        if (cat.getEnergy() < 15) {
            mc.player.displayClientMessage(Component.literal("Not enough Energy to dodge! (Need 15⚡)").withStyle(ChatFormatting.RED), true);
            return;
        }
        dodgeCooldown = 15;
        int dir;
        if (forward < 0) dir = 1;
        else if (strafe > 0) dir = 2;
        else if (strafe < 0) dir = 3;
        else if (forward > 0) dir = 0;
        else dir = 1;

        float rad = (float) Math.toRadians(Mth.wrapDegrees(TwineCamera.yaw()));
        Vec3 forwardVec = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 strafeVec = new Vec3(Math.cos(rad), 0, Math.sin(rad));
        Vec3 dirVec = switch (dir) {
            case 0 -> forwardVec;
            case 2 -> strafeVec;
            case 3 -> strafeVec.scale(-1.0D);
            default -> forwardVec.scale(-1.0D);
        };
        double speed = 1.15D + (cat.getTalentLevel(Talents.FELINE_AGILITY) * 0.05D);
        cat.setDeltaMovement(dirVec.x * speed, 0.22D, dirVec.z * speed);
        cat.hasImpulse = true;
        PacketHandler.sendToServer(new PacketTwineAction(cat.getId(), PacketTwineAction.ActionType.DODGE, dir));
        cat.playSound(ModSounds.CAT_DODGE.get(), 0.9F, 1.4F);
    }

    /** Passive talents that change what the player sees while looking through the cat. */
    private static void applyPassiveViewBuffs(Minecraft mc, TalentedCat cat) {
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
    }

    /** Sends the cast and predicts the cooldown plus Pounce / Dash movement so the cat reacts immediately. */
    private static void castPredicted(TalentedCat cat, String abilityId, PacketTeamCommand.CommandType cmdType) {
        PacketHandler.sendToServer(new PacketTeamCommand(cmdType));
        if (abilityId == null || abilityId.isEmpty()) return;
        CatAbility abil = CatAbilities.get(abilityId);
        if (abil == null) return;

        long now = cat.level().getGameTime();
        long last = cat.getAbilityCooldown(abil.getId());
        if (now < last + abil.getCooldownTicks() || cat.getEnergy() < abil.getEnergyCost()) return;
        cat.setAbilityCooldown(abil.getId(), now);
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

    /**
     * While twined, the camera is the cat, so the crosshair can land on the player's own (invisible, dormant) body.
     * A vanilla attack on yourself gets you kicked ("attempted to attack an invalid entity"), and a vanilla attack on
     * anything else would be the far-away player body swinging its held item. The cat's own attack goes through
     * PacketTwineInput, so vanilla entity attacks are dropped entirely, and clicks on the body or the cat are ignored.
     */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!ClientTwineState.isTwined()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        boolean self = target == mc.player || ClientTwineState.isTwinedWith(target.getId());
        if (self || event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
