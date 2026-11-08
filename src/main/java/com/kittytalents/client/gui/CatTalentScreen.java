package com.kittytalents.client.gui;

import com.kittytalents.client.ClientModEvents;
import com.kittytalents.entity.CatMode;
import com.kittytalents.entity.CatVariant;
import com.kittytalents.entity.TalentedCat;
import com.kittytalents.inventory.CatTalentMenu;
import com.kittytalents.network.*;
import com.kittytalents.talent.Talent;
import com.kittytalents.talent.Talents;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.kittytalents.combat.CatRole;
import com.kittytalents.combat.CatRoleEngine;
import com.kittytalents.combat.FelineElement;
import com.kittytalents.ability.CatAbilities;
import com.kittytalents.ability.CatAbility;
import com.kittytalents.entity.TwineManager;

import java.util.ArrayList;
import java.util.List;

public class CatTalentScreen extends AbstractContainerScreen<CatTalentMenu> {
    private static final String[] SKIN_NAMES = {
            "Tabby", "Tuxedo", "Red", "Siamese", "British Shorthair",
            "Calico", "Persian", "Ragdoll", "White", "Jellie",
            "All Black", "Tiger", "Snow Leopard", "Void Ender", "Golden Maneki", "Ghost Phantom", "Panther"
    };

    private static final String[] TAB_NAMES = {
            "Home", "Talents", "Gear", "Style", "Stats", "Combat", "Twine"
    };

    private static final String[] RANDOM_CAT_NAMES = {
            "Luna", "Milo", "Oliver", "Leo", "Bella", "Charlie", "Chloe", "Lucy", "Simba", "Lily",
            "Loki", "Shadow", "Cleo", "Jack", "Felix", "Nala", "Jasper", "Smokey", "Willow", "Boots"
    };

    private final TalentedCat cat;
    private int currentTab = 0; // 0 = Home, 1 = Talents, 2 = Gear, 3 = Style, 4 = Stats
    private int styleSubTab = 0; // 0 = Accessories, 1 = Skins, 2 = Variants

    // Talent Tab State
    private final List<Talent> allTalents = new ArrayList<>();
    private final List<Talent> filteredTalents = new ArrayList<>();
    private Talent selectedTalent = Talents.POUNCE;
    private int talentPage = 0;
    private static final int TALENTS_PER_PAGE = 7;
    private int talentCategory = 0; // 0 = All, 1 = Combat, 2 = Utility, 3 = Mobility, 4 = Support, 5 = Endgame

    // UI Components
    private EditBox nameEditBox;
    private EditBox trustEditBox;
    private float catModelRotation = 0.0F;

    public CatTalentScreen(CatTalentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.cat = menu.getCat();
        for (Talent t : Talents.getAll()) {
            if (!t.isStandard() && t.isAvailable()) {
                this.allTalents.add(t);
            }
        }
        filterTalents();
    }

    private void filterTalents() {
        filteredTalents.clear();
        for (Talent t : allTalents) {
            if (talentCategory == 0) {
                filteredTalents.add(t);
            } else if (talentCategory == 1 && isCombatTalent(t)) {
                filteredTalents.add(t);
            } else if (talentCategory == 2 && isUtilityTalent(t)) {
                filteredTalents.add(t);
            } else if (talentCategory == 3 && isMobilityTalent(t)) {
                filteredTalents.add(t);
            } else if (talentCategory == 4 && isSupportTalent(t)) {
                filteredTalents.add(t);
            } else if (talentCategory == 5 && t.isDireTalent()) {
                filteredTalents.add(t);
            }
        }
        if (talentPage * TALENTS_PER_PAGE >= filteredTalents.size()) {
            talentPage = 0;
        }
        if (!filteredTalents.isEmpty() && !filteredTalents.contains(selectedTalent)) {
            selectedTalent = filteredTalents.get(0);
        }
    }

    private boolean isCombatTalent(Talent t) {
        return t == Talents.POUNCE_STRIKE || t == Talents.FELINE_REFLEXES || t == Talents.CRITICAL_CLAWS ||
               t == Talents.FELINE_BERSERKER || t == Talents.FELINE_HISS || t == Talents.VENOM_CLAWS ||
               t == Talents.INFERNO_CAT || t == Talents.GUARDIAN_CAT || t == Talents.CATNIP_CATASTROPHE ||
               t == Talents.HUNTER_CAT || t == Talents.CAT_ARMORY || t == Talents.CAT_ARMOR ||
               t == Talents.MARKSMAN || t == Talents.QUICK_RELOAD || t == Talents.AMMO_SCAVENGER;
    }

    private boolean isMobilityTalent(Talent t) {
        return t == Talents.FELINE_AGILITY || t == Talents.CUSHION_PAWS || t == Talents.AQUA_CAT ||
               t == Talents.FIREPROOF_PAWS || t == Talents.SKY_FELINE || t == Talents.CAT_MOUNT;
    }

    private boolean isUtilityTalent(Talent t) {
        return t == Talents.KITTY_PACK || t == Talents.TORCH_KITTY || t == Talents.HOME_WHISKERS ||
               t == Talents.WHISKER_RADAR || t == Talents.SNIFFER_WHISKERS || t == Talents.KITTY_TOOLS ||
               t == Talents.MOLE_CAT || t == Talents.PAW_RETRIEVER || t == Talents.HERDING_KITTY ||
               t == Talents.GATE_PURR || t == Talents.HAPPY_EATER ||
               t == Talents.GREEN_PAW || t == Talents.TIMBER_CLAWS || t == Talents.LOGISTICS_WHISKERS || t == Talents.WORK_ETHIC;
    }

    private boolean isSupportTalent(Talent t) {
        return t == Talents.PURR_MEDIC || t == Talents.RESCUE_PURR || t == Talents.GALE_PURR ||
               t == Talents.CHARM_WHISKERS || t == Talents.SHADOW_PELT || t == Talents.ALCHEMY_KITTY ||
               t == Talents.FRENZY_FOCUS || t == Talents.LUCKY_PURR;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        this.leftPos = 0;
        this.topPos = 0;
        super.init();
        rebuildInterface();
    }

    public void refreshTrustUi() {
        if (currentTab == 0) rebuildInterface();
    }

    public void refreshEquipmentUi() {
        if (currentTab == 2) rebuildInterface();
    }

    public void refreshCombatUi() {
        if (currentTab == 5) rebuildInterface();
    }

    /** The Combat tab edits the local copy for instant feedback; the server validates, applies and syncs back. */
    private void sendLoadout() {
        PacketHandler.sendToServer(new com.kittytalents.network.PacketSetCatLoadout(cat.getId(),
                com.kittytalents.core.component.CatLoadout.of(cat)));
    }

    public void refreshTalentUi() {
        if (currentTab == 1) rebuildInterface();
    }

