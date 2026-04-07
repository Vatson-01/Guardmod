package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.registry.ModBlocks;
import com.settlements.service.WarService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
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

        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        UUID attackerSettlementId = resolveExplosionSettlementId(level, event.getExplosion(), data);

        Iterator<BlockPos> iterator = event.getAffectedBlocks().iterator();
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

    private static UUID resolveExplosionSettlementId(Level level, Explosion explosion, SettlementSavedData data) {
        if (explosion == null) {
            return null;
        }

        LivingEntity indirect = explosion.getIndirectSourceEntity();
        if (indirect instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) indirect;
            Settlement settlement = data.getSettlementByPlayer(serverPlayer.getUUID());
            return settlement == null ? null : settlement.getId();
        }

        Entity direct = explosion.getDirectSourceEntity();
        if (direct == null) {
            return null;
        }

        if (direct instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) direct;
            Settlement settlement = data.getSettlementByPlayer(serverPlayer.getUUID());
            return settlement == null ? null : settlement.getId();
        }

        if (direct instanceof Projectile) {
            Projectile projectile = (Projectile) direct;
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) owner;
                Settlement settlement = data.getSettlementByPlayer(serverPlayer.getUUID());
                return settlement == null ? null : settlement.getId();
            }
        }

        if (direct instanceof PrimedTnt) {
            PrimedTnt primedTnt = (PrimedTnt) direct;
            LivingEntity owner = primedTnt.getOwner();
            if (owner instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) owner;
                Settlement settlement = data.getSettlementByPlayer(serverPlayer.getUUID());
                return settlement == null ? null : settlement.getId();
            }
        }

        return null;
    }
}