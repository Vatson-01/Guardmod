package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PriceMode;
import com.settlements.data.model.ShopRecord;
import com.settlements.data.model.ShopTradeEntry;
import com.settlements.registry.ModBlocks;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.ShopService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShopMenu extends AbstractContainerMenu {
    public static final int BUTTON_PREV = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_BUY = 2;
    public static final int BUTTON_SELL = 3;
    public static final int BUTTON_MANAGE = 4;

    private static final int DATA_SELECTED_INDEX = 0;
    private static final int DATA_BALANCE_LOW = 1;
    private static final int DATA_BALANCE_HIGH = 2;
    private static final int DATA_SELL_LOW = 3;
    private static final int DATA_SELL_HIGH = 4;
    private static final int DATA_BUY_LOW = 5;
    private static final int DATA_BUY_HIGH = 6;
    private static final int DATA_MODE = 7;
    private static final int DATA_FLAGS = 8;
    private static final int DATA_SELL_BATCH = 9;
    private static final int DATA_BUY_BATCH = 10;
    private static final int DATA_CAN_MANAGE = 11;

    private final BlockPos shopPos;
    private final String shopName;
    private final List<ShopTradeView> tradeViews;
    private final ContainerData menuData;

    public ShopMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                buf.readBlockPos(),
                buf.readUtf(),
                readTrades(buf),
                createClientData()
        );
    }

    public ShopMenu(int containerId, Inventory playerInventory, BlockPos shopPos) {
        this(
                containerId,
                playerInventory,
                shopPos,
                getServerShopName(playerInventory, shopPos),
                getServerTradeViews(playerInventory, shopPos),
                createServerData(playerInventory, shopPos)
        );
    }

    private ShopMenu(int containerId,
                     Inventory playerInventory,
                     BlockPos shopPos,
                     String shopName,
                     List<ShopTradeView> tradeViews,
                     ContainerData menuData) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.tradeViews = new ArrayList<ShopTradeView>(tradeViews);
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
    }

    public static void writeOpenData(FriendlyByteBuf buf, BlockPos pos, ShopRecord shop) {
        buf.writeBlockPos(pos);
        buf.writeUtf(shop.getName());

        List<ShopTradeView> views = new ArrayList<ShopTradeView>();
        for (ShopTradeEntry trade : shop.getTrades()) {
            views.add(ShopTradeView.fromTrade(trade));
        }

        buf.writeInt(views.size());
        for (ShopTradeView view : views) {
            view.write(buf);
        }
    }

    private static List<ShopTradeView> readTrades(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ShopTradeView> result = new ArrayList<ShopTradeView>(size);

        for (int i = 0; i < size; i++) {
            result.add(ShopTradeView.read(buf));
        }

        return result;
    }

    private static String getServerShopName(Inventory playerInventory, BlockPos pos) {
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
            return "Магазин";
        }

        ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), pos);
        return shop == null ? "Магазин" : shop.getName();
    }

    private static List<ShopTradeView> getServerTradeViews(Inventory playerInventory, BlockPos pos) {
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
            return Collections.emptyList();
        }

        ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), pos);
        if (shop == null) {
            return Collections.emptyList();
        }

        List<ShopTradeView> result = new ArrayList<ShopTradeView>();
        for (ShopTradeEntry trade : shop.getTrades()) {
            result.add(ShopTradeView.fromTrade(trade));
        }

        return result;
    }

    private static ContainerData createClientData() {
        return new net.minecraft.world.inventory.SimpleContainerData(12);
    }

    private static ContainerData createServerData(final Inventory playerInventory, final BlockPos shopPos) {
        return new ContainerData() {
            private int selectedIndex = 0;

            private ShopTradeEntry getSelectedTrade() {
                if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
                    return null;
                }

                ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
                if (shop == null || shop.getTrades().isEmpty()) {
                    selectedIndex = 0;
                    return null;
                }

                if (selectedIndex < 0) {
                    selectedIndex = 0;
                }

                if (selectedIndex >= shop.getTrades().size()) {
                    selectedIndex = shop.getTrades().size() - 1;
                }

                return shop.getTrades().get(selectedIndex);
            }

            private ShopRecord getShop() {
                if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
                    return null;
                }

                return SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
            }

            @Override
            public int get(int index) {
                ShopRecord shop = getShop();
                ShopTradeEntry trade = getSelectedTrade();

                if (index == DATA_SELECTED_INDEX) {
                    return selectedIndex;
                }

                long balance = shop == null ? 0L : shop.getBalance();

                if (index == DATA_BALANCE_LOW) {
                    return (int) (balance & 0xFFFFFFFFL);
                }

                if (index == DATA_BALANCE_HIGH) {
                    return (int) ((balance >>> 32) & 0xFFFFFFFFL);
                }

                long sellPrice = trade == null ? 0L : trade.getEffectiveSellPrice();
                long buyPrice = trade == null ? 0L : trade.getEffectiveBuyPrice();

                if (index == DATA_SELL_LOW) {
                    return (int) (sellPrice & 0xFFFFFFFFL);
                }

                if (index == DATA_SELL_HIGH) {
                    return (int) ((sellPrice >>> 32) & 0xFFFFFFFFL);
                }

                if (index == DATA_BUY_LOW) {
                    return (int) (buyPrice & 0xFFFFFFFFL);
                }

                if (index == DATA_BUY_HIGH) {
                    return (int) ((buyPrice >>> 32) & 0xFFFFFFFFL);
                }

                if (index == DATA_MODE) {
                    return trade == null ? PriceMode.FIXED.ordinal() : trade.getPriceMode().ordinal();
                }

                if (index == DATA_FLAGS) {
                    int flags = 0;
                    if (trade != null) {
                        if (trade.isEnabled()) {
                            flags |= 1;
                        }
                        if (trade.canSellToPlayer()) {
                            flags |= 2;
                        }
                        if (trade.canBuyFromPlayer()) {
                            flags |= 4;
                        }
                    }
                    return flags;
                }

                if (index == DATA_SELL_BATCH) {
                    return trade == null ? 0 : trade.getSellBatchSize();
                }

                if (index == DATA_BUY_BATCH) {
                    return trade == null ? 0 : trade.getBuyBatchSize();
                }

                if (index == DATA_CAN_MANAGE) {
                    if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
                        return 0;
                    }
                    return ShopService.canManageShopAt(serverPlayer, shopPos) ? 1 : 0;
                }

                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_SELECTED_INDEX) {
                    this.selectedIndex = value;
                }
            }

            @Override
            public int getCount() {
                return 12;
            }
        };
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 140;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 198;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, startX + column * 18, hotbarY));
        }
    }

    public String getShopName() {
        return shopName;
    }

    public List<ShopTradeView> getTradeViews() {
        return Collections.unmodifiableList(tradeViews);
    }

    public int getSelectedIndex() {
        int index = menuData.get(DATA_SELECTED_INDEX);
        if (index < 0) {
            return 0;
        }

        if (index >= tradeViews.size()) {
            return Math.max(0, tradeViews.size() - 1);
        }

        return index;
    }

    public ShopTradeView getSelectedTradeSnapshot() {
        if (tradeViews.isEmpty()) {
            return null;
        }

        return tradeViews.get(getSelectedIndex());
    }

    public long getBalance() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_BALANCE_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_BALANCE_HIGH));
        return low | (high << 32);
    }

    public long getLiveSelectedSellPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELL_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELL_HIGH));
        return low | (high << 32);
    }

    public long getLiveSelectedBuyPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_BUY_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_BUY_HIGH));
        return low | (high << 32);
    }

    public PriceMode getLiveSelectedMode() {
        int ordinal = menuData.get(DATA_MODE);
        PriceMode[] values = PriceMode.values();

        if (ordinal < 0 || ordinal >= values.length) {
            return PriceMode.FIXED;
        }

        return values[ordinal];
    }

    public boolean isLiveSelectedEnabled() {
        return (menuData.get(DATA_FLAGS) & 1) != 0;
    }

    public boolean canLiveSelectedSellToPlayer() {
        return (menuData.get(DATA_FLAGS) & 2) != 0;
    }

    public boolean canLiveSelectedBuyFromPlayer() {
        return (menuData.get(DATA_FLAGS) & 4) != 0;
    }

    public int getLiveSelectedSellBatch() {
        return menuData.get(DATA_SELL_BATCH);
    }

    public int getLiveSelectedBuyBatch() {
        return menuData.get(DATA_BUY_BATCH);
    }

    public boolean canManage() {
        return menuData.get(DATA_CAN_MANAGE) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        int tradeCount = getLiveTradeCountOrSnapshot(player);

        if (buttonId == BUTTON_PREV) {
            int newIndex = Math.max(0, getSelectedIndex() - 1);
            menuData.set(DATA_SELECTED_INDEX, newIndex);
            return true;
        }

        if (buttonId == BUTTON_NEXT) {
            if (tradeCount <= 0) {
                menuData.set(DATA_SELECTED_INDEX, 0);
                return true;
            }

            int newIndex = Math.min(tradeCount - 1, getSelectedIndex() + 1);
            menuData.set(DATA_SELECTED_INDEX, newIndex);
            return true;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        try {
            if (buttonId == BUTTON_BUY) {
                ShopService.buyFromShopAt(serverPlayer, shopPos, getSelectedIndex() + 1);
                return true;
            }

            if (buttonId == BUTTON_SELL) {
                ShopService.sellToShopAt(serverPlayer, shopPos, getSelectedIndex() + 1);
                return true;
            }

            if (buttonId == BUTTON_MANAGE) {
                ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
                if (shop == null) {
                    throw new IllegalStateException("Магазин не найден.");
                }

                if (!ShopService.canManageShopAt(serverPlayer, shopPos)) {
                    throw new IllegalStateException("У тебя нет доступа к управлению магазином.");
                }

                NetworkHooks.openScreen(
                        serverPlayer,
                        new SimpleMenuProvider(
                                (containerId, playerInventory, ignoredPlayer) -> new ShopManagementMenu(containerId, playerInventory, shopPos),
                                Component.literal("Управление магазином")
                        ),
                        buf -> ShopManagementMenu.writeOpenData(buf, shopPos, shop)
                );
                return true;
            }
        } catch (Exception e) {
            serverPlayer.displayClientMessage(Component.literal(e.getMessage()), true);
            return false;
        }

        return false;
    }

    private int getLiveTradeCountOrSnapshot(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return tradeViews.size();
        }

        ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
        return shop == null ? tradeViews.size() : shop.getTrades().size();
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