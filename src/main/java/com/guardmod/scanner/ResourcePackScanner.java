package com.guardmod.scanner;

import com.guardmod.hash.FileHashing;
import com.guardmod.hash.FolderContentHasher;
import com.guardmod.hash.ZipContentHasher;
import com.guardmod.model.ScannedResourcePack;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ResourcePackScanner {
    private ResourcePackScanner() {
    }

    public static List<ScannedResourcePack> scan(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory)) {
            return Collections.emptyList();
        }

        List<ScannedResourcePack> result = new ArrayList<ScannedResourcePack>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                ScannedResourcePack scanned = scanSingle(path);
                if (scanned != null) {
                    result.add(scanned);
                }
            }
        }

        result.sort(Comparator.comparing(ScannedResourcePack::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static ScannedResourcePack scanSingle(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return null;
        }

        Path normalized = path.toAbsolutePath().normalize();
        String fileName = normalized.getFileName() == null ? "" : normalized.getFileName().toString();

        if (Files.isRegularFile(normalized)) {
            String lowerName = fileName.toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".zip")) {
                return null;
            }

            String rawFileHash = FileHashing.sha256Hex(normalized);
            String contentHash = ZipContentHasher.sha256NormalizedZip(normalized);
            long size = Files.size(normalized);
            boolean hasPackMcmeta = zipContainsPackMcmeta(normalized);
            if (!hasPackMcmeta) {
                return null;
            }

            return new ScannedResourcePack(normalized, fileName, "zip", size, true, rawFileHash, contentHash);
        }

        if (Files.isDirectory(normalized)) {
            Path packMcmeta = normalized.resolve("pack.mcmeta");
            if (!Files.exists(packMcmeta) || !Files.isRegularFile(packMcmeta)) {
                return null;
            }

            String contentHash = FolderContentHasher.sha256NormalizedFolder(normalized);
            long size = countFolderSize(normalized);
            return new ScannedResourcePack(normalized, fileName, "folder", size, true, "", contentHash);
        }

        return null;
    }

    private static boolean zipContainsPackMcmeta(Path zipPath) throws IOException {
        java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile());
        try {
            return zipFile.getEntry("pack.mcmeta") != null;
        } finally {
            zipFile.close();
        }
    }

    private static long countFolderSize(Path root) throws IOException {
        final long[] total = new long[] {0L};
        Files.walk(root)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        total[0] += Files.size(path);
                    } catch (IOException ignored) {
                    }
                });
        return total[0];
    }
}
