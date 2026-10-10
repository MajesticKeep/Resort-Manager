package resort.model;

public class SuiteRoom extends Room {
    private static final long serialVersionUID = -5921015057330953805L;

    private static final int MAX_CAPACITY = 6;
    public SuiteRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    public SuiteRoom(String roomId, String viewType) {
        super(roomId, viewType, RoomRates.getNightlyRate("Suite", viewType),
                MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate();
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