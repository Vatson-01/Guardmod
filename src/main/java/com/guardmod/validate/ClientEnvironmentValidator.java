package com.guardmod.validate;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.model.ClientEnvironmentReport;
import com.guardmod.net.GuardNetwork;
import com.guardmod.net.packet.C2SEnvironmentReportPacket;
import com.mojang.logging.LogUtils;
import com.guardmod.model.ClientShaderPackEntry;
import net.minecraft.network.chat.Component;
import com.guardmod.model.ClientResourcePackEntry;
import net.minecraft.server.level.ServerPlayer;
import com.guardmod.validate.ValidationRequestModeRegistry;
import org.slf4j.Logger;
import java.util.List;

public final class ClientEnvironmentValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientEnvironmentValidator() {
    }

    public static void validateAndConsume(ServerPlayer player, C2SEnvironmentReportPacket packet) {
        if (player == null || packet == null) {
            return;
        }

        boolean reactiveRevalidation = packet.getRequestId() == 0L;
        ValidationRequestModeRegistry.RequestMode requestMode = reactiveRevalidation
                ? ValidationRequestModeRegistry.RequestMode.DYNAMIC_ASSETS
                : ValidationRequestModeRegistry.getMode(player);

        if (!reactiveRevalidation && !ValidationStateRegistry.isRequestExpected(player, packet.getRequestId())) {
            LOGGER.warn("GuardMod rejected validation response from {} because requestId {} was not expected.",
                    player.getGameProfile().getName(), packet.getRequestId());
            disconnect(player, "Unexpected validation response");
            return;
        }

        if (reactiveRevalidation) {
            LOGGER.info("GuardMod received reactive environment report from {}.",
                    player.getGameProfile().getName());
        }

        ClientEnvironmentReport report = packet.getReport();
        if (report == null) {
            if (!reactiveRevalidation) {
                ValidationStateRegistry.clear(player);
            }
            LOGGER.warn("GuardMod rejected {} because the client report was missing.", player.getGameProfile().getName());
            disconnect(player, "Client report is missing");
            return;
        }
        if (report.getProtocolVersion() != GuardNetwork.getProtocolVersionNumber()) {
            if (!reactiveRevalidation) {
                ValidationStateRegistry.clear(player);
            }
            LOGGER.warn("GuardMod rejected {} because of protocol version mismatch. Expected {}, got {}.",
                    player.getGameProfile().getName(),
                    GuardNetwork.getProtocolVersionNumber(),
                    report.getProtocolVersion());
            disconnect(player, "Protocol version mismatch");
            return;
        }
        if (requestMode == ValidationRequestModeRegistry.RequestMode.FULL) {
            ValidationResult modResult = ModRulesValidator.validateBootstrapRules(report);
            if (!modResult.isSuccess()) {
                if (!reactiveRevalidation) {
                    ValidationStateRegistry.clear(player);
                }
                LOGGER.warn("GuardMod validation failed for {} at stage {}: {}",
                        player.getGameProfile().getName(),
                        modResult.getFailedStage(),
                        modResult.getReason());
                disconnect(player, modResult.getReason());
                return;
            }
        }

        ValidationResult resourcePackResult = ResourcePackRulesValidator.validateActiveResourcePacks(report);
        if (!resourcePackResult.isSuccess()) {
            if (!reactiveRevalidation) {
                ValidationStateRegistry.clear(player);
            }
            LOGGER.warn("GuardMod validation failed for {} at stage {}: {}",
                    player.getGameProfile().getName(),
                    resourcePackResult.getFailedStage(),
                    resourcePackResult.getReason());
            disconnect(player, resourcePackResult.getReason());
            return;
        }

        ValidationResult shaderPackResult = ShaderPackRulesValidator.validateActiveShaderPack(report);
        if (!shaderPackResult.isSuccess()) {
            if (!reactiveRevalidation) {
                ValidationStateRegistry.clear(player);
            }
            LOGGER.warn("GuardMod validation failed for {} at stage {}: {}",
                    player.getGameProfile().getName(),
                    shaderPackResult.getFailedStage(),
                    shaderPackResult.getReason());
            disconnect(player, shaderPackResult.getReason());
            return;
        }

        ClientEnvironmentReport previousReport = ClientEnvironmentReportCache.get(player);
        ClientEnvironmentReportCache.put(player, report);
        ValidationStateRegistry.markValidated(player, report);

        List<ClientResourcePackEntry> unknownInstalledResourcePacks =
                ResourcePackRulesValidator.collectUnknownInstalledResourcePacks(report);

        if (GuardCommonConfig.WARN_ON_UNKNOWN_INSTALLED_RESOURCEPACKS.get()
                && shouldWarnAboutInstalledResourcePacks(previousReport, report)
                && !unknownInstalledResourcePacks.isEmpty()) {
            String names = joinPackNames(unknownInstalledResourcePacks, 6);
            LOGGER.warn("GuardMod found unknown installed resource packs on {}: {}",
                    player.getGameProfile().getName(), names);
            player.sendSystemMessage(Component.literal(
                    "GuardMod: найдены неразрешённые установленные ресурспаки: " + names));
        }

        List<ClientShaderPackEntry> unknownInstalledShaderPacks =
                ShaderPackRulesValidator.collectUnknownInstalledShaderPacks(report);

        if (GuardCommonConfig.WARN_ON_UNKNOWN_INSTALLED_SHADERPACKS.get()
                && shouldWarnAboutInstalledShaderPacks(previousReport, report)
                && !unknownInstalledShaderPacks.isEmpty()) {
            String names = joinShaderPackNames(unknownInstalledShaderPacks, 6);
            LOGGER.warn("GuardMod found unknown installed shader packs on {}: {}",
                    player.getGameProfile().getName(), names);
            player.sendSystemMessage(Component.literal(
                    "GuardMod: найдены неразрешённые установленные шейдерпаки: " + names));
        }

        if (reactiveRevalidation) {
            LOGGER.info("GuardMod reactive revalidation passed for {}. Reported client mods: {}, active resource packs: {}, installed external resource packs: {}, shader runtime: {}, active shader pack: {}, installed shader packs: {}",
                    player.getGameProfile().getName(),
                    report.getMods().size(),
                    report.getActiveResourcePacks().size(),
                    report.getInstalledResourcePacks().size(),
                    report.getShaderRuntime(),
                    report.getActiveShaderPack() == null ? "(none)" : report.getActiveShaderPack().getDisplayName(),
                    report.getInstalledShaderPacks().size());
        } else if (requestMode == ValidationRequestModeRegistry.RequestMode.DYNAMIC_ASSETS) {
            if (GuardCommonConfig.LOG_PERIODIC_VALIDATION_PASSES.get()) {
                LOGGER.info("GuardMod periodic dynamic validation passed for {}. Active resource packs: {}, shader runtime: {}, active shader pack: {}",
                        player.getGameProfile().getName(),
                        report.getActiveResourcePacks().size(),
                        report.getShaderRuntime(),
                        report.getActiveShaderPack() == null ? "(none)" : report.getActiveShaderPack().getDisplayName());
            }
        } else {
            LOGGER.info("GuardMod validation passed for {}. Reported client mods: {}, active resource packs: {}, installed external resource packs: {}, shader runtime: {}, active shader pack: {}, installed shader packs: {}",
                    player.getGameProfile().getName(),
                    report.getMods().size(),
                    report.getActiveResourcePacks().size(),
                    report.getInstalledResourcePacks().size(),
                    report.getShaderRuntime(),
                    report.getActiveShaderPack() == null ? "(none)" : report.getActiveShaderPack().getDisplayName(),
                    report.getInstalledShaderPacks().size());
        }
    }

    private static void disconnect(ServerPlayer player, String reason) {
        GuardNetwork.sendValidationResult(player, false, reason);

        String message = GuardCommonConfig.VERBOSE_KICK_REASON.get() ? reason : "Client validation failed";
        player.connection.disconnect(Component.literal(message));

    }
    private static boolean shouldWarnAboutInstalledResourcePacks(ClientEnvironmentReport previousReport,
                                                                 ClientEnvironmentReport currentReport) {
        if (currentReport == null) {
            return false;
        }
        if (previousReport == null) {
            return true;
        }
        return !buildInstalledFingerprint(previousReport).equals(buildInstalledFingerprint(currentReport));
    }

    private static String buildInstalledFingerprint(ClientEnvironmentReport report) {
        if (report == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (ClientResourcePackEntry entry : report.getInstalledResourcePacks()) {
            builder.append(entry.getDisplayName() == null ? "" : entry.getDisplayName()).append('|');
            builder.append(entry.getSourceType() == null ? "" : entry.getSourceType()).append('|');
            builder.append(entry.getContentHash() == null ? "" : entry.getContentHash()).append('|');
            builder.append(entry.getRawFileHash() == null ? "" : entry.getRawFileHash()).append('\n');
        }
        return builder.toString();
    }

    private static String joinPackNames(List<ClientResourcePackEntry> packs, int limit) {
        if (packs == null || packs.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;

        for (ClientResourcePackEntry entry : packs) {
            if (entry == null) {
                continue;
            }

            if (count > 0) {
                builder.append(", ");
            }

            builder.append(entry.getDisplayName() == null ? "" : entry.getDisplayName());
            count++;

            if (count >= limit) {
                if (packs.size() > limit) {
                    builder.append(" ... +").append(packs.size() - limit);
                }
                break;
            }
        }

        return builder.toString();
    }
    private static boolean shouldWarnAboutInstalledShaderPacks(ClientEnvironmentReport previousReport,
                                                               ClientEnvironmentReport currentReport) {
        if (currentReport == null) {
            return false;
        }
        if (previousReport == null) {
            return true;
        }
        return !buildInstalledShaderFingerprint(previousReport).equals(buildInstalledShaderFingerprint(currentReport));
    }

    private static String buildInstalledShaderFingerprint(ClientEnvironmentReport report) {
        if (report == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (ClientShaderPackEntry entry : report.getInstalledShaderPacks()) {
            builder.append(entry.getDisplayName() == null ? "" : entry.getDisplayName()).append('|');
            builder.append(entry.getSourceType() == null ? "" : entry.getSourceType()).append('|');
            builder.append(entry.getContentHash() == null ? "" : entry.getContentHash()).append('|');
            builder.append(entry.getRawFileHash() == null ? "" : entry.getRawFileHash()).append('\n');
        }
        return builder.toString();
    }

    private static String joinShaderPackNames(List<ClientShaderPackEntry> packs, int limit) {
        if (packs == null || packs.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;

        for (ClientShaderPackEntry entry : packs) {
            if (entry == null) {
                continue;
            }

            if (count > 0) {
                builder.append(", ");
            }

            builder.append(entry.getDisplayName() == null ? "" : entry.getDisplayName());
            count++;

            if (count >= limit) {
                if (packs.size() > limit) {
                    builder.append(" ... +").append(packs.size() - limit);
                }
                break;
            }
        }

        return builder.toString();
    }
}