package com.kittytalents.client;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.entity.TwineManager;
import com.kittytalents.network.PacketHandler;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;


/**
 * Kitty Talents 3.0 ClientTwineHandler
 * Completely decouples mouse look and camera from the human player body,
 * eliminates camera snapping and micro-jitter, handles fluid traversal,
 * 4-way dodge, team synergy, and preserves a stable dormant player body.
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

    // Decoupled dormant player orientation
    private static float dormantPlayerYaw = 0.0F;
    private static float dormantPlayerPitch = 0.0F;
    private static float dormantPlayerHeadYaw = 0.0F;
    private static float dormantPlayerBodyYaw = 0.0F;

    // True once this session has handed the native camera to the possessed cat.
    // This is deliberately not policed every tick: another camera transition must
    // be allowed to happen naturally instead of being fought by this controller.
    private static boolean cameraBound;

    // Client action cooldowns
    private static int dodgeCooldownClient = 0;
    private static int synergyCooldownClient = 0;

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
    }

    public static boolean isTwined() {
        return activeTwinedCatId != -1;
    }

    public static void handleSyncTwine(int catEntityId, boolean twined) {
        Minecraft mc = Minecraft.getInstance();
        if (twined) {
            activeTwinedCatId = catEntityId;
            if (mc.level != null) {
                Entity catEntity = mc.level.getEntity(catEntityId);
                if (catEntity instanceof TalentedCat cat) {
                    bindCamera(mc, cat);
                    cat.setMaxUpStep(1.0F);
                    cat.playSound(com.kittytalents.registry.ModSounds.TWINE_ENTER.get(), 1.0F, 1.0F);
                    if (cat.getAbilitySlot1().isEmpty()) cat.setAbilitySlot1("pounce");
                    if (cat.getAbilitySlot2().isEmpty()) cat.setAbilitySlot2("feline_hiss");
                    if (cat.getAbilitySlot3().isEmpty()) cat.setAbilitySlot3("healing_purr");
                    if (cat.getAbilityUltimate().isEmpty()) cat.setAbilityUltimate("catnip_frenzy");
                }
            }
            if (mc.player != null) {
                dormantPlayerYaw = mc.player.getYRot();
                dormantPlayerPitch = mc.player.getXRot();
                dormantPlayerHeadYaw = mc.player.getYHeadRot();
                dormantPlayerBodyYaw = mc.player.yBodyRot;
            }
        } else {
            if (activeTwinedCatId != -1 && mc.level != null) {
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

    private static void bindCamera(Minecraft mc, TalentedCat cat) {
        if (!cameraBound) {
            mc.setCameraEntity(cat);
            cameraBound = true;
        }
    }

    private static void endClientSession(Minecraft mc) {
        activeTwinedCatId = -1;
        cameraBound = false;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
    }

    public static void requestUntwine() {
        if (activeTwinedCatId != -1) {
            PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.TWINE_STOP, 0.0F));
        }
    }

    /**
     * Suppress all movement impulse to the human player body so it does not walk, jump,
     * or wander into hazards while Twined.
     */
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

        // KeyMapping clicks are consumed on the client tick, not GLFW key events.
        // This keeps custom bindings reliable with rebinds, menus, and key repeat.
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

            // The entity may arrive one client tick after the session packet. Bind
            // once when it becomes available; never continuously steal the camera.
            bindCamera(mc, cat);

            // Either Twine (toggle) or Untwine ends the active session.
            if ((KEY_TWINE != null && KEY_TWINE.consumeClick()) ||
                    (KEY_UNTWINE != null && KEY_UNTWINE.consumeClick())) {
                requestUntwine();
                return;
            }

            // Decrement client cooldowns
            if (dodgeCooldownClient > 0) dodgeCooldownClient--;
            if (synergyCooldownClient > 0) synergyCooldownClient--;

            // Read directional controls from KeyMappings
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
                PacketHandler.sendToServer(new com.kittytalents.network.PacketTeamCommand(com.kittytalents.network.PacketTeamCommand.CommandType.CAST_ABILITY_1));
            }
            if (ability2) {
                PacketHandler.sendToServer(new com.kittytalents.network.PacketTeamCommand(com.kittytalents.network.PacketTeamCommand.CommandType.CAST_ABILITY_2));
            }
            if (ability3) {
                PacketHandler.sendToServer(new com.kittytalents.network.PacketTeamCommand(com.kittytalents.network.PacketTeamCommand.CommandType.CAST_ABILITY_3));
            }
            if (ultimate) {
                PacketHandler.sendToServer(new com.kittytalents.network.PacketTeamCommand(com.kittytalents.network.PacketTeamCommand.CommandType.CAST_ULTIMATE));
            }

            // These local values only provide normal local entity prediction. The
            // server receives and owns the same movement intent below.
            boolean sneak = mc.options.keyShift.isDown();
            cat.setShiftKeyDown(sneak);

            // Sprinting
            boolean sprint = mc.options.keySprint.isDown();
            cat.setSprinting(sprint);

            // Feed the real entity's normal movement path. Do not directly inject
            // velocity, jump, flight, climb, or wall-jump physics on the client;
            // doing that in parallel with the server was the source of rubberbanding.
            cat.setTwineInput(strafe, forward);
            cat.xxa = strafe;
            cat.zza = forward;

            // R1: 4-Way Dodge
            if (KEY_DODGE != null && KEY_DODGE.consumeClick() && dodgeCooldownClient <= 0) {
                dodgeCooldownClient = 15;
                int dodgeDir = 0;
                if (forward < 0) dodgeDir = 1;
                else if (strafe > 0) dodgeDir = 2;
                else if (strafe < 0) dodgeDir = 3;
                else if (forward > 0) dodgeDir = 0;
                else dodgeDir = 1;

                PacketHandler.sendToServer(new PacketTwineAction(activeTwinedCatId, PacketTwineAction.ActionType.DODGE, dodgeDir));
                cat.playSound(com.kittytalents.registry.ModSounds.CAT_DODGE.get(), 0.9F, 1.4F);
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

            // Send synchronized input packet to server
            PacketHandler.sendToServer(new PacketTwineInput(activeTwinedCatId, strafe, forward, jump, sprint, sneak, attack, pounce, ability1, ability2, cat.getYRot(), cat.getXRot()));

            // Safe Dormant Player Body: strictly motionless, grounded, and stable
            mc.player.setDeltaMovement(0.0D, mc.player.getDeltaMovement().y, 0.0D);
            mc.player.xxa = 0.0F;
            mc.player.zza = 0.0F;
            mc.player.setYRot(dormantPlayerYaw);
            mc.player.setXRot(dormantPlayerPitch);
            mc.player.yRotO = dormantPlayerYaw;
            mc.player.xRotO = dormantPlayerPitch;
            mc.player.yHeadRot = dormantPlayerHeadYaw;
            mc.player.yHeadRotO = dormantPlayerHeadYaw;
            mc.player.yBodyRot = dormantPlayerBodyYaw;
            mc.player.yBodyRotO = dormantPlayerBodyYaw;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!isTwined()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(activeTwinedCatId);
        if (!(entity instanceof TalentedCat cat)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        long gameTime = cat.level().getGameTime();

        // Compact, modern RPG HUD Console
        int hudW = 350;
        int hudH = 58;
        int hudX = (screenWidth - hudW) / 2;
        int hudY = 6;

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
        float engFrac = Math.max(0.0F, Math.min(1.0F, cat.getEnergy() / 100.0F));
        graphics.fill(engBarX, gaugeY + 1, engBarX + engBarW, gaugeY + 9, 0xFF1E293B);
        graphics.fill(engBarX, gaugeY + 1, engBarX + (int)(engBarW * engFrac), gaugeY + 9, 0xFF0284C7);
        String engText = cat.getEnergy() + "/100";
        graphics.drawCenteredString(font, engText, engBarX + engBarW / 2, gaugeY, 0xFFFFFFFF);

        // Ultimate Meter (62px)
        graphics.drawString(font, "ULT", hudX + 248, gaugeY, 0xFFA855F7, false);
        int ultBarX = hudX + 270;
        int ultBarW = 72;
        String ultName = !cat.getAbilityUltimate().isEmpty() ? cat.getAbilityUltimate() : "catnip_frenzy";
        long ultLast = cat.getPersistentData().getLong("cooldown_" + ultName.toLowerCase());
        long ultCd = getAbilityCooldownTicks(ultName, 300);
        float ultFrac = (gameTime >= ultLast + ultCd) ? 1.0F : Math.max(0.0F, (float)(gameTime - ultLast) / ultCd);
        graphics.fill(ultBarX, gaugeY + 1, ultBarX + ultBarW, gaugeY + 9, 0xFF1E293B);
        graphics.fill(ultBarX, gaugeY + 1, ultBarX + (int)(ultBarW * ultFrac), gaugeY + 9, 0xFF9333EA);
        String ultText = ultFrac >= 1.0F ? "READY!" : (int)(ultFrac * 100) + "%";
        graphics.drawCenteredString(font, ultText, ultBarX + ultBarW / 2, gaugeY, ultFrac >= 1.0F ? 0xFFFDE047 : 0xFFFFFFFF);

        // 3. Ability Hotbar Cards (Slots 1-3 + Ultimate)
        int cardY = hudY + 31;
        int cardW = 81;
        int cardH = 20;

        String a1 = !cat.getAbilitySlot1().isEmpty() ? cat.getAbilitySlot1() : "pounce";
        drawAbilityCard(graphics, font, hudX + 5, cardY, cardW, cardH, "V", a1, cat, gameTime);

        String a2 = !cat.getAbilitySlot2().isEmpty() ? cat.getAbilitySlot2() : "feline_hiss";
        drawAbilityCard(graphics, font, hudX + 5 + 86, cardY, cardW, cardH, "G", a2, cat, gameTime);

        String a3 = !cat.getAbilitySlot3().isEmpty() ? cat.getAbilitySlot3() : "healing_purr";
        drawAbilityCard(graphics, font, hudX + 5 + 172, cardY, cardW, cardH, "B", a3, cat, gameTime);

        drawAbilityCard(graphics, font, hudX + 5 + 258, cardY, cardW, cardH, "R", ultName, cat, gameTime);

        // 4. Compact Controls Footer
        String footer = "[L-Click] Attack/Combo  •  [R-Click] Pounce  •  [Alt] Dodge  •  [X] Synergy  •  [U] Untwine";
        graphics.drawCenteredString(font, footer, screenWidth / 2, hudY + hudH + 3, 0xCC94A3B8);
    }

    private static void drawAbilityCard(GuiGraphics graphics, Font font, int x, int y, int w, int h, String keyHint, String abilityId, TalentedCat cat, long gameTime) {
        graphics.fill(x, y, x + w, y + h, 0xCC1E293B);
        graphics.fill(x, y, x + w, y + 1, 0xFF475569);

        com.kittytalents.ability.CatAbility abil = com.kittytalents.ability.CatAbilities.get(abilityId);
        String name = (abil != null) ? abil.getDisplayName() : abilityId.replace('_', ' ');
        if (name.length() > 9) name = name.substring(0, 8) + ".";

        long lastCast = cat.getPersistentData().getLong("cooldown_" + abilityId.toLowerCase());
        long cd = (abil != null) ? abil.getCooldownTicks() : 40;
        long diff = (lastCast + cd) - gameTime;

        // Key hint on left
        graphics.drawString(font, "[" + keyHint + "]", x + 3, y + 2, 0xFFFCD34D, false);
        // Ability name
        graphics.drawString(font, name, x + 20, y + 2, 0xFFFFFFFF, false);

        // Status / Cooldown
        if (diff > 0) {
            String cdStr = String.format("%.1fs", diff / 20.0F);
            graphics.drawString(font, cdStr, x + 20, y + 11, 0xFFEF4444, false);
        } else {
            graphics.drawString(font, "READY", x + 20, y + 11, 0xFF22C55E, false);
        }

        // Energy Cost
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
        com.kittytalents.ability.CatAbility abil = com.kittytalents.ability.CatAbilities.get(abilityId);
        return (abil != null) ? abil.getCooldownTicks() : defaultTicks;
    }

    /**
     * Vanilla's MouseHandler turns the local player even while a different camera
     * entity is active. Transfer that already sensitivity/invert-Y processed delta
     * to the cat once at render time, then restore the dormant body. The camera
     * remains Minecraft's native cat camera; this event does not move it or alter
     * interpolation history.
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isTwined() || mc.level == null || mc.player == null) return;

        Entity entity = mc.level.getEntity(activeTwinedCatId);
        if (!(entity instanceof TalentedCat cat) || mc.getCameraEntity() != cat) return;

        float yawDelta = Mth.wrapDegrees(mc.player.getYRot() - dormantPlayerYaw);
        float pitchDelta = mc.player.getXRot() - dormantPlayerPitch;
        if (Math.abs(yawDelta) > 1.0E-4F || Math.abs(pitchDelta) > 1.0E-4F) {
            float yaw = Mth.wrapDegrees(cat.getYRot() + yawDelta);
            float pitch = Mth.clamp(cat.getXRot() + pitchDelta, -90.0F, 90.0F);
            cat.setYRot(yaw);
            cat.setXRot(pitch);
            cat.setYHeadRot(yaw);
            cat.setYBodyRot(yaw);
        }

        mc.player.setYRot(dormantPlayerYaw);
        mc.player.setXRot(dormantPlayerPitch);
        mc.player.yHeadRot = dormantPlayerHeadYaw;
        mc.player.yBodyRot = dormantPlayerBodyYaw;

        event.setYaw(cat.getYRot());
        event.setPitch(cat.getXRot());
    }

}
