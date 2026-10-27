package com.campus.lostfound.ui.student;

import com.campus.lostfound.ui.auth.LoginFrame;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * StudentDashboard
 * 
 * Main hub for college students.
 * Integrates:
 * - Browse Campus Items (search & filter)
 * - Quick 'Report New Item' modal dialog
 * - My Reported Items (manage student's reported items)
 * - My Claims (track status of submitted claims)
 * - Safe session logout returning to LoginFrame
 */
public class StudentDashboard extends JFrame {

    private ItemBrowsePanel browsePanel;
    private MyReportedItemsPanel reportedItemsPanel;
    private MyClaimsPanel claimsPanel;
    private JTabbedPane tabbedPane;

    public StudentDashboard() {
        // Enforce session security: student must be logged in
        if (!SessionManager.getInstance().isLoggedIn()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Access denied. Please log in first.", "Session Required", JOptionPane.WARNING_MESSAGE);
                new LoginFrame().setVisible(true);
            });
            dispose();
            return;
        }

        initializeUI();
    }

    private void initializeUI() {
        String studentName = SessionManager.getInstance().getUserName();
        setTitle("Smart Campus Lost & Found - Student Portal (" + studentName + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // 1. Top Campus Banner & Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setBackground(UITheme.PRIMARY);
        topBar.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        // Left Branding
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);

        JLabel brandTitle = new JLabel("Smart Campus Lost & Found");
        brandTitle.setFont(UITheme.FONT_TITLE);
        brandTitle.setForeground(UITheme.TEXT_LIGHT);

        JLabel brandSubtitle = new JLabel("Student Self-Service Portal");
        brandSubtitle.setFont(UITheme.FONT_SMALL);
        brandSubtitle.setForeground(new Color(224, 231, 255));

        brandPanel.add(brandTitle);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(brandSubtitle);

        // Right Actions & User Profile
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 2));
        rightPanel.setOpaque(false);

        JLabel userLabel = new JLabel("Welcome, " + studentName + " (Student)");
        userLabel.setFont(UITheme.FONT_BOLD);
        userLabel.setForeground(UITheme.TEXT_LIGHT);

        RoundedButton reportButton = new RoundedButton("+ Report New Item", RoundedButton.ButtonType.ACCENT);
        reportButton.setPreferredSize(new Dimension(165, 38));
        reportButton.addActionListener(e -> openReportDialog());

        RoundedButton logoutButton = new RoundedButton("Logout", RoundedButton.ButtonType.OUTLINE);
        logoutButton.setPreferredSize(new Dimension(95, 38));
        logoutButton.addActionListener(e -> handleLogout());

        rightPanel.add(userLabel);
        rightPanel.add(reportButton);
        rightPanel.add(logoutButton);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        // 2. Tabbed Center Views
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(UITheme.BG_MAIN);

        browsePanel = new ItemBrowsePanel();
        reportedItemsPanel = new MyReportedItemsPanel();
        claimsPanel = new MyClaimsPanel();

        tabbedPane.addTab("  Browse Campus Items  ", browsePanel);
        tabbedPane.addTab("  My Reported Items  ", reportedItemsPanel);
        tabbedPane.addTab("  My Submitted Claims  ", claimsPanel);

        // Refresh tab contents on selection
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0 && browsePanel != null) {
                browsePanel.refreshItems();
            } else if (selectedIndex == 1 && reportedItemsPanel != null) {
                reportedItemsPanel.loadUserItems();
            } else if (selectedIndex == 2 && claimsPanel != null) {
                claimsPanel.loadUserClaims();
            }
        });

        add(topBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void openReportDialog() {
        ReportItemDialog dialog = new ReportItemDialog(this, () -> {
            // Callback when report is submitted: refresh panels
            if (browsePanel != null) browsePanel.refreshItems();
            if (reportedItemsPanel != null) reportedItemsPanel.loadUserItems();
        });
        dialog.setVisible(true);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // Clear session safely
            SessionManager.getInstance().logout();

            // Return to LoginFrame
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            this.dispose();
        }
    }

    /**
     * Standalone main for testing StudentDashboard directly with mock session.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UITheme.setupLookAndFeel();
            // Provide fallback test session if launched standalone
            if (!SessionManager.getInstance().isLoggedIn()) {
                SessionManager.getInstance().login(
                        new com.campus.lostfound.models.User(1, "Test Student", "student@campus.edu", "", com.campus.lostfound.models.User.Role.STUDENT, null)
                );
            }
            new StudentDashboard().setVisible(true);
        });
    }
}
