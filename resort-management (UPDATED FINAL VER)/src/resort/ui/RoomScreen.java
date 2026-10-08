package resort.ui;

import resort.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RoomScreen extends JPanel {

    private static final String[] ROOM_TYPES = {
        "Standard", "Deluxe", "Family", "Suite", "Villa", "Pavilion"
    };

    private DefaultListModel<Room> listModel;
    private JList<Room> roomJList;
    private JButton roomActionButton;
    private JComboBox<String> sortSelector;
    private final boolean isAdmin;

    public RoomScreen(Account account) {
        this.isAdmin = account.isAdmin();

        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        roomJList = new JList<>(listModel);
        Theme.styleList(roomJList);
        roomJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Room room = (Room) value;
                setText(String.format("%s | %s | %s | %d guests | \u20b1%.2f / night | %s",
                        room.getRoomId(), typeOf(room), room.getViewType(),
                        room.getCapacity(), room.calculateRate(), room.getStatusLabel()));
                Theme.styleListItemSeparator(this);
                return this;
            }
        });

        BeachPanel background = new BeachPanel(new BorderLayout());

        JLabel title = new JLabel("\uD83C\uDFE8  Rooms", SwingConstants.CENTER);
        Theme.styleTitleLabel(title);
        title.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        background.add(title, BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(10, 10));
        sortSelector = new JComboBox<>(new String[] {
            "Room ID (A-Z)", "Room type (A-Z)", "Status", "Price (low-high)",
            "Price (high-low)", "Capacity (low-high)"
        });
        Theme.styleInput(sortSelector);
        sortSelector.addActionListener(event -> refreshList());
        JPanel sortRow = new JPanel(new BorderLayout(8, 0));
        sortRow.setOpaque(false);
        JLabel sortLabel = new JLabel("Sort rooms by:");
        Theme.styleLabel(sortLabel);
        sortRow.add(sortLabel, BorderLayout.WEST);
        sortRow.add(sortSelector, BorderLayout.CENTER);
        JPanel roomListArea = new JPanel(new BorderLayout(0, 8));
        roomListArea.setOpaque(false);
        roomListArea.add(sortRow, BorderLayout.NORTH);
        roomListArea.add(new JScrollPane(roomJList), BorderLayout.CENTER);
        card.add(roomListArea, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        JButton addBtn = new JButton("Add");
        JButton presetBtn = new JButton("Add from Preset");
        JButton editBtn = new JButton("Edit");
        roomActionButton = new JButton("Remove");
        Theme.styleButton(addBtn);
        Theme.styleSecondaryButton(presetBtn);
        Theme.styleButton(editBtn);
        Theme.styleButton(roomActionButton);

        addBtn.addActionListener(e -> addRoom());
        presetBtn.addActionListener(e -> addFromPreset());
        editBtn.addActionListener(e -> editRoom());
        roomActionButton.addActionListener(e -> removeRoom());

        // Same view-only pattern as Add-ons: Employee sees the buttons but
        // can't use them, rather than the buttons disappearing entirely.
        addBtn.setEnabled(isAdmin);
        presetBtn.setEnabled(isAdmin);
        editBtn.setEnabled(isAdmin);
        roomActionButton.setEnabled(isAdmin);
        roomJList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateRemoveButton();
            }
        });

        buttons.add(addBtn);
        buttons.add(presetBtn);
        buttons.add(editBtn);
        buttons.add(roomActionButton);
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
        Room selected = roomJList.getSelectedValue();
        listModel.clear();
        List<Room> rooms = new ArrayList<>(DataManager.getInstance().getAllRooms());
        rooms.sort(roomComparator((String) sortSelector.getSelectedItem()));
        for (Room r : rooms) {
            listModel.addElement(r);
        }
        if (selected != null) {
            roomJList.setSelectedValue(selected, true);
        }
        if (roomJList != null && roomActionButton != null) {
            updateRemoveButton();
        }
    }

    private Comparator<Room> roomComparator(String sortOption) {
        Comparator<Room> byId = Comparator.comparing(Room::getRoomId,
                String.CASE_INSENSITIVE_ORDER);
        if ("Room type (A-Z)".equals(sortOption)) {
            return Comparator.comparing((Room room) -> typeOf(room),
                    String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Room::getViewType, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byId);
        }
        if ("Status".equals(sortOption)) {
            return Comparator.comparing(Room::getStatusLabel,
                    String.CASE_INSENSITIVE_ORDER).thenComparing(byId);
        }
        if ("Price (low-high)".equals(sortOption)) {
            return Comparator.comparingDouble(Room::calculateRate).thenComparing(byId);
        }
        if ("Price (high-low)".equals(sortOption)) {
            return Comparator.comparingDouble(Room::calculateRate).reversed()
                    .thenComparing(byId);
        }
        if ("Capacity (low-high)".equals(sortOption)) {
            return Comparator.comparingInt(Room::getCapacity).thenComparing(byId);
        }
        return byId;
    }

    private void updateRemoveButton() {
        Room selected = roomJList.getSelectedValue();
        if (selected == null) {
            roomActionButton.setText("Remove");
        } else if (selected.getStatus() == RoomStatus.ARCHIVED) {
            roomActionButton.setText("Restore Room");
        } else if (DataManager.getInstance().hasRoomBookings(selected.getRoomId())) {
            roomActionButton.setText("Archive Room");
        } else {
            roomActionButton.setText("Remove Room");
        }
    }

    private Room buildRoom(String type, String roomId, String viewType) {
        RoomType roomType = RoomType.fromDisplayName(type);
        return new Room(roomId, roomType.getDisplayName(), viewType,
                roomType.getStandardCapacity(), Room.DEFAULT_MAX_EXTRA_GUESTS);
    }

    private String typeOf(Room room) {
        return room.getRoomType();
    }

    private void addRoom() {
        createRoom(null, null);
    }

    private void addFromPreset() {
        RoomPreset preset = (RoomPreset) JOptionPane.showInputDialog(
                this, "Choose a common room setup. You can adjust it before saving:",
                "Add Room from Preset", JOptionPane.QUESTION_MESSAGE, null,
                RoomPreset.values(), RoomPreset.values()[0]);
        if (preset == null) return;
        createRoom(preset.type, preset.view);
    }

    private void createRoom(String initialType, String initialView) {
        String[] views = { "Garden View", "Pool View", "Beachfront View" };
        JComboBox<String> typeBox = new JComboBox<>(ROOM_TYPES);
        JComboBox<String> viewBox = new JComboBox<>(views);
        if (initialType != null) typeBox.setSelectedItem(initialType);
        if (initialView != null) viewBox.setSelectedItem(initialView);
        JSpinner extraGuests = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1));
        JLabel ratePreview = new JLabel();
        JTextArea amenitiesField = new JTextArea(5, 24);
        amenitiesField.setLineWrap(true);
        amenitiesField.setWrapStyleWord(true);
        Theme.styleInput(amenitiesField);
        amenitiesField.setText(formatAmenities(RoomAmenityDefaults.forRoomType(
                (String) typeBox.getSelectedItem())));
        Theme.styleInput(typeBox);
        Theme.styleInput(viewBox);
        updateRatePreview(typeBox, viewBox, ratePreview);
        typeBox.addActionListener(event -> updateRatePreview(typeBox, viewBox, ratePreview));
        typeBox.addActionListener(event -> amenitiesField.setText(formatAmenities(
                RoomAmenityDefaults.forRoomType((String) typeBox.getSelectedItem()))));
        viewBox.addActionListener(event -> updateRatePreview(typeBox, viewBox, ratePreview));

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Room type:"));
        form.add(typeBox);
        form.add(new JLabel("View:"));
        form.add(viewBox);
        form.add(new JLabel("Fixed nightly rate:"));
        form.add(ratePreview);
        form.add(new JLabel("Extra guest allowance:"));
        form.add(extraGuests);
        form.add(new JLabel("Complimentary amenities (one per line):"));
        form.add(new JScrollPane(amenitiesField));
        if (JOptionPane.showConfirmDialog(this, form, "Add Room",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        String roomId = DataManager.getInstance().generateRoomId();
        Room room = buildRoom((String) typeBox.getSelectedItem(), roomId,
                (String) viewBox.getSelectedItem());
        room.setMaxExtraGuests((Integer) extraGuests.getValue());
        room.setIncludedAmenities(parseAmenities(amenitiesField.getText()));
        DataManager.getInstance().addRoom(room);
        refreshList();
        saveChanges();
    }

    private void editRoom() {
        Room selected = roomJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a room first.");
            return;
        }

        String[] views = { "Garden View", "Pool View", "Beachfront View" };
        String type = typeOf(selected);
        JComboBox<String> viewBox = new JComboBox<>(views);
        viewBox.setSelectedItem(selected.getViewType());
        JSpinner extraGuests = new JSpinner(new SpinnerNumberModel(
                selected.getMaxExtraGuests(), 0, 100, 1));
        JLabel ratePreview = new JLabel();
        JTextArea amenitiesField = new JTextArea(5, 24);
        amenitiesField.setLineWrap(true);
        amenitiesField.setWrapStyleWord(true);
        amenitiesField.setText(formatAmenities(selected.getIncludedAmenities()));
        Theme.styleInput(amenitiesField);
        Theme.styleInput(viewBox);
        updateRatePreview(type, viewBox, ratePreview);
        viewBox.addActionListener(event -> updateRatePreview(type, viewBox, ratePreview));

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Room type:"));
        form.add(new JLabel(type));
        form.add(new JLabel("View:"));
        form.add(viewBox);
        form.add(new JLabel("Fixed nightly rate:"));
        form.add(ratePreview);
        form.add(new JLabel("Extra guest allowance:"));
        form.add(extraGuests);
        form.add(new JLabel("Complimentary amenities (one per line):"));
        form.add(new JScrollPane(amenitiesField));
        if (JOptionPane.showConfirmDialog(this, form, "Edit Room",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        Room updated = buildRoom(type, selected.getRoomId(),
                (String) viewBox.getSelectedItem());
        updated.setStatus(selected.getStatus()); // Preserve the room's readiness across edits.
        updated.setMaxExtraGuests((Integer) extraGuests.getValue());
        updated.setIncludedAmenities(parseAmenities(amenitiesField.getText()));
        DataManager.getInstance().editRoom(selected.getRoomId(), updated);
        refreshList();
        saveChanges();
    }

    private void updateRatePreview(String type, JComboBox<String> viewBox, JLabel preview) {
        String view = (String) viewBox.getSelectedItem();
        if (type != null && view != null) {
            preview.setText(String.format("\u20b1%.2f / night",
                    RoomRates.getNightlyRate(type, view)));
        }
    }

    private void updateRatePreview(JComboBox<String> typeBox,
                                   JComboBox<String> viewBox, JLabel preview) {
        String type = (String) typeBox.getSelectedItem();
        if (type != null) {
            updateRatePreview(type, viewBox, preview);
        }
    }

    private String formatAmenities(List<String> amenities) {
        return String.join("\n", amenities);
    }

    private List<String> parseAmenities(String text) {
        return Arrays.asList(text.split("\\R"));
    }

    private void removeRoom() {
        Room selected = roomJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a room first.");
            return;
        }

        DataManager dataManager = DataManager.getInstance();
        if (selected.getStatus() == RoomStatus.ARCHIVED) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Restore room " + selected.getRoomId()
                            + " to active inventory? Its previous readiness status will be restored.",
                    "Restore Room", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                dataManager.restoreRoom(selected.getRoomId());
                refreshList();
                updateRemoveButton();
                saveChanges();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showRoomActionError(ex);
            }
            return;
        }

        if (dataManager.hasRoomBookings(selected.getRoomId())) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Room " + selected.getRoomId()
                            + " has booking history. Archive it from active inventory? "
                            + "Past booking records will be kept.",
                    "Archive Room", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                dataManager.archiveRoom(selected.getRoomId());
                refreshList();
                updateRemoveButton();
                saveChanges();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showRoomActionError(ex);
            }
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently remove room " + selected.getRoomId() + "?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dataManager.removeRoom(selected.getRoomId());
            refreshList();
            updateRemoveButton();
            saveChanges();
        }
    }

    private void showRoomActionError(RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Room Not Changed", JOptionPane.WARNING_MESSAGE);
    }

    private void saveChanges() {
        SaveSupport.save(this, "The room change could not be saved", null);
    }

    private enum RoomPreset {
        STANDARD_GARDEN("Standard - Garden View", "Standard", "Garden View"),
        STANDARD_POOL("Standard - Pool View", "Standard", "Pool View"),
        DELUXE_POOL("Deluxe - Pool View", "Deluxe", "Pool View"),
        FAMILY_GARDEN("Family - Garden View", "Family", "Garden View"),
        SUITE_BEACHFRONT("Suite - Beachfront View", "Suite", "Beachfront View"),
        VILLA_BEACHFRONT("Villa - Beachfront View", "Villa", "Beachfront View"),
        PAVILION_BEACHFRONT("Pavilion - Beachfront View",
                "Pavilion", "Beachfront View");

        private final String label;
        private final String type;
        private final String view;

        RoomPreset(String label, String type, String view) {
            this.label = label;
            this.type = type;
            this.view = view;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
