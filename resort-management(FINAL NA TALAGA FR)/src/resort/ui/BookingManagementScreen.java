package resort.ui;

import resort.model.*;
import resort.controller.BookingController;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BookingManagementScreen extends JPanel {

    private final Account currentAccount;
    private DefaultListModel<Booking> listModel;
    private JList<Booking> bookingJList;
    private JComboBox<String> sortSelector;
    private final BookingController bookingController =
            new BookingController(DataManager.getInstance());

    public BookingManagementScreen(Account currentAccount) {
        this.currentAccount = currentAccount;
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        bookingJList = new JList<>(listModel);
        Theme.styleList(bookingJList);
        bookingJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                Theme.styleListItemSeparator(item);
                return item;
            }
        });

        BeachPanel background = new BeachPanel(new BorderLayout());

        JLabel title = new JLabel("\u2600\uFE0F  Manage Bookings", SwingConstants.CENTER);
        Theme.styleTitleLabel(title);
        title.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        background.add(title, BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(10, 10));
        sortSelector = new JComboBox<>(new String[] {
            "Guest (A-Z)", "Guest (Z-A)", "Status", "Check-in (earliest)",
            "Check-in (latest)", "Room ID (A-Z)", "Total (low-high)",
            "Total (high-low)"
        });
        Theme.styleInput(sortSelector);
        sortSelector.addActionListener(event -> refreshList());
        JPanel sortRow = new JPanel(new BorderLayout(8, 0));
        sortRow.setOpaque(false);
        JLabel sortLabel = new JLabel("Sort bookings by:");
        Theme.styleLabel(sortLabel);
        sortRow.add(sortLabel, BorderLayout.WEST);
        sortRow.add(sortSelector, BorderLayout.CENTER);
        JPanel bookingListArea = new JPanel(new BorderLayout(0, 8));
        bookingListArea.setOpaque(false);
        bookingListArea.add(sortRow, BorderLayout.NORTH);
        bookingListArea.add(new JScrollPane(bookingJList), BorderLayout.CENTER);
        card.add(bookingListArea, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 3, 8, 8));
        buttons.setOpaque(false);
        JButton checkInBtn = new JButton("Check In");
        JButton checkOutBtn = new JButton("Check Out & Bill");
        JButton cancelBtn = new JButton("Cancel Reservation");
        JButton rescheduleBtn = new JButton("Reschedule");
        JButton refreshBtn = new JButton("Refresh");
        Theme.styleButton(checkInBtn);
        Theme.styleButton(checkOutBtn);
        Theme.styleSecondaryButton(cancelBtn);
        Theme.styleSecondaryButton(rescheduleBtn);
        Theme.styleSecondaryButton(refreshBtn);
        cancelBtn.setVisible(DataManager.getInstance().canCancelBookings(currentAccount));

        checkInBtn.addActionListener(e -> checkIn());
        checkOutBtn.addActionListener(e -> checkOutAndBill());
        cancelBtn.addActionListener(e -> cancelReservation());
        rescheduleBtn.addActionListener(e -> rescheduleReservation());
        refreshBtn.addActionListener(e -> refreshList());

        buttons.add(checkInBtn);
        buttons.add(checkOutBtn);
        buttons.add(cancelBtn);
        buttons.add(rescheduleBtn);
        buttons.add(refreshBtn);
        card.add(buttons, BorderLayout.SOUTH);

        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setOpaque(false);
        cardWrap.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        cardWrap.add(card, BorderLayout.CENTER);
        background.add(cardWrap, BorderLayout.CENTER);

        add(background, BorderLayout.CENTER);
        refreshList();
    }

    public void refreshList() {
        Booking selected = bookingJList.getSelectedValue();
        listModel.clear();
        List<Booking> bookings = new ArrayList<>(DataManager.getInstance().getAllBookings());
        bookings.sort(bookingComparator((String) sortSelector.getSelectedItem()));
        for (Booking b : bookings) {
            listModel.addElement(b);
        }
        if (selected != null) {
            bookingJList.setSelectedValue(selected, true);
        }
    }

    private Comparator<Booking> bookingComparator(String sortOption) {
        Comparator<Booking> byId = Comparator.comparing(Booking::getBookingId,
                String.CASE_INSENSITIVE_ORDER);
        if ("Guest (Z-A)".equals(sortOption)) {
            return Comparator.comparing((Booking booking) -> booking.getGuest().getName(),
                    String.CASE_INSENSITIVE_ORDER).reversed().thenComparing(byId);
        }
        if ("Status".equals(sortOption)) {
            return Comparator.comparing((Booking booking) -> booking.getStatus().name())
                    .thenComparing(byId);
        }
        if ("Check-in (latest)".equals(sortOption)) {
            return Comparator.comparing(Booking::getCheckInDate).reversed().thenComparing(byId);
        }
        if ("Room ID (A-Z)".equals(sortOption)) {
            return Comparator.comparing((Booking booking) -> booking.getRoom().getRoomId(),
                    String.CASE_INSENSITIVE_ORDER).thenComparing(byId);
        }
        if ("Total (low-high)".equals(sortOption)) {
            return Comparator.comparingDouble(Booking::calculateTotal).thenComparing(byId);
        }
        if ("Total (high-low)".equals(sortOption)) {
            return Comparator.comparingDouble(Booking::calculateTotal).reversed().thenComparing(byId);
        }
        if ("Check-in (earliest)".equals(sortOption)) {
            return Comparator.comparing(Booking::getCheckInDate).thenComparing(byId);
        }
        return Comparator.comparing((Booking booking) -> booking.getGuest().getName(),
                String.CASE_INSENSITIVE_ORDER).thenComparing(byId);
    }

    private void checkIn() {
        Booking selected = bookingJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a booking first.");
            return;
        }
        if (selected.getStatus() != BookingStatus.RESERVED) {
            JOptionPane.showMessageDialog(this,
                    "Only a Reserved booking can be checked in. Current status: "
                            + selected.getStatus());
            return;
        }

        try {
            bookingController.checkIn(selected);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Check-in Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        refreshList();
        saveChanges(() -> JOptionPane.showMessageDialog(this, "Checked in."));
    }

    private void checkOutAndBill() {
        Booking selected = bookingJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a booking first.");
            return;
        }
        if (selected.getStatus() != BookingStatus.CHECKED_IN) {
            JOptionPane.showMessageDialog(this,
                    "Only a Checked-in booking can be checked out. Current status: "
                            + selected.getStatus());
            return;
        }

        JCheckBox lateCheckoutOption = new JCheckBox(
                "Late checkout until 2:00 PM (50% of one night's room rate)");
        double lateCheckoutFee = bookingController.calculateLateCheckoutFee(selected);
        JLabel lateCheckoutPrice = new JLabel(
                String.format("Additional fee: \u20b1%.2f", lateCheckoutFee));
        JPanel checkoutPrompt = new JPanel(new BorderLayout(0, 8));
        checkoutPrompt.add(new JLabel("Check out " + selected.getGuest().getName() + "?"),
                BorderLayout.NORTH);
        JPanel options = new JPanel(new GridLayout(0, 1, 0, 4));
        options.add(lateCheckoutOption);
        options.add(lateCheckoutPrice);
        checkoutPrompt.add(options, BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(this, checkoutPrompt, "Check Out & Bill",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) {
            return;
        }

        Bill bill;
        try {
            bill = bookingController.checkOutAndBill(
                    selected, lateCheckoutOption.isSelected());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Check-out Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        refreshList();
        saveChanges(() -> JOptionPane.showMessageDialog(this,
                "Checked out. Room " + selected.getRoom().getRoomId()
                        + " is unavailable until inspection and cleaning are complete.\n\n"
                        + bill.toString(),
                "Bill", JOptionPane.INFORMATION_MESSAGE));
    }

    private void cancelReservation() {
        Booking selected = bookingJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a booking first.");
            return;
        }
        if (selected.getStatus() != BookingStatus.RESERVED) {
            JOptionPane.showMessageDialog(this,
                    "Only reserved bookings can be cancelled.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel reservation " + selected.getBookingId() + " for "
                        + selected.getGuest().getName() + "?",
                "Cancel Reservation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DataManager.getInstance().cancelBooking(
                    selected.getBookingId(), currentAccount);
            refreshList();
            saveChanges(() ->
                    JOptionPane.showMessageDialog(this, "Reservation cancelled."));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Booking Not Changed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void rescheduleReservation() {
        Booking selected = bookingJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a booking first.");
            return;
        }
        if (selected.getStatus() != BookingStatus.RESERVED) {
            JOptionPane.showMessageDialog(this,
                    "Only reserved bookings can be rescheduled.");
            return;
        }

        LocalDate[] dates = {
            selected.getCheckInDate(),
            selected.getCheckOutDate()
        };
        JButton checkInButton = new JButton("\uD83D\uDCC5  " + dates[0]);
        JButton checkOutButton = new JButton("\uD83D\uDCC5  " + dates[1]);
        Theme.styleInput(checkInButton);
        Theme.styleInput(checkOutButton);
        checkInButton.setHorizontalAlignment(SwingConstants.LEFT);
        checkOutButton.setHorizontalAlignment(SwingConstants.LEFT);
        checkInButton.addActionListener(event -> {
            LocalDate date = DatePickerDialog.showDialog(this, "Choose check-in date",
                    dates[0], LocalDate.now());
            if (date != null) {
                dates[0] = date;
                if (!dates[1].isAfter(date)) {
                    dates[1] = date.plusDays(1);
                    checkOutButton.setText("\uD83D\uDCC5  " + dates[1]);
                }
                checkInButton.setText("\uD83D\uDCC5  " + dates[0]);
            }
        });
        checkOutButton.addActionListener(event -> {
            LocalDate date = DatePickerDialog.showDialog(this, "Choose check-out date",
                    dates[1], dates[0].plusDays(1));
            if (date != null) {
                dates[1] = date;
                checkOutButton.setText("\uD83D\uDCC5  " + dates[1]);
            }
        });
        Object[] form = {
            "New check-in date:", checkInButton,
            "New check-out date:", checkOutButton
        };
        if (JOptionPane.showConfirmDialog(this, form, "Reschedule Reservation",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            DataManager.getInstance().rescheduleBooking(
                    selected.getBookingId(), dates[0], dates[1]);
            refreshList();
            saveChanges(() ->
                    JOptionPane.showMessageDialog(this, "Reservation rescheduled."));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Booking Not Changed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveChanges(Runnable onSuccess) {
        SaveSupport.save(this, "The booking change could not be saved", onSuccess);
    }
}
