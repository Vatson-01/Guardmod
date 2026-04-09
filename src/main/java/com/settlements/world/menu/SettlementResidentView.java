package com.settlements.world.menu;

import com.settlements.data.model.SettlementPermission;
import net.minecraft.network.FriendlyByteBuf;

public class SettlementResidentView {
    private final String displayName;
    private final String playerUuid;
    private final boolean leader;
    private final int permissionCount;
    private final long permissionsMask;
    private final long personalTaxAmount;
    private final long personalDebt;
    private final int shopTaxPercent;

    public SettlementResidentView(
            String displayName,
            String playerUuid,
            boolean leader,
            int permissionCount,
            long permissionsMask,
            long personalTaxAmount,
            long personalDebt,
            int shopTaxPercent
    ) {
        this.displayName = displayName;
        this.playerUuid = playerUuid;
        this.leader = leader;
        this.permissionCount = permissionCount;
        this.permissionsMask = permissionsMask;
        this.personalTaxAmount = personalTaxAmount;
        this.personalDebt = personalDebt;
        this.shopTaxPercent = shopTaxPercent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public boolean isLeader() {
        return leader;
    }

    public int getPermissionCount() {
        return permissionCount;
    }

    public long getPermissionsMask() {
        return permissionsMask;
    }

    public boolean hasPermission(SettlementPermission permission) {
        if (permission == null) {
            return false;
        }
        int ordinal = permission.ordinal();
        if (ordinal < 0 || ordinal >= Long.SIZE) {
            return false;
        }
        return (permissionsMask & (1L << ordinal)) != 0L;
    }

    public long getPersonalTaxAmount() {
        return personalTaxAmount;
    }

    public long getPersonalDebt() {
        return personalDebt;
    }

    public int getShopTaxPercent() {
        return shopTaxPercent;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(displayName);
        buf.writeUtf(playerUuid);
        buf.writeBoolean(leader);
        buf.writeInt(permissionCount);
        buf.writeLong(permissionsMask);
        buf.writeLong(personalTaxAmount);
        buf.writeLong(personalDebt);
        buf.writeInt(shopTaxPercent);
    }

    public static SettlementResidentView read(FriendlyByteBuf buf) {
        return new SettlementResidentView(
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readInt()
        );
    }
}
