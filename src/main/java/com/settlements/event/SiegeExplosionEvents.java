package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.registry.ModBlocks;
import com.settlements.service.MsmCompatService;
import com.settlements.service.WarService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SiegeExplosionEvents {
    private SiegeExplosionEvents() {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        Explosion explosion = event.getExplosion();
        SettlementSavedData data = SettlementSavedData.get(level.getServer());

        boolean isMsmExplosion = MsmCompatService.isMsmExplosion(explosion);
        UUID attackerSettlementId = isMsmExplosion
                ? MsmCompatService.resolveAttackerSettlementId(level, explosion)
                : null;

        List<BlockPos> affectedBlocks = event.getAffectedBlocks();
        Iterator<BlockPos> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (state.is(ModBlocks.SHOP_BLOCK.get())) {
                iterator.remove();
                continue;
            }

            Settlement defenderSettlement = data.getSettlementByChunk(level, new ChunkPos(pos));
            if (defenderSettlement == null) {
                continue;
            }

            if (!isMsmExplosion) {
                iterator.remove();
                continue;
            }

            if (attackerSettlementId == null) {
                iterator.remove();
                continue;
            }

            if (attackerSettlementId.equals(defenderSettlement.getId())) {
                iterator.remove();
                continue;
            }

            if (!WarService.canAttackerBreakClaimedBlockByExplosion(
                    level.getServer(),
                    attackerSettlementId,
                    defenderSettlement.getId()
            )) {
                iterator.remove();
            }
        }
    }
}