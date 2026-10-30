package com.campus.lostfound.ui.admin;

import com.campus.lostfound.dao.NotificationDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.ui.auth.LoginFrame;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.CampusLogo;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.ui.student.NotificationsPanel;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminDashboard
 * 
 * Complete UI/UX Redesign:
 * Replaces tabbed interface with a modern, high-contrast administrator sidebar navigation:
 * - Left Sidebar: Dark administrative panel with real vector icons and active-view indicators.
 * - Views:
 *   1. "Overview" -> AdminHomeOverviewPanel (KPI metrics, inventory breakdown, telemetry)
 *   2. "Manage Items" -> ManageItemsPanel (campus item moderation & resolution)
 *   3. "Manage Claims" -> ManageClaimsPanel (claim adjudication & proof review)
 *   4. "Manage Users" -> ManageUsersPanel (student & admin account permissions)
 */
public class AdminDashboard extends JFrame {

    private CardLayout contentCardLayout;
    private JPanel contentContainer;

    private AdminHomeOverviewPanel overviewPanel;
    private ManageItemsPanel itemsPanel;
    private ManageClaimsPanel claimsPanel;
    private ManageUsersPanel usersPanel;
    private NotificationsPanel notificationsPanel;

    private RoundedButton refreshAllBtn;
    private UITheme.BadgeBellButton topBellBtn;
    private final NotificationDAO notificationDAO = new NotificationDAO();

    private final Map<String, JButton> navButtons = new HashMap<>();
    private String activeNav = "OVERVIEW";

