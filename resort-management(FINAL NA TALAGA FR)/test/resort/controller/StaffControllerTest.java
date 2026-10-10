package resort.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;
import resort.model.DataManager;
import resort.model.Room;
import resort.model.RoomStatus;
import resort.model.Staff;
import resort.model.StandardRoom;

public class StaffControllerTest {

    @Test
    public void roomActionsRouteInspectionRepairAndCleaningThroughTheController() {
        DataManager dataManager = DataManager.getInstance();
        Room room = new StandardRoom(UUID.randomUUID().toString(), "Garden", 2500.0);
        room.setStatus(RoomStatus.INSPECTION_REQUIRED);
        dataManager.addRoom(room);
        StaffController controller = new StaffController(dataManager);
        Staff housekeeper = new Staff(UUID.randomUUID().toString(), "Housekeeper",
                "Housekeeping");
        Staff technician = new Staff(UUID.randomUUID().toString(), "Technician",
                "Maintenance");

        assertTrue(controller.getAvailableRoomActions(housekeeper, room)
                .contains(StaffController.RoomAction.DAMAGE_FOUND));
        controller.performRoomAction(housekeeper, room,
                StaffController.RoomAction.DAMAGE_FOUND);
        assertEquals(RoomStatus.REPAIR_REQUIRED, room.getStatus());

        controller.performRoomAction(technician, room,
                StaffController.RoomAction.REPAIR_COMPLETED);
        assertEquals(RoomStatus.HOUSEKEEPING_REQUIRED, room.getStatus());

        controller.performRoomAction(housekeeper, room,
                StaffController.RoomAction.CLEANING_COMPLETED);
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }
}
