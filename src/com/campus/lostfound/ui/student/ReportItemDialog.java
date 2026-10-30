package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.ItemDAO;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.ui.common.CampusIcons;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.ImageUtil;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

/**
 * ReportItemDialog
 * 
 * Clean, properly fitted reporting dialog for campus lost & found items.
 * Layout improvements:
 * - Fits cleanly within standard laptop resolutions without taskbar overflow.
 * - Sticky bottom action bar with Cancel & Submit always visible.
 * - Strictly prevents horizontal scrolling (VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER).
 * - Compact photo preview with side-by-side controls and aspect ratio preservation.
 */
public class ReportItemDialog extends JDialog {

    public static final String[] CATEGORIES = {
        "Electronics & Gadgets",
        "ID Cards & Documents",
        "Keys & Access Cards",
        "Books & Stationery",
        "Bags & Backpacks",
        "Clothing & Accessories",
        "Water Bottles",
        "Wallets & Purses",
        "Other"
    };

    public static final String[] BUILDINGS = {
        "Main Academic Block",
        "Central Library",
        "Science & Tech Block",
        "Engineering Hub",
        "Student Center & Cafeteria",
        "Sports Complex & Gymnasium",
        "Administrative Block",
        "Hostels / Residential",
        "Campus Grounds / Parking",
        "Other"
    };

    private final ItemDAO itemDAO;
    private final Runnable onSuccessCallback;

    private JTextField titleField;
    private JComboBox<Item.ItemType> typeComboBox;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> buildingComboBox;
    private JTextField floorRoomField;
    private JTextField locationField;
    private JTextArea descriptionArea;

    // Photo selection components
    private File selectedPhotoFile = null;
    private JLabel photoPreviewLabel;
    private JLabel photoNameLabel;
    private RoundedButton selectPhotoButton;
    private RoundedButton removePhotoButton;

    private RoundedButton submitButton;
    private RoundedButton cancelButton;
    private JLabel statusLabel;

    public ReportItemDialog(JFrame parent, Runnable onSuccessCallback) {
        super(parent, "Report Lost or Found Item", true);
        this.itemDAO = new ItemDAO();
        this.onSuccessCallback = onSuccessCallback;
        initializeUI();
    }

    private void initializeUI() {
        setSize(580, 680);
        setMinimumSize(new Dimension(520, 580));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // ==========================================
        // 1. COMPACT NAVY HEADER PANEL
        // ==========================================
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel titleLabel = new JLabel("Report an Item");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_LIGHT);

        JLabel subtitleLabel = new JLabel("Provide accurate details to help locate or return the item");
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(new Color(224, 231, 255));

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        // ==========================================
        // 2. FORM CONTENT PANEL (INSIDE SCROLL PANE)
        // ==========================================
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        int row = 0;

        // 1. Report Type
        JLabel typeLabel = UITheme.createFieldLabel("What would you like to report? *");
        typeComboBox = new JComboBox<>(new Item.ItemType[]{Item.ItemType.LOST, Item.ItemType.FOUND});
        typeComboBox.setFont(UITheme.FONT_REGULAR);
        typeComboBox.setPreferredSize(new Dimension(typeComboBox.getPreferredSize().width, 36));

        gbc.gridy = row++;
        formContainer.add(typeLabel, gbc);
        gbc.gridy = row++;
        formContainer.add(typeComboBox, gbc);

