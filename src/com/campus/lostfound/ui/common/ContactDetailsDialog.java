package com.campus.lostfound.ui.common;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;

/**
 * ContactDetailsDialog
 * 
 * Displays authorized contact details between verified owners and finders:
 * - Student Name and Role in this exchange (Finder or Claimant/Owner)
 * - Campus Institutional Email (with one-click clipboard copy)
 * - Academic Roll Number, Department, and Semester
 * - Phone Number (only visible if the student has permitted public phone display)
 * - Campus safety recommendations for item handover
 */
public class ContactDetailsDialog extends JDialog {

    private final User contactUser;
    private final Item item;
    private final String peerRole;

    public ContactDetailsDialog(Window owner, User contactUser, Item item, String peerRole) {
        super(owner, "Authorized Contact Details", ModalityType.APPLICATION_MODAL);
        this.contactUser = contactUser;
        this.item = item;
        this.peerRole = peerRole != null ? peerRole : "Peer Student";
        initializeUI();
    }

    private void initializeUI() {
        setSize(540, 520);
        setMinimumSize(new Dimension(480, 460));
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel titleLbl = new JLabel("Authorized Contact Details");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_LIGHT);

        String itemTitle = item != null ? item.getTitle() : "Item";
        JLabel subLbl = new JLabel("Coordinating return for: " + itemTitle);
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(new Color(224, 231, 255));
        subLbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        headerPanel.add(titleLbl);
        headerPanel.add(subLbl);

        // Center Card Panel
        JPanel card = UITheme.createCardPanel(12);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int row = 0;

        // Dedicated Section Header: "Authorized Contact Details" with Peer Role Badge
        gbc.gridx = 0; gbc.gridy = row++;
        JPanel sectionHeaderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        sectionHeaderRow.setOpaque(false);

        JLabel sectionTitle = new JLabel("Authorized Contact Details");
        sectionTitle.setFont(UITheme.FONT_HEADER);
        sectionTitle.setForeground(UITheme.PRIMARY);

        JLabel roleBadge = new JLabel(" " + peerRole + " ");
        roleBadge.setFont(UITheme.FONT_SMALL);
        roleBadge.setOpaque(true);
        roleBadge.setBackground(new Color(220, 252, 231));
        roleBadge.setForeground(new Color(22, 163, 74));
        roleBadge.setBorder(BorderFactory.createLineBorder(new Color(187, 247, 208), 1));

        sectionHeaderRow.add(sectionTitle);
        sectionHeaderRow.add(roleBadge);
        card.add(sectionHeaderRow, gbc);

        // Campus Email Row with Copy Button
        gbc.gridy = row++;
        JLabel emailHeader = new JLabel("Institutional Campus Email:");
        emailHeader.setFont(UITheme.FONT_LABEL);
        emailHeader.setForeground(UITheme.TEXT_SECONDARY);
        card.add(emailHeader, gbc);

        gbc.gridy = row++;
        JPanel emailRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        emailRow.setOpaque(false);

        final String email = (contactUser != null && contactUser.getEmail() != null) ? contactUser.getEmail() : "N/A";
        JLabel emailVal = new JLabel(email);
        emailVal.setFont(UITheme.FONT_REGULAR);
        emailVal.setForeground(UITheme.PRIMARY_LIGHT);

        RoundedButton copyEmailBtn = new RoundedButton("Copy Email", RoundedButton.ButtonType.SECONDARY);
        copyEmailBtn.setFont(UITheme.FONT_SMALL);
        copyEmailBtn.setPreferredSize(new Dimension(100, 28));
        copyEmailBtn.addActionListener(e -> copyToClipboard(email, "Email address copied to clipboard!"));

        emailRow.add(emailVal);
        emailRow.add(copyEmailBtn);
        card.add(emailRow, gbc);

        // Academic Details (Roll Number, Department, Semester)
        gbc.gridy = row++;
        card.add(createDivider(), gbc);

        gbc.gridy = row++;
        String roll = (contactUser != null && contactUser.getRollNumber() != null) ? contactUser.getRollNumber() : "Not specified";
        String dept = (contactUser != null && contactUser.getDepartment() != null) ? contactUser.getDepartment() : "Not specified";
        String sem = (contactUser != null && contactUser.getSemester() != null) ? contactUser.getSemester() : "Not specified";

