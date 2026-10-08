package resort.model;

import java.io.Serializable;

public class Guest implements Serializable {
    private String guestId;
    private String name;
    private String contactInfo;

    public Guest(String guestId, String name, String contactInfo) {
        this.guestId = guestId;
        this.name = name;
        this.contactInfo = contactInfo;
    }

    public String getGuestId() { return guestId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}