package resort.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModelHardeningTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void guestRequiresNonBlankIdentityAndContactFields() {
        for (String invalid : new String[] { null, "", "   " }) {
            try {
                new Guest("G1", invalid, "contact");
                fail("Expected invalid guest name to be rejected.");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("Guest name"));
            }
        }

        try {
            new Guest("G1", "Guest", "  ");
            fail("Expected blank guest contact information to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("contact"));
        }
    }

    @Test
    public void configurableRoomUsesItsOwnExtraGuestAllowance() {
        Room noExtraGuests = new Room("R1", "Standard", "Garden View", 2, 0);
        Room extraGuests = new Room("R2", "Villa", "Beachfront View", 8, 4);

        assertEquals(2, noExtraGuests.getMaximumGuestCount());
        assertEquals(12, extraGuests.getMaximumGuestCount());
        noExtraGuests.setMaxExtraGuests(1);
        assertEquals(3, noExtraGuests.getMaximumGuestCount());
    }

    @Test
    public void bookingRulesAcceptExplicitlySuppliedDependencies() {
        Room room = new Room("R1", "Standard", "Garden View", 2, 1);
        LocalDate checkIn = LocalDate.now().plusDays(2);
        assertTrue(room.isAvailable(checkIn, checkIn.plusDays(1),
                java.util.Collections.emptyList()));

        Booking booking = new Booking("B1",
                new Guest("G1", "Guest", "contact"), room,
                checkIn, checkIn.plusDays(2), 3, 725.0);
        assertEquals(1450.0, booking.getExtraPersonCharge(), 0.0);
    }

    @Test
    public void taskStatusRejectsArbitraryValues() {
        Task task = new Task("T1", "Clean room", null);
        assertEquals(TaskStatus.PENDING, task.getTaskStatus());
        task.setStatus(TaskStatus.COMPLETED);
        assertEquals("COMPLETED", task.getStatus());
        try {
            task.setStatus("DONE-ISH");
            fail("Expected unsupported task status to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unknown task status"));
        }
    }

    @Test
    public void concurrentIdGenerationDoesNotDuplicateIdentifiers() throws Exception {
        DataManager manager = new DataManager(temporaryFolder.getRoot().toPath()
                .resolve("data.ser"));
        Set<String> ids = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int thread = 0; thread < 8; thread++) {
            executor.submit(() -> {
                start.await();
                for (int i = 0; i < 250; i++) {
                    ids.add(manager.generateBookingId());
                }
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        Set<String> expected = new HashSet<>();
        for (int i = 1; i <= 2000; i++) {
            expected.add("B" + i);
        }
        assertEquals(expected, ids);
    }
}
