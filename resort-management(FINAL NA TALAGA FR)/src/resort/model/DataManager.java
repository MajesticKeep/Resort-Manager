package resort.model;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.SwingUtilities;

public class DataManager {

    private static volatile DataManager instance;
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "resort-data-save");
        thread.setDaemon(true);
        return thread;
    });
    private final Path dataFile;
    private final Path legacyDataFile;

    private List<Room> rooms;
    private List<Guest> guests;
    private List<Booking> bookings;
    private List<Staff> staffList;
    private List<AddOn> addOns;
    private List<Task> tasks;
    private List<Account> accounts;
    private List<Bill> bills;
    private double extraPersonFeePerNight = 500.0;

    // Centralized ID counters — generating IDs per-screen (each screen
    // keeping its own counter) risked two different screens producing the
    // same ID for two different objects of the same type. One counter per
    // entity type, owned by the Singleton, avoids that entirely.
    private final AtomicInteger roomCounter = new AtomicInteger(1);
    private final AtomicInteger guestCounter = new AtomicInteger(1);
    private final AtomicInteger bookingCounter = new AtomicInteger(1);
    private final AtomicInteger staffCounter = new AtomicInteger(1);
    private final AtomicInteger addOnCounter = new AtomicInteger(1);
    private final AtomicInteger taskCounter = new AtomicInteger(1);
    private final AtomicInteger billCounter = new AtomicInteger(1);

    private DataManager() {
        this(new File(System.getProperty("user.home"), "resortdata.db").toPath(),
                new File(System.getProperty("user.home"), "resortdata.ser").toPath());
    }

    DataManager(Path dataFile) {
        this(dataFile, null);
    }

    DataManager(Path dataFile, Path legacyDataFile) {
        if (dataFile == null) {
            throw new IllegalArgumentException("A data file path is required.");
        }
        this.dataFile = dataFile.toAbsolutePath();
        this.legacyDataFile = legacyDataFile == null ? null : legacyDataFile.toAbsolutePath();
        rooms = new ArrayList<>();
        guests = new ArrayList<>();
        bookings = new ArrayList<>();
        staffList = new ArrayList<>();
        addOns = new ArrayList<>();
        tasks = new ArrayList<>();
        accounts = new ArrayList<>();
        bills = new ArrayList<>();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public String generateRoomId() {
        return "R" + roomCounter.getAndIncrement();
    }

    public String generateGuestId() {
        return "G" + guestCounter.getAndIncrement();
    }

    public String generateBookingId() {
        return "B" + bookingCounter.getAndIncrement();
    }

    public String generateStaffId() {
        return "S" + staffCounter.getAndIncrement();
    }

    public String generateAddOnId() {
        return "A" + addOnCounter.getAndIncrement();
    }

    public String generateTaskId() {
        return "T" + taskCounter.getAndIncrement();
    }

    public String generateBillId() {
        return "BL" + billCounter.getAndIncrement();
    }

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public List<Room> getAllRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public void editRoom(String roomId, Room updatedRoom) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoomId().equals(roomId)) {
                rooms.set(i, updatedRoom);
                return;
            }
        }
    }

    public boolean updateRoomStatus(String roomId, RoomStatus status) {
        Room room = findRoom(roomId);
        if (room == null || room.getStatus() == RoomStatus.ARCHIVED
                || (status != RoomStatus.OCCUPIED
                        && status != RoomStatus.INSPECTION_REQUIRED)) {
            return false;
        }
        setRoomStatus(roomId, status);
        return true;
    }

    public void completeRoomInspection(String roomId, boolean damageFound) {
        Room room = requireRoomStatus(roomId, RoomStatus.INSPECTION_REQUIRED);
        if (damageFound) {
            setRoomStatus(roomId, RoomStatus.REPAIR_REQUIRED);
            Task maintenanceTask = new Task(generateTaskId(),
                    "[Maintenance] Inspect and repair reported damage in room "
                            + room.getRoomId(),
                    null);
            addTask(maintenanceTask);
        } else {
            setRoomStatus(roomId, RoomStatus.HOUSEKEEPING_REQUIRED);
        }
    }

    public void completeRoomMaintenance(String roomId) {
        Room room = requireRoomStatus(roomId, RoomStatus.REPAIR_REQUIRED);
        setRoomStatus(roomId, RoomStatus.HOUSEKEEPING_REQUIRED);
        String ticketPrefix = "[Maintenance] Inspect and repair reported damage in room "
                + roomId;
        for (Task task : tasks) {
            if (ticketPrefix.equals(task.getDescription())) {
                task.setStatus(TaskStatus.COMPLETED);
            }
        }
    }

    public void completeMaintenanceTask(Task task) {
        if (task == null || !tasks.contains(task)
                || task.getDescription() == null
                || !task.getDescription().startsWith("[Maintenance]")) {
            throw new IllegalArgumentException("Select a valid maintenance ticket.");
        }
        if (task.getAssignedTo() == null || !task.getAssignedTo().isMaintenance()) {
            throw new IllegalStateException(
                    "Only the assigned Maintenance team can complete this ticket.");
        }
        for (Room room : rooms) {
            String expectedDescription = "[Maintenance] Inspect and repair reported damage in room "
                    + room.getRoomId();
            if (expectedDescription.equals(task.getDescription())
                    && room.getStatus() == RoomStatus.REPAIR_REQUIRED) {
                completeRoomMaintenance(room.getRoomId());
                return;
            }
        }
        throw new IllegalStateException(
                "The room is no longer waiting for this maintenance ticket.");
    }

    public void completeRoomCleaning(String roomId) {
        Room room = requireRoomStatus(roomId, RoomStatus.HOUSEKEEPING_REQUIRED,
                RoomStatus.MAINTENANCE);
        setRoomStatus(roomId, RoomStatus.AVAILABLE);
    }

    private void setRoomStatus(String roomId, RoomStatus status) {
        Room room = findRoom(roomId);
        if (room != null) {
            room.setStatus(status);
        }
        for (Booking booking : bookings) {
            if (booking.getRoom().getRoomId().equals(roomId)) {
                booking.getRoom().setStatus(status);
            }
        }
    }

    private Room requireRoomStatus(String roomId, RoomStatus... allowedStatuses) {
        Room room = findRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("The selected room no longer exists.");
        }
        for (RoomStatus allowedStatus : allowedStatuses) {
            if (room.getStatus() == allowedStatus) {
                return room;
            }
        }
        throw new IllegalStateException("Room " + roomId + " is "
                + room.getStatusLabel() + " and cannot perform this step.");
    }

    public boolean removeRoom(String roomId) {
        if (hasRoomBookings(roomId)) {
            return false;
        }
        return rooms.removeIf(r -> r.getRoomId().equals(roomId));
    }

    public boolean hasRoomBookings(String roomId) {
        for (Booking booking : bookings) {
            if (booking.getRoom().getRoomId().equals(roomId)) {
                return true;
            }
        }
        return false;
    }

    public void archiveRoom(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("The selected room no longer exists.");
        }
        if (room.getStatus() == RoomStatus.ARCHIVED) {
            throw new IllegalStateException("The room is already archived.");
        }
        for (Booking booking : bookings) {
            if (booking.getRoom().getRoomId().equals(roomId)
                    && (booking.getStatus() == BookingStatus.RESERVED
                            || booking.getStatus() == BookingStatus.CHECKED_IN)) {
                throw new IllegalStateException(
                        "This room has an active reservation or checked-in guest. "
                                + "Resolve the booking before archiving the room.");
            }
        }
        room.archive();
    }

    public void restoreRoom(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("The selected room no longer exists.");
        }
        if (room.getStatus() != RoomStatus.ARCHIVED) {
            throw new IllegalStateException("The selected room is not archived.");
        }
        room.restoreFromArchive();
    }

    private Room findRoom(String roomId) {
        for (Room room : rooms) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    public void addGuest(Guest guest) {
        guests.add(guest);
    }

    public List<Guest> getAllGuests() {
        return Collections.unmodifiableList(guests);
    }

    public void addBooking(Booking booking) {
        if (booking == null || booking.getGuest() == null || booking.getRoom() == null) {
            throw new IllegalArgumentException("A booking, guest, and room are required.");
        }
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalArgumentException("A new booking must be reserved.");
        }
        Room room = findRoom(booking.getRoom().getRoomId());
        if (room == null || room.getStatus() == RoomStatus.ARCHIVED) {
            throw new IllegalArgumentException("Select an active room in the resort inventory.");
        }
        if (booking.getCheckInDate() == null || booking.getCheckOutDate() == null
                || !booking.getCheckOutDate().isAfter(booking.getCheckInDate())
                || booking.getCheckInDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Check-in must be today or later and check-out must follow check-in.");
        }
        if (booking.getGuestCount() < 1
                || booking.getGuestCount() > room.getMaximumGuestCount()) {
            throw new IllegalArgumentException(
                    "Guest count must be between 1 and the room's maximum capacity.");
        }
        for (Booking existing : bookings) {
            if (existing.getBookingId().equals(booking.getBookingId())) {
                throw new IllegalArgumentException("A booking with that ID already exists.");
            }
        }
        if (room.getStatus() == RoomStatus.INSPECTION_REQUIRED
                || room.getStatus() == RoomStatus.REPAIR_REQUIRED
                || room.getStatus() == RoomStatus.HOUSEKEEPING_REQUIRED
                || room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("The selected room is not ready for booking.");
        }
        for (Booking existing : bookings) {
            if (existing.getRoom().getRoomId().equals(room.getRoomId())
                    && existing.getStatus() != BookingStatus.CANCELLED
                    && existing.getStatus() != BookingStatus.CHECKED_OUT
                    && booking.getCheckInDate().isBefore(existing.getCheckOutDate())
                    && booking.getCheckOutDate().isAfter(existing.getCheckInDate())) {
                throw new IllegalArgumentException(
                        "The room is not available for the selected dates.");
            }
        }
        bookings.add(booking);
    }

    public void createAndSaveBooking(Booking booking) throws IOException {
        addBooking(booking);
        Guest guest = booking.getGuest();
        guests.add(guest);
        try {
            saveData();
        } catch (IOException ex) {
            bookings.remove(booking);
            guests.remove(guest);
            throw ex;
        }
    }

    public void createAndSaveBookingAsync(Booking booking, Runnable onSuccess,
            Consumer<Exception> onFailure) {
        addBooking(booking);
        Guest guest = booking.getGuest();
        guests.add(guest);
        saveDataAsync(onSuccess, error -> {
            bookings.remove(booking);
            guests.remove(guest);
            onFailure.accept(error);
        });
    }

    public List<Booking> getAllBookings() {
        return Collections.unmodifiableList(bookings);
    }

    public boolean canCancelBookings(Account account) {
        if (account == null || !accounts.contains(account)) {
            return false;
        }
        if (account.isAdmin()) {
            return true;
        }
        if (account.getStaffId() == null) {
            return false;
        }
        for (Staff staff : staffList) {
            if (staff.getStaffId().equals(account.getStaffId())) {
                return staff.getRoleType() == Role.FRONT_DESK;
            }
        }
        return false;
    }

    public void cancelBooking(String bookingId, Account actor) {
        if (!canCancelBookings(actor)) {
            throw new SecurityException(
                    "Only Front Desk employees and administrators can cancel reservations.");
        }
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("The selected booking no longer exists.");
        }
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Only reserved bookings can be cancelled.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }

    public void rescheduleBooking(String bookingId, java.time.LocalDate checkIn,
            java.time.LocalDate checkOut) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("The selected booking no longer exists.");
        }
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Only reserved bookings can be rescheduled.");
        }
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
        if (checkIn.isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }
        if (!booking.getRoom().isAvailable(checkIn, checkOut, bookings, booking)) {
            throw new IllegalArgumentException("The room is not available for those dates.");
        }
        booking.reschedule(checkIn, checkOut);
    }

    private Booking findBooking(String bookingId) {
        for (Booking booking : bookings) {
            if (booking.getBookingId().equals(bookingId)) {
                return booking;
            }
        }
        return null;
    }

    public void addStaff(Staff staff) {
        if (staff == null) {
            throw new IllegalArgumentException("Staff member is required.");
        }
        String name = validateName(staff.getName(), "Staff name");
        if (hasStaffName(name)) {
            throw new IllegalArgumentException("A staff member with that name already exists.");
        }
        staffList.add(staff);
    }

    public List<Staff> getAllStaff() {
        return Collections.unmodifiableList(staffList);
    }

    public void addAddOn(AddOn addOn) {
        if (addOn == null) {
            throw new IllegalArgumentException("Add-on is required.");
        }
        String name = validateName(addOn.getName(), "Add-on name");
        validateAddOn(name, addOn.getPrice(), addOn.getCategory(),
                addOn.getChargeBasis(), null);
        addOn.setName(name);
        addOns.add(addOn);
    }

    public List<AddOn> getAllAddOns() {
        return Collections.unmodifiableList(addOns);
    }

    public void editAddOn(String addOnId, String newName, double newPrice, AddOnCategory newCategory) {
        editAddOn(addOnId, newName, newPrice, newCategory, "");
    }

    public void editAddOn(String addOnId, String newName, double newPrice,
            AddOnCategory newCategory, String newDetails) {
        AddOnChargeBasis existingBasis = null;
        for (AddOn addOn : addOns) {
            if (addOn.getAddOnId().equals(addOnId)) {
                existingBasis = addOn.getChargeBasis();
                break;
            }
        }
        if (existingBasis == null) {
            throw new IllegalArgumentException("The selected add-on no longer exists.");
        }
        editAddOn(addOnId, newName, newPrice, newCategory, newDetails, existingBasis);
    }

    public void editAddOn(String addOnId, String newName, double newPrice,
            AddOnCategory newCategory, String newDetails,
            AddOnChargeBasis newChargeBasis) {
        String cleanName = validateName(newName, "Add-on name");
        validateAddOn(cleanName, newPrice, newCategory, newChargeBasis, addOnId);
        for (int i = 0; i < addOns.size(); i++) {
            AddOn a = addOns.get(i);
            if (a.getAddOnId().equals(addOnId)) {
                addOns.set(i, new AddOn(addOnId, cleanName, newPrice,
                        newCategory, newDetails, newChargeBasis));
                return;
            }
        }
        throw new IllegalArgumentException("The selected add-on no longer exists.");
    }

    public void removeAddOn(String addOnId) {
        addOns.removeIf(a -> a.getAddOnId().equals(addOnId));
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public List<Task> getAllTasks() {
        return Collections.unmodifiableList(tasks);
    }

    // Tickets submitted by Employee have no staff assigned yet — this is
    // what the Staff screen's ticket queue pulls from.
    public List<Task> getUnassignedTasks() {
        List<Task> unassigned = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getAssignedTo() == null
                    && t.getTaskStatus() != TaskStatus.COMPLETED) {
                unassigned.add(t);
            }
        }
        return unassigned;
    }

    public void addBill(Bill bill) {
        bills.add(bill);
    }

    public List<Bill> getAllBills() {
        return Collections.unmodifiableList(bills);
    }

    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account is required.");
        }
        if (hasAccount(account.getUsername())) {
            throw new IllegalArgumentException("That username is already in use.");
        }
        accounts.add(account);
    }

    public List<Account> getAllAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public double getExtraPersonFeePerNight() {
        return extraPersonFeePerNight;
    }

    public void setExtraPersonFeePerNight(double fee) {
        if (!Double.isFinite(fee) || fee < 0) {
            throw new IllegalArgumentException("Extra-person fee must be a finite, non-negative amount.");
        }
        extraPersonFeePerNight = fee;
    }

    public boolean hasAccount(String username) {
        return findAccount(username) != null;
    }

    public void addEmployeeAccount(String username, String password) {
        char[] characters = password == null ? null : password.toCharArray();
        try {
            addEmployeeAccount(username, characters);
        } finally {
            if (characters != null) {
                java.util.Arrays.fill(characters, '\0');
            }
        }
    }

    public void addEmployeeAccount(String username, char[] password) {
        String cleanUsername = username == null ? "" : username.trim();
        validateAccountCredentials(cleanUsername, password);
        if (!cleanUsername.matches("(?i)[a-z0-9._-]{3,40}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-40 characters using letters, numbers, dots, underscores, or hyphens.");
        }
        if (hasAccount(cleanUsername)) {
            throw new IllegalArgumentException("That username is already in use.");
        }
        accounts.add(new Employee(cleanUsername, password));
    }

    public void addEmployeeAccount(String username, String password,
            String staffId) {
        char[] characters = password == null ? null : password.toCharArray();
        try {
            addEmployeeAccount(username, characters, staffId);
        } finally {
            if (characters != null) {
                java.util.Arrays.fill(characters, '\0');
            }
        }
    }

    public void addEmployeeAccount(String username, char[] password,
            String staffId) {
        String cleanUsername = username == null ? "" : username.trim();
        validateAccountCredentials(cleanUsername, password);
        if (!cleanUsername.matches("(?i)[a-z0-9._-]{3,40}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-40 characters using letters, numbers, dots, underscores, or hyphens.");
        }
        if (hasAccount(cleanUsername)) {
            throw new IllegalArgumentException("That username is already in use.");
        }
        Staff staff = findStaff(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("Select an existing staff record.");
        }
        if (isStaffLinkedToAccount(staffId)) {
            throw new IllegalArgumentException(
                    "That staff member is already linked to an account.");
        }

        Employee employee = new Employee(cleanUsername, password);
        employee.setStaffId(staff.getStaffId());
        employee.requirePasswordChange();
        accounts.add(employee);
    }

    public List<Staff> getUnlinkedStaff() {
        List<Staff> unlinked = new ArrayList<>();
        for (Staff staff : staffList) {
            if (!isStaffLinkedToAccount(staff.getStaffId())) {
                unlinked.add(staff);
            }
        }
        return unlinked;
    }

    public void linkEmployeeAccount(String username, String staffId) {
        Account account = findAccount(username);
        if (account == null || account.isAdmin()) {
            throw new IllegalArgumentException("Select an active employee account.");
        }
        Staff staff = findStaff(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("The selected staff member no longer exists.");
        }
        if (isStaffLinkedToAccount(staffId)) {
            throw new IllegalArgumentException(
                    "That staff member is already linked to an account.");
        }
        account.setStaffId(staffId);
    }

    private Staff findStaff(String staffId) {
        for (Staff staff : staffList) {
            if (staff.getStaffId().equals(staffId)) {
                return staff;
            }
        }
        return null;
    }

    private boolean isStaffLinkedToAccount(String staffId) {
        for (Account account : accounts) {
            if (staffId.equals(account.getStaffId())) {
                return true;
            }
        }
        return false;
    }

    public void changeAccountPassword(String username, String newPassword) {
        char[] characters = newPassword == null ? null : newPassword.toCharArray();
        try {
            changeAccountPassword(username, characters);
        } finally {
            if (characters != null) {
                java.util.Arrays.fill(characters, '\0');
            }
        }
    }

    public void changeAccountPassword(String username, char[] newPassword) {
        validateAccountCredentials(username, newPassword);
        Account account = findAccount(username);
        if (account == null) {
            throw new IllegalArgumentException("The selected account no longer exists.");
        }
        account.changePassword(newPassword);
    }

    public void changeAccountPasswordAndSave(String username, char[] newPassword)
            throws IOException {
        validateAccountCredentials(username, newPassword);
        Account account = findAccount(username);
        if (account == null) {
            throw new IllegalArgumentException("The selected account no longer exists.");
        }
        String previousPassword = account.getStoredPassword();
        boolean previousPasswordChangeRequired = account.isPasswordChangeRequired();
        account.changePassword(newPassword);
        try {
            saveData();
        } catch (IOException | RuntimeException ex) {
            account.restorePasswordState(
                    previousPassword, previousPasswordChangeRequired);
            throw ex;
        }
    }

    public void promoteEmployeeAccount(String username) {
        Account account = findAccount(username);
        if (account == null) {
            throw new IllegalArgumentException("The selected account no longer exists.");
        }
        if (account.isAdmin()) {
            throw new IllegalArgumentException("The selected account is already an administrator.");
        }
        account.setAdministratorRole(true);
    }

    public void demoteAdminAccount(String username) {
        Account account = findAccount(username);
        if (account == null) {
            throw new IllegalArgumentException("The selected account no longer exists.");
        }
        if (!account.isAdmin()) {
            throw new IllegalArgumentException("The selected account is not an administrator.");
        }

        long adminCount = accounts.stream().filter(Account::isAdmin).count();
        if (adminCount <= 1) {
            throw new IllegalArgumentException(
                    "The last administrator account cannot be demoted.");
        }

        account.setAdministratorRole(false);
    }

    public void deactivateEmployeeAccount(String username) {
        Account account = findAccount(username);
        if (account == null) {
            throw new IllegalArgumentException("The selected account no longer exists.");
        }
        if (account.isAdmin()) {
            throw new IllegalArgumentException("Administrator accounts cannot be deactivated.");
        }
        accounts.remove(account);
    }

    private Account findAccount(String username) {
        if (username == null) {
            return null;
        }
        for (Account account : accounts) {
            if (account.getUsername().equalsIgnoreCase(username.trim())) {
                return account;
            }
        }
        return null;
    }

    private void validateAccountCredentials(String username, char[] password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
    }

    public Account login(String username, String password) {
        char[] characters = password == null ? null : password.toCharArray();
        try {
            return login(username, characters);
        } finally {
            if (characters != null) {
                java.util.Arrays.fill(characters, '\0');
            }
        }
    }

    public Account login(String username, char[] password) {
        if (username == null || password == null) {
            return null;
        }
        for (Account a : accounts) {
            if (a.getUsername().equalsIgnoreCase(username.trim())) {
                boolean legacyPassword = a.hasLegacyPassword();
                if (!a.checkPassword(password)) {
                    return null;
                }
                if (legacyPassword) {
                    try {
                        saveData();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(
                                "Could not save the upgraded account password.", ex);
                    }
                }
                return a;
            }
        }
        return null;
    }

    private boolean hasStaffName(String name) {
        for (Staff staff : staffList) {
            if (staff.getName() != null && staff.getName().trim().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void validateAddOn(String name, double price, AddOnCategory category,
            AddOnChargeBasis chargeBasis, String ignoredAddOnId) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Price must be a finite, non-negative amount.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Add-on category is required.");
        }
        if (chargeBasis == null) {
            throw new IllegalArgumentException("Add-on charge basis is required.");
        }
        for (AddOn existing : addOns) {
            if (!java.util.Objects.equals(existing.getAddOnId(), ignoredAddOnId)
                    && existing.getName() != null
                    && existing.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("An add-on with that name already exists.");
            }
        }
    }

    private static String validateName(String name, String label) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return cleanName;
    }

    public synchronized void saveData() throws IOException {
        SqliteDataStore.save(dataFile, createSnapshot());
    }

    private DataSnapshot createSnapshot() {
        DataSnapshot snapshot = new DataSnapshot();
        snapshot.rooms = new ArrayList<>(rooms);
        snapshot.guests = new ArrayList<>(guests);
        snapshot.bookings = new ArrayList<>(bookings);
        snapshot.staffList = new ArrayList<>(staffList);
        snapshot.addOns = new ArrayList<>(addOns);
        snapshot.tasks = new ArrayList<>(tasks);
        snapshot.accounts = new ArrayList<>(accounts);
        snapshot.bills = new ArrayList<>(bills);
        snapshot.extraPersonFeePerNight = extraPersonFeePerNight;
        return snapshot;
    }

    public void saveDataAsync(Runnable onSuccess, Consumer<Exception> onFailure) {
        final DataSnapshot snapshot;
        try {
            snapshot = createSnapshot();
        } catch (RuntimeException ex) {
            SwingUtilities.invokeLater(() -> {
                if (onFailure != null) {
                    onFailure.accept(ex);
                } else {
                    throw new IllegalStateException("Could not save resort data.", ex);
                }
            });
            return;
        }
        SAVE_EXECUTOR.execute(() -> {
            Exception failure = null;
            try {
                SqliteDataStore.save(dataFile, snapshot);
            } catch (IOException | RuntimeException ex) {
                failure = ex;
            }
            Exception result = failure;
            SwingUtilities.invokeLater(() -> {
                if (result == null) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else if (onFailure != null) {
                    onFailure.accept(result);
                } else {
                    throw new IllegalStateException("Could not save resort data.", result);
                }
            });
        });
    }

    public void loadData() throws IOException {
        Path sourceFile = Files.exists(dataFile) ? dataFile : legacyDataFile;
        if (sourceFile == null || Files.notExists(sourceFile)) {
            return;
        }
        if (!Files.exists(sourceFile)) {
            throw new IOException("Cannot determine whether the saved resort data exists: "
                    + sourceFile);
        }
        DataSnapshot snapshot;
        if (isSqliteDatabase(sourceFile)) {
            snapshot = SqliteDataStore.load(sourceFile);
        } else {
            snapshot = readLegacySnapshot(sourceFile);
        }
        if (snapshot == null) {
            throw new IOException("The saved resort data is empty.");
        }
        restoreSnapshot(snapshot);
        if (!isSqliteDatabase(sourceFile)) {
            saveData();
        }
    }

    private static boolean isSqliteDatabase(Path file) throws IOException {
        byte[] expected = "SQLite format 3\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] actual = new byte[expected.length];
        try (java.io.InputStream input = Files.newInputStream(file)) {
            int read = input.read(actual);
            if (read < 0) {
                return false;
            }
            return read == expected.length && java.util.Arrays.equals(expected, actual);
        }
    }

    private static DataSnapshot readLegacySnapshot(Path file) throws IOException {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            return (DataSnapshot) input.readObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException("The saved resort data uses an unsupported format.", ex);
        } catch (ClassCastException ex) {
            throw new IOException("The saved resort data uses an unsupported format.", ex);
        }
    }

    private void restoreSnapshot(DataSnapshot snapshot) throws IOException {
        List<Room> loadedRooms = snapshot.rooms == null
                ? new ArrayList<>()
                : snapshot.rooms;
        List<Guest> loadedGuests = snapshot.guests == null
                ? new ArrayList<>()
                : snapshot.guests;
        List<Booking> loadedBookings = snapshot.bookings == null
                ? new ArrayList<>()
                : snapshot.bookings;
        List<Staff> loadedStaff = snapshot.staffList == null
                ? new ArrayList<>()
                : snapshot.staffList;
        List<AddOn> loadedAddOns = snapshot.addOns == null
                ? new ArrayList<>()
                : snapshot.addOns;
        List<Task> loadedTasks = snapshot.tasks == null
                ? new ArrayList<>()
                : snapshot.tasks;
        List<Account> loadedAccounts = snapshot.accounts == null
                ? new ArrayList<>()
                : snapshot.accounts;
        List<Bill> loadedBills = snapshot.bills == null
                ? new ArrayList<>()
                : snapshot.bills;
        requirePasswordChangeForLegacyDefaults(loadedAccounts);
        int loadedRoomCounter = nextCounter(loadedRooms, "R", Room::getRoomId);
        int loadedGuestCounter = nextCounter(loadedGuests, "G", Guest::getGuestId);
        int loadedBookingCounter = nextCounter(loadedBookings, "B", Booking::getBookingId);
        int loadedStaffCounter = nextCounter(loadedStaff, "S", Staff::getStaffId);
        int loadedAddOnCounter = nextCounter(loadedAddOns, "A", AddOn::getAddOnId);
        int loadedTaskCounter = nextCounter(loadedTasks, "T", Task::getTaskId);
        int loadedBillCounter = nextCounter(loadedBills, "BL", Bill::getBillId);

        rooms = loadedRooms;
        guests = loadedGuests;
        bookings = loadedBookings;
        staffList = loadedStaff;
        addOns = loadedAddOns;
        tasks = loadedTasks;
        accounts = loadedAccounts;
        bills = loadedBills;
        if (snapshot.extraPersonFeePerNight != null
                && Double.isFinite(snapshot.extraPersonFeePerNight)
                && snapshot.extraPersonFeePerNight >= 0) {
            extraPersonFeePerNight = snapshot.extraPersonFeePerNight;
        } else if (snapshot.extraPersonFeePerNight != null) {
            System.err.println("Ignoring invalid saved extra-person fee; using default ₱500.00.");
        }

        roomCounter.set(loadedRoomCounter);
        guestCounter.set(loadedGuestCounter);
        bookingCounter.set(loadedBookingCounter);
        staffCounter.set(loadedStaffCounter);
        addOnCounter.set(loadedAddOnCounter);
        taskCounter.set(loadedTaskCounter);
        billCounter.set(loadedBillCounter);
    }

    private static <T> int nextCounter(List<T> records, String prefix,
            Function<T, String> idExtractor) {
        int maximum = 0;
        for (T record : records) {
            String id = idExtractor.apply(record);
            if (id == null || !id.startsWith(prefix)) {
                continue;
            }
            try {
                maximum = Math.max(maximum, Integer.parseInt(id.substring(prefix.length())));
            } catch (NumberFormatException ex) {
                System.err.println("Cannot derive next ID from malformed identifier: " + id);
            }
        }
        return maximum + 1;
    }

    private void requirePasswordChangeForLegacyDefaults(List<Account> loadedAccounts) {
        for (Account account : loadedAccounts) {
            String username = account.getUsername();
            if (("admin".equalsIgnoreCase(username) && account.checkPassword("admin123"))
                    || ("employee".equalsIgnoreCase(username)
                            && account.checkPassword("employee123"))) {
                account.requirePasswordChange();
            }
        }
    }

    static class DataSnapshot implements Serializable {
        private static final long serialVersionUID = 5564476346158060465L;

        List<Room> rooms;
        List<Guest> guests;
        List<Booking> bookings;
        List<Staff> staffList;
        List<AddOn> addOns;
        List<Task> tasks;
        List<Account> accounts;
        List<Bill> bills;
        Double extraPersonFeePerNight;
    }
}
