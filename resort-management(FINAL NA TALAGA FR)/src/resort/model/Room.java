package resort.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room implements Serializable {
    private static final long serialVersionUID = -3645559187625435282L;
    public static final int DEFAULT_MAX_EXTRA_GUESTS = 2;

    private String roomId;
    private String roomType;
    private String viewType;
    private double rate;
    private int capacity;
    private Integer maxExtraGuests;
    private RoomStatus status;
    private RoomStatus statusBeforeArchive;
    private List<String> includedAmenities;

    public Room(String roomId, String viewType, double rate, int capacity) {
        this(roomId, null, viewType, rate, capacity, DEFAULT_MAX_EXTRA_GUESTS);
    }

    public Room(String roomId, String roomType, String viewType, int capacity,
                int maxExtraGuests) {
        this(roomId, roomType, viewType, RoomRates.getNightlyRate(roomType, viewType),
                capacity, maxExtraGuests);
    }

    public Room(String roomId, String roomType, String viewType, double rate,
                int capacity, int maxExtraGuests) {
        String resolvedRoomType = roomType == null
                ? getClass().getSimpleName().replace("Room", "") : roomType.trim();
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID is required.");
        }
        if (resolvedRoomType.isEmpty()) {
            throw new IllegalArgumentException("Room type is required.");
        }
        if (viewType == null || viewType.trim().isEmpty()) {
            throw new IllegalArgumentException("Room view is required.");
        }
        if (!Double.isFinite(rate) || rate < 0) {
            throw new IllegalArgumentException("Room rate must be finite and non-negative.");
        }
        if (capacity < 1 || maxExtraGuests < 0) {
            throw new IllegalArgumentException("Room guest limits are invalid.");
        }
        this.roomId = roomId;
        this.roomType = resolvedRoomType;
        this.viewType = viewType;
        this.rate = rate;
        this.capacity = capacity;
        this.maxExtraGuests = maxExtraGuests;
        this.status = RoomStatus.AVAILABLE;
        this.includedAmenities = new ArrayList<>(
                RoomAmenityDefaults.forRoomType(getRoomType()));
    }

    public String getRoomId() { return roomId; }
    public String getRoomType() {
        return roomType == null || roomType.isEmpty()
                ? getClass().getSimpleName().replace("Room", "") : roomType;
    }
    public String getViewType() { return viewType; }
    public double getRate() { return rate; }
    public int getCapacity() { return capacity; }
    public int getMaxExtraGuests() {
        return maxExtraGuests == null ? DEFAULT_MAX_EXTRA_GUESTS : maxExtraGuests;
    }
    public void setMaxExtraGuests(int maxExtraGuests) {
        if (maxExtraGuests < 0) {
            throw new IllegalArgumentException("Extra guest allowance cannot be negative.");
        }
        this.maxExtraGuests = maxExtraGuests;
    }
    public int getMaximumGuestCount() { return capacity + getMaxExtraGuests(); }
    public RoomStatus getStatus() { return status; }
    RoomStatus getStatusBeforeArchive() { return statusBeforeArchive; }
    void setStatusBeforeArchive(RoomStatus statusBeforeArchive) {
        this.statusBeforeArchive = statusBeforeArchive;
    }
    public List<String> getIncludedAmenities() {
        if (includedAmenities == null) {
            includedAmenities = new ArrayList<>(RoomAmenityDefaults.forRoomType(
                    getRoomType()));
        }
        return Collections.unmodifiableList(includedAmenities);
    }
    public void setStatus(RoomStatus status) { this.status = status; }

    public void setIncludedAmenities(List<String> amenities) {
        if (amenities == null) {
            throw new IllegalArgumentException("Room amenities are required.");
        }
        List<String> cleanAmenities = new ArrayList<>();
        for (String amenity : amenities) {
            String cleanAmenity = amenity == null ? "" : amenity.trim();
            if (cleanAmenity.isEmpty()) {
                continue;
            }
            boolean duplicate = false;
            for (String existing : cleanAmenities) {
                if (existing.equalsIgnoreCase(cleanAmenity)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                cleanAmenities.add(cleanAmenity);
            }
        }
        includedAmenities = cleanAmenities;
    }

    public void archive() {
        if (status != RoomStatus.ARCHIVED) {
            statusBeforeArchive = status;
            status = RoomStatus.ARCHIVED;
        }
    }

    public void restoreFromArchive() {
        if (status == RoomStatus.ARCHIVED) {
            status = statusBeforeArchive == null
                    ? RoomStatus.HOUSEKEEPING_REQUIRED : statusBeforeArchive;
            statusBeforeArchive = null;
        }
    }

    public String getStatusLabel() {
        if (status == RoomStatus.ARCHIVED) {
            return "Archived";
        }
        if (status == RoomStatus.INSPECTION_REQUIRED) {
            return "Inspection required";
        }
        if (status == RoomStatus.REPAIR_REQUIRED) {
            return "Maintenance required";
        }
        if (status == RoomStatus.HOUSEKEEPING_REQUIRED || status == RoomStatus.MAINTENANCE) {
            return "Needs housekeeping";
        }
        return status == null ? "Unknown" : status.name().charAt(0)
                + status.name().substring(1).toLowerCase().replace('_', ' ');
    }

    public double calculateRate() { return rate; }

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        return isAvailable(checkIn, checkOut,
                DataManager.getInstance().getAllBookings(), null);
    }

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut, Booking ignoredBooking) {
        return isAvailable(checkIn, checkOut,
                DataManager.getInstance().getAllBookings(), ignoredBooking);
    }

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut,
                               List<Booking> existingBookings) {
        return isAvailable(checkIn, checkOut, existingBookings, null);
    }

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut,
                               List<Booking> existingBookings, Booking ignoredBooking) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)
                || checkIn.isBefore(LocalDate.now())) {
            return false;
        }
        if (existingBookings == null) {
            throw new IllegalArgumentException("Existing bookings are required.");
        }
        if (status == RoomStatus.ARCHIVED) {
            return false;
        }
        if (status == RoomStatus.INSPECTION_REQUIRED
                || status == RoomStatus.REPAIR_REQUIRED
                || status == RoomStatus.HOUSEKEEPING_REQUIRED
                || status == RoomStatus.MAINTENANCE) {
            return false;
        }
        for (Booking b : existingBookings) {
            if (b != ignoredBooking
                    && b.getRoom().getRoomId().equals(roomId)
                    && b.getStatus() != BookingStatus.CANCELLED
                    && b.getStatus() != BookingStatus.CHECKED_OUT) {
                boolean overlaps = checkIn.isBefore(b.getCheckOutDate())
                        && checkOut.isAfter(b.getCheckInDate());
                if (overlaps) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Room ID  : " + roomId + "\n" +
               "View Type: " + viewType + "\n" +
               "Capacity : " + capacity + " guests\n" +
               "Rate     : \u20b1" + String.format("%.2f", calculateRate());
    }
}