    public AdminDashboard() {
        if (!SessionManager.getInstance().isLoggedIn() || !SessionManager.getInstance().isAdmin()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Access denied. Administrator privileges required.", "Restricted Access", JOptionPane.ERROR_MESSAGE);
                new LoginFrame().setVisible(true);
            });
            dispose();
            return;
        }

        initializeUI();
    }

    private void initializeUI() {
        String adminName = SessionManager.getInstance().getUserName();
        setTitle("Smart Campus Administration Portal (" + adminName + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 800);
        setMinimumSize(new Dimension(1060, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // ==========================================
        // 1. LEFT SIDEBAR NAVIGATION
        // ==========================================
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 800));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));

        // Brand Emblem in Sidebar
        JPanel brandCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandCard.setOpaque(false);
        brandCard.setAlignmentX(LEFT_ALIGNMENT);

        CampusLogo sideLogo = new CampusLogo(CampusLogo.LogoSize.SMALL, false);
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel appTitle = new JLabel("Smart Campus");
        appTitle.setFont(UITheme.FONT_HEADER);
        appTitle.setForeground(UITheme.TEXT_LIGHT);

        JLabel appSub = new JLabel("Admin Control Center");
        appSub.setFont(UITheme.FONT_SMALL);
        appSub.setForeground(UITheme.ACCENT_LIGHT);

        titleBox.add(appTitle);
        titleBox.add(appSub);
        brandCard.add(sideLogo);
        brandCard.add(titleBox);

        sidebar.add(brandCard);
        sidebar.add(Box.createVerticalStrut(28));

        // Nav Section Label
        JLabel navHeader = new JLabel("ADMINISTRATION");
        navHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navHeader.setForeground(new Color(148, 163, 184));
        navHeader.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(navHeader);
        sidebar.add(Box.createVerticalStrut(10));

        // Navigation Items with reliable Java 2D vector icons
        sidebar.add(createNavButton("Dashboard Overview", CampusIcons.IconType.DASHBOARD, "OVERVIEW"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("Manage Items", CampusIcons.IconType.REPORT_ITEM, "ITEMS"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("Manage Claims", CampusIcons.IconType.MY_CLAIMS, "CLAIMS"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("Manage Users", CampusIcons.IconType.USERS, "USERS"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("Notifications", CampusIcons.IconType.NOTICE_BELL, "NOTIFICATIONS"));

        sidebar.add(Box.createVerticalGlue());

        // Bottom User Profile & Logout
        JPanel userCard = new JPanel();
        userCard.setLayout(new BoxLayout(userCard, BoxLayout.Y_AXIS));
        userCard.setOpaque(false);
        userCard.setAlignmentX(LEFT_ALIGNMENT);
        userCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 24)),
                BorderFactory.createEmptyBorder(14, 0, 0, 0)
        ));

        JPanel profileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        profileRow.setOpaque(false);
        profileRow.setAlignmentX(LEFT_ALIGNMENT);

        JPanel avatar = UITheme.createInitialAvatar(adminName, 36);
        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel userNameLbl = new JLabel(adminName);
        userNameLbl.setFont(UITheme.FONT_BOLD);
        userNameLbl.setForeground(UITheme.TEXT_LIGHT);

        JLabel userRoleLbl = new JLabel("Administrator Role");
        userRoleLbl.setFont(UITheme.FONT_SMALL);
        userRoleLbl.setForeground(UITheme.ACCENT_LIGHT);

        textCol.add(userNameLbl);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(userRoleLbl);

        profileRow.add(avatar);
        profileRow.add(textCol);

        RoundedButton logoutBtn = new RoundedButton("Logout", RoundedButton.ButtonType.OUTLINE);
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        logoutBtn.setAlignmentX(LEFT_ALIGNMENT);
        logoutBtn.setIcon(CampusIcons.get(CampusIcons.IconType.LOGOUT, 16, 16, UITheme.PRIMARY));
        logoutBtn.setIconTextGap(8);
        logoutBtn.addActionListener(e -> handleLogout());

        userCard.add(profileRow);
        userCard.add(Box.createVerticalStrut(12));
        userCard.add(logoutBtn);

        sidebar.add(userCard);

        // ==========================================
        // 2. MAIN CONTENT AREA (CARD LAYOUT)
        // ==========================================
        contentCardLayout = new CardLayout();
        contentContainer = new JPanel(contentCardLayout);
        contentContainer.setBackground(UITheme.BG_MAIN);

        // Instantiate Sub-Panels
        overviewPanel = new AdminHomeOverviewPanel();
        itemsPanel = new ManageItemsPanel(() -> overviewPanel.refreshMetrics());
        claimsPanel = new ManageClaimsPanel(() -> overviewPanel.refreshMetrics());
        usersPanel = new ManageUsersPanel(() -> overviewPanel.refreshMetrics());
        notificationsPanel = new NotificationsPanel(this::updateUnreadBadgeAsync);

        contentContainer.add(overviewPanel, "OVERVIEW");
        contentContainer.add(itemsPanel, "ITEMS");
        contentContainer.add(claimsPanel, "CLAIMS");
        contentContainer.add(usersPanel, "USERS");
        contentContainer.add(notificationsPanel, "NOTIFICATIONS");

        // Top Header Actions Strip
        JPanel topStrip = new JPanel(new BorderLayout(14, 0));
        topStrip.setBackground(Color.WHITE);
        topStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        JLabel headerTitle = new JLabel("Campus Administration Console");
        headerTitle.setFont(UITheme.FONT_HEADER);
        headerTitle.setForeground(UITheme.PRIMARY);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        topBellBtn = new UITheme.BadgeBellButton();
        topBellBtn.addActionListener(e -> switchView("NOTIFICATIONS"));

        refreshAllBtn = new RoundedButton("Refresh Data", RoundedButton.ButtonType.OUTLINE);
        refreshAllBtn.setPreferredSize(new Dimension(130, 36));
        refreshAllBtn.setIcon(CampusIcons.get(CampusIcons.IconType.REFRESH, 14, 14, UITheme.PRIMARY));
        refreshAllBtn.setIconTextGap(8);
        refreshAllBtn.addActionListener(e -> refreshActiveView());

        rightActions.add(topBellBtn);
        rightActions.add(refreshAllBtn);

        topStrip.add(headerTitle, BorderLayout.WEST);
        topStrip.add(rightActions, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(topStrip, BorderLayout.NORTH);
        centerPanel.add(contentContainer, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);

        updateNavSelection("OVERVIEW");
        updateUnreadBadgeAsync();
    }

    private JButton createNavButton(String title, CampusIcons.IconType iconType, String viewKey) {
        JButton btn = new JButton(title);
        btn.setFont(UITheme.FONT_REGULAR);
        btn.setForeground(new Color(203, 213, 225));
        btn.setBackground(UITheme.BG_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setIcon(CampusIcons.get(iconType, 18, 18, new Color(148, 163, 184)));
        btn.setIconTextGap(12);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!viewKey.equals(activeNav)) {
                    btn.setBackground(new Color(28, 56, 86));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!viewKey.equals(activeNav)) {
                    btn.setBackground(UITheme.BG_SIDEBAR);
                }
            }
        });

        btn.addActionListener(e -> switchView(viewKey));

        navButtons.put(viewKey, btn);
        return btn;
    }

    private void switchView(String viewKey) {
        this.activeNav = viewKey;
        contentCardLayout.show(contentContainer, viewKey);
        updateNavSelection(viewKey);
        refreshActiveView();
    }

    private void updateNavSelection(String activeKey) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(activeKey)) {
                btn.setBackground(UITheme.PRIMARY_LIGHT);
                btn.setForeground(Color.WHITE);
                btn.setFont(UITheme.FONT_BOLD);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, UITheme.ACCENT_LIGHT),
                        BorderFactory.createEmptyBorder(8, 11, 8, 12)
                ));
            } else {
                btn.setBackground(UITheme.BG_SIDEBAR);
                btn.setForeground(new Color(203, 213, 225));
                btn.setFont(UITheme.FONT_REGULAR);
                btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 12));
            }
        }
    }

    private void refreshActiveView() {
        if (!SessionManager.getInstance().isAdmin() || !new UserDAO().isActiveAdmin(SessionManager.getInstance().getUserId())) {
            JOptionPane.showMessageDialog(this, 
                    "Your administrator session is no longer active. Please sign in again.", 
                    "Session Expired", 
                    JOptionPane.WARNING_MESSAGE);
            SessionManager.getInstance().logout();
            new LoginFrame().setVisible(true);
            dispose();
            return;
        }

        switch (activeNav) {
            case "OVERVIEW":
                overviewPanel.refreshMetrics();
                break;
            case "ITEMS":
                itemsPanel.refreshItems();
                break;
            case "CLAIMS":
                claimsPanel.refreshClaims();
                break;
            case "USERS":
                usersPanel.refreshUsers();
                break;
            case "NOTIFICATIONS":
                notificationsPanel.refreshNotifications();
                break;
        }
        updateUnreadBadgeAsync();
    }

    private void updateUnreadBadgeAsync() {
        int userId = SessionManager.getInstance().getUserId();
        if (userId <= 0) return;

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return notificationDAO.getUnreadCount(userId);
            }

            @Override
            protected void done() {
                try {
                    int unread = get();
                    JButton notifNavBtn = navButtons.get("NOTIFICATIONS");
                    if (notifNavBtn != null) {
                        if (unread > 0) {
                            notifNavBtn.setText("Notifications (" + unread + ")");
                        } else {
                            notifNavBtn.setText("Notifications");
                        }
                    }

                    if (topBellBtn != null) {
                        topBellBtn.setUnreadCount(unread);
                    }
                } catch (Exception ignored) { }
            }
        }.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of administrator mode?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().logout();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }
}
