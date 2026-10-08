package resort.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public final class OceanButtonUI extends BasicButtonUI {

    private static final OceanButtonUI INSTANCE = new OceanButtonUI();

    public static OceanButtonUI createUI(JComponent component) {
        return INSTANCE;
    }

    @Override
    public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (!button.isEnabled()) {
                g.setColor(Theme.OCEAN_DEEP.darker());
            } else if (button.getModel().isArmed()
                    && button.getModel().isPressed()) {
                g.setColor(Theme.OCEAN_DEEP.darker());
            } else if (button.getModel().isRollover()) {
                g.setColor(Theme.OCEAN_HOVER);
            } else {
                g.setColor(Theme.OCEAN_DEEP);
            }
            g.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 12, 12);
        } finally {
            g.dispose();
        }
        super.paint(graphics, component);
    }
}
