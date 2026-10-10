package resort.ui;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import javax.swing.text.JTextComponent;
import javax.swing.border.EmptyBorder;

/**
 * Shared beach-resort visual theme: colors, fonts, and small styling
 * helpers so every screen looks like part of the same app instead of
 * default gray Swing. Nothing here is required by the data model or the
 * OOP requirements — it's purely presentation.
 */
public final class Theme {

    private Theme() { }

    // ---- Palette ----
    public static final Color OCEAN_DEEP   = new Color(0x02, 0x4C, 0x66);
    public static final Color OCEAN_MID    = new Color(0x05, 0x83, 0x9C);
    public static final Color OCEAN_LIGHT  = new Color(0x6B, 0xC9, 0xD9);
    public static final Color SAND         = new Color(0xFC, 0xEF, 0xD1);
    public static final Color SAND_DARK    = new Color(0x9A, 0x78, 0x3E);
    public static final Color SAND_PANEL   = new Color(0xFF, 0xF3, 0xD6);
    public static final Color INPUT_CREAM  = new Color(0xFF, 0xFC, 0xF4);
    public static final Color ERROR        = new Color(0x7D, 0x2F, 0x26);
    public static final Color OCEAN_HOVER  = new Color(0x05, 0x83, 0x9C);
    public static final Color PALM         = new Color(0x16, 0x68, 0x5D);
    public static final Color TEXT_DARK    = new Color(0x16, 0x2E, 0x36);

