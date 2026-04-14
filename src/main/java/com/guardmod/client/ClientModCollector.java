package com.guardmod.client;

import com.guardmod.hash.FileHashing;
import com.guardmod.model.ClientModEntry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientModCollector {
    private ClientModCollector() {
    }

    public static List<ClientModEntry> collectLoadedMods() {
        List<ClientModEntry> result = new ArrayList<ClientModEntry>();
        Map<Path, LocalFileInfo> fileCache = new HashMap<Path, LocalFileInfo>();

        for (IModInfo modInfo : ModList.get().getMods()) {
            String modId = safe(modInfo.getModId());
            String version = modInfo.getVersion() == null ? "" : safe(modInfo.getVersion().toString());

            Path owningFilePath = resolveOwningFilePath(modInfo);
            LocalFileInfo fileInfo = resolveLocalFileInfo(fileCache, owningFilePath);

            result.add(new ClientModEntry(modId, version, fileInfo.jarHash, fileInfo.fileName));
        }

        Collections.sort(result, Comparator.comparing(ClientModEntry::getModId, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static LocalFileInfo resolveLocalFileInfo(Map<Path, LocalFileInfo> fileCache, Path owningFilePath) {
        if (owningFilePath == null) {
            return LocalFileInfo.EMPTY;
        }

        LocalFileInfo cached = fileCache.get(owningFilePath);
        if (cached != null) {
            return cached;
        }

        String fileName = owningFilePath.getFileName() == null ? "" : owningFilePath.getFileName().toString();
        String jarHash = FileHashing.sha256HexOrEmpty(owningFilePath);

        LocalFileInfo resolved = new LocalFileInfo(fileName, jarHash);
        fileCache.put(owningFilePath, resolved);
        return resolved;
    }

    private static Path resolveOwningFilePath(IModInfo modInfo) {
        try {
            Object owningFile = modInfo.getOwningFile();
            if (owningFile == null) {
                return null;
            }

            Object modFile = owningFile.getClass().getMethod("getFile").invoke(owningFile);
            if (modFile == null) {
                return null;
            }

            Object filePath = modFile.getClass().getMethod("getFilePath").invoke(modFile);
            if (filePath instanceof Path) {
                return (Path) filePath;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class LocalFileInfo {
        private static final LocalFileInfo EMPTY = new LocalFileInfo("", "");

        private final String fileName;
        private final String jarHash;

        private LocalFileInfo(String fileName, String jarHash) {
            this.fileName = fileName == null ? "" : fileName;
            this.jarHash = jarHash == null ? "" : jarHash;
        }
    }
}