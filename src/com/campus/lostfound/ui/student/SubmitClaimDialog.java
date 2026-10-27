package com.campus.lostfound.ui.student;

import com.campus.lostfound.dao.ClaimDAO;
import com.campus.lostfound.models.Claim;
import com.campus.lostfound.models.Item;
import com.campus.lostfound.ui.common.RoundedButton;
import com.campus.lostfound.ui.common.UITheme;
import com.campus.lostfound.utils.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * SubmitClaimDialog
 * 
 * Allows students to submit an ownership claim on a FOUND item.
 * Validates eligibility (not student's own item, item is OPEN, no prior duplicate claim),
 * collects detailed ownership proof, and stores the claim asynchronously.
 */
public class SubmitClaimDialog extends JDialog {

    private final Item targetItem;
    private final ClaimDAO claimDAO;
    private final Runnable onSuccessCallback;

    private JTextArea messageArea;
    private RoundedButton submitButton;
    private RoundedButton cancelButton;
    private JLabel statusLabel;

    public SubmitClaimDialog(JFrame parent, Item targetItem, Runnable onSuccessCallback) {
        super(parent, "Claim Found Item", true);
        this.targetItem = targetItem;
        this.claimDAO = new ClaimDAO();
        this.onSuccessCallback = onSuccessCallback;
        initializeUI();
    }

    private void initializeUI() {
        setSize(480, 520);
        setMinimumSize(new Dimension(420, 460));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel titleLabel = new JLabel("Claim Ownership");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_LIGHT);

        JLabel subtitleLabel = new JLabel("Submit proof of ownership for administrator review");
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(new Color(224, 231, 255));

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        // Content Card Panel
        JPanel card = UITheme.createCardPanel(12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Item Details Summary Box
        JPanel summaryBox = new JPanel();
        summaryBox.setLayout(new BoxLayout(summaryBox, BoxLayout.Y_AXIS));
        summaryBox.setBackground(new Color(241, 245, 249));
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel itemNameLabel = new JLabel("Item: " + targetItem.getTitle());
        itemNameLabel.setFont(UITheme.FONT_BOLD);
        itemNameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel metaLabel = new JLabel("Category: " + targetItem.getCategory() + "  |  Found at: " + targetItem.getLocation());
        metaLabel.setFont(UITheme.FONT_SMALL);
        metaLabel.setForeground(UITheme.TEXT_SECONDARY);

        summaryBox.add(itemNameLabel);
        summaryBox.add(Box.createVerticalStrut(4));
        summaryBox.add(metaLabel);

        // Ownership Proof Input
        JLabel proofLabel = UITheme.createFieldLabel("Proof of Ownership / Identification Details *");
        JLabel instructionLabel = new JLabel("<html>Describe distinctive features not mentioned above (e.g., contents, lock screen photo, stickers, serial numbers, exact brand/model).</html>");
        instructionLabel.setFont(UITheme.FONT_SMALL);
        instructionLabel.setForeground(UITheme.TEXT_SECONDARY);

        messageArea = new JTextArea(5, 20);
        messageArea.setFont(UITheme.FONT_REGULAR);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(UITheme.createFieldBorder());

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);

        card.add(summaryBox);
        card.add(Box.createVerticalStrut(14));
        card.add(proofLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(instructionLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(scrollPane);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);

        // Footer Actions
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footerPanel.setBackground(UITheme.BG_MAIN);

        cancelButton = new RoundedButton("Cancel", RoundedButton.ButtonType.OUTLINE);
        cancelButton.addActionListener(e -> dispose());

        submitButton = new RoundedButton("Submit Claim", RoundedButton.ButtonType.PRIMARY);
        submitButton.addActionListener(e -> handleSubmitClaim());

        footerPanel.add(cancelButton);
        footerPanel.add(submitButton);

        // Layout Assembly
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(UITheme.BG_MAIN);
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        wrapperPanel.add(card, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(wrapperPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void handleSubmitClaim() {
        String message = messageArea.getText().trim();
        int currentUserId = SessionManager.getInstance().getUserId();

        // 1. Client-side input validation
        if (message.isEmpty()) {
            statusLabel.setText("Please enter ownership proof details.");
            messageArea.requestFocusInWindow();
            return;
        }
        if (message.length() < 10) {
            statusLabel.setText("Proof message must be at least 10 characters.");
            messageArea.requestFocusInWindow();
            return;
        }

        // 2. Safety checks
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Session expired. Please log in again.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        if (targetItem.getReportedBy() == currentUserId) {
            JOptionPane.showMessageDialog(this, "You cannot submit a claim on an item you reported yourself.", "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (targetItem.getStatus() != Item.ItemStatus.OPEN) {
            JOptionPane.showMessageDialog(this, "This item is no longer open for claims (Status: " + targetItem.getStatus() + ").", "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setLoadingState(true);
        statusLabel.setText(" ");

        // 3. Asynchronous duplicate check & submission via SwingWorker
        new SwingWorker<Boolean, Void>() {
            private String failureReason = "Failed to submit claim.";

            @Override
            protected Boolean doInBackground() {
                try {
                    // Check if student already submitted a claim for this item
                    if (claimDAO.hasUserClaimed(targetItem.getItemId(), currentUserId)) {
                        failureReason = "You have already submitted a claim for this item.";
                        return false;
                    }

                    // Save claim with PENDING status
                    Claim newClaim = new Claim(targetItem.getItemId(), currentUserId, message);
                    return claimDAO.submitClaim(newClaim);
                } catch (Exception ex) {
                    failureReason = "Database error: " + ex.getMessage();
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
                                SubmitClaimDialog.this,
                                "Claim submitted successfully!\nCampus administration will review your claim details.",
                                "Claim Submitted",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        dispose();
                    } else {
                        statusLabel.setText(failureReason);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("An unexpected error occurred.");
                }
            }
        }.execute();
    }

    private void setLoadingState(boolean loading) {
        submitButton.setEnabled(!loading);
        submitButton.setText(loading ? "Submitting..." : "Submit Claim");
        cancelButton.setEnabled(!loading);
        messageArea.setEnabled(!loading);
        setCursor(loading ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
