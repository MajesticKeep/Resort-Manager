package resort.model;

public final class GeneralStaffWorkflow extends StaffWorkflow {

    private static final TaskType[] TASK_TYPES = {
        TaskType.COMPLETE_ASSIGNED_WORK,
        TaskType.ASSIST_RESORT_OPERATIONS
    };

    @Override
    public String getRoleName() {
        return "General Staff";
    }

    @Override
    protected TaskType[] roleTaskTypes() {
        return TASK_TYPES.clone();
    }
}
