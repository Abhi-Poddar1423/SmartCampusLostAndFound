package com.campus.lostfound.ui.auth;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.CampusLogo;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RegisterFrame
 * 
 * Cohesive Registration Screen matching the LoginFrame aesthetic:
 * - Left Panel: Navy-to-Teal campus hero with architectural silhouette and registration guidelines.
 * - Right Panel: Modern card form for Full Name, College Email, and Password with real-time validation.
 */
public class RegisterFrame extends JFrame {

    private final UserDAO userDAO;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JCheckBox showPasswordCheckBox;
    private RoundedButton registerButton;
    private JLabel errorLabel;
    private JLabel loginLink;

    public RegisterFrame() {
        this.userDAO = new UserDAO();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Smart Campus - Create Student Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(940, 650);
        setMinimumSize(new Dimension(860, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        JPanel masterPanel = new JPanel(new BorderLayout());
        masterPanel.setBackground(UITheme.BG_MAIN);

        // ==========================================
        // 1. LEFT PANEL: CAMPUS BRANDING HERO
        // ==========================================
        JPanel leftBrandPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.PRIMARY,
                        w, h, UITheme.ACCENT
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(-80, -80, 260, 260);
                g2.fillOval(w - 120, h / 3, 220, 220);

                // Silhouette
                g2.setColor(new Color(255, 255, 255, 14));
                int baseH = (int)(h * 0.18);
                int baseY = h - baseH;

                g2.fillRect(0, h - 24, w, 24);
                g2.fillRect(20, h - 34, w - 40, 10);

                int numCols = 6;
                int colW = Math.max(8, w / 28);
                int span = (w - 60) / (numCols - 1);
                for (int i = 0; i < numCols; i++) {
                    int cx = 30 + i * span;
                    g2.fillRect(cx - colW / 2, baseY + 20, colW, h - baseY - 54);
                    g2.fillRect(cx - colW / 2 - 3, baseY + 16, colW + 6, 5);
                    g2.fillRect(cx - colW / 2 - 3, h - 38, colW + 6, 5);
                }

                g2.fillRect(24, baseY + 8, w - 48, 8);
                int[] px = {20, w / 2, w - 20};
                int[] py = {baseY + 8, baseY - 24, baseY + 8};
                g2.fillPolygon(px, py, 3);

                g2.setColor(new Color(255, 255, 255, 10));
                int domeW = (int)(w * 0.38);
                g2.fillArc((w - domeW) / 2, baseY - 48, domeW, 56, 0, 180);

                g2.dispose();
            }
        };
        leftBrandPanel.setPreferredSize(new Dimension(420, 650));
        leftBrandPanel.setLayout(new BoxLayout(leftBrandPanel, BoxLayout.Y_AXIS));
        leftBrandPanel.setBorder(BorderFactory.createEmptyBorder(46, 38, 46, 38));

        CampusLogo logo = new CampusLogo(CampusLogo.LogoSize.HERO, false);
        logo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel brandName = new JLabel("Smart Campus", SwingConstants.CENTER);
        brandName.setFont(UITheme.FONT_HERO);
        brandName.setForeground(UITheme.TEXT_LIGHT);
        brandName.setAlignmentX(CENTER_ALIGNMENT);

        JLabel portalSubtitle = new JLabel("Lost & Found Portal", SwingConstants.CENTER);
        portalSubtitle.setFont(UITheme.FONT_HEADER);
        portalSubtitle.setForeground(UITheme.ACCENT_LIGHT);
        portalSubtitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel taglineLabel = new JLabel("<html><center>A Smarter Way to Reunite Lost Items<br>with Their Owners.</center></html>", SwingConstants.CENTER);
        taglineLabel.setFont(UITheme.FONT_TAGLINE);
        taglineLabel.setForeground(new Color(224, 231, 255));
        taglineLabel.setAlignmentX(CENTER_ALIGNMENT);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setAlignmentX(CENTER_ALIGNMENT);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(22, 10, 0, 10));

