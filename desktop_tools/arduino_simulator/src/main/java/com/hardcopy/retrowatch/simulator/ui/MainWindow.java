package com.hardcopy.retrowatch.simulator.ui;

import com.hardcopy.retrowatch.simulator.arduino.ArduinoSimulator;
import com.hardcopy.retrowatch.simulator.arduino.ArduinoSimulator.DisplayUpdate;
import com.hardcopy.retrowatch.simulator.communication.ComPortBridge;
import com.hardcopy.retrowatch.simulator.communication.TcpServerBridge;
import com.hardcopy.retrowatch.simulator.protocol.CommandTypes;
import com.hardcopy.retrowatch.simulator.protocol.LegacyProtocolParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main application window
 */
public class MainWindow extends JFrame {
    
    private LCDPanel lcdPanel;
    private LogPanel logPanel;
    
    private JComboBox<String> portCombo;
    private JButton refreshButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;
    
    private JRadioButton comPortMode;
    private JRadioButton tcpServerMode;
    private JButton startServerButton;
    private JButton stopServerButton;
    
    private ComPortBridge comPortBridge;
    private TcpServerBridge tcpServerBridge;
    private LegacyProtocolParser protocolParser;
    private ArduinoSimulator arduinoSimulator;
    
    private Timer updateTimer;
    private boolean isConnected = false;
    private boolean isServerMode = false;
    
