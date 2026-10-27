package com.campus.lostfound.ui.auth;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RegisterFrame
 * 
 * Provides student and admin registration for Smart Campus Lost & Found.
 * Validates inputs, hashes passwords with PBKDF2, and saves records using UserDAO
 * asynchronously via SwingWorker to keep the GUI responsive.
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
        setTitle("Smart Campus Lost & Found - Create Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 680);
        setMinimumSize(new Dimension(440, 620));
        setLocationRelativeTo(null); // Center on screen
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new GridBagLayout()); // Center card on screen

        // Main Card Panel
        JPanel card = UITheme.createCardPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 35, 28, 35));
        card.setPreferredSize(new Dimension(400, 600));
        card.setMaximumSize(new Dimension(400, 600));

        // Header Section
        JLabel titleLabel = new JLabel("Create Account", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Join the Smart Campus Community", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Error message label (hidden by default)
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Form Fields
        JLabel nameLabel = UITheme.createFieldLabel("Full Name");
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        nameField = UITheme.createTextField(20);
        nameField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel emailLabel = UITheme.createFieldLabel("College Email Address");
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        emailField = UITheme.createTextField(20);
        emailField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel passwordLabel = UITheme.createFieldLabel("Password (min. 6 characters)");
        passwordLabel.setAlignmentX(LEFT_ALIGNMENT);
        passwordField = UITheme.createPasswordField(20);
        passwordField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel confirmPasswordLabel = UITheme.createFieldLabel("Confirm Password");
        confirmPasswordLabel.setAlignmentX(LEFT_ALIGNMENT);
        confirmPasswordField = UITheme.createPasswordField(20);
        confirmPasswordField.setAlignmentX(LEFT_ALIGNMENT);

        // Show/Hide password toggle
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

        // Submit Button
        registerButton = new RoundedButton("Create Account", RoundedButton.ButtonType.PRIMARY);
        registerButton.setAlignmentX(CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        registerButton.addActionListener(e -> handleRegistration());

        // Enter key submits form
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleRegistration();
                }
            }
        };
        nameField.addKeyListener(enterKeyAdapter);
        emailField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);
        confirmPasswordField.addKeyListener(enterKeyAdapter);

        // Login Link
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel alreadyHaveAccountLabel = new JLabel("Already have an account?");
        alreadyHaveAccountLabel.setFont(UITheme.FONT_SMALL);
        alreadyHaveAccountLabel.setForeground(UITheme.TEXT_SECONDARY);

        loginLink = new JLabel("Sign In");
        loginLink.setFont(UITheme.FONT_SMALL);
        loginLink.setForeground(UITheme.PRIMARY);
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLoginFrame();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                loginLink.setText("<html><u>Sign In</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                loginLink.setText("Sign In");
            }
        });

        footerPanel.add(alreadyHaveAccountLabel);
        footerPanel.add(loginLink);

        // Assemble Card Components
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(6));

        card.add(nameLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(nameField);
        card.add(Box.createVerticalStrut(8));

        card.add(emailLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(emailField);
        card.add(Box.createVerticalStrut(8));

        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));

        card.add(confirmPasswordLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(confirmPasswordField);
        card.add(Box.createVerticalStrut(4));
        card.add(showPasswordCheckBox);
        card.add(Box.createVerticalStrut(14));

        card.add(registerButton);
        card.add(Box.createVerticalStrut(12));
        card.add(footerPanel);

        add(card);
    }

    /**
     * Handles account registration using SwingWorker for background database operations.
     */
    private void handleRegistration() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        // Public registration is strictly restricted to STUDENT role
        User.Role role = User.Role.STUDENT;
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // 1. Client-side input validation
        ValidationUtil.ValidationResult result = ValidationUtil.validateRegistration(
                name, email, password, confirmPassword
        );
        if (!result.isValid()) {
            showError(result.getMessage());
            return;
        }

        // 2. Set UI loading state
        setLoadingState(true);
        clearError();

        // 3. Background registration task
        new SwingWorker<Boolean, Void>() {
            private String failureMessage = "Registration failed. Please try again.";

            @Override
            protected Boolean doInBackground() {
                try {
                    // Check if email already exists
                    if (userDAO.emailExists(email)) {
                        failureMessage = "An account with this email already exists.";
                        return false;
                    }

                    // Hash password using PBKDF2 with random salt
                    String hashedPassword = PasswordUtil.hashPassword(password);

                    // Create new user model and persist to database
                    User newUser = new User(name, email, hashedPassword, role);
                    boolean registered = userDAO.register(newUser);

                    if (!registered) {
                        failureMessage = "Could not save user to the database. Check database connection.";
                        return false;
                    }

                    return true;
                } catch (Exception ex) {
                    failureMessage = "Database error: " + ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                setLoadingState(false);
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(
                                RegisterFrame.this,
                                "Account registered successfully!\nPlease sign in with your email and password.",
                                "Registration Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        openLoginFrame();
                    } else {
                        showError(failureMessage);
                    }
                } catch (Exception ex) {
                    showError("An unexpected error occurred during registration.");
                }
            }
        }.execute();
    }

    private void setLoadingState(boolean loading) {
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? "Creating Account..." : "Create Account");
        nameField.setEnabled(!loading);
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        confirmPasswordField.setEnabled(!loading);
        showPasswordCheckBox.setEnabled(!loading);
        loginLink.setEnabled(!loading);
        setCursor(loading ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private void openLoginFrame() {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
        this.dispose();
    }

    /**
     * Standalone main method to test RegisterFrame directly.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UITheme.setupLookAndFeel();
            new RegisterFrame().setVisible(true);
        });
    }
}
