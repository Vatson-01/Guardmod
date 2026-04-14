package com.guardmod.hash;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipContentHasher {
    private ZipContentHasher() {
    }

    public static String sha256NormalizedZip(Path zipFilePath) throws IOException {
        MessageDigest digest = newDigest();
        List<String> entryNames = new ArrayList<String>();

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String normalizedName = normalizeEntryName(entry.getName());
                if (shouldIgnore(normalizedName)) {
                    continue;
                }

                entryNames.add(normalizedName);
            }

            Collections.sort(entryNames);

            byte[] buffer = new byte[8192];
            for (String entryName : entryNames) {
                ZipEntry entry = zipFile.getEntry(entryName);
                if (entry == null) {
                    entry = findEntryCaseSensitive(zipFile, entryName);
                }
                if (entry == null) {
                    continue;
                }

                updateString(digest, entryName);
                updateZero(digest);

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    int read;
                    while ((read = inputStream.read(buffer)) >= 0) {
                        if (read > 0) {
                            digest.update(buffer, 0, read);
                        }
                    }
                }

                updateZero(digest);
            }
        }

        return "sha256:" + toHex(digest.digest());
    }

    private static ZipEntry findEntryCaseSensitive(ZipFile zipFile, String entryName) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && normalizeEntryName(entry.getName()).equals(entryName)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean shouldIgnore(String name) {
        return name.isEmpty()
                || name.startsWith("__MACOSX/")
                || name.endsWith("/.DS_Store")
                || ".DS_Store".equals(name)
                || name.endsWith("/Thumbs.db")
                || "Thumbs.db".equals(name);
    }

    private static String normalizeEntryName(String value) {
        return value == null ? "" : value.replace('\\', '/');
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void updateString(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void updateZero(MessageDigest digest) {
        digest.update((byte) 0);
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
