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

public final class AllowedClientModsScanner {
    private AllowedClientModsScanner() {
    }

    public static List<ScannedModJar> scan(Path allowedClientModsDirectory) throws IOException {
        List<ScannedModJar> result = new ArrayList<ScannedModJar>();
        if (allowedClientModsDirectory == null || !Files.isDirectory(allowedClientModsDirectory)) {
            return result;
        }

        try (Stream<Path> stream = Files.list(allowedClientModsDirectory)) {
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
