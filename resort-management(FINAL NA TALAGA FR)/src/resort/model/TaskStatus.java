package resort.model;

public enum TaskStatus {
    PENDING,
    COMPLETED;

    public static TaskStatus fromLegacyValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown task status: " + value, ex);
        }
    }
}
