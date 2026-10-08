package resort.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Staff implements Serializable {
    private static final long serialVersionUID = -6582291333892535345L;

    private String staffId;
    private String name;
    private String role;
    private List<Task> tasks;

    public Staff(String staffId, String name, String role) {
        this.staffId = staffId;
        this.name = name;
        this.role = role;
        this.tasks = new ArrayList<>();
    }

    // Attaches an existing Task to this staff member. Does NOT add the
    // task to DataManager — that happens once, at the task's creation,
    // regardless of whether it started assigned or unassigned. Calling
    // this a second time (e.g. reassigning) just updates ownership, it
    // never duplicates the task in the master list.
    public void assignTask(Task task) {
        task.setAssignedTo(this);
        tasks.add(task);
    }

    public void updateRoomHousekeepingStatus(Room room, boolean ready) {
        DataManager dataManager = DataManager.getInstance();
        if (ready) {
            dataManager.completeRoomCleaning(room.getRoomId());
        } else if (room.getStatus() == RoomStatus.INSPECTION_REQUIRED) {
            dataManager.completeRoomInspection(room.getRoomId(), false);
        } else {
            throw new IllegalStateException(
                    "Rooms must be inspected before cleaning or maintenance status changes.");
        }
    }

    public List<Task> getTasks() { return tasks; }
    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public Role getRoleType() { return Role.fromLegacyName(role); }
    public boolean isHousekeeping() {
        return getRoleType() == Role.HOUSEKEEPING;
    }
    public boolean isMaintenance() {
        return getRoleType() == Role.MAINTENANCE;
    }
    public boolean isGuestServices() {
        return getRoleType() == Role.GUEST_SERVICES;
    }

    @Override
    public String toString() {
        return name + " (" + getRole() + ")";
    }
}
