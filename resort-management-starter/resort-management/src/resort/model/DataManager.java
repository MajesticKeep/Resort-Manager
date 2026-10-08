package resort.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private static DataManager instance;
    private static final String DATA_FILE = "resortdata.ser";

    private List<Room> rooms;
    private List<Guest> guests;
    private List<Booking> bookings;
    private List<Staff> staffList;
    private List<AddOn> addOns;
    private List<Task> tasks;
    private List<Account> accounts;

    private DataManager() {
        rooms = new ArrayList<>();
        guests = new ArrayList<>();
        bookings = new ArrayList<>();
        staffList = new ArrayList<>();
        addOns = new ArrayList<>();
        tasks = new ArrayList<>();
        accounts = new ArrayList<>();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void addRoom(Room room) { rooms.add(room); }
    public List<Room> getAllRooms() { return rooms; }
    public void editRoom(String roomId, Room updatedRoom) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoomId().equals(roomId)) {
                rooms.set(i, updatedRoom);
                return;
            }
        }
    }
    public void removeRoom(String roomId) {
        rooms.removeIf(r -> r.getRoomId().equals(roomId));
    }

    public void addGuest(Guest guest) { guests.add(guest); }
    public List<Guest> getAllGuests() { return guests; }

    public void addBooking(Booking booking) { bookings.add(booking); }
    public List<Booking> getAllBookings() { return bookings; }

    public void addStaff(Staff staff) { staffList.add(staff); }
    public List<Staff> getAllStaff() { return staffList; }

    public void addAddOn(AddOn addOn) { addOns.add(addOn); }
    public List<AddOn> getAllAddOns() { return addOns; }
    public void editAddOn(String addOnId, String newName, double newPrice, AddOnCategory newCategory) {
        for (AddOn a : addOns) {
            if (a.getAddOnId().equals(addOnId)) {
                a.setName(newName);
                a.setPrice(newPrice);
                a.setCategory(newCategory);
                return;
            }
        }
    }
    public void removeAddOn(String addOnId) {
        addOns.removeIf(a -> a.getAddOnId().equals(addOnId));
    }

    public void addTask(Task task) { tasks.add(task); }
    public List<Task> getAllTasks() { return tasks; }

    public void addAccount(Account account) { accounts.add(account); }

    public Account login(String username, String password) {
        for (Account a : accounts) {
            if (a.getUsername().equals(username) && a.checkPassword(password)) {
                return a;
            }
        }
        return null;
    }

    public void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            DataSnapshot snapshot = new DataSnapshot();
            snapshot.rooms = this.rooms;
            snapshot.guests = this.guests;
            snapshot.bookings = this.bookings;
            snapshot.staffList = this.staffList;
            snapshot.addOns = this.addOns;
            snapshot.tasks = this.tasks;
            snapshot.accounts = this.accounts;
            out.writeObject(snapshot);
        } catch (IOException e) {
            System.out.println("Failed to save data: " + e.getMessage());
        }
    }

    public void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            DataSnapshot snapshot = (DataSnapshot) in.readObject();
            this.rooms = snapshot.rooms;
            this.guests = snapshot.guests;
            this.bookings = snapshot.bookings;
            this.staffList = snapshot.staffList;
            this.addOns = snapshot.addOns;
            this.tasks = snapshot.tasks;
            this.accounts = snapshot.accounts;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to load data: " + e.getMessage());
        }
    }

    private static class DataSnapshot implements Serializable {
        List<Room> rooms;
        List<Guest> guests;
        List<Booking> bookings;
        List<Staff> staffList;
        List<AddOn> addOns;
        List<Task> tasks;
        List<Account> accounts;
    }
}