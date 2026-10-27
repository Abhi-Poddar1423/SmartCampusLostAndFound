package com.campus.lostfound.ui.auth;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.admin.AdminDashboard;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.CampusLogo;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.ui.student.StudentDashboard;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.SessionManager;
import com.campus.lostfound.utils.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
 * LoginFrame
 * 
 * Refined, Production-Quality University Portal Login Screen:
 * - Left Branding: Dark teal-to-emerald gradient with soft glowing accents,
 *   subtle low-opacity campus architecture watermark, clear hierarchy, and 3 feature points.
 * - Right Auth: Centered elevated white card, two-tone "Welcome Back" heading,
 *   email and password fields with leading icons & placeholders, native eye toggle,
 *   "Remember me" / "Forgot password?" row, full-width Sign In button, and clean registration link.
 */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JCheckBox rememberMeCheckBox;
    private RoundedButton loginButton;
    private JLabel errorLabel;
    private JLabel createAccountLink;
    private boolean passwordVisible = false;

    public LoginFrame() {
        this.userDAO = new UserDAO();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Smart Campus - Lost & Found Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 620);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        JPanel masterPanel = new JPanel(new BorderLayout());
        masterPanel.setBackground(UITheme.BG_MAIN);

        // ==========================================
        // 1. LEFT BRANDING SECTION (Dark Teal SaaS Canvas)
        // ==========================================
        JPanel leftBrandPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark Teal to Emerald gradient (#042F2E to #0D9488)
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(4, 47, 46),
                        w, h, new Color(13, 148, 136)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Soft abstract glowing shapes (reduced intensity to avoid competing with text)
                g2.setColor(new Color(20, 184, 166, 22));
                g2.fillOval(-60, -60, 220, 220);
                g2.setColor(new Color(45, 212, 191, 14));
                g2.fillOval(w - 120, h / 3, 180, 180);
                g2.setColor(new Color(16, 185, 129, 16));
                g2.fillOval(w / 4, h - 140, 160, 160);

                // Subtle university/campus architectural illustration near bottom with low opacity
                g2.setColor(new Color(255, 255, 255, 10));
                int baseH = (int)(h * 0.20);
                int baseY = h - baseH;

                // Base steps & plaza
                g2.fillRect(0, h - 20, w, 20);
                g2.fillRect(16, h - 28, w - 32, 8);

                // Classical portico columns
                int numCols = 6;
                int colW = Math.max(8, w / 30);
                int span = (w - 60) / (numCols - 1);
                for (int i = 0; i < numCols; i++) {
                    int cx = 30 + i * span;
                    g2.fillRect(cx - colW / 2, baseY + 16, colW, h - baseY - 44);
                    g2.fillRect(cx - colW / 2 - 2, baseY + 12, colW + 4, 4);
                    g2.fillRect(cx - colW / 2 - 2, h - 32, colW + 4, 4);
                }

                // Architrave & Triangular Pediment
                g2.fillRect(18, baseY + 6, w - 36, 6);
                int[] px = {14, w / 2, w - 14};
                int[] py = {baseY + 6, baseY - 24, baseY + 6};
                g2.fillPolygon(px, py, 3);

                // Dome silhouette behind pediment
                g2.setColor(new Color(255, 255, 255, 7));
                int domeW = (int)(w * 0.38);
                g2.fillArc((w - domeW) / 2, baseY - 46, domeW, 52, 0, 180);

                g2.dispose();
            }
        };
        leftBrandPanel.setPreferredSize(new Dimension(390, 620));
        leftBrandPanel.setLayout(new BoxLayout(leftBrandPanel, BoxLayout.Y_AXIS));
        leftBrandPanel.setBorder(BorderFactory.createEmptyBorder(38, 34, 38, 34));

        // Upper Branding Area (Logo + Brand Name + Subtitle)
        JPanel brandHeaderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandHeaderRow.setOpaque(false);
        brandHeaderRow.setAlignmentX(LEFT_ALIGNMENT);

        CampusLogo logo = new CampusLogo(CampusLogo.LogoSize.MEDIUM, false);
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel brandName = new JLabel("Smart Campus");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brandName.setForeground(UITheme.TEXT_LIGHT);

        JLabel portalSubtitle = new JLabel("Lost & Found Portal");
        portalSubtitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        portalSubtitle.setForeground(UITheme.ACCENT_LIGHT);

        titleBlock.add(brandName);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(portalSubtitle);

        brandHeaderRow.add(logo);
        brandHeaderRow.add(titleBlock);

        // Headline
        JLabel mainHeadline = new JLabel("Lost Something? Find It Here.");
        mainHeadline.setFont(new Font("Segoe UI", Font.BOLD, 21));
        mainHeadline.setForeground(Color.WHITE);
        mainHeadline.setAlignmentX(LEFT_ALIGNMENT);
        mainHeadline.setBorder(BorderFactory.createEmptyBorder(24, 2, 0, 0));

        // Short Description
        JLabel shortDesc = new JLabel("<html>A smarter campus community that helps lost belongings find their way home.</html>");
        shortDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        shortDesc.setForeground(new Color(204, 251, 241));
        shortDesc.setAlignmentX(LEFT_ALIGNMENT);
        shortDesc.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));

        // Three Feature Highlights
        JPanel featuresPanel = new JPanel();
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setOpaque(false);
        featuresPanel.setAlignmentX(LEFT_ALIGNMENT);
        featuresPanel.setBorder(BorderFactory.createEmptyBorder(22, 2, 0, 0));

        featuresPanel.add(createFeatureItem("Report Lost & Found Items"));
        featuresPanel.add(Box.createVerticalStrut(11));
        featuresPanel.add(createFeatureItem("Discover Items Across Campus"));
        featuresPanel.add(Box.createVerticalStrut(11));
        featuresPanel.add(createFeatureItem("Secure Ownership Verification"));

        leftBrandPanel.add(brandHeaderRow);
        leftBrandPanel.add(mainHeadline);
        leftBrandPanel.add(shortDesc);
        leftBrandPanel.add(featuresPanel);
        leftBrandPanel.add(Box.createVerticalGlue());

        // ==========================================
        // 2. RIGHT LOGIN SECTION (Refined White Card)
        // ==========================================
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setBackground(UITheme.BG_MAIN);
        rightWrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel formCard = UITheme.createElevatedCardPanel(16);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));
        formCard.setPreferredSize(new Dimension(390, 490));
        formCard.setMaximumSize(new Dimension(390, 490));

        // Two-Tone Heading: "Welcome Back"
        JLabel welcomeHeader = new JLabel("<html><span style='color:#102A43;'>Welcome </span><span style='color:#0D9488;'>Back</span></html>", SwingConstants.LEFT);
        welcomeHeader.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeHeader.setAlignmentX(LEFT_ALIGNMENT);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Sign in to continue to your campus account.", SwingConstants.LEFT);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Fixed-height Error message label to prevent form jumping
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        errorLabel.setPreferredSize(new Dimension(320, 18));
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        // Email Field with Leading Icon & Placeholder
        JLabel emailLbl = UITheme.createFieldLabel("Campus Email Address");
        emailLbl.setAlignmentX(LEFT_ALIGNMENT);

        emailField = UITheme.createTextField(20);
        emailField.setAlignmentX(LEFT_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        emailField.putClientProperty("JTextField.placeholderText", "student@university.edu");
        emailField.putClientProperty("JTextField.leadingIcon", CampusIcons.get(CampusIcons.IconType.EMAIL, 15, 15, UITheme.TEXT_MUTED));

        // Password Field with Leading Icon, Placeholder & Trailing Eye Toggle embedded inside
        JLabel pwdLbl = UITheme.createFieldLabel("Password");
        pwdLbl.setAlignmentX(LEFT_ALIGNMENT);

        passwordField = UITheme.createPasswordField(20);
        passwordField.setAlignmentX(LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.putClientProperty("JTextField.placeholderText", "Enter your password");
        passwordField.putClientProperty("JTextField.leadingIcon", CampusIcons.get(CampusIcons.IconType.LOCK, 15, 15, UITheme.TEXT_MUTED));

        // Interactive Eye toggle button embedded directly inside the field as trailingComponent
        JButton eyeToggleBtn = new JButton(CampusIcons.get(CampusIcons.IconType.EYE, 15, 15, UITheme.TEXT_MUTED));
        eyeToggleBtn.setToolTipText("Toggle password visibility");
        eyeToggleBtn.setContentAreaFilled(false);
        eyeToggleBtn.setBorderPainted(false);
        eyeToggleBtn.setFocusPainted(false);
        eyeToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeToggleBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 8));
        eyeToggleBtn.addActionListener(e -> {
            passwordVisible = !passwordVisible;
            passwordField.setEchoChar(passwordVisible ? (char) 0 : '•');
            eyeToggleBtn.setIcon(CampusIcons.get(
                    passwordVisible ? CampusIcons.IconType.EYE_OFF : CampusIcons.IconType.EYE,
                    15, 15,
                    passwordVisible ? UITheme.ACCENT : UITheme.TEXT_MUTED
            ));
        });
        passwordField.putClientProperty("JTextField.trailingComponent", eyeToggleBtn);

        // "Remember me" and "Forgot password?" row
        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.setAlignmentX(LEFT_ALIGNMENT);
        optionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        rememberMeCheckBox = new JCheckBox("Remember me");
        rememberMeCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberMeCheckBox.setForeground(UITheme.TEXT_SECONDARY);
        rememberMeCheckBox.setOpaque(false);
        rememberMeCheckBox.setFocusPainted(false);

        JLabel forgotPwdLink = new JLabel("<html><u>Forgot password?</u></html>");
        forgotPwdLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotPwdLink.setForeground(UITheme.ACCENT);
        forgotPwdLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPwdLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(
                        LoginFrame.this,
                        "To reset your campus account password, please contact the Campus IT Desk\n" +
                        "or visit the Administrative Office with your student ID card.",
                        "Password Assistance",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                forgotPwdLink.setForeground(UITheme.ACCENT_LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                forgotPwdLink.setForeground(UITheme.ACCENT);
            }
        });

        optionsRow.add(rememberMeCheckBox, BorderLayout.WEST);
        optionsRow.add(forgotPwdLink, BorderLayout.EAST);

        // Full-Width Sign In Button
        loginButton = new RoundedButton("Sign In", RoundedButton.ButtonType.ACCENT);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setPreferredSize(new Dimension(loginButton.getPreferredSize().width, 42));
        loginButton.addActionListener(e -> handleLogin());

        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        emailField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);

        // Divider Row with "OR"
        JPanel dividerRow = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int midY = getHeight() / 2;
                g2.setColor(UITheme.BORDER);
                g2.drawLine(0, midY, getWidth(), midY);
                g2.dispose();
            }
        };
        dividerRow.setOpaque(false);
        dividerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        dividerRow.setAlignmentX(LEFT_ALIGNMENT);

        JLabel orLabel = new JLabel("  OR  ", SwingConstants.CENTER);
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        orLabel.setForeground(UITheme.TEXT_MUTED);
        orLabel.setOpaque(true);
        orLabel.setBackground(Color.WHITE);

        JPanel orWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 1));
        orWrapper.setOpaque(false);
        orWrapper.add(orLabel);
        dividerRow.add(orWrapper, BorderLayout.CENTER);

        // Bottom Registration Row
        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerRow.setOpaque(false);
        registerRow.setAlignmentX(LEFT_ALIGNMENT);

        JLabel newToCampusLabel = new JLabel("New to Smart Campus?");
        newToCampusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        newToCampusLabel.setForeground(UITheme.TEXT_SECONDARY);

        createAccountLink = new JLabel("<html><u><b>Create an account</b></u></html>");
        createAccountLink.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        createAccountLink.setForeground(UITheme.ACCENT);
        createAccountLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccountLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegistration();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                createAccountLink.setForeground(UITheme.ACCENT_LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                createAccountLink.setForeground(UITheme.ACCENT);
            }
        });

        registerRow.add(newToCampusLabel);
        registerRow.add(createAccountLink);

        // Assemble Form Card with Clean Spacing
        formCard.add(welcomeHeader);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(subtitleLabel);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(errorLabel);
        formCard.add(Box.createVerticalStrut(4));

        formCard.add(emailLbl);
        formCard.add(Box.createVerticalStrut(5));
        formCard.add(emailField);
        formCard.add(Box.createVerticalStrut(12));

        formCard.add(pwdLbl);
        formCard.add(Box.createVerticalStrut(5));
        formCard.add(passwordField);
        formCard.add(Box.createVerticalStrut(8));

        formCard.add(optionsRow);
        formCard.add(Box.createVerticalStrut(16));

        formCard.add(loginButton);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(dividerRow);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(registerRow);

        rightWrapper.add(formCard);

        masterPanel.add(leftBrandPanel, BorderLayout.WEST);
        masterPanel.add(rightWrapper, BorderLayout.CENTER);

        add(masterPanel, BorderLayout.CENTER);
    }

    private JPanel createFeatureItem(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel iconLbl = new JLabel(CampusIcons.get(CampusIcons.IconType.CHECK_CIRCLE, 16, 16, UITheme.ACCENT_LIGHT));
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(new Color(241, 245, 249));

        row.add(iconLbl);
        row.add(label);
        return row;
    }

    private void handleLogin() {
        clearError();

        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        ValidationUtil.ValidationResult validation = ValidationUtil.validateLogin(email, password);
        if (!validation.isValid()) {
            showError(validation.getMessage());
            if (email.isEmpty() || !ValidationUtil.isValidEmail(email)) {
                emailField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }

        setLoading(true);

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                User user = userDAO.findByEmail(email);
                if (user == null) {
                    return null;
                }
                boolean verified = PasswordUtil.verifyPassword(password, user.getPassword());
                return verified ? user : null;
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    User authenticatedUser = get();
                    if (authenticatedUser == null) {
                        showError("Invalid email or password. Please try again.");
                        passwordField.setText("");
                        passwordField.requestFocus();
                        return;
                    }

                    SessionManager.getInstance().login(authenticatedUser);

                    dispose();
                    if (authenticatedUser.getRole() == User.Role.ADMIN) {
                        new AdminDashboard().setVisible(true);
                    } else {
                        new StudentDashboard().setVisible(true);
                    }
                } catch (Exception e) {
                    showError("Database connection error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void openRegistration() {
        new RegisterFrame().setVisible(true);
        dispose();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Signing in..." : "Sign In");
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
