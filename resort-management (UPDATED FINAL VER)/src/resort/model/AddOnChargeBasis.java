package resort.model;

public enum AddOnChargeBasis {
    PER_BOOKING("per booking"),
    PER_PERSON("per guest");

    private final String displayName;

    AddOnChargeBasis(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
