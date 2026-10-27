package com.campus.lostfound.ui.admin;

import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.ui.common.ItemDetailsDialog;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.ui.student.ReportItemDialog;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
 * ManageItemsPanel
 * 
 * Provides administrative oversight over all campus lost and found item reports.
 * Allows moderation, status updates (OPEN, CLAIMED, RETURNED), and safe atomic
 * cascading deletion handling foreign-key constraints safely.
 */
public class ManageItemsPanel extends JPanel {

    private final ItemDAO itemDAO;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final Runnable onDataChangedCallback;

    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> typeCombo;
    private JComboBox<String> statusCombo;
    private RoundedButton searchButton;
    private RoundedButton resetButton;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private RoundedButton updateStatusButton;
    private RoundedButton deleteButton;
    private RoundedButton viewDetailsButton;
    private RoundedButton refreshButton;

    private List<Item> itemList = new ArrayList<>();

    public ManageItemsPanel(Runnable onDataChangedCallback) {
        this.itemDAO = new ItemDAO();
        this.onDataChangedCallback = onDataChangedCallback;
        initializeUI();
        loadItemsAsync(null, "ALL", "ALL", "ALL");
    }

    private void initializeUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Filter Bar
        JPanel filterCard = UITheme.createCardPanel(10);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_LABEL);
        searchField = UITheme.createTextField(14);
        searchField.setToolTipText("Filter by title, description, location");

        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(UITheme.FONT_LABEL);
        List<String> categories = new ArrayList<>();
        categories.add("ALL");
        for (String c : ReportItemDialog.CATEGORIES) {
            categories.add(c);
        }
        categoryCombo = new JComboBox<>(categories.toArray(new String[0]));
        categoryCombo.setFont(UITheme.FONT_REGULAR);
        categoryCombo.setPreferredSize(new Dimension(140, 36));

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
        searchButton.setPreferredSize(new Dimension(85, 36));
        searchButton.addActionListener(e -> applyFilters());

        resetButton = new RoundedButton("Reset", RoundedButton.ButtonType.OUTLINE);
        resetButton.setPreferredSize(new Dimension(80, 36));
        resetButton.addActionListener(e -> resetFilters());

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

        // Center Table
        String[] columns = {"ID", "Type", "Title", "Category", "Campus Location", "Status", "Reported By", "Date Reported"};
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

        // Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    if (col == 1 && val != null) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        c.setForeground("LOST".equalsIgnoreCase(val.toString()) ? new Color(220, 38, 38) : new Color(22, 163, 74));
                    }
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

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);
        table.getColumnModel().getColumn(7).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        table.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Loading items...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonBar.setOpaque(false);

        updateStatusButton = new RoundedButton("Update Status", RoundedButton.ButtonType.PRIMARY);
        updateStatusButton.setEnabled(false);
        updateStatusButton.addActionListener(e -> handleUpdateStatus());

        deleteButton = new RoundedButton("Moderate / Delete", RoundedButton.ButtonType.DANGER);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> handleDeleteItem());

        viewDetailsButton = new RoundedButton("View Details", RoundedButton.ButtonType.OUTLINE);
        viewDetailsButton.setEnabled(false);
        viewDetailsButton.addActionListener(e -> handleViewDetails());

        refreshButton = new RoundedButton("Refresh", RoundedButton.ButtonType.SECONDARY);
        refreshButton.addActionListener(e -> applyFilters());

        buttonBar.add(viewDetailsButton);
        buttonBar.add(updateStatusButton);
        buttonBar.add(deleteButton);
        buttonBar.add(refreshButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);

        add(filterCard, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshItems() {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim();
        String cat = (String) categoryCombo.getSelectedItem();
        String type = (String) typeCombo.getSelectedItem();
        String st = (String) statusCombo.getSelectedItem();

        loadItemsAsync(keyword, cat, type, st);
    }

    private void resetFilters() {
        searchField.setText("");
        categoryCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        loadItemsAsync(null, "ALL", "ALL", "ALL");
    }

    private void loadItemsAsync(String keyword, String category, String itemType, String status) {
        statusLabel.setText("Querying items from database...");
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
                    itemList = get();
                    tableModel.setRowCount(0);
                    for (Item i : itemList) {
                        String dateStr = i.getReportDate() != null ? dateFormat.format(i.getReportDate()) : "N/A";
                        String reporter = i.getReporterName() != null ? i.getReporterName() : "User #" + i.getReportedBy();

                        tableModel.addRow(new Object[]{
                                i.getItemId(),
                                i.getItemType(),
                                i.getTitle(),
                                i.getCategory() != null ? i.getCategory() : "N/A",
                                i.getLocation() != null ? i.getLocation() : "N/A",
                                i.getStatus(),
                                reporter,
                                dateStr
                        });
                    }
                    statusLabel.setText("Total items found: " + itemList.size());
                    updateButtonStates();
                    if (onDataChangedCallback != null) {
                        onDataChangedCallback.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load items: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Item getSelectedItem() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < itemList.size()) {
            return itemList.get(row);
        }
        return null;
    }

    private void updateButtonStates() {
        Item item = getSelectedItem();
        boolean hasSel = (item != null);
        updateStatusButton.setEnabled(hasSel);
        deleteButton.setEnabled(hasSel);
        viewDetailsButton.setEnabled(hasSel);
    }

    private void handleUpdateStatus() {
        Item item = getSelectedItem();
        if (item == null) return;

        Item.ItemStatus[] choices = {Item.ItemStatus.OPEN, Item.ItemStatus.CLAIMED, Item.ItemStatus.RETURNED};
        Item.ItemStatus chosen = (Item.ItemStatus) JOptionPane.showInputDialog(
                this,
                "Select new lifecycle status for '" + item.getTitle() + "':",
                "Update Item Status",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                item.getStatus()
        );

        if (chosen != null && chosen != item.getStatus()) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return itemDAO.updateStatus(item.getItemId(), chosen);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(ManageItemsPanel.this, "Item status updated to " + chosen, "Updated", JOptionPane.INFORMATION_MESSAGE);
                            applyFilters();
                        } else {
                            JOptionPane.showMessageDialog(ManageItemsPanel.this, "Failed to update item status.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ManageItemsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleDeleteItem() {
        Item item = getSelectedItem();
        if (item == null) return;

        // Check if claims exist for this item
        int claimsCount = itemDAO.getItemClaimsCount(item.getItemId());
        String warningMsg = "Are you sure you want to permanently delete item #" + item.getItemId() + " ('" + item.getTitle() + "')?";
        if (claimsCount > 0) {
            warningMsg += "\n\n[WARNING] This item has " + claimsCount + " existing claim(s) associated with it.\n" +
                          "Deleting this item will safely remove its related claims in an atomic transaction.";
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                warningMsg,
                "Confirm Item Moderation Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return itemDAO.deleteItemWithClaims(item.getItemId());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(ManageItemsPanel.this, "Item report and related claims deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                            applyFilters();
                        } else {
                            JOptionPane.showMessageDialog(ManageItemsPanel.this, "Failed to delete item.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ManageItemsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleViewDetails() {
        Item item = getSelectedItem();
        if (item == null) return;

        java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
        ItemDetailsDialog dialog = new ItemDetailsDialog(parentWindow, item);
        dialog.setVisible(true);
    }
}
