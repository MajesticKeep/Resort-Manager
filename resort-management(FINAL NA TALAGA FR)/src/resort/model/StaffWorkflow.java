package resort.model;

import java.util.Arrays;

public abstract class StaffWorkflow {

    public abstract String getRoleName();

    protected abstract TaskType[] roleTaskTypes();

    public final TaskType[] getTaskTypes() {
        TaskType[] roleTypes = roleTaskTypes();
        TaskType[] taskTypes = Arrays.copyOf(roleTypes, roleTypes.length + 1);
        taskTypes[roleTypes.length] = TaskType.CUSTOM_TASK;
        return taskTypes;
    }

    public final TaskType[] getStandardTaskTypes() {
        return roleTaskTypes().clone();
    }

    public final String describeTask(TaskType taskType, String details) {
        if (taskType == null || details == null) {
            throw new IllegalArgumentException("Task type and details are required.");
        }

        String cleanDetails = details.trim();
        if (cleanDetails.isEmpty()) {
            throw new IllegalArgumentException("Enter task details.");
        }

        if (taskType == TaskType.CUSTOM_TASK) {
            return "[" + getRoleName() + "] " + cleanDetails;
        }

        boolean isSupported = Arrays.asList(roleTaskTypes()).contains(taskType);
        if (!isSupported) {
            throw new IllegalArgumentException(
                    "The selected task is not available for " + getRoleName() + ".");
        }
        return "[" + getRoleName() + "] " + taskType.getDisplayName()
                + " - " + cleanDetails;
    }
}
