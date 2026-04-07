package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public class WarRecord {
    private final UUID id;
    private final UUID settlementAId;
    private final UUID settlementBId;

    private boolean active;

    private final long startedAt;
    private long updatedAt;
    private Long endedAt;

    private final UUID startedByAdmin;
    private UUID endedByAdmin;

    private final String startReason;
    private String endReason;

    private WarRecord(
            UUID id,
            UUID settlementAId,
            UUID settlementBId,
            boolean active,
            long startedAt,
            long updatedAt,
            Long endedAt,
            UUID startedByAdmin,
            UUID endedByAdmin,
            String startReason,
            String endReason
    ) {
        this.id = id;
        this.settlementAId = settlementAId;
        this.settlementBId = settlementBId;
        this.active = active;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
        this.endedAt = endedAt;
        this.startedByAdmin = startedByAdmin;
        this.endedByAdmin = endedByAdmin;
        this.startReason = sanitize(startReason);
        this.endReason = sanitize(endReason);
    }

    public static WarRecord createNew(
            UUID settlementAId,
            UUID settlementBId,
            long gameTime,
            UUID startedByAdmin,
            String startReason
    ) {
        return new WarRecord(
                UUID.randomUUID(),
                settlementAId,
                settlementBId,
                true,
                gameTime,
                gameTime,
                null,
                startedByAdmin,
                null,
                startReason,
                ""
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getSettlementAId() {
        return settlementAId;
    }

    public UUID getSettlementBId() {
        return settlementBId;
    }

    public boolean isActive() {
        return active;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Long getEndedAt() {
        return endedAt;
    }

    public UUID getStartedByAdmin() {
        return startedByAdmin;
    }

    public UUID getEndedByAdmin() {
        return endedByAdmin;
    }

    public String getStartReason() {
        return startReason;
    }

    public String getEndReason() {
        return endReason;
    }

    public boolean involvesSettlement(UUID settlementId) {
        return settlementAId.equals(settlementId) || settlementBId.equals(settlementId);
    }

    public boolean isBetween(UUID settlementA, UUID settlementB) {
        WarPairKey self = WarPairKey.of(settlementAId, settlementBId);
        WarPairKey other = WarPairKey.of(settlementA, settlementB);
        return self.equals(other);
    }

    public void close(long gameTime, UUID endedByAdmin, String endReason) {
        if (!active) {
            return;
        }
        this.active = false;
        this.endedAt = gameTime;
        this.endedByAdmin = endedByAdmin;
        this.endReason = sanitize(endReason);
        this.updatedAt = gameTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("SettlementAId", settlementAId);
        tag.putUUID("SettlementBId", settlementBId);
        tag.putBoolean("Active", active);
        tag.putLong("StartedAt", startedAt);
        tag.putLong("UpdatedAt", updatedAt);

        if (endedAt != null) {
            tag.putLong("EndedAt", endedAt.longValue());
        }
        if (startedByAdmin != null) {
            tag.putUUID("StartedByAdmin", startedByAdmin);
        }
        if (endedByAdmin != null) {
            tag.putUUID("EndedByAdmin", endedByAdmin);
        }

        tag.putString("StartReason", startReason);
        tag.putString("EndReason", endReason);
        return tag;
    }

    public static WarRecord load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID settlementAId = tag.getUUID("SettlementAId");
        UUID settlementBId = tag.getUUID("SettlementBId");
        boolean active = tag.getBoolean("Active");
        long startedAt = tag.getLong("StartedAt");
        long updatedAt = tag.contains("UpdatedAt", Tag.TAG_LONG) ? tag.getLong("UpdatedAt") : startedAt;
        Long endedAt = tag.contains("EndedAt", Tag.TAG_LONG) ? Long.valueOf(tag.getLong("EndedAt")) : null;
        UUID startedByAdmin = tag.hasUUID("StartedByAdmin") ? tag.getUUID("StartedByAdmin") : null;
        UUID endedByAdmin = tag.hasUUID("EndedByAdmin") ? tag.getUUID("EndedByAdmin") : null;
        String startReason = tag.getString("StartReason");
        String endReason = tag.getString("EndReason");

        return new WarRecord(
                id,
                settlementAId,
                settlementBId,
                active,
                startedAt,
                updatedAt,
                endedAt,
                startedByAdmin,
                endedByAdmin,
                startReason,
                endReason
        );
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}