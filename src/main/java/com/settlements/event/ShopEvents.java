package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.registry.ModBlocks;
import com.settlements.service.ShopService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShopEvents {
    private ShopEvents() {
    }

    @SubscribeEvent
    public static void onPlaceShop(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!event.getPlacedBlock().is(ModBlocks.SHOP_BLOCK.get())) {
            return;
        }

        if (player.hasPermissions(2) && player.isCreative()) {
            try {
                ShopService.initializePlacedPlayerShop(player, event.getPos());
            } catch (Exception ignored) {
                // Для админов в креативе не отменяем установку, если это не обычный магазин.
                // Потом можно будет превратить блок в админ-магазин.
            }
            return;
        }

        try {
            ShopService.initializePlacedPlayerShop(player, event.getPos());
        } catch (Exception e) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(e.getMessage()), true);
        }
    }

    @SubscribeEvent
    public static void onBreakShop(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!event.getState().is(ModBlocks.SHOP_BLOCK.get())) {
            return;
        }

        try {
            ShopService.handlePlayerBreakShop(player, event.getPos());
        } catch (Exception e) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(e.getMessage()), true);
        }
    }
}