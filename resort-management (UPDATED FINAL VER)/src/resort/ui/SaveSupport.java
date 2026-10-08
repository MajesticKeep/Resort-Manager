package resort.ui;

import java.awt.Component;
import resort.model.DataManager;

final class SaveSupport {
    private SaveSupport() {
    }

    static void save(Component parent, String failureContext, Runnable onSuccess) {
        DataManager.getInstance().saveDataAsync(onSuccess, error ->
                javax.swing.JOptionPane.showMessageDialog(parent,
                        failureContext + ":\n" + error.getMessage(),
                        "Save Failed", javax.swing.JOptionPane.ERROR_MESSAGE));
    }
}
