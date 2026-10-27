package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.MatchResult;
import com.campus.lostfound.ui.common.ItemDetailsDialog;
import com.campus.lostfound.ui.common.MatchResultsDialog;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.ItemMatchingEngine;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * MyReportedItemsPanel
 * 
 * Displays only the items reported by the currently logged-in student.
 * Allows the student to manage their reports (mark as resolved/returned or delete).
 */
public class MyReportedItemsPanel extends JPanel {

    private final ItemDAO itemDAO;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private RoundedButton viewDetailsButton;
    private RoundedButton checkMatchesButton;
    private RoundedButton resolveButton;
    private RoundedButton deleteButton;
    private RoundedButton refreshButton;

    private List<Item> userItems = new ArrayList<>();

    public MyReportedItemsPanel() {
        this.itemDAO = new ItemDAO();
        initializeUI();
        loadUserItems();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top info header
        JPanel headerPanel = UITheme.createCardPanel(10);
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));

        JLabel titleLbl = new JLabel("My Reported Items");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel infoLbl = new JLabel("(Items you have reported as lost or found)");
        infoLbl.setFont(UITheme.FONT_SMALL);
        infoLbl.setForeground(UITheme.TEXT_SECONDARY);

        headerPanel.add(titleLbl);
        headerPanel.add(infoLbl);

        // Center Table
        String[] columns = {"Item ID", "Type", "Title", "Category", "Location", "Status", "Date Reported"};
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

        // Badge renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    // Type color
                    if (col == 1 && val != null) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        c.setForeground("LOST".equalsIgnoreCase(val.toString()) ? new Color(220, 38, 38) : new Color(22, 163, 74));
                    }
                    // Status color
                    if (col == 5 && val != null) {
                        String st = val.toString();
                        if ("RETURNED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(22, 163, 74));
                        } else if ("CLAIMED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(217, 119, 6));
                        } else {
                            c.setForeground(new Color(37, 99, 235));
                        }
                    }
                }
                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        table.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    handleViewDetails();
                }
            }
        });

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Loading your reported items...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonBar.setOpaque(false);

        viewDetailsButton = new RoundedButton("View Details", RoundedButton.ButtonType.SECONDARY);
        viewDetailsButton.setEnabled(false);
        viewDetailsButton.addActionListener(e -> handleViewDetails());

        checkMatchesButton = new RoundedButton("Check Matches", RoundedButton.ButtonType.PRIMARY);
        checkMatchesButton.setEnabled(false);
        checkMatchesButton.addActionListener(e -> handleCheckMatches());

        resolveButton = new RoundedButton("Mark as Returned / Resolved", RoundedButton.ButtonType.ACCENT);
        resolveButton.setEnabled(false);
        resolveButton.addActionListener(e -> handleMarkResolved());

        deleteButton = new RoundedButton("Delete Report", RoundedButton.ButtonType.DANGER);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> handleDeleteReport());

        refreshButton = new RoundedButton("Refresh", RoundedButton.ButtonType.SECONDARY);
        refreshButton.addActionListener(e -> loadUserItems());

        buttonBar.add(viewDetailsButton);
        buttonBar.add(checkMatchesButton);
        buttonBar.add(resolveButton);
        buttonBar.add(deleteButton);
        buttonBar.add(refreshButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void loadUserItems() {
        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0) return;

        statusLabel.setText("Loading your items...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Item>, Void>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.getItemsByReporter(currentUserId);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    userItems = get();
                    tableModel.setRowCount(0);
                    for (Item item : userItems) {
                        String dateStr = item.getReportDate() != null ? dateFormat.format(item.getReportDate()) : "N/A";
                        tableModel.addRow(new Object[]{
                                item.getItemId(),
                                item.getItemType(),
                                item.getTitle(),
                                item.getCategory() != null ? item.getCategory() : "N/A",
                                item.getLocation() != null ? item.getLocation() : "N/A",
                                item.getStatus(),
                                dateStr
                        });
                    }
                    statusLabel.setText("You have reported " + userItems.size() + " items.");
                    updateButtonStates();
                } catch (Exception ex) {
                    statusLabel.setText("Error loading items: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateButtonStates() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < userItems.size()) {
            Item item = userItems.get(row);
            viewDetailsButton.setEnabled(true);
            deleteButton.setEnabled(true);
            resolveButton.setEnabled(item.getStatus() != Item.ItemStatus.RETURNED);
            // Matches can be checked on OPEN items
            checkMatchesButton.setEnabled(item.getStatus() == Item.ItemStatus.OPEN);
        } else {
            viewDetailsButton.setEnabled(false);
            deleteButton.setEnabled(false);
            resolveButton.setEnabled(false);
            checkMatchesButton.setEnabled(false);
        }
    }

    private void handleViewDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= userItems.size()) return;
        Item selectedItem = userItems.get(row);

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ItemDetailsDialog dialog = new ItemDetailsDialog(parentWindow, selectedItem);
        dialog.setVisible(true);
    }

    private void handleCheckMatches() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= userItems.size()) return;
        Item selectedItem = userItems.get(row);

        statusLabel.setText("Searching smart matches for item #" + selectedItem.getItemId() + "...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        checkMatchesButton.setEnabled(false);

        new SwingWorker<List<MatchResult>, Void>() {
            @Override
            protected List<MatchResult> doInBackground() {
                // Fetch eligible opposite-type open items from database
                List<Item> candidates = itemDAO.getEligibleCandidateItems(
                        selectedItem.getItemType(),
                        selectedItem.getReportedBy(),
                        selectedItem.getItemId()
                );
                // Compute deterministic similarity scores
                return ItemMatchingEngine.findMatches(selectedItem, candidates);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                updateButtonStates();
                try {
                    List<MatchResult> results = get();
                    statusLabel.setText("Found " + results.size() + " potential match(es) for #" + selectedItem.getItemId());

                    Window parentWindow = SwingUtilities.getWindowAncestor(MyReportedItemsPanel.this);
                    MatchResultsDialog dialog = new MatchResultsDialog(parentWindow, selectedItem, results);
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    statusLabel.setText("Error calculating matches: " + ex.getMessage());
                    JOptionPane.showMessageDialog(MyReportedItemsPanel.this,
                            "Failed to calculate smart matches: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleMarkResolved() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= userItems.size()) return;

        Item item = userItems.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to mark '" + item.getTitle() + "' as RETURNED / RESOLVED?",
                "Confirm Resolution",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return itemDAO.updateStatus(item.getItemId(), Item.ItemStatus.RETURNED);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(MyReportedItemsPanel.this, "Item marked as RETURNED.", "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                            loadUserItems();
                        } else {
                            JOptionPane.showMessageDialog(MyReportedItemsPanel.this, "Failed to update item status.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MyReportedItemsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleDeleteReport() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= userItems.size()) return;

        Item item = userItems.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete report for '" + item.getTitle() + "'?\nThis action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return itemDAO.deleteItem(item.getItemId());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(MyReportedItemsPanel.this, "Item report deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                            loadUserItems();
                        } else {
                            JOptionPane.showMessageDialog(MyReportedItemsPanel.this, "Failed to delete item report.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MyReportedItemsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
