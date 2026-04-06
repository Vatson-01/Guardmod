package com.settlements.data.model;

import com.settlements.util.BlockPosKeyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ShopRecord {
    private final UUID id;
    private final ShopType type;

    private UUID ownerUuid;
    private final UUID settlementId;

    private final ResourceLocation dimensionId;
    private final int x;
    private final int y;
    private final int z;

    private String name;
    private boolean enabled;
    private long balance;

    private boolean infiniteStock;
    private boolean infiniteBalance;
    private boolean indestructible;

    private final List<ShopTradeEntry> trades = new ArrayList<ShopTradeEntry>();

    private long createdAt;
    private long updatedAt;

    public ShopRecord(UUID id,
                      ShopType type,
                      UUID ownerUuid,
                      UUID settlementId,
                      ResourceLocation dimensionId,
                      BlockPos pos,
                      String name,
                      boolean enabled,
                      long balance,
                      boolean infiniteStock,
                      boolean infiniteBalance,
                      boolean indestructible,
                      long createdAt) {
        this.id = id;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.settlementId = settlementId;
        this.dimensionId = dimensionId;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.name = name;
        this.enabled = enabled;
        this.balance = Math.max(0L, balance);
        this.infiniteStock = infiniteStock;
        this.infiniteBalance = infiniteBalance;
        this.indestructible = indestructible;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static ShopRecord createPlayerShop(UUID ownerUuid, UUID settlementId, ResourceLocation dimensionId, BlockPos pos, long gameTime) {
        return new ShopRecord(
                UUID.randomUUID(),
                ShopType.PLAYER,
                ownerUuid,
                settlementId,
                dimensionId,
                pos,
                "Магазин",
                true,
                0L,
                false,
                false,
                false,
                gameTime
        );
    }

    public static ShopRecord createAdminShop(ResourceLocation dimensionId, BlockPos pos, long gameTime) {
        return new ShopRecord(
                UUID.randomUUID(),
                ShopType.ADMIN,
                null,
                null,
                dimensionId,
                pos,
                "Админ-магазин",
                true,
                0L,
                true,
                true,
                true,
                gameTime
        );
    }

    public UUID getId() {
        return id;
    }

    public ShopType getType() {
        return type;
    }

    public boolean isAdminShop() {
        return type == ShopType.ADMIN;
    }

    public boolean isPlayerShop() {
        return type == ShopType.PLAYER;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public ResourceLocation getDimensionId() {
        return dimensionId;
    }

    public BlockPos getPos() {
        return new BlockPos(x, y, z);
    }

    public String getPosKey() {
        return BlockPosKeyUtil.toKey(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId), getPos());
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getBalance() {
        return balance;
    }

    public boolean isInfiniteStock() {
        return infiniteStock;
    }

    public boolean isInfiniteBalance() {
        return infiniteBalance;
    }

    public boolean isIndestructible() {
        return indestructible;
    }

    public List<ShopTradeEntry> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public ShopTradeEntry getTradeByHumanIndex(int index) {
        if (index < 1 || index > trades.size()) {
            return null;
        }

        return trades.get(index - 1);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setOwnerUuid(UUID ownerUuid, long gameTime) {
        this.ownerUuid = ownerUuid;
        touch(gameTime);
    }

    public void setName(String name, long gameTime) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
            touch(gameTime);
        }
    }

    public void setEnabled(boolean enabled, long gameTime) {
        this.enabled = enabled;
        touch(gameTime);
    }

    public void setBalance(long balance, long gameTime) {
        this.balance = Math.max(0L, balance);
        touch(gameTime);
    }

    public void setInfiniteStock(boolean infiniteStock, long gameTime) {
        this.infiniteStock = infiniteStock;
        touch(gameTime);
    }

    public void setInfiniteBalance(boolean infiniteBalance, long gameTime) {
        this.infiniteBalance = infiniteBalance;
        touch(gameTime);
    }

    public void setIndestructible(boolean indestructible, long gameTime) {
        this.indestructible = indestructible;
        touch(gameTime);
    }

    public void deposit(long amount, long gameTime) {
        if (amount <= 0L) {
            return;
        }

        this.balance += amount;
        touch(gameTime);
    }

    public boolean withdraw(long amount, long gameTime) {
        if (amount <= 0L || this.balance < amount) {
            return false;
        }

        this.balance -= amount;
        touch(gameTime);
        return true;
    }

    public void addTrade(ShopTradeEntry trade, long gameTime) {
        trades.add(trade);
        touch(gameTime);
    }

    public boolean removeTradeByHumanIndex(int index, long gameTime) {
        if (index < 1 || index > trades.size()) {
            return false;
        }

        trades.remove(index - 1);
        touch(gameTime);
        return true;
    }

    private void touch(long gameTime) {
        this.updatedAt = gameTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("Id", id);
        tag.putString("Type", type.name());

        if (ownerUuid != null) {
            tag.putUUID("OwnerUuid", ownerUuid);
        }

        if (settlementId != null) {
            tag.putUUID("SettlementId", settlementId);
        }

        tag.putString("DimensionId", dimensionId.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putInt("Z", z);
        tag.putString("Name", name);
        tag.putBoolean("Enabled", enabled);
        tag.putLong("Balance", balance);
        tag.putBoolean("InfiniteStock", infiniteStock);
        tag.putBoolean("InfiniteBalance", infiniteBalance);
        tag.putBoolean("Indestructible", indestructible);
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("UpdatedAt", updatedAt);

        ListTag tradesTag = new ListTag();
        for (ShopTradeEntry trade : trades) {
            tradesTag.add(trade.save());
        }
        tag.put("Trades", tradesTag);

        return tag;
    }

    public static ShopRecord load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        ShopType type = ShopType.valueOf(tag.getString("Type"));
        UUID ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        UUID settlementId = tag.hasUUID("SettlementId") ? tag.getUUID("SettlementId") : null;

        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("DimensionId"));
        if (dimensionId == null) {
            dimensionId = Level.OVERWORLD.location();
        }

        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        String name = tag.getString("Name");
        boolean enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        long balance = tag.getLong("Balance");
        boolean infiniteStock = tag.contains("InfiniteStock") && tag.getBoolean("InfiniteStock");
        boolean infiniteBalance = tag.contains("InfiniteBalance") && tag.getBoolean("InfiniteBalance");
        boolean indestructible = tag.contains("Indestructible") && tag.getBoolean("Indestructible");
        long createdAt = tag.getLong("CreatedAt");

        ShopRecord record = new ShopRecord(
                id,
                type,
                ownerUuid,
                settlementId,
                dimensionId,
                pos,
                name == null || name.isEmpty() ? "Магазин" : name,
                enabled,
                balance,
                infiniteStock,
                infiniteBalance,
                indestructible,
                createdAt
        );

        record.updatedAt = tag.getLong("UpdatedAt");

        if (tag.contains("Trades", Tag.TAG_LIST)) {
            ListTag tradesTag = tag.getList("Trades", Tag.TAG_COMPOUND);
            for (int i = 0; i < tradesTag.size(); i++) {
                record.trades.add(ShopTradeEntry.load(tradesTag.getCompound(i)));
            }
        }

        return record;
    }
}