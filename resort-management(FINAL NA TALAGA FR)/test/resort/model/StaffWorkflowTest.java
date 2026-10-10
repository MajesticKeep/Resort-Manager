package resort.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class StaffWorkflowTest {

    @Test
    public void factorySelectsWorkflowForEachStaffRole() {
        assertTrue(StaffWorkflowFactory.forStaff(
                new Staff("S1", "Housekeeper", "Housekeeping"))
                instanceof HousekeepingWorkflow);
        assertTrue(StaffWorkflowFactory.forStaff(
                new Staff("S6", "Night Housekeeper", "hOuSeKeEpInG"))
                instanceof HousekeepingWorkflow);
        assertTrue(StaffWorkflowFactory.forStaff(
                new Staff("S2", "Repair Technician", "Repair Technician"))
                instanceof MaintenanceWorkflow);
        assertTrue(StaffWorkflowFactory.forStaff(
                new Staff("S3", "Receptionist", "Front Desk"))
                instanceof FrontDeskWorkflow);
        assertTrue(StaffWorkflowFactory.forStaff(
                new Staff("S4", "Concierge", "Guest Services"))
                instanceof GuestServicesWorkflow);
    }

    @Test
    public void factoryUsesGeneralWorkflowForUnknownRole() {
        assertTrue(StaffWorkflowFactory.forStaff(
                new Staff("S5", "Team Member", "Unrecognized Role"))
                instanceof GeneralStaffWorkflow);
    }

    @Test
    public void maintenanceRoleIsPreservedAndRoutesToMaintenanceWorkflow() {
        Staff staff = new Staff("S7", "Repair Technician", "Repair Technician");

        assertEquals("Repair Technician", staff.getRole());
        assertTrue(staff.isMaintenance());
        assertTrue(StaffWorkflowFactory.forStaff(staff) instanceof MaintenanceWorkflow);
    }

    @Test
    public void factoryRejectsMissingStaff() {
        try {
            StaffWorkflowFactory.forStaff(null);
            fail("Expected an IllegalArgumentException for missing staff.");
        } catch (IllegalArgumentException expected) {
            assertEquals("A staff member is required.", expected.getMessage());
        }
    }

    @Test
    public void workflowFormatsStandardAndCustomTaskDescriptions() {
        StaffWorkflow workflow = new MaintenanceWorkflow();

        assertEquals("[Maintenance] Repair damage in a room - Leaking faucet",
                workflow.describeTask(TaskType.REPAIR_DAMAGE_IN_A_ROOM,
                        "Leaking faucet"));
        assertEquals("[Maintenance] Replace damaged fixtures",
                workflow.describeTask(TaskType.CUSTOM_TASK,
                        " Replace damaged fixtures "));
    }

    @Test
    public void workflowReturnsAnIndependentTaskTypeArray() {
        StaffWorkflow workflow = new HousekeepingWorkflow();
        TaskType[] taskTypes = workflow.getTaskTypes();

        assertEquals(TaskType.CUSTOM_TASK, taskTypes[taskTypes.length - 1]);
        taskTypes[0] = TaskType.CUSTOM_TASK;

        assertNotSame(taskTypes, workflow.getTaskTypes());
        assertArrayEquals(new TaskType[] {
            TaskType.CLEAN_AND_PREPARE_ROOM,
            TaskType.RESTOCK_ROOM_SUPPLIES,
            TaskType.INSPECT_A_ROOM,
            TaskType.UPDATE_ROOM_READINESS
        }, workflow.getStandardTaskTypes());
    }

    @Test
    public void workflowRejectsBlankDetailsAndUnsupportedTaskTypes() {
        StaffWorkflow workflow = new FrontDeskWorkflow();

        assertInvalidTask(workflow, TaskType.CONFIRM_RESERVATION, "  ");
        assertInvalidTask(workflow, TaskType.CLEAN_AND_PREPARE_ROOM, "Room 12");
    }

    private static void assertInvalidTask(StaffWorkflow workflow,
                                          TaskType taskType, String details) {
        try {
            workflow.describeTask(taskType, details);
            fail("Expected invalid task input to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
