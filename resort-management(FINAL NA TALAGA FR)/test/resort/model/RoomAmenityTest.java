package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RoomAmenityTest {

    @Test
    public void roomTypesReceiveTheirComplimentaryDefaults() {
        Room standard = new StandardRoom("R1", "Garden", 2500.0);
        Room suite = new SuiteRoom("R2", "Pool", 6500.0);

        assertTrue(standard.getIncludedAmenities().contains("Complimentary Wi-Fi"));
        assertFalse(standard.getIncludedAmenities().contains("Bathrobes"));
        assertTrue(suite.getIncludedAmenities().contains("Complimentary Wi-Fi"));
        assertTrue(suite.getIncludedAmenities().contains("Bathrobes"));
        assertTrue(suite.getIncludedAmenities().contains("Coffee and tea set"));
    }

    @Test
    public void roomAmenitiesCanBeCustomizedAndAreReadOnlyToCallers() {
        Room room = new StandardRoom("R3", "Garden", 2500.0);
        room.setIncludedAmenities(Arrays.asList(
                "  Welcome basket ", "Welcome basket", "", "Pool access"));

        assertEquals(Arrays.asList("Welcome basket", "Pool access"),
                room.getIncludedAmenities());
        try {
            room.getIncludedAmenities().add("Unexpected charge");
            fail("Expected room amenities to be read-only.");
        } catch (UnsupportedOperationException expected) {
            assertEquals(2, room.getIncludedAmenities().size());
        }
    }

    @Test
    public void missingAmenitiesInOlderSerializedRoomsGetTypeDefaults() throws Exception {
        Room room = new StandardRoom("R4", "Garden", 2500.0);
        java.lang.reflect.Field amenitiesField =
                Room.class.getDeclaredField("includedAmenities");
        amenitiesField.setAccessible(true);
        amenitiesField.set(room, null);

        List<String> amenities = room.getIncludedAmenities();

        assertTrue(amenities.contains("Complimentary Wi-Fi"));
        assertTrue(amenities.contains("Fresh towels"));
    }

    @Test
    public void nullAmenityCollectionIsRejected() {
        Room room = new StandardRoom("R5", "Garden", 2500.0);
        try {
            room.setIncludedAmenities(null);
            fail("Expected a null amenity list to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
