package com.settlements.service;

import com.settlements.SettlementsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class CurrencyService {
    public static final long BRONZE_VALUE = 1L;
    public static final long SILVER_VALUE = 100L;
    public static final long GOLD_VALUE = 1000L;

    private static final ResourceLocation BRONZE_COIN_ID = new ResourceLocation(SettlementsMod.MOD_ID, "bronze_coin");
    private static final ResourceLocation SILVER_COIN_ID = new ResourceLocation(SettlementsMod.MOD_ID, "silver_coin");
    private static final ResourceLocation GOLD_COIN_ID = new ResourceLocation(SettlementsMod.MOD_ID, "gold_coin");

    private CurrencyService() {
    }

    public static boolean isCurrency(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        return item == getBronzeCoinItem()
                || item == getSilverCoinItem()
                || item == getGoldCoinItem();
    }

    public static long getStackCurrencyValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }

        Item item = stack.getItem();
        long singleValue;

        if (item == getBronzeCoinItem()) {
            singleValue = BRONZE_VALUE;
        } else if (item == getSilverCoinItem()) {
            singleValue = SILVER_VALUE;
        } else if (item == getGoldCoinItem()) {
            singleValue = GOLD_VALUE;
        } else {
            return 0L;
        }

        return singleValue * stack.getCount();
    }

    public static long countPlayerCurrency(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        long total = 0L;

        total += countCurrencyInList(inventory.items);
        total += countCurrencyInList(inventory.armor);
        total += countCurrencyInList(inventory.offhand);

        return total;
    }

    public static long removeAllCurrencyFromPlayer(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        long removed = 0L;

        removed += removeCurrencyFromList(inventory.items);
        removed += removeCurrencyFromList(inventory.armor);
        removed += removeCurrencyFromList(inventory.offhand);

        inventory.setChanged();
        return removed;
    }

    public static boolean removeCurrencyAmountFromPlayer(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return false;
        }

        long total = countPlayerCurrency(player);
        if (total < amount) {
            return false;
        }

        RemovalPlan plan = buildBestRemovalPlan(player, amount);
        if (plan == null) {
            return false;
        }

        consumeCoinCount(player, getGoldCoinItem(), plan.goldCoinsToRemove);
        consumeCoinCount(player, getSilverCoinItem(), plan.silverCoinsToRemove);
        consumeCoinCount(player, getBronzeCoinItem(), plan.bronzeCoinsToRemove);

        player.getInventory().setChanged();

        long change = plan.totalRemovedValue - amount;
        if (change > 0L) {
            giveCurrencyToPlayer(player, change);
        }

        return true;
    }

    public static void giveCurrencyToPlayer(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return;
        }

        List<ItemStack> stacks = splitAmountToStacks(amount);

        for (ItemStack stack : stacks) {
            boolean added = player.getInventory().add(stack);
            if (!added) {
                player.drop(stack, false);
            }
        }

        player.getInventory().setChanged();
    }

    public static List<ItemStack> splitAmountToStacks(long amount) {
        List<ItemStack> result = new ArrayList<ItemStack>();

        if (amount <= 0L) {
            return result;
        }

        long goldCount = amount / GOLD_VALUE;
        amount %= GOLD_VALUE;

        long silverCount = amount / SILVER_VALUE;
        amount %= SILVER_VALUE;

        long bronzeCount = amount;

        addStacksForCount(result, getGoldCoinItem(), goldCount);
        addStacksForCount(result, getSilverCoinItem(), silverCount);
        addStacksForCount(result, getBronzeCoinItem(), bronzeCount);

        return result;
    }

    private static RemovalPlan buildBestRemovalPlan(ServerPlayer player, long targetAmount) {
        int goldCount = countCoins(player, getGoldCoinItem());
        int silverCount = countCoins(player, getSilverCoinItem());
        int bronzeCount = countCoins(player, getBronzeCoinItem());

        RemovalPlan bestPlan = null;

        for (int goldToUse = 0; goldToUse <= goldCount; goldToUse++) {
            long goldValue = goldToUse * GOLD_VALUE;

            if (bestPlan != null && goldValue > bestPlan.totalRemovedValue) {
                break;
            }

            for (int silverToUse = 0; silverToUse <= silverCount; silverToUse++) {
                long partialValue = goldValue + (silverToUse * SILVER_VALUE);

                if (bestPlan != null && partialValue > bestPlan.totalRemovedValue) {
                    break;
                }

                int bronzeToUse;
                long totalRemoved;

                if (partialValue >= targetAmount) {
                    bronzeToUse = 0;
                    totalRemoved = partialValue;
                } else {
                    long needBronze = targetAmount - partialValue;
                    if (needBronze > bronzeCount) {
                        continue;
                    }

                    bronzeToUse = (int) needBronze;
                    totalRemoved = targetAmount;
                }

                if (bestPlan == null
                        || totalRemoved < bestPlan.totalRemovedValue
                        || (totalRemoved == bestPlan.totalRemovedValue
                        && (goldToUse + silverToUse + bronzeToUse) < bestPlan.totalCoinCount())) {
                    bestPlan = new RemovalPlan(goldToUse, silverToUse, bronzeToUse, totalRemoved);
                }
            }
        }

        return bestPlan;
    }

    private static int countCoins(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;

        total += countCoinInList(inventory.items, item);
        total += countCoinInList(inventory.armor, item);
        total += countCoinInList(inventory.offhand, item);

        return total;
    }

    private static int countCoinInList(List<ItemStack> list, Item item) {
        int total = 0;

        for (ItemStack stack : list) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static void consumeCoinCount(ServerPlayer player, Item item, int count) {
        if (count <= 0) {
            return;
        }

        Inventory inventory = player.getInventory();

        count = consumeCoinCountFromList(inventory.items, item, count);
        count = consumeCoinCountFromList(inventory.armor, item, count);
        consumeCoinCountFromList(inventory.offhand, item, count);
    }

    private static int consumeCoinCountFromList(List<ItemStack> list, Item item, int remaining) {
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

    private static void addStacksForCount(List<ItemStack> result, Item item, long count) {
        while (count > 0L) {
            int stackSize = (int) Math.min(64L, count);
            result.add(new ItemStack(item, stackSize));
            count -= stackSize;
        }
    }

    private static long countCurrencyInList(List<ItemStack> list) {
        long total = 0L;

        for (ItemStack stack : list) {
            total += getStackCurrencyValue(stack);
        }

        return total;
    }

    private static long removeCurrencyFromList(List<ItemStack> list) {
        long removed = 0L;

        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            long value = getStackCurrencyValue(stack);

            if (value > 0L) {
                removed += value;
                list.set(i, ItemStack.EMPTY);
            }
        }

        return removed;
    }

    private static Item getBronzeCoinItem() {
        Item item = ForgeRegistries.ITEMS.getValue(BRONZE_COIN_ID);
        if (item == null) {
            throw new IllegalStateException("Не найден предмет settlements:bronze_coin");
        }
        return item;
    }

    private static Item getSilverCoinItem() {
        Item item = ForgeRegistries.ITEMS.getValue(SILVER_COIN_ID);
        if (item == null) {
            throw new IllegalStateException("Не найден предмет settlements:silver_coin");
        }
        return item;
    }

    private static Item getGoldCoinItem() {
        Item item = ForgeRegistries.ITEMS.getValue(GOLD_COIN_ID);
        if (item == null) {
            throw new IllegalStateException("Не найден предмет settlements:gold_coin");
        }
        return item;
    }

    private static final class RemovalPlan {
        private final int goldCoinsToRemove;
        private final int silverCoinsToRemove;
        private final int bronzeCoinsToRemove;
        private final long totalRemovedValue;

        private RemovalPlan(int goldCoinsToRemove, int silverCoinsToRemove, int bronzeCoinsToRemove, long totalRemovedValue) {
            this.goldCoinsToRemove = goldCoinsToRemove;
            this.silverCoinsToRemove = silverCoinsToRemove;
            this.bronzeCoinsToRemove = bronzeCoinsToRemove;
            this.totalRemovedValue = totalRemovedValue;
        }

        private int totalCoinCount() {
            return goldCoinsToRemove + silverCoinsToRemove + bronzeCoinsToRemove;
        }
    }
}