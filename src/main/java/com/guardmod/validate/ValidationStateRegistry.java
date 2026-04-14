package com.guardmod.validate;

import com.guardmod.model.ClientEnvironmentReport;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ValidationStateRegistry {
    private static final AtomicLong REQUEST_IDS = new AtomicLong(1L);
    private static final Map<UUID, ValidationState> STATES = new ConcurrentHashMap<UUID, ValidationState>();

    private ValidationStateRegistry() {
    }

    public static ValidationTicket createPending(ServerPlayer player, long deadlineEpochMillis) {
        long requestId = REQUEST_IDS.getAndIncrement();
        ValidationState state = new ValidationState(requestId, deadlineEpochMillis, false, null);
        STATES.put(player.getUUID(), state);
        return new ValidationTicket(requestId, deadlineEpochMillis);
    }

    public static boolean isRequestExpected(ServerPlayer player, long requestId) {
        ValidationState state = STATES.get(player.getUUID());
        return state != null && !state.validated && state.requestId == requestId;
    }

    public static void markValidated(ServerPlayer player, ClientEnvironmentReport report) {
        ValidationState state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        STATES.put(player.getUUID(), new ValidationState(state.requestId, state.deadlineEpochMillis, true, report));
    }

    public static boolean isPendingTimeout(ServerPlayer player, long nowEpochMillis) {
        ValidationState state = STATES.get(player.getUUID());
        return state != null && !state.validated && nowEpochMillis > state.deadlineEpochMillis;
    }

    public static boolean isValidated(ServerPlayer player) {
        ValidationState state = STATES.get(player.getUUID());
        return state != null && state.validated;
    }

    public static ClientEnvironmentReport getLastReport(ServerPlayer player) {
        ValidationState state = STATES.get(player.getUUID());
        return state == null ? null : state.report;
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static final class ValidationTicket {
        private final long requestId;
        private final long deadlineEpochMillis;

        private ValidationTicket(long requestId, long deadlineEpochMillis) {
            this.requestId = requestId;
            this.deadlineEpochMillis = deadlineEpochMillis;
        }

        public long getRequestId() {
            return requestId;
        }

        public long getDeadlineEpochMillis() {
            return deadlineEpochMillis;
        }
    }

    private static final class ValidationState {
        private final long requestId;
        private final long deadlineEpochMillis;
        private final boolean validated;
        private final ClientEnvironmentReport report;

        private ValidationState(long requestId, long deadlineEpochMillis, boolean validated, ClientEnvironmentReport report) {
            this.requestId = requestId;
            this.deadlineEpochMillis = deadlineEpochMillis;
            this.validated = validated;
            this.report = report;
        }
    }
}
