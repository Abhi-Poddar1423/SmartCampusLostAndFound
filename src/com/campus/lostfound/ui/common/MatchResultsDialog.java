package com.campus.lostfound.ui.common;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.MatchResult;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.student.SubmitClaimDialog;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * MatchResultsDialog
 * 
 * Displays ranked potential matches for a target item using FlatLaf styling.
 * Provides transparent score breakdown (Category 40%, Location 25%, Keywords 35%),
 * matched keyword highlights, candidate item inspection, and direct claim shortcut.
 */
public class MatchResultsDialog extends JDialog {

    private final Item targetItem;
    private final List<MatchResult> matchResults;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private JTable matchTable;
    private DefaultTableModel tableModel;
    private JTextArea detailsArea;
    private JLabel breakdownLabel;
    private JLabel scoreBadgeLabel;

    private RoundedButton claimButton;
    private RoundedButton viewDetailsButton;
    private RoundedButton closeButton;

    public MatchResultsDialog(Window owner, Item targetItem, List<MatchResult> matchResults) {
        super(owner, "Smart Item Matches - Smart Campus Lost & Found", ModalityType.APPLICATION_MODAL);
        this.targetItem = targetItem;
        this.matchResults = matchResults != null ? matchResults : List.of();
        initializeUI();
    }

    private void initializeUI() {
        setSize(880, 640);
        setMinimumSize(new Dimension(740, 520));
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout(12, 12));

        // 1. Top Target Item Card
        JPanel headerCard = UITheme.createCardPanel(14);
        headerCard.setLayout(new BorderLayout(10, 6));

        JPanel headerTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerTop.setOpaque(false);

        JLabel headerTitle = new JLabel("Smart Match Recommendations");
        headerTitle.setFont(UITheme.FONT_HEADER);
        headerTitle.setForeground(UITheme.PRIMARY);

        JLabel typeBadge = new JLabel(" " + targetItem.getItemType().name() + " ITEM ");
        typeBadge.setFont(UITheme.FONT_SMALL);
        typeBadge.setOpaque(true);
        if (targetItem.getItemType() == Item.ItemType.LOST) {
            typeBadge.setBackground(new Color(254, 226, 226));
            typeBadge.setForeground(new Color(220, 38, 38));
        } else {
            typeBadge.setBackground(new Color(220, 252, 231));
            typeBadge.setForeground(new Color(22, 163, 74));
        }

        headerTop.add(headerTitle);
        headerTop.add(typeBadge);

        JLabel targetDetails = new JLabel(String.format(
                "<html><b>Target:</b> #%d - %s &nbsp;|&nbsp; <b>Category:</b> %s &nbsp;|&nbsp; <b>Location:</b> %s</html>",
                targetItem.getItemId(),
                escapeHtml(targetItem.getTitle()),
                escapeHtml(targetItem.getCategory() != null ? targetItem.getCategory() : "Unspecified"),
                escapeHtml(targetItem.getLocation() != null ? targetItem.getLocation() : "Unspecified")
        ));
        targetDetails.setFont(UITheme.FONT_REGULAR);
        targetDetails.setForeground(UITheme.TEXT_SECONDARY);

        headerCard.add(headerTop, BorderLayout.NORTH);
        headerCard.add(targetDetails, BorderLayout.SOUTH);

        // 2. Center Table & Detail Panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        String[] columns = {"#", "Match Score", "Confidence Tier", "Type", "Title", "Category", "Location", "Date Reported"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        matchTable = new JTable(tableModel);
        matchTable.setFont(UITheme.FONT_REGULAR);
        matchTable.setRowHeight(32);
        matchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matchTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        matchTable.getTableHeader().setBackground(new Color(241, 245, 249));
        matchTable.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        matchTable.setShowVerticalLines(false);

        // Column widths
        matchTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        matchTable.getColumnModel().getColumn(1).setPreferredWidth(95);
        matchTable.getColumnModel().getColumn(2).setPreferredWidth(115);
        matchTable.getColumnModel().getColumn(3).setPreferredWidth(75);
        matchTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        matchTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        matchTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        matchTable.getColumnModel().getColumn(7).setPreferredWidth(120);

