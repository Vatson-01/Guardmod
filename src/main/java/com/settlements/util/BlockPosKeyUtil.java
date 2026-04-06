package com.settlements.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class BlockPosKeyUtil {
    private BlockPosKeyUtil() {
    }

    public static String toKey(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }
}