        // 2. Item Title
        JLabel itemTitleLabel = UITheme.createFieldLabel("Item Name / Title *");
        titleField = UITheme.createTextField(20);

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(itemTitleLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        formContainer.add(titleField, gbc);

        // 3. Category
        JLabel categoryLabel = UITheme.createFieldLabel("Category *");
        categoryComboBox = new JComboBox<>(CATEGORIES);
        categoryComboBox.setFont(UITheme.FONT_REGULAR);
        categoryComboBox.setPreferredSize(new Dimension(categoryComboBox.getPreferredSize().width, 36));

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(categoryLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        formContainer.add(categoryComboBox, gbc);

        // 4. Campus Building & Floor/Room
        JLabel buildingLabel = UITheme.createFieldLabel("Campus Building *");
        buildingComboBox = new JComboBox<>(BUILDINGS);
        buildingComboBox.setFont(UITheme.FONT_REGULAR);
        buildingComboBox.setPreferredSize(new Dimension(buildingComboBox.getPreferredSize().width, 36));

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(buildingLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        formContainer.add(buildingComboBox, gbc);

        JLabel floorRoomLabel = UITheme.createFieldLabel("Floor / Room / Specific Area");
        floorRoomField = UITheme.createTextField(20);

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(floorRoomLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        formContainer.add(floorRoomField, gbc);

        // 5. Location Summary (Auto-updating)
        JLabel locationLabel = UITheme.createFieldLabel("Campus Location Summary *");
        locationField = UITheme.createTextField(20);

        buildingComboBox.addActionListener(e -> updateLocationSummary());
        floorRoomField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateLocationSummary();
            }
        });
        updateLocationSummary();

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(locationLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        formContainer.add(locationField, gbc);

        // 6. Photo Upload Section
        JLabel photoSectionLabel = UITheme.createFieldLabel("Item Photo (Optional — JPG, PNG up to 5MB)");

        JPanel photoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        photoRow.setOpaque(false);

        photoPreviewLabel = new JLabel("No Photo", SwingConstants.CENTER);
        photoPreviewLabel.setPreferredSize(new Dimension(68, 68));
        photoPreviewLabel.setFont(UITheme.FONT_SMALL);
        photoPreviewLabel.setForeground(UITheme.TEXT_SECONDARY);
        photoPreviewLabel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        photoPreviewLabel.setBackground(new Color(248, 250, 252));
        photoPreviewLabel.setOpaque(true);

        JPanel photoButtonsPanel = new JPanel();
        photoButtonsPanel.setLayout(new BoxLayout(photoButtonsPanel, BoxLayout.Y_AXIS));
        photoButtonsPanel.setOpaque(false);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);

        selectPhotoButton = new RoundedButton("Choose Photo...", RoundedButton.ButtonType.SECONDARY);
        selectPhotoButton.setFont(UITheme.FONT_SMALL);
        selectPhotoButton.setPreferredSize(new Dimension(135, 34));
        selectPhotoButton.addActionListener(e -> handleSelectPhoto());

        removePhotoButton = new RoundedButton("Remove", RoundedButton.ButtonType.OUTLINE);
        removePhotoButton.setFont(UITheme.FONT_SMALL);
        removePhotoButton.setPreferredSize(new Dimension(85, 34));
        removePhotoButton.setEnabled(false);
        removePhotoButton.addActionListener(e -> handleRemovePhoto());

        btnRow.add(selectPhotoButton);
        btnRow.add(removePhotoButton);

        photoNameLabel = new JLabel("No file selected");
        photoNameLabel.setFont(UITheme.FONT_SMALL);
        photoNameLabel.setForeground(UITheme.TEXT_SECONDARY);
        photoNameLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 0));

        photoButtonsPanel.add(btnRow);
        photoButtonsPanel.add(photoNameLabel);

        photoRow.add(photoPreviewLabel);
        photoRow.add(photoButtonsPanel);

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(photoSectionLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        formContainer.add(photoRow, gbc);

        // 7. Description
        JLabel descLabel = UITheme.createFieldLabel("Description (Color, brand, distinguishing marks)");
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setFont(UITheme.FONT_REGULAR);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        descScrollPane.setBorder(UITheme.createFieldBorder());
        descScrollPane.setPreferredSize(new Dimension(100, 75));

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 4, 0);
        formContainer.add(descLabel, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 8, 0);
        formContainer.add(descScrollPane, gbc);

        // Wrap form in ScrollPane (vertical only, horizontal strictly never)
        JScrollPane scrollPane = new JScrollPane(formContainer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setBorder(null);

        // ==========================================
        // 3. STICKY DEDICATED BOTTOM ACTION BAR
        // ==========================================
        JPanel bottomActionPanel = new JPanel(new BorderLayout(12, 0));
        bottomActionPanel.setBackground(Color.WHITE);
        bottomActionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonBar.setOpaque(false);

        cancelButton = new RoundedButton("Cancel", RoundedButton.ButtonType.OUTLINE);
        cancelButton.setPreferredSize(new Dimension(100, 38));
        cancelButton.addActionListener(e -> dispose());

        submitButton = new RoundedButton("Submit Report", RoundedButton.ButtonType.ACCENT);
        submitButton.setPreferredSize(new Dimension(145, 38));
        submitButton.addActionListener(e -> handleSubmitReport());

        buttonBar.add(cancelButton);
        buttonBar.add(submitButton);

        bottomActionPanel.add(statusLabel, BorderLayout.CENTER);
        bottomActionPanel.add(buttonBar, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomActionPanel, BorderLayout.SOUTH);
    }

    private void updateLocationSummary() {
        String building = (String) buildingComboBox.getSelectedItem();
        String room = floorRoomField.getText().trim();
        if (building != null && !building.equalsIgnoreCase("Other")) {
            if (!room.isEmpty()) {
                locationField.setText(building + " - " + room);
            } else {
                locationField.setText(building);
            }
        }
    }

    private void handleSelectPhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Item Photo");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                ImageUtil.validateImageFile(file);
                selectedPhotoFile = file;
                ImageIcon thumb = ImageUtil.createThumbnail(file, 68, 68);
                if (thumb != null) {
                    photoPreviewLabel.setText("");
                    photoPreviewLabel.setIcon(thumb);
                } else {
                    photoPreviewLabel.setText("Selected");
                    photoPreviewLabel.setIcon(null);
                }
                photoNameLabel.setText(file.getName() + " (" + (file.length() / 1024) + " KB)");
                removePhotoButton.setEnabled(true);
                statusLabel.setText(" ");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        ex.getMessage(),
                        "Invalid Photo",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleRemovePhoto() {
        selectedPhotoFile = null;
        photoPreviewLabel.setIcon(null);
        photoPreviewLabel.setText("No Photo");
        photoNameLabel.setText("No file selected");
        removePhotoButton.setEnabled(false);
    }

    private void handleSubmitReport() {
        String title = titleField.getText().trim();
        String location = locationField.getText().trim();
        String category = (String) categoryComboBox.getSelectedItem();
        Item.ItemType type = (Item.ItemType) typeComboBox.getSelectedItem();
        String description = descriptionArea.getText().trim();
        String building = (String) buildingComboBox.getSelectedItem();
        String floorRoom = floorRoomField.getText().trim();

        if (location.isEmpty()) {
            if (building != null && !building.equalsIgnoreCase("Other")) {
                location = building + (!floorRoom.isEmpty() ? " - " + floorRoom : "");
            } else if (!floorRoom.isEmpty()) {
                location = floorRoom;
            }
        }

        if (title.isEmpty()) {
            statusLabel.setText("Please enter an item name/title.");
            titleField.requestFocusInWindow();
            return;
        }
        if (title.length() < 3) {
            statusLabel.setText("Item title must be at least 3 characters.");
            titleField.requestFocusInWindow();
            return;
        }
        if (location.isEmpty()) {
            statusLabel.setText("Please specify where the item was lost or found.");
            locationField.requestFocusInWindow();
            return;
        }

        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Session expired. Please log in again.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setLoadingState(true);
        statusLabel.setText(" ");

        final String finalLocation = location;
        final File photoToUpload = selectedPhotoFile;

        new SwingWorker<Boolean, Void>() {
            private String errorMessage = "";

            @Override
            protected Boolean doInBackground() {
                ImageUtil.SavedImageResult currentAttemptImage = null;
                try {
                    Item newItem = new Item(
                            title,
                            description,
                            category,
                            finalLocation,
                            type,
                            Item.ItemStatus.OPEN,
                            currentUserId
                    );
                    newItem.setBuilding(building);
                    newItem.setFloorRoom(floorRoom);

                    if (photoToUpload != null) {
                        currentAttemptImage = ImageUtil.saveItemImageResult(photoToUpload);
                        newItem.setImagePath(currentAttemptImage.getRelativePath());
                    }

                    boolean success = itemDAO.addItem(newItem);
                    if (!success && currentAttemptImage != null) {
                        cleanupSavedImage(currentAttemptImage);
                    }
                    return success;
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    if (currentAttemptImage != null) {
                        cleanupSavedImage(currentAttemptImage);
                    }
                    return false;
                }
            }

            @Override
            protected void done() {
                setLoadingState(false);
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(
                                ReportItemDialog.this,
                                "Item reported successfully!\nIt is now listed in the campus portal.",
                                "Report Submitted",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        dispose();
                    } else {
                        statusLabel.setText("Failed to save report: " + (errorMessage.isEmpty() ? "Database error" : errorMessage));
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Unexpected error occurred while reporting item.");
                }
            }
        }.execute();
    }

    private void cleanupSavedImage(ImageUtil.SavedImageResult savedResult) {
        if (savedResult == null || savedResult.getTargetPath() == null || savedResult.getFileKey() == null) {
            return;
        }

        try {
            java.nio.file.Path targetPath = savedResult.getTargetPath().toAbsolutePath().normalize();
            java.nio.file.Path uploadDir = ImageUtil.getUploadDirectory().toAbsolutePath().normalize();

            java.nio.file.Path parentPath = targetPath.getParent();
            if (parentPath == null || !parentPath.equals(uploadDir)) {
                System.err.println("[ReportItemDialog] Security alert: Cleanup target is not a direct child of upload directory.");
                return;
            }

            java.nio.file.attribute.BasicFileAttributes targetAttrs =
                    java.nio.file.Files.readAttributes(targetPath, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);

            if (targetAttrs.isSymbolicLink() || targetAttrs.isOther()) {
                System.err.println("[ReportItemDialog] Security alert: Symbolic link or reparse point detected on target file. Cleanup aborted.");
                return;
            }

            java.nio.file.attribute.BasicFileAttributes parentAttrs =
                    java.nio.file.Files.readAttributes(parentPath, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);

            if (parentAttrs.isSymbolicLink() || parentAttrs.isOther()) {
                System.err.println("[ReportItemDialog] Security alert: Symbolic link or reparse point detected on upload directory. Cleanup aborted.");
                return;
            }

            Object currentKey = targetAttrs.fileKey();
            if (currentKey == null) {
                currentKey = targetPath.toRealPath().toString() + ":" + targetAttrs.creationTime().toMillis() + ":" + targetAttrs.size();
            }

            if (!savedResult.getFileKey().equals(currentKey)) {
                System.err.println("[ReportItemDialog] Security alert: File identity mismatch. Target was substituted; cleanup aborted.");
                return;
            }

            java.nio.file.Path canonicalUploadDir = uploadDir.toRealPath();
            java.nio.file.Path canonicalTarget = targetPath.toRealPath();

            if (!canonicalTarget.startsWith(canonicalUploadDir) ||
                canonicalTarget.getParent() == null ||
                !canonicalTarget.getParent().equals(canonicalUploadDir)) {
                System.err.println("[ReportItemDialog] Security alert: Canonical path escaped upload directory. Cleanup aborted.");
                return;
            }

            try {
                java.nio.file.Files.delete(canonicalTarget);
            } catch (java.io.IOException deleteEx) {
                System.err.println("[ReportItemDialog] Warning: Immediate file deletion failed for " 
                        + canonicalTarget + ": " + deleteEx.getMessage());
                canonicalTarget.toFile().deleteOnExit();
            }

        } catch (Exception cleanupEx) {
            System.err.println("[ReportItemDialog] Warning: Exception during orphaned image cleanup: " + cleanupEx.getMessage());
        }
    }

    private void setLoadingState(boolean loading) {
        submitButton.setEnabled(!loading);
        submitButton.setText(loading ? "Submitting..." : "Submit Report");
        cancelButton.setEnabled(!loading);
        titleField.setEnabled(!loading);
        typeComboBox.setEnabled(!loading);
        categoryComboBox.setEnabled(!loading);
        buildingComboBox.setEnabled(!loading);
        floorRoomField.setEnabled(!loading);
        locationField.setEnabled(!loading);
        selectPhotoButton.setEnabled(!loading);
        removePhotoButton.setEnabled(!loading && selectedPhotoFile != null);
        descriptionArea.setEnabled(!loading);
        setCursor(loading ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
