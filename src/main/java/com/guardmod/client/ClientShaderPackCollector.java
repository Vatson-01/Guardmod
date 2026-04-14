package com.guardmod.client;

import com.guardmod.hash.FileHashing;
import com.guardmod.hash.FolderContentHasher;
import com.guardmod.hash.ZipContentHasher;
import com.guardmod.model.ClientShaderPackEntry;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientShaderPackCollector {
    private static String lastComputedInstalledFingerprint = null;
    private static List<ClientShaderPackEntry> lastComputedInstalledPacks = null;

    private ClientShaderPackCollector() {
    }

    public static Result collectShaderPackState() {
        List<ClientShaderPackEntry> installed = collectInstalledExternalShaderPacks();
        String runtime = detectRuntime();
        ClientShaderPackEntry active = detectActiveShaderPack(runtime, installed);
        return new Result(runtime, active, installed);
    }

    public static List<ClientShaderPackEntry> collectInstalledExternalShaderPacks() {
        Path shaderpacksDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize().resolve("shaderpacks");
        if (!Files.isDirectory(shaderpacksDir)) {
            lastComputedInstalledFingerprint = "";
            lastComputedInstalledPacks = Collections.<ClientShaderPackEntry>emptyList();
            return lastComputedInstalledPacks;
        }

        String fingerprint = collectInstalledDirectoryFingerprint(shaderpacksDir);
        if (lastComputedInstalledFingerprint != null
                && lastComputedInstalledFingerprint.equals(fingerprint)
                && lastComputedInstalledPacks != null) {
            return lastComputedInstalledPacks;
        }

        List<Path> candidates = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(shaderpacksDir)) {
            for (Path entry : stream) {
                if (entry != null) {
                    candidates.add(entry.toAbsolutePath().normalize());
                }
            }
        } catch (Exception exception) {
            return Collections.<ClientShaderPackEntry>emptyList();
        }

        candidates.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));

        List<ClientShaderPackEntry> installed = new ArrayList<ClientShaderPackEntry>();

        for (Path path : candidates) {
            try {
                String sourceType = resolveSourceType(path);
                if (sourceType.isEmpty()) {
                    continue;
                }

                String displayName = path.getFileName() == null ? "" : path.getFileName().toString();
                String rawFileHash = "";
                String contentHash = "";

                if ("zip".equals(sourceType)) {
                    rawFileHash = FileHashing.sha256Hex(path);
                    contentHash = ZipContentHasher.sha256NormalizedZip(path);
                } else if ("folder".equals(sourceType)) {
                    contentHash = FolderContentHasher.sha256NormalizedFolder(path);
                }

                if (contentHash.isEmpty()) {
                    continue;
                }

                installed.add(new ClientShaderPackEntry(displayName, sourceType, rawFileHash, contentHash));
            } catch (Exception ignored) {
            }
        }

        lastComputedInstalledFingerprint = fingerprint;
        lastComputedInstalledPacks = Collections.unmodifiableList(new ArrayList<ClientShaderPackEntry>(installed));
        return lastComputedInstalledPacks;
    }

    private static String collectInstalledDirectoryFingerprint(Path shaderpacksDir) {
        StringBuilder builder = new StringBuilder();

        List<Path> candidates = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(shaderpacksDir)) {
            for (Path entry : stream) {
                if (entry != null) {
                    candidates.add(entry.toAbsolutePath().normalize());
                }
            }
        } catch (Exception exception) {
            return "";
        }

        candidates.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));

        for (Path path : candidates) {
            try {
                if (Files.isRegularFile(path)) {
                    builder.append(path.getFileName()).append('|')
                            .append(Files.size(path)).append('|')
                            .append(Files.getLastModifiedTime(path).toMillis()).append('\n');
                } else if (Files.isDirectory(path)) {
                    builder.append(path.getFileName()).append('|')
                            .append("dir").append('|')
                            .append(Files.getLastModifiedTime(path).toMillis()).append('\n');
                }
            } catch (Exception ignored) {
            }
        }

        return builder.toString();
    }

    private static String detectRuntime() {
        if (findIrisClass() != null) {
            return "OCULUS";
        }
        return "NONE";
    }

    private static ClientShaderPackEntry detectActiveShaderPack(String runtime, List<ClientShaderPackEntry> installed) {
        if (!"OCULUS".equals(runtime)) {
            return null;
        }

        String currentPackName = resolveCurrentPackNameFromIris();
        if (currentPackName == null) {
            return null;
        }

        String normalized = currentPackName.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if ("off".equals(lower) || "(off)".equals(lower) || "disabled".equals(lower) || lower.contains("internal")) {
            return null;
        }

        if (resolveIsFallbackFromIris()) {
            return null;
        }

        for (ClientShaderPackEntry entry : installed) {
            if (matchesPackName(entry.getDisplayName(), normalized)) {
                return entry;
            }
        }

        Path shaderpacksDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize().resolve("shaderpacks");
        Path direct = shaderpacksDir.resolve(normalized);
        Path zip = shaderpacksDir.resolve(normalized + ".zip");
        Path selected = Files.exists(direct) ? direct : (Files.exists(zip) ? zip : null);
        if (selected == null) {
            return null;
        }

        try {
            String sourceType = resolveSourceType(selected);
            if (sourceType.isEmpty()) {
                return null;
            }

            String rawFileHash = "";
            String contentHash = "";
            if ("zip".equals(sourceType)) {
                rawFileHash = FileHashing.sha256Hex(selected);
                contentHash = ZipContentHasher.sha256NormalizedZip(selected);
            } else if ("folder".equals(sourceType)) {
                contentHash = FolderContentHasher.sha256NormalizedFolder(selected);
            }

            if (contentHash.isEmpty()) {
                return null;
            }

            return new ClientShaderPackEntry(
                    selected.getFileName() == null ? normalized : selected.getFileName().toString(),
                    sourceType,
                    rawFileHash,
                    contentHash
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean matchesPackName(String entryName, String currentPackName) {
        String a = safe(entryName).trim();
        String b = safe(currentPackName).trim();
        if (a.equalsIgnoreCase(b)) {
            return true;
        }

        String aNoZip = a.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") ? a.substring(0, a.length() - 4) : a;
        String bNoZip = b.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") ? b.substring(0, b.length() - 4) : b;
        return aNoZip.equalsIgnoreCase(bNoZip);
    }

    private static Class<?> findIrisClass() {
        String[] candidates = new String[] {
                "net.coderbot.iris.Iris",
                "net.irisshaders.iris.Iris"
        };

        for (String candidate : candidates) {
            try {
                return Class.forName(candidate);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static String resolveCurrentPackNameFromIris() {
        Class<?> irisClass = findIrisClass();
        if (irisClass == null) {
            return null;
        }

        try {
            java.lang.reflect.Method method = irisClass.getMethod("getCurrentPackName");
            Object value = method.invoke(null);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean resolveIsFallbackFromIris() {
        Class<?> irisClass = findIrisClass();
        if (irisClass == null) {
            return false;
        }

        try {
            java.lang.reflect.Method method = irisClass.getMethod("isFallback");
            Object value = method.invoke(null);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String resolveSourceType(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (Files.isRegularFile(path)) {
            return fileName.endsWith(".zip") ? "zip" : "";
        }

        if (Files.isDirectory(path) && Files.isDirectory(path.resolve("shaders"))) {
            return "folder";
        }

        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Result {
        private final String runtime;
        private final ClientShaderPackEntry activeShaderPack;
        private final List<ClientShaderPackEntry> installedShaderPacks;

        public Result(String runtime,
                      ClientShaderPackEntry activeShaderPack,
                      List<ClientShaderPackEntry> installedShaderPacks) {
            this.runtime = runtime == null ? "" : runtime;
            this.activeShaderPack = activeShaderPack;
            this.installedShaderPacks = installedShaderPacks == null
                    ? Collections.<ClientShaderPackEntry>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ClientShaderPackEntry>(installedShaderPacks));
        }

        public String getRuntime() {
            return runtime;
        }

        public ClientShaderPackEntry getActiveShaderPack() {
            return activeShaderPack;
        }

        public List<ClientShaderPackEntry> getInstalledShaderPacks() {
            return installedShaderPacks;
        }
    }
}
