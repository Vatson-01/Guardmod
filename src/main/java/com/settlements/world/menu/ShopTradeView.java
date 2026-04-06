package com.settlements.world.menu;

import com.settlements.data.model.PriceMode;
import com.settlements.data.model.ShopTradeEntry;
import net.minecraft.network.FriendlyByteBuf;

public class ShopTradeView {
    private final String itemId;
    private final boolean enabled;
    private final boolean canSellToPlayer;
    private final boolean canBuyFromPlayer;
    private final int sellBatchSize;
    private final int buyBatchSize;
    private final long sellPrice;
    private final long buyPrice;
    private final PriceMode priceMode;

    public ShopTradeView(String itemId,
                         boolean enabled,
                         boolean canSellToPlayer,
                         boolean canBuyFromPlayer,
                         int sellBatchSize,
                         int buyBatchSize,
                         long sellPrice,
                         long buyPrice,
                         PriceMode priceMode) {
        this.itemId = itemId;
        this.enabled = enabled;
        this.canSellToPlayer = canSellToPlayer;
        this.canBuyFromPlayer = canBuyFromPlayer;
        this.sellBatchSize = sellBatchSize;
        this.buyBatchSize = buyBatchSize;
        this.sellPrice = sellPrice;
        this.buyPrice = buyPrice;
        this.priceMode = priceMode;
    }

    public static ShopTradeView fromTrade(ShopTradeEntry trade) {
        return new ShopTradeView(
                trade.getItemId(),
                trade.isEnabled(),
                trade.canSellToPlayer(),
                trade.canBuyFromPlayer(),
                trade.getSellBatchSize(),
                trade.getBuyBatchSize(),
                trade.getEffectiveSellPrice(),
                trade.getEffectiveBuyPrice(),
                trade.getPriceMode()
        );
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canSellToPlayer() {
        return canSellToPlayer;
    }

    public boolean canBuyFromPlayer() {
        return canBuyFromPlayer;
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

    public PriceMode getPriceMode() {
        return priceMode;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(itemId);
        buf.writeBoolean(enabled);
        buf.writeBoolean(canSellToPlayer);
        buf.writeBoolean(canBuyFromPlayer);
        buf.writeInt(sellBatchSize);
        buf.writeInt(buyBatchSize);
        buf.writeLong(sellPrice);
        buf.writeLong(buyPrice);
        buf.writeEnum(priceMode);
    }

    public static ShopTradeView read(FriendlyByteBuf buf) {
        return new ShopTradeView(
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readLong(),
                buf.readLong(),
                buf.readEnum(PriceMode.class)
        );
    }
}