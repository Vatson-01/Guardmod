package com.guardmod.hash;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FolderContentHasher {
    private FolderContentHasher() {
    }

    public static String sha256NormalizedFolder(Path root) throws IOException {
        MessageDigest digest = newDigest();
        List<Path> files = new ArrayList<Path>();

        Files.walk(root)
                .filter(Files::isRegularFile)
                .forEach(files::add);

        Collections.sort(files);

        byte[] buffer = new byte[8192];
        for (Path file : files) {
            String relativePath = normalizeRelativePath(root.relativize(file));
            if (shouldIgnore(relativePath)) {
                continue;
            }

            digest.update(relativePath.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);

            try (InputStream inputStream = Files.newInputStream(file)) {
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }

            digest.update((byte) 0);
        }

        return "sha256:" + toHex(digest.digest());
    }

    private static boolean shouldIgnore(String name) {
        return name.isEmpty()
                || name.startsWith("__MACOSX/")
                || name.endsWith("/.DS_Store")
                || ".DS_Store".equals(name)
                || name.endsWith("/Thumbs.db")
                || "Thumbs.db".equals(name);
    }

    private static String normalizeRelativePath(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}
