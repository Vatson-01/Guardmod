package com.settlements.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ClaimKeyUtil {
    private ClaimKeyUtil() {
    }

    public static String toKey(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return dimension.location() + "|" + chunkPos.x + "|" + chunkPos.z;
    }
}