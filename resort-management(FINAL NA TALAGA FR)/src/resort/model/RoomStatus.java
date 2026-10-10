package resort.model;

public enum RoomStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    INSPECTION_REQUIRED,
    HOUSEKEEPING_REQUIRED,
    // Retained so previously saved data using this enum value still loads.
    MAINTENANCE,
    REPAIR_REQUIRED,
    ARCHIVED
}