    // ---- Fonts ----
    public static final Font TITLE_FONT  = new Font("SansSerif", Font.BOLD, 26);
    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 19);
    public static final Font LABEL_FONT  = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 16);

    static {
        UIManager.put("Button.font", BUTTON_FONT);
        UIManager.put("Label.font", LABEL_FONT);
        UIManager.put("List.font", LABEL_FONT);
        UIManager.put("TextField.font", LABEL_FONT);
        UIManager.put("PasswordField.font", LABEL_FONT);
        UIManager.put("ComboBox.font", LABEL_FONT);
        UIManager.put("Spinner.font", LABEL_FONT);
        UIManager.put("OptionPane.messageFont", LABEL_FONT);
        UIManager.put("OptionPane.buttonFont", BUTTON_FONT);
        UIManager.put("OptionPane.background", new Color(0xE8, 0xF3, 0xEE));
        UIManager.put("OptionPaneUI", GradientOptionPaneUI.class.getName());
    }

    /** Applies the standard deep-ocean button look used across the app. */
    public static void styleButton(AbstractButton button) {
        button.setBackground(OCEAN_DEEP);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setRolloverEnabled(true);
        button.setUI(OceanButtonUI.createUI(button));
    }

    /** Uses the shared ocean palette for secondary actions as well. */
    public static void styleSecondaryButton(AbstractButton button) {
        styleButton(button);
    }

    public static void styleTitleLabel(JLabel label) {
        label.setFont(TITLE_FONT);
        label.setForeground(OCEAN_DEEP);
    }

    public static void styleLabel(JLabel label) {
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_DARK);
    }

    public static void styleInput(JComponent input) {
        input.setFont(LABEL_FONT);
        input.setForeground(TEXT_DARK);
        input.setBackground(INPUT_CREAM);
        if (input instanceof JTextComponent) {
            JTextComponent textInput = (JTextComponent) input;
            textInput.setCaretColor(TEXT_DARK);
            textInput.setBorder(inputBorder(SAND_DARK, 1));
            textInput.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    textInput.setBorder(inputBorder(OCEAN_MID, 2));
                }

                @Override
                public void focusLost(FocusEvent event) {
                    textInput.setBorder(inputBorder(SAND_DARK, 1));
                }
            });
        } else if (input instanceof JSpinner) {
            JComponent editor = ((JSpinner) input).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                styleInput(((JSpinner.DefaultEditor) editor).getTextField());
            }
        }
    }

    /** A warm sand-colored card with dark text for readable contrast. */
    public static JPanel card() {
        JPanel card = new GradientCardPanel();
        card.setBorder(new EmptyBorder(19, 21, 19, 21));
        return card;
    }

    private static final class GradientCardPanel extends JPanel {
        private static final Color CARD_BORDER = new Color(0xE1, 0xD5, 0xBA);

        GradientCardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0xE7, 0xF2, 0xEA),
                    0, getHeight(), SAND_PANEL));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g.setColor(CARD_BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g.dispose();
        }
    }

    private static javax.swing.border.Border inputBorder(Color color, int thickness) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, thickness),
                new EmptyBorder(5, 8, 5, 8));
    }

    public static final class GradientOptionPaneUI extends BasicOptionPaneUI {
        private Component previousFocusOwner;
        private PropertyChangeListener focusRestoreListener;

        public static ComponentUI createUI(JComponent component) {
            return new GradientOptionPaneUI();
        }

        @Override
        public void installUI(JComponent component) {
            previousFocusOwner = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusOwner();
            super.installUI(component);
            focusRestoreListener = event -> {
                if (JOptionPane.VALUE_PROPERTY.equals(event.getPropertyName())
                        && event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                    SwingUtilities.invokeLater(() -> {
                        Component focusOwner = previousFocusOwner;
                        if (focusOwner != null && focusOwner.isShowing()
                                && focusOwner.isEnabled()) {
                            focusOwner.requestFocusInWindow();
                        }
                    });
                }
            };
            optionPane.addPropertyChangeListener(focusRestoreListener);
        }

        @Override
        public void uninstallUI(JComponent component) {
            if (optionPane != null && focusRestoreListener != null) {
                optionPane.removePropertyChangeListener(focusRestoreListener);
            }
            focusRestoreListener = null;
            previousFocusOwner = null;
            super.uninstallUI(component);
        }

        @Override
        protected void installComponents() {
            super.installComponents();
            optionPane.setOpaque(false);
            optionPane.setBackground(new Color(0xE8, 0xF3, 0xEE));
            optionPane.setBorder(new OptionPaneGradientBorder());
            for (Component child : optionPane.getComponents()) {
                makeOptionPaneContentTransparent(child);
            }
        }

        private void makeOptionPaneContentTransparent(Component component) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                panel.setOpaque(false);
                for (Component child : panel.getComponents()) {
                    makeOptionPaneContentTransparent(child);
                }
            } else if (component instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) component;
                scrollPane.setOpaque(false);
                if (!(scrollPane.getViewport().getView() instanceof JTextComponent)) {
                    scrollPane.getViewport().setOpaque(false);
                }
            } else if (component instanceof JButton) {
                styleButton((JButton) component);
            }
        }
    }

    private static final class OptionPaneGradientBorder implements Border {
        private static final Insets INSETS = new Insets(14, 16, 14, 16);

        @Override
        public void paintBorder(Component component, Graphics graphics,
                                int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(x, y, new Color(0xE5, 0xF2, 0xEE),
                    x + width, y + height, new Color(0xF0, 0xF5, 0xEE)));
            g.fillRoundRect(x, y, width, height, 16, 16);
            g.setColor(new Color(0xC7, 0xDD, 0xD5));
            g.drawRoundRect(x, y, width - 1, height - 1, 16, 16);
            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return (Insets) INSETS.clone();
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    public static void styleList(JList<?> list) {
        list.setFont(LABEL_FONT);
        list.setBackground(INPUT_CREAM);
        list.setForeground(TEXT_DARK);
        list.setSelectionBackground(OCEAN_DEEP);
        list.setSelectionForeground(Color.WHITE);
        list.setFixedCellHeight(32);
    }

    public static void styleListItemSeparator(JLabel item) {
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5, 0xD9, 0xBE)),
                new EmptyBorder(4, 8, 4, 8)));
    }

    /**
     * Window size as a fraction of the current screen's resolution,
     * instead of a fixed pixel count — so the app opens at a sensible
     * size whether it's running on a small laptop or a large external
     * monitor, rather than looking tiny or clipping content. Both
     * fractions are expected in the 0.0–1.0 range (e.g. 0.7 = 70%).
     */
    public static Dimension screenSize(double widthFraction, double heightFraction) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension(
                (int) (screen.width * widthFraction),
                (int) (screen.height * heightFraction));
    }
}
