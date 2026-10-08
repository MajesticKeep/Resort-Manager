package resort.model;

public class FamilyRoom extends Room {

    // Data doc listed "4-6 people" without a locked single number —
    // using the top of the range. Confirm with the group if 4 was meant.
    private static final int MAX_CAPACITY = 6;
    private static final double FAMILY_SURCHARGE = 0.15;

    public FamilyRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate() * (1 + FAMILY_SURCHARGE);
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