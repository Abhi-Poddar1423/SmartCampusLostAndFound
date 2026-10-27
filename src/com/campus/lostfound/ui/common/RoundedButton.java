package com.campus.lostfound.ui.common;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RoundedButton
 * 
 * A modern Swing button with rounded corners, smooth hover effects,
 * distinct active/pressed states, and proper disabled rendering.
 */
public class RoundedButton extends JButton {

    public enum ButtonType {
        PRIMARY,
        ACCENT,
        SECONDARY,
        OUTLINE,
        DANGER
    }

    private ButtonType buttonType;
    private int cornerRadius = 10;
    private boolean isHovered = false;
    private boolean isPressed = false;

    private Color normalBg;
    private Color hoverBg;
    private Color pressedBg;
    private Color normalFg;

    public RoundedButton(String text) {
        this(text, ButtonType.PRIMARY);
    }

    public RoundedButton(String text, ButtonType type) {
        super(text);
        this.buttonType = type;
        setupStyling();

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(UITheme.FONT_BOLD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(getPreferredSize().width + 24, 40));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    private void setupStyling() {
        switch (buttonType) {
            case PRIMARY:
                normalBg = UITheme.PRIMARY;
                hoverBg = UITheme.PRIMARY_HOVER;
                pressedBg = UITheme.PRIMARY_PRESSED;
                normalFg = Color.WHITE;
                break;
            case ACCENT:
                normalBg = UITheme.ACCENT;
                hoverBg = UITheme.ACCENT_HOVER;
                pressedBg = UITheme.PRIMARY;
                normalFg = Color.WHITE;
                break;
            case DANGER:
                normalBg = UITheme.DANGER;
                hoverBg = new Color(239, 68, 68);
                pressedBg = new Color(185, 28, 28);
                normalFg = Color.WHITE;
                break;
            case OUTLINE:
                normalBg = Color.WHITE;
                hoverBg = new Color(241, 245, 249);
                pressedBg = new Color(226, 232, 240);
                normalFg = UITheme.PRIMARY;
                break;
            case SECONDARY:
            default:
                normalBg = new Color(226, 232, 240);
                hoverBg = new Color(203, 213, 225);
                pressedBg = new Color(148, 163, 184);
                normalFg = UITheme.TEXT_PRIMARY;
                break;
        }
        setForeground(normalFg);
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Determine background color based on state
        Color currentBg;
        Color currentFg;

        if (!isEnabled()) {
            currentBg = new Color(226, 232, 240);
            currentFg = new Color(148, 163, 184);
        } else if (isPressed) {
            currentBg = pressedBg;
            currentFg = normalFg;
        } else if (isHovered) {
            currentBg = hoverBg;
            currentFg = normalFg;
        } else {
            currentBg = normalBg;
            currentFg = normalFg;
        }

        // Fill background
        g2.setColor(currentBg);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // Draw border for OUTLINE type
        if (buttonType == ButtonType.OUTLINE) {
            g2.setColor(isEnabled() ? UITheme.PRIMARY : new Color(203, 213, 225));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        // Draw Text
        g2.setColor(currentFg);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
