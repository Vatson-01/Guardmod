package com.guardmod.scanner;

import com.guardmod.model.ScannedResourcePack;
import com.guardmod.validate.ResourcePackPolicySnapshot;
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

public final class ResourcePackScanService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile ResourcePackPolicySnapshot snapshot = ResourcePackPolicySnapshot.failed("No resource pack scan has been performed yet");

    private ResourcePackScanService() {
    }

    public static void rescanFromServer(Object server) {
        Path serverRoot = resolveServerRoot(server);
        if (serverRoot == null) {
            snapshot = ResourcePackPolicySnapshot.failed("Unable to resolve server directory for resource packs");
            LOGGER.error("GuardMod could not resolve the server directory for resource pack scanning.");
            return;
        }

        ensureDirectories(serverRoot);

        Path allowedResourcePacksDirectory = serverRoot.resolve("guardmod").resolve("allowed_resourcepacks");
        try {
            List<ScannedResourcePack> scannedPacks = ResourcePackScanner.scan(allowedResourcePacksDirectory);
            Map<String, ScannedResourcePack> allowedByContentHash = new LinkedHashMap<String, ScannedResourcePack>();

            for (ScannedResourcePack pack : scannedPacks) {
                String normalizedHash = normalizeHash(pack.getContentHash());
                if (!normalizedHash.isEmpty()) {
                    allowedByContentHash.put(normalizedHash, pack);
                }
            }

            snapshot = ResourcePackPolicySnapshot.ready(
                    allowedByContentHash,
                    "Allowed resource packs: " + allowedByContentHash.size()
            );
            LOGGER.info("GuardMod resource pack scan completed. {}", snapshot.getStatusMessage());
        } catch (IOException exception) {
            snapshot = ResourcePackPolicySnapshot.failed("Resource pack scan failed: " + exception.getMessage());
            LOGGER.error("GuardMod failed to scan resource pack directories.", exception);
        }
    }

    public static ResourcePackPolicySnapshot getSnapshot() {
        return snapshot;
    }

    private static void ensureDirectories(Path serverRoot) {
        Path guardRoot = serverRoot.resolve("guardmod");
        Path allowedResourcePacks = guardRoot.resolve("allowed_resourcepacks");

        try {
            Files.createDirectories(guardRoot);
            Files.createDirectories(allowedResourcePacks);
        } catch (IOException exception) {
            LOGGER.error("GuardMod could not create resource pack directories under {}", serverRoot, exception);
        }
    }

    private static Path resolveServerRoot(Object server) {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        if (gameDir != null) {
            LOGGER.info("GuardMod resolved server root for resource packs from FMLPaths.GAMEDIR: {}", gameDir);
            return gameDir;
        }

        if (server instanceof MinecraftServer) {
            try {
                Path worldRoot = ((MinecraftServer) server).getWorldPath(LevelResource.ROOT);
                if (worldRoot != null) {
                    Path normalized = worldRoot.toAbsolutePath().normalize();
                    LOGGER.info("GuardMod resolved server root for resource packs from getWorldPath(LevelResource.ROOT): {}", normalized);
                    return normalized;
                }
            } catch (Exception exception) {
                LOGGER.error("GuardMod could not resolve the server root for resource packs from getWorldPath(LevelResource.ROOT).", exception);
            }
        }

        return null;
    }

    private static String normalizeHash(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
