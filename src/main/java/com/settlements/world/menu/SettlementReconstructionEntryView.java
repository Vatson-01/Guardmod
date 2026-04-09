package com.settlements.world.menu;

import net.minecraft.network.FriendlyByteBuf;

public class SettlementReconstructionEntryView {
    private final int index;
    private final String requiredItemId;
    private final int requiredCount;
    private final String positionText;
    private final String dimensionText;
    private final boolean skipped;
    private final boolean restored;

    public SettlementReconstructionEntryView(
            int index,
            String requiredItemId,
            int requiredCount,
            String positionText,
            String dimensionText,
            boolean skipped,
            boolean restored
    ) {
        this.index = index;
        this.requiredItemId = requiredItemId;
        this.requiredCount = requiredCount;
        this.positionText = positionText;
        this.dimensionText = dimensionText;
        this.skipped = skipped;
        this.restored = restored;
    }

    public int getIndex() {
        return index;
    }

    public String getRequiredItemId() {
        return requiredItemId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public String getPositionText() {
        return positionText;
    }

    public String getDimensionText() {
        return dimensionText;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isRestored() {
        return restored;
    }

    public boolean isPending() {
        return !skipped && !restored;
    }

    public SettlementReconstructionEntryView withSkipped(boolean newSkipped) {
        return new SettlementReconstructionEntryView(
                index,
                requiredItemId,
                requiredCount,
                positionText,
                dimensionText,
                newSkipped,
                restored
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(index);
        buf.writeUtf(requiredItemId);
        buf.writeInt(requiredCount);
        buf.writeUtf(positionText);
        buf.writeUtf(dimensionText);
        buf.writeBoolean(skipped);
        buf.writeBoolean(restored);
    }

    public static SettlementReconstructionEntryView read(FriendlyByteBuf buf) {
        return new SettlementReconstructionEntryView(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
