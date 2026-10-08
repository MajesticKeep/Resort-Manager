package resort.ui;

import resort.model.Account;
import resort.model.DataManager;

import javax.swing.*;
import java.awt.*;

public class LoginScreen extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginScreen() {
        setTitle("Resort Management System - Login");
        setSize(320, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginBtn = new JButton("Log In");
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            Account account = DataManager.getInstance().login(username, password);
            if (account == null) {
                errorLabel.setText("Invalid username or password.");
                return;
            }

            MainMenu menu = new MainMenu(account);
            menu.setVisible(true);
            dispose();
        });

        panel.add(labeled("Username:", usernameField));
        panel.add(Box.createVerticalStrut(8));
        panel.add(labeled("Password:", passwordField));
        panel.add(Box.createVerticalStrut(12));
        panel.add(loginBtn);
        panel.add(errorLabel);

        add(panel);
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return row;
    }
}