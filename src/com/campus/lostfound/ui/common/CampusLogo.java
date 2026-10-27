package com.campus.lostfound.ui.common;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * CampusLogo
 *
 * Custom vector-rendered Swing component that creates a professional,
 * campus-themed lost-and-found brand emblem:
 * - A modern graduation cap / academic shield representing "Campus".
 * - A clean magnifying glass / locator pin representing "Search / Lost & Found".
 * - Rendered in deep navy blue and vibrant teal gradients without external image assets.
 */
public class CampusLogo extends JComponent {

    public enum LogoSize {
        SMALL(36, 36),
        MEDIUM(48, 48),
        LARGE(72, 72),
        HERO(96, 96);

        final int width;
        final int height;

        LogoSize(int w, int h) {
            this.width = w;
            this.height = h;
        }
    }

    private final LogoSize size;
    private final boolean lightTheme;

    public CampusLogo() {
        this(LogoSize.MEDIUM, false);
    }

    public CampusLogo(LogoSize size) {
        this(size, false);
    }

    public CampusLogo(LogoSize size, boolean lightTheme) {
        this.size = size;
        this.lightTheme = lightTheme;
        setPreferredSize(new Dimension(size.width, size.height));
        setMinimumSize(new Dimension(size.width, size.height));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();
        int pad = Math.max(2, w / 16);
        int sizeBox = Math.min(w - pad * 2, h - pad * 2);
        int ox = (w - sizeBox) / 2;
        int oy = (h - sizeBox) / 2;

        // Base rounded badge background with subtle gradient
        Color bgStart = lightTheme ? new Color(255, 255, 255, 240) : UITheme.PRIMARY;
        Color bgEnd = lightTheme ? new Color(243, 247, 251, 240) : UITheme.ACCENT;

        LinearGradientPaint bgPaint = new LinearGradientPaint(
                ox, oy, ox + sizeBox, oy + sizeBox,
                new float[]{0.0f, 1.0f},
                new Color[]{bgStart, bgEnd}
        );

        int arc = sizeBox / 3;
        g2.setPaint(bgPaint);
        g2.fill(new RoundRectangle2D.Double(ox, oy, sizeBox, sizeBox, arc, arc));

        // Subtle border
        g2.setColor(lightTheme ? UITheme.BORDER : new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(Math.max(1.0f, sizeBox / 32f)));
        g2.draw(new RoundRectangle2D.Double(ox, oy, sizeBox, sizeBox, arc, arc));

        // Draw Emblem: Combined Academic Diamond + Search Loop
        float strokeWidth = Math.max(2.0f, sizeBox / 16.0f);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color iconColor = lightTheme ? UITheme.PRIMARY : Color.WHITE;
        Color accentColor = lightTheme ? UITheme.ACCENT : UITheme.ACCENT_LIGHT;

        // 1. Academic Diamond (Grad cap / campus diamond top)
        int cx = ox + sizeBox / 2;
        int topY = oy + (int)(sizeBox * 0.22);
        int midY = oy + (int)(sizeBox * 0.38);
        int leftX = ox + (int)(sizeBox * 0.22);
        int rightX = ox + (int)(sizeBox * 0.78);
        int bottomCapY = oy + (int)(sizeBox * 0.52);

        int[] xPoly = {cx, rightX, cx, leftX};
        int[] yPoly = {topY, midY, bottomCapY, midY};
        g2.setColor(iconColor);
        g2.drawPolygon(xPoly, yPoly, 4);

        // Cap center pip
        g2.fillOval(cx - (int)(strokeWidth), midY - (int)(strokeWidth), (int)(strokeWidth * 2), (int)(strokeWidth * 2));

        // Tassel strand
        g2.setColor(accentColor);
        g2.drawLine(cx, midY, rightX - (int)(sizeBox * 0.08), midY + (int)(sizeBox * 0.18));

        // 2. Magnifying glass / Search locator in lower section
        int glassRadius = (int)(sizeBox * 0.20);
        int glassCenterX = cx - (int)(sizeBox * 0.06);
        int glassCenterY = oy + (int)(sizeBox * 0.62);

        g2.setColor(accentColor);
        g2.drawOval(glassCenterX - glassRadius, glassCenterY - glassRadius, glassRadius * 2, glassRadius * 2);

        // Handle
        int handleStartX = glassCenterX + (int)(glassRadius * 0.72);
        int handleStartY = glassCenterY + (int)(glassRadius * 0.72);
        int handleEndX = handleStartX + (int)(sizeBox * 0.16);
        int handleEndY = handleStartY + (int)(sizeBox * 0.16);
        g2.setColor(iconColor);
        g2.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);

        g2.dispose();
    }
}
