package com.guardmod.validate;

public class ValidationResult {
    private final boolean success;
    private final String reason;
    private final ValidationStage failedStage;

    private ValidationResult(boolean success, String reason, ValidationStage failedStage) {
        this.success = success;
        this.reason = reason;
        this.failedStage = failedStage;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, "", null);
    }

    public static ValidationResult failure(String reason, ValidationStage failedStage) {
        return new ValidationResult(false, reason, failedStage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public ValidationStage getFailedStage() {
        return failedStage;
    }
}
