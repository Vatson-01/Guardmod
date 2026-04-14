package com.guardmod.scanner;

import com.guardmod.model.ContainedMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ModsTomlReader {
    private ModsTomlReader() {
    }

    public static List<ContainedMod> readContainedMods(Path jarPath) throws IOException {
        List<ContainedMod> result = new ArrayList<ContainedMod>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry("META-INF/mods.toml");
            if (entry == null) {
                return result;
            }

            try (InputStream inputStream = jarFile.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                boolean inModsBlock = false;
                String currentModId = null;
                String currentVersion = null;
                String currentDisplayName = null;

                while ((line = reader.readLine()) != null) {
                    String cleaned = stripComments(line).trim();
                    if (cleaned.isEmpty()) {
                        continue;
                    }

                    if ("[[mods]]".equals(cleaned)) {
                        if (inModsBlock) {
                            addCurrent(result, currentModId, currentVersion, currentDisplayName);
                        }
                        inModsBlock = true;
                        currentModId = null;
                        currentVersion = null;
                        currentDisplayName = null;
                        continue;
                    }

                    if (cleaned.startsWith("[[") || (cleaned.startsWith("[") && !cleaned.startsWith("[["))) {
                        if (inModsBlock) {
                            addCurrent(result, currentModId, currentVersion, currentDisplayName);
                            inModsBlock = false;
                            currentModId = null;
                            currentVersion = null;
                            currentDisplayName = null;
                        }
                        continue;
                    }

                    if (!inModsBlock) {
                        continue;
                    }

                    int equalsIndex = cleaned.indexOf('=');
                    if (equalsIndex <= 0 || equalsIndex >= cleaned.length() - 1) {
                        continue;
                    }

                    String key = cleaned.substring(0, equalsIndex).trim();
                    String value = unquote(cleaned.substring(equalsIndex + 1).trim());

                    if ("modId".equals(key)) {
                        currentModId = value;
                    } else if ("version".equals(key)) {
                        currentVersion = value;
                    } else if ("displayName".equals(key)) {
                        currentDisplayName = value;
                    }
                }

                if (inModsBlock) {
                    addCurrent(result, currentModId, currentVersion, currentDisplayName);
                }
            }
        }

        return result;
    }

    private static void addCurrent(List<ContainedMod> result, String modId, String version, String displayName) {
        if (modId == null || modId.trim().isEmpty()) {
            return;
        }
        result.add(new ContainedMod(modId.trim(), version == null ? "" : version.trim(), displayName == null ? "" : displayName.trim()));
    }

    private static String stripComments(String line) {
        StringBuilder builder = new StringBuilder(line.length());
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);

            if ((current == '"' || current == '\'') && (i == 0 || line.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = current;
                } else if (quoteChar == current) {
                    inQuotes = false;
                    quoteChar = 0;
                }
            }

            if (!inQuotes && current == '#') {
                break;
            }

            builder.append(current);
        }

        return builder.toString();
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
