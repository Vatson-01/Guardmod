package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.service.MsmCompatService;
import com.settlements.service.WarService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MsmProjectileImpactEvents {
    private MsmProjectileImpactEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile == null) {
            return;
        }

        Level level = projectile.level();
        if (level.isClientSide()) {
            return;
        }

        if (!MsmCompatService.isMsmEntity(projectile)) {
            return;
        }

        HitResult hitResult = event.getRayTraceResult();
        if (!(hitResult instanceof BlockHitResult)) {
            return;
        }

        BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();

        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        Settlement defenderSettlement = data.getSettlementByChunk(level, new ChunkPos(hitPos));
        if (defenderSettlement == null) {
            return;
        }

        UUID attackerSettlementId = MsmCompatService.resolveAttackerSettlementId(level, projectile);
        if (attackerSettlementId == null) {
            event.setCanceled(true);
            projectile.discard();
            return;
        }

        if (!WarService.canAttackerBreakClaimedBlockByExplosion(
                level.getServer(),
                attackerSettlementId,
                defenderSettlement.getId()
        )) {
            event.setCanceled(true);
            projectile.discard();
        }
    }
}