        infoPanel.add(createGuidelineItem("Use your valid college email address"));
        infoPanel.add(Box.createVerticalStrut(12));
        infoPanel.add(createGuidelineItem("Password must be at least 6 characters"));
        infoPanel.add(Box.createVerticalStrut(12));
        infoPanel.add(createGuidelineItem("Instant access to report and claim lost items"));

        leftBrandPanel.add(logo);
        leftBrandPanel.add(Box.createVerticalStrut(16));
        leftBrandPanel.add(brandName);
        leftBrandPanel.add(Box.createVerticalStrut(4));
        leftBrandPanel.add(portalSubtitle);
        leftBrandPanel.add(Box.createVerticalStrut(10));
        leftBrandPanel.add(taglineLabel);
        leftBrandPanel.add(Box.createVerticalStrut(22));
        leftBrandPanel.add(infoPanel);
        leftBrandPanel.add(Box.createVerticalGlue());

        // ==========================================
        // 2. RIGHT PANEL: REGISTRATION FORM
        // ==========================================
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setBackground(UITheme.BG_MAIN);
        rightWrapper.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        JPanel formCard = UITheme.createElevatedCardPanel(16);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        formCard.setPreferredSize(new Dimension(420, 560));
        formCard.setMaximumSize(new Dimension(420, 560));

        JLabel titleLabel = new JLabel("<html><span style='color:#102A43;'>Create </span><span style='color:#0D9488;'>Account</span></html>", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel accentBar = UITheme.createAccentBar(46, 4);
        accentBar.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Join your campus lost & found network.", SwingConstants.LEFT);
        subtitleLabel.setFont(UITheme.FONT_REGULAR);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Fields
        JLabel nameLbl = UITheme.createFieldLabel("Full Name");
        nameLbl.setAlignmentX(LEFT_ALIGNMENT);
        nameField = UITheme.createTextField(20);
        nameField.setAlignmentX(LEFT_ALIGNMENT);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        nameField.putClientProperty("JTextField.placeholderText", "e.g. Alex Morgan");

        JLabel emailLbl = UITheme.createFieldLabel("Campus Email Address");
        emailLbl.setAlignmentX(LEFT_ALIGNMENT);
        emailField = UITheme.createTextField(20);
        emailField.setAlignmentX(LEFT_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        emailField.putClientProperty("JTextField.placeholderText", "student@university.edu");
        emailField.putClientProperty("JTextField.leadingIcon", CampusIcons.get(CampusIcons.IconType.EMAIL, 16, 16, UITheme.TEXT_MUTED));

        JLabel pwdLbl = UITheme.createFieldLabel("Password (Min. 6 characters)");
        pwdLbl.setAlignmentX(LEFT_ALIGNMENT);
        passwordField = UITheme.createPasswordField(20);
        passwordField.setAlignmentX(LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.putClientProperty("JTextField.placeholderText", "Create password (min. 6 chars)");
        passwordField.putClientProperty("JTextField.leadingIcon", CampusIcons.get(CampusIcons.IconType.LOCK, 16, 16, UITheme.TEXT_MUTED));

        JLabel confirmPwdLbl = UITheme.createFieldLabel("Confirm Password");
        confirmPwdLbl.setAlignmentX(LEFT_ALIGNMENT);
        confirmPasswordField = UITheme.createPasswordField(20);
        confirmPasswordField.setAlignmentX(LEFT_ALIGNMENT);
        confirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        confirmPasswordField.putClientProperty("JTextField.placeholderText", "Re-enter password");
        confirmPasswordField.putClientProperty("JTextField.leadingIcon", CampusIcons.get(CampusIcons.IconType.LOCK, 16, 16, UITheme.TEXT_MUTED));

        showPasswordCheckBox = new JCheckBox("Show passwords");
        showPasswordCheckBox.setFont(UITheme.FONT_SMALL);
        showPasswordCheckBox.setForeground(UITheme.TEXT_SECONDARY);
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.setFocusPainted(false);
        showPasswordCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        showPasswordCheckBox.addActionListener(e -> {
            char echo = showPasswordCheckBox.isSelected() ? (char) 0 : '•';
            passwordField.setEchoChar(echo);
            confirmPasswordField.setEchoChar(echo);
        });

        registerButton = new RoundedButton("Create Account", RoundedButton.ButtonType.ACCENT);
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        registerButton.setAlignmentX(LEFT_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        registerButton.setPreferredSize(new Dimension(registerButton.getPreferredSize().width, 44));
        registerButton.addActionListener(e -> handleRegister());

        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleRegister();
                }
            }
        };
        nameField.addKeyListener(enterSubmit);
        emailField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);
        confirmPasswordField.addKeyListener(enterSubmit);

