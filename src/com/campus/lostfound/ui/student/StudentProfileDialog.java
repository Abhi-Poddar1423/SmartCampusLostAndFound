package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.SessionManager;
import com.campus.lostfound.utils.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * StudentProfileDialog
 * 
 * Allows students to view and update their academic profile details:
 * - Roll Number (with campus uniqueness validation)
 * - Department / Academic Branch
 * - Semester / Academic Year
 * - Optional Phone Number
 * - Phone Privacy Toggle (private by default)
 */
public class StudentProfileDialog extends JDialog {

    public static final String[] DEPARTMENTS = {
            "-- Select Department --",
            "Computer Science & Engineering",
            "Information Technology",
            "Electronics & Communication Engineering",
            "Electrical & Electronics Engineering",
            "Mechanical Engineering",
            "Civil Engineering",
            "Chemical Engineering",
            "Biotechnology",
            "Management & Business Studies",
            "Basic Sciences & Humanities",
            "Other / General"
    };

    public static final String[] SEMESTERS = {
            "-- Select Semester --",
            "Semester 1",
            "Semester 2",
            "Semester 3",
            "Semester 4",
            "Semester 5",
            "Semester 6",
            "Semester 7",
            "Semester 8",
            "Postgraduate / Master's",
            "Doctoral / PhD",
            "Faculty / Staff"
    };

    private final UserDAO userDAO = new UserDAO();
    private User currentUser;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField rollField;
    private JComboBox<String> deptCombo;
    private JComboBox<String> semCombo;
    private JTextField phoneField;
    private JCheckBox phonePublicCheck;
    private JLabel statusLabel;
    private RoundedButton saveBtn;
    private RoundedButton cancelBtn;

    public StudentProfileDialog(Window owner) {
        super(owner, "Student Profile & Privacy Settings", ModalityType.APPLICATION_MODAL);
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        initializeUI();
        loadProfileData();
    }

    private void initializeUI() {
        setSize(520, 640);
        setMinimumSize(new Dimension(480, 580));
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel titleLbl = new JLabel("Campus Student Profile");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_LIGHT);

        JLabel subLbl = new JLabel("Keep your campus academic and contact details up-to-date");
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(new Color(224, 231, 255));
        subLbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        headerPanel.add(titleLbl);
        headerPanel.add(subLbl);

        // Center Card Panel
        JPanel card = UITheme.createCardPanel(12);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int row = 0;

        // Name (Read-only)
        gbc.gridx = 0; gbc.gridy = row++;
        JLabel nameLbl = new JLabel("Full Name");
        nameLbl.setFont(UITheme.FONT_LABEL);
        card.add(nameLbl, gbc);

        gbc.gridy = row++;
        nameField = UITheme.createTextField(20);
        nameField.setEditable(false);
        nameField.setBackground(new Color(241, 245, 249));
        nameField.setForeground(UITheme.TEXT_SECONDARY);
        card.add(nameField, gbc);

        // Email (Read-only)
        gbc.gridy = row++;
        JLabel emailLbl = new JLabel("Campus Institutional Email");
        emailLbl.setFont(UITheme.FONT_LABEL);
        card.add(emailLbl, gbc);

        gbc.gridy = row++;
        emailField = UITheme.createTextField(20);
        emailField.setEditable(false);
        emailField.setBackground(new Color(241, 245, 249));
        emailField.setForeground(UITheme.TEXT_SECONDARY);
        card.add(emailField, gbc);

        // Roll Number
        gbc.gridy = row++;
        JLabel rollLbl = new JLabel("Student Roll Number / Campus ID");
        rollLbl.setFont(UITheme.FONT_LABEL);
        card.add(rollLbl, gbc);

        gbc.gridy = row++;
        rollField = UITheme.createTextField(20);
        rollField.setToolTipText("e.g. CS2024-042");
        card.add(rollField, gbc);

        // Department & Semester Row
        gbc.gridy = row++;
        JPanel acadRow = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        acadRow.setOpaque(false);

        JPanel deptBox = new JPanel(new BorderLayout(0, 4));
        deptBox.setOpaque(false);
        JLabel deptLbl = new JLabel("Department / Branch");
        deptLbl.setFont(UITheme.FONT_LABEL);
        deptCombo = new JComboBox<>(DEPARTMENTS);
        deptCombo.setFont(UITheme.FONT_REGULAR);
        deptCombo.setPreferredSize(new Dimension(180, 36));
        deptBox.add(deptLbl, BorderLayout.NORTH);
        deptBox.add(deptCombo, BorderLayout.CENTER);

        JPanel semBox = new JPanel(new BorderLayout(0, 4));
        semBox.setOpaque(false);
        JLabel semLbl = new JLabel("Semester / Level");
        semLbl.setFont(UITheme.FONT_LABEL);
        semCombo = new JComboBox<>(SEMESTERS);
        semCombo.setFont(UITheme.FONT_REGULAR);
        semCombo.setPreferredSize(new Dimension(140, 36));
        semBox.add(semLbl, BorderLayout.NORTH);
        semBox.add(semCombo, BorderLayout.CENTER);

        acadRow.add(deptBox);
        acadRow.add(semBox);
        card.add(acadRow, gbc);

