package com.hardcopy.smartglasses.testdevice;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main GUI panel for the Bluetooth test device
 */
class MainPanel extends JPanel {
    private final JTextArea logArea;
    private JLabel statusLabel;
    private JButton connectButton;
    private JButton disconnectButton;
    private JComboBox<String> deviceCombo;
    private JButton refreshButton;
    
    private BluetoothConnectionManager btManager;
    private BluetoothServerManager serverManager;
    private boolean isConnected = false;
    private boolean isServerMode = false;
    private JButton startServerButton;
    private JButton stopServerButton;

    public MainPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: Connection panel
        JPanel topPanel = createConnectionPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center: Log area
        logArea = new JTextArea(20, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Message Log"));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: Control buttons
        JPanel bottomPanel = createControlPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        btManager = new BluetoothConnectionManager(this);
        serverManager = new BluetoothServerManager(this);
        refreshDevices();
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Connection Mode"));

        // Mode selection
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton clientMode = new JRadioButton("COM Port (Client)", true);
        JRadioButton serverMode = new JRadioButton("TCP Server (Android connects to PC)", false);
        modeGroup.add(clientMode);
        modeGroup.add(serverMode);
        clientMode.addActionListener(e -> {
            isServerMode = false;
            updateUIForMode();
        });
        serverMode.addActionListener(e -> {
            isServerMode = true;
            updateUIForMode();
        });
        modePanel.add(clientMode);
        modePanel.add(serverMode);
        panel.add(modePanel, BorderLayout.NORTH);

        // Status
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.CENTER);

        // Device/Server controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Client mode controls
        JPanel clientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clientPanel.add(new JLabel("Device:"));
        deviceCombo = new JComboBox<>();
        deviceCombo.setPreferredSize(new Dimension(300, 25));
        clientPanel.add(deviceCombo);
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshDevices());
        clientPanel.add(refreshButton);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        clientPanel.add(connectButton);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());
        clientPanel.add(disconnectButton);
        
        // Server mode controls
        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startServerButton = new JButton("Start Server");
        startServerButton.addActionListener(e -> startServer());
        serverPanel.add(startServerButton);
        
        stopServerButton = new JButton("Stop Server");
        stopServerButton.setEnabled(false);
        stopServerButton.addActionListener(e -> stopServer());
        serverPanel.add(stopServerButton);
        
        controlPanel.add(clientPanel);
        controlPanel.add(serverPanel);
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        updateUIForMode();
        return panel;
    }
    
    private void updateUIForMode() {
        deviceCombo.setEnabled(!isServerMode);
        refreshButton.setEnabled(!isServerMode);
        connectButton.setEnabled(!isServerMode && !isConnected);
        disconnectButton.setEnabled(!isServerMode && isConnected);
        startServerButton.setEnabled(isServerMode && !serverManager.isRunning());
        stopServerButton.setEnabled(isServerMode && serverManager.isRunning());
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Test Controls"));

        JButton sendAckButton = new JButton("Send ACK");
        sendAckButton.addActionListener(e -> sendTestAck());
        panel.add(sendAckButton);

        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        panel.add(clearLogButton);

        return panel;
    }

    private void refreshDevices() {
        log("Refreshing Bluetooth devices...");
        btManager.refreshDevices(deviceCombo);
    }

    private void connect() {
        String selected = (String) deviceCombo.getSelectedItem();
        if (selected == null || selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a device first", 
                "No Device Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        log("Connecting to: " + selected);
        connectButton.setEnabled(false);
        
        new Thread(() -> {
            boolean success = btManager.connect(selected);
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    isConnected = true;
                    statusLabel.setText("Status: Connected");
                    statusLabel.setForeground(Color.GREEN);
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    log("✓ Connected successfully!");
                } else {
                    statusLabel.setText("Status: Connection Failed");
                    statusLabel.setForeground(Color.RED);
                    connectButton.setEnabled(true);
                    log("✗ Connection failed. Check Bluetooth and try again.");
                }
            });
        }).start();
    }

    private void disconnect() {
        log("Disconnecting...");
        if (isServerMode) {
            stopServer();
        } else {
            btManager.disconnect();
            isConnected = false;
            statusLabel.setText("Status: Disconnected");
            statusLabel.setForeground(Color.RED);
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            log("Disconnected.");
        }
        updateUIForMode();
    }
    
    private void startServer() {
        log("Starting TCP server...");
        if (serverManager.startServer()) {
            statusLabel.setText("Status: Server Running (Port " + serverManager.getServerPort() + ")");
            statusLabel.setForeground(Color.GREEN);
            startServerButton.setEnabled(false);
            stopServerButton.setEnabled(true);
            log("✓ Server started. Android app can now connect via TCP.");
        } else {
            statusLabel.setText("Status: Server Start Failed");
            statusLabel.setForeground(Color.RED);
        }
        updateUIForMode();
    }
    
    private void stopServer() {
        log("Stopping server...");
        serverManager.stopServer();
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        startServerButton.setEnabled(true);
        stopServerButton.setEnabled(false);
        log("Server stopped.");
        updateUIForMode();
    }

    private void sendTestAck() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(this, "Not connected", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        btManager.sendTestAck();
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void onMessageReceived(byte type, byte[] payload) {
        String typeStr = getTypeString(type);
        String payloadStr = payload != null ? new String(payload) : "";
        log("RECV: " + typeStr + " | " + payloadStr);
        
        // Auto-respond with ACK if ACK_REQ flag was set
        // (This is handled in BluetoothConnectionManager)
    }

    private String getTypeString(byte type) {
        switch (type) {
            case 0x01: return "STATUS";
            case 0x02: return "TIME";
            case 0x03: return "CALL";
            case 0x04: return "NOTIFY";
            case 0x05: return "PING";
            case 0x10: return "ACK";
            default: return "UNKNOWN(0x" + String.format("%02X", type) + ")";
        }
    }
}