        JPanel acadInfo = new JPanel(new java.awt.GridLayout(3, 2, 8, 4));
        acadInfo.setOpaque(false);

        acadInfo.add(createLabel("Roll Number / ID:", true));
        acadInfo.add(createLabel(roll, false));
        acadInfo.add(createLabel("Department / Branch:", true));
        acadInfo.add(createLabel(dept, false));
        acadInfo.add(createLabel("Semester / Year:", true));
        acadInfo.add(createLabel(sem, false));

        card.add(acadInfo, gbc);

        // Phone Number Row
        gbc.gridy = row++;
        card.add(createDivider(), gbc);

        gbc.gridy = row++;
        JLabel phoneHeader = new JLabel("Contact Phone Number:");
        phoneHeader.setFont(UITheme.FONT_LABEL);
        phoneHeader.setForeground(UITheme.TEXT_SECONDARY);
        card.add(phoneHeader, gbc);

        gbc.gridy = row++;
        JPanel phoneRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        phoneRow.setOpaque(false);

        if (contactUser != null && contactUser.getPhoneNumber() != null && !contactUser.getPhoneNumber().trim().isEmpty()) {
            final String phone = contactUser.getPhoneNumber();
            JLabel phoneVal = new JLabel(phone);
            phoneVal.setFont(UITheme.FONT_REGULAR);
            phoneVal.setForeground(UITheme.PRIMARY_LIGHT);

            RoundedButton copyPhoneBtn = new RoundedButton("Copy Phone", RoundedButton.ButtonType.SECONDARY);
            copyPhoneBtn.setFont(UITheme.FONT_SMALL);
            copyPhoneBtn.setPreferredSize(new Dimension(100, 28));
            copyPhoneBtn.addActionListener(e -> copyToClipboard(phone, "Phone number copied to clipboard!"));

            phoneRow.add(phoneVal);
            phoneRow.add(copyPhoneBtn);
        } else {
            JLabel privatePhoneLbl = new JLabel("🔒 Private (Student coordinates exclusively via campus email)");
            privatePhoneLbl.setFont(UITheme.FONT_SMALL);
            privatePhoneLbl.setForeground(new Color(100, 116, 139));
            phoneRow.add(privatePhoneLbl);
        }
        card.add(phoneRow, gbc);

        // Safety Recommendation Box
        gbc.gridy = row++;
        JPanel safetyCard = new JPanel(new BorderLayout());
        safetyCard.setBackground(new Color(254, 243, 199)); // Amber background
        safetyCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(253, 230, 138), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        JLabel safetyLbl = new JLabel("<html><small style='color:#92400E;'>" +
                "<b>💡 Campus Safety Protocol:</b> Always arrange item handovers during regular university hours at an official campus service desk (e.g. Campus Security, Library circulation desk, or Student Union)." +
                "</small></html>");
        safetyCard.add(safetyLbl, BorderLayout.CENTER);
        card.add(safetyCard, gbc);

        // Wrapper
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_MAIN);
        wrapper.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        wrapper.add(card, BorderLayout.CENTER);

        // Footer Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UITheme.BG_MAIN);

        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.ButtonType.PRIMARY);
        closeBtn.setPreferredSize(new Dimension(90, 34));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        add(headerPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String text, boolean isHeader) {
        JLabel lbl = new JLabel(text);
        if (isHeader) {
            lbl.setFont(UITheme.FONT_LABEL);
            lbl.setForeground(UITheme.TEXT_SECONDARY);
        } else {
            lbl.setFont(UITheme.FONT_REGULAR);
            lbl.setForeground(UITheme.TEXT_PRIMARY);
        }
        return lbl;
    }

    private JPanel createDivider() {
        JPanel div = new JPanel();
        div.setBackground(new Color(241, 245, 249));
        div.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        return div;
    }

    private void copyToClipboard(String text, String notification) {
        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            JOptionPane.showMessageDialog(this, notification, "Copied", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            System.err.println("[ContactDetailsDialog] Clipboard copy failed: " + ex.getMessage());
        }
    }
}