        // Phone Number (Optional)
        gbc.gridy = row++;
        JLabel phoneLbl = new JLabel("Phone Number (Optional)");
        phoneLbl.setFont(UITheme.FONT_LABEL);
        card.add(phoneLbl, gbc);

        gbc.gridy = row++;
        phoneField = UITheme.createTextField(20);
        phoneField.setToolTipText("e.g. +1 555-0199 or 9876543210");
        card.add(phoneField, gbc);

        // Privacy Checkbox
        gbc.gridy = row++;
        phonePublicCheck = new JCheckBox("Make phone number visible to student peers upon approved claim");
        phonePublicCheck.setFont(UITheme.FONT_REGULAR);
        phonePublicCheck.setForeground(UITheme.TEXT_PRIMARY);
        phonePublicCheck.setOpaque(false);
        card.add(phonePublicCheck, gbc);

        // Privacy Note Panel
        gbc.gridy = row++;
        JPanel privacyNotice = new JPanel(new BorderLayout());
        privacyNotice.setBackground(new Color(238, 242, 255));
        privacyNotice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(199, 210, 254), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JLabel noteLbl = new JLabel("<html><small style='color:#3730A3;'><b>🔒 Privacy Protected:</b> Campus email is your primary coordination channel. If the checkbox is unchecked, your phone number remains strictly private.</small></html>");
        privacyNotice.add(noteLbl, BorderLayout.CENTER);
        card.add(privacyNotice, gbc);

        // Wrapper for scroll / border
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_MAIN);
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        wrapper.add(card, BorderLayout.CENTER);

        // Footer Bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_MAIN);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 20, 16, 20));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        cancelBtn = new RoundedButton("Cancel", RoundedButton.ButtonType.OUTLINE);
        cancelBtn.addActionListener(e -> dispose());

        saveBtn = new RoundedButton("Save Profile", RoundedButton.ButtonType.ACCENT);
        saveBtn.addActionListener(e -> handleSaveProfile());

        btnBar.add(cancelBtn);
        btnBar.add(saveBtn);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(btnBar, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void loadProfileData() {
        if (currentUser == null) return;

        // Fetch fresh profile from database
        User freshUser = userDAO.findById(currentUser.getUserId());
        if (freshUser != null) {
            currentUser = freshUser;
            SessionManager.getInstance().login(freshUser);
        }

        nameField.setText(currentUser.getName() != null ? currentUser.getName() : "");
        emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        rollField.setText(currentUser.getRollNumber() != null ? currentUser.getRollNumber() : "");

        if (currentUser.getDepartment() != null) {
            deptCombo.setSelectedItem(currentUser.getDepartment());
        }
        if (currentUser.getSemester() != null) {
            semCombo.setSelectedItem(currentUser.getSemester());
        }

        phoneField.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        phonePublicCheck.setSelected(currentUser.isPhonePublic());
    }

    private void handleSaveProfile() {
        String roll = rollField.getText().trim();
        String dept = (String) deptCombo.getSelectedItem();
        String sem = (String) semCombo.getSelectedItem();
        String phone = phoneField.getText().trim();
        boolean isPhonePublic = phonePublicCheck.isSelected();

        if (deptCombo.getSelectedIndex() <= 0) {
            dept = null;
        }
        if (semCombo.getSelectedIndex() <= 0) {
            sem = null;
        }

        // Validate roll number format if provided
        if (!roll.isEmpty() && !ValidationUtil.isValidRollNumber(roll)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid roll number format (letters, digits, hyphens, 2-30 characters).",
                    "Invalid Roll Number", JOptionPane.WARNING_MESSAGE);
            rollField.requestFocus();
            return;
        }

        // Validate phone number format if provided
        if (!phone.isEmpty() && !ValidationUtil.isValidPhoneNumber(phone)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid phone number (e.g. +1 555-0199 or 9876543210).",
                    "Invalid Phone Number", JOptionPane.WARNING_MESSAGE);
            phoneField.requestFocus();
            return;
        }

        statusLabel.setText("Saving profile...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        saveBtn.setEnabled(false);

        final String fRoll = roll.isEmpty() ? null : roll;
        final String fDept = dept;
        final String fSem = sem;
        final String fPhone = phone.isEmpty() ? null : phone;

        new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                // Check roll number uniqueness
                if (fRoll != null && userDAO.isRollNumberTaken(fRoll, currentUser.getUserId())) {
                    errorMessage = "Roll Number '" + fRoll + "' is already registered to another student.";
                    return false;
                }

                currentUser.setRollNumber(fRoll);
                currentUser.setDepartment(fDept);
                currentUser.setSemester(fSem);
                currentUser.setPhoneNumber(fPhone);
                currentUser.setPhonePublic(isPhonePublic);

                return userDAO.updateProfile(currentUser);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                saveBtn.setEnabled(true);
                try {
                    boolean success = get();
                    if (success) {
                        SessionManager.getInstance().login(currentUser);
                        JOptionPane.showMessageDialog(StudentProfileDialog.this,
                                "Campus profile updated successfully!",
                                "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        String msg = errorMessage != null ? errorMessage : "Failed to update profile. Please try again.";
                        JOptionPane.showMessageDialog(StudentProfileDialog.this,
                                msg, "Update Failed", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Failed to save.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentProfileDialog.this,
                            "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Error occurred.");
                }
            }
        }.execute();
    }
}
