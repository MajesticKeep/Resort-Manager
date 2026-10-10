package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AccountAuthorizationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DataManager dataManager;

    @Before
    public void setUp() throws Exception {
        dataManager = new DataManager(temporaryFolder.newFile().toPath());
    }

    @Test
    public void loginAndPasswordChangeAcceptOnlyCurrentCredentials() {
        String username = unique("employee");
        String password = "OriginalPassword1";
        dataManager.addEmployeeAccount(username, password);

        assertEquals(username, dataManager.login(username.toUpperCase(), password).getUsername());
        assertNull(dataManager.login(username, "incorrect-password"));
        assertNull(dataManager.login(null, password));

        dataManager.changeAccountPassword(username, "NewPassword123");
        assertNull(dataManager.login(username, password));
        assertEquals(username, dataManager.login(username, "NewPassword123").getUsername());
    }

    @Test
    public void cancellationIsLimitedToRegisteredAdminsAndFrontDeskEmployees() {
        Admin administrator = new Admin(unique("admin"), "AdminPassword123".toCharArray());
        dataManager.addAccount(administrator);

        Staff frontDesk = addStaff("Front Desk");
        Employee frontDeskAccount = addLinkedEmployee(frontDesk);
        Staff housekeeping = addStaff("Housekeeping");
        Employee housekeepingAccount = addLinkedEmployee(housekeeping);
        Staff maintenance = addStaff("Repair Technician");
        Employee maintenanceAccount = addLinkedEmployee(maintenance);
        Employee unlinkedAccount = new Employee(unique("unlinked"), "EmployeePassword1".toCharArray());
        dataManager.addAccount(unlinkedAccount);
        Booking booking = createBooking();

        assertTrue(dataManager.canCancelBookings(administrator));
        assertTrue(dataManager.canCancelBookings(frontDeskAccount));
        assertFalse(dataManager.canCancelBookings(housekeepingAccount));
        assertFalse(dataManager.canCancelBookings(maintenanceAccount));
        assertFalse(dataManager.canCancelBookings(unlinkedAccount));
        assertFalse(dataManager.canCancelBookings(
                new Admin(unique("unregistered"), "AdminPassword123".toCharArray())));
        assertFalse(dataManager.canCancelBookings(null));

        assertThrows(SecurityException.class, () ->
                dataManager.cancelBooking(booking.getBookingId(), housekeepingAccount));
        assertEquals(BookingStatus.RESERVED, booking.getStatus());

        dataManager.cancelBooking(booking.getBookingId(), frontDeskAccount);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    public void checkedInBookingsCannotBeCancelled() {
        Admin administrator = new Admin(unique("admin"), "AdminPassword123".toCharArray());
        dataManager.addAccount(administrator);
        Booking booking = createBooking();
        booking.checkIn();

        assertThrows(IllegalStateException.class, () ->
                dataManager.cancelBooking(booking.getBookingId(), administrator));
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
    }

    @Test
    public void accountLifecycleProtectsTheLastAdministrator() {
        String firstAdminName = unique("admin");
        String secondAdminName = unique("admin");
        String employeeName = unique("employee");
        dataManager.addAccount(new Admin(firstAdminName, "AdminPassword123".toCharArray()));
        dataManager.addAccount(new Admin(secondAdminName, "AdminPassword123".toCharArray()));
        dataManager.addEmployeeAccount(employeeName, "EmployeePassword1");
        Account firstAdmin = findAccount(firstAdminName);

        dataManager.demoteAdminAccount(firstAdminName);
        assertSame(firstAdmin, findAccount(firstAdminName));
        assertFalse(findAccount(firstAdminName).isAdmin());
        dataManager.promoteEmployeeAccount(firstAdminName);
        assertSame(firstAdmin, findAccount(firstAdminName));
        assertTrue(firstAdmin.isAdmin());
        dataManager.demoteAdminAccount(firstAdminName);
        assertThrows(IllegalArgumentException.class, () ->
                dataManager.demoteAdminAccount(secondAdminName));
        assertThrows(IllegalArgumentException.class, () ->
                dataManager.deactivateEmployeeAccount(secondAdminName));

        dataManager.deactivateEmployeeAccount(employeeName);
        assertFalse(dataManager.hasAccount(employeeName));
    }

    private Staff addStaff(String role) {
        Staff staff = new Staff(UUID.randomUUID().toString(),
                unique(role.replace(" ", "").toLowerCase()), role);
        dataManager.addStaff(staff);
        return staff;
    }

    private Employee addLinkedEmployee(Staff staff) {
        String username = unique("linked");
        dataManager.addEmployeeAccount(username, "EmployeePassword1", staff.getStaffId());
        return (Employee) findAccount(username);
    }

    private Booking createBooking() {
        Room room = new StandardRoom(UUID.randomUUID().toString(), "Garden", 2500.0);
        dataManager.addRoom(room);
        LocalDate start = LocalDate.now().plusDays(20);
        Booking booking = new Booking(UUID.randomUUID().toString(),
                new Guest(UUID.randomUUID().toString(), "Test Guest", "test"),
                room, start, start.plusDays(1), 1, 500.0);
        dataManager.addBooking(booking);
        return booking;
    }

    private Account findAccount(String username) {
        for (Account account : dataManager.getAllAccounts()) {
            if (username.equalsIgnoreCase(account.getUsername())) {
                return account;
            }
        }
        return null;
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static void assertThrows(Class<? extends RuntimeException> expectedType,
                                     Runnable action) {
        try {
            action.run();
            fail("Expected " + expectedType.getSimpleName() + ".");
        } catch (RuntimeException ex) {
            assertTrue("Unexpected exception: " + ex,
                    expectedType.isInstance(ex));
        }
    }
}
