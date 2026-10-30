package com.campus.lostfound.ui.common;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UITheme
 * 
 * Central styling constants and factory methods for the Smart Campus application.
 * Provides a modern, clean, campus-inspired color palette and fonts.
 */
public class UITheme {

    // Primary Brand Colors (Deep Navy / Dark Blue)
    public static final Color PRIMARY = new Color(16, 42, 67);           // #102A43 Deep Navy
    public static final Color PRIMARY_LIGHT = new Color(23, 59, 99);     // #173B63 Dark Blue
    public static final Color PRIMARY_HOVER = new Color(30, 75, 122);
    public static final Color PRIMARY_PRESSED = new Color(11, 30, 48);

    // Accent / Secondary Colors (Teal & Bright Teal)
    public static final Color ACCENT = new Color(13, 148, 136);          // #0D9488 Teal
    public static final Color ACCENT_LIGHT = new Color(20, 184, 166);    // #14B8A6 Bright Teal
    public static final Color ACCENT_HOVER = new Color(15, 168, 155);
    public static final Color ACCENT_PRESSED = new Color(11, 120, 110);

    // Neutral & Background Colors
    public static final Color BG_MAIN = new Color(243, 247, 251);        // #F3F7FB Background
    public static final Color BG_CARD = Color.WHITE;                     // #FFFFFF Card Background
    public static final Color BG_SIDEBAR = new Color(16, 42, 67);        // #102A43 Deep Navy Sidebar
    public static final Color BORDER = new Color(220, 229, 239);         // #DCE5EF Borders
    public static final Color BORDER_FOCUS = new Color(13, 148, 136);    // Teal Focus Border

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(24, 49, 83);      // #183153 Primary Text
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139); // #64748B Secondary Text
    public static final Color TEXT_MUTED = new Color(148, 163, 184);     // #94A3B8
    public static final Color TEXT_LIGHT = Color.WHITE;

    // Status Colors
    public static final Color SUCCESS = new Color(16, 185, 129);         // #10B981 (Modern Emerald)
    public static final Color DANGER = new Color(239, 68, 68);           // #EF4444 (Clean Red)
    public static final Color WARNING = new Color(245, 158, 11);         // #F59E0B (Amber Gold)
    public static final Color INFO = new Color(59, 130, 246);            // #3B82F6 (Blue)

    // Typography
    public static final Font FONT_HERO = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TAGLINE = new Font("Segoe UI", Font.ITALIC, 13);

    /**
     * Initializes the modern FlatLaf Look and Feel.
     * Falls back to System Look and Feel if FlatLaf is unavailable.
     */
    public static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.focusColor", ACCENT);
            UIManager.put("Component.focusedBorderColor", ACCENT);
            UIManager.put("ScrollBar.thumbArc", 999);
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
        }
    }

    /**
     * Creates a styled modern text field with comfortable padding and border.
     */
    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_REGULAR);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 40));
        if (UIManager.getLookAndFeel().getClass().getName().contains("Flat")) {
            tf.putClientProperty("JComponent.roundRect", true);
        } else {
            tf.setBorder(createFieldBorder());
        }
        return tf;
    }

    /**
     * Creates a styled modern password field with comfortable padding.
     */
    public static JPasswordField createPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(FONT_REGULAR);
        pf.setForeground(TEXT_PRIMARY);
        pf.setCaretColor(TEXT_PRIMARY);
        pf.setPreferredSize(new Dimension(pf.getPreferredSize().width, 40));
        if (UIManager.getLookAndFeel().getClass().getName().contains("Flat")) {
            pf.putClientProperty("JComponent.roundRect", true);
            pf.putClientProperty("JPasswordField.showRevealButton", true);
        } else {
            pf.setBorder(createFieldBorder());
        }
        return pf;
    }

    /**
     * Standard padded field border.
     */
    public static Border createFieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        );
    }

    /**
     * Creates a standardized field label.
     */
    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    /**
     * Creates a small rounded decorative accent line.
     */
    public static JPanel createAccentBar(int width, int height) {
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, width, height, height, height);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(width, height + 2));
        bar.setMaximumSize(new Dimension(width, height + 2));
        return bar;
    }

    /**
     * Creates a rounded card panel with an elevated soft shadow border.
     */
    public static JPanel createElevatedCardPanel(int cornerRadius) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Subtle soft outer glow / drop shadow simulation
                g2.setColor(new Color(16, 42, 67, 8));
                g2.fillRoundRect(2, 3, w - 4, h - 3, cornerRadius + 2, cornerRadius + 2);
                g2.setColor(new Color(16, 42, 67, 12));
                g2.fillRoundRect(1, 2, w - 2, h - 2, cornerRadius + 1, cornerRadius + 1);

                // Card surface
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, w - 1, h - 2, cornerRadius, cornerRadius);

                // Fine border
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 2, h - 3, cornerRadius, cornerRadius);

                g2.dispose();
            }
        };
    }

    /**
     * Creates a rounded card panel for dialogs and auth forms.
     */
    public static JPanel createCardPanel(int cornerRadius) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
                g2.dispose();
            }
        };
    }

    /**
     * Standard empty state panel with icon, title, and descriptive subtitle.
     */
    public static JPanel createEmptyStatePanel(CampusIcons.IconType iconType, String title, String subtitle) {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(60, 24, 60, 24));
        emptyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (iconType != null) {
            JLabel iconLabel = new JLabel(CampusIcons.get(iconType, 44, 44, new Color(203, 213, 225)));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(iconLabel);
            emptyPanel.add(Box.createVerticalStrut(14));
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(FONT_REGULAR);
        subLabel.setForeground(TEXT_SECONDARY);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(titleLabel);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(subLabel);

        return emptyPanel;
    }

    /**
     * Creates an initials avatar circle for sidebar profile cards.
     */
    public static JPanel createInitialAvatar(String fullName, int size) {
        String initials = "U";
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length == 1) {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            } else {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            }
        }
        final String text = initials;
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Outer circle
                g2.setColor(ACCENT);
                g2.fillOval(1, 1, w - 2, h - 2);

                // Subtle border
                g2.setColor(new Color(255, 255, 255, 90));
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawOval(1, 1, w - 2, h - 2);

                // Initials
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(size * 0.42)));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(size, size));
        avatar.setMaximumSize(new Dimension(size, size));
        avatar.setMinimumSize(new Dimension(size, size));
        return avatar;
    }

    /**
     * Interactive notification bell button with unread count pill badge.
     */
    public static class BadgeBellButton extends JButton {
        private int unreadCount = 0;
        private boolean isHovered = false;

        public BadgeBellButton() {
            setPreferredSize(new Dimension(42, 36));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("View Notifications");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public void setUnreadCount(int count) {
            this.unreadCount = count;
            if (count > 0) {
                setToolTipText("You have " + count + " unread notification" + (count == 1 ? "" : "s"));
            } else {
                setToolTipText("View Notifications");
            }
            repaint();
        }

        public int getUnreadCount() {
            return unreadCount;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background & border
            g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            // Bell icon
            Icon bell = CampusIcons.get(CampusIcons.IconType.NOTICE_BELL, 18, 18, unreadCount > 0 ? ACCENT : PRIMARY);
            int iconX = (w - bell.getIconWidth()) / 2;
            int iconY = (h - bell.getIconHeight()) / 2;
            bell.paintIcon(this, g2, iconX, iconY);

            // Badge pill
            if (unreadCount > 0) {
                String badgeText = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
                Font badgeFont = new Font("Segoe UI", Font.BOLD, 10);
                g2.setFont(badgeFont);
                FontMetrics fm = g2.getFontMetrics();

                int pad = 5;
                int textW = fm.stringWidth(badgeText);
                int badgeW = Math.max(16, textW + pad * 2);
                int badgeH = 15;
                int badgeX = w - badgeW - 3;
                int badgeY = 3;

                // Red pill
                g2.setColor(DANGER);
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, badgeH, badgeH);

                // White pill border
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, badgeH, badgeH);

                // Text
                g2.setColor(Color.WHITE);
                int tx = badgeX + (badgeW - textW) / 2;
                int ty = badgeY + (badgeH + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(badgeText, tx, ty);
            }

            g2.dispose();
        }
    }
}
