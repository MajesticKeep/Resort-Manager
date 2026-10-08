package resort.model;

public enum AddOnCategory {
    FOOD,
    ACTIVITY,
    TRANSPORT,
    EQUIPMENT,
    SERVICE,
    OTHER,
    TOURS,
    WELLNESS,
    EVENTS;

    @Override
    public String toString() {
        switch (this) {
            case FOOD: return "Food & Drinks";
            case ACTIVITY: return "Activities & Experiences";
            case TRANSPORT: return "Transport";
            case EQUIPMENT: return "Beach & Water Equipment";
            case SERVICE: return "Resort Services";
            case TOURS: return "Tours & Excursions";
            case WELLNESS: return "Spa & Wellness";
            case EVENTS: return "Events & Celebrations";
            case OTHER: return "Others";
            default: throw new AssertionError("Unhandled add-on category: " + this);
        }
    }
}
