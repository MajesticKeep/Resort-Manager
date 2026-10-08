package resort.ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import resort.model.*;

public class AddOnScreen extends JFrame {

    private DefaultListModel<AddOn> listModel;
    private JList<AddOn> addOnJList;
    private final boolean isAdmin;
    private int addOnCounter = 1;

    public AddOnScreen(Account account) {
        this.isAdmin = account.isAdmin();

        setTitle("Resort Management System - Add-ons");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        listModel = new DefaultListModel<>();
        refreshList();
        addOnJList = new JList<>(listModel);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(new JScrollPane(addOnJList), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton removeBtn = new JButton("Remove");

        addBtn.addActionListener(e -> addAddOn());
        editBtn.addActionListener(e -> editAddOn());
        removeBtn.addActionListener(e -> removeAddOn());

        // View-only for Employee — the buttons stay visible so it's clear
        // what actions exist, just disabled rather than hidden entirely.
        addBtn.setEnabled(isAdmin);
        editBtn.setEnabled(isAdmin);
        removeBtn.setEnabled(isAdmin);

        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(removeBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel);
    }

    private void refreshList() { 
        listModel.clear();
        List<AddOn> addOns = DataManager.getInstance().getAllAddOns();
        for (AddOn a : addOns) {
            listModel.addElement(a);
        }
    }

    private void addAddOn() {
        String name = JOptionPane.showInputDialog(this, "Add-on name:");
        if (name == null || name.trim().isEmpty()) return;

        String priceStr = JOptionPane.showInputDialog(this, "Price:");
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price must be a number.");
            return;
        }

        AddOnCategory category = (AddOnCategory) JOptionPane.showInputDialog(
                this, "Category:", "Category",
                JOptionPane.QUESTION_MESSAGE, null,
                AddOnCategory.values(), AddOnCategory.FOOD);
        if (category == null) return;

        DataManager.getInstance().addAddOn(
                new AddOn("A" + addOnCounter++, name.trim(), price, category));
        refreshList();
    }

    private void editAddOn() {
        AddOn selected = addOnJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select an add-on first.");
            return;
        }

        String name = JOptionPane.showInputDialog(this, "New name:", selected.getName());
        if (name == null || name.trim().isEmpty()) return;

        String priceStr = JOptionPane.showInputDialog(this, "New price:",
                String.valueOf(selected.getPrice()));
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price must be a number.");
            return;
        }

        AddOnCategory category = (AddOnCategory) JOptionPane.showInputDialog(
                this, "New category:", "Category",
                JOptionPane.QUESTION_MESSAGE, null,
                AddOnCategory.values(), selected.getCategory());
        if (category == null) return;

        DataManager.getInstance().editAddOn(selected.getAddOnId(), name.trim(), price, category);
        refreshList();
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
        }
    }
}