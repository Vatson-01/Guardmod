package com.guardmod.validate;

import com.guardmod.model.ClientEnvironmentReport;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClientEnvironmentReportCache {
    private static final ConcurrentMap<UUID, ClientEnvironmentReport> REPORTS =
            new ConcurrentHashMap<UUID, ClientEnvironmentReport>();

    private ClientEnvironmentReportCache() {
    }

    public static ClientEnvironmentReport get(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return REPORTS.get(player.getUUID());
    }

    public static void put(ServerPlayer player, ClientEnvironmentReport report) {
        if (player == null || report == null) {
            return;
        }
        REPORTS.put(player.getUUID(), report);
    }

    public static void remove(ServerPlayer player) {
        if (player == null) {
            return;
        }
        REPORTS.remove(player.getUUID());
    }
}