    private void rebuildInterface() {
        this.clearWidgets();
        this.nameEditBox = null;
        this.trustEditBox = null;
        if (cat == null) return;

        // Bottom Navigation Bar Tabs (7 Tabs)
        int tabBtnW = 46;
        int navWidth = TAB_NAMES.length * (tabBtnW + 4);
        int navX = (this.width - navWidth) / 2;
        int navY = this.height - 24;

        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int tIdx = i;
            boolean selected = (currentTab == tIdx);
            this.addRenderableWidget(Button.builder(
                    Component.literal(TAB_NAMES[i]).withStyle(selected ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
                    button -> {
                        currentTab = tIdx;
                        rebuildInterface();
                    }
            ).bounds(navX + i * (tabBtnW + 4), navY, tabBtnW, 18).build());
        }

        if (currentTab == 0) {
            initHomeTab();
        } else if (currentTab == 1) {
            initTalentsTab();
        } else if (currentTab == 2) {
            initGearTab();
        } else if (currentTab == 3) {
            initStyleTab();
        } else if (currentTab == 4) {
            initStatsTab();
        } else if (currentTab == 5) {
            initCombatTab();
        } else if (currentTab == 6) {
            initTwineTab();
        }
    }

    // ==========================================
    // TAB 0: HOME TAB (Compact Companion HUD)
    // ==========================================
    private void initHomeTab() {
        int centerX = (int) (this.width * 0.38F);
        int centerY = (int) (this.height * 0.52F);

        // Mode Selector [<] [ Mode Name ] [>] [≡]
        int modeW = 86;
        int totalModeW = 20 + 2 + modeW + 2 + 20 + 2 + 20; // 152
        int modeBoxX = centerX - (totalModeW / 2);
        int modeBoxY = centerY + 34;

        this.addRenderableWidget(Button.builder(Component.literal("◀"), button -> {
            cat.setCatMode(cat.getCatMode().previous());
            PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), cat.getCatMode().ordinal()));
            rebuildInterface();
        }).bounds(modeBoxX, modeBoxY, 20, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal(cat.getCatMode().name()), button -> {
            cat.setCatMode(cat.getCatMode().next());
            PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), cat.getCatMode().ordinal()));
            button.setMessage(Component.literal(cat.getCatMode().name()));
        }).bounds(modeBoxX + 22, modeBoxY, modeW, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("▶"), button -> {
            cat.setCatMode(cat.getCatMode().next());
            PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), cat.getCatMode().ordinal()));
            rebuildInterface();
        }).bounds(modeBoxX + 22 + modeW + 2, modeBoxY, 20, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("≡"), button -> {
            ClientModEvents.openWhistleScreen();
        }).bounds(modeBoxX + 22 + modeW + 26, modeBoxY, 20, 18).build());

        // Right side Action Buttons
        int actionX = (int) (this.width * 0.58F);
        int actionY = (int) (this.height * 0.60F);

        // Trust / Co-Owner input box
        this.trustEditBox = new EditBox(this.font, actionX, actionY - 48, 100, 16, Component.literal("Trust"));
        this.trustEditBox.setMaxLength(16);
        this.trustEditBox.setHint(Component.literal("Add Co-Owner...").withStyle(ChatFormatting.DARK_GRAY));
        this.addWidget(this.trustEditBox);

        this.addRenderableWidget(Button.builder(Component.literal("+ Trust"), button -> {
            if (trustEditBox != null) {
                String target = trustEditBox.getValue().trim();
                if (!target.isEmpty()) {
                    PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 0, target));
                    trustEditBox.setValue("");
                }
            }
        }).bounds(actionX + 104, actionY - 48, 54, 16).build());

        // Co-owner removal (owner only): one "x Name" button per trusted player, right of the stats panel.
        // Stopgap until the Phase 6 UI redesign.
        net.minecraft.client.player.LocalPlayer me = net.minecraft.client.Minecraft.getInstance().player;
        if (me != null && me.getUUID().equals(cat.getOwnerUUID())) {
            int listX = (int) (this.width * 0.58F) + 170;
            int listY = (int) (this.height * 0.35F) - 8;
            int shown = 0;
            for (java.util.Map.Entry<java.util.UUID, String> e : cat.getTrustedPlayers().entrySet()) {
                if (shown >= 6) break;
                java.util.UUID id = e.getKey();
                String name = e.getValue().isEmpty() ? "Player" : e.getValue();
                Button remove = Button.builder(Component.literal("✕ " + name).withStyle(ChatFormatting.RED),
                        b -> PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 1, id.toString())))
                        .bounds(listX, listY + shown * 18, 96, 16).build();
                remove.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Remove " + name + " as co-owner")));
                this.addRenderableWidget(remove);
                shown++;
            }
        }

        // Rename box
        String currentName = cat.hasCustomName() ? cat.getCustomName().getString() : cat.getName().getString();
        this.nameEditBox = new EditBox(this.font, actionX, actionY - 26, 100, 16, Component.literal("Name"));
        this.nameEditBox.setMaxLength(32);
        this.nameEditBox.setValue(currentName);
        this.addWidget(this.nameEditBox);

        this.addRenderableWidget(Button.builder(Component.literal("Rename"), button -> {
            if (nameEditBox != null) {
                String newName = nameEditBox.getValue().trim();
                if (!newName.isEmpty() && newName.length() <= 32) {
                    PacketHandler.sendToServer(new PacketRenameCat(cat.getId(), newName));
                }
            }
        }).bounds(actionX + 104, actionY - 26, 54, 16).build());

        // Sit/Stand button
        this.addRenderableWidget(Button.builder(
                Component.literal(cat.isOrderedToSit() ? "Stand Up" : "Sit Down"),
                button -> {
                    CatMode newMode = cat.isOrderedToSit() ? CatMode.FOLLOW : CatMode.DOCILE;
                    cat.setCatMode(newMode);
                    PacketHandler.sendToServer(new PacketChangeCatMode(cat.getId(), newMode.ordinal()));
                    rebuildInterface();
                }
        ).bounds(actionX, actionY, 78, 18).build());

        // Twine button
        this.addRenderableWidget(Button.builder(
                Component.literal("✦ TWINE ✦").withStyle(ChatFormatting.AQUA),
                button -> {
                    PacketHandler.sendToServer(new PacketTwineAction(cat.getId(), true));
                    this.onClose();
                }
        ).bounds(actionX + 82, actionY, 78, 18).build());

        // Open Pack button
        this.addRenderableWidget(Button.builder(
                Component.literal("Kitty Pack"),
                button -> PacketHandler.sendToServer(new PacketOpenCatPack(cat.getId()))
        ).bounds(actionX, actionY + 22, 78, 18).build());

        // Whistle button
        this.addRenderableWidget(Button.builder(
                Component.literal("Whistle"),
                button -> ClientModEvents.openWhistleScreen()
        ).bounds(actionX + 82, actionY + 22, 78, 18).build());

        // Job Assignment & Clock In/Out row
        com.kittytalents.work.CatJob curJob = cat.getCatJob();
        this.addRenderableWidget(Button.builder(
                Component.literal("Job: " + curJob.getDisplayName()).withStyle(ChatFormatting.GOLD),
                button -> {
                    com.kittytalents.work.CatJob[] jobs = com.kittytalents.work.CatJob.values();
                    com.kittytalents.work.CatJob next = jobs[(cat.getCatJob().ordinal() + 1) % jobs.length];
                    cat.setCatJob(next);
                    if (next != com.kittytalents.work.CatJob.NONE) {
                        cat.setCatMode(CatMode.WORK);
                        cat.setClockedIn(true);
                    }
                    PacketHandler.sendToServer(new PacketSetCatJob(cat.getId(), next, cat.isClockedIn()));
                    rebuildInterface();
                }
        ).bounds(actionX, actionY + 44, 102, 18).build());

        boolean clockedIn = cat.isClockedIn();
        this.addRenderableWidget(Button.builder(
                Component.literal(clockedIn ? "Clocked In" : "Clocked Out")
                        .withStyle(clockedIn ? ChatFormatting.GREEN : ChatFormatting.RED),
                button -> {
                    boolean nextClock = !cat.isClockedIn();
                    cat.setClockedIn(nextClock);
                    PacketHandler.sendToServer(new PacketSetCatJob(cat.getId(), cat.getCatJob(), nextClock));
                    rebuildInterface();
                }
        ).bounds(actionX + 106, actionY + 44, 54, 18).build());
    }

    // ==========================================
    // TAB 1: TALENTS TAB (2-Column Reference Layout)
    // ==========================================
    private void initTalentsTab() {
        int panelW = Math.min(540, this.width - 40);
        int panelH = Math.min(270, this.height - 50);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2 - 10;

        // Categories at top
        String[] cats = { "All", "Combat", "Utility", "Mobility", "Support", "Endgame" };
        int catX = startX + 10;
        int catBtnW = 44;
        for (int i = 0; i < cats.length; i++) {
            final int cIdx = i;
            boolean isSel = (talentCategory == cIdx);
            this.addRenderableWidget(Button.builder(
                    Component.literal(cats[i]).withStyle(isSel ? ChatFormatting.YELLOW : ChatFormatting.GRAY),
                    button -> {
                        talentCategory = cIdx;
                        filterTalents();
                        rebuildInterface();
                    }
            ).bounds(catX + i * (catBtnW + 2), startY + 6, catBtnW, 16).build());
        }

        // Respec button
        this.addRenderableWidget(Button.builder(
                Component.literal("Respec").withStyle(ChatFormatting.RED),
                button -> PacketHandler.sendToServer(new PacketRespecTalents(cat.getId()))
        ).bounds(startX + panelW - 54, startY + 6, 44, 16).build());

        // Left list: Talents (7 per page)
        int listX = startX + 10;
        int listY = startY + 28;
        int maxPages = Math.max(1, (int) Math.ceil((double) filteredTalents.size() / TALENTS_PER_PAGE));

        int pageStart = talentPage * TALENTS_PER_PAGE;
        int pageEnd = Math.min(filteredTalents.size(), pageStart + TALENTS_PER_PAGE);

        for (int i = pageStart; i < pageEnd; i++) {
            final Talent t = filteredTalents.get(i);
            boolean isCurrent = (t == selectedTalent);
            int rowY = listY + (i - pageStart) * 26;

            int currentLvl = cat.getTalentLevel(t);
            String label = t.getDisplayName() + " (" + currentLvl + "/" + t.getMaxLevel() + ")";

            this.addRenderableWidget(Button.builder(
                    Component.literal(label).withStyle(isCurrent ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE),
                    button -> {
                        selectedTalent = t;
                        rebuildInterface();
                    }
            ).bounds(listX, rowY, 140, 24).build());
        }

        // Page Controls
        this.addRenderableWidget(Button.builder(Component.literal("◀"), button -> {
            if (talentPage > 0) { talentPage--; rebuildInterface(); }
        }).bounds(listX + 20, startY + panelH - 26, 24, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("▶"), button -> {
            if (talentPage < maxPages - 1) { talentPage++; rebuildInterface(); }
        }).bounds(listX + 96, startY + panelH - 26, 24, 18).build());

        // Right Detail: Train Button
        if (selectedTalent != null) {
            int currentLvl = cat.getTalentLevel(selectedTalent);
            int cost = cat.getCostForNextRank(selectedTalent);
            boolean canTrain = currentLvl < selectedTalent.getMaxLevel() && cat.getSkillPoints() >= cost &&
                               (!selectedTalent.isDireTalent() || cat.isDireAwakened());

            this.addRenderableWidget(Button.builder(
                    Component.literal(currentLvl >= selectedTalent.getMaxLevel() ? "Max Level!" : "Train (-" + cost + " SP)"),
                    button -> {
                        PacketHandler.sendToServer(new PacketTrainTalent(cat.getId(), selectedTalent.getId()));
                        rebuildInterface();
                    }
            ).bounds(startX + panelW - 96, startY + panelH - 32, 86, 22).build());
        }
    }

    // ==========================================
    // TAB 2: GEAR TAB (10-Slot Equipment Layout)
    // ==========================================
    private void initGearTab() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 14;

        // Helmet (Top)
        addEquipSlotButton(centerX - 10, centerY - 64, 1, "Helmet");

        // Weapon (Left)
        addEquipSlotButton(centerX - 70, centerY - 20, 0, "Weapon");

        // Accessory (Right)
        addEquipSlotButton(centerX + 50, centerY - 20, 5, "Accessory");

        // Body Armor stack below cat
        addEquipSlotButton(centerX - 10, centerY + 24, 2, "Chest");
        addEquipSlotButton(centerX - 10, centerY + 46, 3, "Legs");
        addEquipSlotButton(centerX - 10, centerY + 68, 4, "Boots");

        // Tool slot
        addEquipSlotButton(centerX + 50, centerY + 68, 6, "Tool");

        // 3 Cosmetics Below
        int cosY = centerY + 94;
        addEquipSlotButton(centerX - 60, cosY, 7, "Cosmetic 1");
        addEquipSlotButton(centerX - 10, cosY, 8, "Cosmetic 2");
        addEquipSlotButton(centerX + 40, cosY, 9, "Cosmetic 3");

        // Visibility toggle
        this.addRenderableWidget(Button.builder(
                Component.literal("Cosmetics: " + (cat.isCosmeticsVisible() ? "Visible" : "Hidden")),
                button -> {
                    PacketHandler.sendToServer(new PacketEquipCatItem(cat.getId(), 10));
                    cat.setCosmeticsVisible(!cat.isCosmeticsVisible());
                    button.setMessage(Component.literal("Cosmetics: " + (cat.isCosmeticsVisible() ? "Visible" : "Hidden")));
                }
        ).bounds(centerX - 60, cosY + 24, 120, 16).build());
    }

    private void addEquipSlotButton(int x, int y, int slotId, String tooltipName) {
        this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            boolean isShift = hasShiftDown();
            PacketHandler.sendToServer(new PacketEquipCatItem(cat.getId(), slotId, isShift));
        }).bounds(x, y, 20, 20).build());
    }

    // ==========================================
    // TAB 3: STYLE TAB (Accessories, Skins, Variants)
    // ==========================================
    private void initStyleTab() {
        int panelW = Math.min(520, this.width - 40);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - 260) / 2 - 10;

        // Sub-tabs on left
        String[] subs = { "Accessories", "Skins", "Variants" };
        for (int i = 0; i < subs.length; i++) {
            final int sIdx = i;
            boolean isSel = (styleSubTab == sIdx);
            this.addRenderableWidget(Button.builder(
                    Component.literal(subs[i]).withStyle(isSel ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
                    button -> {
                        styleSubTab = sIdx;
                        rebuildInterface();
                    }
            ).bounds(startX + 10, startY + 20 + i * 26, 90, 22).build());
        }

        // Random name button
        this.addRenderableWidget(Button.builder(Component.literal("Random Name"), button -> {
            String rand = RANDOM_CAT_NAMES[(int) (Math.random() * RANDOM_CAT_NAMES.length)];
            cat.setCustomName(Component.literal(rand));
            PacketHandler.sendToServer(new PacketRenameCat(cat.getId(), rand));
            rebuildInterface();
        }).bounds(startX + 10, startY + 110, 90, 18).build());

        if (styleSubTab == 1) {
            // 17 Skins Grid
            int gridX = startX + 110;
            int gridY = startY + 120;
            for (int s = 0; s < SKIN_NAMES.length; s++) {
                final int sId = s;
                boolean isCurrent = (cat.getSkinId() == sId);
                this.addRenderableWidget(Button.builder(
                        Component.literal(SKIN_NAMES[s]).withStyle(isCurrent ? ChatFormatting.GOLD : ChatFormatting.WHITE),
                        button -> {
                            cat.setSkinId(sId);
                            PacketHandler.sendToServer(new PacketChangeCatSkin(cat.getId(), sId));
                            rebuildInterface();
                        }
                ).bounds(gridX + (s % 3) * 110, gridY + (s / 3) * 20, 105, 18).build());
            }
        } else if (styleSubTab == 2) {
            // Variants Info & Codex Link
            int varX = startX + 120;
            int varY = startY + 140;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Open Tier Evolution & Breeding Codex").withStyle(ChatFormatting.GOLD),
                    button -> {
                        this.minecraft.setScreen(new VariantChartScreen());
                    }
            ).bounds(varX, varY, 260, 22).build());
        } else {
            // Accessories & Collar Colors
            int colorX = startX + 110;
            int colorY = startY + 130;
            DyeColor[] colors = DyeColor.values();
            for (int c = 0; c < colors.length; c++) {
                final DyeColor dye = colors[c];
                this.addRenderableWidget(Button.builder(Component.empty(), button -> {
                    cat.setCollarColor(dye);
                    PacketHandler.sendToServer(new PacketChangeCollarColor(cat.getId(), dye.getId()));
                }).bounds(colorX + (c % 8) * 22, colorY + (c / 8) * 22, 18, 18).build());
            }
        }
    }

    // ==========================================
    // TAB 4: STATS TAB (2-Column Companion RPG Dashboard)
    // ==========================================
    private void initStatsTab() {
        // Statistics are protected and read-only.
        // In-world cleansing requires Sneak + Right-Click with Coal.
    }

    // ==========================================
    // TAB 5: COMBAT TAB (Role, Affinities & Loadouts)
    // ==========================================
    private void initCombatTab() {
        int panelW = Math.min(540, this.width - 40);
        int panelH = Math.min(270, this.height - 50);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2 - 10;

        // Auto-Calculate Role Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Auto (Talents)").withStyle(ChatFormatting.YELLOW),
                button -> {
                    com.kittytalents.combat.CatRoleEngine.autoUpdateRole(cat);
                    rebuildInterface();
                }
        ).bounds(startX + 10, startY + 26, 90, 18).build());

        // Selectable Combat Roles Buttons (Row of roles)
        com.kittytalents.combat.CatRole[] roles = {
                com.kittytalents.combat.CatRole.DPS,
                com.kittytalents.combat.CatRole.TANK,
                com.kittytalents.combat.CatRole.HEALER,
                com.kittytalents.combat.CatRole.MAGE,
                com.kittytalents.combat.CatRole.SCOUT,
                com.kittytalents.combat.CatRole.BRUISER
        };

        int rX = startX + 104;
        int rBtnW = 44;
        for (int i = 0; i < roles.length; i++) {
            final com.kittytalents.combat.CatRole r = roles[i];
            boolean isSel = (cat.getCatRole() == r);
            this.addRenderableWidget(Button.builder(
                    Component.literal(r.getDisplayName()).withStyle(isSel ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                    button -> {
                        cat.setCatRole(r); sendLoadout();
                        rebuildInterface();
                    }
            ).bounds(rX + i * (rBtnW + 2), startY + 26, rBtnW, 18).build());
        }

        // Elemental Affinity Switcher Buttons
        com.kittytalents.combat.FelineElement[] elements = {
                com.kittytalents.combat.FelineElement.PHYSICAL,
                com.kittytalents.combat.FelineElement.FIRE,
                com.kittytalents.combat.FelineElement.FROST,
                com.kittytalents.combat.FelineElement.ELECTRIC,
                com.kittytalents.combat.FelineElement.POISON,
                com.kittytalents.combat.FelineElement.SHADOW,
                com.kittytalents.combat.FelineElement.ARCANE
        };

        int elemX = startX + 10;
        int elemY = startY + 115;
        int elemBtnW = 56;
        for (int i = 0; i < elements.length; i++) {
            final com.kittytalents.combat.FelineElement el = elements[i];
            boolean isCur = (cat.getPrimaryElement() == el);
            this.addRenderableWidget(Button.builder(
                    Component.literal(el.name()).withStyle(isCur ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY),
                    button -> {
                        cat.setPrimaryElement(el); sendLoadout();
                        rebuildInterface();
                    }
            ).bounds(elemX + (i % 4) * (elemBtnW + 4), elemY + (i / 4) * 20, elemBtnW, 18).build());
        }

        // Right side: Loadout Presets
        int loadoutX = startX + panelW / 2 + 10;
        int loadoutY = startY + 195;

        this.addRenderableWidget(Button.builder(
                Component.literal("Balanced Preset"),
                button -> {
                    cat.setAbilitySlot1("feline_dash");
                    cat.setAbilitySlot2("feline_hiss");
                    cat.setAbilitySlot3("healing_purr");
                    cat.setAbilityUltimate("catnip_frenzy"); sendLoadout();
                    rebuildInterface();
                }
        ).bounds(loadoutX, loadoutY, 115, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Berserker Preset"),
                button -> {
                    cat.setAbilitySlot1("shadow_strike");
                    cat.setAbilitySlot2("venom_fang");
                    cat.setAbilitySlot3("inferno_spin");
                    cat.setAbilityUltimate("apex_predator"); sendLoadout();
                    rebuildInterface();
                }
        ).bounds(loadoutX + 120, loadoutY, 115, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Elemental Preset"),
                button -> {
                    cat.setAbilitySlot1("volt_dash");
                    cat.setAbilitySlot2("frost_nova");
                    cat.setAbilitySlot3("tidal_surge");
                    cat.setAbilityUltimate("soul_flame_barrage"); sendLoadout();
                    rebuildInterface();
                }
        ).bounds(loadoutX, loadoutY + 20, 115, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Guardian Preset"),
                button -> {
                    cat.setAbilitySlot1("fortress_guard");
                    cat.setAbilitySlot2("feline_dash");
                    cat.setAbilitySlot3("healing_purr");
                    cat.setAbilityUltimate("celestial_purr"); sendLoadout();
                    rebuildInterface();
                }
        ).bounds(loadoutX + 120, loadoutY + 20, 115, 18).build());

        // Slot cycle buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("Cycle [V]"),
                button -> {
                    cycleAbility(1);
                    rebuildInterface();
                }
        ).bounds(loadoutX, startY + 48, 105, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cycle [G]"),
                button -> {
                    cycleAbility(2);
                    rebuildInterface();
                }
        ).bounds(loadoutX + 110, startY + 48, 105, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cycle [B]"),
                button -> {
                    cycleAbility(3);
                    rebuildInterface();
                }
        ).bounds(loadoutX, startY + 112, 105, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cycle [R]"),
                button -> {
                    cycleAbility(4);
                    rebuildInterface();
                }
        ).bounds(loadoutX + 110, startY + 112, 105, 18).build());
    }

    private void cycleAbility(int slot) {
        List<com.kittytalents.ability.CatAbility> skills = com.kittytalents.ability.CatAbilities.getSkills();
        List<com.kittytalents.ability.CatAbility> ults = com.kittytalents.ability.CatAbilities.getUltimates();
        if (skills.isEmpty()) return;

        if (slot == 1) {
            String cur = cat.getAbilitySlot1();
            int idx = findAbilityIndex(skills, cur);
            cat.setAbilitySlot1(skills.get((idx + 1) % skills.size()).getId()); sendLoadout();
        } else if (slot == 2) {
            String cur = cat.getAbilitySlot2();
            int idx = findAbilityIndex(skills, cur);
            cat.setAbilitySlot2(skills.get((idx + 1) % skills.size()).getId()); sendLoadout();
        } else if (slot == 3) {
            String cur = cat.getAbilitySlot3();
            int idx = findAbilityIndex(skills, cur);
            cat.setAbilitySlot3(skills.get((idx + 1) % skills.size()).getId()); sendLoadout();
        } else if (slot == 4 && !ults.isEmpty()) {
            String cur = cat.getAbilityUltimate();
            int idx = findAbilityIndex(ults, cur);
            cat.setAbilityUltimate(ults.get((idx + 1) % ults.size()).getId()); sendLoadout();
        }
    }

    private int findAbilityIndex(List<com.kittytalents.ability.CatAbility> list, String id) {
        if (id == null || id.isEmpty()) return -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equalsIgnoreCase(id)) return i;
        }
        return -1;
    }

    // ==========================================
    // TAB 6: TWINE TAB (Direct Inhabitation & Controls)
    // ==========================================
    private void initTwineTab() {
        int panelW = Math.min(540, this.width - 40);
        int panelH = Math.min(270, this.height - 50);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2 - 10;

        // Big Prominent "✦ ENTER TWINE MODE ✦" Button
        this.addRenderableWidget(Button.builder(
                Component.literal("✦ COMMENCE TWINING (BECOME CAT) ✦").withStyle(ChatFormatting.AQUA),
                button -> {
                    PacketHandler.sendToServer(new PacketTwineAction(cat.getId(), true));
                    this.onClose();
                }
        ).bounds(startX + (panelW - 240) / 2, startY + 28, 240, 22).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (cat == null) return;

        catModelRotation += partialTick * 0.5F;

        if (currentTab == 0) {
            renderHomeBg(graphics, mouseX, mouseY);
        } else if (currentTab == 1) {
            renderTalentsBg(graphics);
        } else if (currentTab == 2) {
            renderGearBg(graphics, mouseX, mouseY);
        } else if (currentTab == 3) {
            renderStyleBg(graphics, mouseX, mouseY);
        } else if (currentTab == 4) {
            renderStatsBg(graphics);
        } else if (currentTab == 5) {
            renderCombatBg(graphics, mouseX, mouseY);
        } else if (currentTab == 6) {
            renderTwineBg(graphics, mouseX, mouseY);
        }

        // Bottom Nav Bar outline
        int tabBtnW = 46;
        int navWidth = TAB_NAMES.length * (tabBtnW + 4);
        int navX = (this.width - navWidth) / 2;
        int navY = this.height - 26;
        graphics.fill(navX - 2, navY, navX + navWidth + 2, navY + 22, 0xCC0F172A);
        graphics.fill(navX - 2, navY, navX + navWidth + 2, navY + 1, 0xFF6366F1);
    }

    private void renderHomeBg(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = (int) (this.width * 0.38F);
        int centerY = (int) (this.height * 0.52F);

        // Circular Emblem / Aura Seal behind Cat
        int sealRadius = 38;
        graphics.fill(centerX - sealRadius, centerY - sealRadius - 15, centerX + sealRadius, centerY + sealRadius - 15, 0x44D97706);
        graphics.fill(centerX - sealRadius + 4, centerY - sealRadius - 11, centerX + sealRadius - 4, centerY + sealRadius - 19, 0x66B45309);

        // Skill points on top-left of crest
        graphics.drawString(this.font, "Pts: " + cat.getSkillPoints(), centerX - 56, centerY - 54, 0xFFFBBF24, false);

        // Name and Level above Cat
        String direTag = cat.isDireAwakened() ? "<D>" : "";
        String genderTag = cat.isFemale() ? "(F)" : "(M)";
        String levelHeader = direTag + "<" + cat.getCatLevel() + ">" + genderTag;
        graphics.drawCenteredString(this.font, levelHeader, centerX, centerY - 62, 0xFF9CA3AF);
        graphics.drawCenteredString(this.font, cat.getName().getString(), centerX, centerY - 50, cat.getProductionTier().getColorValue() | 0xFF000000);

        // Render 3D Cat Model
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, centerX, centerY + 10, 48, (float)(centerX) - mouseX, (float)(centerY - 20) - mouseY, cat);

        // Health Hearts below cat
        float hp = cat.getHealth();
        float maxHp = cat.getMaxHealth();
        String hpText = "♥ " + String.format("%.1f/%.0f", hp, maxHp);
        graphics.drawCenteredString(this.font, hpText, centerX, centerY + 20, 0xFFEF4444);

        // Center-Right: Statistics Block
        int statsX = (int) (this.width * 0.58F);
        int statsY = (int) (this.height * 0.35F);

        graphics.fill(statsX - 8, statsY - 8, statsX + 164, statsY + 98, 0x661F2937);

        graphics.drawString(this.font, "Name: " + cat.getName().getString(), statsX, statsY, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "Level: " + cat.getCatLevel(), statsX, statsY + 12, 0xFFFCD34D, false);
        graphics.drawString(this.font, "Gender: " + (cat.isFemale() ? "Female ♀" : "Male ♂"), statsX, statsY + 24, cat.isFemale() ? 0xFFF472B6 : 0xFF60A5FA, false);
        graphics.drawString(this.font, "Owner: " + (cat.getOwner() != null ? cat.getOwner().getName().getString() : (cat.isTame() ? "You" : "Untamed")), statsX, statsY + 36, 0xFFE5E7EB, false);
        java.util.Map<java.util.UUID, String> trusted = cat.getTrustedPlayers();
        if (!trusted.isEmpty()) {
            String list = String.join(", ", trusted.values());
            if (list.length() > 22) list = list.substring(0, 20) + "...";
            graphics.drawString(this.font, "Trusted: " + list, statsX, statsY + 48, 0xFF38BDF8, false);
        } else {
            graphics.drawString(this.font, "Trusted: None", statsX, statsY + 48, 0xFF9CA3AF, false);
        }
        graphics.drawString(this.font, "Breed: " + cat.getCatVariant().getDisplayName(), statsX, statsY + 60, cat.getCatVariant().getRarity().getColor(), false);
        graphics.drawString(this.font, "Tier: " + cat.getProductionTier().getDisplayName(), statsX, statsY + 72, cat.getProductionTier().getColorValue(), false);
        graphics.drawString(this.font, "Hunger: " + cat.getHunger() + "/120", statsX, statsY + 84, 0xFF34D399, false);

        if (nameEditBox != null) nameEditBox.render(graphics, mouseX, mouseY, 0.0F);
        if (trustEditBox != null) trustEditBox.render(graphics, mouseX, mouseY, 0.0F);
    }

    private void renderTalentsBg(GuiGraphics graphics) {
        int panelW = Math.min(540, this.width - 40);
        int panelH = Math.min(270, this.height - 50);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2 - 10;

        graphics.fill(startX, startY, startX + panelW, startY + panelH, 0xEE111827);
        graphics.fill(startX + 1, startY + 1, startX + panelW - 1, startY + 24, 0xFF1F2937);

        // Right Detail Panel
        if (selectedTalent != null) {
            int rightX = startX + 160;
            int rightY = startY + 30;
            int rightW = panelW - 170;

            // Title in Yellow
            graphics.drawString(this.font, selectedTalent.getDisplayName().toUpperCase(), rightX, rightY, 0xFFFACC15, false);

            // Multiline Description
            Component desc = selectedTalent.getDescription();
            int lineY = rightY + 16;
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(desc, rightW - 10);
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, rightX, lineY, 0xFFE5E7EB, false);
                lineY += 10;
            }

            // Mastery perk (Phase 4): gold when earned, grey with the required rank when not.
            boolean mastered = com.kittytalents.talent.effect.TalentEngine.isMastered(cat, selectedTalent);
            Component masteryLabel = Component.translatable("gui.kittytalents.mastery")
                    .append(mastered ? " ★ " : " (rank " + selectedTalent.getMaxLevel() + "): ");
            Component masteryText = masteryLabel.copy().append(Component.translatable("talent.kittytalents." + selectedTalent.getId() + ".mastery"));
            lineY += 6;
            for (net.minecraft.util.FormattedCharSequence line : this.font.split(masteryText, rightW - 10)) {
                graphics.drawString(this.font, line, rightX, lineY, mastered ? 0xFFFBBF24 : 0xFF9CA3AF, false);
                lineY += 10;
            }

            // Level Badge & Crest
            int crestY = startY + panelH - 42;
            int currentLvl = cat.getTalentLevel(selectedTalent);
            graphics.fill(rightX, crestY, rightX + 24, crestY + 24, 0xFFD97706);
            graphics.drawCenteredString(this.font, String.valueOf(currentLvl), rightX + 12, crestY + 8, 0xFFFFFFFF);

            graphics.drawString(this.font, "Current Level: " + currentLvl + " / " + selectedTalent.getMaxLevel(), rightX + 32, crestY + 4, 0xFFFCD34D, false);
            if (currentLvl >= selectedTalent.getMaxLevel()) {
                graphics.drawString(this.font, "Max Level!", rightX + 32, crestY + 14, 0xFF10B981, false);
            } else {
                int cost = cat.getCostForNextRank(selectedTalent);
                graphics.drawString(this.font, "Upgrade Cost: " + cost + " Skill Points", rightX + 32, crestY + 14, 0xFF60A5FA, false);
            }
        }
    }

    private void renderGearBg(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 14;

        // Header Title with Tier Color
        graphics.drawCenteredString(this.font, cat.getName().getString() + " - Equipment Loadout", centerX, centerY - 82, cat.getProductionTier().getColorValue() | 0xFF000000);

        // Render 3D Cat in center of equipment
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, centerX, centerY + 18, 45, (float)(centerX) - mouseX, (float)(centerY - 20) - mouseY, cat);

        // Draw slots outlines and item contents
        drawGearSlot(graphics, mouseX, mouseY, centerX - 10, centerY - 64, cat.getArmorSlotItem(1), "Helmet");
        drawGearSlot(graphics, mouseX, mouseY, centerX - 70, centerY - 20, cat.getEquippedWeapon(), "Weapon");
        drawGearSlot(graphics, mouseX, mouseY, centerX + 50, centerY - 20, cat.getEquippedAccessory(), "Accessory");
        drawGearSlot(graphics, mouseX, mouseY, centerX - 10, centerY + 24, cat.getArmorSlotItem(2), "Chest");
        drawGearSlot(graphics, mouseX, mouseY, centerX - 10, centerY + 46, cat.getArmorSlotItem(3), "Legs");
        drawGearSlot(graphics, mouseX, mouseY, centerX - 10, centerY + 68, cat.getArmorSlotItem(4), "Boots");
        drawGearSlot(graphics, mouseX, mouseY, centerX + 50, centerY + 68, cat.getEquippedTool(), "Tool");

        int cosY = centerY + 94;
        drawGearSlot(graphics, mouseX, mouseY, centerX - 60, cosY, cat.getCosmeticSlot(0), "Cosmetic 1");
        drawGearSlot(graphics, mouseX, mouseY, centerX - 10, cosY, cat.getCosmeticSlot(1), "Cosmetic 2");
        drawGearSlot(graphics, mouseX, mouseY, centerX + 40, cosY, cat.getCosmeticSlot(2), "Cosmetic 3");
    }

    private void drawGearSlot(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, ItemStack item, String label) {
        graphics.fill(x - 1, y - 1, x + 19, y + 19, 0xFF374151);
        graphics.fill(x, y, x + 18, y + 18, 0xFF1F2937);
        if (!item.isEmpty()) {
            graphics.renderItem(item, x + 1, y + 1);
            graphics.renderItemDecorations(this.font, item, x + 1, y + 1);
        } else {
            graphics.drawCenteredString(this.font, label.substring(0, 1), x + 9, y + 5, 0xFF4B5563);
        }

        if (mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
            graphics.fill(x, y, x + 18, y + 18, 0x44FFFFFF);
            if (!item.isEmpty()) {
                graphics.renderTooltip(this.font, item, mouseX, mouseY);
            } else {
                graphics.renderTooltip(this.font, Component.literal(label + " (Empty - Click to Equip)"), mouseX, mouseY);
            }
        }
    }

    private void renderStyleBg(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelW = Math.min(520, this.width - 40);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - 260) / 2 - 10;

        graphics.fill(startX, startY, startX + panelW, startY + 260, 0xEE111827);

        // Center 3D Cat preview
        int catX = startX + panelW / 2 + 30;
        int catY = startY + 70;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, catX, catY, 44, (float)(catX) - mouseX, (float)(catY - 20) - mouseY, cat);

        // Read-only Gender line
        graphics.drawString(this.font, "Gender: " + (cat.isFemale() ? "Female ♀" : "Male ♂"), startX + 110, startY + 20, cat.isFemale() ? 0xFFF472B6 : 0xFF60A5FA, false);
        graphics.drawString(this.font, "(Requires Gender Ribbon to change)", startX + 110, startY + 32, 0xFF9CA3AF, false);

        // Current appearance
        graphics.drawString(this.font, "Breed: " + cat.getCatVariant().getDisplayName(), startX + 110, startY + 46, cat.getCatVariant().getRarity().getColor(), false);
        graphics.drawString(this.font, "Tier: " + cat.getProductionTier().getDisplayName() + " Tier", startX + 110, startY + 58, cat.getProductionTier().getColorValue() | 0xFF000000, false);
        graphics.drawString(this.font, "Skin: " + (cat.getSkinId() < SKIN_NAMES.length ? SKIN_NAMES[cat.getSkinId()] : "Custom"), startX + 110, startY + 70, 0xFFE5E7EB, false);
    }

    private void renderStatsBg(GuiGraphics graphics) {
        int panelW = Math.min(500, this.width - 40);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - 240) / 2 - 10;

        graphics.fill(startX, startY, startX + panelW, startY + 240, 0xEE111827);
        graphics.fill(startX + 1, startY + 1, startX + panelW - 1, startY + 24, 0xFF1F2937);

        graphics.drawString(this.font, "COMPANION RPG STATISTICS - " + cat.getName().getString().toUpperCase(), startX + 16, startY + 8, cat.getProductionTier().getColorValue() | 0xFF000000, false);

        int col1X = startX + 20;
        int col2X = startX + panelW / 2 + 10;
        int statY = startY + 36;

        // Column 1
        graphics.drawString(this.font, "Total Kills: " + cat.getTotalKills(), col1X, statY, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "Hostile Kills: " + cat.getHostileKills(), col1X, statY + 16, 0xFFEF4444, false);
        graphics.drawString(this.font, "Prey Kills: " + cat.getPassiveKills(), col1X, statY + 32, 0xFFFBBF24, false);
        graphics.drawString(this.font, "Damage Dealt: " + (int) cat.getDamageDealt(), col1X, statY + 48, 0xFFF87171, false);
        graphics.drawString(this.font, "Damage Taken: " + (int) cat.getDamageTaken(), col1X, statY + 64, 0xFFF43F5E, false);
        graphics.drawString(this.font, "Distance Traveled: " + (int) cat.getDistanceTraveled() + "m", col1X, statY + 80, 0xFF60A5FA, false);
        graphics.drawString(this.font, "Food Consumed: " + cat.getFoodConsumed(), col1X, statY + 96, 0xFF34D399, false);

        // Column 2
        graphics.drawString(this.font, "Blocks Mined: " + cat.getBlocksMined(), col2X, statY, 0xFFFCD34D, false);
        graphics.drawString(this.font, "Items Retrieved: " + cat.getItemsRetrieved(), col2X, statY + 16, 0xFF38BDF8, false);
        graphics.drawString(this.font, "Pounces Performed: " + cat.getPouncesPerformed(), col2X, statY + 32, 0xFFA78BFA, false);
        graphics.drawString(this.font, "Creepers Scared: " + cat.getCreepersScared(), col2X, statY + 48, 0xFF4ADE80, false);
        graphics.drawString(this.font, "Healing Provided: " + cat.getHealingProvided() + " HP", col2X, statY + 64, 0xFFFB7185, false);
        graphics.drawString(this.font, "Times Petted: " + cat.getTimesPet(), col2X, statY + 80, 0xFFF472B6, false);
        graphics.drawString(this.font, "Frenzy Activations: " + cat.getFrenzyActivations(), col2X, statY + 96, 0xFFC084FC, false);

        // In-world cleansing notice
        graphics.drawString(this.font, "Stats are permanent. Sneak + Right-Click with Coal in-world to cleanse memories", col1X, startY + 200, 0xFF9CA3AF, false);
    }

    private void renderCombatBg(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelW = Math.min(540, this.width - 40);
        int panelH = Math.min(270, this.height - 50);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2 - 10;

        // Dark translucent panel with border
        graphics.fill(startX, startY, startX + panelW, startY + panelH, 0xEE0B0F19);
        graphics.renderOutline(startX, startY, panelW, panelH, 0xFF3B82F6);

        // Header Title
        graphics.drawString(this.font, "§6✦ COMBAT SPECIALIZATION & LOADOUTS ✦", startX + 10, startY + 10, 0xFFFFFFFF, false);

        // --- Left Column: Role & Element ---
        com.kittytalents.combat.CatRole role = cat.getCatRole();
        graphics.drawString(this.font, "Combat Role:", startX + 10, startY + 48, 0xFF94A3B8, false);
        graphics.drawString(this.font, "§e" + role.getDisplayName() + " §7- " + role.getDescription(), startX + 10, startY + 58, 0xFFE2E8F0, false);

        // Role stat modifiers summary
        String statSummary = String.format("HP: §a%.0f%%§7 | Dmg: §c%.0f%%§7 | Spd: §b%.0f%%§7 | Energy: §e%.0f%%§7 | Crit: §d+%.0f%%",
                role.getHpMultiplier() * 100, role.getDamageMultiplier() * 100, role.getSpeedMultiplier() * 100,
                role.getEnergyRegenMultiplier() * 100, role.getCritChanceBonus() * 100);
        graphics.drawString(this.font, statSummary, startX + 10, startY + 70, 0xFFCBD5E1, false);

        // Element Header
        graphics.drawString(this.font, "Elemental Affinity:", startX + 10, startY + 98, 0xFF94A3B8, false);
        com.kittytalents.combat.FelineElement el = cat.getPrimaryElement();
        String elDesc;
        switch (el) {
            case FIRE -> elDesc = "Ignites foes with continuous Burn damage and fiery aura.";
            case FROST -> elDesc = "Slows enemy mobility and inflicts Freezing debuffs.";
            case ELECTRIC -> elDesc = "Chains lightning between nearby enemies and shocks targets.";
            case POISON -> elDesc = "Inflicts lingering venom ticks that ignore defense.";
            case SHADOW -> elDesc = "Applies Wither decay and enhances stealth ambush strikes.";
            case ARCANE -> elDesc = "Deals raw magic damage and increases energy recovery.";
            default -> elDesc = "Pure physical kinetic strikes with high stagger potential.";
        }
        graphics.drawString(this.font, "§b" + el.name() + " §7- " + elDesc, startX + 10, startY + 160, 0xFFCBD5E1, false);

        // --- Right Column: Active Ability Loadout & Presets ---
        int rightX = startX + panelW / 2 + 10;
        graphics.drawString(this.font, "§aActive Ability Loadout (Twine Hotkeys)", rightX, startY + 10, 0xFFFFFFFF, false);

        // Ability Card 1 (V)
        renderAbilitySlotInfo(graphics, rightX, startY + 26, "V", cat.getAbilitySlot1(), "feline_dash");

        // Ability Card 2 (G)
        renderAbilitySlotInfo(graphics, rightX + 110, startY + 26, "G", cat.getAbilitySlot2(), "feline_hiss");

        // Ability Card 3 (B)
        renderAbilitySlotInfo(graphics, rightX, startY + 84, "B", cat.getAbilitySlot3(), "healing_purr");

        // Ability Card 4 (R - Ultimate)
        renderAbilitySlotInfo(graphics, rightX + 110, startY + 84, "R", cat.getAbilityUltimate(), "catnip_frenzy");

        // Presets header
        graphics.drawString(this.font, "Quick Loadout Presets:", rightX, startY + 180, 0xFF94A3B8, false);
    }

    private void renderAbilitySlotInfo(GuiGraphics graphics, int x, int y, String slotKey, String abilityId, String defaultId) {
        String actId = (abilityId != null && !abilityId.isEmpty()) ? abilityId : defaultId;
        com.kittytalents.ability.CatAbility abil = com.kittytalents.ability.CatAbilities.get(actId);
        String name = (abil != null) ? abil.getDisplayName() : actId.replace('_', ' ');
        if (name.length() > 14) name = name.substring(0, 13) + ".";

        graphics.fill(x, y, x + 105, y + 26, 0x991E293B);
        graphics.renderOutline(x, y, 105, 26, 0xFF475569);
        graphics.drawString(this.font, "§e[" + slotKey + "] §f" + name, x + 4, y + 4, 0xFFFFFFFF, false);

        int cost = (abil != null) ? abil.getEnergyCost() : 0;
        int cd = (abil != null) ? abil.getCooldownTicks() / 20 : 2;
        graphics.drawString(this.font, "§b" + cost + "⚡ §7| §c" + cd + "s cd", x + 4, y + 15, 0xFF94A3B8, false);
    }

    private void renderTwineBg(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelW = Math.min(540, this.width - 40);
        int panelH = Math.min(270, this.height - 50);
        int startX = (this.width - panelW) / 2;
        int startY = (this.height - panelH) / 2 - 10;

        // Dark translucent panel with cyan border
        graphics.fill(startX, startY, startX + panelW, startY + panelH, 0xEE070B14);
        graphics.renderOutline(startX, startY, panelW, panelH, 0xFF06B6D4);

        // Header Title
        graphics.drawString(this.font, "§b✦ SOUL TWINING (DIRECT COMPANION INHABITATION) ✦", startX + 10, startY + 10, 0xFFFFFFFF, false);

        // Overview / Status Description below the big button
        int contentY = startY + 56;
        graphics.drawString(this.font, "Twining allows direct 1st/3rd-person possession of your feline companion.", startX + 10, contentY, 0xFFE2E8F0, false);
        graphics.drawString(this.font, "Your human body enters a protective dormant state while your soul commands the cat.", startX + 10, contentY + 12, 0xFF94A3B8, false);

        // Divider
        graphics.fill(startX + 10, contentY + 28, startX + panelW - 10, contentY + 29, 0xFF1E293B);

        // Left Column: Keybinds & In-Twine Controls
        int col1X = startX + 14;
        int controlsY = contentY + 36;
        graphics.drawString(this.font, "§eTwine Control Mapping:", col1X, controlsY, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "• §f[W/A/S/D]§7: Feline Movement & Step-Assist (1.0 block)", col1X, controlsY + 14, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[Space]§7: High Pounce Jump & Wall Maneuvers", col1X, controlsY + 26, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[L-Click]§7: Claw Combo / Bite Finisher", col1X, controlsY + 38, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[R-Click]§7: Pounce Launch / World Interaction", col1X, controlsY + 50, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[Alt]§7: Directional 4-Way Evasive Dodge", col1X, controlsY + 62, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[V / G / B]§7: Trigger Slotted Abilities 1-3", col1X, controlsY + 74, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[R]§7: Unleash Ultimate Ability (at 100% meter)", col1X, controlsY + 86, 0xFFCBD5E1, false);
        graphics.drawString(this.font, "• §f[U]§7: Safely Disconnect and Return to Player Body", col1X, controlsY + 98, 0xFFCBD5E1, false);

        // Right Column: Active Feline Capabilities & Status
        int col2X = startX + panelW / 2 + 10;
        graphics.drawString(this.font, "§aInherent Feline Capabilities:", col2X, controlsY, 0xFFFFFFFF, false);
        renderCapabilityLine(graphics, "PASSIVE", "Fall Damage Immunity", "Cats land on their feet; zero fall damage.", col2X, controlsY + 14, 0xFF10B981);
        renderCapabilityLine(graphics, "PASSIVE", "1x1 Crawl & Squeeze", "Can slip through small crawlspaces and crawl tunnels.", col2X, controlsY + 38, 0xFF3B82F6);
        renderCapabilityLine(graphics, "RADAR", "Feline Low-Light Senses", "Enhanced night vision and ambient movement detection.", col2X, controlsY + 62, 0xFFF59E0B);
        renderCapabilityLine(graphics, "SYSTEM", "Feline Energy Reserve", "100 Energy pool regenerates at " + (int)(cat.getCatRole().getEnergyRegenMultiplier() * 10) + " pts/sec.", col2X, controlsY + 86, 0xFFA855F7);
    }

    private void renderCapabilityLine(GuiGraphics graphics, String tag, String title, String desc, int x, int y, int tagColor) {
        graphics.fill(x, y, x + 36, y + 10, tagColor | 0x33000000);
        graphics.renderOutline(x, y, 36, 10, tagColor);
        graphics.drawCenteredString(this.font, tag, x + 18, y + 1, tagColor);
        graphics.drawString(this.font, "§f" + title, x + 42, y + 1, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "§8" + desc, x + 4, y + 12, 0xFF94A3B8, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameEditBox != null && this.nameEditBox.isFocused()) {
            if (keyCode == 256) { // Escape
                this.nameEditBox.setFocused(false);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                String newName = nameEditBox.getValue().trim();
                if (!newName.isEmpty() && newName.length() <= 32) {
                    PacketHandler.sendToServer(new PacketRenameCat(cat.getId(), newName));
                }
                this.nameEditBox.setFocused(false);
                return true;
            }
            if (this.nameEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // Consume key completely to prevent closing the screen or swapping hotbar items while typing
            return true;
        }

        if (this.trustEditBox != null && this.trustEditBox.isFocused()) {
            if (keyCode == 256) { // Escape
                this.trustEditBox.setFocused(false);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                String target = trustEditBox.getValue().trim();
                if (!target.isEmpty()) {
                    PacketHandler.sendToServer(new PacketManageTrust(cat.getId(), 0, target));
                    trustEditBox.setValue("");
                }
                this.trustEditBox.setFocused(false);
                return true;
            }
            if (this.trustEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // Consume key completely to prevent closing the screen or swapping hotbar items while typing
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameEditBox != null && this.nameEditBox.isFocused()) {
            if (this.nameEditBox.charTyped(codePoint, modifiers)) {
                return true;
            }
            return true;
        }
        if (this.trustEditBox != null && this.trustEditBox.isFocused()) {
            if (this.trustEditBox.charTyped(codePoint, modifiers)) {
                return true;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (this.nameEditBox != null && !this.nameEditBox.isMouseOver(mouseX, mouseY)) {
            this.nameEditBox.setFocused(false);
        }
        if (this.trustEditBox != null && !this.trustEditBox.isMouseOver(mouseX, mouseY)) {
            this.trustEditBox.setFocused(false);
        }
        return handled;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Subtle translucent gradient so live Minecraft world remains visible behind companion interface
        graphics.fillGradient(0, 0, this.width, this.height, 0x3305050C, 0x550A0A14);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
