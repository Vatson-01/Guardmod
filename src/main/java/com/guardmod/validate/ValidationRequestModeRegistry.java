package com.guardmod.validate;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ValidationRequestModeRegistry {
    public enum RequestMode {
        FULL,
        DYNAMIC_ASSETS
    }

    private static final ConcurrentMap<UUID, RequestMode> REQUEST_MODES =
            new ConcurrentHashMap<UUID, RequestMode>();

    private ValidationRequestModeRegistry() {
    }

    public static void setMode(ServerPlayer player, RequestMode mode) {
        if (player == null || mode == null) {
            return;
        }
        REQUEST_MODES.put(player.getUUID(), mode);
    }

    public static RequestMode getMode(ServerPlayer player) {
        if (player == null) {
            return RequestMode.FULL;
        }
        RequestMode mode = REQUEST_MODES.get(player.getUUID());
        return mode == null ? RequestMode.FULL : mode;
    }

    public static void clear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        REQUEST_MODES.remove(player.getUUID());
    }
}
