package resort.model;

import java.io.Serializable;

public class AddOn implements Serializable {
    private String addOnId;
    private String name;
    private double price;
    private AddOnCategory category;

    public AddOn(String addOnId, String name, double price, AddOnCategory category) {
        this.addOnId = addOnId;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public String getAddOnId() { return addOnId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public AddOnCategory getCategory() { return category; }
    public void setCategory(AddOnCategory category) { this.category = category; }
}