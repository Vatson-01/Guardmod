package com.settlements.data.model;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReconstructionSession {
    private static final int STORAGE_SIZE = 54;

    private final UUID id;
    private final UUID settlementId;
    private final UUID siegeId;
    private final UUID snapshotId;
    private final long createdAt;
    private boolean active;
    private final List<ReconstructionBlockEntry> entries;
    private final NonNullList<ItemStack> storedItems;

    public ReconstructionSession(
            UUID id,
            UUID settlementId,
            UUID siegeId,
            UUID snapshotId,
            long createdAt,
            boolean active,
            List<ReconstructionBlockEntry> entries,
            NonNullList<ItemStack> storedItems
    ) {
        this.id = id;
        this.settlementId = settlementId;
        this.siegeId = siegeId;
        this.snapshotId = snapshotId;
        this.createdAt = createdAt;
        this.active = active;
        this.entries = new ArrayList<ReconstructionBlockEntry>(entries);
        this.storedItems = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);

        int limit = Math.min(STORAGE_SIZE, storedItems.size());
        for (int i = 0; i < limit; i++) {
            this.storedItems.set(i, storedItems.get(i).copy());
        }
    }

    public static ReconstructionSession createNew(
            UUID settlementId,
            UUID siegeId,
            UUID snapshotId,
            long createdAt,
            List<ReconstructionBlockEntry> entries
    ) {
        return new ReconstructionSession(
                UUID.randomUUID(),
                settlementId,
                siegeId,
                snapshotId,
                createdAt,
                true,
                entries,
                NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY)
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public UUID getSiegeId() {
        return siegeId;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public List<ReconstructionBlockEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getStorageSize() {
        return STORAGE_SIZE;
    }

    public NonNullList<ItemStack> copyStoredItems() {
        NonNullList<ItemStack> copy = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < STORAGE_SIZE; i++) {
            copy.set(i, storedItems.get(i).copy());
        }
        return copy;
    }

    public void overwriteStoredItems(NonNullList<ItemStack> newItems) {
        for (int i = 0; i < STORAGE_SIZE; i++) {
            if (i < newItems.size()) {
                storedItems.set(i, newItems.get(i).copy());
            } else {
                storedItems.set(i, ItemStack.EMPTY);
            }
        }
    }

    public Map<String, Integer> getStoredResources() {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();

        for (ItemStack stack : storedItems) {
            if (stack.isEmpty()) {
                continue;
            }

            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            Integer current = result.get(itemId);
            if (current == null) {
                result.put(itemId, Integer.valueOf(stack.getCount()));
            } else {
                result.put(itemId, Integer.valueOf(current.intValue() + stack.getCount()));
            }
        }

        return Collections.unmodifiableMap(result);
    }

    public int getStoredResourceAmount(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (ItemStack stack : storedItems) {
            if (stack.isEmpty()) {
                continue;
            }

            String stackItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (itemId.equals(stackItemId)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    public void consumeStoredResource(String itemId, int amount) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("Нельзя списать пустой идентификатор ресурса.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Количество ресурса для списания должно быть больше нуля.");
        }

        int remaining = amount;
        for (int i = 0; i < storedItems.size(); i++) {
            ItemStack stack = storedItems.get(i);
            if (stack.isEmpty()) {
                continue;
            }

            String stackItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!itemId.equals(stackItemId)) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            if (stack.isEmpty()) {
                storedItems.set(i, ItemStack.EMPTY);
            }

            remaining -= taken;
            if (remaining <= 0) {
                return;
            }
        }

        throw new IllegalStateException("Недостаточно ресурса для списания: " + itemId);
    }

    public int countPendingEntries() {
        int count = 0;
        for (ReconstructionBlockEntry entry : entries) {
            if (entry.isPending()) {
                count++;
            }
        }
        return count;
    }

    public int countSkippedEntries() {
        int count = 0;
        for (ReconstructionBlockEntry entry : entries) {
            if (entry.isSkipped()) {
                count++;
            }
        }
        return count;
    }

    public int countRestoredEntries() {
        int count = 0;
        for (ReconstructionBlockEntry entry : entries) {
            if (entry.isRestored()) {
                count++;
            }
        }
        return count;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("SettlementId", settlementId);
        tag.putUUID("SiegeId", siegeId);
        tag.putUUID("SnapshotId", snapshotId);
        tag.putLong("CreatedAt", createdAt);
        tag.putBoolean("Active", active);

        ListTag entriesTag = new ListTag();
        for (ReconstructionBlockEntry entry : entries) {
            entriesTag.add(entry.save());
        }
        tag.put("Entries", entriesTag);

        CompoundTag storageTag = new CompoundTag();
        ContainerHelper.saveAllItems(storageTag, storedItems);
        tag.put("StoredItems", storageTag);

        return tag;
    }

    public static ReconstructionSession load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID settlementId = tag.getUUID("SettlementId");
        UUID siegeId = tag.getUUID("SiegeId");
        UUID snapshotId = tag.getUUID("SnapshotId");
        long createdAt = tag.getLong("CreatedAt");
        boolean active = tag.getBoolean("Active");

        List<ReconstructionBlockEntry> entries = new ArrayList<ReconstructionBlockEntry>();
        if (tag.contains("Entries", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                entries.add(ReconstructionBlockEntry.load(listTag.getCompound(i)));
            }
        }

        NonNullList<ItemStack> storedItems = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        if (tag.contains("StoredItems", Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(tag.getCompound("StoredItems"), storedItems);
        }

        return new ReconstructionSession(
                id,
                settlementId,
                siegeId,
                snapshotId,
                createdAt,
                active,
                entries,
                storedItems
        );
    }
}