package com.guardmod.command;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.net.GuardNetwork;
import com.guardmod.validate.ValidationStateRegistry;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.model.ClientResourcePackEntry;
import com.guardmod.validate.ClientEnvironmentReportCache;
import com.guardmod.validate.ValidationRequestModeRegistry;
import com.guardmod.model.ClientShaderPackEntry;
import com.guardmod.validate.ShaderPackRulesValidator;
import com.guardmod.validate.ResourcePackRulesValidator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class GuardModCommands {
    private GuardModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guardmod")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> triggerSingleCheck(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("checkall")
                        .executes(context -> triggerCheckAll(context.getSource())))
                .then(Commands.literal("packs")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> showResourcePacks(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("shaders")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> showShaderPacks(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))));
    }

    private static int triggerSingleCheck(CommandSourceStack source, ServerPlayer player) {
        requestValidation(player);
        source.sendSuccess(() -> Component.literal(
                "GuardMod: запрошена принудительная проверка игрока " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int triggerCheckAll(CommandSourceStack source) {
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        int count = 0;

        for (ServerPlayer player : players) {
            requestValidation(player);
            count++;
        }

        final int finalCount = count;
        source.sendSuccess(() -> Component.literal(
                "GuardMod: запрошена принудительная проверка всех игроков. Количество: " + finalCount), true);
        return count;
    }

    private static void requestValidation(ServerPlayer player) {
        long timeoutMillis = GuardCommonConfig.VALIDATION_TIMEOUT_SECONDS.get().longValue() * 1000L;
        long deadlineEpochMillis = System.currentTimeMillis() + timeoutMillis;

        ValidationRequestModeRegistry.setMode(player, ValidationRequestModeRegistry.RequestMode.FULL);

        ValidationStateRegistry.ValidationTicket ticket =
                ValidationStateRegistry.createPending(player, deadlineEpochMillis);

        GuardNetwork.sendEnvironmentRequest(player, ticket.getRequestId(), ticket.getDeadlineEpochMillis());
    }
    private static int showResourcePacks(CommandSourceStack source, ServerPlayer player) {
        ClientEnvironmentReport report = ClientEnvironmentReportCache.get(player);
        if (report == null) {
            source.sendFailure(Component.literal(
                    "GuardMod: нет последнего отчёта для игрока " + player.getGameProfile().getName()
                            + ". Сначала запусти /guardmod check " + player.getGameProfile().getName()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "GuardMod: ресурспаки игрока " + player.getGameProfile().getName()), false);

        source.sendSuccess(() -> Component.literal(
                "  Активные: " + formatPackNames(report.getActiveResourcePacks())), false);

        source.sendSuccess(() -> Component.literal(
                "  Установленные внешние: " + formatPackNames(report.getInstalledResourcePacks())), false);

        java.util.List<ClientResourcePackEntry> unknownInstalled =
                ResourcePackRulesValidator.collectUnknownInstalledResourcePacks(report);

        source.sendSuccess(() -> Component.literal(
                "  Неразрешённые установленные: " + formatPackNames(unknownInstalled)), false);

        return 1;
    }

    private static String formatPackNames(List<ClientResourcePackEntry> packs) {
        if (packs == null || packs.isEmpty()) {
            return "(нет)";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < packs.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            ClientResourcePackEntry entry = packs.get(i);
            builder.append(entry == null ? "" : entry.getDisplayName());
        }

        return builder.toString();
    }
    private static int showShaderPacks(CommandSourceStack source, ServerPlayer player) {
        ClientEnvironmentReport report = ClientEnvironmentReportCache.get(player);
        if (report == null) {
            source.sendFailure(Component.literal(
                    "GuardMod: нет последнего отчёта для игрока " + player.getGameProfile().getName()
                            + ". Сначала запусти /guardmod check " + player.getGameProfile().getName()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "GuardMod: шейдеры игрока " + player.getGameProfile().getName()), false);

        source.sendSuccess(() -> Component.literal(
                "  Runtime: " + (report.getShaderRuntime() == null || report.getShaderRuntime().isEmpty()
                        ? "(none)"
                        : report.getShaderRuntime())), false);

        source.sendSuccess(() -> Component.literal(
                "  Активный шейдерпак: " + (report.getActiveShaderPack() == null
                        ? "(нет)"
                        : report.getActiveShaderPack().getDisplayName())), false);

        source.sendSuccess(() -> Component.literal(
                "  Установленные внешние шейдерпаки: " + formatShaderPackNames(report.getInstalledShaderPacks())), false);

        java.util.List<ClientShaderPackEntry> unknownInstalled =
                ShaderPackRulesValidator.collectUnknownInstalledShaderPacks(report);

        source.sendSuccess(() -> Component.literal(
                "  Неразрешённые установленные шейдерпаки: " + formatShaderPackNames(unknownInstalled)), false);

        return 1;
    }

    private static String formatShaderPackNames(List<ClientShaderPackEntry> packs) {
        if (packs == null || packs.isEmpty()) {
            return "(нет)";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < packs.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            ClientShaderPackEntry entry = packs.get(i);
            builder.append(entry == null ? "" : entry.getDisplayName());
        }

        return builder.toString();
    }
}
