package resort.model;

public class PavilionRoom extends Room {
    private static final long serialVersionUID = -2903522704451832220L;

    // Base rate covers 10-15 pax. 16-20 requires the Extra Person add-on,
    // manually attached by front desk per guest over base — same pattern
    // as every other room type. Above 20 is a hard reject, enforced in
    // Booking, not here.
    private static final int MAX_CAPACITY = 20;

    public PavilionRoom(String roomId, String viewType, double rate) {
        super(roomId, viewType, rate, MAX_CAPACITY);
    }

    public PavilionRoom(String roomId, String viewType) {
        super(roomId, viewType, RoomRates.getNightlyRate("Pavilion", viewType),
                MAX_CAPACITY);
    }

    @Override
    public double calculateRate() {
        return getRate();
    }

    @Override
    public String toString() {
        return "Pavilion Room\n" +
               "Room ID  : " + getRoomId() + "\n" +
               "View Type: " + getViewType() + "\n" +
               "Capacity : " + getCapacity() + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}