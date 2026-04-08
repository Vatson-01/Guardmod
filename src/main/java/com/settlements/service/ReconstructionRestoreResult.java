package com.settlements.service;

public class ReconstructionRestoreResult {
    private int restored;
    private int missingResources;
    private int blockedBySupport;
    private int occupied;
    private int otherBlocked;
    private int remainingPending;

    public int getRestored() {
        return restored;
    }

    public int getMissingResources() {
        return missingResources;
    }

    public int getBlockedBySupport() {
        return blockedBySupport;
    }

    public int getOccupied() {
        return occupied;
    }

    public int getOtherBlocked() {
        return otherBlocked;
    }

    public int getRemainingPending() {
        return remainingPending;
    }

    public void addRestored() {
        this.restored++;
    }

    public void addMissingResources() {
        this.missingResources++;
    }

    public void addBlockedBySupport() {
        this.blockedBySupport++;
    }

    public void addOccupied() {
        this.occupied++;
    }

    public void addOtherBlocked() {
        this.otherBlocked++;
    }

    public void setRemainingPending(int remainingPending) {
        this.remainingPending = remainingPending;
    }
}