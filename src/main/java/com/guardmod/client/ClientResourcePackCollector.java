package com.guardmod.client;

import com.guardmod.hash.FileHashing;
import com.guardmod.hash.FolderContentHasher;
import com.guardmod.hash.ZipContentHasher;
import com.guardmod.model.ClientResourcePackEntry;
import com.guardmod.util.PackReflectionHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.slf4j.Logger;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ClientResourcePackCollector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static String lastComputedSelectionFingerprint = null;
    private static Result lastComputedResult = null;
    private static String lastComputedInstalledFingerprint = null;
    private static java.util.List<ClientResourcePackEntry> lastComputedInstalledPacks = null;

    private ClientResourcePackCollector() {
    }

    public static Result collectActiveResourcePacks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return Result.empty();
        }

        PackRepository repository = minecraft.getResourcePackRepository();
        if (repository == null) {
            LOGGER.warn("GuardMod could not access the client PackRepository.");
            return Result.empty();
        }

        Collection<Pack> selectedPacks = repository.getSelectedPacks();
        if (selectedPacks == null || selectedPacks.isEmpty()) {
            lastComputedSelectionFingerprint = "";
            lastComputedResult = Result.empty();
            return lastComputedResult;
        }

        String selectionFingerprint = collectSelectionFingerprint();
        if (lastComputedSelectionFingerprint != null
                && lastComputedSelectionFingerprint.equals(selectionFingerprint)
                && lastComputedResult != null) {
            return lastComputedResult;
        }

        List<ClientResourcePackEntry> entries = new ArrayList<ClientResourcePackEntry>();
        List<String> order = new ArrayList<String>();

        for (Pack pack : selectedPacks) {
            if (pack == null) {
                continue;
            }

            if (shouldIgnorePack(pack)) {
                continue;
            }

            Path path = resolvePackPath(pack);
            if (path == null || !Files.exists(path)) {
                continue;
            }

            path = path.toAbsolutePath().normalize();
            String sourceType = resolveSourceType(path);
            if (sourceType.isEmpty()) {
                continue;
            }

            String displayName = resolveDisplayName(pack, path);
            String rawFileHash = "";
            String contentHash = "";

            try {
                if ("zip".equals(sourceType)) {
                    rawFileHash = FileHashing.sha256Hex(path);
                    contentHash = ZipContentHasher.sha256NormalizedZip(path);
                } else if ("folder".equals(sourceType)) {
                    contentHash = FolderContentHasher.sha256NormalizedFolder(path);
                }
            } catch (Exception exception) {
                LOGGER.warn("GuardMod could not hash active client resource pack {} at {}.", displayName, path, exception);
                continue;
            }

            if (contentHash.isEmpty()) {
                continue;
            }

            entries.add(new ClientResourcePackEntry(displayName, sourceType, rawFileHash, contentHash));
            order.add(displayName);
        }

        Result result = new Result(entries, order);
        lastComputedSelectionFingerprint = selectionFingerprint;
        lastComputedResult = result;
        return result;
    }
    public static List<ClientResourcePackEntry> collectInstalledExternalResourcePacks() {
        Path resourcepacksDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize().resolve("resourcepacks");
        if (!Files.isDirectory(resourcepacksDir)) {
            lastComputedInstalledFingerprint = "";
            lastComputedInstalledPacks = Collections.<ClientResourcePackEntry>emptyList();
            return lastComputedInstalledPacks;
        }

        String installedFingerprint = collectInstalledDirectoryFingerprint(resourcepacksDir);
        if (lastComputedInstalledFingerprint != null
                && lastComputedInstalledFingerprint.equals(installedFingerprint)
                && lastComputedInstalledPacks != null) {
            return lastComputedInstalledPacks;
        }

        java.util.List<Path> candidates = new java.util.ArrayList<Path>();
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(resourcepacksDir)) {
            for (Path entry : stream) {
                if (entry == null) {
                    continue;
                }
                candidates.add(entry.toAbsolutePath().normalize());
            }
        } catch (Exception exception) {
            LOGGER.warn("GuardMod could not enumerate installed resource packs in {}.", resourcepacksDir, exception);
            return Collections.<ClientResourcePackEntry>emptyList();
        }

        candidates.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));

        java.util.List<ClientResourcePackEntry> installed = new java.util.ArrayList<ClientResourcePackEntry>();

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

                installed.add(new ClientResourcePackEntry(displayName, sourceType, rawFileHash, contentHash));
            } catch (Exception exception) {
                LOGGER.warn("GuardMod could not hash installed client resource pack at {}.", path, exception);
            }
        }

        lastComputedInstalledFingerprint = installedFingerprint;
        lastComputedInstalledPacks = Collections.unmodifiableList(new java.util.ArrayList<ClientResourcePackEntry>(installed));
        return lastComputedInstalledPacks;
    }

    private static String collectInstalledDirectoryFingerprint(Path resourcepacksDir) {
        StringBuilder builder = new StringBuilder();

        java.util.List<Path> candidates = new java.util.ArrayList<Path>();
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(resourcepacksDir)) {
            for (Path entry : stream) {
                if (entry == null) {
                    continue;
                }
                candidates.add(entry.toAbsolutePath().normalize());
            }
        } catch (Exception exception) {
            LOGGER.warn("GuardMod could not build installed resource pack fingerprint for {}.", resourcepacksDir, exception);
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
    public static String collectSelectionFingerprint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return "";
        }

        PackRepository repository = minecraft.getResourcePackRepository();
        if (repository == null) {
            return "";
        }

        Collection<Pack> selectedPacks = repository.getSelectedPacks();
        if (selectedPacks == null || selectedPacks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Pack pack : selectedPacks) {
            if (pack == null || shouldIgnorePack(pack)) {
                continue;
            }

            String packId = safe(pack.getId());
            String title = "";
            try {
                if (pack.getTitle() != null) {
                    title = safe(pack.getTitle().getString());
                }
            } catch (Exception ignored) {
            }

            builder.append(packId).append('|').append(title).append('\n');
        }

        return builder.toString();
    }
    private static Path resolvePackPath(Pack pack) {
        Path directPath = PackReflectionHelper.findFirstPath(pack);
        if (directPath != null) {
            return directPath;
        }

        PackResources resources = null;
        try {
            resources = pack.open();
            Path resourcesPath = PackReflectionHelper.findFirstPath(resources);
            if (resourcesPath != null) {
                return resourcesPath;
            }
        } catch (Exception exception) {
            LOGGER.warn("GuardMod could not inspect PackResources for active pack {}.", safe(pack.getId()), exception);
        } finally {
            if (resources != null) {
                try {
                    resources.close();
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }
    private static boolean shouldIgnorePack(Pack pack) {
        String packId = "";
        try {
            packId = safe(pack.getId()).trim().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
        }

        if (packId.isEmpty()) {
            return false;
        }

        if ("vanilla".equals(packId)) {
            return true;
        }

        if ("mod_resources".equals(packId)) {
            return true;
        }

        if (packId.startsWith("builtin/")) {
            return true;
        }

        if (packId.startsWith("builtin:")) {
            return true;
        }

        return false;
    }
    private static String resolveSourceType(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (Files.isRegularFile(path)) {
            if (fileName.endsWith(".zip")) {
                return "zip";
            }
            return "";
        }

        if (Files.isDirectory(path) && Files.exists(path.resolve("pack.mcmeta"))) {
            return "folder";
        }

        return "";
    }

    private static String resolveDisplayName(Pack pack, Path path) {
        try {
            if (pack.getTitle() != null) {
                String title = pack.getTitle().getString();
                if (title != null && !title.trim().isEmpty()) {
                    return title.trim();
                }
            }
        } catch (Exception ignored) {
        }

        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (!fileName.isEmpty()) {
            return fileName;
        }

        return safe(pack.getId());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Result {
        private final List<ClientResourcePackEntry> activePacks;
        private final List<String> order;

        public Result(List<ClientResourcePackEntry> activePacks, List<String> order) {
            this.activePacks = activePacks == null
                    ? Collections.<ClientResourcePackEntry>emptyList()
                    : Collections.unmodifiableList(new ArrayList<ClientResourcePackEntry>(activePacks));
            this.order = order == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(order));
        }

        public static Result empty() {
            return new Result(Collections.<ClientResourcePackEntry>emptyList(), Collections.<String>emptyList());
        }

        public List<ClientResourcePackEntry> getActivePacks() {
            return activePacks;
        }

        public List<String> getOrder() {
            return order;
        }
    }
}