package com.hardcopy.smartglasses.testdevice;

import com.fazecast.jSerialComm.SerialPort;
import com.hardcopy.smartglasses.protocol.ProtoV2;
import com.hardcopy.smartglasses.protocol.ProtoV2StreamDecoder;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Bluetooth connection and Protocol v2 communication
 * 
 * Note: This uses a simplified approach. For Windows, you may need to:
 * 1. Use javax.bluetooth library (add to classpath)
 * 2. Or use a serial port library if Bluetooth device appears as COM port
 * 3. Or use a platform-specific JNI wrapper
 */
public class BluetoothConnectionManager {
    private final MainPanel gui;
    private ProtoV2StreamDecoder decoder;
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread readThread;
    private boolean running = false;
    private byte nextSeq = 0;

    public BluetoothConnectionManager(MainPanel gui) {
        this.gui = gui;
        this.decoder = new ProtoV2StreamDecoder(new ProtoV2StreamDecoder.Callback() {
            @Override
            public void onFrame(byte ver, byte type, byte flags, byte seq, byte[] payload) {
                handleReceivedFrame(type, flags, seq, payload);
            }
        });
    }

    public void refreshDevices(JComboBox<String> combo) {
        combo.removeAllItems();
        gui.log("Scanning for serial/COM ports...");
        
        // Get all available serial ports (Bluetooth devices often appear as COM ports on Windows)
        SerialPort[] ports = SerialPort.getCommPorts();
        
        if (ports.length == 0) {
            combo.addItem("(No COM ports found)");
            gui.log("⚠ No COM ports detected.");
            gui.log("");
            gui.log("TROUBLESHOOTING:");
            gui.log("1. Check Device Manager → Ports (COM & LPT)");
            gui.log("2. If you see COM ports there, jSerialComm may need admin rights");
            gui.log("3. Try running this app as Administrator");
            gui.log("");
            gui.log("NOTE: This app connects via COM ports.");
            gui.log("For Android-to-PC testing, you may need:");
            gui.log("  - A Bluetooth USB dongle that creates COM ports, OR");
            gui.log("  - Direct Bluetooth SPP (requires javax.bluetooth library)");
            gui.log("");
            gui.log("Alternative: Test with actual Arduino hardware instead.");
        } else {
            for (SerialPort port : ports) {
                String desc = port.getDescriptivePortName();
                String systemName = port.getSystemPortName();
                String item = systemName + " - " + desc;
                combo.addItem(item);
                gui.log("✓ Found: " + item);
            }
            gui.log("");
            gui.log("Found " + ports.length + " port(s). Select one to connect.");
        }
    }

    public boolean connect(String deviceName) {
        if (deviceName == null || deviceName.isEmpty() || deviceName.contains("(No")) {
            gui.log("Invalid device selected");
            return false;
        }
        
        // Extract COM port name (e.g., "COM3 - Bluetooth" -> "COM3")
        String portName = deviceName.split(" - ")[0].trim();
        gui.log("Connecting to: " + portName);
        
        try {
            // Find the port
            SerialPort[] ports = SerialPort.getCommPorts();
            SerialPort targetPort = null;
            for (SerialPort port : ports) {
                if (port.getSystemPortName().equals(portName)) {
                    targetPort = port;
                    break;
                }
            }
            
            if (targetPort == null) {
                gui.log("Port not found: " + portName);
                return false;
            }
            
            // Configure port
            targetPort.setBaudRate(9600); // Standard for Bluetooth modules
            targetPort.setNumDataBits(8);
            targetPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            targetPort.setParity(SerialPort.NO_PARITY);
            targetPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
            
            // Open port
            if (!targetPort.openPort()) {
                gui.log("Failed to open port: " + portName);
                return false;
            }
            
            serialPort = targetPort;
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            
            running = true;
            startReadThread();
            
            gui.log("✓ Connected successfully to " + portName);
            gui.log("  Baud: 9600, Data: 8, Stop: 1, Parity: None");
            return true;
            
        } catch (Exception e) {
            gui.log("Connection error: " + e.getMessage());
            e.printStackTrace();
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
            return false;
        }
    }

    public void disconnect() {
        running = false;
        if (readThread != null) {
            readThread.interrupt();
            try {
                readThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        } catch (IOException e) {
            // Ignore
        }
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            gui.log("Port closed");
        }
        serialPort = null;
        inputStream = null;
        outputStream = null;
    }

    private void startReadThread() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (running) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        decoder.feed(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    if (running) {
                        gui.log("Read error: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    private void handleReceivedFrame(byte type, byte flags, byte seq, byte[] payload) {
        gui.onMessageReceived(type, payload);
        
        // Auto-send ACK if ACK_REQ flag is set
        if ((flags & ProtoV2.FLAG_ACK_REQ) != 0) {
            sendAck(type, seq, (byte) 0x00); // 0x00 = OK
        }
    }

    private void sendAck(byte ackType, byte ackSeq, byte result) {
        byte[] payload = new byte[]{ackType, ackSeq, result};
        byte[] frame = ProtoV2.encode(ProtoV2.TYPE_ACK, (byte) 0, nextSeq++, payload);
        sendFrame(frame);
        gui.log("SENT: ACK (type=" + ackType + ", seq=" + ackSeq + ")");
    }

    public void sendTestAck() {
        sendAck(ProtoV2.TYPE_PING, (byte) 0, (byte) 0x00);
    }

    private void sendFrame(byte[] frame) {
        if (outputStream == null) {
            gui.log("Cannot send: not connected");
            return;
        }
        try {
            outputStream.write(frame);
            outputStream.flush();
        } catch (IOException e) {
            gui.log("Send error: " + e.getMessage());
        }
    }

    private String getTypeString(byte type) {
        switch (type) {
            case ProtoV2.TYPE_STATUS: return "STATUS";
            case ProtoV2.TYPE_TIME: return "TIME";
            case ProtoV2.TYPE_CALL: return "CALL";
            case ProtoV2.TYPE_NOTIFY: return "NOTIFY";
            case ProtoV2.TYPE_PING: return "PING";
            case ProtoV2.TYPE_ACK: return "ACK";
            default: return "UNKNOWN(0x" + String.format("%02X", type) + ")";
        }
    }
}

