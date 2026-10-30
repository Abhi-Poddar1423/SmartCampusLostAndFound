package com.campus.lostfound.ui.common;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * CampusIcons
 *
 * High-performance, anti-aliased Java 2D vector icons for the Smart Campus desktop UI.
 * Replaces unreliable emoji Unicode characters to prevent broken square glyphs on Windows.
 */
public class CampusIcons {

    public enum IconType {
        DASHBOARD,
        BROWSE,
        REPORT_ITEM,
        MY_REPORTS,
        MY_CLAIMS,
        USERS,
        LOGOUT,
        REFRESH,
        CHECK_CIRCLE,
        LOCATION_PIN,
        EMAIL,
        LOCK,
        NOTICE_BELL,
        CALENDAR,
        EYE,
        EYE_OFF,
        CAMERA
    }

    public static Icon get(IconType type) {
        return get(type, 18, 18, new Color(203, 213, 225));
    }

    public static Icon get(IconType type, Color color) {
        return get(type, 18, 18, color);
    }

    public static Icon get(IconType type, int width, int height, Color color) {
        return new VectorIcon(type, width, height, color);
    }

    private static class VectorIcon implements Icon {
        private final IconType type;
        private final int width;
        private final int height;
        private final Color color;

        VectorIcon(IconType type, int width, int height, Color color) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            Color drawCol = (color != null) ? color : (c != null ? c.getForeground() : Color.BLACK);
            if (c instanceof RoundedButton) {
                RoundedButton rb = (RoundedButton) c;
                if (rb.getButtonType() == RoundedButton.ButtonType.OUTLINE && (rb.isHovered() || rb.isPressed())) {
                    drawCol = UITheme.ACCENT;
                }
            }
            g2.setColor(drawCol);

            float stroke = Math.max(1.5f, width / 11.0f);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int w = width;
            int h = height;

