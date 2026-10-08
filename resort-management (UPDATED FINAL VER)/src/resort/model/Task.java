package resort.model;

import java.io.Serializable;

public class Task implements Serializable {
    private static final long serialVersionUID = 63474732771090102L;

    private String taskId;
    private String description;
    private String status;
    private TaskStatus taskStatus;
    private Staff assignedTo;

    public Task(String taskId, String description, Staff assignedTo) {
        this.taskId = taskId;
        this.description = description;
        this.assignedTo = assignedTo;
        setStatus(TaskStatus.PENDING);
    }

    public String getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public Staff getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Staff assignedTo) { this.assignedTo = assignedTo; }

    public String getStatus() {
        if (taskStatus == null) {
            taskStatus = TaskStatus.fromLegacyValue(status);
        }
        return taskStatus.name();
    }
    public TaskStatus getTaskStatus() {
        if (taskStatus == null) {
            taskStatus = TaskStatus.fromLegacyValue(status);
        }
        return taskStatus;
    }
    public void setStatus(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Task status is required.");
        }
        taskStatus = status;
        this.status = status.name();
    }
    public void setStatus(String status) {
        setStatus(TaskStatus.fromLegacyValue(status));
    }
}
