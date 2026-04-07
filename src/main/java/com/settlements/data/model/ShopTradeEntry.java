package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class ShopTradeEntry {
    private final UUID id;
    private String itemId;

    private boolean enabled;

    private int sellBatchSize;
    private int buyBatchSize;

    private long sellPrice;
    private long buyPrice;

    private boolean canSellToPlayer;
    private boolean canBuyFromPlayer;

    private int minStockToSell;
    private boolean requireRealStock;
    private boolean requireRealBalance;

    private PriceMode priceMode;

    private long baseSellPrice;
    private long baseBuyPrice;

    private long currentSellPrice;
    private long currentBuyPrice;

    private long minSellPrice;
    private long maxSellPrice;
    private long minBuyPrice;
    private long maxBuyPrice;

    private double elasticity;
    private double decayPerStep;
    private double inactivitySellDrop;
    private double inactivityBuyRise;

    private double sellDemand;
    private double buySupply;

    private long lastSellTimestamp;
    private long lastBuyTimestamp;

    public ShopTradeEntry(UUID id,
                          String itemId,
                          boolean enabled,
                          int sellBatchSize,
                          int buyBatchSize,
                          long sellPrice,
                          long buyPrice,
                          boolean canSellToPlayer,
                          boolean canBuyFromPlayer,
                          int minStockToSell,
                          boolean requireRealStock,
                          boolean requireRealBalance) {
        this.id = id;
        this.itemId = itemId;
        this.enabled = enabled;
        this.sellBatchSize = Math.max(1, sellBatchSize);
        this.buyBatchSize = Math.max(1, buyBatchSize);
        this.sellPrice = Math.max(0L, sellPrice);
        this.buyPrice = Math.max(0L, buyPrice);
        this.canSellToPlayer = canSellToPlayer;
        this.canBuyFromPlayer = canBuyFromPlayer;
        this.minStockToSell = Math.max(0, minStockToSell);
        this.requireRealStock = requireRealStock;
        this.requireRealBalance = requireRealBalance;

        this.priceMode = PriceMode.FIXED;

        this.baseSellPrice = this.sellPrice;
        this.baseBuyPrice = this.buyPrice;
        this.currentSellPrice = this.sellPrice;
        this.currentBuyPrice = this.buyPrice;

        this.minSellPrice = this.sellPrice > 0L ? this.sellPrice : 1L;
        this.maxSellPrice = Math.max(this.sellPrice, 1L);
        this.minBuyPrice = this.buyPrice > 0L ? this.buyPrice : 1L;
        this.maxBuyPrice = Math.max(this.buyPrice, 1L);

        this.elasticity = 0.01D;
        this.decayPerStep = 0.98D;
        this.inactivitySellDrop = 0.0D;
        this.inactivityBuyRise = 0.0D;

        this.sellDemand = 0.0D;
        this.buySupply = 0.0D;

        this.lastSellTimestamp = 0L;
        this.lastBuyTimestamp = 0L;
    }

    public static ShopTradeEntry createSell(String itemId, int batchSize, long sellPrice) {
        return new ShopTradeEntry(
                UUID.randomUUID(),
                itemId,
                true,
                batchSize,
                1,
                sellPrice,
                0L,
                true,
                false,
                0,
                true,
                true
        );
    }

    public static ShopTradeEntry createBuy(String itemId, int batchSize, long buyPrice) {
        return new ShopTradeEntry(
                UUID.randomUUID(),
                itemId,
                true,
                1,
                batchSize,
                0L,
                buyPrice,
                false,
                true,
                0,
                true,
                true
        );
    }

    public static ShopTradeEntry createDual(String itemId, int sellBatchSize, long sellPrice, int buyBatchSize, long buyPrice) {
        return new ShopTradeEntry(
                UUID.randomUUID(),
                itemId,
                true,
                sellBatchSize,
                buyBatchSize,
                sellPrice,
                buyPrice,
                true,
                true,
                0,
                true,
                true
        );
    }

    public UUID getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSellBatchSize() {
        return sellBatchSize;
    }

    public int getBuyBatchSize() {
        return buyBatchSize;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public long getBuyPrice() {
        return buyPrice;
    }

    public long getEffectiveSellPrice() {
        return priceMode == PriceMode.DYNAMIC ? currentSellPrice : sellPrice;
    }

    public long getEffectiveBuyPrice() {
        return priceMode == PriceMode.DYNAMIC ? currentBuyPrice : buyPrice;
    }

    public boolean canSellToPlayer() {
        return canSellToPlayer;
    }

    public boolean canBuyFromPlayer() {
        return canBuyFromPlayer;
    }

    public int getMinStockToSell() {
        return minStockToSell;
    }

    public boolean requireRealStock() {
        return requireRealStock;
    }

    public boolean requireRealBalance() {
        return requireRealBalance;
    }

    public PriceMode getPriceMode() {
        return priceMode;
    }

    public long getBaseSellPrice() {
        return baseSellPrice;
    }

    public long getBaseBuyPrice() {
        return baseBuyPrice;
    }

    public long getCurrentSellPrice() {
        return currentSellPrice;
    }

    public long getCurrentBuyPrice() {
        return currentBuyPrice;
    }

    public long getMinSellPrice() {
        return minSellPrice;
    }

    public long getMaxSellPrice() {
        return maxSellPrice;
    }

    public long getMinBuyPrice() {
        return minBuyPrice;
    }

    public long getMaxBuyPrice() {
        return maxBuyPrice;
    }

    public double getElasticity() {
        return elasticity;
    }

    public double getDecayPerStep() {
        return decayPerStep;
    }

    public double getInactivitySellDrop() {
        return inactivitySellDrop;
    }

    public double getInactivityBuyRise() {
        return inactivityBuyRise;
    }

    public double getSellDemand() {
        return sellDemand;
    }

    public double getBuySupply() {
        return buySupply;
    }

    public long getLastSellTimestamp() {
        return lastSellTimestamp;
    }

    public long getLastBuyTimestamp() {
        return lastBuyTimestamp;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDynamicPricing(long baseSellPrice,
                                  long baseBuyPrice,
                                  long minSellPrice,
                                  long maxSellPrice,
                                  long minBuyPrice,
                                  long maxBuyPrice,
                                  double elasticity,
                                  double decayPerStep,
                                  double inactivitySellDrop,
                                  double inactivityBuyRise) {
        this.priceMode = PriceMode.DYNAMIC;

        this.baseSellPrice = Math.max(0L, baseSellPrice);
        this.baseBuyPrice = Math.max(0L, baseBuyPrice);

        this.minSellPrice = Math.max(1L, minSellPrice);
        this.maxSellPrice = Math.max(this.minSellPrice, maxSellPrice);

        this.minBuyPrice = Math.max(1L, minBuyPrice);
        this.maxBuyPrice = Math.max(this.minBuyPrice, maxBuyPrice);

        this.elasticity = Math.max(0.0D, elasticity);
        this.decayPerStep = clampDouble(decayPerStep, 0.0D, 10.0D);
        this.inactivitySellDrop = Math.max(0.0D, inactivitySellDrop);
        this.inactivityBuyRise = Math.max(0.0D, inactivityBuyRise);

        this.currentSellPrice = clampLong(this.baseSellPrice, this.minSellPrice, this.maxSellPrice);
        this.currentBuyPrice = clampLong(this.baseBuyPrice, this.minBuyPrice, this.maxBuyPrice);
    }

    public void setFixedPricing() {
        this.priceMode = PriceMode.FIXED;
        this.currentSellPrice = this.sellPrice;
        this.currentBuyPrice = this.buyPrice;
        this.sellDemand = 0.0D;
        this.buySupply = 0.0D;
        this.lastSellTimestamp = 0L;
        this.lastBuyTimestamp = 0L;
    }

    public void applyDynamicDecay(long currentGameTime) {
        if (priceMode != PriceMode.DYNAMIC) {
            return;
        }

        this.sellDemand *= decayPerStep;
        this.buySupply *= decayPerStep;

        if (canSellToPlayer && lastSellTimestamp > 0L && inactivitySellDrop > 0.0D) {
            long idleTicks = Math.max(0L, currentGameTime - lastSellTimestamp);
            this.sellDemand = Math.max(0.0D, sellDemand - inactivitySellDrop * idleTicks);
        }

        if (canBuyFromPlayer && lastBuyTimestamp > 0L && inactivityBuyRise > 0.0D) {
            long idleTicks = Math.max(0L, currentGameTime - lastBuyTimestamp);
            this.buySupply = Math.max(0.0D, buySupply - inactivityBuyRise * idleTicks);
        }

        recomputeDynamicPrices();
    }

    public void markPlayerBoughtFromShop(int amount, long currentGameTime) {
        if (priceMode != PriceMode.DYNAMIC) {
            return;
        }

        this.sellDemand += Math.max(0, amount);
        this.lastSellTimestamp = currentGameTime;
        recomputeDynamicPrices();
    }

    public void markPlayerSoldToShop(int amount, long currentGameTime) {
        if (priceMode != PriceMode.DYNAMIC) {
            return;
        }

        this.buySupply += Math.max(0, amount);
        this.lastBuyTimestamp = currentGameTime;
        recomputeDynamicPrices();
    }

    private void recomputeDynamicPrices() {
        if (canSellToPlayer) {
            long computedSell = Math.round(baseSellPrice * (1.0D + elasticity * sellDemand));
            this.currentSellPrice = clampLong(computedSell, minSellPrice, maxSellPrice);
        }

        if (canBuyFromPlayer) {
            double factor = 1.0D - elasticity * buySupply;
            if (factor < 0.0D) {
                factor = 0.0D;
            }

            long computedBuy = Math.round(baseBuyPrice * factor);
            this.currentBuyPrice = clampLong(computedBuy, minBuyPrice, maxBuyPrice);
        }
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("Id", id);
        tag.putString("ItemId", itemId);
        tag.putBoolean("Enabled", enabled);
        tag.putInt("SellBatchSize", sellBatchSize);
        tag.putInt("BuyBatchSize", buyBatchSize);
        tag.putLong("SellPrice", sellPrice);
        tag.putLong("BuyPrice", buyPrice);
        tag.putBoolean("CanSellToPlayer", canSellToPlayer);
        tag.putBoolean("CanBuyFromPlayer", canBuyFromPlayer);
        tag.putInt("MinStockToSell", minStockToSell);
        tag.putBoolean("RequireRealStock", requireRealStock);
        tag.putBoolean("RequireRealBalance", requireRealBalance);

        tag.putString("PriceMode", priceMode.name());
        tag.putLong("BaseSellPrice", baseSellPrice);
        tag.putLong("BaseBuyPrice", baseBuyPrice);
        tag.putLong("CurrentSellPrice", currentSellPrice);
        tag.putLong("CurrentBuyPrice", currentBuyPrice);
        tag.putLong("MinSellPrice", minSellPrice);
        tag.putLong("MaxSellPrice", maxSellPrice);
        tag.putLong("MinBuyPrice", minBuyPrice);
        tag.putLong("MaxBuyPrice", maxBuyPrice);
        tag.putDouble("Elasticity", elasticity);
        tag.putDouble("DecayPerStep", decayPerStep);
        tag.putDouble("InactivitySellDrop", inactivitySellDrop);
        tag.putDouble("InactivityBuyRise", inactivityBuyRise);
        tag.putDouble("SellDemand", sellDemand);
        tag.putDouble("BuySupply", buySupply);
        tag.putLong("LastSellTimestamp", lastSellTimestamp);
        tag.putLong("LastBuyTimestamp", lastBuyTimestamp);

        return tag;
    }

    public static ShopTradeEntry load(CompoundTag tag) {
        ShopTradeEntry entry = new ShopTradeEntry(
                tag.getUUID("Id"),
                tag.getString("ItemId"),
                !tag.contains("Enabled") || tag.getBoolean("Enabled"),
                tag.contains("SellBatchSize") ? tag.getInt("SellBatchSize") : 1,
                tag.contains("BuyBatchSize") ? tag.getInt("BuyBatchSize") : 1,
                tag.getLong("SellPrice"),
                tag.getLong("BuyPrice"),
                tag.getBoolean("CanSellToPlayer"),
                tag.getBoolean("CanBuyFromPlayer"),
                tag.getInt("MinStockToSell"),
                !tag.contains("RequireRealStock") || tag.getBoolean("RequireRealStock"),
                !tag.contains("RequireRealBalance") || tag.getBoolean("RequireRealBalance")
        );

        if (tag.contains("PriceMode")) {
            entry.priceMode = PriceMode.valueOf(tag.getString("PriceMode"));
        }

        entry.baseSellPrice = tag.contains("BaseSellPrice") ? tag.getLong("BaseSellPrice") : entry.sellPrice;
        entry.baseBuyPrice = tag.contains("BaseBuyPrice") ? tag.getLong("BaseBuyPrice") : entry.buyPrice;
        entry.currentSellPrice = tag.contains("CurrentSellPrice") ? tag.getLong("CurrentSellPrice") : entry.sellPrice;
        entry.currentBuyPrice = tag.contains("CurrentBuyPrice") ? tag.getLong("CurrentBuyPrice") : entry.buyPrice;
        entry.minSellPrice = tag.contains("MinSellPrice") ? tag.getLong("MinSellPrice") : Math.max(1L, entry.sellPrice);
        entry.maxSellPrice = tag.contains("MaxSellPrice") ? tag.getLong("MaxSellPrice") : Math.max(entry.minSellPrice, entry.sellPrice);
        entry.minBuyPrice = tag.contains("MinBuyPrice") ? tag.getLong("MinBuyPrice") : Math.max(1L, entry.buyPrice);
        entry.maxBuyPrice = tag.contains("MaxBuyPrice") ? tag.getLong("MaxBuyPrice") : Math.max(entry.minBuyPrice, entry.buyPrice);

        entry.elasticity = tag.contains("Elasticity") ? tag.getDouble("Elasticity") : 0.01D;
        entry.decayPerStep = tag.contains("DecayPerStep") ? tag.getDouble("DecayPerStep") : 0.98D;
        entry.inactivitySellDrop = tag.contains("InactivitySellDrop") ? tag.getDouble("InactivitySellDrop") : 0.0D;
        entry.inactivityBuyRise = tag.contains("InactivityBuyRise") ? tag.getDouble("InactivityBuyRise") : 0.0D;

        entry.sellDemand = tag.contains("SellDemand") ? tag.getDouble("SellDemand") : 0.0D;
        entry.buySupply = tag.contains("BuySupply") ? tag.getDouble("BuySupply") : 0.0D;
        entry.lastSellTimestamp = tag.contains("LastSellTimestamp") ? tag.getLong("LastSellTimestamp") : 0L;
        entry.lastBuyTimestamp = tag.contains("LastBuyTimestamp") ? tag.getLong("LastBuyTimestamp") : 0L;

        return entry;
    }
}