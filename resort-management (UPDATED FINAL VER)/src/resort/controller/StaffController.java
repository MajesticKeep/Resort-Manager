package resort.controller;

import java.util.ArrayList;
import java.util.List;

import resort.model.AddOn;
import resort.model.AddOnCategory;
import resort.model.Booking;
import resort.model.BookingStatus;
import resort.model.DataManager;
import resort.model.Room;
import resort.model.RoomStatus;
import resort.model.Staff;
import resort.model.TaskType;

public final class StaffController {

    public enum RoomAction {
        INSPECTION_PASSED("Inspection complete - no damage; send for cleaning"),
        DAMAGE_FOUND("Damage found - route to maintenance"),
        REPAIR_COMPLETED("Maintenance complete - send for cleaning"),
        CLEANING_COMPLETED("Cleaning complete - mark ready for guests");

        private final String displayName;

        RoomAction(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static final class TaskTarget {
        private final String label;
        private final String details;

        private TaskTarget(String label, String details) {
            this.label = label;
            this.details = details;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final DataManager dataManager;

    public StaffController(DataManager dataManager) {
        if (dataManager == null) {
            throw new IllegalArgumentException("Data manager is required.");
        }
        this.dataManager = dataManager;
    }

    public List<TaskTarget> getTaskTargets(Staff staff, TaskType taskType) {
        if (staff == null || taskType == null) {
            throw new IllegalArgumentException("Staff and task type are required.");
        }
        List<TaskTarget> targets = new ArrayList<>();
        if (staff.isHousekeeping() || staff.isMaintenance()) {
            for (Room room : dataManager.getAllRooms()) {
                if (isRoomRelevant(staff, taskType, room)) {
                    targets.add(new TaskTarget("Room " + room.getRoomId()
                            + " - " + room.getStatusLabel(),
                            "Room " + room.getRoomId()));
                }
            }
            return targets;
        }

        for (Booking booking : dataManager.getAllBookings()) {
            if (booking.getStatus() != BookingStatus.RESERVED
                    && booking.getStatus() != BookingStatus.CHECKED_IN) {
                continue;
            }
            if (staff.isGuestServices()) {
                addGuestServiceTargets(targets, taskType, booking);
            } else {
                targets.add(bookingTarget(booking, null));
            }
        }
        return targets;
    }

    public List<RoomAction> getAvailableRoomActions(Staff staff, Room room) {
        if (staff == null || room == null || room.getStatus() == RoomStatus.ARCHIVED
                || room.getStatus() == RoomStatus.OCCUPIED) {
            return List.of();
        }
        List<RoomAction> actions = new ArrayList<>();
        if (room.getStatus() == RoomStatus.INSPECTION_REQUIRED
                && staff.isHousekeeping()) {
            actions.add(RoomAction.INSPECTION_PASSED);
            actions.add(RoomAction.DAMAGE_FOUND);
        } else if (room.getStatus() == RoomStatus.REPAIR_REQUIRED
                && staff.isMaintenance()) {
            actions.add(RoomAction.REPAIR_COMPLETED);
        } else if ((room.getStatus() == RoomStatus.HOUSEKEEPING_REQUIRED
                || room.getStatus() == RoomStatus.MAINTENANCE)
                && staff.isHousekeeping()) {
            actions.add(RoomAction.CLEANING_COMPLETED);
        }
        return List.copyOf(actions);
    }

    public void performRoomAction(Staff staff, Room room, RoomAction action) {
        if (action == null || !getAvailableRoomActions(staff, room).contains(action)) {
            throw new IllegalStateException(
                    "This staff member cannot perform that action on the selected room.");
        }
        switch (action) {
            case INSPECTION_PASSED:
                dataManager.completeRoomInspection(room.getRoomId(), false);
                break;
            case DAMAGE_FOUND:
                dataManager.completeRoomInspection(room.getRoomId(), true);
                break;
            case REPAIR_COMPLETED:
                dataManager.completeRoomMaintenance(room.getRoomId());
                break;
            case CLEANING_COMPLETED:
                dataManager.completeRoomCleaning(room.getRoomId());
                break;
            default:
                throw new IllegalStateException("Unsupported room action: " + action);
        }
    }

    private boolean isRoomRelevant(Staff staff, TaskType taskType, Room room) {
        RoomStatus status = room.getStatus();
        if (status == RoomStatus.ARCHIVED || status == RoomStatus.OCCUPIED) {
            return false;
        }
        if (staff.isHousekeeping()) {
            if (taskType == TaskType.INSPECT_A_ROOM) {
                return status == RoomStatus.INSPECTION_REQUIRED;
            }
            if (taskType == TaskType.UPDATE_ROOM_READINESS) {
                return status == RoomStatus.INSPECTION_REQUIRED
                        || status == RoomStatus.HOUSEKEEPING_REQUIRED
                        || status == RoomStatus.MAINTENANCE;
            }
            return status == RoomStatus.HOUSEKEEPING_REQUIRED
                    || status == RoomStatus.MAINTENANCE;
        }
        if (taskType == TaskType.REPAIR_DAMAGE_IN_A_ROOM
                || taskType == TaskType.REPORT_MAINTENANCE_COMPLETED) {
            return status == RoomStatus.REPAIR_REQUIRED;
        }
        return status != RoomStatus.INSPECTION_REQUIRED
                && status != RoomStatus.HOUSEKEEPING_REQUIRED
                && status != RoomStatus.MAINTENANCE
                && status != RoomStatus.REPAIR_REQUIRED;
    }

    private void addGuestServiceTargets(List<TaskTarget> targets, TaskType taskType,
                                        Booking booking) {
        boolean matchedAddOn = false;
        for (AddOn addOn : booking.getAddOns()) {
            if (isAddOnRelevant(taskType, addOn)) {
                targets.add(bookingTarget(booking, addOn));
                matchedAddOn = true;
            }
        }
        if (!matchedAddOn && (taskType == TaskType.DELIVER_GUEST_REQUEST
                || taskType == TaskType.RESOLVE_GUEST_CONCERN)) {
            targets.add(bookingTarget(booking, null));
        }
    }

    private boolean isAddOnRelevant(TaskType taskType, AddOn addOn) {
        AddOnCategory category = addOn.getCategory();
        if (taskType == TaskType.ARRANGE_RESORT_ACTIVITY) {
            return category == AddOnCategory.ACTIVITY
                    || category == AddOnCategory.TOURS
                    || category == AddOnCategory.EVENTS;
        }
        if (taskType == TaskType.COORDINATE_ROOM_AMENITY) {
            return category == AddOnCategory.EQUIPMENT
                    || category == AddOnCategory.SERVICE
                    || category == AddOnCategory.WELLNESS;
        }
        return taskType == TaskType.DELIVER_GUEST_REQUEST
                || taskType == TaskType.RESOLVE_GUEST_CONCERN;
    }

    private TaskTarget bookingTarget(Booking booking, AddOn addOn) {
        String details = "Room " + booking.getRoom().getRoomId()
                + " - Guest " + booking.getGuest().getName();
        String label = "Room " + booking.getRoom().getRoomId()
                + " - " + booking.getGuest().getName();
        if (addOn != null) {
            details += " - " + addOn.getName();
            label += " - " + addOn.getName();
        }
        return new TaskTarget(label, details);
    }
}
