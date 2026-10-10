package resort.model;

public enum TaskType {
    CLEAN_AND_PREPARE_ROOM("Clean and prepare a room"),
    RESTOCK_ROOM_SUPPLIES("Restock room supplies"),
    INSPECT_A_ROOM("Inspect a room"),
    UPDATE_ROOM_READINESS("Update room readiness"),
    REPAIR_DAMAGE_IN_A_ROOM("Repair damage in a room"),
    INSPECT_ROOM_FIXTURES("Inspect room fixtures"),
    REPORT_MAINTENANCE_COMPLETED("Report maintenance completed"),
    PREPARE_GUEST_CHECK_IN("Prepare a guest check-in"),
    PROCESS_GUEST_CHECK_OUT("Process a guest check-out"),
    CONFIRM_RESERVATION("Confirm a reservation"),
    RESPOND_TO_BOOKING_INQUIRY("Respond to a booking inquiry"),
    DELIVER_GUEST_REQUEST("Deliver a guest request"),
    ARRANGE_RESORT_ACTIVITY("Arrange a resort activity"),
    COORDINATE_ROOM_AMENITY("Coordinate a room amenity"),
    RESOLVE_GUEST_CONCERN("Resolve a guest concern"),
    COMPLETE_ASSIGNED_WORK("Complete assigned work"),
    ASSIST_RESORT_OPERATIONS("Assist resort operations"),
    CUSTOM_TASK("Custom task");

    private final String displayName;

    TaskType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
