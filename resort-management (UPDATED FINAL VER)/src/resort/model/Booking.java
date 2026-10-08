package resort.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Booking implements Serializable {
    private static final long serialVersionUID = -4913997931853300526L;

    private String bookingId;
    private Guest guest;
    private Room room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int guestCount;
    private List<AddOn> addOns;
    private Map<String, Integer> addOnQuantities;
    private BookingStatus status;
    private double extraPersonFeePerNight;
    private boolean lateCheckoutSelected;
    // Retained so bookings serialized by older versions can still be read.
    private int lateCheckoutHours;
    private double lateCheckoutFeePerHour;

    public Booking(String bookingId, Guest guest, Room room, LocalDate checkInDate,
                   LocalDate checkOutDate, int guestCount,
                   double extraPersonFeePerNight) {
        this.bookingId = bookingId;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestCount = guestCount;
        this.addOns = new ArrayList<>();
        this.addOnQuantities = new HashMap<>();
        this.status = BookingStatus.RESERVED;
        if (!Double.isFinite(extraPersonFeePerNight) || extraPersonFeePerNight < 0) {
            throw new IllegalArgumentException(
                    "Extra-person fee must be finite and non-negative.");
        }
        this.extraPersonFeePerNight = extraPersonFeePerNight;
    }

    public void addAddOn(AddOn addOn) {
        addAddOn(addOn, addOn != null
                && addOn.getChargeBasis() == AddOnChargeBasis.PER_PERSON
                        ? guestCount : 1);
    }

    public void addAddOn(AddOn addOn, int quantity) {
        if (addOn == null) {
            throw new IllegalArgumentException("Add-on is required.");
        }
        if (quantity < 1 || quantity > guestCount) {
            throw new IllegalArgumentException(
                    "Add-on quantity must be between 1 and the booking guest count.");
        }
        if (addOn.getChargeBasis() == AddOnChargeBasis.PER_BOOKING && quantity != 1) {
            throw new IllegalArgumentException(
                    "Per-booking add-ons can only have a quantity of one.");
        }
        for (AddOn existing : addOns) {
            if (Objects.equals(existing.getAddOnId(), addOn.getAddOnId())) {
                addOnQuantities().put(addOn.getAddOnId(), quantity);
                return;
            }
        }
        addOns.add(addOn);
        addOnQuantities().put(addOn.getAddOnId(), quantity);
    }

    public double calculateTotal() {
        long nights = Math.max(1, ChronoUnit.DAYS.between(checkInDate, checkOutDate));
        double roomCharge = room.calculateRate() * nights;
        double addOnCharge = getAddOnCharge();
        return roomCharge + addOnCharge + calculateExtraPersonCharge(nights)
                + getLateCheckoutCharge();
    }

    private double calculateExtraPersonCharge(long nights) {
        int extraPeople = Math.max(0, guestCount - room.getCapacity());
        return extraPeople * extraPersonFeePerNight * nights;
    }

    public double getExtraPersonCharge() {
        long nights = Math.max(1, ChronoUnit.DAYS.between(checkInDate, checkOutDate));
        return calculateExtraPersonCharge(nights);
    }

    double getExtraPersonFeePerNight() {
        return extraPersonFeePerNight;
    }

    int getLateCheckoutHours() {
        return lateCheckoutHours;
    }

    double getLateCheckoutFeePerHour() {
        return lateCheckoutFeePerHour;
    }

    void restorePersistentState(BookingStatus status, boolean lateCheckoutSelected,
                               int lateCheckoutHours, double lateCheckoutFeePerHour) {
        if (status == null || lateCheckoutHours < 0
                || !Double.isFinite(lateCheckoutFeePerHour)
                || lateCheckoutFeePerHour < 0) {
            throw new IllegalArgumentException("Saved booking state is invalid.");
        }
        this.status = status;
        this.lateCheckoutSelected = lateCheckoutSelected;
        this.lateCheckoutHours = lateCheckoutHours;
        this.lateCheckoutFeePerHour = lateCheckoutFeePerHour;
    }

    public void setLateCheckoutSelected(boolean selected) {
        if (status != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Late checkout can only be selected during checkout.");
        }
        lateCheckoutSelected = selected;
    }

    public boolean isLateCheckoutSelected() {
        return lateCheckoutSelected || lateCheckoutHours > 0;
    }

    public double getLateCheckoutCharge() {
        return isLateCheckoutSelected() ? room.calculateRate() * 0.5 : 0.0;
    }

    public void checkIn() {
        if (status != BookingStatus.RESERVED) {
            throw new IllegalStateException("Only reserved bookings can be checked in.");
        }
        this.status = BookingStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in bookings can be checked out.");
        }
        this.status = BookingStatus.CHECKED_OUT;
    }

    public void reschedule(LocalDate checkInDate, LocalDate checkOutDate) {
        if (status != BookingStatus.RESERVED) {
            throw new IllegalStateException("Only reserved bookings can be rescheduled.");
        }
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }

    public String getBookingId() { return bookingId; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public int getGuestCount() { return guestCount; }
    public List<AddOn> getAddOns() { return Collections.unmodifiableList(addOns); }
    public int getAddOnQuantity(AddOn addOn) {
        if (addOn == null) {
            throw new IllegalArgumentException("Add-on is required.");
        }
        return addOnQuantities().getOrDefault(addOn.getAddOnId(), 1);
    }
    public double getAddOnCharge() {
        return addOns.stream().mapToDouble(addOn ->
                addOn.getPrice() * getAddOnQuantity(addOn)).sum();
    }

    private Map<String, Integer> addOnQuantities() {
        if (addOnQuantities == null) {
            addOnQuantities = new HashMap<>();
        }
        return addOnQuantities;
    }

    public BookingStatus getStatus() { return status; }
    void setStatus(BookingStatus status) { this.status = status; }

    @Override
    public String toString() {
        return bookingId + " - " + guest.getName() + " (" + room.getRoomId() + ") "
                + status + ", " + checkInDate + " to " + checkOutDate;
    }
}
