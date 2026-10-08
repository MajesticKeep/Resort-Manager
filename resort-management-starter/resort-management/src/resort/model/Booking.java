package resort.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Booking implements Serializable {
    private String bookingId;
    private Guest guest;
    private Room room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int guestCount;
    private List<AddOn> addOns;
    private BookingStatus status;

    public Booking(String bookingId, Guest guest, Room room, LocalDate checkInDate,
                    LocalDate checkOutDate, int guestCount) {
        this.bookingId = bookingId;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestCount = guestCount;
        this.addOns = new ArrayList<>();
        this.status = BookingStatus.RESERVED;
    }

    public void addAddOn(AddOn addOn) {
        addOns.add(addOn);
    }

    public double calculateTotal() {
        long nights = Math.max(1, ChronoUnit.DAYS.between(checkInDate, checkOutDate));
        double roomCharge = room.calculateRate() * nights;
        double addOnCharge = addOns.stream().mapToDouble(AddOn::getPrice).sum();
        return roomCharge + addOnCharge;
    }

    public void checkIn() {
        this.status = BookingStatus.CHECKED_IN;
    }

    public void checkOut() {
        this.status = BookingStatus.CHECKED_OUT;
    }

    public String getBookingId() { return bookingId; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public int getGuestCount() { return guestCount; }
    public List<AddOn> getAddOns() { return addOns; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}