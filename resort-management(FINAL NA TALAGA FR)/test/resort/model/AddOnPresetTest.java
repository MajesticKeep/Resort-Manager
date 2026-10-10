package resort.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AddOnPresetTest {

    @Test
    public void everyPresetHasAnAssignedPriceAndDescription() {
        for (AddOnPreset preset : AddOnPreset.values()) {
            assertTrue(preset.getId() + " needs a finite non-negative price.",
                    Double.isFinite(preset.getPrice()) && preset.getPrice() >= 0);
            assertTrue(preset.getId() + " needs a name.",
                    !preset.getName().trim().isEmpty());
            assertTrue(preset.getId() + " needs details.",
                    !preset.getDetails().trim().isEmpty());
            assertTrue(preset.getId() + " should show its suggested price.",
                    preset.toString().contains(String.format("%.2f", preset.getPrice())));
            String details = preset.getDetails().toLowerCase(java.util.Locale.ROOT);
            assertTrue(preset.getId() + " should provide a concrete resort offering.",
                    !details.contains("confirm") && !details.contains("specify")
                            && !details.contains("starter price"));
        }
    }

    @Test
    public void guestServingPresetsUsePerPersonPricing() {
        assertTrue(preset("BREAKFAST").getChargeBasis()
                == AddOnChargeBasis.PER_PERSON);
        assertTrue(preset("ISLAND_HOPPING").getChargeBasis()
                == AddOnChargeBasis.PER_PERSON);
        assertTrue(preset("AIRPORT_TRANSFER").getChargeBasis()
                == AddOnChargeBasis.PER_BOOKING);
        assertTrue(preset("EVENT_PACKAGE").getChargeBasis()
                == AddOnChargeBasis.PER_BOOKING);
    }

    private static AddOnPreset preset(String id) {
        for (AddOnPreset preset : AddOnPreset.values()) {
            if (preset.getId().equals(id)) {
                return preset;
            }
        }
        throw new AssertionError("No configured add-on preset: " + id);
    }
}
