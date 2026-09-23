package io.github.andis382.installbook.visits;

public enum ServiceKind {
    ANNUAL_SERVICE,
    REPAIR,
    INSPECTION,
    WARRANTY_CLAIM;

    /**
     * Whether this visit counts as the unit's periodic service and so restarts the cycle.
     * A repair or a warranty call-out fixes one fault; the yearly check is still owed.
     */
    public boolean restartsCycle() {
        return this == ANNUAL_SERVICE || this == INSPECTION;
    }
}
