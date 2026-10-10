package resort.model;

public class FamilyRoom extends Room {
    private static final long serialVersionUID = 6080043284937144770L;

    // Data doc listed "4-6 people" without a locked single number —
    // using the top of the range. Confirm with the group if 4 was meant.
    private static final int MAX_CAPACITY = 6;
    public FamilyRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    public FamilyRoom(String roomId, String viewType) {
        super(roomId, viewType, RoomRates.getNightlyRate("Family", viewType),
                MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate();
    }

    @Override
    public String toString() {
        return "Family Room\n" +
               "Room ID  : " + getRoomId() + "\n" +
               "View Type: " + getViewType() + "\n" +
               "Capacity : " + getCapacity() + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}