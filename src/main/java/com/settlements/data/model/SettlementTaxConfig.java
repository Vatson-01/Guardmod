package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;

public class SettlementTaxConfig {
    private long landTaxPerClaimedChunk;
    private long residentTaxPerResident;

    public SettlementTaxConfig() {
        this.landTaxPerClaimedChunk = 0L;
        this.residentTaxPerResident = 0L;
    }

    public long getLandTaxPerClaimedChunk() {
        return landTaxPerClaimedChunk;
    }

    public void setLandTaxPerClaimedChunk(long landTaxPerClaimedChunk) {
        this.landTaxPerClaimedChunk = Math.max(0L, landTaxPerClaimedChunk);
    }

    public long getResidentTaxPerResident() {
        return residentTaxPerResident;
    }

    public void setResidentTaxPerResident(long residentTaxPerResident) {
        this.residentTaxPerResident = Math.max(0L, residentTaxPerResident);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LandTaxPerClaimedChunk", landTaxPerClaimedChunk);
        tag.putLong("ResidentTaxPerResident", residentTaxPerResident);
        return tag;
    }

    public static SettlementTaxConfig load(CompoundTag tag) {
        SettlementTaxConfig config = new SettlementTaxConfig();

        if (tag == null) {
            return config;
        }

        config.setLandTaxPerClaimedChunk(tag.getLong("LandTaxPerClaimedChunk"));
        config.setResidentTaxPerResident(tag.getLong("ResidentTaxPerResident"));

        return config;
    }
}