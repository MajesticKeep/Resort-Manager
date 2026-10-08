package resort.model;

public final class FrontDeskWorkflow extends StaffWorkflow {

    private static final TaskType[] TASK_TYPES = {
        TaskType.PREPARE_GUEST_CHECK_IN,
        TaskType.PROCESS_GUEST_CHECK_OUT,
        TaskType.CONFIRM_RESERVATION,
        TaskType.RESPOND_TO_BOOKING_INQUIRY
    };

    @Override
    public String getRoleName() {
        return "Front Desk";
    }

    @Override
    protected TaskType[] roleTaskTypes() {
        return TASK_TYPES.clone();
    }
}
