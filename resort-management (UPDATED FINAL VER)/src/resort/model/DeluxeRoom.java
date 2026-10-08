package resort.model;

public class DeluxeRoom extends Room {
    private static final long serialVersionUID = 5023704138100530782L;

    private static final int MAX_CAPACITY = 4;
    public DeluxeRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    public DeluxeRoom(String roomId, String viewType) {
        super(roomId, viewType, RoomRates.getNightlyRate("Deluxe", viewType),
                MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate();
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