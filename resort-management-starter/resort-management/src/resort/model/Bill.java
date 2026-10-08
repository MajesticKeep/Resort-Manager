package resort.model;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class Bill {
    private String billId;
    private Booking booking;
    private double roomCharge;
    private double addOnCharge;
    private double totalAmount;

    public Bill(String billId, Booking booking) {
        this.billId = billId;
        this.booking = booking;
    }

    public void generateBill() {
        long nights = Math.max(1, ChronoUnit.DAYS.between(
                booking.getCheckInDate(), booking.getCheckOutDate()));
        this.roomCharge = booking.getRoom().calculateRate() * nights;

        List<AddOn> addOns = booking.getAddOns();
        this.addOnCharge = addOns.stream().mapToDouble(AddOn::getPrice).sum();

        this.totalAmount = roomCharge + addOnCharge;
    }

    public String getBillId() { return billId; }
    public Booking getBooking() { return booking; }
    public double getRoomCharge() { return roomCharge; }
    public double getAddOnCharge() { return addOnCharge; }
    public double getTotalAmount() { return totalAmount; }
}