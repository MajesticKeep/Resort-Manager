package resort.ui;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import resort.model.*;

public class BookingScreen extends JFrame {

    private JTextField guestNameField;
    private JTextField guestContactField;
    private JComboBox<Room> roomCombo;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JSpinner guestCountSpinner;
    private JList<AddOn> addOnList;
    private JLabel statusLabel;

    private int guestCounter = 1;
    private int bookingCounter = 1;

    public BookingScreen() {
        setTitle("Resort Management System - Bookings");
        setSize(450, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        guestNameField = new JTextField();
        guestContactField = new JTextField();
        roomCombo = new JComboBox<>(
                DataManager.getInstance().getAllRooms().toArray(new Room[0]));
        checkInField = new JTextField("YYYY-MM-DD");
        checkOutField = new JTextField("YYYY-MM-DD");
        guestCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));

        List<AddOn> addOns = DataManager.getInstance().getAllAddOns();
        addOnList = new JList<>(addOns.toArray(new AddOn[0]));
        addOnList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // NOTE: this only lets you pick each AddOn once per booking. The
        // "attach Extra Person fee N times for N extra guests" idea from
        // earlier needs a quantity spinner per add-on instead of a plain
        // list — left out for now, worth adding later if there's time.
        JScrollPane addOnScroll = new JScrollPane(addOnList);
        addOnScroll.setPreferredSize(new Dimension(380, 80));

        statusLabel = new JLabel(" ");

        JButton checkAvailabilityBtn = new JButton("Check Availability");
        JButton createBookingBtn = new JButton("Create Booking");

        checkAvailabilityBtn.addActionListener(e -> checkAvailability());
        createBookingBtn.addActionListener(e -> createBooking());

        panel.add(labeled("Guest name:", guestNameField));
        panel.add(labeled("Guest contact:", guestContactField));
        panel.add(labeled("Room:", roomCombo));
        panel.add(labeled("Check-in date:", checkInField));
        panel.add(labeled("Check-out date:", checkOutField));
        panel.add(labeled("Guest count:", guestCountSpinner));
        panel.add(new JLabel("Add-ons (ctrl/cmd-click for multiple):"));
        panel.add(addOnScroll);
        panel.add(Box.createVerticalStrut(10));
        panel.add(checkAvailabilityBtn);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(createBookingBtn);

        add(panel);
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return row;
    }

    private void checkAvailability() {
        Room room = (Room) roomCombo.getSelectedItem();
        if (room == null) {
            statusLabel.setText("No rooms available to select. Add rooms first.");
            return;
        }

        LocalDate checkIn;
        LocalDate checkOut;
        try {
            checkIn = LocalDate.parse(checkInField.getText().trim());
            checkOut = LocalDate.parse(checkOutField.getText().trim());
        } catch (DateTimeParseException ex) {
            statusLabel.setText("Dates must be in YYYY-MM-DD format.");
            return;
        }

        boolean available = room.isAvailable(checkIn, checkOut);
        statusLabel.setText(available ? "Room is available." : "Room is NOT available.");
    }

    private void createBooking() {
        Room room = (Room) roomCombo.getSelectedItem();
        if (room == null) {
            JOptionPane.showMessageDialog(this, "No room selected.");
            return;
        }

        String guestName = guestNameField.getText().trim();
        if (guestName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Guest name is required.");
            return;
        }

        LocalDate checkIn;
        LocalDate checkOut;
        try {
            checkIn = LocalDate.parse(checkInField.getText().trim());
            checkOut = LocalDate.parse(checkOutField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be in YYYY-MM-DD format.");
            return;
        }

        int guestCount = (int) guestCountSpinner.getValue();

        // Hard cap enforcement — the decision from earlier. Room shouldn't
        // know about a specific booking's headcount, so this check lives
        // here, not inside Room.
        if (guestCount > room.getCapacity()) {
            JOptionPane.showMessageDialog(this,
                    "Guest count (" + guestCount + ") exceeds this room's maximum capacity ("
                            + room.getCapacity() + "). Choose a larger room type.",
                    "Booking rejected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!room.isAvailable(checkIn, checkOut)) {
            JOptionPane.showMessageDialog(this,
                    "This room is not available for the selected dates.",
                    "Booking rejected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Guest guest = new Guest("G" + guestCounter++, guestName, guestContactField.getText().trim());
        DataManager.getInstance().addGuest(guest);

        Booking booking = new Booking("B" + bookingCounter++, guest, room, checkIn, checkOut, guestCount);
        for (AddOn addOn : addOnList.getSelectedValuesList()) {
            booking.addAddOn(addOn);
        }
        DataManager.getInstance().addBooking(booking);

        JOptionPane.showMessageDialog(this,
                "Booking created.\nBooking ID: " + booking.getBookingId()
                        + "\nGuest count: " + guestCount
                        + "\nTotal: \u20b1" + String.format("%.2f", booking.calculateTotal()));

        dispose();
    }
}