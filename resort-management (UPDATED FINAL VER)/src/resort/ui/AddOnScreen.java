package resort.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import resort.model.*;

public class AddOnScreen extends JPanel {

    private DefaultListModel<AddOn> listModel;
    private JList<AddOn> addOnJList;
    private JComboBox<Object> categoryFilter;
    private JComboBox<String> sortSelector;
    private JTextArea detailsArea;
    private JLabel extraGuestRateLabel;
    private final CardLayout contentCards = new CardLayout();
    private final JPanel contentWorkspace = new JPanel(contentCards);
    private JScrollPane editorScrollPane;
    private JTextField editorNameField;
    private JTextField editorPriceField;
    private JTextArea editorDetailsArea;
    private JComboBox<AddOnCategory> editorCategoryBox;
    private JComboBox<AddOnChargeBasis> editorChargeBasisBox;
    private JComboBox<AddOnPreset> presetPicker;
    private JLabel editorTitle;
    private JLabel validationMessage;
    private JPanel presetRow;
    private String editingAddOnId;
    private final boolean isAdmin;

    private static final String EDITOR_VIEW = "editor";
    private static final String CATALOG_VIEW = "catalog";

    public AddOnScreen(Account account) {
        this.isAdmin = account.isAdmin();

        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        addOnJList = new JList<>(listModel);
        Theme.styleList(addOnJList);
        addOnJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                Theme.styleListItemSeparator(item);
                return item;
            }
        });
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        Theme.styleInput(detailsArea);
        editorScrollPane = new JScrollPane(createEditorPanel());
        editorScrollPane.setBorder(null);
        editorScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addOnJList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedDetails();
            }
        });
        BeachPanel background = new BeachPanel(new BorderLayout());

        JLabel title = new JLabel("\uD83C\uDF79  Add-ons", SwingConstants.CENTER);
        Theme.styleTitleLabel(title);
        title.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        background.add(title, BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(10, 10));
        JPanel catalogHeader = new JPanel(new BorderLayout(10, 0));
        catalogHeader.setOpaque(false);
        JLabel browseLabel = new JLabel("Browse by category:");
        Theme.styleLabel(browseLabel);
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("All categories");
        for (AddOnCategory category : AddOnCategory.values()) {
            categoryFilter.addItem(category);
        }
        Theme.styleInput(categoryFilter);
        categoryFilter.addActionListener(event -> refreshList());
        catalogHeader.add(browseLabel, BorderLayout.WEST);
        catalogHeader.add(categoryFilter, BorderLayout.CENTER);
        JPanel sortHeader = new JPanel(new BorderLayout(10, 0));
        sortHeader.setOpaque(false);
        JLabel sortLabel = new JLabel("Sort by:");
        Theme.styleLabel(sortLabel);
        sortSelector = new JComboBox<>(new String[] {
            "Name (A-Z)", "Name (Z-A)", "Category", "Price (low-high)",
            "Price (high-low)"
        });
        Theme.styleInput(sortSelector);
        sortSelector.addActionListener(event -> refreshList());
        sortHeader.add(sortLabel, BorderLayout.WEST);
        sortHeader.add(sortSelector, BorderLayout.CENTER);
        JPanel catalogControls = new JPanel(new GridLayout(2, 1, 0, 6));
        catalogControls.setOpaque(false);
        catalogControls.add(catalogHeader);
        catalogControls.add(sortHeader);
        card.add(catalogControls, BorderLayout.NORTH);

        JPanel pricingRow = new JPanel(new BorderLayout(12, 0));
        pricingRow.setOpaque(false);
        extraGuestRateLabel = new JLabel();
        Theme.styleLabel(extraGuestRateLabel);
        JButton extraGuestRateBtn = new JButton("Change");
        Theme.styleSecondaryButton(extraGuestRateBtn);
        extraGuestRateBtn.addActionListener(e -> editExtraGuestRate());
        JPanel feeSettings = new JPanel(new BorderLayout(8, 0));
        feeSettings.setOpaque(false);
        feeSettings.add(extraGuestRateLabel);
        if (isAdmin) {
            feeSettings.add(extraGuestRateBtn, BorderLayout.EAST);
        }
        pricingRow.add(feeSettings, BorderLayout.CENTER);
        updateExtraGuestRateLabel();
        JSplitPane addOnWorkspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(addOnJList), new JScrollPane(detailsArea));
        addOnWorkspace.setResizeWeight(0.62);
        card.add(addOnWorkspace, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(pricingRow, BorderLayout.NORTH);
        if (isAdmin) {
            JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
            buttons.setOpaque(false);
            JButton addBtn = new JButton("Add");
            JButton presetBtn = new JButton("Add from Preset");
            JButton editBtn = new JButton("Edit");
            JButton removeBtn = new JButton("Remove");
            Theme.styleButton(addBtn);
            Theme.styleSecondaryButton(presetBtn);
            Theme.styleButton(editBtn);
            Theme.styleButton(removeBtn);

            addBtn.addActionListener(e -> addAddOn());
            presetBtn.addActionListener(e -> addFromPreset());
            editBtn.addActionListener(e -> editAddOn());
            removeBtn.addActionListener(e -> removeAddOn());

            buttons.add(addBtn);
            buttons.add(presetBtn);
            buttons.add(editBtn);
            buttons.add(removeBtn);
            footer.add(buttons, BorderLayout.SOUTH);
        }
        card.add(footer, BorderLayout.SOUTH);

        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setOpaque(false);
        cardWrap.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        cardWrap.add(card, BorderLayout.CENTER);
        contentWorkspace.setOpaque(false);
        contentWorkspace.add(cardWrap, CATALOG_VIEW);

        JPanel editorWrap = new JPanel(new BorderLayout());
        editorWrap.setOpaque(false);
        editorWrap.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        editorWrap.add(editorScrollPane, BorderLayout.CENTER);
        contentWorkspace.add(editorWrap, EDITOR_VIEW);
        background.add(contentWorkspace, BorderLayout.CENTER);

        add(background);
        refreshList();
    }

    public void refreshList() {
        AddOn selected = addOnJList.getSelectedValue();
        listModel.clear();
        List<AddOn> addOns = new ArrayList<>(DataManager.getInstance().getAllAddOns());
        Object selectedCategory = categoryFilter.getSelectedItem();
        addOns.removeIf(addOn -> selectedCategory instanceof AddOnCategory
                && addOn.getCategory() != selectedCategory);
        addOns.sort(addOnComparator((String) sortSelector.getSelectedItem()));
        for (AddOn addOn : addOns) {
            listModel.addElement(addOn);
        }
        if (!listModel.isEmpty()) {
            addOnJList.setSelectedValue(selected, true);
            if (addOnJList.getSelectedIndex() < 0) {
                addOnJList.setSelectedIndex(0);
            }
        }
        showSelectedDetails();
    }

    private Comparator<AddOn> addOnComparator(String sortOption) {
        Comparator<AddOn> byName = Comparator.comparing(AddOn::getName,
                String.CASE_INSENSITIVE_ORDER);
        if ("Name (Z-A)".equals(sortOption)) {
            return byName.reversed().thenComparing(AddOn::getAddOnId);
        }
        if ("Category".equals(sortOption)) {
            return Comparator.comparing((AddOn addOn) -> String.valueOf(addOn.getCategory()),
                    String.CASE_INSENSITIVE_ORDER).thenComparing(byName);
        }
        if ("Price (low-high)".equals(sortOption)) {
            return Comparator.comparingDouble(AddOn::getPrice).thenComparing(byName);
        }
        if ("Price (high-low)".equals(sortOption)) {
            return Comparator.comparingDouble(AddOn::getPrice).reversed().thenComparing(byName);
        }
        return byName.thenComparing(AddOn::getAddOnId);
    }

    private JPanel createEditorPanel() {
        editorNameField = new JTextField();
        editorPriceField = new JTextField();
        editorDetailsArea = new JTextArea(5, 18);
        editorDetailsArea.setLineWrap(true);
        editorDetailsArea.setWrapStyleWord(true);
        editorCategoryBox = new JComboBox<>(AddOnCategory.values());
        editorChargeBasisBox = new JComboBox<>(AddOnChargeBasis.values());
        presetPicker = new JComboBox<>();
        editorTitle = new JLabel("Add add-on");
        editorTitle.setFont(Theme.HEADER_FONT);
        editorTitle.setForeground(Theme.OCEAN_DEEP);
        validationMessage = new JLabel(" ");
        validationMessage.setForeground(Theme.ERROR);
        Theme.styleInput(editorNameField);
        Theme.styleInput(editorPriceField);
        Theme.styleInput(editorDetailsArea);
        Theme.styleInput(editorCategoryBox);
        Theme.styleInput(editorChargeBasisBox);
        Theme.styleInput(presetPicker);

        JPanel editor = new JPanel(new BorderLayout(0, 10));
        editor.setBackground(Theme.INPUT_CREAM);
        editor.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        editor.add(editorTitle, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 3, 4, 3);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        fields.add(new JLabel("Name:"), constraints);
        constraints.gridy++;
        fields.add(editorNameField, constraints);
        constraints.gridy++;
        fields.add(new JLabel("Category:"), constraints);
        constraints.gridy++;
        fields.add(editorCategoryBox, constraints);
        constraints.gridy++;
        fields.add(new JLabel("Unit price (PHP):"), constraints);
        constraints.gridy++;
        fields.add(editorPriceField, constraints);
        constraints.gridy++;
        fields.add(new JLabel("Charge basis:"), constraints);
        constraints.gridy++;
        fields.add(editorChargeBasisBox, constraints);
        constraints.gridy++;
        fields.add(new JLabel("Details:"), constraints);
        constraints.gridy++;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        fields.add(new JScrollPane(editorDetailsArea), constraints);
        editor.add(fields, BorderLayout.CENTER);

        presetRow = new JPanel(new BorderLayout(6, 0));
        presetRow.setOpaque(false);
        JButton applyPresetButton = new JButton("Apply");
        Theme.styleSecondaryButton(applyPresetButton);
        applyPresetButton.addActionListener(event -> applySelectedPreset());
        presetRow.add(presetPicker, BorderLayout.CENTER);
        presetRow.add(applyPresetButton, BorderLayout.EAST);
        presetRow.setVisible(false);

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(presetRow, BorderLayout.NORTH);
        footer.add(validationMessage, BorderLayout.CENTER);
        JPanel editorButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        editorButtons.setOpaque(false);
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        Theme.styleButton(saveButton);
        Theme.styleSecondaryButton(cancelButton);
        saveButton.addActionListener(event -> saveEditor());
        cancelButton.addActionListener(event -> closeEditor());
        editorButtons.add(cancelButton);
        editorButtons.add(saveButton);
        footer.add(editorButtons, BorderLayout.SOUTH);
        editor.add(footer, BorderLayout.SOUTH);
        return editor;
    }

    private void showSelectedDetails() {
        AddOn selected = addOnJList.getSelectedValue();
        if (selected == null) {
            detailsArea.setText(listModel.isEmpty()
                    ? "No add-ons are currently listed in this category."
                    : "Select an add-on to view its details.");
            return;
        }
        detailsArea.setText("Name: " + selected.getName()
                + "\nCategory: " + selected.getCategory()
                + String.format("\nPrice %s: \u20b1%.2f",
                        selected.getChargeBasis().getDisplayName(), selected.getPrice())
                + "\n\nDetails:\n"
                + (selected.getDetails().isEmpty() ? "No details provided."
                        : selected.getDetails()));
        detailsArea.setCaretPosition(0);
    }

    private void addAddOn() {
        openEditor(null);
    }

    private void addFromPreset() {
        openEditor(null);
        presetRow.setVisible(true);
        refreshAvailablePresets();
        editorScrollPane.revalidate();
    }

    private void refreshAvailablePresets() {
        presetPicker.removeAllItems();
        for (AddOnPreset preset : AddOnPreset.values()) {
            boolean alreadyAdded = false;
            for (AddOn addOn : DataManager.getInstance().getAllAddOns()) {
                if (addOn.getName() != null
                        && addOn.getName().equalsIgnoreCase(preset.getName())) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                presetPicker.addItem(preset);
            }
        }
        if (presetPicker.getItemCount() == 0) {
            validationMessage.setText("All add-on presets are already in the catalog.");
        }
    }

    private void applySelectedPreset() {
        AddOnPreset preset = (AddOnPreset) presetPicker.getSelectedItem();
        if (preset == null) {
            validationMessage.setText("No presets are available.");
            return;
        }
        editorNameField.setText(preset.getName());
        editorDetailsArea.setText(preset.getDetails());
        editorPriceField.setText(String.format(java.util.Locale.ROOT, "%.2f",
                preset.getPrice()));
        editorCategoryBox.setSelectedItem(preset.getCategory());
        editorChargeBasisBox.setSelectedItem(preset.getChargeBasis());
        presetRow.setVisible(false);
        validationMessage.setText(String.format(
                "Typical resort price applied: \u20b1%.2f.", preset.getPrice()));
        editorScrollPane.revalidate();
        editorScrollPane.repaint();
        editorPriceField.requestFocusInWindow();
    }

    private void editAddOn() {
        AddOn selected = addOnJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select an add-on first.");
            return;
        }
        openEditor(selected);
    }

    private void openEditor(AddOn selected) {
        editingAddOnId = selected == null ? null : selected.getAddOnId();
        presetRow.setVisible(false);
        editorTitle.setText(selected == null ? "Add add-on" : "Edit add-on");
        editorNameField.setText(selected == null ? "" : selected.getName());
        editorPriceField.setText(selected == null ? "0.00"
                : String.valueOf(selected.getPrice()));
        editorDetailsArea.setText(selected == null ? "" : selected.getDetails());
        editorCategoryBox.setSelectedItem(selected == null
                ? AddOnCategory.FOOD : selected.getCategory());
        editorChargeBasisBox.setSelectedItem(selected == null
                ? AddOnChargeBasis.PER_BOOKING : selected.getChargeBasis());
        validationMessage.setText(" ");
        contentCards.show(contentWorkspace, EDITOR_VIEW);
        contentWorkspace.revalidate();
        contentWorkspace.repaint();
        editorNameField.requestFocusInWindow();
    }

    private void saveEditor() {
        String name = editorNameField.getText().trim();
        if (name.isEmpty()) {
            validationMessage.setText("Add-on name is required.");
            editorNameField.requestFocusInWindow();
            return;
        }
        Double price = parsePrice(editorPriceField.getText());
        if (price == null) {
            validationMessage.setText(
                    "Price must be a number that is zero or greater.");
            editorPriceField.requestFocusInWindow();
            editorPriceField.selectAll();
            return;
        }

        try {
            DataManager dataManager = DataManager.getInstance();
            if (editingAddOnId == null) {
                dataManager.addAddOn(new AddOn(dataManager.generateAddOnId(),
                        name, price, (AddOnCategory) editorCategoryBox.getSelectedItem(),
                        editorDetailsArea.getText().trim(),
                        (AddOnChargeBasis) editorChargeBasisBox.getSelectedItem()));
            } else {
                dataManager.editAddOn(editingAddOnId, name, price,
                        (AddOnCategory) editorCategoryBox.getSelectedItem(),
                        editorDetailsArea.getText().trim(),
                        (AddOnChargeBasis) editorChargeBasisBox.getSelectedItem());
            }
            editingAddOnId = null;
            saveChanges();
            refreshList();
            presetRow.setVisible(false);
            contentCards.show(contentWorkspace, CATALOG_VIEW);
            contentWorkspace.revalidate();
            contentWorkspace.repaint();
        } catch (IllegalArgumentException ex) {
            validationMessage.setText(ex.getMessage());
        }
    }

    private void closeEditor() {
        editingAddOnId = null;
        presetRow.setVisible(false);
        validationMessage.setText(" ");
        contentCards.show(contentWorkspace, CATALOG_VIEW);
        showSelectedDetails();
        contentWorkspace.revalidate();
        contentWorkspace.repaint();
    }

    private Double parsePrice(String value) {
        try {
            double price = Double.parseDouble(value);
            if (Double.isFinite(price) && price >= 0) {
                return price;
            }
        } catch (NumberFormatException ex) {
            // Show a consistent validation message for malformed input.
        }
        return null;
    }

    private void editExtraGuestRate() {
        double currentRate = DataManager.getInstance().getExtraPersonFeePerNight();
        JTextField rateField = new JTextField(String.valueOf(currentRate), 12);
        JLabel validationMessage = new JLabel(" ");
        validationMessage.setForeground(Theme.ERROR);
        Theme.styleInput(rateField);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.add(new JLabel("Fee per extra guest per night:"));
        form.add(rateField);
        form.add(validationMessage);

        Double rate = null;
        while (rate == null) {
            if (JOptionPane.showConfirmDialog(this, form, "Extra Guest Rate",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) {
                return;
            }
            rate = parsePrice(rateField.getText());
            if (rate == null) {
                validationMessage.setText(
                        "Fee must be a number that is zero or greater.");
                SwingUtilities.invokeLater(() -> {
                    rateField.requestFocusInWindow();
                    rateField.selectAll();
                });
            }
        }

        try {
            DataManager.getInstance().setExtraPersonFeePerNight(rate);
            double savedRate = rate;
            saveChanges(() -> {
                updateExtraGuestRateLabel();
                JOptionPane.showMessageDialog(this,
                        String.format("Extra guest rate set to \u20b1%.2f per person per night.",
                                savedRate));
            });
        } catch (IllegalArgumentException ex) {
            showValidationError(ex);
        }
    }

    private void updateExtraGuestRateLabel() {
        extraGuestRateLabel.setText(String.format(
                "Extra guest charge: \u20b1%.2f per person / night",
                DataManager.getInstance().getExtraPersonFeePerNight()));
    }

    private void removeAddOn() {
        AddOn selected = addOnJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select an add-on first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove \"" + selected.getName() + "\"?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            DataManager.getInstance().removeAddOn(selected.getAddOnId());
            refreshList();
            saveChanges();
        }
    }

    private void saveChanges() {
        saveChanges(null);
    }

    private void saveChanges(Runnable onSuccess) {
        SaveSupport.save(this, "The change was made, but could not be saved", onSuccess);
    }

    private void showValidationError(IllegalArgumentException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Invalid Add-on", JOptionPane.WARNING_MESSAGE);
    }

}