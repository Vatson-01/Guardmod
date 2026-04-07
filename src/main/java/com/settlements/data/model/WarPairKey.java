package com.settlements.data.model;

import java.util.Objects;
import java.util.UUID;

public final class WarPairKey {
    private final UUID firstSettlementId;
    private final UUID secondSettlementId;

    private WarPairKey(UUID firstSettlementId, UUID secondSettlementId) {
        this.firstSettlementId = firstSettlementId;
        this.secondSettlementId = secondSettlementId;
    }

    public static WarPairKey of(UUID settlementA, UUID settlementB) {
        if (settlementA == null || settlementB == null) {
            throw new IllegalArgumentException("Settlement ids must not be null.");
        }
        if (settlementA.equals(settlementB)) {
            throw new IllegalArgumentException("War pair requires two different settlements.");
        }

        if (compare(settlementA, settlementB) <= 0) {
            return new WarPairKey(settlementA, settlementB);
        }
        return new WarPairKey(settlementB, settlementA);
    }

    public UUID getFirstSettlementId() {
        return firstSettlementId;
    }

    public UUID getSecondSettlementId() {
        return secondSettlementId;
    }

    public boolean contains(UUID settlementId) {
        return firstSettlementId.equals(settlementId) || secondSettlementId.equals(settlementId);
    }

    private static int compare(UUID left, UUID right) {
        int msbCompare = Long.compare(left.getMostSignificantBits(), right.getMostSignificantBits());
        if (msbCompare != 0) {
            return msbCompare;
        }
        return Long.compare(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WarPairKey)) {
            return false;
        }
        WarPairKey other = (WarPairKey) obj;
        return Objects.equals(firstSettlementId, other.firstSettlementId)
                && Objects.equals(secondSettlementId, other.secondSettlementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstSettlementId, secondSettlementId);
    }

    @Override
    public String toString() {
        return firstSettlementId + "|" + secondSettlementId;
    }
}