package resort.ui;

import resort.model.*;
import resort.controller.StaffController;
import resort.controller.StaffController.RoomAction;
import resort.controller.StaffController.TaskTarget;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StaffScreen extends JPanel {

    private final Staff employeeStaff;
    private final StaffController staffController =
            new StaffController(DataManager.getInstance());
    private DefaultListModel<Staff> listModel;
    private JList<Staff> staffJList;

    public StaffScreen() {
        this(null);
    }

    public StaffScreen(Staff employeeStaff) {
        this.employeeStaff = employeeStaff;
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        staffJList = new JList<>(listModel);
        Theme.styleList(staffJList);
        staffJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                Theme.styleListItemSeparator(item);
                return item;
            }
        });
        refreshList();

        BeachPanel background = new BeachPanel(new BorderLayout());

        JLabel title = new JLabel("\uD83D\uDC65  Staff", SwingConstants.CENTER);
        Theme.styleTitleLabel(title);
        title.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        background.add(title, BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(10, 10));
        card.add(new JScrollPane(staffJList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 3, 8, 8));
        buttons.setOpaque(false);
        if (employeeStaff == null) {
            JButton addStaffBtn = createButton("Add Staff", this::addStaff);
            JButton assignTaskBtn = createButton("Assign Task", this::assignTask);
            JButton viewTasksBtn = createButton("View Tasks", this::viewTasks);
            JButton housekeepingBtn = createButton("Update Room Status", this::updateRoomStatus);
            JButton ticketsBtn = createButton("View Tickets", this::viewTickets);
            JButton completeTaskBtn = createButton("Mark Complete", this::markTaskComplete);
            housekeepingBtn.setEnabled(false);

            staffJList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    Staff selected = staffJList.getSelectedValue();
                    housekeepingBtn.setEnabled(selected != null
                            && (selected.isHousekeeping() || selected.isMaintenance()));
                }
            });

            buttons.add(addStaffBtn);
            buttons.add(assignTaskBtn);
            buttons.add(viewTasksBtn);
            buttons.add(housekeepingBtn);
            buttons.add(ticketsBtn);
            buttons.add(completeTaskBtn);
        } else {
            staffJList.setEnabled(false);
            String workTitle = employeeStaff.isHousekeeping() ? "Housekeeping"
                    : employeeStaff.isMaintenance() ? "Maintenance"
                    : employeeStaff.isGuestServices() ? "Guest Services" : "Front Desk";
            JLabel roleHeading = new JLabel(workTitle + " workspace",
                    SwingConstants.CENTER);
            roleHeading.setFont(Theme.HEADER_FONT);
            roleHeading.setForeground(Theme.OCEAN_DEEP);
            card.add(roleHeading, BorderLayout.NORTH);
            buttons.add(createButton("My Tasks", this::viewTasks));
            buttons.add(createButton("Complete Task", this::markTaskComplete));
            if (employeeStaff.isHousekeeping() || employeeStaff.isMaintenance()) {
                String roomAction = employeeStaff.isHousekeeping()
                        ? "Room Readiness" : "Repair Queue";
                buttons.add(createButton(roomAction, this::updateRoomStatus));
            }
        }
        card.add(buttons, BorderLayout.SOUTH);

        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setOpaque(false);
        cardWrap.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        cardWrap.add(card, BorderLayout.CENTER);
        background.add(cardWrap, BorderLayout.CENTER);

        add(background, BorderLayout.CENTER);
    }

    private JButton createButton(String label, Runnable action) {
        JButton button = new JButton(label);
        Theme.styleButton(button);
        button.addActionListener(event -> action.run());
        return button;
    }

    public void refreshList() {
        listModel.clear();
        if (employeeStaff != null) {
            listModel.addElement(employeeStaff);
            staffJList.setSelectedIndex(0);
            return;
        }
        List<Staff> staff = DataManager.getInstance().getAllStaff();
        for (Staff s : staff) {
            listModel.addElement(s);
        }
    }

    private void addStaff() {
        String name = JOptionPane.showInputDialog(this, "Staff name:");
        if (name == null) return;

        Role[] roles = { Role.HOUSEKEEPING, Role.FRONT_DESK,
                Role.GUEST_SERVICES, Role.MAINTENANCE };
        Role role = (Role) JOptionPane.showInputDialog(
                this, "Role:", "Role",
                JOptionPane.QUESTION_MESSAGE, null, roles, roles[0]);
        if (role == null) return;

        try {
            DataManager.getInstance().addStaff(
                    new Staff(DataManager.getInstance().generateStaffId(), name.trim(),
                            role.getDisplayName()));
            saveChanges();
            refreshList();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid Staff Name", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void assignTask() {
        Staff selected = staffJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }

        StaffWorkflow workflow = StaffWorkflowFactory.forStaff(selected);
        DataManager dataManager = DataManager.getInstance();
        java.util.Map<TaskType, List<TaskTarget>> targetsByTaskType =
                new java.util.LinkedHashMap<>();
        for (TaskType taskType : workflow.getStandardTaskTypes()) {
            List<TaskTarget> targets = staffController.getTaskTargets(selected, taskType);
            if (!targets.isEmpty()) {
                targetsByTaskType.put(taskType, targets);
            }
        }
        if (targetsByTaskType.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There are no matching rooms or active guest bookings for "
                            + selected.getName() + "'s " + workflow.getRoleName()
                            + " tasks yet.",
                    "No Related Tasks", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<TaskType> taskTypeChoice = new JComboBox<>(
                targetsByTaskType.keySet().toArray(new TaskType[0]));
        JComboBox<TaskTarget> targetChoice = new JComboBox<>();
        Runnable refreshTargets = () -> {
            targetChoice.removeAllItems();
            List<TaskTarget> targets = targetsByTaskType.get(
                    taskTypeChoice.getSelectedItem());
            if (targets != null) {
                for (TaskTarget target : targets) {
                    targetChoice.addItem(target);
                }
            }
        };
        taskTypeChoice.addActionListener(event -> refreshTargets.run());
        refreshTargets.run();

        JPanel assignmentForm = new JPanel(new GridLayout(0, 1, 0, 6));
        assignmentForm.add(new JLabel("Task for " + selected.getName()
                + " (" + workflow.getRoleName() + "):"));
        assignmentForm.add(taskTypeChoice);
        assignmentForm.add(new JLabel("Related room / guest / service:"));
        assignmentForm.add(targetChoice);
        int choice = JOptionPane.showConfirmDialog(this, assignmentForm,
                "Assign Task", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        TaskType taskType = (TaskType) taskTypeChoice.getSelectedItem();
        TaskTarget target = (TaskTarget) targetChoice.getSelectedItem();
        if (taskType == null || target == null) {
            JOptionPane.showMessageDialog(this,
                    "Select a task and a related resort item.",
                    "Task Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String description;
        try {
            description = workflow.describeTask(taskType, target.getDetails());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid Task", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Task task = new Task(dataManager.generateTaskId(), description, selected);
        dataManager.addTask(task);
        selected.assignTask(task);
        saveChanges();
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

    private void updateRoomStatus() {
        Staff selected = staffJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }
        if (!selected.isHousekeeping() && !selected.isMaintenance()) {
            JOptionPane.showMessageDialog(this,
                    "Only Housekeeping or Maintenance staff can update room readiness.");
            return;
        }

        List<Room> rooms = new java.util.ArrayList<>();
        for (Room room : DataManager.getInstance().getAllRooms()) {
            if (room.getStatus() != RoomStatus.ARCHIVED) {
                rooms.add(room);
            }
        }
        if (rooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No rooms exist yet.");
            return;
        }

        Room room = (Room) JOptionPane.showInputDialog(
                this, "Room:", "Select Room",
                JOptionPane.QUESTION_MESSAGE, null,
                rooms.toArray(new Room[0]), rooms.get(0));
        if (room == null) return;

        List<RoomAction> actions =
                staffController.getAvailableRoomActions(selected, room);
        if (actions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "This room is "
                    + room.getStatusLabel()
                    + ". Only the assigned team can complete its next step.");
            return;
        }

        int choice = JOptionPane.showOptionDialog(this,
                "Next step for " + room.getRoomId() + " (" + room.getStatusLabel() + "):",
                "Room Inspection / Maintenance",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, actions.toArray(new RoomAction[0]), actions.get(0));

        if (choice < 0 || choice >= actions.size()) {
            return;
        }

        try {
            RoomAction action = actions.get(choice);
            staffController.performRoomAction(selected, room, action);
            saveChanges();
            JOptionPane.showMessageDialog(this, roomActionConfirmation(action));
            refreshList();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Room Not Updated", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String roomActionConfirmation(RoomAction action) {
        switch (action) {
            case INSPECTION_PASSED:
                return "Inspection complete. Room is unavailable until Housekeeping finishes cleaning.";
            case DAMAGE_FOUND:
                return "Room routed to Maintenance. An unassigned maintenance ticket was created.";
            case REPAIR_COMPLETED:
                return "Maintenance marked complete. Room is now waiting for Housekeeping.";
            case CLEANING_COMPLETED:
                return "Cleaning complete. Room is now available for guests.";
            default:
                throw new IllegalArgumentException("Unsupported room action: " + action);
        }
    }

    // Admin picks a staff member, then one of their non-completed tasks,
    // and marks it done. This is what closes the last known gap from the
    // previous handoff — task status was being set at creation ("PENDING")
    // and never touched again anywhere in the app.
    private void markTaskComplete() {
        Staff selected = staffJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member first.");
            return;
        }

        List<Task> pending = new java.util.ArrayList<>();
        for (Task t : selected.getTasks()) {
            if (t.getTaskStatus() != TaskStatus.COMPLETED) {
                pending.add(t);
            }
        }

        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    selected.getName() + " has no pending tasks.");
            return;
        }

        String[] descriptions = new String[pending.size()];
        for (int i = 0; i < pending.size(); i++) {
            descriptions[i] = pending.get(i).getTaskId() + ": " + pending.get(i).getDescription()
                    + " [" + pending.get(i).getStatus() + "]";
        }

        String chosen = (String) JOptionPane.showInputDialog(
                this, "Mark which task complete?", "Complete Task",
                JOptionPane.QUESTION_MESSAGE, null, descriptions, descriptions[0]);
        if (chosen == null) return;

        int index = java.util.Arrays.asList(descriptions).indexOf(chosen);
        Task task = pending.get(index);
        try {
            if (task.getDescription() != null
                    && task.getDescription().startsWith("[Maintenance]")) {
                DataManager.getInstance().completeMaintenanceTask(task);
            } else {
                task.setStatus(TaskStatus.COMPLETED);
            }
            saveChanges();
            JOptionPane.showMessageDialog(this, "Task marked complete.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Task Not Completed", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Shows tickets Employee submitted that have no staff assigned yet, and
    // lets Admin pick one to hand off to a selected staff member. This is
    // what closes the gap between "Employee can submit a ticket" and
    // "nothing ever displays it."
    private void viewTickets() {
        List<Task> unassigned = DataManager.getInstance().getUnassignedTasks();
        if (unassigned.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No unassigned tickets.");
            return;
        }

        JList<Task> ticketList = new JList<>(unassigned.toArray(new Task[0]));
        ticketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketList.setFixedCellWidth(520);
        ticketList.setCellRenderer(new TicketCellRenderer());
        ticketList.setSelectedIndex(0);
        JScrollPane ticketScrollPane = new JScrollPane(ticketList);
        ticketScrollPane.setPreferredSize(new Dimension(560, 320));

        int choice = JOptionPane.showConfirmDialog(this,
                new Object[] {"Select an unassigned ticket:", ticketScrollPane},
                "Tickets", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        Task ticket = ticketList.getSelectedValue();
        if (ticket == null) return;

        boolean maintenanceTicket = ticket.getDescription() != null
                && ticket.getDescription().startsWith("[Maintenance]");
        List<Staff> staff = new java.util.ArrayList<>();
        for (Staff candidate : DataManager.getInstance().getAllStaff()) {
            if (!maintenanceTicket || candidate.isMaintenance()) {
                staff.add(candidate);
            }
        }
        if (staff.isEmpty()) {
            JOptionPane.showMessageDialog(this, maintenanceTicket
                    ? "Add a Maintenance staff member before assigning this repair ticket."
                    : "No staff exist yet to assign this to.");
            return;
        }

        Staff assignee = (Staff) JOptionPane.showInputDialog(
                this, "Assign to:", "Assign Ticket",
                JOptionPane.QUESTION_MESSAGE, null,
                staff.toArray(new Staff[0]), staff.get(0));
        if (assignee == null) return;

        assignee.assignTask(ticket);
        refreshList();
        saveChanges();
        JOptionPane.showMessageDialog(this, "Ticket assigned to " + assignee.getName() + ".");
    }

    private static final class TicketCellRenderer extends JTextArea
            implements ListCellRenderer<Task> {

        TicketCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Task> list,
                Task ticket, int index, boolean isSelected, boolean cellHasFocus) {
            setFont(list.getFont());
            setText(ticket.getTaskId() + ": " + ticket.getDescription());
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0,
                            new Color(0xE5, 0xD9, 0xBE)),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            setSize(list.getFixedCellWidth() - 16, Short.MAX_VALUE);
            Dimension preferredSize = getPreferredSize();
            setPreferredSize(new Dimension(list.getFixedCellWidth(),
                    Math.max(preferredSize.height, getFontMetrics(getFont()).getHeight() + 16)));
            return this;
        }
    }

    private void saveChanges() {
        SaveSupport.save(this, "The change was made, but could not be saved", null);
    }
}
