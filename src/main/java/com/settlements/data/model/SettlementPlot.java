package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SettlementPlot {
    private final UUID id;
    private final UUID settlementId;

    private UUID ownerUuid;

    private final Set<String> chunkKeys = new LinkedHashSet<>();
    private final Map<UUID, PlotPermissionSet> accessByPlayer = new LinkedHashMap<>();

    private long createdAt;
    private long updatedAt;

    public SettlementPlot(UUID id, UUID settlementId, UUID ownerUuid, long createdAt) {
        this.id = id;
        this.settlementId = settlementId;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static SettlementPlot createNew(UUID settlementId, UUID ownerUuid, long createdAt) {
        return new SettlementPlot(UUID.randomUUID(), settlementId, ownerUuid, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Set<String> getChunkKeys() {
        return Collections.unmodifiableSet(chunkKeys);
    }

    public Map<UUID, PlotPermissionSet> getAccessByPlayer() {
        return Collections.unmodifiableMap(accessByPlayer);
    }

    public boolean isOwner(UUID playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    public boolean containsChunkKey(String chunkKey) {
        return chunkKeys.contains(chunkKey);
    }

    public void addChunkKey(String chunkKey, long gameTime) {
        if (chunkKeys.add(chunkKey)) {
            touch(gameTime);
        }
    }

    public void removeChunkKey(String chunkKey, long gameTime) {
        if (chunkKeys.remove(chunkKey)) {
            touch(gameTime);
        }
    }

    public boolean isEmpty() {
        return chunkKeys.isEmpty();
    }

    public void grantPermission(UUID playerUuid, PlotPermission permission, long gameTime) {
        PlotPermissionSet set = accessByPlayer.computeIfAbsent(playerUuid, uuid -> new PlotPermissionSet());
        set.grant(permission);
        touch(gameTime);
    }

    public void revokePermission(UUID playerUuid, PlotPermission permission, long gameTime) {
        PlotPermissionSet set = accessByPlayer.get(playerUuid);
        if (set == null) {
            return;
        }

        set.revoke(permission);

        if (set.asReadOnlySet().isEmpty()) {
            accessByPlayer.remove(playerUuid);
        }

        touch(gameTime);
    }

    public boolean hasPermission(UUID playerUuid, PlotPermission permission) {
        PlotPermissionSet set = accessByPlayer.get(playerUuid);
        return set != null && set.has(permission);
    }

    public void transferOwnership(UUID newOwnerUuid, boolean clearAccessRules, long gameTime) {
        this.ownerUuid = newOwnerUuid;

        if (clearAccessRules) {
            this.accessByPlayer.clear();
        }

        touch(gameTime);
    }

    public void mergeFrom(SettlementPlot otherPlot, boolean clearForeignAccessRules, long gameTime) {
        if (otherPlot == null) {
            return;
        }

        this.chunkKeys.addAll(otherPlot.chunkKeys);

        if (!clearForeignAccessRules) {
            this.accessByPlayer.putAll(otherPlot.accessByPlayer);
        }

        touch(gameTime);
    }

    private void touch(long gameTime) {
        this.updatedAt = gameTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("Id", id);
        tag.putUUID("SettlementId", settlementId);
        tag.putUUID("OwnerUuid", ownerUuid);
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("UpdatedAt", updatedAt);

        ListTag chunksTag = new ListTag();
        for (String chunkKey : chunkKeys) {
            chunksTag.add(StringTag.valueOf(chunkKey));
        }
        tag.put("ChunkKeys", chunksTag);

        ListTag accessList = new ListTag();
        for (Map.Entry<UUID, PlotPermissionSet> entry : accessByPlayer.entrySet()) {
            CompoundTag accessTag = new CompoundTag();
            accessTag.putUUID("PlayerUuid", entry.getKey());
            accessTag.put("PermissionSet", entry.getValue().save());
            accessList.add(accessTag);
        }
        tag.put("AccessByPlayer", accessList);

        return tag;
    }

    public static SettlementPlot load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID settlementId = tag.getUUID("SettlementId");
        UUID ownerUuid = tag.getUUID("OwnerUuid");
        long createdAt = tag.getLong("CreatedAt");

        SettlementPlot plot = new SettlementPlot(id, settlementId, ownerUuid, createdAt);
        plot.updatedAt = tag.getLong("UpdatedAt");

        if (tag.contains("ChunkKeys", Tag.TAG_LIST)) {
            ListTag chunksTag = tag.getList("ChunkKeys", Tag.TAG_STRING);
            for (int i = 0; i < chunksTag.size(); i++) {
                plot.chunkKeys.add(chunksTag.getString(i));
            }
        }

        if (tag.contains("AccessByPlayer", Tag.TAG_LIST)) {
            ListTag accessList = tag.getList("AccessByPlayer", Tag.TAG_COMPOUND);
            for (int i = 0; i < accessList.size(); i++) {
                CompoundTag accessTag = accessList.getCompound(i);
                UUID playerUuid = accessTag.getUUID("PlayerUuid");
                PlotPermissionSet set = PlotPermissionSet.load(accessTag.getCompound("PermissionSet"));
                plot.accessByPlayer.put(playerUuid, set);
            }
        }

        return plot;
    }
}