        // Custom renderer for score badge and colors
        matchTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    // Score coloring
                    if (col == 1 && val != null) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        String str = val.toString().replace("%", "").trim();
                        try {
                            double s = Double.parseDouble(str);
                            if (s >= 75) c.setForeground(new Color(22, 163, 74));
                            else if (s >= 50) c.setForeground(new Color(37, 99, 235));
                            else if (s >= 30) c.setForeground(new Color(217, 119, 6));
                            else c.setForeground(new Color(100, 116, 139));
                        } catch (NumberFormatException ignored) {}
                    }
                    // Type coloring
                    if (col == 3 && val != null) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        c.setForeground("LOST".equalsIgnoreCase(val.toString()) ? new Color(220, 38, 38) : new Color(22, 163, 74));
                    }
                }
                return c;
            }
        });

        matchTable.getSelectionModel().addListSelectionListener(e -> updateSelectedMatchDetails());

        JScrollPane scrollPane = new JScrollPane(matchTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Populate Table Rows
        int rank = 1;
        for (MatchResult res : matchResults) {
            Item cand = res.getCandidateItem();
            String dateStr = cand.getReportDate() != null ? dateFormat.format(cand.getReportDate()) : "N/A";
            tableModel.addRow(new Object[]{
                    rank++,
                    res.getFormattedScore(),
                    res.getMatchConfidenceTier(),
                    cand.getItemType().name(),
                    cand.getTitle(),
                    cand.getCategory() != null ? cand.getCategory() : "N/A",
                    cand.getLocation() != null ? cand.getLocation() : "N/A",
                    dateStr
            });
        }

        // 3. Match Breakdown & Inspection Card (Below Table)
        JPanel breakdownCard = UITheme.createCardPanel(12);
        breakdownCard.setLayout(new BorderLayout(8, 8));

        JPanel breakdownHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        breakdownHeader.setOpaque(false);

        JLabel expTitle = new JLabel("Match Analysis & Score Breakdown:");
        expTitle.setFont(UITheme.FONT_BOLD);
        expTitle.setForeground(UITheme.TEXT_PRIMARY);

        scoreBadgeLabel = new JLabel("Select an item to view score details");
        scoreBadgeLabel.setFont(UITheme.FONT_SMALL);
        scoreBadgeLabel.setForeground(UITheme.TEXT_SECONDARY);

        breakdownHeader.add(expTitle);
        breakdownHeader.add(scoreBadgeLabel);

        breakdownLabel = new JLabel(" ");
        breakdownLabel.setFont(UITheme.FONT_REGULAR);
        breakdownLabel.setForeground(UITheme.PRIMARY);

        detailsArea = new JTextArea(3, 40);
        detailsArea.setFont(UITheme.FONT_SMALL);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBackground(new Color(248, 250, 252));
        detailsArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        breakdownCard.add(breakdownHeader, BorderLayout.NORTH);
        breakdownCard.add(breakdownLabel, BorderLayout.CENTER);
        breakdownCard.add(new JScrollPane(detailsArea), BorderLayout.SOUTH);

        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(breakdownCard, BorderLayout.SOUTH);

        // 4. Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6, 16, 14, 16));

        JLabel countSummary = new JLabel(matchResults.isEmpty() ?
                "No candidate items met the minimum similarity threshold." :
                String.format("Displaying %d potential match(es) sorted by similarity.", matchResults.size()));
        countSummary.setFont(UITheme.FONT_SMALL);
        countSummary.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonGroup.setOpaque(false);

        claimButton = new RoundedButton("Claim Found Item", RoundedButton.ButtonType.PRIMARY);
        claimButton.setEnabled(false);
        claimButton.addActionListener(e -> handleClaimMatch());

        viewDetailsButton = new RoundedButton("View Candidate Details", RoundedButton.ButtonType.OUTLINE);
        viewDetailsButton.setEnabled(false);
        viewDetailsButton.addActionListener(e -> handleViewDetails());

        closeButton = new RoundedButton("Close", RoundedButton.ButtonType.SECONDARY);
        closeButton.addActionListener(e -> dispose());

        buttonGroup.add(claimButton);
        buttonGroup.add(viewDetailsButton);
        buttonGroup.add(closeButton);

        bottomBar.add(countSummary, BorderLayout.WEST);
        bottomBar.add(buttonGroup, BorderLayout.EAST);

        // Assemble Dialog
        add(headerCard, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        // Auto-select first row if available
        if (!matchResults.isEmpty()) {
            matchTable.setRowSelectionInterval(0, 0);
        }
    }

    private MatchResult getSelectedMatch() {
        int row = matchTable.getSelectedRow();
        if (row >= 0 && row < matchResults.size()) {
            return matchResults.get(row);
        }
        return null;
    }

    private void updateSelectedMatchDetails() {
        MatchResult match = getSelectedMatch();
        if (match == null) {
            scoreBadgeLabel.setText("Select an item to view score details");
            breakdownLabel.setText(" ");
            detailsArea.setText("");
            claimButton.setEnabled(false);
            viewDetailsButton.setEnabled(false);
            return;
        }

        Item cand = match.getCandidateItem();
        scoreBadgeLabel.setText(String.format("Match Score: %s (%s)", match.getFormattedScore(), match.getMatchConfidenceTier()));
        breakdownLabel.setText(String.format("Score Components: Category: %.0f/40 | Location: %.0f/25 | Keywords: %.0f/35",
                match.getCategoryScore(), match.getLocationScore(), match.getTextScore()));

        StringBuilder sb = new StringBuilder();
        sb.append("Description: ").append(cand.getDescription() != null && !cand.getDescription().isEmpty() ? cand.getDescription() : "(No description provided)").append("\n");
        sb.append("Analysis: ").append(match.getExplanation());
        detailsArea.setText(sb.toString());

        viewDetailsButton.setEnabled(true);

        // Can only claim if candidate is a FOUND item and logged in user is a student and not the reporter
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean canClaim = (cand.getItemType() == Item.ItemType.FOUND)
                && (cand.getStatus() == Item.ItemStatus.OPEN)
                && (currentUser != null && currentUser.getRole() == User.Role.STUDENT)
                && (cand.getReportedBy() != currentUser.getUserId());

        claimButton.setEnabled(canClaim);
    }

    private void handleViewDetails() {
        MatchResult match = getSelectedMatch();
        if (match == null) return;

        Item cand = match.getCandidateItem();
        String message = String.format(
                "Item ID: #%d\n" +
                "Title: %s\n" +
                "Type: %s\n" +
                "Category: %s\n" +
                "Location: %s\n" +
                "Status: %s\n" +
                "Reported Date: %s\n" +
                "Reported By: %s\n\n" +
                "Description:\n%s\n\n" +
                "Smart Match Breakdown:\n" +
                "• Match Score: %s (%s)\n" +
                "• %s",
                cand.getItemId(),
                cand.getTitle(),
                cand.getItemType(),
                cand.getCategory() != null ? cand.getCategory() : "N/A",
                cand.getLocation() != null ? cand.getLocation() : "N/A",
                cand.getStatus(),
                cand.getReportDate() != null ? dateFormat.format(cand.getReportDate()) : "N/A",
                cand.getReporterName() != null ? cand.getReporterName() : "Student #" + cand.getReportedBy(),
                cand.getDescription() != null && !cand.getDescription().isEmpty() ? cand.getDescription() : "(None)",
                match.getFormattedScore(),
                match.getMatchConfidenceTier(),
                match.getExplanation()
        );

        JOptionPane.showMessageDialog(this, message, "Item Match Details - #" + cand.getItemId(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleClaimMatch() {
        MatchResult match = getSelectedMatch();
        if (match == null) return;

        Item cand = match.getCandidateItem();
        if (cand.getItemType() != Item.ItemType.FOUND) {
            JOptionPane.showMessageDialog(this, "Claims can only be filed against FOUND items.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
        SubmitClaimDialog claimDialog = new SubmitClaimDialog(parentFrame, cand, () -> {
            JOptionPane.showMessageDialog(this, "Your claim was submitted successfully for review!", "Claim Submitted", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
        claimDialog.setVisible(true);
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
