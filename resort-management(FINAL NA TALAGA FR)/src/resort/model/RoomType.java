package resort.model;

public enum RoomType {
    STANDARD("Standard", 2),
    DELUXE("Deluxe", 4),
    FAMILY("Family", 6),
    SUITE("Suite", 6),
    VILLA("Villa", 8),
    PAVILION("Pavilion", 20);

    private final String displayName;
    private final int standardCapacity;

    RoomType(String displayName, int standardCapacity) {
        this.displayName = displayName;
        this.standardCapacity = standardCapacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStandardCapacity() {
        return standardCapacity;
    }

    public static RoomType fromDisplayName(String value) {
        if (value != null) {
            for (RoomType type : values()) {
                if (type.displayName.equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + value);
    }
}
