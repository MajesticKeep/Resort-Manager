package resort.model;

public final class RoomRates {

    private RoomRates() {
    }

    public static double getNightlyRate(String roomType, String viewType) {
        return ResortConfig.getInstance().getNightlyRate(roomType, viewType);
    }
}
