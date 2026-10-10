package resort.ui;

import resort.model.Account;
import resort.model.DataManager;
import resort.model.Staff;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AccountManagementScreen extends JPanel {

    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final DefaultListModel<Account> listModel = new DefaultListModel<>();
    private final JList<Account> accountList = new JList<>(listModel);
    private final JLabel accountCountLabel = new JLabel();
    private final JPanel workspace = new JPanel(new BorderLayout());
    private final JPanel accountWorkspace = new JPanel(new GridLayout(1, 2, 16, 0));
    private final JPanel detailsPanel = new JPanel(new BorderLayout());
    private JButton addButton;
    private boolean creatingEmployee;
    private boolean savingChanges;

    public AccountManagementScreen() {
        setLayout(new BorderLayout());

        configureAccountList();

        BeachPanel background = new BeachPanel(new BorderLayout());
        background.add(createHeader(), BorderLayout.NORTH);
        background.add(createWorkspace(), BorderLayout.CENTER);
        add(background, BorderLayout.CENTER);

        refreshAccounts();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 30, 14, 30));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Team accounts");
        Theme.styleTitleLabel(title);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel subtitle = new JLabel("Manage staff access to the resort system");
        Theme.styleLabel(subtitle);
        subtitle.setForeground(Theme.OCEAN_DEEP);

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        addButton = new JButton("+  Add employee");
        Theme.styleButton(addButton);
        addButton.addActionListener(event -> addEmployee());

        header.add(heading, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkspace() {
        workspace.setOpaque(false);
        workspace.setBorder(new EmptyBorder(8, 30, 26, 30));
        accountWorkspace.setOpaque(false);

        JPanel directory = Theme.card();
        directory.setLayout(new BorderLayout(0, 12));

        JPanel directoryHeading = new JPanel(new BorderLayout());
        directoryHeading.setOpaque(false);
        JLabel directoryTitle = new JLabel("ACCOUNT DIRECTORY");
        directoryTitle.setFont(Theme.HEADER_FONT);
        directoryTitle.setForeground(Theme.OCEAN_DEEP);
        accountCountLabel.setFont(Theme.LABEL_FONT.deriveFont(Font.BOLD, 14f));
        accountCountLabel.setForeground(Theme.PALM);
        directoryHeading.add(directoryTitle, BorderLayout.WEST);
        directoryHeading.add(accountCountLabel, BorderLayout.EAST);
        directory.add(directoryHeading, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(accountList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xD8, 0xCF, 0xB7)));
        scrollPane.getViewport().setBackground(Theme.INPUT_CREAM);
        directory.add(scrollPane, BorderLayout.CENTER);
        accountWorkspace.add(directory);

        detailsPanel.setOpaque(true);
        detailsPanel.setBackground(Theme.INPUT_CREAM);
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SAND_DARK, 1),
                new EmptyBorder(22, 22, 22, 22)));
        accountWorkspace.add(detailsPanel);
        workspace.add(accountWorkspace, BorderLayout.CENTER);
        updateAccountActions();
        return workspace;
    }

    private void configureAccountList() {
        Theme.styleList(accountList);
        accountList.setFixedCellHeight(76);
        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountList.setBorder(new EmptyBorder(4, 4, 4, 4));
        accountList.setCellRenderer(new AccountCellRenderer());
        accountList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateAccountActions();
            }
        });
        accountList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && accountList.getSelectedValue() != null) {
                    changePassword();
                }
            }
        });
    }

    public void refreshAccounts() {
        Account selected = accountList.getSelectedValue();
        String selectedUsername = selected == null ? null : selected.getUsername();

        listModel.clear();
        List<Account> accounts = DataManager.getInstance().getAllAccounts();
        for (Account account : accounts) {
            listModel.addElement(account);
            if (account.getUsername().equals(selectedUsername)) {
                accountList.setSelectedValue(account, true);
            }
        }

        accountCountLabel.setText(accounts.size() + (accounts.size() == 1 ? " account" : " accounts"));
        updateAccountActions();
    }

    private void updateAccountActions() {
        if (creatingEmployee) {
            return;
        }
        detailsPanel.removeAll();
        Account selected = accountList.getSelectedValue();
        if (selected == null) {
            detailsPanel.add(createEmptyDetails(), BorderLayout.CENTER);
        } else {
            detailsPanel.add(createSelectedDetails(selected), BorderLayout.CENTER);
        }
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private JPanel createEmptyDetails() {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("\u2659");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        icon.setForeground(Theme.OCEAN_MID);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Select an account");
        title.setFont(Theme.HEADER_FONT);
        title.setForeground(Theme.TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("<html><div style='text-align:center'>"
                + "Choose someone from the directory<br>to manage their access.</div></html>");
        Theme.styleLabel(hint);
        hint.setForeground(Theme.OCEAN_DEEP);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        empty.add(Box.createVerticalGlue());
        empty.add(icon);
        empty.add(Box.createVerticalStrut(12));
        empty.add(title);
        empty.add(Box.createVerticalStrut(8));
        empty.add(hint);
        empty.add(Box.createVerticalGlue());
        return empty;
    }

    private JPanel createSelectedDetails(Account account) {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        Avatar avatar = new Avatar(account.getUsername(), 64);
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(account.getUsername());
        name.setFont(new Font("SansSerif", Font.BOLD, 24));
        name.setForeground(Theme.TEXT_DARK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        Staff linkedStaff = getLinkedStaff(account);
        String accountRole = account.isAdmin() ? "ADMINISTRATOR"
                : linkedStaff == null ? "EMPLOYEE - UNLINKED"
                : "EMPLOYEE - " + linkedStaff.getRole().toUpperCase();
        JLabel role = new JLabel(accountRole);
        role.setFont(Theme.BUTTON_FONT.deriveFont(13f));
        role.setForeground(Theme.PALM);
        role.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel status = new JLabel("ACTIVE ACCOUNT");
        status.setFont(Theme.LABEL_FONT.deriveFont(Font.BOLD, 13f));
        status.setForeground(Theme.PALM);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(account.isAdmin()
                ? "<html>Administrator access to staff, accounts,<br>and resort operations.</html>"
                : "<html>Employee access to resort operations<br>and assigned responsibilities.</html>");
        Theme.styleLabel(description);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton changePasswordButton = new JButton("Change password");
        Theme.styleButton(changePasswordButton);
        changePasswordButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePasswordButton.addActionListener(event -> changePassword());

        content.add(Box.createVerticalGlue());
        content.add(avatar);
        content.add(Box.createVerticalStrut(18));
        content.add(name);
        content.add(Box.createVerticalStrut(4));
        content.add(role);
        content.add(Box.createVerticalStrut(18));
        content.add(status);
        content.add(Box.createVerticalStrut(10));
        content.add(description);
        content.add(Box.createVerticalStrut(24));
        content.add(changePasswordButton);

        if (!account.isAdmin()) {
            if (linkedStaff == null) {
                JButton linkStaffButton = new JButton("Link staff record");
                Theme.styleButton(linkStaffButton);
                linkStaffButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                linkStaffButton.addActionListener(event -> linkStaffRecord(account));
                content.add(Box.createVerticalStrut(10));
                content.add(linkStaffButton);
            }
            JButton promoteButton = new JButton("Promote to administrator");
            Theme.styleButton(promoteButton);
            promoteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            promoteButton.addActionListener(event -> promoteEmployee(account));
            content.add(Box.createVerticalStrut(10));
            content.add(promoteButton);

            JButton deactivateButton = new JButton("Deactivate account");
            Theme.styleSecondaryButton(deactivateButton);
            deactivateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            deactivateButton.addActionListener(event -> deactivateEmployee());
            content.add(Box.createVerticalStrut(10));
            content.add(deactivateButton);
        } else {
            JLabel protectedNotice = new JLabel("Admin accounts cannot be deactivated.");
            Theme.styleLabel(protectedNotice);
            protectedNotice.setFont(Theme.LABEL_FONT.deriveFont(13f));
            protectedNotice.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(Box.createVerticalStrut(12));
            content.add(protectedNotice);

            JButton demoteButton = new JButton("Demote to employee");
            Theme.styleSecondaryButton(demoteButton);
            demoteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            demoteButton.addActionListener(event -> demoteAdmin(account));
            content.add(Box.createVerticalStrut(10));
            content.add(demoteButton);
        }

        content.add(Box.createVerticalGlue());
        return content;
    }

    private void addEmployee() {
        if (savingChanges) {
            return;
        }
        DataManager dataManager = DataManager.getInstance();
        List<Staff> unlinkedStaff = dataManager.getUnlinkedStaff();
        detailsPanel.removeAll();
        creatingEmployee = true;
        accountList.setEnabled(false);
        addButton.setEnabled(false);
        workspace.removeAll();
        workspace.add(detailsPanel, BorderLayout.CENTER);

        if (unlinkedStaff.isEmpty()) {
            JPanel emptyForm = new JPanel();
            emptyForm.setOpaque(false);
            emptyForm.setLayout(new BoxLayout(emptyForm, BoxLayout.Y_AXIS));
            JLabel message = new JLabel("<html>Add the employee's staff record in "
                    + "Staff &amp; Tasks first, then create their account here.</html>");
            Theme.styleLabel(message);
            JButton cancelButton = new JButton("Back to accounts");
            Theme.styleSecondaryButton(cancelButton);
            cancelButton.addActionListener(event -> closeEmployeeForm());
            emptyForm.add(message);
            emptyForm.add(Box.createVerticalStrut(12));
            emptyForm.add(cancelButton);
            detailsPanel.add(emptyForm, BorderLayout.CENTER);
            workspace.revalidate();
            workspace.repaint();
            detailsPanel.revalidate();
            detailsPanel.repaint();
            return;
        }

        JComboBox<Staff> staffBox = new JComboBox<>(
                unlinkedStaff.toArray(new Staff[0]));
        JTextField usernameField = new JTextField();
        JLabel usernameStatus = new JLabel(
                "<html>Select a staff record for a username suggestion.</html>");
        JPasswordField passwordField = new JPasswordField(createTemporaryPassword());
        passwordField.setEditable(false);
        char passwordEchoChar = passwordField.getEchoChar();
        JCheckBox showPassword = new JCheckBox("Show");
        showPassword.setOpaque(false);
        showPassword.addActionListener(event -> passwordField.setEchoChar(
                showPassword.isSelected() ? (char) 0 : passwordEchoChar));
        JLabel validationMessage = new JLabel(" ");
        validationMessage.setForeground(Theme.ERROR);
        validationMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        Theme.styleInput(staffBox);
        Theme.styleInput(usernameField);
        Theme.styleInput(passwordField);
        Theme.styleLabel(usernameStatus);
        JPanel passwordControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        passwordControls.setOpaque(false);
        passwordControls.add(showPassword);
        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setOpaque(false);
        passwordRow.add(passwordField, BorderLayout.CENTER);
        passwordRow.add(passwordControls, BorderLayout.EAST);

        final boolean[] manuallyEditedUsername = { false };
        final boolean[] updatingSuggestion = { false };
        staffBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof Staff) {
                    Staff staff = (Staff) value;
                    item.setText(staff.getName() + " - " + staff.getRole());
                }
                return item;
            }
        });
        usernameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                if (!updatingSuggestion[0]) manuallyEditedUsername[0] = true;
                updateUsernameAvailability(usernameField, usernameStatus);
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                if (!updatingSuggestion[0]) manuallyEditedUsername[0] = true;
                updateUsernameAvailability(usernameField, usernameStatus);
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                if (!updatingSuggestion[0]) manuallyEditedUsername[0] = true;
                updateUsernameAvailability(usernameField, usernameStatus);
            }
        });

        Runnable suggestUsername = () -> {
            if (manuallyEditedUsername[0]) return;
            Staff staff = (Staff) staffBox.getSelectedItem();
            String suggestion = staff == null ? "" : suggestUsername(staff.getName());
            updatingSuggestion[0] = true;
            usernameField.setText(suggestion);
            updatingSuggestion[0] = false;
            updateUsernameAvailability(usernameField, usernameStatus);
        };
        staffBox.addActionListener(event -> {
            manuallyEditedUsername[0] = false;
            suggestUsername.run();
        });
        suggestUsername.run();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        row = addEmployeeFormField(form, row, "Staff record:", staffBox);
        row = addEmployeeFormField(form, row,
                "Username (suggested, editable):", usernameField);
        row = addEmployeeFormField(form, row, "Username availability:", usernameStatus);
        row = addEmployeeFormField(form, row,
                "Temporary password (change at first sign-in):", passwordRow);
        JLabel passwordHint = new JLabel(
                "<html>Share this one-time password with the employee securely. "
                        + "They must set a new password before accessing the app.</html>");
        Theme.styleLabel(passwordHint);
        row = addEmployeeFormField(form, row, "Password instructions:", passwordHint);
        GridBagConstraints messageConstraints = new GridBagConstraints();
        messageConstraints.gridx = 0;
        messageConstraints.gridy = row;
        messageConstraints.weightx = 1;
        messageConstraints.fill = GridBagConstraints.HORIZONTAL;
        messageConstraints.anchor = GridBagConstraints.WEST;
        messageConstraints.insets = new Insets(6, 0, 4, 0);
        form.add(validationMessage, messageConstraints);

        JLabel heading = new JLabel("Create employee account");
        heading.setFont(Theme.HEADER_FONT);
        heading.setForeground(Theme.OCEAN_DEEP);
        JPanel formContent = new JPanel(new BorderLayout(0, 14));
        formContent.setOpaque(false);
        formContent.add(heading, BorderLayout.NORTH);
        formContent.add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        JButton cancelButton = new JButton("Cancel");
        JButton createButton = new JButton("Create Account");
        Theme.styleSecondaryButton(cancelButton);
        Theme.styleButton(createButton);
        buttons.add(cancelButton);
        buttons.add(createButton);
        cancelButton.addActionListener(event -> closeEmployeeForm());
        createButton.addActionListener(event -> {
            if (savingChanges) {
                return;
            }
            Staff selectedStaff = (Staff) staffBox.getSelectedItem();
            if (selectedStaff == null) {
                setWrappedMessage(validationMessage, "Select a staff record.");
                staffBox.requestFocusInWindow();
                return;
            }
            if (!usernameField.getText().trim().matches("(?i)[a-z0-9._-]{3,40}")) {
                setWrappedMessage(validationMessage,
                        "Username must be 3-40 letters, numbers, dots, underscores, or hyphens.");
                usernameField.requestFocusInWindow();
                usernameField.selectAll();
                return;
            }
            try {
                String username = usernameField.getText().trim();
                char[] temporaryPassword = passwordField.getPassword();
                try {
                        dataManager.addEmployeeAccount(username, temporaryPassword,
                                selectedStaff.getStaffId());
                } finally {
                        java.util.Arrays.fill(temporaryPassword, '\0');
                }
                saveChanges(() -> {
                        closeEmployeeForm();
                        refreshAccounts();
                        accountList.setSelectedValue(
                                findAccountByUsername(username), true);
                }, () -> {
                    dataManager.deactivateEmployeeAccount(username);
                    refreshAccounts();
                });
            } catch (IllegalArgumentException ex) {
                setWrappedMessage(validationMessage, ex.getMessage());
                usernameField.requestFocusInWindow();
                usernameField.selectAll();
            }
        });

        JScrollPane formScrollPane = new JScrollPane(formContent);
        formScrollPane.setBorder(BorderFactory.createEmptyBorder());
        formScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setOpaque(false);
        formScrollPane.getViewport().setOpaque(false);
        formScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        detailsPanel.add(formScrollPane, BorderLayout.CENTER);
        detailsPanel.add(buttons, BorderLayout.SOUTH);
        detailsPanel.revalidate();
        detailsPanel.repaint();
        workspace.revalidate();
        workspace.repaint();
        staffBox.requestFocusInWindow();
    }

    private String createTemporaryPassword() {
        byte[] randomBytes = new byte[18];
        PASSWORD_RANDOM.nextBytes(randomBytes);
        StringBuilder password = new StringBuilder(randomBytes.length);
        for (byte randomByte : randomBytes) {
            password.append(TEMP_PASSWORD_CHARACTERS.charAt(
                    (randomByte & 0xff) % TEMP_PASSWORD_CHARACTERS.length()));
        }
        return password.toString();
    }

    private int addEmployeeFormField(JPanel form, int row, String labelText,
                                    JComponent field) {
        JLabel label = new JLabel(labelText);
        Theme.styleLabel(label);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 0, 2, 0);
        form.add(label, constraints);
        constraints.gridy = row++;
        constraints.insets = new Insets(0, 0, 5, 0);
        form.add(field, constraints);
        return row;
    }

    private void setWrappedMessage(JLabel label, String message) {
        String escaped = message == null ? "Unknown validation error."
                : message.replace("&", "&amp;").replace("<", "&lt;")
                        .replace(">", "&gt;");
        label.setText("<html><body style='width:240px'>" + escaped
                + "</body></html>");
    }

    private Account findAccountByUsername(String username) {
        for (Account account : DataManager.getInstance().getAllAccounts()) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return account;
            }
        }
        return null;
    }

    private void closeEmployeeForm() {
        creatingEmployee = false;
        accountList.setEnabled(true);
        addButton.setEnabled(true);
        workspace.removeAll();
        accountWorkspace.add(detailsPanel);
        workspace.add(accountWorkspace, BorderLayout.CENTER);
        updateAccountActions();
        workspace.revalidate();
        workspace.repaint();
    }

    private Staff getLinkedStaff(Account account) {
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

    private void linkStaffRecord(Account account) {
        if (savingChanges) {
            return;
        }
        List<Staff> availableStaff = DataManager.getInstance().getUnlinkedStaff();
        if (availableStaff.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No unlinked staff records are available. Add the staff member "
                            + "from Staff & Tasks first.",
                    "No Staff Records", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Staff selectedStaff = (Staff) JOptionPane.showInputDialog(this,
                "Select the staff member for " + account.getUsername() + ":",
                "Link Staff Record", JOptionPane.QUESTION_MESSAGE, null,
                availableStaff.toArray(new Staff[0]), availableStaff.get(0));
        if (selectedStaff == null) {
            return;
        }
        String previousStaffId = account.getStaffId();
        try {
            DataManager dataManager = DataManager.getInstance();
            dataManager.linkEmployeeAccount(
                    account.getUsername(), selectedStaff.getStaffId());
            saveChanges(this::refreshAccounts, () -> {
                account.setStaffId(previousStaffId);
                refreshAccounts();
            });
        } catch (IllegalArgumentException ex) {
            showValidationError(ex);
        }
    }

    private void updateUsernameAvailability(JTextField usernameField, JLabel statusLabel) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            setWrappedMessage(statusLabel, "Enter a username.");
            statusLabel.setForeground(Theme.ERROR);
        } else if (!username.matches("(?i)[a-z0-9._-]{3,40}")) {
            setWrappedMessage(statusLabel,
                    "Use 3-40 letters, numbers, dots, underscores, or hyphens.");
            statusLabel.setForeground(Theme.ERROR);
        } else if (DataManager.getInstance().hasAccount(username)) {
            setWrappedMessage(statusLabel,
                    "Already taken — edit it to choose another.");
            statusLabel.setForeground(Theme.ERROR);
        } else {
            setWrappedMessage(statusLabel, "Available");
            statusLabel.setForeground(Theme.PALM);
        }
    }

    private String suggestUsername(String fullName) {
        String[] nameParts = fullName == null ? new String[0]
                : fullName.trim().split("[\\s._-]+");
        if (nameParts.length == 0) {
            return "";
        }
        String first = usernamePart(nameParts[0]);
        String last = nameParts.length > 1
                ? usernamePart(nameParts[nameParts.length - 1]) : "";
        if (first.isEmpty()) {
            return "";
        }
        String base = last.isEmpty() ? first : first + "." + last;
        String candidate = base;
        int suffix = 2;
        while (DataManager.getInstance().hasAccount(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String usernamePart(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private void changePassword() {
        if (savingChanges) {
            return;
        }
        Account selected = accountList.getSelectedValue();
        if (selected == null) {
            return;
        }

        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        Theme.styleInput(passwordField);
        Theme.styleInput(confirmPasswordField);
        Object[] form = {
            "New password (at least 8 characters):", passwordField,
            "Confirm new password:", confirmPasswordField
        };
        if (JOptionPane.showConfirmDialog(this, form, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        boolean passwordChanged = false;
        char[] passwordToSave = null;
        try {
            char[] password = passwordField.getPassword();
            char[] confirmation = confirmPasswordField.getPassword();
            try {
                if (!java.util.Arrays.equals(password, confirmation)) {
                    JOptionPane.showMessageDialog(this, "Passwords do not match.",
                            "Invalid Password", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                passwordToSave = password.clone();
                passwordChanged = true;
            } finally {
                java.util.Arrays.fill(password, '\0');
                java.util.Arrays.fill(confirmation, '\0');
            }
        } catch (IllegalArgumentException ex) {
            showValidationError(ex);
        }
        if (passwordChanged) {
            passwordField.setText("");
            confirmPasswordField.setText("");
            char[] passwordForSave = passwordToSave;
            saveChanges(() -> {
                try {
                    DataManager.getInstance().changeAccountPasswordAndSave(
                            selected.getUsername(), passwordForSave);
                } finally {
                    java.util.Arrays.fill(passwordForSave, '\0');
                }
            }, this::refreshAccounts, this::refreshAccounts);
        }
    }

    private void promoteEmployee(Account account) {
        if (savingChanges) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Promote \"" + account.getUsername()
                        + "\" to administrator? They will gain full account and resort management access.",
                "Promote to Administrator", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DataManager dataManager = DataManager.getInstance();
            dataManager.promoteEmployeeAccount(account.getUsername());
            saveChanges(this::refreshAccounts, () -> {
                dataManager.demoteAdminAccount(account.getUsername());
                refreshAccounts();
            });
        } catch (IllegalArgumentException ex) {
            showValidationError(ex);
        }
    }

    private void demoteAdmin(Account account) {
        if (savingChanges) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Demote \"" + account.getUsername()
                        + "\" to employee? They will lose administrator access.",
                "Demote Administrator", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DataManager dataManager = DataManager.getInstance();
            dataManager.demoteAdminAccount(account.getUsername());
            saveChanges(this::refreshAccounts, () -> {
                dataManager.promoteEmployeeAccount(account.getUsername());
                refreshAccounts();
            });
        } catch (IllegalArgumentException ex) {
            showValidationError(ex);
        }
    }

    private void deactivateEmployee() {
        if (savingChanges) {
            return;
        }
        Account selected = accountList.getSelectedValue();
        if (selected == null || selected.isAdmin()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Deactivate \"" + selected.getUsername() + "\"? They will no longer be able to sign in.",
                "Deactivate Account", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DataManager dataManager = DataManager.getInstance();
            dataManager.deactivateEmployeeAccount(selected.getUsername());
            saveChanges(this::refreshAccounts, () -> {
                dataManager.addAccount(selected);
                refreshAccounts();
            });
        } catch (IllegalArgumentException ex) {
            showValidationError(ex);
        }
    }

    private void saveChanges(Runnable onSuccess, Runnable onFailure) {
        saveChanges(() -> DataManager.getInstance().saveData(), onSuccess, onFailure);
    }

    private void saveChanges(SaveOperation operation, Runnable onSuccess,
                            Runnable onFailure) {
        if (savingChanges) {
            return;
        }
        savingChanges = true;
        boolean wasAccountListEnabled = accountList.isEnabled();
        boolean wasAddButtonEnabled = addButton.isEnabled();
        Cursor previousCursor = getCursor();
        accountList.setEnabled(false);
        addButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                operation.run();
                return null;
            }

            @Override
            protected void done() {
                Exception saveFailure = null;
                try {
                    get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    saveFailure = new IOException("Saving account changes was interrupted.", ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    saveFailure = cause instanceof Exception
                            ? (Exception) cause
                            : new IOException("Could not save account changes.", cause);
                } finally {
                    savingChanges = false;
                    accountList.setEnabled(wasAccountListEnabled);
                    addButton.setEnabled(wasAddButtonEnabled);
                    setCursor(previousCursor);
                }

                if (saveFailure == null) {
                    onSuccess.run();
                } else {
                    onFailure.run();
                    if (saveFailure instanceof IllegalArgumentException) {
                        showValidationError((IllegalArgumentException) saveFailure);
                    } else {
                        JOptionPane.showMessageDialog(AccountManagementScreen.this,
                                "The account change could not be saved:\n"
                                        + saveFailure.getMessage(),
                                "Save Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    @FunctionalInterface
    private interface SaveOperation {
        void run() throws IOException;
    }

    private void showValidationError(IllegalArgumentException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Invalid Account", JOptionPane.WARNING_MESSAGE);
    }

    private static class AccountCellRenderer extends JPanel implements ListCellRenderer<Account> {
        private final Avatar avatar = new Avatar("", 42);
        private final JLabel username = new JLabel();
        private final JLabel role = new JLabel();

        AccountCellRenderer() {
            setLayout(new BorderLayout(12, 0));
            setBorder(new EmptyBorder(8, 10, 8, 12));
            setOpaque(true);

            JPanel labels = new JPanel();
            labels.setOpaque(false);
            labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
            labels.add(username);
            labels.add(Box.createVerticalStrut(3));
            labels.add(role);
            add(avatar, BorderLayout.WEST);
            add(labels, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Account> list, Account account,
                int index, boolean isSelected, boolean cellHasFocus) {
            boolean admin = account.isAdmin();
            setBackground(isSelected ? new Color(0xD9, 0xEE, 0xE9) : Theme.INPUT_CREAM);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5, 0xD9, 0xBE)),
                    new EmptyBorder(8, 10, 8, 12)));

            avatar.setUsername(account.getUsername());
            username.setText(account.getUsername());
            username.setFont(Theme.LABEL_FONT.deriveFont(Font.BOLD, 16f));
            username.setForeground(Theme.TEXT_DARK);
            role.setText(admin ? "Administrator" : "Employee");
            role.setFont(Theme.LABEL_FONT.deriveFont(13f));
            role.setForeground(Theme.OCEAN_DEEP);
            return this;
        }
    }

    private static class Avatar extends JPanel {
        private String initials;

        Avatar(String username, int size) {
            setUsername(username);
            setOpaque(false);
            Dimension avatarSize = new Dimension(size, size);
            setMinimumSize(avatarSize);
            setPreferredSize(avatarSize);
            setMaximumSize(avatarSize);
        }

        void setUsername(String username) {
            String cleanName = username == null ? "" : username.trim();
            initials = cleanName.isEmpty()
                    ? "?"
                    : cleanName.substring(0, 1).toUpperCase();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            g.setColor(Theme.OCEAN_DEEP);
            g.fillOval(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.setFont(Theme.HEADER_FONT);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(initials, (size - metrics.stringWidth(initials)) / 2,
                    (size - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }
}
