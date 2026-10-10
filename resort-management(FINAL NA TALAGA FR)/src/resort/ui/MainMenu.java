package resort.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import resort.model.Account;
import resort.model.DataManager;
import resort.model.Staff;
import resort.model.Task;
import resort.model.TaskStatus;

public class MainMenu extends JFrame {
    private static final String HOME_SCREEN = "home";

    private final JPanel screens = new JPanel(new CardLayout());
    private final JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
    private final Map<String, Runnable> screenRefreshActions = new HashMap<>();
    private final Staff linkedStaff;

    public MainMenu(Account account) {
        linkedStaff = account.isAdmin() ? null : findLinkedStaff(account);
        setTitle("Resort Management System - Main Menu (" + account.getUsername() + ")");
        setSize(Theme.screenSize(0.70, 0.78));
        setMinimumSize(new Dimension(760, 540));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Keep the application alive until the queued background save completes.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setEnabled(false);
                DataManager.getInstance().saveDataAsync(() -> dispose(), error -> {
                    setEnabled(true);
                    JOptionPane.showMessageDialog(MainMenu.this,
                            "Could not save resort data:\n" + error.getMessage(),
                            "Save Failed", JOptionPane.ERROR_MESSAGE);
                });
            }
        });

        JPanel dashboard = createDashboard(account);

        screens.setOpaque(false);
        screens.add(dashboard, HOME_SCREEN);
        refreshDashboard();
        BookingScreen bookingScreen = new BookingScreen(
                () -> showScreen("booking-management"), () -> showScreen(HOME_SCREEN));
        BookingManagementScreen bookingManagementScreen =
                new BookingManagementScreen(account);
        RoomScreen roomScreen = new RoomScreen(account);
        AddOnScreen addOnScreen = new AddOnScreen(account);
        screens.add(bookingScreen, "bookings");
        screens.add(bookingManagementScreen, "booking-management");
        screens.add(roomScreen, "rooms");
        screens.add(addOnScreen, "add-ons");
        screenRefreshActions.put("bookings", bookingScreen::refreshCatalog);
        screenRefreshActions.put("booking-management", bookingManagementScreen::refreshList);
        screenRefreshActions.put("rooms", roomScreen::refreshList);
        screenRefreshActions.put("add-ons", addOnScreen::refreshList);
        if (account.isAdmin() || linkedStaff != null) {
            StaffScreen staffScreen = account.isAdmin()
                    ? new StaffScreen() : new StaffScreen(linkedStaff);
            screens.add(staffScreen, "staff");
            screenRefreshActions.put("staff", staffScreen::refreshList);
            if (account.isAdmin()) {
                AccountManagementScreen accountScreen = new AccountManagementScreen();
                screens.add(accountScreen, "accounts");
                screenRefreshActions.put("accounts", accountScreen::refreshAccounts);
            }
        }

        JButton returnButton = new JButton("Return to Main Menu");
        Theme.styleSecondaryButton(returnButton);
        returnButton.addActionListener(e -> showScreen(HOME_SCREEN));
        navigation.setOpaque(true);
        navigation.setBackground(Theme.SAND);
        navigation.add(returnButton);
        navigation.setVisible(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.SAND);
        root.add(screens, BorderLayout.CENTER);
        root.add(navigation, BorderLayout.SOUTH);
        add(root);
    }

    private void showScreen(String screenName) {
        if (HOME_SCREEN.equals(screenName)) {
            refreshDashboard();
        }
        Runnable refresh = screenRefreshActions.get(screenName);
        if (refresh != null) {
            refresh.run();
        }
        ((CardLayout) screens.getLayout()).show(screens, screenName);
        navigation.setVisible(!HOME_SCREEN.equals(screenName));
    }

    private Staff findLinkedStaff(Account account) {
        if (account.getStaffId() == null) {
            return null;
        }
        for (Staff staff : DataManager.getInstance().getAllStaff()) {
            if (staff.getStaffId().equals(account.getStaffId())) {
                return staff;
            }
        }
        return null;
    }

    private JPanel createDashboard(Account account) {
        BeachPanel background = new BeachPanel(new BorderLayout(18, 16));
        background.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));

        JPanel header = new JPanel(new BorderLayout(12, 4));
        header.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("The Juans Resort");
        Theme.styleTitleLabel(title);
        title.setFont(Theme.TITLE_FONT.deriveFont(Font.BOLD, 30f));
        String roleLabel = account.isAdmin() ? "Administrator"
                : linkedStaff == null ? "Employee - role not linked"
                : linkedStaff.getRole();
        JLabel subtitle = new JLabel("Welcome, " + account.getUsername()
                + "  \u2022  " + roleLabel);
        Theme.styleLabel(subtitle);
        subtitle.setForeground(Theme.OCEAN_DEEP);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        JLabel prompt = new JLabel("Your resort operations at a glance");
        Theme.styleLabel(prompt);
        header.add(heading, BorderLayout.WEST);
        header.add(prompt, BorderLayout.EAST);
        background.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.62;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 12);
        content.add(createQuickActions(account), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.38;
        constraints.insets = new Insets(0, 12, 0, 0);
        content.add(createOverview(account), constraints);
        background.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel dateLabel = new JLabel(LocalDate.now().toString());
        Theme.styleLabel(dateLabel);
        dateLabel.setForeground(Theme.OCEAN_DEEP);
        JButton logoutButton = new JButton("Log Out");
        Theme.styleSecondaryButton(logoutButton);
        logoutButton.addActionListener(event -> confirmLogOut());
        footer.add(dateLabel, BorderLayout.WEST);
        footer.add(logoutButton, BorderLayout.EAST);
        background.add(footer, BorderLayout.SOUTH);
        return background;
    }

    private JPanel createQuickActions(Account account) {
        JPanel panel = Theme.card();
        panel.setLayout(new BorderLayout(0, 14));
        JLabel sectionTitle = new JLabel("Quick actions");
        sectionTitle.setFont(Theme.HEADER_FONT);
        sectionTitle.setForeground(Theme.OCEAN_DEEP);
        panel.add(sectionTitle, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        List<JButton> actionButtons = new ArrayList<>();
        if (account.isAdmin()) {
            actionButtons.add(createActionButton("New booking", "Create a reservation",
                    "bookings", 0));
            actionButtons.add(createActionButton("Manage bookings",
                    "Check in, check out, or reschedule", "booking-management", 1));
            actionButtons.add(createActionButton("Rooms", "View room rates and readiness",
                    "rooms", 2));
            actionButtons.add(createActionButton("Add-ons", "Browse resort offerings",
                    "add-ons", 3));
            actionButtons.add(createActionButton("Staff & tasks", "Assignments and tickets",
                    "staff", 4));
            actionButtons.add(createActionButton("Team accounts", "Manage staff access",
                    "accounts", 5));
        } else if (linkedStaff == null) {
            actionButtons.add(createActionButton("Staff role not linked",
                    "Ask an administrator to connect your staff profile",
                    null, 5));
        } else {
            if (linkedStaff.getRoleType() == resort.model.Role.FRONT_DESK) {
                actionButtons.add(createActionButton("New booking",
                        "Create a guest reservation", "bookings", 0));
                actionButtons.add(createActionButton("Manage bookings",
                        "Check in, check out, or reschedule",
                        "booking-management", 1));
                actionButtons.add(createActionButton("Room availability",
                        "View room rates and readiness", "rooms", 2));
                actionButtons.add(createActionButton("Guest add-ons",
                        "Browse resort offerings", "add-ons", 3));
                actionButtons.add(createActionButton("My work",
                        "View your assigned front desk tasks", "staff", 4));
            } else if (linkedStaff.isHousekeeping()) {
                actionButtons.add(createActionButton("Room readiness",
                        "Inspect rooms and complete cleaning", "staff", 2));
                actionButtons.add(createActionButton("My cleaning tasks",
                        "View and complete assigned tasks", "staff", 4));
            } else if (linkedStaff.isMaintenance()) {
                actionButtons.add(createActionButton("Repair queue",
                        "View rooms awaiting maintenance", "staff", 2));
                actionButtons.add(createActionButton("My maintenance tasks",
                        "View and complete assigned tasks", "staff", 4));
            } else if (linkedStaff.isGuestServices()) {
                actionButtons.add(createActionButton("Guest requests",
                        "View and complete guest service tasks", "staff", 6));
                actionButtons.add(createActionButton("Resort offerings",
                        "Browse activities and guest add-ons", "add-ons", 3));
            } else {
                actionButtons.add(createActionButton("My work",
                        "View and complete your assigned tasks", "staff", 4));
            }
        }
        if (!account.isAdmin()) {
            JButton ticketButton = createActionButton("Report a problem",
                    "Send a ticket to the administrator", null, 6);
            ticketButton.addActionListener(event -> submitTicket());
            actionButtons.add(ticketButton);
        }
        for (int i = 0; i < actionButtons.size(); i++) {
            GridBagConstraints actionConstraints = new GridBagConstraints();
            actionConstraints.gridx = i % 2;
            actionConstraints.gridy = i / 2;
            actionConstraints.weightx = 1;
            actionConstraints.weighty = 1;
            actionConstraints.fill = GridBagConstraints.BOTH;
            actionConstraints.insets = new Insets(6, 6, 6, 6);
            if (i == actionButtons.size() - 1 && actionButtons.size() % 2 != 0) {
                actionConstraints.gridwidth = 2;
            }
            actions.add(actionButtons.get(i), actionConstraints);
        }
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private JButton createActionButton(String title, String description,
                                       String screenName, int iconType) {
        JButton button = new ActionTile(title, description, iconType);
        if (screenName != null) {
            button.addActionListener(event -> showScreen(screenName));
        }
        return button;
    }

    private static final class ActionTile extends JButton {
        private static final Color TILE = new Color(0xF7, 0xFB, 0xF8);
        private static final Color TILE_HOVER = new Color(0xE9, 0xF4, 0xEF);
        private static final Color TILE_BORDER = new Color(0xD7, 0xE5, 0xDD);
        private static final Color MARK = new Color(0x05, 0x83, 0x9C, 105);

        private final String title;
        private final String description;
        private final int iconType;

        ActionTile(String title, String description, int iconType) {
            this.title = title;
            this.description = description;
            this.iconType = iconType;
            setText("");
            getAccessibleContext().setAccessibleName(title + ". " + description);
            setFont(Theme.LABEL_FONT);
            setForeground(Theme.TEXT_DARK);
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 76));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(220, 108));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(getModel().isRollover() || hasFocus() ? TILE_HOVER : TILE);
            g.fillRoundRect(0, 0, width, height, 12, 12);
            g.setColor(Theme.OCEAN_MID);
            g.fillRoundRect(0, 0, 5, height, 5, 5);
            g.setColor(TILE_BORDER);
            g.drawRoundRect(0, 0, width - 1, height - 1, 12, 12);
            g.dispose();
            super.paintComponent(graphics);

            Graphics2D content = (Graphics2D) graphics.create();
            content.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets insets = getInsets();
            int textX = insets.left;
            int availableWidth = Math.max(1, width - insets.left - insets.right);
            content.setClip(textX, 0, availableWidth, height);
            int textY = insets.top + 18;
            textY = drawWrappedText(content, title,
                    Theme.HEADER_FONT.deriveFont(Font.BOLD, 16f),
                    Theme.TEXT_DARK, textX, textY, availableWidth);
            textY += 3;
            textY = drawWrappedText(content, description,
                    Theme.LABEL_FONT.deriveFont(13f),
                    new Color(0x38, 0x54, 0x5A), textX, textY, availableWidth);
            textY += 4;
            content.setFont(Theme.LABEL_FONT.deriveFont(Font.BOLD, 11f));
            content.setColor(Theme.OCEAN_MID);
            content.drawString("OPEN  \u2192", textX, textY);
            content.setClip(null);
            paintWatermark(content, width, height);
            content.dispose();
        }

        private int drawWrappedText(Graphics2D g, String text, Font font, Color color,
                                    int x, int baseline, int maxWidth) {
            g.setFont(font);
            g.setColor(color);
            FontMetrics metrics = g.getFontMetrics();
            StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                    g.drawString(line.toString(), x, baseline);
                    baseline += metrics.getHeight();
                    line.setLength(0);
                    line.append(word);
                } else {
                    if (line.length() > 0) {
                        line.append(' ');
                    }
                    line.append(word);
                }
            }
            if (line.length() > 0) {
                g.drawString(line.toString(), x, baseline);
                baseline += metrics.getHeight();
            }
            return baseline;
        }

        private void paintWatermark(Graphics2D g, int width, int height) {
            int centerX = width - 42;
            int centerY = height / 2;
            g.setColor(new Color(0x6B, 0xC9, 0xD9, 38));
            g.fillOval(centerX - 27, centerY - 27, 54, 54);
            g.setColor(MARK);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            switch (iconType) {
                case 0:
                    g.drawRoundRect(centerX - 12, centerY - 12, 24, 23, 4, 4);
                    g.drawLine(centerX - 12, centerY - 5, centerX + 12, centerY - 5);
                    g.drawLine(centerX - 6, centerY - 15, centerX - 6, centerY - 9);
                    g.drawLine(centerX + 6, centerY - 15, centerX + 6, centerY - 9);
                    g.drawLine(centerX - 6, centerY + 1, centerX - 2, centerY + 1);
                    g.drawLine(centerX + 3, centerY + 1, centerX + 7, centerY + 1);
                    break;
                case 1:
                    g.drawRoundRect(centerX - 11, centerY - 13, 22, 27, 4, 4);
                    for (int row = 0; row < 3; row++) {
                        int y = centerY - 6 + row * 7;
                        g.drawOval(centerX - 7, y - 1, 3, 3);
                        g.drawLine(centerX - 1, y, centerX + 7, y);
                    }
                    break;
                case 2:
                    g.drawLine(centerX - 14, centerY + 9, centerX + 14, centerY + 9);
                    g.drawLine(centerX - 11, centerY + 9, centerX - 11, centerY + 14);
                    g.drawLine(centerX + 11, centerY + 9, centerX + 11, centerY + 14);
                    g.drawRoundRect(centerX - 10, centerY - 5, 10, 9, 3, 3);
                    g.drawLine(centerX, centerY - 1, centerX + 11, centerY - 1);
                    g.drawLine(centerX + 11, centerY - 1, centerX + 11, centerY + 9);
                    break;
                case 3:
                    g.drawOval(centerX - 11, centerY - 11, 9, 9);
                    g.drawOval(centerX + 2, centerY - 11, 9, 9);
                    g.drawOval(centerX - 11, centerY + 2, 9, 9);
                    g.drawOval(centerX + 2, centerY + 2, 9, 9);
                    break;
                case 4:
                    g.drawRoundRect(centerX - 11, centerY - 12, 22, 25, 4, 4);
                    g.drawLine(centerX - 6, centerY - 4, centerX - 3, centerY - 1);
                    g.drawLine(centerX - 3, centerY - 1, centerX + 1, centerY - 6);
                    g.drawLine(centerX + 3, centerY - 4, centerX + 7, centerY - 4);
                    g.drawLine(centerX - 6, centerY + 5, centerX + 7, centerY + 5);
                    break;
                case 5:
                    g.drawOval(centerX - 11, centerY - 12, 9, 9);
                    g.drawOval(centerX + 2, centerY - 12, 9, 9);
                    g.drawArc(centerX - 15, centerY - 1, 17, 16, 0, 180);
                    g.drawArc(centerX - 2, centerY - 1, 17, 16, 0, 180);
                    break;
                default:
                    g.drawRoundRect(centerX - 13, centerY - 9, 26, 19, 6, 6);
                    g.drawLine(centerX - 5, centerY + 10, centerX - 10, centerY + 15);
                    g.drawLine(centerX - 10, centerY + 15, centerX + 1, centerY + 9);
                    g.drawLine(centerX - 7, centerY - 2, centerX + 7, centerY - 2);
                    g.drawLine(centerX - 7, centerY + 3, centerX + 3, centerY + 3);
            }
        }
    }

    private JPanel createOverview(Account account) {
        JPanel panel = Theme.card();
        panel.setLayout(new BorderLayout(0, 12));
        JLabel sectionTitle = new JLabel("Today's overview");
        sectionTitle.setFont(Theme.HEADER_FONT);
        sectionTitle.setForeground(Theme.OCEAN_DEEP);
        panel.add(sectionTitle, BorderLayout.NORTH);

        JPanel summaries = new JPanel(new GridLayout(0, 2, 10, 10));
        summaries.setOpaque(false);
        addSummary(summaries, "Ready rooms", "0", "Available to prepare for guests");
        addSummary(summaries, "Check-ins", "0", "Scheduled for today");
        addSummary(summaries, "Check-outs", "0", "Due today");
        if (account.isAdmin()) {
            addSummary(summaries, "Pending tasks", "0", "Not marked complete");
            addSummary(summaries, "Open tickets", "0", "Waiting for staff assignment");
            addSummary(summaries, "Staff members", "0", "On the resort team");
        } else {
            addSummary(summaries, "Resort add-ons", "0", "Available to include with bookings");
        }
        panel.add(summaries, BorderLayout.CENTER);
        return panel;
    }

    private void addSummary(JPanel parent, String title, String initialValue,
                            String description) {
        JPanel card = new SummaryTile();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 12));
        JLabel value = new JLabel(initialValue);
        value.setName("summary:" + title);
        value.setFont(Theme.TITLE_FONT.deriveFont(Font.BOLD, 27f));
        value.setForeground(Theme.OCEAN_DEEP);
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.LABEL_FONT.deriveFont(Font.BOLD, 14f));
        heading.setForeground(Theme.TEXT_DARK);
        JLabel hint = new JLabel("<html>" + description + "</html>");
        hint.setFont(Theme.LABEL_FONT.deriveFont(12f));
        hint.setForeground(Theme.OCEAN_DEEP);
        card.add(value);
        card.add(Box.createVerticalStrut(4));
        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(hint);
        parent.add(card);
    }

    private static final class SummaryTile extends JPanel {
        SummaryTile() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            g.setColor(Theme.INPUT_CREAM);
            g.fillRoundRect(0, 0, width, height, 16, 16);
            g.setColor(new Color(0xD8, 0xCF, 0xB7));
            g.drawRoundRect(0, 0, width, height, 16, 16);
            g.setColor(Theme.OCEAN_LIGHT);
            g.setStroke(new BasicStroke(3f));
            g.drawLine(2, 10, 2, height - 10);
            g.dispose();
        }
    }

    private void refreshDashboard() {
        DataManager data = DataManager.getInstance();
        LocalDate today = LocalDate.now();
        int readyRooms = 0;
        int checkIns = 0;
        int checkOuts = 0;
        int pendingTasks = 0;
        for (resort.model.Room room : data.getAllRooms()) {
            if (room.getStatus() == resort.model.RoomStatus.AVAILABLE) {
                readyRooms++;
            }
        }
        for (resort.model.Booking booking : data.getAllBookings()) {
            if (booking.getStatus() == resort.model.BookingStatus.RESERVED
                    && today.equals(booking.getCheckInDate())) {
                checkIns++;
            }
            if (booking.getStatus() == resort.model.BookingStatus.CHECKED_IN
                    && today.equals(booking.getCheckOutDate())) {
                checkOuts++;
            }
        }
        for (Task task : data.getAllTasks()) {
            if (task.getAssignedTo() != null
                    && task.getTaskStatus() != TaskStatus.COMPLETED) {
                pendingTasks++;
            }
        }

        setSummaryValue("Ready rooms", readyRooms);
        setSummaryValue("Check-ins", checkIns);
        setSummaryValue("Check-outs", checkOuts);
        setSummaryValue("Pending tasks", pendingTasks);
        int openTickets = 0;
        for (Task task : data.getUnassignedTasks()) {
            if (task.getTaskStatus() != TaskStatus.COMPLETED) {
                openTickets++;
            }
        }
        setSummaryValue("Open tickets", openTickets);
        setSummaryValue("Staff members", data.getAllStaff().size());
        setSummaryValue("Resort add-ons", data.getAllAddOns().size());
    }

    private void setSummaryValue(String title, int value) {
        for (Component component : screens.getComponents()) {
            if (!(component instanceof JPanel)) continue;
            updateSummaryValue((Container) component, title, value);
        }
    }

    private void updateSummaryValue(Container container, String title, int value) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel
                    && ("summary:" + title).equals(component.getName())) {
                ((JLabel) component).setText(String.valueOf(value));
            }
            if (component instanceof Container) {
                updateSummaryValue((Container) component, title, value);
            }
        }
    }

    private void submitTicket() {
        JTextArea descriptionArea = new JTextArea(6, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        Theme.styleInput(descriptionArea);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setPreferredSize(new Dimension(480, 150));
        JPanel ticketForm = new JPanel(new BorderLayout(0, 8));
        ticketForm.add(new JLabel("Describe the issue:"), BorderLayout.NORTH);
        ticketForm.add(descriptionScrollPane, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(this, ticketForm,
                "Submit Problem Ticket", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A ticket description is required.",
                    "Description Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // No Staff assigned yet — deliberately unassigned. Admin picks it
        // up and assigns a real staff member from the Staff screen's
        // ticket queue.
        Task ticket = new Task(DataManager.getInstance().generateTaskId(),
                description.trim(), null);
        DataManager.getInstance().addTask(ticket);
        DataManager.getInstance().saveDataAsync(() ->
                JOptionPane.showMessageDialog(this,
                        "Ticket submitted: " + ticket.getTaskId()), error ->
                JOptionPane.showMessageDialog(this,
                        "The ticket was created, but could not be saved:\n"
                                + error.getMessage(),
                        "Save Failed", JOptionPane.ERROR_MESSAGE));
    }

    private void confirmLogOut() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?",
                "Confirm Log Out",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        setEnabled(false);
        DataManager.getInstance().saveDataAsync(() -> {
            new LoginScreen().setVisible(true);
            dispose();
        }, error -> {
            setEnabled(true);
            JOptionPane.showMessageDialog(this,
                    "Could not save resort data:\n" + error.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        });
    }
}
