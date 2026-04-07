package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public class SiegeState {
    private final UUID id;
    private final UUID warId;

    private final UUID attackerSettlementId;
    private final UUID defenderSettlementId;

    private boolean active;

    private final long startedAt;
    private long updatedAt;
    private Long endedAt;

    private final UUID startedByAdmin;
    private UUID endedByAdmin;

    private final String startReason;
    private String endReason;

    public SiegeState(
            UUID id,
            UUID warId,
            UUID attackerSettlementId,
            UUID defenderSettlementId,
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
        this.warId = warId;
        this.attackerSettlementId = attackerSettlementId;
        this.defenderSettlementId = defenderSettlementId;
        this.active = active;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
        this.endedAt = endedAt;
        this.startedByAdmin = startedByAdmin;
        this.endedByAdmin = endedByAdmin;
        this.startReason = sanitize(startReason);
        this.endReason = sanitize(endReason);
    }

    public static SiegeState createNew(
            UUID warId,
            UUID attackerSettlementId,
            UUID defenderSettlementId,
            long gameTime,
            UUID startedByAdmin,
            String startReason
    ) {
        return new SiegeState(
                UUID.randomUUID(),
                warId,
                attackerSettlementId,
                defenderSettlementId,
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

    public UUID getWarId() {
        return warId;
    }

    public UUID getAttackerSettlementId() {
        return attackerSettlementId;
    }

    public UUID getDefenderSettlementId() {
        return defenderSettlementId;
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

    public boolean isDirection(UUID attackerSettlementId, UUID defenderSettlementId) {
        return this.attackerSettlementId.equals(attackerSettlementId)
                && this.defenderSettlementId.equals(defenderSettlementId);
    }

    public boolean involvesSettlement(UUID settlementId) {
        return attackerSettlementId.equals(settlementId) || defenderSettlementId.equals(settlementId);
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
        tag.putUUID("WarId", warId);
        tag.putUUID("AttackerSettlementId", attackerSettlementId);
        tag.putUUID("DefenderSettlementId", defenderSettlementId);
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

    public static SiegeState load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID warId = tag.getUUID("WarId");
        UUID attackerSettlementId = tag.getUUID("AttackerSettlementId");
        UUID defenderSettlementId = tag.getUUID("DefenderSettlementId");
        boolean active = tag.getBoolean("Active");
        long startedAt = tag.getLong("StartedAt");
        long updatedAt = tag.contains("UpdatedAt", Tag.TAG_LONG) ? tag.getLong("UpdatedAt") : startedAt;
        Long endedAt = tag.contains("EndedAt", Tag.TAG_LONG) ? Long.valueOf(tag.getLong("EndedAt")) : null;
        UUID startedByAdmin = tag.hasUUID("StartedByAdmin") ? tag.getUUID("StartedByAdmin") : null;
        UUID endedByAdmin = tag.hasUUID("EndedByAdmin") ? tag.getUUID("EndedByAdmin") : null;
        String startReason = tag.getString("StartReason");
        String endReason = tag.getString("EndReason");

        return new SiegeState(
                id,
                warId,
                attackerSettlementId,
                defenderSettlementId,
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