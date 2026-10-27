package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * MyClaimsPanel
 * 
 * Displays all ownership claims submitted by the logged-in student.
 * Shows administrator review decisions (PENDING, APPROVED, REJECTED) in real-time.
 */
public class MyClaimsPanel extends JPanel {

    private final ClaimDAO claimDAO;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private RoundedButton detailsButton;
    private RoundedButton refreshButton;

    private List<Claim> userClaims = new ArrayList<>();

    public MyClaimsPanel() {
        this.claimDAO = new ClaimDAO();
        initializeUI();
        loadUserClaims();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top info header
        JPanel headerPanel = UITheme.createCardPanel(10);
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));

        JLabel titleLbl = new JLabel("My Submitted Claims");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel infoLbl = new JLabel("(Status of your ownership claims reviewed by campus administration)");
        infoLbl.setFont(UITheme.FONT_SMALL);
        infoLbl.setForeground(UITheme.TEXT_SECONDARY);

        headerPanel.add(titleLbl);
        headerPanel.add(infoLbl);

        // Center Table
        String[] columns = {"Claim ID", "Item ID", "Item Title", "Proof / Verification Message", "Admin Status", "Date Submitted"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(UITheme.FONT_REGULAR);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(UITheme.FONT_BOLD);
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        table.setShowVerticalLines(false);

        // Status color renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    // Admin Status (col 4)
                    if (col == 4 && val != null) {
                        String st = val.toString();
                        setFont(getFont().deriveFont(Font.BOLD));
                        if ("APPROVED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(22, 163, 74)); // Green
                        } else if ("REJECTED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                        } else {
                            c.setForeground(new Color(217, 119, 6)); // Amber / Pending
                        }
                    }
                }
                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(65);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(280);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        table.getSelectionModel().addListSelectionListener(e -> {
            detailsButton.setEnabled(table.getSelectedRow() >= 0);
        });

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Loading your claims...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonBar.setOpaque(false);

        detailsButton = new RoundedButton("View Claim Details", RoundedButton.ButtonType.OUTLINE);
        detailsButton.setEnabled(false);
        detailsButton.addActionListener(e -> handleViewDetails());

        refreshButton = new RoundedButton("Refresh", RoundedButton.ButtonType.SECONDARY);
        refreshButton.addActionListener(e -> loadUserClaims());

        buttonBar.add(detailsButton);
        buttonBar.add(refreshButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void loadUserClaims() {
        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0) return;

        statusLabel.setText("Loading your claims...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Claim>, Void>() {
            @Override
            protected List<Claim> doInBackground() {
                return claimDAO.getClaimsByUser(currentUserId);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    userClaims = get();
                    tableModel.setRowCount(0);
                    for (Claim c : userClaims) {
                        String dateStr = c.getClaimDate() != null ? dateFormat.format(c.getClaimDate()) : "N/A";
                        String itemTitle = c.getItemTitle() != null ? c.getItemTitle() : "Item #" + c.getItemId();

                        tableModel.addRow(new Object[]{
                                c.getClaimId(),
                                c.getItemId(),
                                itemTitle,
                                c.getMessage(),
                                c.getStatus(),
                                dateStr
                        });
                    }
                    statusLabel.setText("You have submitted " + userClaims.size() + " claim(s).");
                    detailsButton.setEnabled(false);
                } catch (Exception ex) {
                    statusLabel.setText("Error loading claims: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleViewDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= userClaims.size()) return;

        Claim claim = userClaims.get(row);
        String dateStr = claim.getClaimDate() != null ? dateFormat.format(claim.getClaimDate()) : "N/A";
        String itemTitle = claim.getItemTitle() != null ? claim.getItemTitle() : "Item #" + claim.getItemId();

        String statusNotice;
        if (claim.getStatus() == Claim.ClaimStatus.APPROVED) {
            statusNotice = "✓ Claim APPROVED! Please visit the Campus Security / Lost & Found Office to collect your item.";
        } else if (claim.getStatus() == Claim.ClaimStatus.REJECTED) {
            statusNotice = "✗ Claim REJECTED. The proof provided did not match the item specifications.";
        } else {
            statusNotice = "⏳ Claim PENDING. An administrator is currently reviewing your claim.";
        }

        String details = String.format(
                "Claim ID: %d\n" +
                "Item ID: %d\n" +
                "Item Title: %s\n" +
                "Submission Date: %s\n\n" +
                "Status: %s\n%s\n\n" +
                "Your Submitted Proof Message:\n%s",
                claim.getClaimId(),
                claim.getItemId(),
                itemTitle,
                dateStr,
                claim.getStatus(),
                statusNotice,
                claim.getMessage()
        );

        JOptionPane.showMessageDialog(this, details, "Claim Details - #" + claim.getClaimId(), JOptionPane.INFORMATION_MESSAGE);
    }
}
