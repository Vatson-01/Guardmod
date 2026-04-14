package com.guardmod.scanner;

import com.guardmod.config.GuardCommonConfig;
import com.guardmod.model.ContainedMod;
import com.guardmod.model.ExpectedModEntry;
import com.guardmod.model.ScannedModJar;
import com.guardmod.validate.ModPolicySnapshot;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModScanService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile ModPolicySnapshot snapshot = ModPolicySnapshot.failed("No mod scan has been performed yet");

    private ModScanService() {
    }

    public static void rescanFromServer(Object server) {
        Path serverRoot = resolveServerRoot(server);
        if (serverRoot == null) {
            snapshot = ModPolicySnapshot.failed("Unable to resolve server directory");
            LOGGER.error("GuardMod could not resolve the server directory for mod scanning.");
            return;
        }
        ensureGuardDirectories(serverRoot);

        Path modsDirectory = serverRoot.resolve("mods");
        Path requiredClientModsDirectory = serverRoot.resolve("guardmod").resolve("required_client_mods");
        Path allowedClientModsDirectory = serverRoot.resolve("guardmod").resolve("allowed_client_mods");

        try {
            List<ScannedModJar> requiredServerJars = ServerModsScanner.scan(modsDirectory);
            List<ScannedModJar> requiredClientJars = ServerModsScanner.scan(requiredClientModsDirectory);
            List<ScannedModJar> allowedJars = AllowedClientModsScanner.scan(allowedClientModsDirectory);

            Set<String> excludedModIds = parseExcludedModIds();
            Map<String, ExpectedModEntry> requiredMods = new LinkedHashMap<String, ExpectedModEntry>();

            addRequiredEntries(requiredMods, requiredServerJars, excludedModIds);
            addRequiredEntries(requiredMods, requiredClientJars, java.util.Collections.<String>emptySet());

            Map<String, ExpectedModEntry> allowedOptionalMods = buildAllowedMap(allowedJars, requiredMods);

            int requiredServerCount = countContainedMods(requiredServerJars, excludedModIds);
            int requiredClientOnlyCount = countContainedMods(requiredClientJars, java.util.Collections.<String>emptySet());

            snapshot = ModPolicySnapshot.ready(
                    requiredMods,
                    allowedOptionalMods,
                    "Required server mods: " + requiredServerCount
                            + ", required client-only mods: " + requiredClientOnlyCount
                            + ", total required mods: " + requiredMods.size()
                            + ", allowed optional client mods: " + allowedOptionalMods.size()
            );

            LOGGER.info("GuardMod scan completed. {}", snapshot.getStatusMessage());
        } catch (IOException exception) {
            snapshot = ModPolicySnapshot.failed("Mod scan failed: " + exception.getMessage());
            LOGGER.error("GuardMod failed to scan mod directories.", exception);
        }
    }

    public static ModPolicySnapshot getSnapshot() {
        return snapshot;
    }
    private static void ensureGuardDirectories(Path serverRoot) {
        Path guardRoot = serverRoot.resolve("guardmod");
        Path requiredClientMods = guardRoot.resolve("required_client_mods");
        Path allowedClientMods = guardRoot.resolve("allowed_client_mods");

        try {
            Files.createDirectories(guardRoot);
            Files.createDirectories(requiredClientMods);
            Files.createDirectories(allowedClientMods);
        } catch (Exception e) {
            LOGGER.error("GuardMod could not create required guard directories under {}", serverRoot, e);
        }
    }
    private static void addRequiredEntries(Map<String, ExpectedModEntry> target,
                                           List<ScannedModJar> jars,
                                           Set<String> excludedModIds) {
        for (ScannedModJar jar : jars) {
            for (ContainedMod containedMod : jar.getContainedMods()) {
                String normalizedModId = normalize(containedMod.getModId());
                if (normalizedModId.isEmpty() || excludedModIds.contains(normalizedModId)) {
                    continue;
                }

                target.put(normalizedModId, new ExpectedModEntry(
                        normalizedModId,
                        safe(containedMod.getVersion()),
                        safe(containedMod.getDisplayName()),
                        safe(jar.getSha256()),
                        safe(jar.getFileName()),
                        true
                ));
            }
        }
    }

    private static Map<String, ExpectedModEntry> buildAllowedMap(List<ScannedModJar> jars,
                                                                 Map<String, ExpectedModEntry> requiredMods) {
        Map<String, ExpectedModEntry> result = new LinkedHashMap<String, ExpectedModEntry>();

        for (ScannedModJar jar : jars) {
            for (ContainedMod containedMod : jar.getContainedMods()) {
                String normalizedModId = normalize(containedMod.getModId());
                if (normalizedModId.isEmpty() || requiredMods.containsKey(normalizedModId)) {
                    continue;
                }

                result.put(normalizedModId, new ExpectedModEntry(
                        normalizedModId,
                        safe(containedMod.getVersion()),
                        safe(containedMod.getDisplayName()),
                        safe(jar.getSha256()),
                        safe(jar.getFileName()),
                        false
                ));
            }
        }

        return result;
    }

    private static int countContainedMods(List<ScannedModJar> jars, Set<String> excludedModIds) {
        int count = 0;
        for (ScannedModJar jar : jars) {
            for (ContainedMod containedMod : jar.getContainedMods()) {
                String normalizedModId = normalize(containedMod.getModId());
                if (!normalizedModId.isEmpty() && !excludedModIds.contains(normalizedModId)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Set<String> parseExcludedModIds() {
        Set<String> result = new LinkedHashSet<String>();
        String raw = GuardCommonConfig.SERVER_REQUIRED_EXCLUDES.get();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }

        String[] parts = raw.split("[,;\\n\\r\\t ]+");
        for (String part : parts) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }

        return result;
    }

    private static Path resolveServerRoot(Object server) {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        if (gameDir != null) {
            LOGGER.info("GuardMod resolved server root from FMLPaths.GAMEDIR: {}", gameDir);
            return gameDir;
        }

        if (server instanceof MinecraftServer) {
            try {
                Path worldRoot = ((MinecraftServer) server).getWorldPath(LevelResource.ROOT);
                if (worldRoot != null) {
                    Path normalized = worldRoot.toAbsolutePath().normalize();
                    LOGGER.info("GuardMod resolved server root from getWorldPath(LevelResource.ROOT): {}", normalized);
                    return normalized;
                }
            } catch (Exception exception) {
                LOGGER.error("GuardMod could not resolve the server root from getWorldPath(LevelResource.ROOT).", exception);
            }
        }

        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}