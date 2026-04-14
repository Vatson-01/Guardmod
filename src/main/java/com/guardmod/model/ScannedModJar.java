package com.guardmod.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScannedModJar {
    private final Path file;
    private final String fileName;
    private final long fileSize;
    private final String sha256;
    private final List<ContainedMod> containedMods;

    public ScannedModJar(Path file, String fileName, long fileSize, String sha256, List<ContainedMod> containedMods) {
        this.file = file;
        this.fileName = fileName == null ? "" : fileName;
        this.fileSize = fileSize;
        this.sha256 = sha256 == null ? "" : sha256;
        this.containedMods = containedMods == null ? Collections.<ContainedMod>emptyList() : new ArrayList<ContainedMod>(containedMods);
    }

    public Path getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSha256() {
        return sha256;
    }

    public List<ContainedMod> getContainedMods() {
        return Collections.unmodifiableList(containedMods);
    }
}
