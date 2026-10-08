package resort.model;

public class SuiteRoom extends Room {

    private static final int MAX_CAPACITY = 6;
    private static final double SUITE_SURCHARGE = 0.10;

    public SuiteRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate() * (1 + SUITE_SURCHARGE);
    }

    @Override
    public String toString() {
        return "Suite Room\n" +
               "Room ID  : " + getRoomId() + "\n" +
               "View Type: " + getViewType() + "\n" +
               "Capacity : " + getCapacity() + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}