    public MainWindow() {
        setTitle("RetroWatch Arduino Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Initialize components
        lcdPanel = new LCDPanel();
        logPanel = new LogPanel();
        
        // Initialize simulators
        arduinoSimulator = new ArduinoSimulator();
        arduinoSimulator.setDisplayUpdateCallback(this::onDisplayUpdate);
        
        protocolParser = new LegacyProtocolParser(this::onCommandReceived);
        
        comPortBridge = new ComPortBridge(this::onDataReceived);
        tcpServerBridge = new TcpServerBridge(this::onDataReceived);
        
        // Create UI
        createUI();
        
        // Start update timer (100ms interval for smooth display updates)
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                arduinoSimulator.update();
            }
        }, 0, 100);
        
        // Initial display
        ArduinoSimulator.DisplayUpdate initialUpdate = new ArduinoSimulator.DisplayUpdate();
        initialUpdate.mode = com.hardcopy.retrowatch.simulator.arduino.DisplayMode.START_UP;
        initialUpdate.timeState = new com.hardcopy.retrowatch.simulator.arduino.TimeState();
        initialUpdate.showStartup = true;
        onDisplayUpdate(initialUpdate);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        log("RetroWatch Arduino Simulator started");
        log("Select connection method and connect to begin");
    }
    
    private void createUI() {
        // Top: Connection panel
        JPanel connectionPanel = createConnectionPanel();
        add(connectionPanel, BorderLayout.NORTH);
        
        // Center: LCD and Log
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setLeftComponent(lcdPanel);
        centerSplit.setRightComponent(logPanel);
        centerSplit.setResizeWeight(0.5);
        centerSplit.setDividerLocation(550);
        add(centerSplit, BorderLayout.CENTER);
        
        // Bottom: Control buttons
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));
        
        // Mode selection
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup modeGroup = new ButtonGroup();
        comPortMode = new JRadioButton("COM Port", true);
        tcpServerMode = new JRadioButton("TCP Server", false);
        modeGroup.add(comPortMode);
        modeGroup.add(tcpServerMode);
        
        comPortMode.addActionListener(e -> {
            isServerMode = false;
            updateUIForMode();
        });
        tcpServerMode.addActionListener(e -> {
            isServerMode = true;
            updateUIForMode();
        });
        
        modePanel.add(comPortMode);
        modePanel.add(tcpServerMode);
        panel.add(modePanel, BorderLayout.NORTH);
        
        // Status
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.CENTER);
        
        // Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // COM Port controls
        JPanel comPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comPanel.add(new JLabel("Port:"));
        portCombo = new JComboBox<>();
        portCombo.setPreferredSize(new Dimension(150, 25));
        comPanel.add(portCombo);
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshPorts());
        comPanel.add(refreshButton);
        
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        comPanel.add(connectButton);
        
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());
        comPanel.add(disconnectButton);
        
        // TCP Server controls
        JPanel tcpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startServerButton = new JButton("Start Server");
        startServerButton.addActionListener(e -> startServer());
        tcpPanel.add(startServerButton);
        
        stopServerButton = new JButton("Stop Server");
        stopServerButton.setEnabled(false);
        stopServerButton.addActionListener(e -> stopServer());
        tcpPanel.add(stopServerButton);
        
        controlPanel.add(comPanel);
        controlPanel.add(tcpPanel);
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        updateUIForMode();
        refreshPorts();
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logPanel.clear());
        panel.add(clearLogButton);
        
        return panel;
    }
    
    private void updateUIForMode() {
        boolean comMode = !isServerMode;
        portCombo.setEnabled(comMode);
        refreshButton.setEnabled(comMode);
        connectButton.setEnabled(comMode && !isConnected);
        disconnectButton.setEnabled(comMode && isConnected);
        startServerButton.setEnabled(isServerMode && !tcpServerBridge.isRunning());
        stopServerButton.setEnabled(isServerMode && tcpServerBridge.isRunning());
    }
    
    private void refreshPorts() {
        String[] ports = comPortBridge.getAvailablePorts();
        portCombo.removeAllItems();
        for (String port : ports) {
            portCombo.addItem(port);
        }
        if (ports.length == 0) {
            log("No COM ports found");
        } else {
            log("Found " + ports.length + " COM port(s)");
        }
    }
    
    private void connect() {
        String portName = (String) portCombo.getSelectedItem();
        if (portName == null || portName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a COM port", 
                "No Port Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        log("Connecting to " + portName + " at 9600 baud...");
        if (comPortBridge.connect(portName, 9600)) {
            isConnected = true;
            statusLabel.setText("Status: Connected to " + portName);
            statusLabel.setForeground(Color.GREEN);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            log("Connected successfully");
        } else {
            statusLabel.setText("Status: Connection Failed");
            statusLabel.setForeground(Color.RED);
            log("Connection failed");
            JOptionPane.showMessageDialog(this, "Failed to connect to " + portName, 
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
        updateUIForMode();
    }
    
    private void disconnect() {
        comPortBridge.disconnect();
        isConnected = false;
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        log("Disconnected");
        updateUIForMode();
    }
    
    private void startServer() {
        int port = 8888;
        log("Starting TCP server on port " + port + "...");
        if (tcpServerBridge.startServer(port)) {
            statusLabel.setText("Status: Server Running (" + 
                tcpServerBridge.getServerAddress() + ":" + port + ")");
            statusLabel.setForeground(Color.GREEN);
            startServerButton.setEnabled(false);
            stopServerButton.setEnabled(true);
            log("TCP server started. Android app can connect to: " + 
                tcpServerBridge.getServerAddress() + ":" + port);
        } else {
            statusLabel.setText("Status: Server Start Failed");
            statusLabel.setForeground(Color.RED);
            log("Failed to start TCP server");
        }
        updateUIForMode();
    }
    
    private void stopServer() {
        tcpServerBridge.stopServer();
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        startServerButton.setEnabled(true);
        stopServerButton.setEnabled(false);
        log("TCP server stopped");
        updateUIForMode();
    }
    
    private void onDataReceived(byte[] data) {
        log("RECV", "Received " + data.length + " bytes");
        
        // Process each byte through protocol parser
        for (byte b : data) {
            protocolParser.processByte(b);
        }
    }
    
    private void onCommandReceived(LegacyProtocolParser.CommandData cmd) {
        log("CMD", CommandTypes.getCommandName(cmd.command));
        arduinoSimulator.processCommand(cmd);
    }
    
    private void onDisplayUpdate(DisplayUpdate update) {
        SwingUtilities.invokeLater(() -> {
            BufferedImage display = lcdPanel.getLCDSimulator().render(update);
            lcdPanel.updateDisplay(display);
        });
    }
    
    private void log(String message) {
        logPanel.log(message);
    }
    
    private void log(String level, String message) {
        logPanel.log(level, message);
    }
}