        // Login Row
        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        loginRow.setOpaque(false);
        loginRow.setAlignmentX(LEFT_ALIGNMENT);

        JLabel alreadyHaveAccount = new JLabel("Already have an account?");
        alreadyHaveAccount.setFont(UITheme.FONT_REGULAR);
        alreadyHaveAccount.setForeground(UITheme.TEXT_SECONDARY);

        loginLink = new JLabel("<html><u><b>Sign In Here</b></u></html>");
        loginLink.setFont(UITheme.FONT_REGULAR);
        loginLink.setForeground(UITheme.ACCENT);
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLogin();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                loginLink.setForeground(UITheme.ACCENT_LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                loginLink.setForeground(UITheme.ACCENT);
            }
        });

        loginRow.add(alreadyHaveAccount);
        loginRow.add(loginLink);

        // Assemble Form
        formCard.add(titleLabel);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(accentBar);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(subtitleLabel);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(errorLabel);
        formCard.add(Box.createVerticalStrut(6));

        formCard.add(nameLbl);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(nameField);
        formCard.add(Box.createVerticalStrut(10));

        formCard.add(emailLbl);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(emailField);
        formCard.add(Box.createVerticalStrut(10));

        formCard.add(pwdLbl);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(passwordField);
        formCard.add(Box.createVerticalStrut(10));

        formCard.add(confirmPwdLbl);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(confirmPasswordField);
        formCard.add(Box.createVerticalStrut(6));

        formCard.add(showPasswordCheckBox);
        formCard.add(Box.createVerticalStrut(14));

        formCard.add(registerButton);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(loginRow);

        rightWrapper.add(formCard);

        masterPanel.add(leftBrandPanel, BorderLayout.WEST);
        masterPanel.add(rightWrapper, BorderLayout.CENTER);

        add(masterPanel, BorderLayout.CENTER);
    }

    private JPanel createGuidelineItem(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel iconLbl = new JLabel(CampusIcons.get(CampusIcons.IconType.CHECK_CIRCLE, 16, 16, UITheme.ACCENT_LIGHT));
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(new Color(241, 245, 249));

        row.add(iconLbl);
        row.add(label);
        return row;
    }

    private void handleRegister() {
        clearError();

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        ValidationUtil.ValidationResult validation = ValidationUtil.validateRegistration(name, email, password, confirmPassword);
        if (!validation.isValid()) {
            showError(validation.getMessage());
            return;
        }

        setLoading(true);

        new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                if (userDAO.emailExists(email)) {
                    errorMessage = "This campus email is already registered. Please sign in.";
                    return false;
                }

                String hashedPassword = PasswordUtil.hashPassword(password);
                User newUser = new User(name, email, hashedPassword, User.Role.STUDENT);

                return userDAO.register(newUser);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    boolean success = get();
                    if (!success) {
                        showError(errorMessage != null ? errorMessage : "Registration failed. Please try again.");
                        return;
                    }

                    JOptionPane.showMessageDialog(
                            RegisterFrame.this,
                            "Your account has been created successfully!\nPlease sign in with your email and password.",
                            "Account Created",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    openLogin();
                } catch (Exception e) {
                    showError("Database connection error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void openLogin() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? "Creating Account..." : "Create Account");
        nameField.setEnabled(!loading);
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        confirmPasswordField.setEnabled(!loading);
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
