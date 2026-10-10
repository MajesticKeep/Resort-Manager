package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class RoomArchiveTest {

    private final DataManager dataManager = DataManager.getInstance();

    @Test
    public void activeReservationPreventsArchiving() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        Booking booking = new Booking(UUID.randomUUID().toString(),
                new Guest(UUID.randomUUID().toString(), "Test Guest", "test"),
                room, start, start.plusDays(1), 1, 500.0);
        dataManager.addBooking(booking);

        assertThrows(IllegalStateException.class,
                () -> dataManager.archiveRoom(room.getRoomId()));
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());

        booking.checkIn();
        assertThrows(IllegalStateException.class,
                () -> dataManager.archiveRoom(room.getRoomId()));
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    @Test
    public void roomWithoutActiveBookingCanBeArchivedAndRestored() {
        Room room = createRoom();
        room.setStatus(RoomStatus.HOUSEKEEPING_REQUIRED);

        dataManager.archiveRoom(room.getRoomId());
        assertEquals(RoomStatus.ARCHIVED, room.getStatus());

        assertThrows(IllegalStateException.class,
                () -> dataManager.archiveRoom(room.getRoomId()));
        dataManager.restoreRoom(room.getRoomId());
        assertEquals(RoomStatus.HOUSEKEEPING_REQUIRED, room.getStatus());
    }

    private Room createRoom() {
        Room room = new StandardRoom(UUID.randomUUID().toString(), "Garden", 2500.0);
        dataManager.addRoom(room);
        return room;
    }

    private static void assertThrows(Class<? extends RuntimeException> expectedType,
                                     Runnable action) {
        try {
            action.run();
            fail("Expected " + expectedType.getSimpleName() + ".");
        } catch (RuntimeException ex) {
            if (!expectedType.isInstance(ex)) {
                throw ex;
            }
        }
    }
}
