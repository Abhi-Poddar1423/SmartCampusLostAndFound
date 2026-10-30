package com.campus.lostfound.ui.student;

import com.campus.lostfound.ui.auth.LoginFrame;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.CampusLogo;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * StudentDashboard
 * 
 * Pixel-Accurate Student Dashboard matching the reference specification:
 * - Three-part layout: Dark Navy left sidebar, Clean White top action bar, Light Content area.
 * - Sidebar: Smart Campus branding, vector navigation icons, active indicator, user info, and logout.
 * - Top Action Bar: Integrated search field, refresh button, and prominent teal "+ Report Item" button.
 * - Views: Dashboard Overview, Browse Items, My Reported Items, My Claims.
 */
public class StudentDashboard extends JFrame {

    private CardLayout contentCardLayout;
    private JPanel contentContainer;

    private StudentHomeOverviewPanel overviewPanel;
    private ItemBrowsePanel browsePanel;
    private MyReportedItemsPanel reportedItemsPanel;
    private MyClaimsPanel claimsPanel;
    private NotificationsPanel notificationsPanel;

    private JTextField topSearchField;
    private RoundedButton refreshAllBtn;
    private UITheme.BadgeBellButton topBellBtn;
    private final com.campus.lostfound.dao.NotificationDAO notificationDAO = new com.campus.lostfound.dao.NotificationDAO();

    private final Map<String, JButton> navButtons = new HashMap<>();
    private String activeNav = "OVERVIEW";

