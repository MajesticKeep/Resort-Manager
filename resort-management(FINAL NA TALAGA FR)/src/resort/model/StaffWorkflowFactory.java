package resort.model;

public final class StaffWorkflowFactory {

    private StaffWorkflowFactory() {
    }

    public static StaffWorkflow forStaff(Staff staff) {
        if (staff == null) {
            throw new IllegalArgumentException("A staff member is required.");
        }

        return staff.getRoleType().createWorkflow();
    }
}
