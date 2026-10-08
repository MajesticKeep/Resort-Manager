package resort.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Staff implements Serializable {
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

    public void assignTask(Task task) {
        tasks.add(task);
        DataManager.getInstance().addTask(task);
    }

    public void markRoomForMaintenance(Room room) {
        room.setStatus(RoomStatus.MAINTENANCE);
    }

    public void completeMaintenanceTask(Room room) {
        room.setStatus(RoomStatus.AVAILABLE);
    }

    public List<Task> getTasks() { return tasks; }
    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}