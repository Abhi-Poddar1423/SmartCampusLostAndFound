package com.campus.lostfound.ui.admin;

import com.campus.lostfound.dao.UserDAO;
import com.campus.lostfound.models.User;
import com.campus.lostfound.ui.common.CampusIcons;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ManageUsersPanel
 * 
 * Allows campus administrators to review registered users and safely delete
 * accounts with built-in protections:
 * 1. Self-protection: Admin cannot delete their own account.
 * 2. Last-admin protection: Prevents removing the last active administrator.
 * 3. Foreign key integrity: Blocks deleting users with existing item reports or claims.
 */
public class ManageUsersPanel extends JPanel {

    private final UserDAO userDAO;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final Runnable onDataChangedCallback;

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private CardLayout tableCardLayout;
    private JPanel tableContainer;

    private RoundedButton deleteButton;
    private RoundedButton refreshButton;

    private List<User> userList = new ArrayList<>();

    public ManageUsersPanel(Runnable onDataChangedCallback) {
        this.userDAO = new UserDAO();
        this.onDataChangedCallback = onDataChangedCallback;
        initializeUI();
        loadUsersAsync();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Header Panel
        JPanel headerPanel = UITheme.createCardPanel(10);
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));

        JLabel titleLbl = new JLabel("User Account Management");
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel infoLbl = new JLabel("(View registered campus users or manage accounts)");
        infoLbl.setFont(UITheme.FONT_SMALL);
        infoLbl.setForeground(UITheme.TEXT_SECONDARY);

        headerPanel.add(titleLbl);
        headerPanel.add(infoLbl);

        // Center Table
        String[] columns = {"User ID", "Full Name", "Email Address", "System Role", "Registered Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setFont(UITheme.FONT_REGULAR);
        userTable.setRowHeight(34);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        userTable.getTableHeader().setBackground(new Color(241, 245, 249));
        userTable.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        userTable.setShowVerticalLines(false);

        // Role Renderer
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFoc, row, col);
                if (isSel) {
                    c.setBackground(new Color(224, 242, 254));
                    c.setForeground(UITheme.PRIMARY);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);

                    // Role column (col 3)
                    if (col == 3 && val != null) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        if ("ADMIN".equalsIgnoreCase(val.toString())) {
                            c.setForeground(new Color(30, 58, 138)); // Deep Navy
                        } else {
                            c.setForeground(new Color(100, 116, 139)); // Slate
                        }
                    }
                }
                return c;
            }
        });

        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        userTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        // Center card container with empty state
        tableCardLayout = new CardLayout();
        tableContainer = new JPanel(tableCardLayout);
        tableContainer.setOpaque(false);

        JPanel emptyStatePanel = UITheme.createEmptyStatePanel(
                CampusIcons.IconType.USERS,
                "No Users Found",
                "No registered user accounts match the query in the database."
        );

        tableContainer.add(scrollPane, "TABLE");
        tableContainer.add(emptyStatePanel, "EMPTY");

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Loading user accounts...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttonBar.setOpaque(false);

        deleteButton = new RoundedButton("Delete User", RoundedButton.ButtonType.DANGER);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> handleDeleteUser());

        refreshButton = new RoundedButton("Refresh", RoundedButton.ButtonType.SECONDARY);
        refreshButton.addActionListener(e -> loadUsersAsync());

        buttonBar.add(deleteButton);
        buttonBar.add(refreshButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refreshUsers() {
        loadUsersAsync();
    }

    public void loadUsersAsync() {
        statusLabel.setText("Loading accounts from database...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() {
                return userDAO.getAllUsers();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    userList = get();
                    tableModel.setRowCount(0);
                    for (User u : userList) {
                        String dateStr = u.getCreatedAt() != null ? dateFormat.format(u.getCreatedAt()) : "N/A";
                        tableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getName(),
                                u.getEmail(),
                                u.getRole(),
                                dateStr
                        });
                    }
                    if (userList.isEmpty()) {
                        tableCardLayout.show(tableContainer, "EMPTY");
                    } else {
                        tableCardLayout.show(tableContainer, "TABLE");
                    }
                    statusLabel.setText("Total registered accounts: " + userList.size());
                    updateButtonStates();
                    if (onDataChangedCallback != null) {
                        onDataChangedCallback.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load users: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row >= 0 && row < userList.size()) {
            return userList.get(row);
        }
        return null;
    }

    private void updateButtonStates() {
        User user = getSelectedUser();
        if (user == null) {
            deleteButton.setEnabled(false);
            return;
        }

        int currentAdminId = SessionManager.getInstance().getUserId();
        boolean isSelf = (user.getUserId() == currentAdminId);

        // Cannot delete self
        deleteButton.setEnabled(!isSelf);
    }

    private void handleDeleteUser() {
        User user = getSelectedUser();
        if (user == null) return;

        int currentAdminId = SessionManager.getInstance().getUserId();
        if (user.getUserId() == currentAdminId) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account.", "Action Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (user.getRole() == User.Role.ADMIN && userDAO.countAdminUsers() <= 1) {
            JOptionPane.showMessageDialog(this, "Cannot delete the last remaining administrator account.", "Action Blocked", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Foreign Key Integrity Check
        if (userDAO.hasUserRelatedRecords(user.getUserId())) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot delete user '" + user.getName() + "'.\n" +
                    "This user has associated item reports or submitted claims in the database.\n" +
                    "To preserve data integrity, please delete or resolve their items/claims first.",
                    "Integrity Protection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete user '" + user.getName() + "'?\n" +
                "This action cannot be undone.",
                "Confirm Account Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return userDAO.deleteUser(user.getUserId());
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(ManageUsersPanel.this, "User account deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                            loadUsersAsync();
                        } else {
                            JOptionPane.showMessageDialog(ManageUsersPanel.this, "Failed to delete account.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ManageUsersPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
