package com.campus.lostfound;

import com.campus.lostfound.ui.auth.LoginFrame;
import com.campus.lostfound.ui.common.UITheme;

import javax.swing.SwingUtilities;

/**
 * Main
 * 
 * Application Entry Point for Smart Campus Lost & Found System.
 * Initializes the modern UI theme and launches the authentication window on the Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {
        // Run GUI initialization on the Swing Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            // Apply FlatLaf modern styling
            UITheme.setupLookAndFeel();

            // Launch the Login Screen
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
