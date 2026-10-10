package resort.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.QuadCurve2D;
import javax.swing.*;

/**
 * A JPanel that paints a simple beach-resort scene as its background:
 * ocean gradient, a sun, a soft wave line, and sand. Drawn with Graphics2D
 * rather than loading an image file, so the submitted zip doesn't depend
 * on bundling external assets.
 */
public class BeachPanel extends JPanel {

    public BeachPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    public BeachPanel() {
        super();
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int sandTop = (int) (h * 0.70);

        // Sky/ocean gradient
        GradientPaint sky = new GradientPaint(0, 0, Theme.OCEAN_LIGHT, 0, sandTop, Theme.OCEAN_MID);
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, sandTop);

        // Sunrise with soft rays.
        g2.setColor(new Color(255, 231, 166, 170));
        g2.setStroke(new BasicStroke(2f));
        int sunX = w - 80;
        int sunY = 50;
        for (int angle = 0; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            int x1 = sunX + (int) (36 * Math.cos(radians));
            int y1 = sunY + (int) (36 * Math.sin(radians));
            int x2 = sunX + (int) (44 * Math.cos(radians));
            int y2 = sunY + (int) (44 * Math.sin(radians));
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setColor(new Color(255, 235, 184, 235));
        g2.fill(new Ellipse2D.Double(sunX - 25, sunY - 25, 50, 50));

        // A couple of soft wave curves over the ocean
        g2.setColor(new Color(255, 255, 255, 90));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 3; i++) {
            int y = sandTop - 30 - (i * 22);
            QuadCurve2D wave = new QuadCurve2D.Double(
                    -20, y, w / 2.0, y - 14, w + 20, y);
            g2.draw(wave);
        }

        // Sand
        GradientPaint sand = new GradientPaint(0, sandTop, Theme.SAND_DARK, 0, h, Theme.SAND);
        g2.setPaint(sand);
        g2.fillRect(0, sandTop, w, h - sandTop);

        // Shoreline edge
        g2.setColor(new Color(255, 255, 255, 140));
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(0, sandTop, w, sandTop);

        // Small shell accents on the sand keep the backdrop beach-like without
        // placing texture behind the form text.
        g2.setColor(new Color(190, 143, 91, 105));
        for (int i = 0; i < 5; i++) {
            int x = 24 + i * 34;
            int y = sandTop + 26 + (i % 2) * 10;
            g2.draw(new Ellipse2D.Double(x, y, 9, 6));
            g2.drawLine(x + 4, y + 1, x + 4, y + 5);
        }

        drawHiddenDetails(g2, w, h, sandTop);
        g2.dispose();
    }

    private void drawHiddenDetails(Graphics2D g2, int width, int height, int sandTop) {
        if (width < 180 || height < 140) {
            return;
        }

        // A faint little constellation sits far from the dashboard content.
        int starX = Math.max(22, width / 10);
        int starY = Math.max(20, sandTop / 5);
        g2.setColor(new Color(255, 247, 218, 95));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(starX, starY + 5, starX + 9, starY);
        g2.drawLine(starX + 9, starY, starX + 17, starY + 7);
        g2.drawLine(starX + 17, starY + 7, starX + 25, starY + 2);
        g2.fill(new Ellipse2D.Double(starX - 1, starY + 4, 3, 3));
        g2.fill(new Ellipse2D.Double(starX + 8, starY - 1, 3, 3));
        g2.fill(new Ellipse2D.Double(starX + 16, starY + 6, 3, 3));
        g2.fill(new Ellipse2D.Double(starX + 24, starY + 1, 3, 3));

        // A tiny sailboat rests just above the shoreline.
        int boatX = Math.max(34, width / 8);
        int boatY = sandTop - Math.max(24, height / 18);
        g2.setColor(new Color(255, 248, 226, 90));
        g2.drawLine(boatX + 8, boatY - 14, boatX + 8, boatY + 1);
        Polygon sail = new Polygon(
                new int[] {boatX + 7, boatX + 7, boatX - 1},
                new int[] {boatY - 12, boatY - 2, boatY - 2}, 3);
        g2.fill(sail);
        g2.drawArc(boatX, boatY - 1, 19, 7, 190, 160);

        // A nearly buried treasure marker hides near the far end of the sand.
        int markerX = width - 31;
        int markerY = sandTop + Math.max(18, (height - sandTop) / 2);
        g2.setColor(new Color(119, 83, 43, 90));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(markerX - 3, markerY - 3, markerX + 3, markerY + 3);
        g2.drawLine(markerX - 3, markerY + 3, markerX + 3, markerY - 3);
        g2.draw(new Ellipse2D.Double(markerX - 10, markerY + 5, 2, 2));
        g2.draw(new Ellipse2D.Double(markerX - 15, markerY + 8, 2, 2));
    }
}
