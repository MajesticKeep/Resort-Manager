package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class LateCheckoutFeeTest {

    @Test
    public void lateCheckoutIsAOneTimeHalfNightRoomFee() {
        Room room = new StandardRoom(UUID.randomUUID().toString(), "Garden", 2500.0);
        LocalDate checkIn = LocalDate.now().plusDays(10);
        Booking booking = new Booking(UUID.randomUUID().toString(),
                new Guest(UUID.randomUUID().toString(), "Test Guest", "test"),
                room, checkIn, checkIn.plusDays(2), 1, 500.0);

        booking.checkIn();
        booking.setLateCheckoutSelected(true);
        booking.checkOut();
        booking.addAddOn(new AddOn("A-test", "Breakfast", 125.0, AddOnCategory.FOOD));
        assertEquals(1250.0, booking.getLateCheckoutCharge(), 0.0);
        assertEquals(6375.0, booking.calculateTotal(), 0.0);

        Bill bill = new Bill("BL-test", booking);
        bill.generateBill();
        assertEquals(1250.0, bill.getLateCheckoutCharge(), 0.0);
        assertEquals(125.0, bill.getAddOnCharge(), 0.0);
        assertEquals(6375.0, bill.getTotalAmount(), 0.0);
        assertTrue(bill.toString().contains(
                "Late checkout (until 2:00 PM): \u20b11250.00"));
    }

    @Test
    public void lateCheckoutCanOnlyBeSelectedDuringCheckout() {
        Room room = new StandardRoom("R-late", "Garden", 2500.0);
        LocalDate checkIn = LocalDate.now().plusDays(10);
        Booking booking = new Booking("B-late",
                new Guest("G-late", "Test Guest", "test"),
                room, checkIn, checkIn.plusDays(1), 1, 500.0);

        assertEquals(0.0, booking.getLateCheckoutCharge(), 0.0);
        try {
            booking.setLateCheckoutSelected(true);
            fail("Expected late checkout to be unavailable before check-in.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
        booking.checkIn();
        booking.setLateCheckoutSelected(true);
        assertEquals(1250.0, booking.getLateCheckoutCharge(), 0.0);
        booking.checkOut();
        try {
            booking.setLateCheckoutSelected(false);
            fail("Expected late checkout to be immutable after checkout.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
