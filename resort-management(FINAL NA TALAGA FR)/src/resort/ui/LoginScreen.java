package resort.ui;

import resort.model.Account;
import resort.model.DataManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.QuadCurve2D;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginScreen extends JFrame {

    private JTextField usernameField;
    private PlaceholderPasswordField passwordField;
    private JPanel loginContent;

    public LoginScreen() {
        setTitle("Resort Management System - Login");
        setSize(Theme.screenSize(0.80, 0.78));
        setMinimumSize(new Dimension(720, 460));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                exitApplication();
            }
        });

        JPanel background = new BeachFramePanel();
        background.setLayout(new GridBagLayout());

        JPanel content = new JPanel(new GridLayout(1, 2, 0, 0));
        content.setPreferredSize(new Dimension(900, 500));
        content.setBorder(BorderFactory.createLineBorder(Theme.SAND_DARK, 1));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.INPUT_CREAM);
        form.setBorder(BorderFactory.createEmptyBorder(48, 52, 48, 52));

        usernameField = new JTextField();
        passwordField = new PlaceholderPasswordField();
        Theme.styleInput(usernameField);
        Theme.styleInput(passwordField);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JLabel subtitle = new JLabel("SIGN IN");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        subtitle.setForeground(Theme.TEXT_DARK);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel instructions = new JLabel(
                "<html>Log in to manage resort operations.</html>");
        Theme.styleLabel(instructions);
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usernameLabel = new JLabel("Username");
        Theme.styleLabel(usernameLabel);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passwordLabel = new JLabel("Password");
        Theme.styleLabel(passwordLabel);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loginBtn = new JButton("Log In");
        Theme.styleButton(loginBtn);
        JButton exitBtn = new JButton("Exit Application");
        Theme.styleSecondaryButton(exitBtn);
        JPanel loginButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        loginButtonRow.setOpaque(false);
        loginButtonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButtonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                loginButtonRow.getPreferredSize().height));
        loginButtonRow.add(loginBtn);
        loginButtonRow.add(Box.createHorizontalStrut(12));
        loginButtonRow.add(exitBtn);
        getRootPane().setDefaultButton(loginBtn);

        Runnable submitLogin = () -> {
            loginBtn.setEnabled(false);
            exitBtn.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
            loginBtn.setText("Signing In...");

            String username = usernameField.getText().trim();
            char[] password = passwordField.getPassword();

            new SwingWorker<Account, Void>() {
                @Override
                protected Account doInBackground() {
                    return DataManager.getInstance().login(username, password);
                }

                @Override
                protected void done() {
                    try {
                        Account account = get();
                        if (account == null) {
                            passwordField.setText("");
                            passwordField.showError(
                                    "Incorrect username or password. Try again.");
                            passwordField.requestFocusInWindow();
                            return;
                        }

                        if (account.isPasswordChangeRequired()) {
                            showFirstLoginPasswordChange(account);
                            return;
                        }

                        JOptionPane.showMessageDialog(LoginScreen.this,
                                "Log-in successful.",
                                "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                        MainMenu menu = new MainMenu(account);
                        menu.setVisible(true);
                        dispose();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        showLoginFailure("Login was interrupted. Please try again.");
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        String message = cause == null ? ex.getMessage() : cause.getMessage();
                        showLoginFailure("Could not verify the account:\n" + message);
                    } finally {
                        if (isDisplayable()) {
                            loginBtn.setEnabled(true);
                            exitBtn.setEnabled(true);
                            usernameField.setEnabled(true);
                            passwordField.setEnabled(true);
                            loginBtn.setText("Log In");
                        }
                        java.util.Arrays.fill(password, '\0');
                    }
                }
            }.execute();
        };
        loginBtn.addActionListener(e -> submitLogin.run());
        exitBtn.addActionListener(e -> exitApplication());
        passwordField.addActionListener(e -> loginBtn.doClick());

        form.add(Box.createVerticalGlue());
        form.add(subtitle);
        form.add(Box.createVerticalStrut(12));
        form.add(instructions);
        form.add(Box.createVerticalStrut(34));
        form.add(usernameLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(20));
        form.add(passwordLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(28));
        form.add(loginButtonRow);
        form.add(Box.createVerticalStrut(16));
        form.add(Box.createVerticalGlue());

        content.add(createWelcomePanel());
        content.add(form);
        background.add(content);
        loginContent = background;
        add(loginContent);
    }

    private void showLoginFailure(String message) {
        JOptionPane.showMessageDialog(this, message,
                "Login Failed", JOptionPane.ERROR_MESSAGE);
    }

    private void showFirstLoginPasswordChange(Account account) {
        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        Theme.styleInput(newPasswordField);
        Theme.styleInput(confirmPasswordField);
        char newPasswordEcho = newPasswordField.getEchoChar();
        char confirmPasswordEcho = confirmPasswordField.getEchoChar();

        JCheckBox revealPasswords = new JCheckBox("Show passwords");
        revealPasswords.setOpaque(false);
        revealPasswords.addActionListener(event -> {
            char echo = revealPasswords.isSelected() ? (char) 0 : newPasswordEcho;
            newPasswordField.setEchoChar(echo);
            confirmPasswordField.setEchoChar(revealPasswords.isSelected()
                    ? (char) 0 : confirmPasswordEcho);
        });

        JLabel message = new JLabel("<html>You must set a new password before "
                + "continuing. Use at least 8 characters.</html>");
        Theme.styleLabel(message);
        JLabel validation = new JLabel(" ");
        validation.setForeground(Theme.ERROR);
        validation.setPreferredSize(new Dimension(360, 42));

        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 7));
        fields.setOpaque(false);
        fields.add(new JLabel("New password:"));
        fields.add(newPasswordField);
        fields.add(new JLabel("Confirm new password:"));
        fields.add(confirmPasswordField);
        fields.add(revealPasswords);
        fields.add(validation);

        JLabel heading = new JLabel("Set your password");
        heading.setFont(Theme.HEADER_FONT);
        heading.setForeground(Theme.OCEAN_DEEP);
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(heading, BorderLayout.NORTH);
        card.add(message, BorderLayout.CENTER);
        card.add(fields, BorderLayout.SOUTH);

        JButton changeButton = new JButton("Save New Password");
        JButton signOutButton = new JButton("Sign Out");
        Theme.styleButton(changeButton);
        Theme.styleSecondaryButton(signOutButton);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(signOutButton);
        actions.add(changeButton);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));
        content.add(card, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        BeachPanel background = new BeachPanel(new GridBagLayout());
        background.add(content);

        AtomicBoolean passwordUpdated = new AtomicBoolean(false);
        changeButton.addActionListener(event -> {
            if (!passwordUpdated.get()) {
                char[] password = newPasswordField.getPassword();
                char[] confirmation = confirmPasswordField.getPassword();
                if (password.length < 8) {
                    java.util.Arrays.fill(password, '\0');
                    java.util.Arrays.fill(confirmation, '\0');
                    validation.setText("<html>Password must be at least 8 characters.</html>");
                    newPasswordField.requestFocusInWindow();
                    newPasswordField.selectAll();
                    return;
                }
                if (!java.util.Arrays.equals(password, confirmation)) {
                    validation.setText("<html>Passwords do not match.</html>");
                    confirmPasswordField.requestFocusInWindow();
                    confirmPasswordField.selectAll();
                    java.util.Arrays.fill(password, '\0');
                    java.util.Arrays.fill(confirmation, '\0');
                    return;
                }
                java.util.Arrays.fill(password, '\0');
                java.util.Arrays.fill(confirmation, '\0');
            }

            changeButton.setEnabled(false);
            signOutButton.setEnabled(false);
            validation.setText(passwordUpdated.get()
                    ? "Saving password change..." : "Updating password...");
            char[] password = passwordUpdated.get()
                    ? null : newPasswordField.getPassword();

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (!passwordUpdated.get()) {
                        DataManager.getInstance().changeAccountPassword(
                                account.getUsername(), password);
                        passwordUpdated.set(true);
                    }
                    DataManager.getInstance().saveData();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        MainMenu menu = new MainMenu(account);
                        menu.setVisible(true);
                        dispose();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        validation.setText("<html>Password change was interrupted. "
                                + "Please retry.</html>");
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        String error = cause == null ? ex.getMessage() : cause.getMessage();
                        validation.setText("<html>Could not save the password change: "
                                + error + "</html>");
                    } finally {
                        java.util.Arrays.fill(password == null ? new char[0] : password, '\0');
                        if (isDisplayable()) {
                            changeButton.setEnabled(true);
                            signOutButton.setEnabled(true);
                        }
                    }
                }
            }.execute();
        });
        signOutButton.addActionListener(event -> {
            setContentPane(loginContent);
            revalidate();
            repaint();
            usernameField.setText("");
            passwordField.setText("");
            usernameField.requestFocusInWindow();
        });

        setContentPane(background);
        revalidate();
        repaint();
        newPasswordField.requestFocusInWindow();
    }

    private void exitApplication() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit the application?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        setEnabled(false);
        DataManager.getInstance().saveDataAsync(() -> dispose(), error -> {
            setEnabled(true);
            JOptionPane.showMessageDialog(this,
                    "Could not save resort data:\n" + error.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        });
    }

    private static final class PlaceholderPasswordField extends JPasswordField {
        private String errorMessage;

        PlaceholderPasswordField() {
            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    clearError();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    repaint();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    clearError();
                }
            });
        }

        void showError(String message) {
            errorMessage = message;
            repaint();
        }

        private void clearError() {
            if (errorMessage != null) {
                errorMessage = null;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (errorMessage == null || getPassword().length > 0) {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(Theme.ERROR);
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            Insets insets = getInsets();
            int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(errorMessage, insets.left, baseline);
            g.dispose();
        }
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new WelcomeArtPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(44, 48, 44, 32));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logo = new JLabel("\uD83C\uDFD6\uFE0F");
        logo.setFont(new Font("SansSerif", Font.PLAIN, 46));

        JLabel resortName = new JLabel("<html>THE JUANS<br>RESORT</html>");
        resortName.setFont(new Font("SansSerif", Font.BOLD, 25));
        resortName.setForeground(Color.WHITE);
        brand.add(logo);
        brand.add(resortName);

        JLabel welcome = new JLabel("<html>WELCOME<br>TO YOUR<br>RESORT</html>");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 46));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
                "<html>Bookings, rooms, and guest services<br>"
                        + "come together in one place.</html>");
        description.setFont(Theme.LABEL_FONT);
        description.setForeground(Color.WHITE);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalStrut(10));
        panel.add(brand);
        panel.add(Box.createVerticalStrut(42));
        panel.add(welcome);
        panel.add(Box.createVerticalStrut(24));
        panel.add(description);
        return panel;
    }

    private static class WelcomeArtPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int shoreY = (int) (height * 0.76);

            g.setPaint(new GradientPaint(0, 0, new Color(0x08, 0x55, 0x70),
                    0, shoreY, new Color(0x29, 0xA5, 0xB5)));
            g.fillRect(0, 0, width, shoreY);

            int sunSize = Math.min(width / 5, height / 4);
            int sunX = (int) (width * 0.72);
            int sunY = (int) (height * 0.13);
            g.setColor(new Color(0xFF, 0xE8, 0xA6, 210));
            g.fill(new Ellipse2D.Double(sunX, sunY, sunSize, sunSize));

            g.setPaint(new GradientPaint(0, height * 0.48f, new Color(0x08, 0x83, 0x9C),
                    0, shoreY, new Color(0x02, 0x4C, 0x66)));
            g.fillRect(0, (int) (height * 0.48), width, shoreY - (int) (height * 0.48));

            g.setColor(new Color(0xC7, 0xF3, 0xEE, 170));
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 4; i++) {
                double y = height * (0.55 + i * 0.055);
                g.draw(new QuadCurve2D.Double(-20, y, width * 0.42, y - 15,
                        width + 20, y + 3));
            }
            g.setColor(new Color(0xFF, 0xFF, 0xFF, 210));
            g.setStroke(new BasicStroke(3f));
            g.draw(new QuadCurve2D.Double(-20, shoreY, width * 0.46,
                    shoreY - 20, width + 20, shoreY + 2));

            g.setPaint(new GradientPaint(0, shoreY, new Color(0xD7, 0xB5, 0x78),
                    0, height, new Color(0xF4, 0xDE, 0xA8)));
            g.fillRect(0, shoreY, width, height - shoreY);

            g.setColor(new Color(0xFF, 0xF0, 0xC7, 150));
            for (int i = 0; i < 9; i++) {
                int x = (i * 71 + 28) % Math.max(width, 1);
                int y = shoreY + 15 + (i * 19) % Math.max(height - shoreY - 18, 1);
                g.fill(new Ellipse2D.Double(x, y, 3, 3));
            }

            // Palm silhouette at the edge of the artwork leaves the welcome text clear.
            g.setColor(new Color(0x08, 0x45, 0x3F));
            g.setStroke(new BasicStroke(Math.max(5f, width / 75f), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            QuadCurve2D trunk = new QuadCurve2D.Double(width * 0.93, height * 1.05,
                    width * 0.84, height * 0.67, width * 0.89, height * 0.28);
            g.draw(trunk);
            int crownX = (int) (width * 0.89);
            int crownY = (int) (height * 0.28);
            g.setStroke(new BasicStroke(Math.max(3f, width / 100f), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 7; i++) {
                double angle = Math.toRadians(195 + i * 30);
                double length = width * 0.16;
                double controlX = crownX + Math.cos(angle) * length * 0.55;
                double controlY = crownY + Math.sin(angle) * length * 0.28 - height * 0.04;
                double endX = crownX + Math.cos(angle) * length;
                double endY = crownY + Math.sin(angle) * length * 0.46;
                g.draw(new QuadCurve2D.Double(crownX, crownY, controlX, controlY, endX, endY));
            }
            g.dispose();
        }
    }

    private static class BeachFramePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0xB8, 0xDF, 0xD9),
                    width, height, new Color(0xF4, 0xDE, 0xAE)));
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(0xFF, 0xFF, 0xFF, 95));
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 5; i++) {
                double y = height * (0.16 + i * 0.17);
                g.draw(new QuadCurve2D.Double(-30, y, width * 0.28, y - 24,
                        width * 0.53, y));
                g.draw(new QuadCurve2D.Double(width * 0.57, y + 8,
                        width * 0.78, y + 30, width + 30, y + 5));
            }

            g.setColor(new Color(0x02, 0x4C, 0x66, 35));
            for (int i = 0; i < 8; i++) {
                int x = 18 + i * 37;
                g.draw(new Ellipse2D.Double(x, height - 28 - (i % 3) * 8, 8, 5));
            }
            g.dispose();
        }
    }
}
