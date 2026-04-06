package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PlotPermission;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementPlot;
import com.settlements.util.ClaimKeyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class PermissionService {
    private PermissionService() {
    }

    public static boolean canPerform(ServerPlayer player, BlockPos pos, ProtectedAction action) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        ChunkPos chunkPos = new ChunkPos(pos);
        Settlement settlement = data.getSettlementByChunk(player.level(), chunkPos);

        if (settlement == null) {
            return true;
        }

        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        String chunkKey = ClaimKeyUtil.toKey(player.level().dimension(), chunkPos);
        SettlementPlot plot = data.getPlotByChunkKey(chunkKey);

        if (plot != null) {
            if (plot.isOwner(player.getUUID())) {
                return true;
            }

            PlotPermission requiredPermission = mapAction(action);
            return plot.hasPermission(player.getUUID(), requiredPermission);
        }

        return settlement.isResident(player.getUUID());
    }

    private static PlotPermission mapAction(ProtectedAction action) {
        switch (action) {
            case PLACE_BLOCK:
                return PlotPermission.BUILD;
            case BREAK_BLOCK:
                return PlotPermission.BREAK;
            case OPEN_DOOR:
                return PlotPermission.OPEN_DOORS;
            case USE_REDSTONE:
                return PlotPermission.USE_REDSTONE;
            case OPEN_CONTAINER:
            default:
                return PlotPermission.OPEN_CONTAINERS;
        }
    }
}