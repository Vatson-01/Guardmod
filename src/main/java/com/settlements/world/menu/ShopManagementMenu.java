package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ShopRecord;
import com.settlements.registry.ModBlocks;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.ShopService;
import com.settlements.world.blockentity.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShopManagementMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_ENABLED = 0;
    public static final int BUTTON_OPEN_STORAGE = 1;

    public static final int BUTTON_DEPOSIT_ALL = 2;
    public static final int BUTTON_WITHDRAW_ALL = 3;
    public static final int BUTTON_DEPOSIT_10 = 4;
    public static final int BUTTON_WITHDRAW_10 = 5;
    public static final int BUTTON_DEPOSIT_100 = 6;
    public static final int BUTTON_WITHDRAW_100 = 7;
    public static final int BUTTON_DEPOSIT_1000 = 8;
    public static final int BUTTON_WITHDRAW_1000 = 9;

    public static final int BUTTON_TOGGLE_INFINITE_STOCK = 10;
    public static final int BUTTON_TOGGLE_INFINITE_BALANCE = 11;
    public static final int BUTTON_TOGGLE_INDESTRUCTIBLE = 12;

    private static final int DATA_BALANCE_LOW = 0;
    private static final int DATA_BALANCE_HIGH = 1;
    private static final int DATA_ENABLED = 2;
    private static final int DATA_IS_ADMIN = 3;
    private static final int DATA_INFINITE_STOCK = 4;
    private static final int DATA_INFINITE_BALANCE = 5;
    private static final int DATA_INDESTRUCTIBLE = 6;

    private final BlockPos shopPos;
    private final String shopName;
    private final ContainerData menuData;

    public ShopManagementMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                buf.readBlockPos(),
                buf.readUtf(),
                createClientData()
        );
    }

    public ShopManagementMenu(int containerId, Inventory playerInventory, BlockPos shopPos) {
        this(
                containerId,
                playerInventory,
                shopPos,
                getServerShopName(playerInventory, shopPos),
                createServerData(playerInventory, shopPos)
        );
    }

    private ShopManagementMenu(int containerId,
                               Inventory playerInventory,
                               BlockPos shopPos,
                               String shopName,
                               ContainerData menuData) {
        super(ModMenuTypes.SHOP_MANAGEMENT_MENU.get(), containerId);
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
    }

    public static void writeOpenData(FriendlyByteBuf buf, BlockPos pos, ShopRecord shop) {
        buf.writeBlockPos(pos);
        buf.writeUtf(shop.getName());
    }

    private static String getServerShopName(Inventory playerInventory, BlockPos pos) {
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
            return "Управление магазином";
        }

        ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), pos);
        return shop == null ? "Управление магазином" : shop.getName();
    }

    private static ContainerData createClientData() {
        return new net.minecraft.world.inventory.SimpleContainerData(7);
    }

    private static ContainerData createServerData(final Inventory playerInventory, final BlockPos shopPos) {
        return new ContainerData() {
            private ShopRecord getShop() {
                if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
                    return null;
                }

                return SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
            }

            @Override
            public int get(int index) {
                ShopRecord shop = getShop();
                long balance = shop == null ? 0L : shop.getBalance();

                if (index == DATA_BALANCE_LOW) {
                    return (int) (balance & 0xFFFFFFFFL);
                }

                if (index == DATA_BALANCE_HIGH) {
                    return (int) ((balance >>> 32) & 0xFFFFFFFFL);
                }

                if (index == DATA_ENABLED) {
                    return shop != null && shop.isEnabled() ? 1 : 0;
                }

                if (index == DATA_IS_ADMIN) {
                    return shop != null && shop.isAdminShop() ? 1 : 0;
                }

                if (index == DATA_INFINITE_STOCK) {
                    return shop != null && shop.isInfiniteStock() ? 1 : 0;
                }

                if (index == DATA_INFINITE_BALANCE) {
                    return shop != null && shop.isInfiniteBalance() ? 1 : 0;
                }

                if (index == DATA_INDESTRUCTIBLE) {
                    return shop != null && shop.isIndestructible() ? 1 : 0;
                }

                return 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 7;
            }
        };
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 176;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 234;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, startX + column * 18, hotbarY));
        }
    }

    public String getShopName() {
        return shopName;
    }

    public long getBalance() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_BALANCE_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_BALANCE_HIGH));
        return low | (high << 32);
    }

    public boolean isEnabled() {
        return menuData.get(DATA_ENABLED) != 0;
    }

    public boolean isAdminShop() {
        return menuData.get(DATA_IS_ADMIN) != 0;
    }

    public boolean isInfiniteStock() {
        return menuData.get(DATA_INFINITE_STOCK) != 0;
    }

    public boolean isInfiniteBalance() {
        return menuData.get(DATA_INFINITE_BALANCE) != 0;
    }

    public boolean isIndestructible() {
        return menuData.get(DATA_INDESTRUCTIBLE) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        try {
            if (buttonId == BUTTON_TOGGLE_ENABLED) {
                ShopService.setShopEnabledAt(serverPlayer, shopPos, !isEnabled());
                return true;
            }

            if (buttonId == BUTTON_DEPOSIT_ALL) {
                ShopService.depositAllToShopAt(serverPlayer, shopPos);
                return true;
            }

            if (buttonId == BUTTON_WITHDRAW_ALL) {
                ShopService.withdrawAllFromShopAt(serverPlayer, shopPos);
                return true;
            }

            if (buttonId == BUTTON_DEPOSIT_10) {
                ShopService.depositToShopAt(serverPlayer, shopPos, 10L);
                return true;
            }

            if (buttonId == BUTTON_WITHDRAW_10) {
                ShopService.withdrawFromShopAt(serverPlayer, shopPos, 10L);
                return true;
            }

            if (buttonId == BUTTON_DEPOSIT_100) {
                ShopService.depositToShopAt(serverPlayer, shopPos, 100L);
                return true;
            }

            if (buttonId == BUTTON_WITHDRAW_100) {
                ShopService.withdrawFromShopAt(serverPlayer, shopPos, 100L);
                return true;
            }

            if (buttonId == BUTTON_DEPOSIT_1000) {
                ShopService.depositToShopAt(serverPlayer, shopPos, 1000L);
                return true;
            }

            if (buttonId == BUTTON_WITHDRAW_1000) {
                ShopService.withdrawFromShopAt(serverPlayer, shopPos, 1000L);
                return true;
            }

            if (buttonId == BUTTON_OPEN_STORAGE) {
                if (!(serverPlayer.level().getBlockEntity(shopPos) instanceof ShopBlockEntity shopBlockEntity)) {
                    throw new IllegalStateException("Склад магазина не найден.");
                }

                serverPlayer.openMenu(shopBlockEntity);
                return true;
            }

            if (buttonId == BUTTON_TOGGLE_INFINITE_STOCK) {
                ShopService.setAdminInfiniteStockAt(serverPlayer, shopPos, !isInfiniteStock());
                return true;
            }

            if (buttonId == BUTTON_TOGGLE_INFINITE_BALANCE) {
                ShopService.setAdminInfiniteBalanceAt(serverPlayer, shopPos, !isInfiniteBalance());
                return true;
            }

            if (buttonId == BUTTON_TOGGLE_INDESTRUCTIBLE) {
                ShopService.setAdminIndestructibleAt(serverPlayer, shopPos, !isIndestructible());
                return true;
            }
        } catch (Exception e) {
            serverPlayer.displayClientMessage(Component.literal(e.getMessage()), true);
            return false;
        }

        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.level().getBlockState(shopPos).is(ModBlocks.SHOP_BLOCK.get())) {
            return false;
        }

        return player.distanceToSqr(
                shopPos.getX() + 0.5D,
                shopPos.getY() + 0.5D,
                shopPos.getZ() + 0.5D
        ) <= 64.0D;
    }
}