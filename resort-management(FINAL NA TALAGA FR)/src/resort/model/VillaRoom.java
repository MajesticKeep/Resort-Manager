package resort.model;

public class VillaRoom extends Room {
    private static final long serialVersionUID = 4544528516989835292L;

    private static final int MAX_CAPACITY = 8;
    public VillaRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    public VillaRoom(String roomId, String viewType) {
        super(roomId, viewType, RoomRates.getNightlyRate("Villa", viewType),
                MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate();
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