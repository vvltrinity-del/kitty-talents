package com.kittytalents.command;

import com.kittytalents.entity.TalentedCat;
import com.kittytalents.combat.CatRole;
import com.kittytalents.combat.FelineElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import java.util.List;

public class KittyRPGCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kittyrpg").requires(s -> s.hasPermission(2))
            .then(Commands.literal("setrole")
                .then(Commands.argument("role", StringArgumentType.string())
                    .executes(ctx -> {
                        Player p = ctx.getSource().getPlayerOrException();
                        String r = StringArgumentType.getString(ctx, "role");
                        CatRole role;
                        try { role = CatRole.valueOf(r.toUpperCase()); } catch (Exception e) {
                            ctx.getSource().sendFailure(Component.literal("Invalid role!")); return 0;
                        }
                        List<TalentedCat> cats = p.level().getEntitiesOfClass(TalentedCat.class, p.getBoundingBox().inflate(5.0));
                        for (TalentedCat cat : cats) {
                            if (cat.isOwnedBy(p)) {
                                cat.setCatRole(role);
                                ctx.getSource().sendSuccess(() -> Component.literal("Set role to " + role.getDisplayName()), false);
                                return 1;
                            }
                        }
                        return 0;
                    })))
            .then(Commands.literal("setability")
                .then(Commands.argument("slot", StringArgumentType.string())
                .then(Commands.argument("abilityId", StringArgumentType.string())
                    .executes(ctx -> {
                        Player p = ctx.getSource().getPlayerOrException();
                        String slot = StringArgumentType.getString(ctx, "slot");
                        String abil = StringArgumentType.getString(ctx, "abilityId");
                        List<TalentedCat> cats = p.level().getEntitiesOfClass(TalentedCat.class, p.getBoundingBox().inflate(5.0));
                        for (TalentedCat cat : cats) {
                            if (cat.isOwnedBy(p)) {
                                if (slot.equals("1")) cat.setAbilitySlot1(abil);
                                else if (slot.equals("2")) cat.setAbilitySlot2(abil);
                                else if (slot.equals("3")) cat.setAbilitySlot3(abil);
                                else if (slot.equals("ult")) cat.setAbilityUltimate(abil);
                                ctx.getSource().sendSuccess(() -> Component.literal("Set slot " + slot + " to " + abil), false);
                                return 1;
                            }
                        }
                        return 0;
                    }))))
            .then(Commands.literal("setelement")
                .then(Commands.argument("slot", StringArgumentType.string())
                .then(Commands.argument("element", StringArgumentType.string())
                    .executes(ctx -> {
                        Player p = ctx.getSource().getPlayerOrException();
                        String slot = StringArgumentType.getString(ctx, "slot");
                        String el = StringArgumentType.getString(ctx, "element");
                        FelineElement element;
                        try { element = FelineElement.valueOf(el.toUpperCase()); } catch (Exception e) { return 0; }
                        List<TalentedCat> cats = p.level().getEntitiesOfClass(TalentedCat.class, p.getBoundingBox().inflate(5.0));
                        for (TalentedCat cat : cats) {
                            if (cat.isOwnedBy(p)) {
                                if (slot.equals("primary")) cat.setPrimaryElement(element);
                                else cat.setSecondaryElement(element);
                                ctx.getSource().sendSuccess(() -> Component.literal("Set element to " + element.name()), false);
                                return 1;
                            }
                        }
                        return 0;
                    })))));
    }
}
