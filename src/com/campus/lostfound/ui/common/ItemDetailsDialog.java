package com.campus.lostfound.ui.common;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;
import com.campus.lostfound.utils.ImageUtil;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.text.SimpleDateFormat;
import javax.imageio.ImageIO;

/**
 * ItemDetailsDialog
 * 
 * Displays complete item details including:
 * - High-quality photo thumbnail with "View Full Photo" preview modal
 * - Extended details: Building, Floor / Room, and Campus Location
 * - Reporter information, category, type, and status badges
 */
public class ItemDetailsDialog extends JDialog {

    private final Item item;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ItemDetailsDialog(Window owner, Item item) {
        super(owner, "Item Details - #" + item.getItemId(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        initializeUI();
    }

    private void initializeUI() {
        setSize(650, 620);
        setMinimumSize(new Dimension(550, 520));
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout(0, 10));

        // 1. Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);

        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_LIGHT);

        JLabel typeBadge = new JLabel(" " + item.getItemType().name() + " ");
        typeBadge.setFont(UITheme.FONT_SMALL);
        typeBadge.setOpaque(true);
        if (item.getItemType() == Item.ItemType.LOST) {
            typeBadge.setBackground(new Color(254, 226, 226));
            typeBadge.setForeground(new Color(220, 38, 38));
        } else {
            typeBadge.setBackground(new Color(220, 252, 231));
            typeBadge.setForeground(new Color(22, 163, 74));
        }

        JLabel statusBadge = new JLabel(" " + item.getStatus().name() + " ");
        statusBadge.setFont(UITheme.FONT_SMALL);
        statusBadge.setOpaque(true);
        if (item.getStatus() == Item.ItemStatus.OPEN) {
            statusBadge.setBackground(new Color(219, 234, 254));
            statusBadge.setForeground(new Color(37, 99, 235));
        } else if (item.getStatus() == Item.ItemStatus.CLAIMED) {
            statusBadge.setBackground(new Color(254, 243, 199));
            statusBadge.setForeground(new Color(217, 119, 6));
        } else {
            statusBadge.setBackground(new Color(241, 245, 249));
            statusBadge.setForeground(new Color(100, 116, 139));
        }

        titleRow.add(titleLabel);
        titleRow.add(typeBadge);
        titleRow.add(statusBadge);

        JLabel subtitleLabel = new JLabel("Item ID: #" + item.getItemId() + " | Category: " + (item.getCategory() != null ? item.getCategory() : "Unspecified"));
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(new Color(224, 231, 255));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 0, 0));

        headerPanel.add(titleRow);
        headerPanel.add(subtitleLabel);

        // 2. Main Body (Left: Photo, Right: Info)
        JPanel contentCard = UITheme.createCardPanel(12);
        contentCard.setLayout(new BorderLayout(16, 16));
        contentCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Photo Panel (Left)
        JPanel photoPanel = new JPanel(new BorderLayout(0, 8));
        photoPanel.setOpaque(false);
        photoPanel.setPreferredSize(new Dimension(200, 260));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(190, 190));
        imageLabel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        imageLabel.setBackground(new Color(248, 250, 252));
        imageLabel.setOpaque(true);

        boolean hasImage = false;
        ImageIcon thumbnail = null;
        if (item.getImagePath() != null && !item.getImagePath().trim().isEmpty()) {
            thumbnail = ImageUtil.loadThumbnail(item.getImagePath(), 190, 190);
            if (thumbnail != null) {
                imageLabel.setIcon(thumbnail);
                hasImage = true;
            }
        }

        if (!hasImage) {
            imageLabel.setIcon(CampusIcons.get(CampusIcons.IconType.CAMERA, 48, 48, new Color(148, 163, 184)));
            imageLabel.setText("No Photo Available");
            imageLabel.setFont(UITheme.FONT_SMALL);
            imageLabel.setForeground(new Color(148, 163, 184));
            imageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
            imageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            imageLabel.setIconTextGap(8);
        }

        photoPanel.add(imageLabel, BorderLayout.CENTER);

        if (hasImage) {
            RoundedButton viewFullBtn = new RoundedButton("View Full Photo", RoundedButton.ButtonType.SECONDARY);
            viewFullBtn.setFont(UITheme.FONT_SMALL);
            viewFullBtn.addActionListener(e -> showFullImageDialog(item.getImagePath()));
            photoPanel.add(viewFullBtn, BorderLayout.SOUTH);
        }

        // Details Grid Panel (Right)
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 12);
        gbc.gridx = 0;
        gbc.gridy = 0;

        addDetailRow(detailsPanel, gbc, "Campus Location:", item.getLocation() != null ? item.getLocation() : "Unspecified");
        
        if (item.getBuilding() != null && !item.getBuilding().trim().isEmpty()) {
            addDetailRow(detailsPanel, gbc, "Building:", item.getBuilding());
        }
        if (item.getFloorRoom() != null && !item.getFloorRoom().trim().isEmpty()) {
            addDetailRow(detailsPanel, gbc, "Floor / Room:", item.getFloorRoom());
        }

        String reporter = item.getReporterName() != null ? item.getReporterName() : "Student #" + item.getReportedBy();
        addDetailRow(detailsPanel, gbc, "Reported By:", reporter);

        String dateStr = item.getReportDate() != null ? dateFormat.format(item.getReportDate()) : "N/A";
        addDetailRow(detailsPanel, gbc, "Date Reported:", dateStr);

        // Description area
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 4, 4, 4);

        JLabel descHeader = new JLabel("Description & Identifying Marks:");
        descHeader.setFont(UITheme.FONT_LABEL);
        descHeader.setForeground(UITheme.TEXT_PRIMARY);
        detailsPanel.add(descHeader, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        JTextArea descArea = new JTextArea(4, 24);
        descArea.setText(item.getDescription() != null && !item.getDescription().trim().isEmpty() 
                ? item.getDescription() 
                : "(No additional description provided)");
        descArea.setFont(UITheme.FONT_REGULAR);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(new Color(248, 250, 252));
        descArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        detailsPanel.add(descScroll, gbc);

        contentCard.add(photoPanel, BorderLayout.WEST);
        contentCard.add(detailsPanel, BorderLayout.CENTER);

        // 3. Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footerPanel.setBackground(UITheme.BG_MAIN);

        RoundedButton contactBtn = new RoundedButton("Contact Finder / Owner", RoundedButton.ButtonType.ACCENT);
        contactBtn.addActionListener(e -> handleContactAction());
        footerPanel.add(contactBtn);

        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.ButtonType.SECONDARY);
        closeBtn.addActionListener(e -> dispose());
        footerPanel.add(closeBtn);

        // Assemble
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_MAIN);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        wrapper.add(contentCard, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(UITheme.FONT_REGULAR);
        valLbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(valLbl, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
    }

    private void showFullImageDialog(String relPath) {
        File file = ImageUtil.resolveStoragePath(relPath);
        if (!file.exists()) return;

        try {
            Image fullImage = ImageIO.read(file);
            if (fullImage == null) return;

            JDialog viewer = new JDialog(this, "Photo Preview - " + item.getTitle(), ModalityType.APPLICATION_MODAL);
            viewer.setSize(Math.min(fullImage.getWidth(null) + 40, 800), Math.min(fullImage.getHeight(null) + 80, 700));
            viewer.setLocationRelativeTo(this);
            viewer.setLayout(new BorderLayout());

            JLabel fullLabel = new JLabel(new ImageIcon(fullImage));
            JScrollPane scrollPane = new JScrollPane(fullLabel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());

            viewer.add(scrollPane, BorderLayout.CENTER);

            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            RoundedButton closeViewer = new RoundedButton("Close", RoundedButton.ButtonType.PRIMARY);
            closeViewer.addActionListener(e -> viewer.dispose());
            btnBar.add(closeViewer);

            viewer.add(btnBar, BorderLayout.SOUTH);
            viewer.setVisible(true);
        } catch (Exception ex) {
            System.err.println("[ItemDetailsDialog] Error opening full image: " + ex.getMessage());
        }
    }

    private void handleContactAction() {
        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Please log in to contact student peers.",
                    "Login Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. If item is RETURNED
        if (item.getStatus() == Item.ItemStatus.RETURNED) {
            JOptionPane.showMessageDialog(this,
                    "Contact details are no longer available.",
                    "Contact Details", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        UserDAO userDAO = new UserDAO();
        User peer = userDAO.getAuthorizedPeerContactForItem(currentUserId, item.getItemId());
        if (peer != null) {
            String role = (item.getItemType() == Item.ItemType.FOUND && item.getReportedBy() == peer.getUserId())
                    ? "Verified Finder"
                    : "Approved Owner";
            new ContactDetailsDialog(this, peer, item, role).setVisible(true);
            return;
        }

        // Check if there is an approved claim or rejected or pending
        com.campus.lostfound.dao.ClaimDAO claimDAO = new com.campus.lostfound.dao.ClaimDAO();
        com.campus.lostfound.models.Claim claim = claimDAO.getLatestClaimForItem(item.getItemId());

        if (claim != null && claim.getStatus() == com.campus.lostfound.models.Claim.ClaimStatus.REJECTED) {
            JOptionPane.showMessageDialog(this,
                    "Contact details are no longer available.",
                    "Contact Details", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Contact details are hidden until the Admin approves the claim.",
                    "Contact Details", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
