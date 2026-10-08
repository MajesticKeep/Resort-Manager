package resort.model;

import java.io.Serializable;
import java.time.temporal.ChronoUnit;

public class Bill implements Serializable {
    private static final long serialVersionUID = -5672277820897090509L;

    private String billId;
    private Booking booking;
    private double roomCharge;
    private double addOnCharge;
    private double extraPersonCharge;
    private double lateCheckoutCharge;
    private double totalAmount;

    public Bill(String billId, Booking booking) {
        this.billId = billId;
        this.booking = booking;
    }

    public void generateBill() {
        long nights = Math.max(1, ChronoUnit.DAYS.between(
                booking.getCheckInDate(), booking.getCheckOutDate()));
        this.roomCharge = booking.getRoom().calculateRate() * nights;

        this.addOnCharge = booking.getAddOnCharge();
        this.lateCheckoutCharge = booking.getLateCheckoutCharge();
        this.extraPersonCharge = booking.getExtraPersonCharge();

        this.totalAmount = roomCharge + addOnCharge + extraPersonCharge + lateCheckoutCharge;
    }

    public String getBillId() { return billId; }
    public Booking getBooking() { return booking; }
    public double getRoomCharge() { return roomCharge; }
    public double getAddOnCharge() { return addOnCharge; }
    public double getLateCheckoutCharge() { return lateCheckoutCharge; }
    public double getTotalAmount() { return totalAmount; }
    public double getExtraPersonCharge() { return extraPersonCharge; }

    void restoreAmounts(double roomCharge, double addOnCharge,
                        double extraPersonCharge, double lateCheckoutCharge,
                        double totalAmount) {
        this.roomCharge = roomCharge;
        this.addOnCharge = addOnCharge;
        this.extraPersonCharge = extraPersonCharge;
        this.lateCheckoutCharge = lateCheckoutCharge;
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "Bill " + billId + " - \u20b1" + String.format("%.2f", totalAmount)
                + " (Room: \u20b1" + String.format("%.2f", roomCharge)
                + ", Add-ons: \u20b1" + String.format("%.2f", addOnCharge)
                + ", Extra guests: \u20b1" + String.format("%.2f", extraPersonCharge)
                + ", Late checkout (until 2:00 PM): \u20b1"
                + String.format("%.2f", lateCheckoutCharge) + ")";
    }
}
