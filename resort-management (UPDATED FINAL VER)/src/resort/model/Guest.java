package resort.model;

import java.io.Serializable;

public class Guest implements Serializable {
    private String guestId;
    private String name;
    private String contactInfo;

    public Guest(String guestId, String name, String contactInfo) {
        this.guestId = requireText(guestId, "Guest ID");
        this.name = requireText(name, "Guest name");
        this.contactInfo = requireText(contactInfo, "Guest contact information");
    }

    public String getGuestId() { return guestId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = requireText(name, "Guest name"); }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) {
        this.contactInfo = requireText(contactInfo, "Guest contact information");
    }

    private static String requireText(String value, String label) {
        String cleanValue = value == null ? "" : value.trim();
        if (cleanValue.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return cleanValue;
    }
}