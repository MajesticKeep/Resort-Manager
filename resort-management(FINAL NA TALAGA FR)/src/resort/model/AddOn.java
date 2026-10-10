package resort.model;

import java.io.Serializable;

public class AddOn implements Serializable {
    private static final long serialVersionUID = 1039328717530730073L;

    private String addOnId;
    private String name;
    private double price;
    private AddOnCategory category;
    private String details;
    private AddOnChargeBasis chargeBasis;

    public AddOn(String addOnId, String name, double price, AddOnCategory category) {
        this(addOnId, name, price, category, "");
    }

    public AddOn(String addOnId, String name, double price, AddOnCategory category,
                 String details) {
        this(addOnId, name, price, category, details, AddOnChargeBasis.PER_BOOKING);
    }

    public AddOn(String addOnId, String name, double price, AddOnCategory category,
                 String details, AddOnChargeBasis chargeBasis) {
        this.addOnId = addOnId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.details = details == null ? "" : details.trim();
        this.chargeBasis = chargeBasis;
    }

    public String getAddOnId() { return addOnId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public AddOnCategory getCategory() { return category; }
    public void setCategory(AddOnCategory category) { this.category = category; }
    public AddOnChargeBasis getChargeBasis() {
        return chargeBasis == null ? AddOnChargeBasis.PER_BOOKING : chargeBasis;
    }
    public void setChargeBasis(AddOnChargeBasis chargeBasis) {
        this.chargeBasis = chargeBasis;
    }
    public String getDetails() { return details == null ? "" : details; }
    public void setDetails(String details) { this.details = details == null ? "" : details.trim(); }

    @Override
    public String toString() {
        String description = getDetails().isEmpty() ? "" : " - " + getDetails();
        return name + " - \u20b1" + String.format("%.2f", price)
                + " " + getChargeBasis().getDisplayName() + " (" + category + ")"
                + description;
    }
}