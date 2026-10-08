package resort.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public abstract class Room implements Serializable {

    private String roomId;
    private String viewType;
    private double rate;
    private int capacity;
    private RoomStatus status;

    public Room(String roomId, String viewType, double rate, int capacity) {
        this.roomId = roomId;
        this.viewType = viewType;
        this.rate = rate;
        this.capacity = capacity;
        this.status = RoomStatus.AVAILABLE;
    }

    public String getRoomId() { return roomId; }
    public String getViewType() { return viewType; }
    public double getRate() { return rate; }
    public int getCapacity() { return capacity; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public abstract double calculateRate();

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        if (status == RoomStatus.MAINTENANCE) {
            return false;
        }
        List<Booking> existing = DataManager.getInstance().getAllBookings();
        for (Booking b : existing) {
            if (b.getRoom() == this
                    && b.getStatus() != BookingStatus.CANCELLED
                    && b.getStatus() != BookingStatus.CHECKED_OUT) {
                boolean overlaps = checkIn.isBefore(b.getCheckOutDate())
                        && checkOut.isAfter(b.getCheckInDate());
                if (overlaps) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Room ID  : " + roomId + "\n" +
               "View Type: " + viewType + "\n" +
               "Capacity : " + capacity + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}