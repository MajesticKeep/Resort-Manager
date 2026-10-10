package resort.model;

public final class GuestServicesWorkflow extends StaffWorkflow {

    private static final TaskType[] TASK_TYPES = {
        TaskType.DELIVER_GUEST_REQUEST,
        TaskType.ARRANGE_RESORT_ACTIVITY,
        TaskType.COORDINATE_ROOM_AMENITY,
        TaskType.RESOLVE_GUEST_CONCERN
    };

    @Override
    public String getRoleName() {
        return "Guest Services";
    }

    @Override
    protected TaskType[] roleTaskTypes() {
        return TASK_TYPES.clone();
    }
}
