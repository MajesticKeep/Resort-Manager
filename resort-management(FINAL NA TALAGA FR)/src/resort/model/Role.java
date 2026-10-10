package resort.model;

import java.util.Locale;

public enum Role {
    ADMINISTRATOR("Administrator"),
    HOUSEKEEPING("Housekeeping"),
    MAINTENANCE("Repair Technician"),
    FRONT_DESK("Front Desk"),
    GUEST_SERVICES("Guest Services"),
    GENERAL_STAFF("Staff");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public StaffWorkflow createWorkflow() {
        switch (this) {
            case HOUSEKEEPING: return new HousekeepingWorkflow();
            case MAINTENANCE: return new MaintenanceWorkflow();
            case FRONT_DESK: return new FrontDeskWorkflow();
            case GUEST_SERVICES: return new GuestServicesWorkflow();
            default: return new GeneralStaffWorkflow();
        }
    }

    public static Role fromLegacyName(String value) {
        if (value == null) {
            return GENERAL_STAFF;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "housekeeping": return HOUSEKEEPING;
            case "maintenance":
            case "repair technician": return MAINTENANCE;
            case "front desk": return FRONT_DESK;
            case "guest services": return GUEST_SERVICES;
            default: return GENERAL_STAFF;
        }
    }
}
