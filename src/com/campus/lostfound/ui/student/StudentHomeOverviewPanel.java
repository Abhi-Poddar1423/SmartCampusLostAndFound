package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.ItemDetailsDialog;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.ImageUtil;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
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
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * StudentHomeOverviewPanel
 *
 * Pixel-Accurate Student Dashboard Overview matching the reference specification:
 * - Wide campus-themed welcome banner with real user name, tagline, and current date.
 * - 4 horizontally aligned live metric cards (Lost Items, Found Items, My Reports, My Claims).
 * - Recent Items section with card grid (up to 4 in a row) and "View All" action.
 * - Right-side Quick Actions card and Campus Notice card.
 */
public class StudentHomeOverviewPanel extends JPanel {

    private final ItemDAO itemDAO;
    private final ClaimDAO claimDAO;
    private final Runnable openReportCallback;
    private final Runnable openBrowseCallback;
    private final Runnable openMyReportsCallback;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
    private final SimpleDateFormat bannerDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");

    private JLabel welcomeTitleLabel;
    private JLabel bannerDateLabel;

    private JLabel lostCountLabel;
    private JLabel foundCountLabel;
    private JLabel myReportsCountLabel;
    private JLabel myClaimsCountLabel;

    private JPanel recentItemsContainer;
    private JLabel statusLabel;

    public StudentHomeOverviewPanel(Runnable openReportCallback, Runnable openBrowseCallback, Runnable openMyReportsCallback) {
        this.itemDAO = new ItemDAO();
        this.claimDAO = new ClaimDAO();
        this.openReportCallback = openReportCallback;
        this.openBrowseCallback = openBrowseCallback;
        this.openMyReportsCallback = openMyReportsCallback;
        initializeUI();
        refreshOverviewData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ==========================================
        // 1. WIDE WELCOME BANNER (Navy/Teal Campus Banner)
        // ==========================================
        JPanel bannerCard = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Multi-tone navy to dark blue gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.PRIMARY,
                        w, h, UITheme.PRIMARY_LIGHT
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 16, 16);

                // Subtle campus teal accent overlay shapes
                g2.setColor(new Color(13, 148, 136, 45));
                g2.fillOval(w - 220, -50, 260, 260);
                g2.setColor(new Color(20, 184, 166, 30));
                g2.fillOval(w - 120, h - 90, 180, 180);

                // Architectural pillar outline watermark
                g2.setColor(new Color(255, 255, 255, 12));
                for (int i = 0; i < 4; i++) {
                    int px = w - 180 + i * 40;
                    g2.fillRect(px, 15, 12, h - 30);
                }

                // Border
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

