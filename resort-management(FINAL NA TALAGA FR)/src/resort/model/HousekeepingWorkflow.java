package resort.model;

public final class HousekeepingWorkflow extends StaffWorkflow {

    private static final TaskType[] TASK_TYPES = {
        TaskType.CLEAN_AND_PREPARE_ROOM,
        TaskType.RESTOCK_ROOM_SUPPLIES,
        TaskType.INSPECT_A_ROOM,
        TaskType.UPDATE_ROOM_READINESS
    };

    @Override
    public String getRoleName() {
        return "Housekeeping";
    }

    @Override
    protected TaskType[] roleTaskTypes() {
        return TASK_TYPES.clone();
    }
}
