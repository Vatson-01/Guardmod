package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class SettlementMember {
    private final UUID playerUuid;
    private boolean leader;
    private SettlementPermissionSet permissionSet;

    private long joinTime;
    private long rejoinBlockedUntil;

    private long personalTaxDebt;
    private long personalTaxAmount;
    private int shopTaxPercent;

    public SettlementMember(UUID playerUuid, boolean leader, long joinTime) {
        this.playerUuid = playerUuid;
        this.leader = leader;
        this.joinTime = joinTime;
        this.permissionSet = new SettlementPermissionSet();
        this.personalTaxDebt = 0L;
        this.personalTaxAmount = 0L;
        this.shopTaxPercent = 0;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public SettlementPermissionSet getPermissionSet() {
        return permissionSet;
    }

    public void setPermissionSet(SettlementPermissionSet permissionSet) {
        this.permissionSet = permissionSet == null ? new SettlementPermissionSet() : permissionSet;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public long getRejoinBlockedUntil() {
        return rejoinBlockedUntil;
    }

    public void setRejoinBlockedUntil(long rejoinBlockedUntil) {
        this.rejoinBlockedUntil = rejoinBlockedUntil;
    }

    public long getPersonalTaxDebt() {
        return personalTaxDebt;
    }

    public void setPersonalTaxDebt(long personalTaxDebt) {
        this.personalTaxDebt = Math.max(0L, personalTaxDebt);
    }

    public void addPersonalTaxDebt(long amount) {
        if (amount <= 0L) {
            return;
        }
        this.personalTaxDebt += amount;
    }

    public long reducePersonalTaxDebt(long amount) {
        if (amount <= 0L) {
            return 0L;
        }

        long paid = Math.min(this.personalTaxDebt, amount);
        this.personalTaxDebt -= paid;
        return paid;
    }

    public long getPersonalTaxAmount() {
        return personalTaxAmount;
    }

    public void setPersonalTaxAmount(long personalTaxAmount) {
        this.personalTaxAmount = Math.max(0L, personalTaxAmount);
    }

    public int getShopTaxPercent() {
        return shopTaxPercent;
    }

    public void setShopTaxPercent(int shopTaxPercent) {
        this.shopTaxPercent = Math.max(0, Math.min(100, shopTaxPercent));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("PlayerUuid", playerUuid);
        tag.putBoolean("Leader", leader);
        tag.putLong("JoinTime", joinTime);
        tag.putLong("RejoinBlockedUntil", rejoinBlockedUntil);
        tag.putLong("PersonalTaxDebt", personalTaxDebt);
        tag.putLong("PersonalTaxAmount", personalTaxAmount);
        tag.putInt("ShopTaxPercent", shopTaxPercent);
        tag.put("PermissionSet", permissionSet.save());

        return tag;
    }

    public static SettlementMember load(CompoundTag tag) {
        UUID playerUuid = tag.getUUID("PlayerUuid");
        boolean leader = tag.getBoolean("Leader");
        long joinTime = tag.getLong("JoinTime");

        SettlementMember member = new SettlementMember(playerUuid, leader, joinTime);
        member.setRejoinBlockedUntil(tag.getLong("RejoinBlockedUntil"));
        member.setPersonalTaxDebt(tag.getLong("PersonalTaxDebt"));
        member.setPersonalTaxAmount(tag.contains("PersonalTaxAmount") ? tag.getLong("PersonalTaxAmount") : 0L);
        member.setShopTaxPercent(tag.getInt("ShopTaxPercent"));

        if (tag.contains("PermissionSet")) {
            member.setPermissionSet(SettlementPermissionSet.load(tag.getCompound("PermissionSet")));
        }

        return member;
    }
}