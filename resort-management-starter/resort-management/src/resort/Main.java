package resort;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import resort.model.Admin;
import resort.model.DataManager;
import resort.model.Employee;
import resort.ui.LoginScreen;

public class Main {
    public static void main(String[] args) {
        DataManager.getInstance().loadData();

        // Seed a default Admin account only if none exist yet — otherwise
        // there's no way to log in on a fresh install.
        if (DataManager.getInstance().login("admin", "admin123") == null) {
            DataManager.getInstance().addAccount(new Admin("admin", "admin123"));
        }
        if (DataManager.getInstance().login("employee", "employee123") == null) {
            DataManager.getInstance().addAccount(new Employee("employee", "employee123"));
        }

        SwingUtilities.invokeLater(() -> {
            LoginScreen login = new LoginScreen();
            login.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    DataManager.getInstance().saveData();
                }
            });
            login.setVisible(true);
        });
    }
}