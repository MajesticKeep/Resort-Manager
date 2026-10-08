package resort;

import java.awt.GridLayout;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JTextField;

import resort.model.Admin;
import resort.model.DataManager;
import resort.model.ResortConfig;
import resort.ui.LoginScreen;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startApplication);
    }

    private static void startApplication() {
        SwingWorker<Void, Void> loadWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                ResortConfig.initialize();
                DataManager.getInstance().loadData();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (DataManager.getInstance().getAllAccounts().isEmpty()) {
                        createInitialAdministrator();
                    } else {
                        showLoginScreen();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(null,
                            "Startup was interrupted. Please restart the application.",
                            "Startup Failed", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String message = cause == null ? ex.getMessage() : cause.getMessage();
                    JOptionPane.showMessageDialog(null,
                            "The application could not load its data:\n" + message,
                            "Startup Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        loadWorker.execute();
     }

    private static void createInitialAdministrator() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmationField = new JPasswordField();
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 6));
        fields.add(new JLabel("Create the first administrator account."));
        fields.add(new JLabel("Username"));
        fields.add(usernameField);
        fields.add(new JLabel("Password (at least 8 characters)"));
        fields.add(passwordField);
        fields.add(new JLabel("Confirm password"));
        fields.add(confirmationField);

        int choice = JOptionPane.showConfirmDialog(null, fields,
                "Initial Administrator Setup", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        char[] password = passwordField.getPassword();
        char[] confirmation = confirmationField.getPassword();
        if (!java.util.Arrays.equals(password, confirmation)) {
            JOptionPane.showMessageDialog(null, "The passwords do not match. Please try again.",
                    "Setup Error", JOptionPane.ERROR_MESSAGE);
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
            SwingUtilities.invokeLater(Main::createInitialAdministrator);
            return;
        }
        java.util.Arrays.fill(confirmation, '\0');

        try {
            Admin administrator;
            try {
                administrator = new Admin(usernameField.getText(), password);
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
            DataManager dataManager = DataManager.getInstance();
            dataManager.addAccount(administrator);
            dataManager.saveDataAsync(Main::showLoginScreen, error ->
                    JOptionPane.showMessageDialog(null,
                            "Could not save the administrator account:\n"
                                    + error.getMessage(),
                            "Setup Failed", JOptionPane.ERROR_MESSAGE));
        } catch (IllegalArgumentException ex) {
            java.util.Arrays.fill(password, '\0');
            JOptionPane.showMessageDialog(null, ex.getMessage(),
                    "Setup Error", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(Main::createInitialAdministrator);
        }
    }

    private static void showLoginScreen() {
        LoginScreen login = new LoginScreen();
        login.setVisible(true);
    }
}
