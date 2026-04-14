package com.guardmod.scanner;

import com.guardmod.hash.FileHashing;
import com.guardmod.hash.FolderContentHasher;
import com.guardmod.hash.ZipContentHasher;
import com.guardmod.model.ScannedShaderPack;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ShaderPackScanner {
    private ShaderPackScanner() {
    }

    public static List<ScannedShaderPack> scan(Path directory) throws IOException {
        List<ScannedShaderPack> result = new ArrayList<ScannedShaderPack>();
        if (directory == null || !Files.isDirectory(directory)) {
            return result;
        }

        List<Path> candidates = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (entry != null) {
                    candidates.add(entry.toAbsolutePath().normalize());
                }
            }
        }

        candidates.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));

        for (Path path : candidates) {
            String sourceType = resolveSourceType(path);
            if (sourceType.isEmpty()) {
                continue;
            }

            String rawFileHash = "";
            String contentHash = "";
            long size = Files.isRegularFile(path) ? Files.size(path) : 0L;
            boolean hasShadersDirectory = Files.isDirectory(path) && Files.isDirectory(path.resolve("shaders"));

            if ("zip".equals(sourceType)) {
                rawFileHash = FileHashing.sha256Hex(path);
                contentHash = ZipContentHasher.sha256NormalizedZip(path);
            } else if ("folder".equals(sourceType)) {
                contentHash = FolderContentHasher.sha256NormalizedFolder(path);
            }

            if (contentHash.isEmpty()) {
                continue;
            }

            result.add(new ScannedShaderPack(
                    path,
                    path.getFileName() == null ? "" : path.getFileName().toString(),
                    sourceType,
                    size,
                    hasShadersDirectory,
                    rawFileHash,
                    contentHash
            ));
        }

        return result;
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
}
