package resort.controller;

import resort.model.Bill;
import resort.model.Booking;
import resort.model.BookingStatus;
import resort.model.DataManager;
import resort.model.RoomStatus;

public final class BookingController {

    private final DataManager dataManager;

    public BookingController(DataManager dataManager) {
        if (dataManager == null) {
            throw new IllegalArgumentException("Data manager is required.");
        }
        this.dataManager = dataManager;
    }

    public void checkIn(Booking booking) {
        requireBooking(booking);
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Only reserved bookings can be checked in.");
        }
        if (!dataManager.updateRoomStatus(
                booking.getRoom().getRoomId(), RoomStatus.OCCUPIED)) {
            throw new IllegalStateException("The room cannot be checked in right now.");
        }
        booking.checkIn();
    }

    public Bill checkOutAndBill(Booking booking, boolean lateCheckoutSelected) {
        requireBooking(booking);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Only checked-in bookings can be checked out.");
        }
        if (!dataManager.updateRoomStatus(
                booking.getRoom().getRoomId(), RoomStatus.INSPECTION_REQUIRED)) {
            throw new IllegalStateException("The room cannot be checked out right now.");
        }
        booking.setLateCheckoutSelected(lateCheckoutSelected);
        booking.checkOut();

        Bill bill = new Bill(dataManager.generateBillId(), booking);
        bill.generateBill();
        dataManager.addBill(bill);
        return bill;
    }

    public double calculateLateCheckoutFee(Booking booking) {
        requireBooking(booking);
        return booking.getRoom().calculateRate() * 0.5;
    }

    private static void requireBooking(Booking booking) {
        if (booking == null || booking.getRoom() == null) {
            throw new IllegalArgumentException("A valid booking is required.");
        }
    }
}
