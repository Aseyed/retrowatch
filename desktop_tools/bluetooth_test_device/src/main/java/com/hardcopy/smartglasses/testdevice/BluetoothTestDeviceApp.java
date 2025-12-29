package com.hardcopy.smartglasses.testdevice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Desktop Bluetooth Test Device - Simulates Arduino for Android app testing
 * 
 * This app connects to your Android phone via Bluetooth and implements
 * Protocol v2 to test the smartglasses_companion Android app.
 */
public class BluetoothTestDeviceApp {
    public static void main(String[] args) {
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new BluetoothTestDeviceApp().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Smart Glasses Bluetooth Test Device");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        MainPanel mainPanel = new MainPanel();
        frame.add(mainPanel);

        frame.setVisible(true);
    }
}


