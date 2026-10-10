package resort.model;

import java.util.List;

public final class RoomAmenityDefaults {

    private RoomAmenityDefaults() {
    }

    public static List<String> forRoomType(String roomType) {
        return ResortConfig.getInstance().getAmenitiesForRoomType(roomType);
    }
}
