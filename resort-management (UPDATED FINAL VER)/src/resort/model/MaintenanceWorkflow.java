package resort.model;

public final class MaintenanceWorkflow extends StaffWorkflow {

    private static final TaskType[] TASK_TYPES = {
        TaskType.REPAIR_DAMAGE_IN_A_ROOM,
        TaskType.INSPECT_ROOM_FIXTURES,
        TaskType.REPORT_MAINTENANCE_COMPLETED
    };

    @Override
    public String getRoleName() {
        return "Maintenance";
    }

    @Override
    protected TaskType[] roleTaskTypes() {
        return TASK_TYPES.clone();
    }
}
