package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DataPersistenceTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void saveAndLoadPreserveRecordsLinksStatusesAndIdCounters() throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("resortdata.ser");
        DataManager original = new DataManager(dataFile);
        Room room = new StandardRoom("R9", "Garden", 2500.0);
        room.setIncludedAmenities(java.util.Arrays.asList(
                "Welcome basket", "Late checkout"));
        Guest guest = new Guest("G8", "Persisted Guest", "test contact");
        LocalDate checkIn = LocalDate.now().plusDays(5);
        Booking booking = new Booking("B7", guest, room,
                checkIn, checkIn.plusDays(2), 2, 500.0);
        AddOn addOn = new AddOn("A4", "Breakfast", 450.0, AddOnCategory.FOOD,
                "Filipino breakfast", AddOnChargeBasis.PER_PERSON);
        booking.addAddOn(addOn, 2);
        Staff staff = new Staff("S6", "Persisted Staff", "Housekeeping");
        Task task = new Task("T5", "Prepare room", staff);
        staff.assignTask(task);
        original.addRoom(room);
        original.addGuest(guest);
        original.addStaff(staff);
        original.addAddOn(addOn);
        original.addTask(task);
        original.addBooking(booking);
        booking.checkIn();
        booking.setLateCheckoutSelected(true);
        booking.checkOut();
        room.setStatus(RoomStatus.INSPECTION_REQUIRED);
        Bill bill = new Bill("BL3", booking);
        bill.generateBill();
        original.addBill(bill);
        original.setExtraPersonFeePerNight(725.0);
        original.addAccount(new Admin("admin", "admin123".toCharArray()));
        original.addAccount(new Employee("operator", "SafePassword123".toCharArray()));
        original.saveData();
        original.saveData();

        DataManager restored = new DataManager(dataFile);
        restored.loadData();

        assertEquals(1, restored.getAllRooms().size());
        assertEquals(1, restored.getAllGuests().size());
        assertEquals(1, restored.getAllBookings().size());
        assertEquals(1, restored.getAllStaff().size());
        assertEquals(1, restored.getAllAddOns().size());
        assertEquals(1, restored.getAllTasks().size());
        assertEquals(1, restored.getAllBills().size());
        assertEquals(RoomStatus.INSPECTION_REQUIRED,
                restored.getAllRooms().get(0).getStatus());
        assertEquals(java.util.Arrays.asList("Welcome basket", "Late checkout"),
                restored.getAllRooms().get(0).getIncludedAmenities());
        assertEquals(BookingStatus.CHECKED_OUT, restored.getAllBookings().get(0).getStatus());
        assertSame(restored.getAllRooms().get(0), restored.getAllBookings().get(0).getRoom());
        assertSame(restored.getAllGuests().get(0), restored.getAllBookings().get(0).getGuest());
        assertEquals(1, restored.getAllBookings().get(0).getAddOns().size());
        AddOn restoredBreakfast = restored.getAllBookings().get(0).getAddOns().get(0);
        assertEquals(AddOnChargeBasis.PER_PERSON, restoredBreakfast.getChargeBasis());
        assertEquals(2,
                restored.getAllBookings().get(0).getAddOnQuantity(restoredBreakfast));
        assertEquals(900.0, restored.getAllBills().get(0).getAddOnCharge(), 0.0);
        assertSame(restored.getAllStaff().get(0),
                restored.getAllTasks().get(0).getAssignedTo());
        assertEquals(1, restored.getAllStaff().get(0).getTasks().size());
        assertEquals(bill.getTotalAmount(),
                restored.getAllBills().get(0).getTotalAmount(), 0.0);
        assertEquals(725.0, restored.getExtraPersonFeePerNight(), 0.0);
        assertTrue(restored.getAllBookings().get(0).isLateCheckoutSelected());
        assertEquals(1250.0,
                restored.getAllBookings().get(0).getLateCheckoutCharge(), 0.0);
        assertTrue(findAccount(restored, "admin").isPasswordChangeRequired());
        assertFalse(findAccount(restored, "operator").isPasswordChangeRequired());
        assertEquals("R10", restored.generateRoomId());
        assertEquals("G9", restored.generateGuestId());
        assertEquals("B8", restored.generateBookingId());
    }

    @Test
    public void legacySerializationIsImportedIntoTheSqliteFile() throws Exception {
        Path legacyFile = temporaryFolder.getRoot().toPath().resolve("old-install.ser");
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("resortdata.db");
        DataManager.DataSnapshot snapshot = new DataManager.DataSnapshot();
        snapshot.rooms = java.util.Arrays.asList(new StandardRoom("R1", "Garden", 2500.0));
        snapshot.guests = java.util.Arrays.asList(new Guest("G1", "Legacy Guest", "legacy contact"));
        snapshot.bookings = new java.util.ArrayList<>();
        Staff staff = new Staff("S1", "Legacy Staff", "Housekeeping");
        Task task = new Task("T1", "Legacy room task", staff);
        staff.assignTask(task);
        snapshot.staffList = java.util.Arrays.asList(staff);
        snapshot.addOns = new java.util.ArrayList<>();
        snapshot.tasks = java.util.Arrays.asList(task);
        snapshot.accounts = java.util.Arrays.asList(
                new Admin("legacy.admin", "LegacyPassword123".toCharArray()));
        snapshot.bills = new java.util.ArrayList<>();
        snapshot.extraPersonFeePerNight = 500.0;
        try (ObjectOutputStream output = new ObjectOutputStream(
                Files.newOutputStream(legacyFile))) {
            output.writeObject(snapshot);
        }

        DataManager restored = new DataManager(dataFile, legacyFile);
        restored.loadData();

        assertEquals(1, restored.getAllRooms().size());
        assertEquals("Legacy Guest", restored.getAllGuests().get(0).getName());
        assertEquals("Legacy room task", restored.getAllTasks().get(0).getDescription());
        assertEquals("S1", restored.getAllTasks().get(0).getAssignedTo().getStaffId());
        assertEquals(TaskStatus.PENDING, restored.getAllTasks().get(0).getTaskStatus());
        byte[] sqliteHeader = new byte[16];
        try (java.io.InputStream input = Files.newInputStream(dataFile)) {
            assertEquals(16, input.read(sqliteHeader));
        }
        assertEquals("SQLite format 3\0",
                new String(sqliteHeader, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    public void removingARecordIsPersistedWithoutClearingUnrelatedRows() throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("resortdata.db");
        DataManager manager = new DataManager(dataFile);
        Room roomToRemove = new StandardRoom("R1", "Garden", 2500.0);
        Room roomToKeep = new StandardRoom("R2", "Pool", 3000.0);
        manager.addRoom(roomToRemove);
        manager.addRoom(roomToKeep);
        manager.saveData();

        assertTrue(manager.removeRoom(roomToRemove.getRoomId()));
        manager.saveData();

        DataManager restored = new DataManager(dataFile);
        restored.loadData();
        assertEquals(1, restored.getAllRooms().size());
        assertEquals(roomToKeep.getRoomId(), restored.getAllRooms().get(0).getRoomId());
    }

    @Test
    public void existingSqliteSchemaGainsAddOnPricingColumns() throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("resortdata.db");
        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(
                "jdbc:sqlite:" + dataFile);
             java.sql.Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE add_ons (add_on_id TEXT PRIMARY KEY, "
                    + "name TEXT NOT NULL, price REAL NOT NULL, category TEXT NOT NULL, "
                    + "details TEXT NOT NULL)");
            statement.execute("CREATE TABLE booking_add_ons (booking_id TEXT NOT NULL, "
                    + "position INTEGER NOT NULL, add_on_id TEXT NOT NULL, name TEXT NOT NULL, "
                    + "price REAL NOT NULL, category TEXT NOT NULL, details TEXT NOT NULL, "
                    + "PRIMARY KEY(booking_id, position))");
        }

        DataManager restored = new DataManager(dataFile);
        restored.loadData();

        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(
                    "jdbc:sqlite:" + dataFile);
             java.sql.Statement statement = connection.createStatement();
             java.sql.ResultSet columns = statement.executeQuery(
                     "PRAGMA table_info(booking_add_ons)")) {
            boolean hasQuantity = false;
            boolean hasChargeBasis = false;
            while (columns.next()) {
                hasQuantity |= "quantity".equals(columns.getString("name"));
                hasChargeBasis |= "charge_basis".equals(columns.getString("name"));
            }
            assertTrue(hasQuantity);
            assertTrue(hasChargeBasis);
        }
        assertTrue(restored.getAllAddOns().isEmpty());
    }

    @Test
    public void legacyStaffAndTaskSerializationIdsStayCompatible() {
        assertEquals(-6582291333892535345L,
                ObjectStreamClass.lookup(Staff.class).getSerialVersionUID());
        assertEquals(63474732771090102L,
                ObjectStreamClass.lookup(Task.class).getSerialVersionUID());
    }

    @Test
    public void loadAcceptsAccountRecordsSerializedBeforePasswordConstructorChange()
            throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("legacy-admin.ser");
        DataManager original = new DataManager(dataFile);
        original.addAccount(new Admin("legacy.admin", "SafePassword123".toCharArray()));
        original.addAccount(new Employee("legacy.employee", "SafePassword456".toCharArray()));
        original.saveData();

        assertEquals(5390203282765632553L,
                ObjectStreamClass.lookup(Admin.class).getSerialVersionUID());
        assertEquals(8475601791189894288L,
                ObjectStreamClass.lookup(Employee.class).getSerialVersionUID());

        DataManager restored = new DataManager(dataFile);
        restored.loadData();

        Account admin = restored.getAllAccounts().get(0);
        assertEquals("legacy.admin", admin.getUsername());
        assertTrue(admin.isAdmin());
        assertTrue(admin.checkPassword("SafePassword123"));

        Account employee = restored.getAllAccounts().get(1);
        assertEquals("legacy.employee", employee.getUsername());
        assertFalse(employee.isAdmin());
        assertTrue(employee.checkPassword("SafePassword456"));
    }

    @Test
    public void changedNewEmployeePasswordWorksAfterSaveAndReload() throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("accounts.ser");
        DataManager original = new DataManager(dataFile);
        Staff staff = new Staff("S1", "New Employee", "Maintenance");
        original.addStaff(staff);
        original.addEmployeeAccount("new.employee", "TemporaryPassword1",
                staff.getStaffId());

        Account firstLogin = original.login("new.employee", "TemporaryPassword1");
        assertTrue(firstLogin.isPasswordChangeRequired());
        original.changeAccountPassword("new.employee", "UpdatedPassword123");
        original.saveData();

        DataManager restored = new DataManager(dataFile);
        restored.loadData();

        assertNull(restored.login("new.employee", "TemporaryPassword1"));
        Account account = restored.login("new.employee", "UpdatedPassword123");
        assertTrue(account != null);
        assertFalse(account.isPasswordChangeRequired());
    }

    @Test
    public void successfulLegacyPasswordLoginPersistsTheHashUpgrade() throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("legacy-account.ser");
        DataManager original = new DataManager(dataFile);
        Account legacyAccount = new Employee("legacy.user", "InitialPassword123".toCharArray());
        Field passwordField = Account.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(legacyAccount, "LegacyPassword123");
        original.addAccount(legacyAccount);
        original.saveData();

        DataManager firstReload = new DataManager(dataFile);
        firstReload.loadData();
        Account authenticated = firstReload.login("legacy.user", "LegacyPassword123");

        assertTrue(authenticated != null);
        assertFalse(authenticated.hasLegacyPassword());

        DataManager secondReload = new DataManager(dataFile);
        secondReload.loadData();
        Account persisted = secondReload.login("legacy.user", "LegacyPassword123");
        assertTrue(persisted != null);
        assertFalse(persisted.hasLegacyPassword());
    }

    @Test
    public void failedPasswordSaveRestoresThePreviousCredentials() throws Exception {
        Path blocker = temporaryFolder.getRoot().toPath().resolve("not-a-directory");
        Files.write(blocker, new byte[] { 9, 8, 7 });
        DataManager manager = new DataManager(blocker.resolve("resortdata.ser"));
        Account account = new Employee("operator", "OriginalPassword123".toCharArray());
        manager.addAccount(account);

        try {
            manager.changeAccountPasswordAndSave(
                    "operator", "ReplacementPassword123".toCharArray());
            fail("Expected password persistence to fail.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }

        assertTrue(account.checkPassword("OriginalPassword123"));
        assertFalse(account.checkPassword("ReplacementPassword123"));
    }

    @Test
    public void corruptSaveFailsWithoutReplacingCurrentInMemoryData() throws Exception {
        Path dataFile = temporaryFolder.getRoot().toPath().resolve("corrupt.ser");
        Files.write(dataFile, new byte[] { 1, 2, 3, 4 });
        DataManager manager = new DataManager(dataFile);
        Room existingRoom = new StandardRoom("R1", "Garden", 2500.0);
        manager.addRoom(existingRoom);

        try {
            manager.loadData();
            fail("Expected corrupt saved data to fail loading.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }

        assertEquals(1, manager.getAllRooms().size());
        assertSame(existingRoom, manager.getAllRooms().get(0));
    }

    @Test
    public void saveFailureDoesNotOverwriteExistingFile() throws Exception {
        Path blocker = temporaryFolder.getRoot().toPath().resolve("not-a-directory");
        Files.write(blocker, new byte[] { 9, 8, 7 });
        Path dataFile = blocker.resolve("resortdata.ser");
        DataManager manager = new DataManager(dataFile);
        manager.addRoom(new StandardRoom("R1", "Garden", 2500.0));

        try {
            manager.saveData();
            fail("Expected saving below a regular file to fail.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }

        assertTrue(Files.exists(blocker));
        assertEquals(3, Files.readAllBytes(blocker).length);
    }

    @Test
    public void failedBookingSaveRollsBackBookingAndGuestInMemory() throws Exception {
        Path blocker = temporaryFolder.getRoot().toPath().resolve("not-a-directory");
        Files.write(blocker, new byte[] { 9, 8, 7 });
        DataManager manager = new DataManager(blocker.resolve("resortdata.ser"));
        Room room = new StandardRoom("R1", "Garden", 2500.0);
        Guest guest = new Guest("G1", "Test Guest", "test contact");
        LocalDate checkIn = LocalDate.now().plusDays(5);
        Booking booking = new Booking("B1", guest, room, checkIn,
                checkIn.plusDays(1), 1, 500.0);
        manager.addRoom(room);

        try {
            manager.createAndSaveBooking(booking);
            fail("Expected saving the booking to fail.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }

        assertTrue(manager.getAllBookings().isEmpty());
        assertTrue(manager.getAllGuests().isEmpty());
    }

    private static Account findAccount(DataManager dataManager, String username) {
        for (Account account : dataManager.getAllAccounts()) {
            if (username.equalsIgnoreCase(account.getUsername())) {
                return account;
            }
        }
        return null;
    }

    private static int indexOf(byte[] data, byte[] value) {
        for (int index = 0; index <= data.length - value.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < value.length; offset++) {
                if (data[index + offset] != value[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return index;
            }
        }
        return -1;
    }
}
