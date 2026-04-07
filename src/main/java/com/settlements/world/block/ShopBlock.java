package com.settlements.world.block;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.ShopRecord;
import com.settlements.service.ShopService;
import com.settlements.service.WarService;
import com.settlements.world.blockentity.ShopBlockEntity;
import com.settlements.world.menu.ShopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class ShopBlock extends BaseEntityBlock {
    public ShopBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShopBlockEntity shopBlockEntity)) {
            return InteractionResult.PASS;
        }

        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        ShopRecord shop = data.getShopByPos(serverPlayer.level(), pos);
        if (shop == null) {
            serverPlayer.displayClientMessage(Component.literal("Этот блок магазина еще не зарегистрирован."), true);
            return InteractionResult.CONSUME;
        }

        if (isHostileSiegeAccess(serverPlayer, pos, data)) {
            serverPlayer.displayClientMessage(Component.literal("Во время осады вражеские магазины недоступны."), true);
            return InteractionResult.CONSUME;
        }

        if (serverPlayer.isShiftKeyDown()) {
            if (!ShopService.canManageShopAt(serverPlayer, pos)) {
                serverPlayer.displayClientMessage(
                        Component.literal("Открывать склад магазина может только владелец, глава или админ."),
                        true
                );
                return InteractionResult.CONSUME;
            }

            serverPlayer.openMenu((MenuProvider) shopBlockEntity);
            return InteractionResult.CONSUME;
        }

        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new ShopMenu(containerId, playerInventory, pos),
                        Component.literal(shop.getName())
                ),
                buf -> ShopMenu.writeOpenData(buf, pos, shop)
        );

        return InteractionResult.CONSUME;
    }

    private boolean isHostileSiegeAccess(ServerPlayer player, BlockPos pos, SettlementSavedData data) {
        Settlement targetSettlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));
        if (targetSettlement == null) {
            return false;
        }

        Settlement attackerSettlement = data.getSettlementByPlayer(player.getUUID());
        if (attackerSettlement == null) {
            return false;
        }

        if (attackerSettlement.getId().equals(targetSettlement.getId())) {
            return false;
        }

        return WarService.isActiveSiegeBetween(
                player.server,
                attackerSettlement.getId(),
                targetSettlement.getId()
        );
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShopBlockEntity shopBlockEntity) {
                shopBlockEntity.dropInventory();
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}