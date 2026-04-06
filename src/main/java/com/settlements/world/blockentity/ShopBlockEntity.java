package com.settlements.world.blockentity;

import com.settlements.data.model.ShopRecord;
import com.settlements.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShopBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    private UUID shopId;
    private UUID ownerUuid;
    private UUID settlementUuid;
    private String shopName = "Магазин";
    private boolean enabled = true;
    private long balance = 0L;

    private final SimpleContainer inventory = new SimpleContainer(27) {
        @Override
        public void setChanged() {
            super.setChanged();
            ShopBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return ShopBlockEntity.this.getLevel() != null
                    && ShopBlockEntity.this.getLevel().getBlockEntity(ShopBlockEntity.this.getBlockPos()) == ShopBlockEntity.this
                    && player.distanceToSqr(
                    ShopBlockEntity.this.getBlockPos().getX() + 0.5D,
                    ShopBlockEntity.this.getBlockPos().getY() + 0.5D,
                    ShopBlockEntity.this.getBlockPos().getZ() + 0.5D
            ) <= 64.0D;
        }
    };

    private LazyOptional<InvWrapper> inventoryCapability = LazyOptional.empty();

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOP_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        inventoryCapability = LazyOptional.of(() -> new InvWrapper(inventory));
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCapability.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (shopId != null) {
            tag.putUUID("ShopId", shopId);
        }

        if (ownerUuid != null) {
            tag.putUUID("OwnerUuid", ownerUuid);
        }

        if (settlementUuid != null) {
            tag.putUUID("SettlementUuid", settlementUuid);
        }

        tag.putString("ShopName", shopName);
        tag.putBoolean("Enabled", enabled);
        tag.putLong("Balance", balance);

        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items =
                net.minecraft.core.NonNullList.withSize(inventory.getContainerSize(), net.minecraft.world.item.ItemStack.EMPTY);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            items.set(i, inventory.getItem(i));
        }

        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.shopId = tag.hasUUID("ShopId") ? tag.getUUID("ShopId") : null;
        this.ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        this.settlementUuid = tag.hasUUID("SettlementUuid") ? tag.getUUID("SettlementUuid") : null;
        this.shopName = tag.getString("ShopName");
        if (this.shopName == null || this.shopName.isEmpty()) {
            this.shopName = "Магазин";
        }

        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        this.balance = tag.getLong("Balance");

        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items =
                net.minecraft.core.NonNullList.withSize(inventory.getContainerSize(), net.minecraft.world.item.ItemStack.EMPTY);

        ContainerHelper.loadAllItems(tag, items);

        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    public void syncFromRecord(ShopRecord record) {
        if (record == null) {
            return;
        }

        this.shopId = record.getId();
        this.ownerUuid = record.getOwnerUuid();
        this.settlementUuid = record.getSettlementId();
        this.shopName = record.getName();
        this.enabled = record.isEnabled();
        this.balance = record.getBalance();
        setChanged();
    }

    public UUID getShopId() {
        return shopId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public UUID getSettlementUuid() {
        return settlementUuid;
    }

    public String getShopName() {
        return shopName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getBalance() {
        return balance;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public void setShopId(UUID shopId) {
        this.shopId = shopId;
        setChanged();
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        setChanged();
    }

    public void setSettlementUuid(UUID settlementUuid) {
        this.settlementUuid = settlementUuid;
        setChanged();
    }

    public void setShopName(String shopName) {
        if (shopName != null && !shopName.trim().isEmpty()) {
            this.shopName = shopName.trim();
            setChanged();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    public void setBalance(long balance) {
        this.balance = Math.max(0L, balance);
        setChanged();
    }

    public void dropInventory() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack
                );
                inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }

        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(shopName + " [склад]");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, inventory);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCapability.cast();
        }

        return super.getCapability(capability, side);
    }
}