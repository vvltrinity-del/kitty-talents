package com.kittytalents.command;

import com.kittytalents.test.CatTestHarness;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class KittyTalentsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kittytalents")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("audit").executes(context -> {
            CommandSourceStack source = context.getSource();
            source.sendSuccess(() -> Component.literal("§6[Kitty Talents]§r Running Real Programmatic Feature Audit..."), false);

            CatTestHarness.AuditResult result = CatTestHarness.runAudit(source);
            for (String line : result.details) {
                source.sendSuccess(() -> Component.literal(line), false);
            }

            if (result.failed == 0) {
                source.sendSuccess(() -> Component.literal(String.format("§a[AUDIT COMPLETE]§r %d checks passed (%d warnings, %d failures).",
                        result.passed, result.warnings, result.failed)), false);
                return 1;
            } else {
                source.sendFailure(Component.literal(String.format("§c[AUDIT FAILED]§r %d failures detected (%d passed, %d warnings).",
                        result.failed, result.passed, result.warnings)));
                return 0;
            }
        }));

        root.then(Commands.literal("nekonight")
                .then(Commands.literal("start").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    com.kittytalents.event.NekoNightManager.startNekoNight(source.getLevel());
                    source.sendSuccess(() -> Component.literal("§d[Kitty Talents]§r Neko Night event force started!"), true);
                    return 1;
                }))
                .then(Commands.literal("stop").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    com.kittytalents.event.NekoNightManager.stopNekoNight(source.getLevel());
                    source.sendSuccess(() -> Component.literal("§b[Kitty Talents]§r Neko Night event ended!"), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    boolean active = com.kittytalents.event.NekoNightManager.isNekoNight(source.getLevel());
                    source.sendSuccess(() -> Component.literal("§6[Kitty Talents]§r Neko Night is currently: " + (active ? "§aACTIVE" : "§cINACTIVE")), false);
                    return active ? 1 : 0;
                }))
        );

        dispatcher.register(root);
    }
}
