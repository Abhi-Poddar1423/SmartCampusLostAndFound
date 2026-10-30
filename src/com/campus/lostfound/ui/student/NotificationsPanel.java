package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.NotificationDAO;
import com.campus.lostfound.models.Notification;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsPanel
 * 
 * Elegant, responsive notification center matching the university navy/teal design theme:
 * - Filter toggle (All vs Unread).
 * - Live unread count header.
 * - Interactive individual "Mark as Read" action.
 * - "Mark All as Read" batch action.
 * - Empty state with campus graphic placeholder.
 * - Anti-aliased custom card styling for unread vs read notifications.
 */
public class NotificationsPanel extends JPanel {

    private final NotificationDAO notificationDAO;
    private final Runnable onUnreadCountChanged;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a");

    private JPanel listContainer;
    private JLabel countHeaderLabel;
    private JComboBox<String> filterCombo;
    private RoundedButton markAllReadBtn;
    private RoundedButton refreshBtn;
    private List<Notification> currentNotifications = new ArrayList<>();

    public NotificationsPanel(Runnable onUnreadCountChanged) {
        this.notificationDAO = new NotificationDAO();
        this.onUnreadCountChanged = onUnreadCountChanged;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Control Header
        JPanel topHeader = UITheme.createCardPanel(12);
        topHeader.setLayout(new BorderLayout(16, 0));
        topHeader.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        // Left title & badge
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel mainTitle = new JLabel("Notifications");
        mainTitle.setFont(UITheme.FONT_TITLE);
        mainTitle.setForeground(UITheme.PRIMARY);

        countHeaderLabel = new JLabel("Checking for updates...");
        countHeaderLabel.setFont(UITheme.FONT_SMALL);
        countHeaderLabel.setForeground(UITheme.TEXT_SECONDARY);

        titleBox.add(mainTitle);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(countHeaderLabel);

        // Right actions (Filter combo, Mark All as Read, Refresh)
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JLabel filterLbl = new JLabel("View:");
        filterLbl.setFont(UITheme.FONT_SMALL);
        filterLbl.setForeground(UITheme.TEXT_SECONDARY);

        filterCombo = new JComboBox<>(new String[]{"All Notifications", "Unread Only"});
        filterCombo.setFont(UITheme.FONT_REGULAR);
        filterCombo.setPreferredSize(new Dimension(150, 34));
        filterCombo.addActionListener(e -> loadNotificationsAsync());

        markAllReadBtn = new RoundedButton("Mark All as Read", RoundedButton.ButtonType.OUTLINE);
        markAllReadBtn.setPreferredSize(new Dimension(145, 34));
        markAllReadBtn.addActionListener(e -> handleMarkAllAsRead());

        refreshBtn = new RoundedButton("Refresh", RoundedButton.ButtonType.OUTLINE);
        refreshBtn.setPreferredSize(new Dimension(95, 34));
        refreshBtn.setIcon(CampusIcons.get(CampusIcons.IconType.REFRESH, 14, 14, UITheme.PRIMARY));
        refreshBtn.setIconTextGap(6);
        refreshBtn.addActionListener(e -> loadNotificationsAsync());

        actionBox.add(filterLbl);
        actionBox.add(filterCombo);
        actionBox.add(markAllReadBtn);
        actionBox.add(refreshBtn);

        topHeader.add(titleBox, BorderLayout.WEST);
        topHeader.add(actionBox, BorderLayout.EAST);

        // Content Area with Scroll
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(UITheme.BG_MAIN);
        listContainer.setBorder(BorderFactory.createEmptyBorder(16, 6, 16, 6));

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UITheme.BG_MAIN);
        scrollPane.getViewport().setBackground(UITheme.BG_MAIN);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(topHeader, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshNotifications() {
        loadNotificationsAsync();
    }

    public void loadNotificationsAsync() {
        int userId = SessionManager.getInstance().getUserId();
        if (userId <= 0) return;

        boolean unreadOnly = filterCombo.getSelectedIndex() == 1;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Notification>, Void>() {
            private int unreadCount = 0;

            @Override
            protected List<Notification> doInBackground() {
                unreadCount = notificationDAO.getUnreadCount(userId);
                return notificationDAO.getNotificationsByUser(userId, unreadOnly);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    currentNotifications = get();
                    countHeaderLabel.setText(unreadCount == 0 ? "You're all caught up! No unread notifications."
                            : "You have " + unreadCount + " unread notification" + (unreadCount == 1 ? "" : "s") + ".");
                    markAllReadBtn.setEnabled(unreadCount > 0);
                    renderNotificationsList();

                    if (onUnreadCountChanged != null) {
                        onUnreadCountChanged.run();
                    }
                } catch (Exception ex) {
                    countHeaderLabel.setText("Failed to load notifications: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void renderNotificationsList() {
        listContainer.removeAll();

        if (currentNotifications.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
            emptyPanel.setOpaque(false);
            emptyPanel.setBorder(BorderFactory.createEmptyBorder(60, 20, 60, 20));
            emptyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel emptyIcon = new JLabel(CampusIcons.get(CampusIcons.IconType.NOTICE_BELL, 48, 48, new Color(203, 213, 225)));
            emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel emptyTitle = new JLabel("No Notifications");
            emptyTitle.setFont(UITheme.FONT_TITLE);
            emptyTitle.setForeground(UITheme.TEXT_SECONDARY);
            emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel emptySub = new JLabel("When your claims are reviewed or item statuses update, you'll see them here.");
            emptySub.setFont(UITheme.FONT_REGULAR);
            emptySub.setForeground(UITheme.TEXT_MUTED);
            emptySub.setAlignmentX(Component.CENTER_ALIGNMENT);

            emptyPanel.add(emptyIcon);
            emptyPanel.add(Box.createVerticalStrut(12));
            emptyPanel.add(emptyTitle);
            emptyPanel.add(Box.createVerticalStrut(6));
            emptyPanel.add(emptySub);

            listContainer.add(emptyPanel);
        } else {
            for (Notification n : currentNotifications) {
                listContainer.add(createNotificationCard(n));
                listContainer.add(Box.createVerticalStrut(12));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createNotificationCard(Notification n) {
        JPanel card = UITheme.createCardPanel(10);
        card.setLayout(new BorderLayout(14, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        card.setPreferredSize(new Dimension(800, 86));

        if (!n.isRead()) {
            card.setBackground(new Color(240, 253, 250)); // Soft teal background for unread
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, UITheme.ACCENT),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)
            ));
        } else {
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(226, 232, 240)),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)
            ));
        }

        // Left status icon
        CampusIcons.IconType iconType;
        Color iconColor;
        switch (n.getNotificationType()) {
            case CLAIM_APPROVED:
            case ITEM_RETURNED:
                iconType = CampusIcons.IconType.CHECK_CIRCLE;
                iconColor = UITheme.ACCENT;
                break;
            case CLAIM_REJECTED:
                iconType = CampusIcons.IconType.NOTICE_BELL;
                iconColor = UITheme.DANGER;
                break;
            default:
                iconType = CampusIcons.IconType.NOTICE_BELL;
                iconColor = UITheme.PRIMARY;
                break;
        }

        JLabel iconLabel = new JLabel(CampusIcons.get(iconType, 22, 22, iconColor));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);

        // Center Text Block
        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        JLabel titleLbl = new JLabel(n.getTitle());
        titleLbl.setFont(n.isRead() ? UITheme.FONT_REGULAR : UITheme.FONT_BOLD);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        if (!n.isRead()) {
            JLabel newBadge = new JLabel("NEW");
            newBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            newBadge.setForeground(UITheme.ACCENT);
            newBadge.setOpaque(true);
            newBadge.setBackground(new Color(204, 251, 241));
            newBadge.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
            titleRow.add(titleLbl);
            titleRow.add(newBadge);
        } else {
            titleRow.add(titleLbl);
        }

        JLabel messageLbl = new JLabel(n.getMessage());
        messageLbl.setFont(UITheme.FONT_REGULAR);
        messageLbl.setForeground(UITheme.TEXT_SECONDARY);
        messageLbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 0, 0));

        String dateStr = n.getCreatedAt() != null ? dateFormat.format(n.getCreatedAt()) : "Recently";
        JLabel dateLbl = new JLabel(dateStr);
        dateLbl.setFont(UITheme.FONT_SMALL);
        dateLbl.setForeground(UITheme.TEXT_MUTED);
        dateLbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 0));

        textBlock.add(titleRow);
        textBlock.add(messageLbl);
        textBlock.add(dateLbl);

        // Right Action: Mark as read button
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 12));
        actionBox.setOpaque(false);

        if (!n.isRead()) {
            RoundedButton readBtn = new RoundedButton("Mark as read", RoundedButton.ButtonType.OUTLINE);
            readBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            readBtn.setPreferredSize(new Dimension(115, 28));
            readBtn.addActionListener(e -> handleMarkSingleRead(n.getNotificationId()));
            actionBox.add(readBtn);
        } else {
            JLabel readStatus = new JLabel("Read");
            readStatus.setFont(UITheme.FONT_SMALL);
            readStatus.setForeground(UITheme.TEXT_MUTED);
            actionBox.add(readStatus);
        }

        card.add(iconLabel, BorderLayout.WEST);
        card.add(textBlock, BorderLayout.CENTER);
        card.add(actionBox, BorderLayout.EAST);

        return card;
    }

    private void handleMarkSingleRead(int notificationId) {
        int userId = SessionManager.getInstance().getUserId();
        if (userId <= 0) return;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return notificationDAO.markAsRead(notificationId, userId);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadNotificationsAsync();
                    }
                } catch (Exception ignored) { }
            }
        }.execute();
    }

    private void handleMarkAllAsRead() {
        int userId = SessionManager.getInstance().getUserId();
        if (userId <= 0) return;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return notificationDAO.markAllAsRead(userId);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadNotificationsAsync();
                    }
                } catch (Exception ignored) { }
            }
        }.execute();
    }
}
