package com.settlements.client.screen;

import com.settlements.data.model.PriceMode;
import com.settlements.world.menu.ShopMenu;
import com.settlements.world.menu.ShopTradeView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private Button prevButton;
    private Button nextButton;
    private Button buyButton;
    private Button sellButton;
    private Button manageButton;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.imageHeight = 222;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        this.prevButton = Button.builder(Component.literal("<"), button -> pressButton(ShopMenu.BUTTON_PREV))
                .bounds(left + 10, top + 18, 20, 20)
                .build();

        this.nextButton = Button.builder(Component.literal(">"), button -> pressButton(ShopMenu.BUTTON_NEXT))
                .bounds(left + 82, top + 18, 20, 20)
                .build();

        this.buyButton = Button.builder(Component.literal("Купить"), button -> pressButton(ShopMenu.BUTTON_BUY))
                .bounds(left + 166, top + 96, 46, 20)
                .build();

        this.sellButton = Button.builder(Component.literal("Продать"), button -> pressButton(ShopMenu.BUTTON_SELL))
                .bounds(left + 218, top + 96, 50, 20)
                .build();

        this.manageButton = Button.builder(Component.literal("Упр."), button -> pressButton(ShopMenu.BUTTON_MANAGE))
                .bounds(left + 218, top + 18, 50, 20)
                .build();

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(buyButton);
        this.addRenderableWidget(sellButton);
        this.addRenderableWidget(manageButton);

        updateButtons();
    }

    private void pressButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        ShopTradeView trade = menu.getSelectedTradeSnapshot();
        boolean hasTrades = trade != null;

        prevButton.active = hasTrades && menu.getSelectedIndex() > 0;
        nextButton.active = hasTrades && menu.getSelectedIndex() < menu.getTradeViews().size() - 1;
        buyButton.active = hasTrades && menu.canLiveSelectedSellToPlayer() && menu.isLiveSelectedEnabled();
        sellButton.active = hasTrades && menu.canLiveSelectedBuyFromPlayer() && menu.isLiveSelectedEnabled();

        manageButton.visible = menu.canManage();
        manageButton.active = menu.canManage();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF262626);

        graphics.fill(left + 4, top + 4, left + 110, top + 126, 0xFF373737);
        graphics.fill(left + 114, top + 4, left + imageWidth - 4, top + 126, 0xFF373737);
        graphics.fill(left + 4, top + 132, left + 176, top + imageHeight - 4, 0xFF323232);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 7 + column * 18;
                int slotY = top + 139 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 7 + column * 18;
            int slotY = top + 197;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }

        renderSelectedTradeIcon(graphics, left + 150, top + 30);
    }

    private void renderSelectedTradeIcon(GuiGraphics graphics, int x, int y) {
        ShopTradeView trade = menu.getSelectedTradeSnapshot();
        if (trade == null) {
            return;
        }

        Item item = resolveItem(trade.getItemId());
        if (item == null) {
            return;
        }

        graphics.renderItem(new ItemStack(item), x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.menu.getShopName(), 8, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, "Баланс магазина: " + this.menu.getBalance(), 114, 6, 0xE0E0E0, false);

        ShopTradeView selected = menu.getSelectedTradeSnapshot();
        if (selected == null) {
            graphics.drawString(this.font, "Сделок нет", 20, 50, 0xFFAAAA, false);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
            return;
        }

        graphics.drawString(
                this.font,
                "Сделка " + (menu.getSelectedIndex() + 1) + "/" + menu.getTradeViews().size(),
                36,
                24,
                0xFFFFFF,
                false
        );

        int listStart = Math.max(0, menu.getSelectedIndex() - 4);
        int listEnd = Math.min(menu.getTradeViews().size(), listStart + 9);
        int y = 38;

        for (int i = listStart; i < listEnd; i++) {
            ShopTradeView view = menu.getTradeViews().get(i);
            int color = i == menu.getSelectedIndex() ? 0xFFFF66 : 0xDDDDDD;
            graphics.drawString(
                    this.font,
                    "#" + (i + 1) + " " + shorten(resolveItemName(view.getItemId()), 12),
                    10,
                    y,
                    color,
                    false
            );
            y += 10;
        }

        String itemName = resolveItemName(selected.getItemId());
        graphics.drawString(this.font, shorten(itemName, 18), 174, 34, 0xFFFFFF, false);
        graphics.drawString(this.font, "Режим: " + translateMode(menu.getLiveSelectedMode()), 140, 52, 0xD8D8D8, false);

        if (menu.canLiveSelectedSellToPlayer()) {
            graphics.drawString(
                    this.font,
                    "Покупка x" + menu.getLiveSelectedSellBatch() + " за " + menu.getLiveSelectedSellPrice(),
                    136,
                    70,
                    0xA8FFA8,
                    false
            );
        }

        if (menu.canLiveSelectedBuyFromPlayer()) {
            graphics.drawString(
                    this.font,
                    "Продажа x" + menu.getLiveSelectedBuyBatch() + " за " + menu.getLiveSelectedBuyPrice(),
                    136,
                    82,
                    0xA8D8FF,
                    false
            );
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private String translateMode(PriceMode mode) {
        if (mode == PriceMode.DYNAMIC) {
            return "Динамический";
        }

        return "Фиксированный";
    }

    private Item resolveItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return null;
        }

        return ForgeRegistries.ITEMS.getValue(id);
    }

    private String resolveItemName(String itemId) {
        Item item = resolveItem(itemId);
        if (item == null) {
            return itemId;
        }

        return item.getDescription().getString();
    }

    private String shorten(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}