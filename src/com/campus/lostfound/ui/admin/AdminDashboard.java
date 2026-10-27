package com.campus.lostfound.ui.admin;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.UserDAO;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * AdminDashboard
 * 
 * Central management hub for campus administrators.
 * Displays live key performance metrics (users, lost, found, open items, pending claims)
 * and hosts management tabs for Users, Items, and Claims.
 * Requires an authenticated ADMIN session.
 */
public class AdminDashboard extends JFrame {

    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final ClaimDAO claimDAO;

    private JLabel totalUsersStatLabel;
    private JLabel lostItemsStatLabel;
    private JLabel foundItemsStatLabel;
    private JLabel openItemsStatLabel;
    private JLabel pendingClaimsStatLabel;

    private ManageUsersPanel usersPanel;
    private ManageItemsPanel itemsPanel;
    private ManageClaimsPanel claimsPanel;
    private JTabbedPane tabbedPane;

    public AdminDashboard() {
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
        this.claimDAO = new ClaimDAO();

        // Enforce session security: must be logged in as an ADMIN
        if (!SessionManager.getInstance().isLoggedIn() || !SessionManager.getInstance().isAdmin()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Access denied. Administrator privileges required.", "Unauthorized", JOptionPane.ERROR_MESSAGE);
                new LoginFrame().setVisible(true);
            });
            dispose();
            return;
        }

        initializeUI();
        refreshStatisticsAsync();
    }

    private void initializeUI() {
        String adminName = SessionManager.getInstance().getUserName();
        setTitle("Smart Campus Lost & Found - Administrator Dashboard (" + adminName + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1160, 760);
        setMinimumSize(new Dimension(920, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // 1. Top Header Bar
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setBackground(UITheme.PRIMARY);
        topBar.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);

        JLabel brandTitle = new JLabel("Smart Campus Lost & Found");
        brandTitle.setFont(UITheme.FONT_TITLE);
        brandTitle.setForeground(UITheme.TEXT_LIGHT);

        JLabel brandSubtitle = new JLabel("Administrator Control Center");
        brandSubtitle.setFont(UITheme.FONT_SMALL);
        brandSubtitle.setForeground(new Color(224, 231, 255));

        brandPanel.add(brandTitle);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(brandSubtitle);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 2));
        userPanel.setOpaque(false);

        JLabel adminBadge = new JLabel("Admin: " + adminName);
        adminBadge.setFont(UITheme.FONT_BOLD);
        adminBadge.setForeground(UITheme.TEXT_LIGHT);

        RoundedButton refreshStatsBtn = new RoundedButton("Refresh Stats", RoundedButton.ButtonType.SECONDARY);
        refreshStatsBtn.setPreferredSize(new Dimension(125, 38));
        refreshStatsBtn.addActionListener(e -> refreshAllData());

        RoundedButton logoutBtn = new RoundedButton("Logout", RoundedButton.ButtonType.OUTLINE);
        logoutBtn.setPreferredSize(new Dimension(95, 38));
        logoutBtn.addActionListener(e -> handleLogout());

        userPanel.add(adminBadge);
        userPanel.add(refreshStatsBtn);
        userPanel.add(logoutBtn);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(userPanel, BorderLayout.EAST);

        // 2. Summary KPI Metrics Panel
        JPanel metricsContainer = new JPanel(new GridLayout(1, 5, 14, 0));
        metricsContainer.setOpaque(false);
        metricsContainer.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        totalUsersStatLabel = new JLabel("...", SwingConstants.CENTER);
        lostItemsStatLabel = new JLabel("...", SwingConstants.CENTER);
        foundItemsStatLabel = new JLabel("...", SwingConstants.CENTER);
        openItemsStatLabel = new JLabel("...", SwingConstants.CENTER);
        pendingClaimsStatLabel = new JLabel("...", SwingConstants.CENTER);

        metricsContainer.add(createStatCard("Registered Users", totalUsersStatLabel, UITheme.PRIMARY));
        metricsContainer.add(createStatCard("Lost Items", lostItemsStatLabel, new Color(220, 38, 38)));
        metricsContainer.add(createStatCard("Found Items", foundItemsStatLabel, new Color(22, 163, 74)));
        metricsContainer.add(createStatCard("Open Items", openItemsStatLabel, new Color(37, 99, 235)));
        metricsContainer.add(createStatCard("Pending Claims", pendingClaimsStatLabel, new Color(217, 119, 6)));

        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.setOpaque(false);
        northWrapper.add(topBar, BorderLayout.NORTH);
        northWrapper.add(metricsContainer, BorderLayout.SOUTH);

        // 3. Tabbed Center Views
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(UITheme.BG_MAIN);

        usersPanel = new ManageUsersPanel(this::refreshStatisticsAsync);
        itemsPanel = new ManageItemsPanel(this::refreshStatisticsAsync);
        claimsPanel = new ManageClaimsPanel(this::refreshStatisticsAsync);

        tabbedPane.addTab("  Manage Users  ", usersPanel);
        tabbedPane.addTab("  Manage Items  ", itemsPanel);
        tabbedPane.addTab("  Manage Claims  ", claimsPanel);

        tabbedPane.addChangeListener(e -> {
            refreshStatisticsAsync();
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 0 && usersPanel != null) usersPanel.loadUsersAsync();
            else if (idx == 1 && itemsPanel != null) itemsPanel.refreshItems();
            else if (idx == 2 && claimsPanel != null) claimsPanel.loadClaimsAsync();
        });

        add(northWrapper, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = UITheme.createCardPanel(10);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);

        return card;
    }

    private void refreshAllData() {
        refreshStatisticsAsync();
        if (usersPanel != null) usersPanel.loadUsersAsync();
        if (itemsPanel != null) itemsPanel.refreshItems();
        if (claimsPanel != null) claimsPanel.loadClaimsAsync();
    }

    public void refreshStatisticsAsync() {
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                int users = userDAO.getTotalUsersCount();
                int lost = itemDAO.getLostItemsCount();
                int found = itemDAO.getFoundItemsCount();
                int open = itemDAO.getOpenItemsCount();
                int pending = claimDAO.getPendingClaimsCount();
                return new int[]{users, lost, found, open, pending};
            }

            @Override
            protected void done() {
                try {
                    int[] stats = get();
                    totalUsersStatLabel.setText(String.valueOf(stats[0]));
                    lostItemsStatLabel.setText(String.valueOf(stats[1]));
                    foundItemsStatLabel.setText(String.valueOf(stats[2]));
                    openItemsStatLabel.setText(String.valueOf(stats[3]));
                    pendingClaimsStatLabel.setText(String.valueOf(stats[4]));
                } catch (Exception ex) {
                    System.err.println("[AdminDashboard] Error refreshing statistics: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of the Administrator Portal?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().logout();
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            this.dispose();
        }
    }

    /**
     * Standalone main for testing with fallback mock session.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UITheme.setupLookAndFeel();
            if (!SessionManager.getInstance().isLoggedIn() || !SessionManager.getInstance().isAdmin()) {
                SessionManager.getInstance().login(
                        new com.campus.lostfound.models.User(1, "Test Administrator", "admin@campus.edu", "", com.campus.lostfound.models.User.Role.ADMIN, null)
                );
            }
            new AdminDashboard().setVisible(true);
        });
    }
}
