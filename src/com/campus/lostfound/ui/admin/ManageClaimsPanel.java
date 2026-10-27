package com.campus.lostfound.ui.admin;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ManageClaimsPanel
 * 
 * Administrative moderation panel for student ownership claims.
 * Allows approving or rejecting claims with atomic transactional synchronization
 * between claim records and item status, preventing conflicting approved claims.
 */
public class ManageClaimsPanel extends JPanel {

    private final ClaimDAO claimDAO;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final Runnable onDataChangedCallback;

    private JComboBox<String> statusFilterCombo;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private RoundedButton approveButton;
    private RoundedButton rejectButton;
    private RoundedButton viewProofButton;
    private RoundedButton refreshButton;

    private List<Claim> allClaims = new ArrayList<>();
    private List<Claim> displayedClaims = new ArrayList<>();

    public ManageClaimsPanel(Runnable onDataChangedCallback) {
        this.claimDAO = new ClaimDAO();
        this.onDataChangedCallback = onDataChangedCallback;
        initializeUI();
        loadClaimsAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Filter Bar
        JPanel filterCard = UITheme.createCardPanel(10);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));

        JLabel titleLbl = new JLabel("Claim Moderation & Adjudication");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel filterLbl = new JLabel("Filter Status:");
        filterLbl.setFont(UITheme.FONT_LABEL);

        statusFilterCombo = new JComboBox<>(new String[]{"ALL", "PENDING", "APPROVED", "REJECTED"});
        statusFilterCombo.setFont(UITheme.FONT_REGULAR);
        statusFilterCombo.setPreferredSize(new Dimension(130, 36));
        statusFilterCombo.addActionListener(e -> applyFilter());

        filterCard.add(titleLbl);
        filterCard.add(Box.createHorizontalStrut(20));
        filterCard.add(filterLbl);
        filterCard.add(statusFilterCombo);

        // Center Table
        String[] columns = {"Claim ID", "Item ID", "Item Title", "Claimant Name", "Claimant Email", "Status", "Date Submitted"};
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

        // Status Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    // Status column (col 5)
                    if (col == 5 && val != null) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        String st = val.toString();
                        if ("APPROVED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(22, 163, 74));
                        } else if ("REJECTED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(220, 38, 38));
                        } else {
                            c.setForeground(new Color(217, 119, 6)); // Amber / Pending
                        }
                    }
                }
                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(180);
        table.getColumnModel().getColumn(5).setPreferredWidth(95);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        table.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Loading claims...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonBar.setOpaque(false);

        viewProofButton = new RoundedButton("View Proof Details", RoundedButton.ButtonType.OUTLINE);
        viewProofButton.setEnabled(false);
        viewProofButton.addActionListener(e -> handleViewProof());

        approveButton = new RoundedButton("Approve Claim", RoundedButton.ButtonType.PRIMARY);
        approveButton.setEnabled(false);
        approveButton.addActionListener(e -> handleApproveClaim());

        rejectButton = new RoundedButton("Reject Claim", RoundedButton.ButtonType.DANGER);
        rejectButton.setEnabled(false);
        rejectButton.addActionListener(e -> handleRejectClaim());

        refreshButton = new RoundedButton("Refresh", RoundedButton.ButtonType.SECONDARY);
        refreshButton.addActionListener(e -> loadClaimsAsync());

        buttonBar.add(viewProofButton);
        buttonBar.add(approveButton);
        buttonBar.add(rejectButton);
        buttonBar.add(refreshButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);

        add(filterCard, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshClaims() {
        loadClaimsAsync();
    }

    public void loadClaimsAsync() {
        statusLabel.setText("Fetching claims from database...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Claim>, Void>() {
            @Override
            protected List<Claim> doInBackground() {
                return claimDAO.getAllClaims();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    allClaims = get();
                    applyFilter();
                    if (onDataChangedCallback != null) {
                        onDataChangedCallback.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load claims: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String filter = (String) statusFilterCombo.getSelectedItem();
        displayedClaims = new ArrayList<>();

        for (Claim c : allClaims) {
            if ("ALL".equalsIgnoreCase(filter) || c.getStatus().name().equalsIgnoreCase(filter)) {
                displayedClaims.add(c);
            }
        }

        tableModel.setRowCount(0);
        for (Claim c : displayedClaims) {
            String dateStr = c.getClaimDate() != null ? dateFormat.format(c.getClaimDate()) : "N/A";
            String title = c.getItemTitle() != null ? c.getItemTitle() : "Item #" + c.getItemId();
            String name = c.getClaimantName() != null ? c.getClaimantName() : "User #" + c.getClaimedBy();
            String email = c.getClaimantEmail() != null ? c.getClaimantEmail() : "N/A";

            tableModel.addRow(new Object[]{
                    c.getClaimId(),
                    c.getItemId(),
                    title,
                    name,
                    email,
                    c.getStatus(),
                    dateStr
            });
        }

        statusLabel.setText("Displaying " + displayedClaims.size() + " claim(s) (Filter: " + filter + ")");
        updateButtonStates();
    }

    private Claim getSelectedClaim() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < displayedClaims.size()) {
            return displayedClaims.get(row);
        }
        return null;
    }

    private void updateButtonStates() {
        Claim claim = getSelectedClaim();
        if (claim == null) {
            viewProofButton.setEnabled(false);
            approveButton.setEnabled(false);
            rejectButton.setEnabled(false);
            return;
        }

        viewProofButton.setEnabled(true);
        // Can approve if not already approved
        approveButton.setEnabled(claim.getStatus() != Claim.ClaimStatus.APPROVED);
        // Can reject if not already rejected
        rejectButton.setEnabled(claim.getStatus() != Claim.ClaimStatus.REJECTED);
    }

    private void handleApproveClaim() {
        Claim claim = getSelectedClaim();
        if (claim == null) return;

        if (claim.getStatus() == Claim.ClaimStatus.APPROVED) {
            JOptionPane.showMessageDialog(this, "This claim is already APPROVED.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String confirmMsg = "Are you sure you want to APPROVE Claim #" + claim.getClaimId() + "?\n\n" +
                "Item: " + claim.getItemTitle() + " (#" + claim.getItemId() + ")\n" +
                "Claimant: " + claim.getClaimantName() + " (" + claim.getClaimantEmail() + ")\n\n" +
                "System Impact (Atomic Transaction):\n" +
                "• Claim status will become APPROVED\n" +
                "• Item lifecycle status will synchronize to CLAIMED\n" +
                "• Any competing pending claims on this item will be automatically marked REJECTED.";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                confirmMsg,
                "Confirm Claim Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Boolean, Void>() {
                private String errorReason = "";

                @Override
                protected Boolean doInBackground() {
                    try {
                        return claimDAO.approveClaimTransaction(claim.getClaimId(), claim.getItemId());
                    } catch (IllegalStateException ise) {
                        errorReason = ise.getMessage();
                        return false;
                    } catch (Exception ex) {
                        errorReason = "Database transaction error: " + ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(
                                    ManageClaimsPanel.this,
                                    "Claim #" + claim.getClaimId() + " successfully APPROVED!\n" +
                                    "Item status synchronized to CLAIMED.",
                                    "Approval Complete",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            loadClaimsAsync();
                        } else {
                            JOptionPane.showMessageDialog(ManageClaimsPanel.this, errorReason, "Approval Blocked", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ManageClaimsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleRejectClaim() {
        Claim claim = getSelectedClaim();
        if (claim == null) return;

        if (claim.getStatus() == Claim.ClaimStatus.REJECTED) {
            JOptionPane.showMessageDialog(this, "This claim is already REJECTED.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reject Claim #" + claim.getClaimId() + " by " + claim.getClaimantName() + "?\n" +
                "The claimant will see this status in their personal claims dashboard.",
                "Confirm Rejection",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return claimDAO.rejectClaim(claim.getClaimId());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(ManageClaimsPanel.this, "Claim marked as REJECTED.", "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                            loadClaimsAsync();
                        } else {
                            JOptionPane.showMessageDialog(ManageClaimsPanel.this, "Failed to reject claim.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ManageClaimsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleViewProof() {
        Claim claim = getSelectedClaim();
        if (claim == null) return;

        String dateStr = claim.getClaimDate() != null ? dateFormat.format(claim.getClaimDate()) : "N/A";
        String title = claim.getItemTitle() != null ? claim.getItemTitle() : "Item #" + claim.getItemId();
        String name = claim.getClaimantName() != null ? claim.getClaimantName() : "User #" + claim.getClaimedBy();
        String email = claim.getClaimantEmail() != null ? claim.getClaimantEmail() : "N/A";

        String details = String.format(
                "Claim ID: %d\n" +
                "Item ID: %d (%s)\n" +
                "Claimant: %s (%s)\n" +
                "Submission Date: %s\n" +
                "Current Review Status: %s\n\n" +
                "Claimant's Submitted Proof Message:\n" +
                "--------------------------------------------------\n" +
                "%s\n" +
                "--------------------------------------------------",
                claim.getClaimId(),
                claim.getItemId(),
                title,
                name,
                email,
                dateStr,
                claim.getStatus(),
                claim.getMessage() != null && !claim.getMessage().isEmpty() ? claim.getMessage() : "(No proof message entered)"
        );

        JOptionPane.showMessageDialog(this, details, "Proof Inspection - Claim #" + claim.getClaimId(), JOptionPane.INFORMATION_MESSAGE);
    }
}
