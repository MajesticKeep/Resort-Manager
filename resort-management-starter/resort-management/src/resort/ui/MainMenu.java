package resort.ui;

import java.awt.*;
import javax.swing.*;
import resort.model.Account;

public class MainMenu extends JFrame {

    public MainMenu(Account account) {
        setTitle("Resort Management System - Main Menu (" + account.getUsername() + ")");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JButton bookingsBtn = new JButton("Bookings");
        JButton roomsBtn = new JButton("Rooms");
        JButton staffBtn = new JButton("Staff");
        JButton maintenanceBtn = new JButton("Maintenance");
        JButton addOnsBtn = new JButton("Add-ons");

        bookingsBtn.addActionListener(e -> new BookingScreen().setVisible(true));
        roomsBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "Rooms screen not built yet."));
        staffBtn.addActionListener(e ->
            new StaffScreen().setVisible(true));
        maintenanceBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "Maintenance screen not built yet."));
        addOnsBtn.addActionListener(e -> new AddOnScreen(account).setVisible(true));

        panel.add(bookingsBtn);
        panel.add(roomsBtn);

        // Staff management is admin-only, per the group's access decision.
        if (account.isAdmin()) {
            panel.add(staffBtn);
        }

        panel.add(maintenanceBtn);
        panel.add(addOnsBtn);

        add(panel);
    }
}