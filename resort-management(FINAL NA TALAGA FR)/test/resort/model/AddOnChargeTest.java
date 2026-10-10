package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.LocalDate;

import org.junit.Test;

public class AddOnChargeTest {

    @Test
    public void perPersonAddOnDefaultsToEveryGuestAndCanBeAdjusted() {
        Room room = new StandardRoom("R1", "Garden", 2500.0);
        Guest guest = new Guest("G1", "Test Guest", "test contact");
        LocalDate checkIn = LocalDate.now().plusDays(5);
        Booking booking = new Booking("B1", guest, room, checkIn,
                checkIn.plusDays(1), 3, 500.0);
        AddOn breakfast = new AddOn("A1", "Breakfast", 450.0,
                AddOnCategory.FOOD, "Breakfast per guest",
                AddOnChargeBasis.PER_PERSON);

        booking.addAddOn(breakfast);
        assertEquals(3, booking.getAddOnQuantity(breakfast));
        assertEquals(1350.0, booking.getAddOnCharge(), 0.0);

        booking.addAddOn(breakfast, 2);
        assertEquals(2, booking.getAddOnQuantity(breakfast));
        Bill bill = new Bill("BL1", booking);
        bill.generateBill();
        assertEquals(900.0, bill.getAddOnCharge(), 0.0);
    }

    @Test
    public void perBookingAddOnStaysSingleAndQuantityCannotExceedGuestCount() {
        Room room = new StandardRoom("R2", "Garden", 2500.0);
        Guest guest = new Guest("G2", "Test Guest", "test contact");
        LocalDate checkIn = LocalDate.now().plusDays(5);
        Booking booking = new Booking("B2", guest, room, checkIn,
                checkIn.plusDays(1), 2, 500.0);
        AddOn transfer = new AddOn("A2", "Airport Transfer", 1800.0,
                AddOnCategory.TRANSPORT, "Private transfer",
                AddOnChargeBasis.PER_BOOKING);

        booking.addAddOn(transfer);
        assertEquals(1800.0, booking.getAddOnCharge(), 0.0);
        assertThrows(IllegalArgumentException.class,
                () -> booking.addAddOn(transfer, 2));
        AddOn breakfast = new AddOn("A3", "Breakfast", 450.0,
                AddOnCategory.FOOD, "Breakfast",
                AddOnChargeBasis.PER_PERSON);
        assertThrows(IllegalArgumentException.class,
                () -> booking.addAddOn(breakfast, 3));
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
