package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

public class RoomReadinessWorkflowTest {

    private final DataManager dataManager = DataManager.getInstance();

    @Test
    public void undamagedRoomMovesFromInspectionThroughCleaningToAvailable() {
        Room room = createRoom();

        assertTrue(dataManager.updateRoomStatus(
                room.getRoomId(), RoomStatus.INSPECTION_REQUIRED));
        dataManager.completeRoomInspection(room.getRoomId(), false);
        assertEquals(RoomStatus.HOUSEKEEPING_REQUIRED, room.getStatus());

        dataManager.completeRoomCleaning(room.getRoomId());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    @Test
    public void damagedRoomMovesThroughMaintenanceAndCleaningToAvailable() {
        Room room = createRoom();
        dataManager.updateRoomStatus(room.getRoomId(), RoomStatus.INSPECTION_REQUIRED);

        dataManager.completeRoomInspection(room.getRoomId(), true);
        assertEquals(RoomStatus.REPAIR_REQUIRED, room.getStatus());

        Task ticket = findMaintenanceTicket(room);
        assertNotNull(ticket);
        assertNull(ticket.getAssignedTo());

        Staff maintenanceStaff = new Staff(
                UUID.randomUUID().toString(), "Repair Technician", "Repair Technician");
        maintenanceStaff.assignTask(ticket);
        dataManager.completeMaintenanceTask(ticket);
        assertEquals(RoomStatus.HOUSEKEEPING_REQUIRED, room.getStatus());
        assertEquals("COMPLETED", ticket.getStatus());

        dataManager.completeRoomCleaning(room.getRoomId());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    @Test
    public void inspectionCannotBeCompletedBeforeItIsRequired() {
        Room room = createRoom();

        try {
            dataManager.completeRoomInspection(room.getRoomId(), false);
            fail("Expected inspection of an available room to be rejected.");
        } catch (IllegalStateException expected) {
            assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        }
    }

    @Test
    public void roomStatusUpdateRejectsUnsupportedTransitions() {
        Room room = createRoom();

        assertFalse(dataManager.updateRoomStatus(room.getRoomId(), RoomStatus.AVAILABLE));
        assertFalse(dataManager.updateRoomStatus(room.getRoomId(), RoomStatus.REPAIR_REQUIRED));
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    private Room createRoom() {
        Room room = new StandardRoom(UUID.randomUUID().toString(), "Garden", 2500.0);
        dataManager.addRoom(room);
        return room;
    }

    private Task findMaintenanceTicket(Room room) {
        String description =
                "[Maintenance] Inspect and repair reported damage in room "
                        + room.getRoomId();
        for (Task task : dataManager.getAllTasks()) {
            if (description.equals(task.getDescription())) {
                return task;
            }
        }
        return null;
    }
}