            switch (type) {
                case DASHBOARD:
                    int bSize = (int)(w * 0.36);
                    int gap = (int)(w * 0.16);
                    int ox = (w - (bSize * 2 + gap)) / 2;
                    int oy = (h - (bSize * 2 + gap)) / 2;
                    g2.fill(new RoundRectangle2D.Double(ox, oy, bSize, bSize, 3, 3));
                    g2.fill(new RoundRectangle2D.Double(ox + bSize + gap, oy, bSize, bSize, 3, 3));
                    g2.fill(new RoundRectangle2D.Double(ox, oy + bSize + gap, bSize, bSize, 3, 3));
                    g2.fill(new RoundRectangle2D.Double(ox + bSize + gap, oy + bSize + gap, bSize, bSize, 3, 3));
                    break;

                case BROWSE:
                    int r = (int)(w * 0.32);
                    int cx = (int)(w * 0.38);
                    int cy = (int)(h * 0.38);
                    g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                    int hx1 = (int)(cx + r * 0.7);
                    int hy1 = (int)(cy + r * 0.7);
                    int hx2 = (int)(w * 0.88);
                    int hy2 = (int)(h * 0.88);
                    g2.draw(new Line2D.Double(hx1, hy1, hx2, hy2));
                    break;

                case REPORT_ITEM:
                    int rw = (int)(w * 0.80);
                    int rh = (int)(h * 0.80);
                    int rox = (w - rw) / 2;
                    int roy = (h - rh) / 2;
                    g2.draw(new RoundRectangle2D.Double(rox, roy, rw, rh, 4, 4));
                    int midX = w / 2;
                    int midY = h / 2;
                    int pLen = (int)(rw * 0.32);
                    g2.draw(new Line2D.Double(midX - pLen, midY, midX + pLen, midY));
                    g2.draw(new Line2D.Double(midX, midY - pLen, midX, midY + pLen));
                    break;

                case MY_REPORTS:
                    int dw = (int)(w * 0.65);
                    int dh = (int)(h * 0.82);
                    int dox = (w - dw) / 2;
                    int doy = (h - dh) / 2;
                    g2.draw(new RoundRectangle2D.Double(dox, doy, dw, dh, 3, 3));
                    int lineY1 = (int)(doy + dh * 0.32);
                    int lineY2 = (int)(doy + dh * 0.52);
                    int lineY3 = (int)(doy + dh * 0.72);
                    int lStart = (int)(dox + dw * 0.22);
                    int lEnd = (int)(dox + dw * 0.78);
                    g2.draw(new Line2D.Double(lStart, lineY1, lEnd, lineY1));
                    g2.draw(new Line2D.Double(lStart, lineY2, lEnd, lineY2));
                    g2.draw(new Line2D.Double(lStart, lineY3, (int)(dox + dw * 0.58), lineY3));
                    break;

                case MY_CLAIMS:
                    Path2D tag = new Path2D.Double();
                    tag.moveTo(w * 0.20, h * 0.45);
                    tag.lineTo(w * 0.45, h * 0.20);
                    tag.lineTo(w * 0.82, h * 0.20);
                    tag.lineTo(w * 0.82, h * 0.55);
                    tag.lineTo(w * 0.55, h * 0.82);
                    tag.closePath();
                    g2.draw(tag);
                    g2.fill(new Ellipse2D.Double(w * 0.68, h * 0.30, w * 0.10, h * 0.10));
                    break;

                case USERS:
                    int headR = (int)(w * 0.20);
                    g2.draw(new Ellipse2D.Double(w * 0.50 - headR, h * 0.24 - headR, headR * 2, headR * 2));
                    Path2D shoulders = new Path2D.Double();
                    shoulders.moveTo(w * 0.22, h * 0.84);
                    shoulders.curveTo(w * 0.22, h * 0.58, w * 0.78, h * 0.58, w * 0.78, h * 0.84);
                    g2.draw(shoulders);
                    break;

                case LOGOUT:
                    int dx = (int)(w * 0.18);
                    int dy = (int)(h * 0.16);
                    int dww = (int)(w * 0.40);
                    int dhh = (int)(h * 0.68);
                    g2.draw(new RoundRectangle2D.Double(dx, dy, dww, dhh, 2, 2));
                    int ax1 = (int)(w * 0.46);
                    int ax2 = (int)(w * 0.88);
                    int ay = h / 2;
                    g2.draw(new Line2D.Double(ax1, ay, ax2, ay));
                    g2.draw(new Line2D.Double(ax2 - w * 0.14, ay - h * 0.14, ax2, ay));
                    g2.draw(new Line2D.Double(ax2 - w * 0.14, ay + h * 0.14, ax2, ay));
                    break;

                case REFRESH:
                    int cr = (int)(w * 0.36);
                    g2.draw(new java.awt.geom.Arc2D.Double(w / 2.0 - cr, h / 2.0 - cr, cr * 2, cr * 2, 35, 290, java.awt.geom.Arc2D.OPEN));
                    int arx = (int)(w / 2.0 + cr * 0.85);
                    int ary = (int)(h / 2.0 - cr * 0.50);
                    g2.draw(new Line2D.Double(arx - w * 0.12, ary, arx, ary));
                    g2.draw(new Line2D.Double(arx, ary, arx, ary + h * 0.12));
                    break;

                case CHECK_CIRCLE:
                    int crr = (int)(w * 0.44);
                    g2.fill(new Ellipse2D.Double(w / 2.0 - crr, h / 2.0 - crr, crr * 2, crr * 2));
                    g2.setColor(Color.WHITE);
                    Path2D check = new Path2D.Double();
                    check.moveTo(w * 0.32, h * 0.50);
                    check.lineTo(w * 0.46, h * 0.64);
                    check.lineTo(w * 0.70, h * 0.36);
                    g2.draw(check);
                    break;

                case LOCATION_PIN:
                    Path2D pin = new Path2D.Double();
                    pin.moveTo(w * 0.50, h * 0.88);
                    pin.curveTo(w * 0.26, h * 0.58, w * 0.26, h * 0.30, w * 0.50, h * 0.30);
                    pin.curveTo(w * 0.74, h * 0.30, w * 0.74, h * 0.58, w * 0.50, h * 0.88);
                    pin.closePath();
                    g2.draw(pin);
                    g2.fill(new Ellipse2D.Double(w * 0.44, h * 0.42, w * 0.12, h * 0.12));
                    break;

                case EMAIL:
                    // Envelope
                    int ew = (int)(w * 0.78);
                    int eh = (int)(h * 0.58);
                    int eox = (w - ew) / 2;
                    int eoy = (h - eh) / 2;
                    g2.draw(new RoundRectangle2D.Double(eox, eoy, ew, eh, 3, 3));
                    Path2D flap = new Path2D.Double();
                    flap.moveTo(eox, eoy);
                    flap.lineTo(w / 2.0, eoy + eh * 0.55);
                    flap.lineTo(eox + ew, eoy);
                    g2.draw(flap);
                    break;

                case LOCK:
                    // Padlock
                    int bw = (int)(w * 0.65);
                    int bh = (int)(h * 0.48);
                    int box = (w - bw) / 2;
                    int boy = (int)(h * 0.44);
                    g2.draw(new RoundRectangle2D.Double(box, boy, bw, bh, 3, 3));
                    // Shackle
                    int sw = (int)(bw * 0.60);
                    int sox = (w - sw) / 2;
                    int soy = (int)(h * 0.16);
                    int sh = (int)(h * 0.32);
                    g2.draw(new java.awt.geom.Arc2D.Double(sox, soy, sw, sh, 0, 180, java.awt.geom.Arc2D.OPEN));
                    // Keyhole dot
                    g2.fill(new Ellipse2D.Double(w / 2.0 - 1.5, boy + bh * 0.4, 3, 3));
                    break;

                case NOTICE_BELL:
                    // Bell icon
                    Path2D bell = new Path2D.Double();
                    bell.moveTo(w * 0.50, h * 0.20);
                    bell.curveTo(w * 0.35, h * 0.25, w * 0.30, h * 0.55, w * 0.22, h * 0.70);
                    bell.lineTo(w * 0.78, h * 0.70);
                    bell.curveTo(w * 0.70, h * 0.55, w * 0.65, h * 0.25, w * 0.50, h * 0.20);
                    bell.closePath();
                    g2.draw(bell);
                    g2.fill(new Ellipse2D.Double(w * 0.44, h * 0.75, w * 0.12, w * 0.12));
                    break;

                case CALENDAR:
                    // Calendar icon
                    int cw = (int)(w * 0.74);
                    int ch = (int)(h * 0.70);
                    int cox = (w - cw) / 2;
                    int coy = (int)(h * 0.20);
                    g2.draw(new RoundRectangle2D.Double(cox, coy, cw, ch, 3, 3));
                    g2.draw(new Line2D.Double(cox, coy + ch * 0.30, cox + cw, coy + ch * 0.30));
                    // Pins
                    g2.draw(new Line2D.Double(cox + cw * 0.25, coy - 2, cox + cw * 0.25, coy + 3));
                    g2.draw(new Line2D.Double(cox + cw * 0.75, coy - 2, cox + cw * 0.75, coy + 3));
                    break;

                case EYE:
                    // Eye outline
                    Path2D eye = new Path2D.Double();
                    eye.moveTo(w * 0.15, h * 0.50);
                    eye.curveTo(w * 0.35, h * 0.22, w * 0.65, h * 0.22, w * 0.85, h * 0.50);
                    eye.curveTo(w * 0.65, h * 0.78, w * 0.35, h * 0.78, w * 0.15, h * 0.50);
                    eye.closePath();
                    g2.draw(eye);
                    g2.fill(new Ellipse2D.Double(w * 0.50 - 3, h * 0.50 - 3, 6, 6));
                    break;

                case EYE_OFF:
                    // Eye with slash
                    Path2D eyeOff = new Path2D.Double();
                    eyeOff.moveTo(w * 0.15, h * 0.50);
                    eyeOff.curveTo(w * 0.35, h * 0.22, w * 0.65, h * 0.22, w * 0.85, h * 0.50);
                    eyeOff.curveTo(w * 0.65, h * 0.78, w * 0.35, h * 0.78, w * 0.15, h * 0.50);
                    eyeOff.closePath();
                    g2.draw(eyeOff);
                    g2.draw(new Line2D.Double(w * 0.20, h * 0.80, w * 0.80, h * 0.20));
                    break;

                case CAMERA:
                    // Modern vector camera
                    int camW = (int)(w * 0.78);
                    int camH = (int)(h * 0.56);
                    int camX = (w - camW) / 2;
                    int camY = (int)(h * 0.36);
                    g2.draw(new RoundRectangle2D.Double(camX, camY, camW, camH, 3, 3));
                    // Lens circle
                    int lr = (int)(camH * 0.26);
                    g2.draw(new Ellipse2D.Double(w / 2.0 - lr, camY + camH / 2.0 - lr, lr * 2, lr * 2));
                    // Flash / top viewfinder bump
                    int bumpW = (int)(camW * 0.36);
                    int bumpH = (int)(h * 0.14);
                    int bumpX = (w - bumpW) / 2;
                    int bumpY = camY - bumpH;
                    g2.draw(new RoundRectangle2D.Double(bumpX, bumpY, bumpW, bumpH + 2, 2, 2));
                    break;
            }

            g2.dispose();
        }
    }
}
