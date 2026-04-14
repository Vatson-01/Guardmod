package com.guardmod.scanner;

import com.guardmod.hash.FileHashing;
import com.guardmod.model.ContainedMod;
import com.guardmod.model.ScannedModJar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ServerModsScanner {
    private ServerModsScanner() {
    }

    public static List<ScannedModJar> scan(Path modsDirectory) throws IOException {
        return scanDirectory(modsDirectory);
    }

    private static List<ScannedModJar> scanDirectory(Path directory) throws IOException {
        List<ScannedModJar> result = new ArrayList<ScannedModJar>();
        if (directory == null || !Files.isDirectory(directory)) {
            return result;
        }

        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> jarFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            for (Path jarFile : jarFiles) {
                String fileName = jarFile.getFileName().toString();
                long fileSize = Files.size(jarFile);
                String sha256 = FileHashing.sha256Hex(jarFile);
                List<ContainedMod> containedMods = ModsTomlReader.readContainedMods(jarFile);

                result.add(new ScannedModJar(jarFile, fileName, fileSize, sha256, containedMods));
            }
        }

        return result;
    }
}
