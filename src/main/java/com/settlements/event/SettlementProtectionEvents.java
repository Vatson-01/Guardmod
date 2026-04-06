package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.registry.ModBlocks;
import com.settlements.service.PermissionService;
import com.settlements.service.ProtectedAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SettlementProtectionEvents {
    private SettlementProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!PermissionService.canPerform(player, event.getPos(), ProtectedAction.BREAK_BLOCK)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Ты не можешь ломать блоки на этой территории."), true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!PermissionService.canPerform(player, event.getPos(), ProtectedAction.PLACE_BLOCK)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Ты не можешь ставить блоки на этой территории."), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ProtectedAction action = resolveAction(player, event.getPos());
        if (action == null) {
            return;
        }

        if (!PermissionService.canPerform(player, event.getPos(), action)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            player.displayClientMessage(Component.literal("У тебя нет доступа к этому участку."), true);
        }
    }

    private static ProtectedAction resolveAction(ServerPlayer player, BlockPos pos) {
        Block block = player.level().getBlockState(pos).getBlock();

        if (block == ModBlocks.SHOP_BLOCK.get()) {
            return null;
        }

        if (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock) {
            return ProtectedAction.OPEN_DOOR;
        }

        if (block instanceof ButtonBlock || block instanceof LeverBlock) {
            return ProtectedAction.USE_REDSTONE;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity != null) {
            if (blockEntity instanceof MenuProvider) {
                return ProtectedAction.OPEN_CONTAINER;
            }

            if (blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                return ProtectedAction.OPEN_CONTAINER;
            }
        }

        return null;
    }
}