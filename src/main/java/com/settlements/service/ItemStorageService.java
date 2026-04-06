package com.settlements.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemStorageService {
    private ItemStorageService() {
    }

    public static int countPlayerItem(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;

        total += countItemInList(inventory.items, item);
        total += countItemInList(inventory.offhand, item);
        total += countItemInList(inventory.armor, item);

        return total;
    }

    public static boolean removePlayerItem(ServerPlayer player, Item item, int count) {
        if (count <= 0) {
            return true;
        }

        if (countPlayerItem(player, item) < count) {
            return false;
        }

        Inventory inventory = player.getInventory();

        count = removeItemFromList(inventory.items, item, count);
        count = removeItemFromList(inventory.offhand, item, count);
        count = removeItemFromList(inventory.armor, item, count);

        inventory.setChanged();
        return count == 0;
    }

    public static boolean canPlayerFit(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        int remaining = stack.getCount();
        List<ItemStack> items = player.getInventory().items;

        for (ItemStack slotStack : items) {
            if (slotStack.isEmpty()) {
                remaining -= Math.min(remaining, stack.getMaxStackSize());
            } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                int free = Math.max(0, slotStack.getMaxStackSize() - slotStack.getCount());
                remaining -= Math.min(remaining, free);
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return remaining <= 0;
    }

    public static void addItemToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        boolean added = player.getInventory().add(remaining);

        if (!added && !remaining.isEmpty()) {
            player.drop(remaining, false);
        }

        player.getInventory().setChanged();
    }

    public static int countContainerItem(SimpleContainer container, Item item) {
        int total = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }

        return total;
    }

    public static boolean removeContainerItem(SimpleContainer container, Item item, int count) {
        if (count <= 0) {
            return true;
        }

        if (countContainerItem(container, item) < count) {
            return false;
        }

        for (int i = 0; i < container.getContainerSize() && count > 0; i++) {
            ItemStack stack = container.getItem(i);

            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }

            int taken = Math.min(count, stack.getCount());
            stack.shrink(taken);
            count -= taken;

            if (stack.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
            }
        }

        container.setChanged();
        return count == 0;
    }

    public static boolean canContainerFit(SimpleContainer container, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        int remaining = stack.getCount();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);

            if (slotStack.isEmpty()) {
                remaining -= Math.min(remaining, stack.getMaxStackSize());
            } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                int free = Math.max(0, slotStack.getMaxStackSize() - slotStack.getCount());
                remaining -= Math.min(remaining, free);
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return remaining <= 0;
    }

    public static boolean addItemToContainer(SimpleContainer container, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack remaining = stack.copy();

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slotStack = container.getItem(i);

            if (slotStack.isEmpty()) {
                int placed = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack newStack = remaining.copy();
                newStack.setCount(placed);
                container.setItem(i, newStack);
                remaining.shrink(placed);
            } else if (ItemStack.isSameItemSameTags(slotStack, remaining)) {
                int free = Math.max(0, slotStack.getMaxStackSize() - slotStack.getCount());
                if (free > 0) {
                    int placed = Math.min(free, remaining.getCount());
                    slotStack.grow(placed);
                    remaining.shrink(placed);
                }
            }
        }

        container.setChanged();
        return remaining.isEmpty();
    }

    public static ItemStack buildStack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static int countItemInList(List<ItemStack> list, Item item) {
        int total = 0;

        for (ItemStack stack : list) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static int removeItemFromList(List<ItemStack> list, Item item, int remaining) {
        for (int i = 0; i < list.size() && remaining > 0; i++) {
            ItemStack stack = list.get(i);

            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;

            if (stack.isEmpty()) {
                list.set(i, ItemStack.EMPTY);
            }
        }

        return remaining;
    }
}