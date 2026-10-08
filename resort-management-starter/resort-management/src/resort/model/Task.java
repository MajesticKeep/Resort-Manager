package resort.model;

import java.io.Serializable;

public class Task implements Serializable {
    private String taskId;
    private String description;
    private String status;
    private Staff assignedTo;

    public Task(String taskId, String description, Staff assignedTo) {
        this.taskId = taskId;
        this.description = description;
        this.assignedTo = assignedTo;
        this.status = "PENDING";
    }

    public String getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public Staff getAssignedTo() { return assignedTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}