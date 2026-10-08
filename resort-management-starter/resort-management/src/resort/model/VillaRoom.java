package resort.model;

public class VillaRoom extends Room {

    private static final int MAX_CAPACITY = 8;
    private static final double VILLA_SURCHARGE = 0.20;

    public VillaRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate() * (1 + VILLA_SURCHARGE);
    }

    @Override
    public String toString() {
        return "Villa Room\n" +
               "Room ID  : " + getRoomId() + "\n" +
               "View Type: " + getViewType() + "\n" +
               "Capacity : " + getCapacity() + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}