package com.campus.lostfound.ui.auth;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.admin.AdminDashboard;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.ui.student.StudentDashboard;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.SessionManager;
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
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * LoginFrame
 * 
 * Provides a clean, modern Java Swing login screen.
 * Authenticates users using UserDAO and PBKDF2 password verification via SwingWorker
 * so the interface never freezes during database access.
 */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private RoundedButton loginButton;
    private JLabel errorLabel;
    private JLabel registerLink;

    public LoginFrame() {
        this.userDAO = new UserDAO();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Smart Campus Lost & Found - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 580);
        setMinimumSize(new Dimension(420, 520));
        setLocationRelativeTo(null); // Center on screen
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new GridBagLayout()); // Centers the card on screen

        // Main Card Panel
        JPanel card = UITheme.createCardPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));
        card.setPreferredSize(new Dimension(380, 480));
        card.setMaximumSize(new Dimension(380, 480));

        // Header Section
        JLabel titleLabel = new JLabel("Smart Campus", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Lost & Found Portal", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.FONT_HEADER);
        subtitleLabel.setForeground(UITheme.ACCENT);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel promptLabel = new JLabel("Sign in to your account", SwingConstants.CENTER);
        promptLabel.setFont(UITheme.FONT_SMALL);
        promptLabel.setForeground(UITheme.TEXT_SECONDARY);
        promptLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Error message label (hidden by default)
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Form Fields
        JLabel emailLabel = UITheme.createFieldLabel("Email Address");
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        emailField = UITheme.createTextField(20);
        emailField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel passwordLabel = UITheme.createFieldLabel("Password");
        passwordLabel.setAlignmentX(LEFT_ALIGNMENT);
        passwordField = UITheme.createPasswordField(20);
        passwordField.setAlignmentX(LEFT_ALIGNMENT);

        // Show/Hide password toggle
        showPasswordCheckBox = new JCheckBox("Show password");
        showPasswordCheckBox.setFont(UITheme.FONT_SMALL);
        showPasswordCheckBox.setForeground(UITheme.TEXT_SECONDARY);
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.setFocusPainted(false);
        showPasswordCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        showPasswordCheckBox.addActionListener(e -> {
            if (showPasswordCheckBox.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });

        // Submit Button
        loginButton = new RoundedButton("Sign In", RoundedButton.ButtonType.PRIMARY);
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.addActionListener(e -> handleLogin());

        // Enter key submits form
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        emailField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);

        // Register Link
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setFont(UITheme.FONT_SMALL);
        noAccountLabel.setForeground(UITheme.TEXT_SECONDARY);

        registerLink = new JLabel("Register here");
        registerLink.setFont(UITheme.FONT_SMALL);
        registerLink.setForeground(UITheme.PRIMARY);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegisterFrame();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                registerLink.setText("<html><u>Register here</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                registerLink.setText("Register here");
            }
        });

        footerPanel.add(noAccountLabel);
        footerPanel.add(registerLink);

        // Assemble Card Components
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(promptLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        card.add(emailLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));

        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(6));
        card.add(showPasswordCheckBox);
        card.add(Box.createVerticalStrut(20));

        card.add(loginButton);
        card.add(Box.createVerticalStrut(16));
        card.add(footerPanel);

        add(card);
    }

    /**
     * Handles the login action using SwingWorker for background database queries.
     */
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        // 1. Client-side input validation
        ValidationUtil.ValidationResult result = ValidationUtil.validateLogin(email, password);
        if (!result.isValid()) {
            showError(result.getMessage());
            return;
        }

        // 2. Set UI to loading state
        setLoadingState(true);
        clearError();

        // 3. Execute background authentication task
        new SwingWorker<User, Void>() {
            private String failureReason = "Invalid email or password.";

            @Override
            protected User doInBackground() {
                try {
                    // Fetch user record by email
                    User user = userDAO.findByEmail(email);
                    if (user == null) {
                        return null;
                    }

                    // Verify candidate password against stored PBKDF2 hash
                    boolean passwordMatches = PasswordUtil.verifyPassword(password, user.getPassword());
                    if (passwordMatches) {
                        return user;
                    } else {
                        return null;
                    }
                } catch (Exception ex) {
                    failureReason = "Database connection error: " + ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                setLoadingState(false);
                try {
                    User authenticatedUser = get();
                    if (authenticatedUser != null) {
                        // Store session
                        SessionManager.getInstance().login(authenticatedUser);

                        // Clear sensitive password field
                        passwordField.setText("");

                        if (authenticatedUser.getRole() == User.Role.STUDENT) {
                            StudentDashboard dashboard = new StudentDashboard();
                            dashboard.setVisible(true);
                            LoginFrame.this.dispose();
                        } else if (authenticatedUser.getRole() == User.Role.ADMIN) {
                            AdminDashboard adminDashboard = new AdminDashboard();
                            adminDashboard.setVisible(true);
                            LoginFrame.this.dispose();
                        }
                    } else {
                        showError(failureReason);
                        passwordField.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    showError("An unexpected error occurred during login.");
                }
            }
        }.execute();
    }

    private void setLoadingState(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Authenticating..." : "Sign In");
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        showPasswordCheckBox.setEnabled(!loading);
        registerLink.setEnabled(!loading);
        setCursor(loading ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private void openRegisterFrame() {
        RegisterFrame registerFrame = new RegisterFrame();
        registerFrame.setVisible(true);
        this.dispose();
    }

    /**
     * Standalone main method to test LoginFrame directly.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UITheme.setupLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }
}
