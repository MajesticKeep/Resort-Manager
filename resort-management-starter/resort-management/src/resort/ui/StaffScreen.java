package resort.ui;

import resort.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StaffScreen extends JFrame {

    private DefaultListModel<Staff> listModel;
    private JList<Staff> staffJList;
    private int staffCounter = 1;
    private int taskCounter = 1;

    public StaffScreen() {
        setTitle("Resort Management System - Staff");
        setSize(420, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        listModel = new DefaultListModel<>();
        refreshList();
        staffJList = new JList<>(listModel);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(new JScrollPane(staffJList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        JButton addStaffBtn = new JButton("Add Staff");
        JButton assignTaskBtn = new JButton("Assign Task");
        JButton viewTasksBtn = new JButton("View Tasks");
        JButton maintenanceBtn = new JButton("Set Room Status");

        addStaffBtn.addActionListener(e -> addStaff());
        assignTaskBtn.addActionListener(e -> assignTask());
        viewTasksBtn.addActionListener(e -> viewTasks());
        maintenanceBtn.addActionListener(e -> setRoomStatus());

        buttons.add(addStaffBtn);
        buttons.add(assignTaskBtn);
        buttons.add(viewTasksBtn);
        buttons.add(maintenanceBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel);
    }

    private void refreshList() {
        listModel.clear();
        List<Staff> staff = DataManager.getInstance().getAllStaff();
        for (Staff s : staff) {
            listModel.addElement(s);
        }
    }

    private void addStaff() {
        String name = JOptionPane.showInputDialog(this, "Staff name:");
        if (name == null || name.trim().isEmpty()) return;

        String[] roles = { "Housekeeping", "Maintenance" };
        String role = (String) JOptionPane.showInputDialog(
                this, "Role:", "Role",
                JOptionPane.QUESTION_MESSAGE, null, roles, roles[0]);
        if (role == null) return;

        DataManager.getInstance().addStaff(
                new Staff("S" + staffCounter++, name.trim(), role));
        refreshList();
    }

    private void assignTask() {
        Staff selected = staffJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }

        String description = JOptionPane.showInputDialog(this, "Task description:");
        if (description == null || description.trim().isEmpty()) return;

        Task task = new Task("T" + taskCounter++, description.trim(), selected);
        selected.assignTask(task);
        JOptionPane.showMessageDialog(this,
                "Task assigned to " + selected.getName() + ".");
    }

    private void viewTasks() {
        Staff selected = staffJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }

        List<Task> tasks = selected.getTasks();
        if (tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, selected.getName() + " has no tasks yet.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Task t : tasks) {
            sb.append(t.getTaskId()).append(": ").append(t.getDescription())
              .append(" [").append(t.getStatus()).append("]\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Tasks", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setRoomStatus() {
        Staff selected = staffJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }
        if (!selected.getRole().equals("Maintenance")) {
            JOptionPane.showMessageDialog(this,
                    "Only Maintenance-role staff can change room status.");
            return;
        }

        List<Room> rooms = DataManager.getInstance().getAllRooms();
        if (rooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No rooms exist yet.");
            return;
        }

        Room room = (Room) JOptionPane.showInputDialog(
                this, "Room:", "Select Room",
                JOptionPane.QUESTION_MESSAGE, null,
                rooms.toArray(new Room[0]), rooms.get(0));
        if (room == null) return;

        String[] options = { "Mark under maintenance", "Mark available" };
        int choice = JOptionPane.showOptionDialog(this,
                "Set status for " + room.getRoomId() + ":", "Room Status",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            selected.markRoomForMaintenance(room);
        } else if (choice == 1) {
            selected.completeMaintenanceTask(room);
        }
        JOptionPane.showMessageDialog(this, "Room status updated.");
    }
}