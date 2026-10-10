package resort.ui;

import resort.model.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BookingScreen extends JPanel {

    private final DataManager dataManager;

    private JTextField guestNameField;
    private JTextField guestContactField;
    private JComboBox<Room> roomComboBox;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private JButton checkInButton;
    private JButton checkOutButton;
    private JSpinner guestCountSpinner;
    private JLabel guestCountLabel;
    private JLabel includedAmenitiesLabel;
    private JList<AddOn> addOnList;
    private DefaultListModel<AddOn> addOnListModel;
    private JComboBox<AddOn> addOnPicker;
    private JComboBox<Object> addOnCategoryFilter;
    private JLabel addOnSelectionSummary;
    private final Set<String> selectedAddOnIds = new HashSet<>();
    private final Map<String, Integer> selectedAddOnQuantities = new HashMap<>();
    private JLabel extraGuestHint;
    private JLabel statusLabel;
    private final Runnable onBookingComplete;

    public BookingScreen() {
        this(DataManager.getInstance(), null, null);
    }

    public BookingScreen(DataManager dataManager) {
        this(dataManager, null, null);
    }

    public BookingScreen(Runnable showBookingManagement, Runnable onBookingComplete) {
        this(DataManager.getInstance(), showBookingManagement, onBookingComplete);
    }

    private BookingScreen(DataManager dataManager, Runnable showBookingManagement,
                          Runnable onBookingComplete) {
        this.dataManager = dataManager;
        this.onBookingComplete = onBookingComplete;

        setLayout(new BorderLayout());

        BeachPanel background = new BeachPanel(new BorderLayout());

        JLabel screenTitle = new JLabel("\uD83D\uDCC5  New Booking", SwingConstants.CENTER);
        Theme.styleTitleLabel(screenTitle);
        screenTitle.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        background.add(screenTitle, BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        guestNameField = new JTextField();
        guestContactField = new JTextField();
        Theme.styleInput(guestNameField);
        Theme.styleInput(guestContactField);
        guestContactField.setToolTipText(
                "Enter 11 digits; the phone number is formatted automatically.");
        ((javax.swing.text.AbstractDocument) guestContactField.getDocument())
                .setDocumentFilter(new ContactNumberFilter(guestContactField));
        row = addFieldRow(card, gbc, row, "Guest name:", guestNameField);
        row = addFieldRow(card, gbc, row, "Guest contact:", guestContactField);

        roomComboBox = new JComboBox<>();
        Theme.styleInput(roomComboBox);
        roomComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room) {
                    Room room = (Room) value;
                    item.setText(String.format("%s | %s | %s | \u20b1%.2f/night | %s",
                            room.getRoomId(), room.getClass().getSimpleName()
                                    .replace("Room", ""),
                            room.getViewType(), room.calculateRate(),
                            room.getStatusLabel()));
                }
                return item;
            }
        });
        populateRooms();
        row = addFieldRow(card, gbc, row, "Room:", roomComboBox);
        includedAmenitiesLabel = new JLabel();
        Theme.styleLabel(includedAmenitiesLabel);
        includedAmenitiesLabel.setFont(Theme.LABEL_FONT.deriveFont(13f));
        roomComboBox.addActionListener(event -> handleRoomSelectionChanged());
        row = addFieldRow(card, gbc, row, "Included with room:", includedAmenitiesLabel);
        updateIncludedAmenities();

        checkInDate = LocalDate.now();
        checkOutDate = checkInDate.plusDays(1);
        checkInButton = createDateButton(checkInDate);
        checkOutButton = createDateButton(checkOutDate);
        checkInButton.addActionListener(event -> chooseCheckInDate());
        checkOutButton.addActionListener(event -> chooseCheckOutDate());
        row = addFieldRow(card, gbc, row, "Check-in date:", checkInButton);
        row = addFieldRow(card, gbc, row, "Check-out date:", checkOutButton);

        guestCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        Theme.styleInput(guestCountSpinner);
        JPanel guestCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        guestCountPanel.setOpaque(false);
        guestCountPanel.add(guestCountSpinner);
        extraGuestHint = new JLabel(
                String.format("\u20b1%.2f per extra guest / night",
                        dataManager.getExtraPersonFeePerNight()));
        Theme.styleLabel(extraGuestHint);
        extraGuestHint.setFont(Theme.LABEL_FONT.deriveFont(14f));
        guestCountPanel.add(extraGuestHint);
        guestCountLabel = new JLabel();
        Theme.styleLabel(guestCountLabel);
        row = addFieldRow(card, gbc, row, guestCountLabel, guestCountPanel);
        updateGuestCountLimit();

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        JLabel addOnLabel = new JLabel(
                "Add-ons (per guest or per booking):", SwingConstants.CENTER);
        Theme.styleLabel(addOnLabel);
        card.add(addOnLabel, gbc);
        row++;

        addOnCategoryFilter = new JComboBox<>();
        addOnCategoryFilter.addItem("All categories");
        for (AddOnCategory category : AddOnCategory.values()) {
            addOnCategoryFilter.addItem(category);
        }
        Theme.styleInput(addOnCategoryFilter);
        addOnCategoryFilter.addActionListener(event -> refreshAddOns());
        row = addFieldRow(card, gbc, row, "Filter add-ons:", addOnCategoryFilter);

        addOnPicker = new JComboBox<>();
        Theme.styleInput(addOnPicker);
        addOnPicker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof AddOn) {
                    AddOn addOn = (AddOn) value;
                    item.setText(String.format("%s - \u20b1%.2f %s",
                            addOn.getName(), addOn.getPrice(),
                            addOn.getChargeBasis().getDisplayName()));
                }
                return item;
            }
        });
        JButton addSelectedAddOnButton = new JButton("Add / Set Qty");
        JButton removeSelectedAddOnButton = new JButton("Remove");
        Theme.styleButton(addSelectedAddOnButton);
        Theme.styleSecondaryButton(removeSelectedAddOnButton);
        addSelectedAddOnButton.addActionListener(event -> addSelectedAddOn());
        removeSelectedAddOnButton.addActionListener(event -> removeSelectedAddOn());
        JPanel addOnPickerRow = new JPanel(new BorderLayout(8, 0));
        addOnPickerRow.setOpaque(false);
        addOnPickerRow.add(addOnPicker, BorderLayout.CENTER);
        JPanel addOnActions = new JPanel(new GridLayout(1, 2, 6, 0));
        addOnActions.setOpaque(false);
        addOnActions.add(addSelectedAddOnButton);
        addOnActions.add(removeSelectedAddOnButton);
        addOnPickerRow.add(addOnActions, BorderLayout.EAST);
        row = addFieldRow(card, gbc, row, "Choose add-on:", addOnPickerRow);

        addOnListModel = new DefaultListModel<>();
        addOnList = new JList<>(addOnListModel);
        addOnList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        Theme.styleList(addOnList);
        addOnList.setFixedCellHeight(26);
        addOnList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof AddOn) {
                    AddOn addOn = (AddOn) value;
                    int quantity = selectedAddOnQuantities.getOrDefault(
                            addOn.getAddOnId(), 1);
                    item.setText(String.format("%s x%d (%s) - \u20b1%.2f",
                            addOn.getName(), quantity,
                            addOn.getChargeBasis().getDisplayName(),
                            addOn.getPrice() * quantity));
                }
                Theme.styleListItemSeparator(item);
                return item;
            }
        });
        JScrollPane addOnScroll = new JScrollPane(addOnList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        addOnScroll.setPreferredSize(new Dimension(360, 58));
        addOnScroll.setMinimumSize(new Dimension(100, 34));

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(addOnScroll, gbc);
        row++;

        addOnSelectionSummary = new JLabel("No add-ons selected.", SwingConstants.CENTER);
        Theme.styleLabel(addOnSelectionSummary);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(addOnSelectionSummary, gbc);
        row++;
        refreshAddOns();

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(Theme.LABEL_FONT);
        statusLabel.setForeground(Theme.PALM);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(statusLabel, gbc);
        row++;

        JButton checkAvailabilityBtn = new JButton("Check Availability");
        JButton createBookingBtn = new JButton("Create Booking");
        JButton manageBookingsBtn = new JButton("Manage Existing Bookings");
        Theme.styleSecondaryButton(checkAvailabilityBtn);
        Theme.styleButton(createBookingBtn);
        Theme.styleSecondaryButton(manageBookingsBtn);

        checkAvailabilityBtn.addActionListener(e -> checkAvailability());
        createBookingBtn.addActionListener(e -> createBooking());
        manageBookingsBtn.setEnabled(showBookingManagement != null);
        manageBookingsBtn.addActionListener(e -> {
            if (showBookingManagement != null) showBookingManagement.run();
        });

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.add(checkAvailabilityBtn);
        buttonPanel.add(createBookingBtn);
        buttonPanel.add(manageBookingsBtn);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(buttonPanel, gbc);

        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setOpaque(false);
        cardWrap.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        cardWrap.add(card, BorderLayout.CENTER);
        JScrollPane formScroll = new JScrollPane(cardWrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(18);
        background.add(formScroll, BorderLayout.CENTER);

        add(background, BorderLayout.CENTER);
    }

    private int addFieldRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        JLabel fieldLabel = new JLabel(label);
        Theme.styleLabel(fieldLabel);
        return addFieldRow(panel, gbc, row, fieldLabel, field);
    }

    private int addFieldRow(JPanel panel, GridBagConstraints gbc, int row,
                            JLabel fieldLabel, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(fieldLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
        return row + 1;
    }

    private void updateGuestCountLimit() {
        Room room = (Room) roomComboBox.getSelectedItem();
        SpinnerNumberModel model = (SpinnerNumberModel) guestCountSpinner.getModel();
        int normalCapacity = room == null ? 1 : room.getCapacity();
        int maximumCapacity = room == null ? 1 : room.getMaximumGuestCount();
        model.setMaximum(maximumCapacity);
        if ((Integer) model.getValue() > maximumCapacity) {
            model.setValue(maximumCapacity);
        }
        guestCountLabel.setText(String.format(
                "Guest count (normal: %d, max: %d):", normalCapacity, maximumCapacity));
    }

    private void handleRoomSelectionChanged() {
        updateIncludedAmenities();
        updateGuestCountLimit();
        if (statusLabel != null) {
            statusLabel.setText(" ");
        }
    }

    private void updateIncludedAmenities() {
        if (includedAmenitiesLabel == null) {
            return;
        }
        Room room = (Room) roomComboBox.getSelectedItem();
        if (room == null || room.getIncludedAmenities().isEmpty()) {
            includedAmenitiesLabel.setText("None");
            return;
        }
        String amenities = String.join(", ", room.getIncludedAmenities())
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        includedAmenitiesLabel.setText("<html>"
                + amenities
                + " <i>(included at no extra charge)</i></html>");
    }

    private JButton createDateButton(LocalDate date) {
        JButton button = new JButton();
        Theme.styleInput(button);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setText("\uD83D\uDCC5  " + date);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void chooseCheckInDate() {
        LocalDate selected = DatePickerDialog.showDialog(this, "Choose check-in date",
                checkInDate, LocalDate.now());
        if (selected == null) {
            return;
        }
        checkInDate = selected;
        if (!checkOutDate.isAfter(checkInDate)) {
            checkOutDate = checkInDate.plusDays(1);
            checkOutButton.setText("\uD83D\uDCC5  " + checkOutDate);
        }
        checkInButton.setText("\uD83D\uDCC5  " + checkInDate);
        statusLabel.setText(" ");
    }

    private void chooseCheckOutDate() {
        LocalDate selected = DatePickerDialog.showDialog(this, "Choose check-out date",
                checkOutDate, checkInDate.plusDays(1));
        if (selected != null) {
            checkOutDate = selected;
            checkOutButton.setText("\uD83D\uDCC5  " + checkOutDate);
            statusLabel.setText(" ");
        }
    }

    private void populateRooms() {
        Room previousSelection = (Room) roomComboBox.getSelectedItem();
        roomComboBox.removeAllItems();
        Room matchingRoom = null;
        for (Room room : dataManager.getAllRooms()) {
            if (room.getStatus() != RoomStatus.ARCHIVED) {
                roomComboBox.addItem(room);
                if (previousSelection != null
                        && room.getRoomId().equals(previousSelection.getRoomId())) {
                    matchingRoom = room;
                }
            }
        }
        if (matchingRoom != null) {
            roomComboBox.setSelectedItem(matchingRoom);
        }
    }

    public void refreshCatalog() {
        populateRooms();
        refreshAddOns();
        extraGuestHint.setText(String.format("\u20b1%.2f per extra guest / night",
                dataManager.getExtraPersonFeePerNight()));
        updateGuestCountLimit();
    }

    private void refreshAddOns() {
        addOnPicker.removeAllItems();
        Object selectedCategory = addOnCategoryFilter.getSelectedItem();
        List<AddOn> catalog = dataManager.getAllAddOns();
        for (AddOn addOn : catalog) {
            if (!(selectedCategory instanceof AddOnCategory)
                    || addOn.getCategory() == selectedCategory) {
                addOnPicker.addItem(addOn);
            }
        }
        refreshSelectedAddOns(catalog);
    }

    private void addSelectedAddOn() {
        AddOn selected = (AddOn) addOnPicker.getSelectedItem();
        if (selected == null) {
            return;
        }
        int quantity = 1;
        if (selected.getChargeBasis() == AddOnChargeBasis.PER_PERSON) {
            int guestCount = (int) guestCountSpinner.getValue();
            int currentQuantity = selectedAddOnQuantities.getOrDefault(
                    selected.getAddOnId(), guestCount);
            JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(
                    Math.min(currentQuantity, guestCount), 1, guestCount, 1));
            JPanel prompt = new JPanel(new BorderLayout(8, 0));
            prompt.add(new JLabel("Guests for " + selected.getName() + ":"),
                    BorderLayout.CENTER);
            prompt.add(quantitySpinner, BorderLayout.EAST);
            if (JOptionPane.showConfirmDialog(this, prompt, "Add-on quantity",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                return;
            }
            quantity = (int) quantitySpinner.getValue();
        }
        selectedAddOnIds.add(selected.getAddOnId());
        selectedAddOnQuantities.put(selected.getAddOnId(), quantity);
        refreshSelectedAddOns(dataManager.getAllAddOns());
    }

    private void removeSelectedAddOn() {
        AddOn selected = addOnList.getSelectedValue();
        if (selected != null) {
            selectedAddOnIds.remove(selected.getAddOnId());
            selectedAddOnQuantities.remove(selected.getAddOnId());
            refreshSelectedAddOns(dataManager.getAllAddOns());
        }
    }

    private void refreshSelectedAddOns(List<AddOn> catalog) {
        addOnListModel.clear();
        double total = 0;
        int count = 0;
        for (AddOn addOn : catalog) {
            if (selectedAddOnIds.contains(addOn.getAddOnId())) {
                addOnListModel.addElement(addOn);
                total += addOn.getPrice()
                        * selectedAddOnQuantities.getOrDefault(addOn.getAddOnId(), 1);
                count++;
            }
        }
        addOnSelectionSummary.setText(count == 0
                ? "No add-ons selected."
                : String.format("%d add-on%s selected — total: \u20b1%.2f",
                        count, count == 1 ? "" : "s", total));
    }

    private void checkAvailability() {
        Room room = (Room) roomComboBox.getSelectedItem();
        if (room == null) {
            statusLabel.setText("No rooms available to select. Add rooms first.");
            return;
        }

        boolean available = room.isAvailable(checkInDate, checkOutDate);
        statusLabel.setText(available ? "Room is available." : "Room is NOT available.");
    }

    private void createBooking() {
        Room room = (Room) roomComboBox.getSelectedItem();
        if (room == null) {
            JOptionPane.showMessageDialog(this, "No room selected.");
            return;
        }

        String guestName = guestNameField.getText().trim();
        if (guestName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Guest name is required.");
            return;
        }

        int guestCount = (int) guestCountSpinner.getValue();

        if (guestCount > room.getMaximumGuestCount()) {
            JOptionPane.showMessageDialog(this,
                    "Guest count (" + guestCount + ") exceeds this room's maximum capacity ("
                            + room.getMaximumGuestCount()
                            + "). Choose a larger room type.",
                    "Booking rejected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!room.isAvailable(checkInDate, checkOutDate,
                dataManager.getAllBookings())) {
            JOptionPane.showMessageDialog(this,
                    "This room is not available for the selected dates.",
                    "Booking rejected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Guest guest = new Guest(dataManager.generateGuestId(), guestName,
                guestContactField.getText().trim());
        Booking booking = new Booking(dataManager.generateBookingId(),
                guest, room, checkInDate, checkOutDate, guestCount,
                dataManager.getExtraPersonFeePerNight());
        for (AddOn addOn : dataManager.getAllAddOns()) {
            if (selectedAddOnIds.contains(addOn.getAddOnId())) {
                booking.addAddOn(addOn,
                        selectedAddOnQuantities.getOrDefault(addOn.getAddOnId(), 1));
            }
        }
        try {
            dataManager.createAndSaveBookingAsync(booking, () -> {
                JOptionPane.showMessageDialog(this,
                        "Booking created.\nBooking ID: " + booking.getBookingId()
                                + "\nGuest count: " + guestCount
                                + (guestCount > room.getCapacity()
                                        ? "\nExtra guest charge: \u20b1"
                                                + String.format("%.2f",
                                                        booking.getExtraPersonCharge())
                                        : "")
                                + "\nTotal: \u20b1"
                                        + String.format("%.2f", booking.calculateTotal()));
                if (onBookingComplete != null) onBookingComplete.run();
            }, error -> JOptionPane.showMessageDialog(this,
                    "Could not save the booking:\n" + error.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Booking rejected", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private static final class ContactNumberFilter extends DocumentFilter {
        private static final int MAX_DIGITS = 11;

        private final JTextField field;

        private ContactNumberFilter(JTextField field) {
            this.field = field;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String text,
                AttributeSet attributes) throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length)
                throws BadLocationException {
            replace(bypass, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text,
                AttributeSet attributes) throws BadLocationException {
            String current = bypass.getDocument().getText(
                    0, bypass.getDocument().getLength());
            String inserted = text == null ? "" : text;
            String updated = current.substring(0, offset) + inserted
                    + current.substring(offset + length);
            int rawCaret = Math.min(offset + inserted.length(), updated.length());

            StringBuilder digits = new StringBuilder(MAX_DIGITS);
            int caretDigitCount = 0;
            for (int i = 0; i < updated.length(); i++) {
                char character = updated.charAt(i);
                if (character >= '0' && character <= '9') {
                    if (digits.length() < MAX_DIGITS) {
                        digits.append(character);
                    }
                    if (i < rawCaret && caretDigitCount < MAX_DIGITS) {
                        caretDigitCount++;
                    }
                }
            }

            StringBuilder formatted = new StringBuilder(MAX_DIGITS + 2);
            for (int i = 0; i < digits.length(); i++) {
                if (i == 4 || i == 7) {
                    formatted.append('-');
                }
                formatted.append(digits.charAt(i));
            }

            bypass.replace(0, bypass.getDocument().getLength(),
                    formatted.toString(), attributes);
            int caret = caretDigitCount;
            if (caretDigitCount > 4) {
                caret++;
            }
            if (caretDigitCount > 7) {
                caret++;
            }
            int newCaret = Math.min(caret, formatted.length());
            SwingUtilities.invokeLater(() -> field.setCaretPosition(newCaret));
        }
    }
}
