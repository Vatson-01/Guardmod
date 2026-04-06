package com.settlements.client.screen;

import com.settlements.world.menu.ShopManagementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ShopManagementScreen extends AbstractContainerScreen<ShopManagementMenu> {
    private Button toggleEnabledButton;
    private Button openStorageButton;

    private Button depositAllButton;
    private Button withdrawAllButton;
    private Button deposit10Button;
    private Button withdraw10Button;
    private Button deposit100Button;
    private Button withdraw100Button;
    private Button deposit1000Button;
    private Button withdraw1000Button;

    private Button infiniteStockButton;
    private Button infiniteBalanceButton;
    private Button indestructibleButton;

    public ShopManagementScreen(ShopManagementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 340;
        this.imageHeight = 260;
        this.inventoryLabelY = 165;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        toggleEnabledButton = addRenderableWidget(Button.builder(Component.literal("Вкл/Выкл"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_ENABLED))
                .bounds(left + 10, top + 28, 82, 20)
                .build());

        openStorageButton = addRenderableWidget(Button.builder(Component.literal("Склад"), b -> press(ShopManagementMenu.BUTTON_OPEN_STORAGE))
                .bounds(left + 98, top + 28, 82, 20)
                .build());

        depositAllButton = addRenderableWidget(Button.builder(Component.literal("+Все"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_ALL))
                .bounds(left + 10, top + 76, 70, 20)
                .build());

        withdrawAllButton = addRenderableWidget(Button.builder(Component.literal("-Все"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_ALL))
                .bounds(left + 90, top + 76, 70, 20)
                .build());

        deposit10Button = addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_10))
                .bounds(left + 10, top + 100, 70, 20)
                .build());

        withdraw10Button = addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_10))
                .bounds(left + 90, top + 100, 70, 20)
                .build());

        deposit100Button = addRenderableWidget(Button.builder(Component.literal("+100"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_100))
                .bounds(left + 10, top + 124, 70, 20)
                .build());

        withdraw100Button = addRenderableWidget(Button.builder(Component.literal("-100"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_100))
                .bounds(left + 90, top + 124, 70, 20)
                .build());

        deposit1000Button = addRenderableWidget(Button.builder(Component.literal("+1000"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_1000))
                .bounds(left + 10, top + 148, 70, 20)
                .build());

        withdraw1000Button = addRenderableWidget(Button.builder(Component.literal("-1000"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_1000))
                .bounds(left + 90, top + 148, 70, 20)
                .build());

        infiniteStockButton = addRenderableWidget(Button.builder(Component.literal("∞ Склад"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_STOCK))
                .bounds(left + 226, top + 32, 96, 20)
                .build());

        infiniteBalanceButton = addRenderableWidget(Button.builder(Component.literal("∞ Баланс"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_BALANCE))
                .bounds(left + 226, top + 58, 96, 20)
                .build());

        indestructibleButton = addRenderableWidget(Button.builder(Component.literal("Неразр."), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INDESTRUCTIBLE))
                .bounds(left + 226, top + 84, 96, 20)
                .build());

        updateButtons();
    }

    private void press(int buttonId) {
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
        boolean admin = menu.isAdminShop();

        infiniteStockButton.visible = admin;
        infiniteBalanceButton.visible = admin;
        indestructibleButton.visible = admin;

        infiniteStockButton.active = admin;
        infiniteBalanceButton.active = admin;
        indestructibleButton.active = admin;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF262626);

        graphics.fill(left + 4, top + 4, left + imageWidth - 4, top + 160, 0xFF373737);
        graphics.fill(left + 4, top + 170, left + 176, top + imageHeight - 4, 0xFF323232);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 7 + column * 18;
                int slotY = top + 176 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 7 + column * 18;
            int slotY = top + 234;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, "Управление: " + this.menu.getShopName(), 8, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, "Баланс: " + this.menu.getBalance(), 8, 18, 0xE0E0E0, false);
        graphics.drawString(this.font, "Активен: " + yesNo(this.menu.isEnabled()), 10, 54, 0xD8D8D8, false);

        graphics.drawString(this.font, "Пополнение", 10, 64, 0xFFFFFF, false);
        graphics.drawString(this.font, "Вывод", 106, 64, 0xFFFFFF, false);

        if (this.menu.isAdminShop()) {
            graphics.drawString(this.font, "Тип: Админ-магазин", 226, 12, 0xFFD080, false);
            graphics.drawString(this.font, "∞ Склад: " + yesNo(this.menu.isInfiniteStock()), 226, 114, 0xD8D8D8, false);
            graphics.drawString(this.font, "∞ Баланс: " + yesNo(this.menu.isInfiniteBalance()), 226, 126, 0xD8D8D8, false);
            graphics.drawString(this.font, "Неразр.: " + yesNo(this.menu.isIndestructible()), 226, 138, 0xD8D8D8, false);
        } else {
            graphics.drawString(this.font, "Тип: Магазин игрока", 226, 12, 0xA8FFA8, false);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private String yesNo(boolean value) {
        return value ? "Да" : "Нет";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}