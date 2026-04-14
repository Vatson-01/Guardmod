package com.guardmod.scanner;

import com.guardmod.model.ScannedShaderPack;
import com.guardmod.validate.ShaderPackPolicySnapshot;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShaderPackScanService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile ShaderPackPolicySnapshot snapshot =
            ShaderPackPolicySnapshot.failed("No shader pack scan has been performed yet");

    private ShaderPackScanService() {
    }

    public static void rescanFromServer(Object server) {
        Path serverRoot = resolveServerRoot(server);
        if (serverRoot == null) {
            snapshot = ShaderPackPolicySnapshot.failed("Unable to resolve server directory for shader packs");
            LOGGER.error("GuardMod could not resolve the server directory for shader pack scanning.");
            return;
        }

        ensureDirectories(serverRoot);

        Path allowedShaderPacksDirectory = serverRoot.resolve("guardmod").resolve("allowed_shaderpacks");
        try {
            List<ScannedShaderPack> scannedPacks = ShaderPackScanner.scan(allowedShaderPacksDirectory);
            Map<String, ScannedShaderPack> allowedByContentHash = new LinkedHashMap<String, ScannedShaderPack>();

            for (ScannedShaderPack pack : scannedPacks) {
                String normalizedHash = normalizeHash(pack.getContentHash());
                if (!normalizedHash.isEmpty()) {
                    allowedByContentHash.put(normalizedHash, pack);
                }
            }

            snapshot = ShaderPackPolicySnapshot.ready(
                    allowedByContentHash,
                    "Allowed shader packs: " + allowedByContentHash.size()
            );
            LOGGER.info("GuardMod shader pack scan completed. {}", snapshot.getStatusMessage());
        } catch (IOException exception) {
            snapshot = ShaderPackPolicySnapshot.failed("Shader pack scan failed: " + exception.getMessage());
            LOGGER.error("GuardMod failed to scan shader pack directories.", exception);
        }
    }

    public static ShaderPackPolicySnapshot getSnapshot() {
        return snapshot;
    }

    private static void ensureDirectories(Path serverRoot) {
        Path guardRoot = serverRoot.resolve("guardmod");
        Path allowedShaderPacks = guardRoot.resolve("allowed_shaderpacks");

        try {
            Files.createDirectories(guardRoot);
            Files.createDirectories(allowedShaderPacks);
        } catch (IOException exception) {
            LOGGER.error("GuardMod could not create shader pack directories under {}", serverRoot, exception);
        }
    }

    private static Path resolveServerRoot(Object server) {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        if (gameDir != null) {
            LOGGER.info("GuardMod resolved server root for shader packs from FMLPaths.GAMEDIR: {}", gameDir);
            return gameDir;
        }

        if (server instanceof MinecraftServer) {
            try {
                Path worldRoot = ((MinecraftServer) server).getWorldPath(LevelResource.ROOT);
                if (worldRoot != null) {
                    Path normalized = worldRoot.toAbsolutePath().normalize();
                    LOGGER.info("GuardMod resolved server root for shader packs from getWorldPath(LevelResource.ROOT): {}", normalized);
                    return normalized;
                }
            } catch (Exception exception) {
                LOGGER.error("GuardMod could not resolve the server root for shader packs from getWorldPath(LevelResource.ROOT).", exception);
            }
        }

        return null;
    }

    private static String normalizeHash(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
