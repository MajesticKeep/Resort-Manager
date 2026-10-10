package resort.controller;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Test;
import resort.model.Bill;
import resort.model.Booking;
import resort.model.BookingStatus;
import resort.model.DataManager;
import resort.model.Guest;
import resort.model.Room;
import resort.model.RoomStatus;
import resort.model.StandardRoom;

public class BookingControllerTest {

    @Test
    public void checkInAndCheckoutUpdateBookingRoomAndBill() {
        DataManager dataManager = DataManager.getInstance();
        String roomId = UUID.randomUUID().toString();
        Room room = new StandardRoom(roomId, "Garden", 2500.0);
        LocalDate checkInDate = LocalDate.now().plusDays(10);
        Booking booking = new Booking(UUID.randomUUID().toString(),
                new Guest(UUID.randomUUID().toString(), "Controller Guest", "test"),
                room, checkInDate, checkInDate.plusDays(1), 1, 500.0);
        dataManager.addRoom(room);
        dataManager.addBooking(booking);

        BookingController controller = new BookingController(dataManager);
        assertEquals(1250.0, controller.calculateLateCheckoutFee(booking), 0.0);
        controller.checkIn(booking);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());

        Bill bill = controller.checkOutAndBill(booking, true);
        assertEquals(BookingStatus.CHECKED_OUT, booking.getStatus());
        assertEquals(RoomStatus.INSPECTION_REQUIRED, room.getStatus());
        assertEquals(1250.0, bill.getLateCheckoutCharge(), 0.0);
        assertEquals(3750.0, bill.getTotalAmount(), 0.0);
    }
}
