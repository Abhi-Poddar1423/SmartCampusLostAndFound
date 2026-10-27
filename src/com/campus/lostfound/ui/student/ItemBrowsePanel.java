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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
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
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemBrowsePanel
 * 
 * Main student browsing panel.
 * Provides multi-criteria search and filtering for campus items.
 * Uses SwingWorker for non-blocking database queries and enforces claim eligibility rules.
 */
public class ItemBrowsePanel extends JPanel {

    private final ItemDAO itemDAO;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> typeCombo;
    private JComboBox<String> statusCombo;
    private RoundedButton searchButton;
    private RoundedButton resetButton;

    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private RoundedButton claimButton;
    private RoundedButton detailsButton;
    private RoundedButton findMatchesButton;
    private RoundedButton refreshButton;

    private List<Item> currentItemList = new ArrayList<>();

    public ItemBrowsePanel() {
        this.itemDAO = new ItemDAO();
        initializeUI();
        loadItemsAsync(null, "ALL", "ALL", "ALL");
    }

    private void initializeUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Filter Bar Panel
        JPanel filterCard = UITheme.createCardPanel(10);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_LABEL);
        searchField = UITheme.createTextField(14);
        searchField.setToolTipText("Search title, description, or campus location");

        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(UITheme.FONT_LABEL);
        List<String> categories = new ArrayList<>();
        categories.add("ALL");
        for (String c : ReportItemDialog.CATEGORIES) {
            categories.add(c);
        }
        categoryCombo = new JComboBox<>(categories.toArray(new String[0]));
        categoryCombo.setFont(UITheme.FONT_REGULAR);
        categoryCombo.setPreferredSize(new Dimension(150, 36));

        JLabel typeLbl = new JLabel("Type:");
        typeLbl.setFont(UITheme.FONT_LABEL);
        typeCombo = new JComboBox<>(new String[]{"ALL", "LOST", "FOUND"});
        typeCombo.setFont(UITheme.FONT_REGULAR);
        typeCombo.setPreferredSize(new Dimension(90, 36));

        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UITheme.FONT_LABEL);
        statusCombo = new JComboBox<>(new String[]{"ALL", "OPEN", "CLAIMED", "RETURNED"});
        statusCombo.setFont(UITheme.FONT_REGULAR);
        statusCombo.setPreferredSize(new Dimension(110, 36));

        searchButton = new RoundedButton("Filter", RoundedButton.ButtonType.PRIMARY);
        searchButton.setPreferredSize(new Dimension(90, 36));
        searchButton.addActionListener(e -> applyFilters());

        resetButton = new RoundedButton("Reset", RoundedButton.ButtonType.OUTLINE);
        resetButton.setPreferredSize(new Dimension(80, 36));
        resetButton.addActionListener(e -> resetFilters());

        // Press Enter to search
        searchField.addActionListener(e -> applyFilters());

        filterCard.add(searchLbl);
        filterCard.add(searchField);
        filterCard.add(catLbl);
        filterCard.add(categoryCombo);
        filterCard.add(typeLbl);
        filterCard.add(typeCombo);
        filterCard.add(statusLbl);
        filterCard.add(statusCombo);
        filterCard.add(searchButton);
        filterCard.add(resetButton);

        // Center Table Panel
        String[] columnNames = {"ID", "Type", "Title", "Category", "Campus Location", "Status", "Reported By", "Report Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };

        itemTable = new JTable(tableModel);
        itemTable.setFont(UITheme.FONT_REGULAR);
        itemTable.setRowHeight(34);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        itemTable.getTableHeader().setBackground(new Color(241, 245, 249));
        itemTable.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        itemTable.setGridColor(UITheme.BORDER);
        itemTable.setShowVerticalLines(false);

        // Custom renderer for Type and Status badges
        itemTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    // Color code Type column (index 1)
                    if (col == 1 && value != null) {
                        String type = value.toString();
                        if ("LOST".equalsIgnoreCase(type)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else if ("FOUND".equalsIgnoreCase(type)) {
                            c.setForeground(new Color(22, 163, 74)); // Green
                            setFont(getFont().deriveFont(Font.BOLD));
                        }
                    }

                    // Color code Status column (index 5)
                    if (col == 5 && value != null) {
                        String st = value.toString();
                        if ("OPEN".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(37, 99, 235)); // Blue
                        } else if ("CLAIMED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(217, 119, 6)); // Amber
                        } else if ("RETURNED".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(22, 163, 74)); // Green
                        }
                    }
                }
                return c;
            }
        });

        // Set column widths
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // ID
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(70);  // Type
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(180); // Title
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(140); // Category
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Location
        itemTable.getColumnModel().getColumn(5).setPreferredWidth(85);  // Status
        itemTable.getColumnModel().getColumn(6).setPreferredWidth(120); // Reported By
        itemTable.getColumnModel().getColumn(7).setPreferredWidth(130); // Date

        JScrollPane tableScrollPane = new JScrollPane(itemTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        tableScrollPane.getViewport().setBackground(Color.WHITE);

        // Selection listener to update action buttons
        itemTable.getSelectionModel().addListSelectionListener(e -> updateActionButtonsState());

        // Bottom Action Bar Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Loading items...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonBar.setOpaque(false);

        claimButton = new RoundedButton("Claim Found Item", RoundedButton.ButtonType.PRIMARY);
        claimButton.setEnabled(false);
        claimButton.addActionListener(e -> handleClaimAction());

        detailsButton = new RoundedButton("View Details", RoundedButton.ButtonType.OUTLINE);
        detailsButton.setEnabled(false);
        detailsButton.addActionListener(e -> handleViewDetails());

        findMatchesButton = new RoundedButton("Find Matches", RoundedButton.ButtonType.ACCENT);
        findMatchesButton.setEnabled(false);
        findMatchesButton.addActionListener(e -> handleFindMatches());

        refreshButton = new RoundedButton("Refresh", RoundedButton.ButtonType.SECONDARY);
        refreshButton.addActionListener(e -> applyFilters());

        buttonBar.add(detailsButton);
        buttonBar.add(findMatchesButton);
        buttonBar.add(claimButton);
        buttonBar.add(refreshButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);

        // Layout Assembly
        add(filterCard, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshItems() {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        String itemType = (String) typeCombo.getSelectedItem();
        String status = (String) statusCombo.getSelectedItem();

        loadItemsAsync(keyword, category, itemType, status);
    }

    private void resetFilters() {
        searchField.setText("");
        categoryCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        loadItemsAsync(null, "ALL", "ALL", "ALL");
    }

    private void loadItemsAsync(String keyword, String category, String itemType, String status) {
        statusLabel.setText("Searching items from database...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Item>, Void>() {
            @Override
            protected List<Item> doInBackground() {
                return itemDAO.searchItems(keyword, category, itemType, status);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    currentItemList = get();
                    populateTable(currentItemList);
                    statusLabel.setText("Displaying " + currentItemList.size() + " items");
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load items: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateTable(List<Item> items) {
        tableModel.setRowCount(0);
        for (Item item : items) {
            String dateStr = item.getReportDate() != null ? dateFormat.format(item.getReportDate()) : "N/A";
            String reporter = item.getReporterName() != null ? item.getReporterName() : "Student #" + item.getReportedBy();

            tableModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getItemType(),
                    item.getTitle(),
                    item.getCategory() != null ? item.getCategory() : "N/A",
                    item.getLocation() != null ? item.getLocation() : "N/A",
                    item.getStatus(),
                    reporter,
                    dateStr
            });
        }
        updateActionButtonsState();
    }

    private Item getSelectedListItem() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentItemList.size()) {
            return currentItemList.get(selectedRow);
        }
        return null;
    }

    private void updateActionButtonsState() {
        Item item = getSelectedListItem();
        if (item == null) {
            claimButton.setEnabled(false);
            detailsButton.setEnabled(false);
            findMatchesButton.setEnabled(false);
            return;
        }

        detailsButton.setEnabled(true);
        findMatchesButton.setEnabled(item.getStatus() == Item.ItemStatus.OPEN);

        int currentUserId = SessionManager.getInstance().getUserId();
        // A student cannot claim:
        // 1. Their own reported item
        // 2. An item that is LOST (only FOUND items can be claimed)
        // 3. An item whose status is not OPEN
        boolean canClaim = (item.getItemType() == Item.ItemType.FOUND)
                && (item.getStatus() == Item.ItemStatus.OPEN)
                && (item.getReportedBy() != currentUserId);

        claimButton.setEnabled(canClaim);
    }

    private void handleFindMatches() {
        Item item = getSelectedListItem();
        if (item == null) return;

        statusLabel.setText("Finding smart matches for item #" + item.getItemId() + "...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        findMatchesButton.setEnabled(false);

        new SwingWorker<List<MatchResult>, Void>() {
            @Override
            protected List<MatchResult> doInBackground() {
                List<Item> candidates = itemDAO.getEligibleCandidateItems(
                        item.getItemType(),
                        item.getReportedBy(),
                        item.getItemId()
                );
                return ItemMatchingEngine.findMatches(item, candidates);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                updateActionButtonsState();
                try {
                    List<MatchResult> results = get();
                    statusLabel.setText("Found " + results.size() + " potential match(es) for #" + item.getItemId());

                    Window parentWindow = SwingUtilities.getWindowAncestor(ItemBrowsePanel.this);
                    MatchResultsDialog dialog = new MatchResultsDialog(parentWindow, item, results);
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    statusLabel.setText("Error finding matches: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ItemBrowsePanel.this,
                            "Failed to calculate smart matches: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleClaimAction() {
        Item item = getSelectedListItem();
        if (item == null) return;

        int currentUserId = SessionManager.getInstance().getUserId();
        if (item.getReportedBy() == currentUserId) {
            JOptionPane.showMessageDialog(this, "You reported this item yourself.", "Cannot Claim", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (item.getItemType() != Item.ItemType.FOUND) {
            JOptionPane.showMessageDialog(this, "Only items marked as 'FOUND' can be claimed by an owner.", "Cannot Claim", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (item.getStatus() != Item.ItemStatus.OPEN) {
            JOptionPane.showMessageDialog(this, "This item is already " + item.getStatus() + ".", "Cannot Claim", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        SubmitClaimDialog dialog = new SubmitClaimDialog(topFrame, item, this::applyFilters);
        dialog.setVisible(true);
    }

    private void handleViewDetails() {
        Item item = getSelectedListItem();
        if (item == null) return;

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ItemDetailsDialog dialog = new ItemDetailsDialog(parentWindow, item);
        dialog.setVisible(true);
    }
}
