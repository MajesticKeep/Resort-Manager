package resort.model;

public class DeluxeRoom extends Room {

    private static final int MAX_CAPACITY = 4;
    private static final double DELUXE_SURCHARGE = 0.25;

    public DeluxeRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate() * (1 + DELUXE_SURCHARGE);
    }

    @Override
    public String toString() {
        return "Deluxe Room\n" +
               "Room ID  : " + getRoomId() + "\n" +
               "View Type: " + getViewType() + "\n" +
               "Capacity : " + getCapacity() + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}