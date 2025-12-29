package com.hardcopy.retrowatch.simulator;

import com.hardcopy.retrowatch.simulator.ui.MainWindow;

import javax.swing.*;

/**
 * Main application entry point
 */
public class RetroWatchSimulatorApp {
    
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show GUI on EDT
        SwingUtilities.invokeLater(() -> {
            new MainWindow();
        });
    }
}

