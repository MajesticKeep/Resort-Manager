package resort.model;

import java.util.List;

public final class AddOnPreset {

    private final String id;
    private final String name;
    private final AddOnCategory category;
    private final double price;
    private final String details;
    private final AddOnChargeBasis chargeBasis;

    AddOnPreset(String id, String name, AddOnCategory category, double price,
                String details, AddOnChargeBasis chargeBasis) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.details = details;
        this.chargeBasis = chargeBasis;
    }

    public static AddOnPreset[] values() {
        List<AddOnPreset> presets = ResortConfig.getInstance().getAddOnPresets();
        return presets.toArray(new AddOnPreset[0]);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AddOnCategory getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public String getDetails() {
        return details;
    }

    public AddOnChargeBasis getChargeBasis() {
        return chargeBasis;
    }

    @Override
    public String toString() {
        return name + " (\u20b1" + String.format("%.2f", price) + " "
                + chargeBasis.getDisplayName() + " - " + category + ")";
    }
}
