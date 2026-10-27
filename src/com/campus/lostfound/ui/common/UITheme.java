package com.campus.lostfound.ui.common;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * UITheme
 * 
 * Central styling constants and factory methods for the Smart Campus application.
 * Provides a modern, clean, campus-inspired color palette and fonts.
 */
public class UITheme {

    // Primary Brand Colors (Deep Navy / Indigo)
    public static final Color PRIMARY = new Color(30, 58, 138);         // #1E3A8A
    public static final Color PRIMARY_HOVER = new Color(41, 74, 172);   // Slightly lighter on hover
    public static final Color PRIMARY_PRESSED = new Color(23, 45, 107); // Darker on click

    // Accent / Secondary Colors (Teal / Cyan)
    public static final Color ACCENT = new Color(13, 148, 136);         // #0D9488
    public static final Color ACCENT_HOVER = new Color(15, 168, 155);

    // Neutral & Background Colors
    public static final Color BG_MAIN = new Color(241, 245, 249);       // #F1F5F9 (Soft campus gray)
    public static final Color BG_CARD = Color.WHITE;                    // Pure white card background
    public static final Color BORDER = new Color(226, 232, 240);        // #E2E8F0 (Subtle borders)
    public static final Color BORDER_FOCUS = new Color(59, 130, 246);   // #3B82F6 (Blue focus border)

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);     // #0F172A (Deep dark slate)
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);// #64748B (Muted subtitle text)
    public static final Color TEXT_LIGHT = Color.WHITE;

    // Status Colors
    public static final Color SUCCESS = new Color(22, 163, 74);         // #16A34A (Green)
    public static final Color DANGER = new Color(220, 38, 38);          // #DC2626 (Red)
    public static final Color WARNING = new Color(217, 119, 6);         // #D97706 (Amber)

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);

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
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 38));
        tf.setBorder(createFieldBorder());
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
        pf.setPreferredSize(new Dimension(pf.getPreferredSize().width, 38));
        pf.setBorder(createFieldBorder());
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
}
