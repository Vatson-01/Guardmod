package com.settlements.data.model;

import com.settlements.util.ClaimKeyUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class SettlementChunkClaim {
    private final UUID settlementId;
    private final ResourceLocation dimensionId;
    private final int chunkX;
    private final int chunkZ;

    public SettlementChunkClaim(UUID settlementId, ResourceLocation dimensionId, int chunkX, int chunkZ) {
        this.settlementId = settlementId;
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public ResourceLocation getDimensionId() {
        return dimensionId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }

    public String getChunkKey() {
        return ClaimKeyUtil.toKey(
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                new ChunkPos(chunkX, chunkZ)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("SettlementId", settlementId);
        tag.putString("DimensionId", dimensionId.toString());
        tag.putInt("ChunkX", chunkX);
        tag.putInt("ChunkZ", chunkZ);
        return tag;
    }

    public static SettlementChunkClaim load(CompoundTag tag) {
        UUID settlementId = tag.getUUID("SettlementId");
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("DimensionId"));
        if (dimensionId == null) {
            dimensionId = Level.OVERWORLD.location();
        }

        int chunkX = tag.getInt("ChunkX");
        int chunkZ = tag.getInt("ChunkZ");

        return new SettlementChunkClaim(settlementId, dimensionId, chunkX, chunkZ);
    }
}