    public StudentDashboard() {
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
        setTitle("Smart Campus - Lost & Found Portal (" + studentName + ")");
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
        sidebar.setPreferredSize(new Dimension(245, 800));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));

        // Brand Block at Sidebar Top
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

        JLabel appSub = new JLabel("Lost & Found Portal");
        appSub.setFont(UITheme.FONT_SMALL);
        appSub.setForeground(UITheme.ACCENT_LIGHT);

        titleBox.add(appTitle);
        titleBox.add(appSub);
        brandCard.add(sideLogo);
        brandCard.add(titleBox);

        sidebar.add(brandCard);
        sidebar.add(Box.createVerticalStrut(28));

        // Nav Section Label
        JLabel navHeader = new JLabel("MAIN MENU");
        navHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navHeader.setForeground(new Color(148, 163, 184));
        navHeader.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(navHeader);
        sidebar.add(Box.createVerticalStrut(10));

        // Navigation Items using high-contrast vector icons
        sidebar.add(createNavButton("Dashboard", CampusIcons.IconType.DASHBOARD, "OVERVIEW"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("Browse Items", CampusIcons.IconType.BROWSE, "BROWSE"));
        sidebar.add(Box.createVerticalStrut(6));

        // "Report an Item" direct action
        JButton reportNavBtn = createDirectActionButton("Report an Item", CampusIcons.IconType.REPORT_ITEM, this::openReportDialog);
        sidebar.add(reportNavBtn);
        sidebar.add(Box.createVerticalStrut(6));

        sidebar.add(createNavButton("My Reported Items", CampusIcons.IconType.MY_REPORTS, "REPORTED"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("My Claims", CampusIcons.IconType.MY_CLAIMS, "CLAIMS"));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(createNavButton("Notifications", CampusIcons.IconType.NOTICE_BELL, "NOTIFICATIONS"));

        sidebar.add(Box.createVerticalGlue());

        // User Profile Card & Logout at Sidebar Bottom
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

        JPanel avatar = UITheme.createInitialAvatar(studentName, 36);
        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel userNameLbl = new JLabel(studentName);
        userNameLbl.setFont(UITheme.FONT_BOLD);
        userNameLbl.setForeground(UITheme.TEXT_LIGHT);

        JLabel userRoleLbl = new JLabel("Student Portal Access");
        userRoleLbl.setFont(UITheme.FONT_SMALL);
        userRoleLbl.setForeground(new Color(148, 163, 184));

        textCol.add(userNameLbl);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(userRoleLbl);

        profileRow.add(avatar);
        profileRow.add(textCol);

        RoundedButton profileBtn = new RoundedButton("My Profile", RoundedButton.ButtonType.SECONDARY);
        profileBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        profileBtn.setAlignmentX(LEFT_ALIGNMENT);
        profileBtn.addActionListener(e -> new StudentProfileDialog(this).setVisible(true));

        RoundedButton logoutBtn = new RoundedButton("Logout", RoundedButton.ButtonType.OUTLINE);
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        logoutBtn.setAlignmentX(LEFT_ALIGNMENT);
        logoutBtn.setIcon(CampusIcons.get(CampusIcons.IconType.LOGOUT, 16, 16, UITheme.PRIMARY));
        logoutBtn.setIconTextGap(8);
        logoutBtn.addActionListener(e -> handleLogout());

        userCard.add(profileRow);
        userCard.add(Box.createVerticalStrut(10));
        userCard.add(profileBtn);
        userCard.add(Box.createVerticalStrut(8));
        userCard.add(logoutBtn);

        sidebar.add(userCard);

        // ==========================================
        // 2. MAIN CONTENT AREA & TOP ACTION BAR
        // ==========================================
        contentCardLayout = new CardLayout();
        contentContainer = new JPanel(contentCardLayout);
        contentContainer.setBackground(UITheme.BG_MAIN);

        overviewPanel = new StudentHomeOverviewPanel(
                this::openReportDialog,
                () -> switchView("BROWSE"),
                () -> switchView("REPORTED")
        );
        browsePanel = new ItemBrowsePanel();
        reportedItemsPanel = new MyReportedItemsPanel();
        claimsPanel = new MyClaimsPanel();
        notificationsPanel = new NotificationsPanel(this::updateUnreadBadgeAsync);

        contentContainer.add(overviewPanel, "OVERVIEW");
        contentContainer.add(browsePanel, "BROWSE");
        contentContainer.add(reportedItemsPanel, "REPORTED");
        contentContainer.add(claimsPanel, "CLAIMS");
        contentContainer.add(notificationsPanel, "NOTIFICATIONS");

        // Top Action Bar
        JPanel topStrip = new JPanel(new BorderLayout(14, 0));
        topStrip.setBackground(Color.WHITE);
        topStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        // Left section of toolbar: Search field
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);

        JLabel searchIcon = new JLabel(CampusIcons.get(CampusIcons.IconType.BROWSE, 16, 16, UITheme.TEXT_SECONDARY));
        topSearchField = UITheme.createTextField(18);
        topSearchField.setPreferredSize(new Dimension(240, 36));
        topSearchField.setToolTipText("Search title, category, or location...");
        topSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeTopSearch();
                }
            }
        });

        searchBox.add(searchIcon);
        searchBox.add(topSearchField);

        // Right section of toolbar: Notifications Bell, Refresh and + Report Item
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        topBellBtn = new UITheme.BadgeBellButton();
        topBellBtn.addActionListener(e -> switchView("NOTIFICATIONS"));

        refreshAllBtn = new RoundedButton("Refresh Data", RoundedButton.ButtonType.OUTLINE);
        refreshAllBtn.setPreferredSize(new Dimension(130, 36));
        refreshAllBtn.setIcon(CampusIcons.get(CampusIcons.IconType.REFRESH, 14, 14, UITheme.PRIMARY));
        refreshAllBtn.setIconTextGap(8);
        refreshAllBtn.addActionListener(e -> refreshActiveView());

        RoundedButton quickReportBtn = new RoundedButton("+ Report Item", RoundedButton.ButtonType.ACCENT);
        quickReportBtn.setPreferredSize(new Dimension(145, 36));
        quickReportBtn.addActionListener(e -> openReportDialog());

        rightActions.add(topBellBtn);
        rightActions.add(refreshAllBtn);
        rightActions.add(quickReportBtn);

        topStrip.add(searchBox, BorderLayout.WEST);
        topStrip.add(rightActions, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(topStrip, BorderLayout.NORTH);
        centerPanel.add(contentContainer, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);

        updateNavSelection("OVERVIEW");
        updateUnreadBadgeAsync();
    }

    private void executeTopSearch() {
        String query = topSearchField.getText().trim();
        if (!query.isEmpty()) {
            switchView("BROWSE");
            browsePanel.searchWithKeyword(query);
        }
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

    private JButton createDirectActionButton(String title, CampusIcons.IconType iconType, Runnable action) {
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
        btn.setIcon(CampusIcons.get(iconType, 18, 18, UITheme.ACCENT_LIGHT));
        btn.setIconTextGap(12);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(28, 56, 86));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(UITheme.BG_SIDEBAR);
            }
        });

        btn.addActionListener(e -> action.run());
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
        switch (activeNav) {
            case "OVERVIEW":
                overviewPanel.refreshOverviewData();
                break;
            case "BROWSE":
                browsePanel.refreshItems();
                break;
            case "REPORTED":
                reportedItemsPanel.refreshItems();
                break;
            case "CLAIMS":
                claimsPanel.refreshClaims();
                break;
            case "NOTIFICATIONS":
                notificationsPanel.refreshNotifications();
                break;
        }
        updateUnreadBadgeAsync();
    }

    private void openReportDialog() {
        ReportItemDialog dialog = new ReportItemDialog(this, () -> {
            overviewPanel.refreshOverviewData();
            browsePanel.refreshItems();
            reportedItemsPanel.refreshItems();
        });
        dialog.setVisible(true);
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
                "Are you sure you want to log out of your session?",
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
