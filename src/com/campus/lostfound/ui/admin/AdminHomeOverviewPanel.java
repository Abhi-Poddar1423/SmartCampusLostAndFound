package com.campus.lostfound.ui.admin;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * AdminHomeOverviewPanel
 *
 * Clean, modern overview panel for campus administrators:
 * - Headline banner highlighting moderation responsibility.
 * - Live KPI metric cards for Users, Lost Items, Found Items, Open Items, and Pending Claims.
 * - System operational health and quick summary indicators.
 */
public class AdminHomeOverviewPanel extends JPanel {

    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final ClaimDAO claimDAO;

    private JLabel usersCountLabel;
    private JLabel lostCountLabel;
    private JLabel foundCountLabel;
    private JLabel openCountLabel;
    private JLabel claimsCountLabel;

    private JLabel statusLabel;

    public AdminHomeOverviewPanel() {
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
        this.claimDAO = new ClaimDAO();
        initializeUI();
        refreshMetrics();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        // 1. Admin Header Banner
        JPanel bannerCard = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.PRIMARY,
                        w, h, UITheme.PRIMARY_LIGHT
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(new Color(13, 148, 136, 35));
                g2.fillOval(w - 160, -30, 200, 200);

                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        bannerCard.setOpaque(false);
        bannerCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel bannerTextPanel = new JPanel();
        bannerTextPanel.setLayout(new BoxLayout(bannerTextPanel, BoxLayout.Y_AXIS));
        bannerTextPanel.setOpaque(false);

        JLabel bannerTitle = new JLabel("Administrative Control Center");
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        bannerTitle.setForeground(Color.WHITE);

        JLabel bannerTagline = new JLabel("Campus Inventory Moderation & Verification");
        bannerTagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        bannerTagline.setForeground(UITheme.ACCENT_LIGHT);

        JLabel bannerSubtitle = new JLabel("Oversee campus inventory, adjudicate ownership claims, and manage student accounts.");
        bannerSubtitle.setFont(UITheme.FONT_SMALL);
        bannerSubtitle.setForeground(new Color(224, 231, 255));

        bannerTextPanel.add(bannerTitle);
        bannerTextPanel.add(Box.createVerticalStrut(4));
        bannerTextPanel.add(bannerTagline);
        bannerTextPanel.add(Box.createVerticalStrut(4));
        bannerTextPanel.add(bannerSubtitle);

        statusLabel = new JLabel("Connecting to database...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(new Color(203, 213, 225));

        bannerCard.add(bannerTextPanel, BorderLayout.CENTER);
        bannerCard.add(statusLabel, BorderLayout.EAST);

        // 2. Metrics Cards Grid (6 KPI Cards across two rows)
        JPanel statsContainer = new JPanel(new GridLayout(2, 3, 16, 16));
        statsContainer.setOpaque(false);

        usersCountLabel = new JLabel("—", SwingConstants.LEFT);
        lostCountLabel = new JLabel("—", SwingConstants.LEFT);
        foundCountLabel = new JLabel("—", SwingConstants.LEFT);
        openCountLabel = new JLabel("—", SwingConstants.LEFT);
        claimsCountLabel = new JLabel("—", SwingConstants.LEFT);

        statsContainer.add(createMetricCard("Registered Accounts", usersCountLabel, UITheme.PRIMARY, "Active students & staff", CampusIcons.IconType.USERS));
        statsContainer.add(createMetricCard("Lost Items Reported", lostCountLabel, new Color(220, 38, 38), "Missing item alerts", CampusIcons.IconType.BROWSE));
        statsContainer.add(createMetricCard("Found Items Reported", foundCountLabel, UITheme.ACCENT, "Recovered on campus", CampusIcons.IconType.REPORT_ITEM));
        statsContainer.add(createMetricCard("Open Inventory", openCountLabel, new Color(16, 185, 129), "Active recoverable items", CampusIcons.IconType.DASHBOARD));
        statsContainer.add(createMetricCard("Pending Claims", claimsCountLabel, new Color(217, 119, 6), "Requires admin review", CampusIcons.IconType.MY_CLAIMS));

        // Sixth card: System Security & Integrity status
        JPanel sysCard = UITheme.createCardPanel(12);
        sysCard.setLayout(new BorderLayout(0, 6));
        sysCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel sysTop = new JPanel(new BorderLayout());
        sysTop.setOpaque(false);
        JLabel sysTitle = new JLabel("Security & Integrity");
        sysTitle.setFont(UITheme.FONT_SMALL);
        sysTitle.setForeground(UITheme.TEXT_SECONDARY);
        JLabel sysIcon = new JLabel(CampusIcons.get(CampusIcons.IconType.CHECK_CIRCLE, 16, 16, new Color(16, 185, 129)));
        sysTop.add(sysTitle, BorderLayout.WEST);
        sysTop.add(sysIcon, BorderLayout.EAST);

        JLabel sysStatus = new JLabel("SECURE & ONLINE");
        sysStatus.setFont(new Font("Segoe UI", Font.BOLD, 20));
        sysStatus.setForeground(new Color(16, 185, 129));

        JLabel sysSub = new JLabel("PBKDF2 Password Hashing & Safe JDBC");
        sysSub.setFont(UITheme.FONT_SMALL);
        sysSub.setForeground(UITheme.TEXT_MUTED);

        sysCard.add(sysTop, BorderLayout.NORTH);
        sysCard.add(sysStatus, BorderLayout.CENTER);
        sysCard.add(sysSub, BorderLayout.SOUTH);

        statsContainer.add(sysCard);

        add(bannerCard, BorderLayout.NORTH);
        add(statsContainer, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, String subtitle, CampusIcons.IconType iconType) {
        JPanel card = UITheme.createCardPanel(12);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);

        JLabel iconLbl = new JLabel(CampusIcons.get(iconType, 16, 16, accentColor));

        headerRow.add(titleLbl, BorderLayout.WEST);
        headerRow.add(iconLbl, BorderLayout.EAST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        card.add(headerRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    public void refreshMetrics() {
        statusLabel.setText("Refreshing data...");

        new SwingWorker<Void, Void>() {
            private int userCount = 0;
            private int lostCount = 0;
            private int foundCount = 0;
            private int openCount = 0;
            private int pendingClaimsCount = 0;

            @Override
            protected Void doInBackground() {
                userCount = userDAO.getAllUsers().size();
                lostCount = itemDAO.getLostItemsCount();
                foundCount = itemDAO.getFoundItemsCount();
                openCount = itemDAO.getOpenItemsCount();
                pendingClaimsCount = claimDAO.getPendingClaimsCount();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    usersCountLabel.setText(String.valueOf(userCount));
                    lostCountLabel.setText(String.valueOf(lostCount));
                    foundCountLabel.setText(String.valueOf(foundCount));
                    openCountLabel.setText(String.valueOf(openCount));
                    claimsCountLabel.setText(String.valueOf(pendingClaimsCount));
                    statusLabel.setText("System telemetry active & synced");
                } catch (Exception ex) {
                    statusLabel.setText("Failed to sync: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