                g2.dispose();
            }
        };
        bannerCard.setOpaque(false);
        bannerCard.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel bannerTextPanel = new JPanel();
        bannerTextPanel.setLayout(new BoxLayout(bannerTextPanel, BoxLayout.Y_AXIS));
        bannerTextPanel.setOpaque(false);

        String currentUserName = SessionManager.getInstance().isLoggedIn() ? SessionManager.getInstance().getUserName() : "Student";
        welcomeTitleLabel = new JLabel("Welcome to Smart Campus, " + currentUserName + "!");
        welcomeTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeTitleLabel.setForeground(Color.WHITE);

        JLabel bannerTagline = new JLabel("Find what matters. Return what belongs.");
        bannerTagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        bannerTagline.setForeground(new Color(45, 212, 191)); // Vibrant Teal

        bannerDateLabel = new JLabel(bannerDateFormat.format(new Date()));
        bannerDateLabel.setFont(UITheme.FONT_SMALL);
        bannerDateLabel.setForeground(new Color(224, 231, 255));

        bannerTextPanel.add(welcomeTitleLabel);
        bannerTextPanel.add(Box.createVerticalStrut(4));
        bannerTextPanel.add(bannerTagline);
        bannerTextPanel.add(Box.createVerticalStrut(6));
        bannerTextPanel.add(bannerDateLabel);

        // Action Buttons on Banner Right
        JPanel bannerBtnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bannerBtnBox.setOpaque(false);

        RoundedButton reportBtn = new RoundedButton("+ Report Item", RoundedButton.ButtonType.ACCENT);
        reportBtn.setPreferredSize(new Dimension(145, 38));
        reportBtn.addActionListener(e -> {
            if (openReportCallback != null) {
                openReportCallback.run();
            }
        });

        RoundedButton browseBtn = new RoundedButton("Browse Items", RoundedButton.ButtonType.OUTLINE);
        browseBtn.setPreferredSize(new Dimension(135, 38));
        browseBtn.addActionListener(e -> {
            if (openBrowseCallback != null) {
                openBrowseCallback.run();
            }
        });

        bannerBtnBox.add(browseBtn);
        bannerBtnBox.add(reportBtn);

        bannerCard.add(bannerTextPanel, BorderLayout.CENTER);
        bannerCard.add(bannerBtnBox, BorderLayout.EAST);

        // ==========================================
        // 2. STATISTICS METRIC CARDS (4 Columns)
        // ==========================================
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);

        lostCountLabel = new JLabel("—", SwingConstants.LEFT);
        foundCountLabel = new JLabel("—", SwingConstants.LEFT);
        myReportsCountLabel = new JLabel("—", SwingConstants.LEFT);
        myClaimsCountLabel = new JLabel("—", SwingConstants.LEFT);

        statsRow.add(createMetricCard("Lost Items", lostCountLabel, new Color(220, 38, 38), new Color(254, 242, 242), "Active missing reports", CampusIcons.IconType.BROWSE));
        statsRow.add(createMetricCard("Found Items", foundCountLabel, UITheme.ACCENT, new Color(240, 253, 250), "Awaiting claimant", CampusIcons.IconType.REPORT_ITEM));
        statsRow.add(createMetricCard("My Reports", myReportsCountLabel, UITheme.PRIMARY, new Color(239, 246, 255), "Reports submitted by you", CampusIcons.IconType.MY_REPORTS));
        statsRow.add(createMetricCard("My Claims", myClaimsCountLabel, new Color(217, 119, 6), new Color(254, 243, 199), "Claims you submitted", CampusIcons.IconType.MY_CLAIMS));

        // ==========================================
        // 3. LOWER SECTION: RECENT ITEMS + SIDEBAR
        // ==========================================
        JPanel lowerSection = new JPanel(new BorderLayout(16, 0));
        lowerSection.setOpaque(false);

        // Left Container: Recent Items (Card Grid up to 4 columns)
        JPanel recentSection = new JPanel(new BorderLayout(0, 10));
        recentSection.setOpaque(false);

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);

        JLabel recentTitle = new JLabel("Recently Reported Campus Items");
        recentTitle.setFont(UITheme.FONT_HEADER);
        recentTitle.setForeground(UITheme.PRIMARY);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        statusLabel = new JLabel("Loading live records...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel viewAllLink = new JLabel("<html><u><b>View All Items →</b></u></html>");
        viewAllLink.setFont(UITheme.FONT_SMALL);
        viewAllLink.setForeground(UITheme.ACCENT);
        viewAllLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAllLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (openBrowseCallback != null) {
                    openBrowseCallback.run();
                }
            }
        });

        headerRight.add(statusLabel);
        headerRight.add(Box.createHorizontalStrut(8));
        headerRight.add(viewAllLink);

        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(headerRight, BorderLayout.EAST);

        // Grid for recent items (up to 4 in a row)
        recentItemsContainer = new JPanel(new GridLayout(0, 2, 14, 14));
        recentItemsContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(recentItemsContainer);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        recentSection.add(recentHeader, BorderLayout.NORTH);
        recentSection.add(scrollPane, BorderLayout.CENTER);

        // Right Container: Quick Actions & Campus Notice
        JPanel rightSidePanel = new JPanel();
        rightSidePanel.setLayout(new BoxLayout(rightSidePanel, BoxLayout.Y_AXIS));
        rightSidePanel.setOpaque(false);
        rightSidePanel.setPreferredSize(new Dimension(280, 0));

        // Quick Actions Card
        JPanel quickActionsCard = UITheme.createCardPanel(14);
        quickActionsCard.setLayout(new BoxLayout(quickActionsCard, BoxLayout.Y_AXIS));
        quickActionsCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel qaTitle = new JLabel("Quick Actions");
        qaTitle.setFont(UITheme.FONT_HEADER);
        qaTitle.setForeground(UITheme.PRIMARY);
        qaTitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel qaAccent = UITheme.createAccentBar(32, 3);
        qaAccent.setAlignmentX(LEFT_ALIGNMENT);

        RoundedButton qaReportBtn = new RoundedButton("+ Report an Item", RoundedButton.ButtonType.ACCENT);
        qaReportBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        qaReportBtn.setAlignmentX(LEFT_ALIGNMENT);
        qaReportBtn.addActionListener(e -> {
            if (openReportCallback != null) {
                openReportCallback.run();
            }
        });

        RoundedButton qaBrowseBtn = new RoundedButton("Browse All Items", RoundedButton.ButtonType.OUTLINE);
        qaBrowseBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        qaBrowseBtn.setAlignmentX(LEFT_ALIGNMENT);
        qaBrowseBtn.setIcon(CampusIcons.get(CampusIcons.IconType.BROWSE, 14, 14, UITheme.PRIMARY));
        qaBrowseBtn.setIconTextGap(8);
        qaBrowseBtn.addActionListener(e -> {
            if (openBrowseCallback != null) {
                openBrowseCallback.run();
            }
        });

        RoundedButton qaReportsBtn = new RoundedButton("My Reported Items", RoundedButton.ButtonType.SECONDARY);
        qaReportsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        qaReportsBtn.setAlignmentX(LEFT_ALIGNMENT);
        qaReportsBtn.setIcon(CampusIcons.get(CampusIcons.IconType.MY_REPORTS, 14, 14, UITheme.TEXT_PRIMARY));
        qaReportsBtn.setIconTextGap(8);
        qaReportsBtn.addActionListener(e -> {
            if (openMyReportsCallback != null) {
                openMyReportsCallback.run();
            }
        });

        quickActionsCard.add(qaTitle);
        quickActionsCard.add(Box.createVerticalStrut(4));
        quickActionsCard.add(qaAccent);
        quickActionsCard.add(Box.createVerticalStrut(14));
        quickActionsCard.add(qaReportBtn);
        quickActionsCard.add(Box.createVerticalStrut(10));
        quickActionsCard.add(qaBrowseBtn);
        quickActionsCard.add(Box.createVerticalStrut(10));
        quickActionsCard.add(qaReportsBtn);

        // Campus Safety Notice Card
        JPanel noticeCard = UITheme.createCardPanel(14);
        noticeCard.setLayout(new BoxLayout(noticeCard, BoxLayout.Y_AXIS));
        noticeCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        noticeCard.setBackground(new Color(255, 255, 255));

        JPanel noticeHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        noticeHeader.setOpaque(false);
        noticeHeader.setAlignmentX(LEFT_ALIGNMENT);

        JLabel noticeIcon = new JLabel(CampusIcons.get(CampusIcons.IconType.NOTICE_BELL, 16, 16, UITheme.ACCENT));
        JLabel noticeTitle = new JLabel("Campus Notice");
        noticeTitle.setFont(UITheme.FONT_HEADER);
        noticeTitle.setForeground(UITheme.PRIMARY);

        noticeHeader.add(noticeIcon);
        noticeHeader.add(noticeTitle);

        JLabel noticeBody = new JLabel("<html><body style='line-height:1.35;'>" +
                "Verify distinguishing marks or serial numbers before claiming lost property.<br><br>" +
                "Turn in found items to <b>Campus Security Desk</b> (Academic Block 1) within 24 hours." +
                "</body></html>");
        noticeBody.setFont(UITheme.FONT_SMALL);
        noticeBody.setForeground(UITheme.TEXT_SECONDARY);
        noticeBody.setAlignmentX(LEFT_ALIGNMENT);
        noticeBody.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));

        noticeCard.add(noticeHeader);
        noticeCard.add(Box.createVerticalStrut(6));
        noticeCard.add(noticeBody);

        rightSidePanel.add(quickActionsCard);
        rightSidePanel.add(Box.createVerticalStrut(14));
        rightSidePanel.add(noticeCard);
        rightSidePanel.add(Box.createVerticalGlue());

        lowerSection.add(recentSection, BorderLayout.CENTER);
        lowerSection.add(rightSidePanel, BorderLayout.EAST);

        // Assemble Content Container
        JPanel centerContainer = new JPanel(new BorderLayout(0, 14));
        centerContainer.setOpaque(false);
        centerContainer.add(statsRow, BorderLayout.NORTH);
        centerContainer.add(lowerSection, BorderLayout.CENTER);

        add(bannerCard, BorderLayout.NORTH);
        add(centerContainer, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, Color bgTint, String subtitle, CampusIcons.IconType iconType) {
        JPanel card = UITheme.createCardPanel(12);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);

        // Small icon badge with tinted background
        JPanel iconBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        iconBadge.setBackground(bgTint);
        iconBadge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        iconBadge.add(new JLabel(CampusIcons.get(iconType, 16, 16, accentColor)));

        headerRow.add(titleLbl, BorderLayout.WEST);
        headerRow.add(iconBadge, BorderLayout.EAST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(UITheme.TEXT_MUTED);

        card.add(headerRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    public void refreshOverviewData() {
        if (SessionManager.getInstance().isLoggedIn()) {
            welcomeTitleLabel.setText("Welcome to Smart Campus, " + SessionManager.getInstance().getUserName() + "!");
        }
        bannerDateLabel.setText(bannerDateFormat.format(new Date()));
        statusLabel.setText("Refreshing from database...");

        int currentUserId = SessionManager.getInstance().getUserId();

        new SwingWorker<List<Item>, Void>() {
            private int lostCount = 0;
            private int foundCount = 0;
            private int myReportsCount = 0;
            private int myClaimsCount = 0;

            @Override
            protected List<Item> doInBackground() {
                lostCount = itemDAO.getLostItemsCount();
                foundCount = itemDAO.getFoundItemsCount();
                if (currentUserId > 0) {
                    myReportsCount = itemDAO.getItemsByReporter(currentUserId).size();
                    myClaimsCount = claimDAO.getClaimsByUser(currentUserId).size();
                }
                return itemDAO.getAllItems();
            }

            @Override
            protected void done() {
                try {
                    List<Item> allItems = get();
                    lostCountLabel.setText(String.valueOf(lostCount));
                    foundCountLabel.setText(String.valueOf(foundCount));
                    myReportsCountLabel.setText(String.valueOf(myReportsCount));
                    myClaimsCountLabel.setText(String.valueOf(myClaimsCount));

                    recentItemsContainer.removeAll();

                    int displayLimit = Math.min(6, allItems.size());
                    if (displayLimit == 0) {
                        JPanel emptyCard = UITheme.createCardPanel(10);
                        emptyCard.setLayout(new BorderLayout());
                        emptyCard.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
                        JLabel emptyLbl = new JLabel("<html><center><b>No items reported yet</b><br><span style='color:#64748B;'>Be the first to report a lost or found item.</span></center></html>", SwingConstants.CENTER);
                        emptyLbl.setFont(UITheme.FONT_REGULAR);
                        emptyCard.add(emptyLbl, BorderLayout.CENTER);
                        recentItemsContainer.setLayout(new BorderLayout());
                        recentItemsContainer.add(emptyCard, BorderLayout.CENTER);
                    } else {
                        recentItemsContainer.setLayout(new GridLayout(0, 2, 14, 14));
                        for (int i = 0; i < displayLimit; i++) {
                            recentItemsContainer.add(createItemCard(allItems.get(i)));
                        }
                    }

                    recentItemsContainer.revalidate();
                    recentItemsContainer.repaint();
                    statusLabel.setText("Synced live (" + allItems.size() + " total items)");
                } catch (Exception e) {
                    statusLabel.setText("Failed to update: " + e.getMessage());
                }
            }
        }.execute();
    }

    private JPanel createItemCard(Item item) {
        JPanel card = UITheme.createCardPanel(12);
        card.setLayout(new BorderLayout(10, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top Row: Type Badge + Date
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        boolean isLost = item.getItemType() == Item.ItemType.LOST;
        JLabel typeBadge = new JLabel(" " + item.getItemType().name() + " ");
        typeBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        typeBadge.setOpaque(true);
        typeBadge.setBackground(isLost ? new Color(254, 226, 226) : new Color(220, 252, 231));
        typeBadge.setForeground(isLost ? new Color(220, 38, 38) : new Color(22, 163, 74));

        String dateStr = item.getReportDate() != null ? dateFormat.format(item.getReportDate()) : "";
        JLabel dateLbl = new JLabel(dateStr);
        dateLbl.setFont(UITheme.FONT_SMALL);
        dateLbl.setForeground(UITheme.TEXT_MUTED);

        topRow.add(typeBadge, BorderLayout.WEST);
        topRow.add(dateLbl, BorderLayout.EAST);

        // Center Content: Title, Category & Location
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(item.getTitle());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(UITheme.PRIMARY);

        String cat = item.getCategory() != null ? item.getCategory() : "General";
        JLabel catLbl = new JLabel(cat);
        catLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        catLbl.setForeground(UITheme.TEXT_SECONDARY);

        String bldg = (item.getBuilding() != null && !item.getBuilding().trim().isEmpty()) ? item.getBuilding() : item.getLocation();
        JLabel locationLbl = new JLabel("📍 " + (bldg != null ? bldg : "Campus"));
        locationLbl.setFont(UITheme.FONT_SMALL);
        locationLbl.setForeground(UITheme.TEXT_SECONDARY);

        bodyPanel.add(titleLbl);
        bodyPanel.add(Box.createVerticalStrut(2));
        bodyPanel.add(catLbl);
        bodyPanel.add(Box.createVerticalStrut(3));
        bodyPanel.add(locationLbl);

        // Thumbnail on right (preserving aspect ratio)
        JLabel imgThumb = new JLabel();
        imgThumb.setPreferredSize(new Dimension(64, 64));
        imgThumb.setOpaque(true);
        imgThumb.setBackground(new Color(241, 245, 249));
        imgThumb.setHorizontalAlignment(SwingConstants.CENTER);
        imgThumb.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));

        if (item.getImagePath() != null && !item.getImagePath().trim().isEmpty()) {
            ImageIcon icon = ImageUtil.loadThumbnail(item.getImagePath(), 64, 64);
            if (icon != null) {
                imgThumb.setIcon(icon);
            } else {
                imgThumb.setText("📷");
            }
        } else {
            imgThumb.setText("—");
            imgThumb.setForeground(UITheme.TEXT_MUTED);
        }

        card.add(topRow, BorderLayout.NORTH);
        card.add(bodyPanel, BorderLayout.CENTER);
        card.add(imgThumb, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ItemDetailsDialog dialog = new ItemDetailsDialog(null, item);
                dialog.setVisible(true);
            }
        });

        return card;
    }
}
