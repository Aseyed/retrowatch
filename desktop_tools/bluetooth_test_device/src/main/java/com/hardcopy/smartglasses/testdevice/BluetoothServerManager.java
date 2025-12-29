package com.hardcopy.smartglasses.testdevice;

import com.hardcopy.smartglasses.protocol.ProtoV2;
import com.hardcopy.smartglasses.protocol.ProtoV2StreamDecoder;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

/**
 * Bluetooth/TCP Server Manager - Accepts connections from Android app
 * 
 * Uses TCP as a bridge for testing (Android connects via TCP instead of Bluetooth).
 * For production Bluetooth, would use javax.bluetooth or platform-specific library.
 */
public class BluetoothServerManager {
    private final MainPanel gui;
    private ProtoV2StreamDecoder decoder;
    private ServerSocket tcpServer;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread serverThread;
    private Thread readThread;
    private boolean running = false;
    private byte nextSeq = 0;
    private int serverPort = 8888; // Default TCP port

    public BluetoothServerManager(MainPanel gui) {
        this.gui = gui;
        this.decoder = new ProtoV2StreamDecoder(new ProtoV2StreamDecoder.Callback() {
            @Override
            public void onFrame(byte ver, byte type, byte flags, byte seq, byte[] payload) {
                handleReceivedFrame(type, flags, seq, payload);
            }
        });
    }

    /**
     * Start TCP server (acts as Bluetooth bridge for testing)
     */
    public boolean startServer() {
        if (running) {
            gui.log("Server already running");
            return true;
        }

        try {
            tcpServer = new ServerSocket(serverPort);
            running = true;
            
            serverThread = new Thread(() -> {
                gui.log("TCP Server started on port " + serverPort);
                gui.log("Waiting for Android app to connect...");
                gui.log("(Android app needs to connect to: " + getLocalIP() + ":" + serverPort + ")");
                
                while (running) {
                    try {
                        Socket socket = tcpServer.accept();
                        gui.log("✓ Client connected: " + socket.getRemoteSocketAddress());
                        
                        // Close previous connection if any
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                        
                        clientSocket = socket;
                        inputStream = clientSocket.getInputStream();
                        outputStream = clientSocket.getOutputStream();
                        
                        startReadThread();
                        
                        // Send connection status to Android
                        sendStatus(ProtoV2.STATUS_CONNECTED);
                        
                    } catch (IOException e) {
                        if (running) {
                            gui.log("Server accept error: " + e.getMessage());
                        }
                        break;
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
            return true;
        } catch (IOException e) {
            gui.log("Failed to start server: " + e.getMessage());
            running = false;
            return false;
        }
    }

    /**
     * Stop the server
     */
    public void stopServer() {
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
            if (clientSocket != null && !clientSocket.isClosed()) {
                sendStatus(ProtoV2.STATUS_DISCONNECTED);
                clientSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            if (tcpServer != null && !tcpServer.isClosed()) {
                tcpServer.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        clientSocket = null;
        inputStream = null;
        outputStream = null;
        tcpServer = null;
        
        gui.log("Server stopped");
    }

    private void startReadThread() {
        if (readThread != null && readThread.isAlive()) {
            return;
        }
        
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (running && clientSocket != null && !clientSocket.isClosed()) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        decoder.feed(buffer, 0, bytesRead);
                    } else if (bytesRead < 0) {
                        // Connection closed
                        gui.log("Client disconnected");
                        break;
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

    private void sendStatus(byte status) {
        byte[] payload = new byte[]{status};
        byte[] frame = ProtoV2.encode(ProtoV2.TYPE_STATUS, ProtoV2.FLAG_ACK_REQ, nextSeq++, payload);
        sendFrame(frame);
        gui.log("SENT: STATUS (" + (status == ProtoV2.STATUS_CONNECTED ? "CONNECTED" : "DISCONNECTED") + ")");
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

    public boolean isRunning() {
        return running;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    private String getLocalIP() {
        try {
            java.net.NetworkInterface networkInterface = java.net.NetworkInterface.getNetworkInterfaces()
                .asIterator().next();
            java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                java.net.InetAddress addr = addresses.nextElement();
                if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "localhost";
    }
}


