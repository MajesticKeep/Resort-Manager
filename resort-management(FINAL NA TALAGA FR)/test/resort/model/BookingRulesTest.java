package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class BookingRulesTest {

    private final DataManager dataManager = DataManager.getInstance();

    @Test
    public void availabilityRejectsOverlapsButAllowsAdjacentBookings() {
        Room room = createRoom();
        LocalDate checkIn = LocalDate.now().plusDays(10);
        Booking existing = createBooking(room, checkIn, checkIn.plusDays(2));

        assertFalse(room.isAvailable(checkIn.plusDays(1), checkIn.plusDays(3)));
        assertTrue(room.isAvailable(checkIn.plusDays(2), checkIn.plusDays(4)));
        assertTrue(room.isAvailable(checkIn, checkIn.plusDays(2), existing));

        existing.checkIn();
        existing.checkOut();
        assertTrue(room.isAvailable(checkIn.plusDays(1), checkIn.plusDays(3)));
    }

    @Test
    public void rescheduleRejectsConflictsAndInvalidDatesWithoutChangingBooking() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        Booking booking = createBooking(room, start, start.plusDays(2));
        createBooking(room, start.plusDays(5), start.plusDays(7));

        assertThrows(IllegalArgumentException.class, () ->
                dataManager.rescheduleBooking(booking.getBookingId(),
                        start.plusDays(5), start.plusDays(6)));
        assertEquals(start, booking.getCheckInDate());
        assertEquals(start.plusDays(2), booking.getCheckOutDate());

        assertThrows(IllegalArgumentException.class, () ->
                dataManager.rescheduleBooking(booking.getBookingId(), start, start));
        assertThrows(IllegalArgumentException.class, () ->
                dataManager.rescheduleBooking(booking.getBookingId(),
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
    }

    @Test
    public void rescheduleAllowsAvailableDatesForReservedBooking() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        Booking booking = createBooking(room, start, start.plusDays(2));

        dataManager.rescheduleBooking(booking.getBookingId(),
                start.plusDays(2), start.plusDays(4));

        assertEquals(start.plusDays(2), booking.getCheckInDate());
        assertEquals(start.plusDays(4), booking.getCheckOutDate());
    }

    @Test
    public void checkedOutAndCancelledBookingsDoNotBlockRoomDates() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        Booking checkedOut = createBooking(room, start, start.plusDays(2));
        checkedOut.checkIn();
        checkedOut.checkOut();
        assertTrue(room.isAvailable(start, start.plusDays(2)));

        Booking cancelled = createBooking(room, start.plusDays(3), start.plusDays(5));
        cancelled.setStatus(BookingStatus.CANCELLED);
        assertTrue(room.isAvailable(start.plusDays(3), start.plusDays(5)));
    }

    @Test
    public void bookingCreationRejectsInvalidDatesAndOverlappingReservations() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        Booking initial = createBooking(room, start, start.plusDays(2));

        assertThrows(IllegalArgumentException.class, () ->
                dataManager.addBooking(new Booking(UUID.randomUUID().toString(),
                        initial.getGuest(), room, start.plusDays(1),
                        start.plusDays(3), 1, 500.0)));
        assertThrows(IllegalArgumentException.class, () ->
                dataManager.addBooking(new Booking(UUID.randomUUID().toString(),
                        initial.getGuest(), room, start, start, 1, 500.0)));
        assertThrows(IllegalArgumentException.class, () ->
                dataManager.addBooking(new Booking(UUID.randomUUID().toString(),
                        initial.getGuest(), room, LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1), 1, 500.0)));
        assertEquals(1L, dataManager.getAllBookings().stream()
                .filter(booking -> booking.getRoom() == room).count());
    }

    @Test
    public void bookingLifecycleRejectsInvalidTransitions() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        Booking booking = new Booking(UUID.randomUUID().toString(),
                new Guest(UUID.randomUUID().toString(), "Test Guest", "test"),
                room, start, start.plusDays(1), 1, 500.0);

        assertThrows(IllegalStateException.class, booking::checkOut);
        booking.checkIn();
        assertThrows(IllegalStateException.class, booking::checkIn);
        booking.checkOut();
        assertThrows(IllegalStateException.class, booking::checkOut);
    }

    @Test
    public void collectionGettersCannotModifyManagedRecords() {
        Room room = createRoom();
        LocalDate start = LocalDate.now().plusDays(10);
        createBooking(room, start, start.plusDays(1));

        assertThrows(UnsupportedOperationException.class,
                () -> dataManager.getAllRooms().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> dataManager.getAllBookings().clear());
    }

    private Room createRoom() {
        Room room = new StandardRoom(UUID.randomUUID().toString(), "Garden", 2500.0);
        dataManager.addRoom(room);
        return room;
    }

    private Booking createBooking(Room room, LocalDate checkIn, LocalDate checkOut) {
        Guest guest = new Guest(UUID.randomUUID().toString(), "Test Guest", "test");
        Booking booking = new Booking(UUID.randomUUID().toString(), guest, room,
                checkIn, checkOut, 1, 500.0);
        dataManager.addBooking(booking);
        return booking;
    }

    private static void assertThrows(Class<? extends RuntimeException> expectedType,
                                     Runnable action) {
        try {
            action.run();
            fail("Expected " + expectedType.getSimpleName() + ".");
        } catch (RuntimeException ex) {
            assertTrue("Unexpected exception: " + ex,
                    expectedType.isInstance(ex));
        }
    }
}
