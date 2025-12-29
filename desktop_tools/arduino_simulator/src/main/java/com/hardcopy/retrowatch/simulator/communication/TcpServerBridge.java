package com.hardcopy.retrowatch.simulator.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP Server bridge for Android app connection
 */
public class TcpServerBridge {
    
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread serverThread;
    private Thread clientThread;
    private Consumer<byte[]> dataCallback;
    private boolean isRunning = false;
    private int port = 8888;
    
    public TcpServerBridge(Consumer<byte[]> dataCallback) {
        this.dataCallback = dataCallback;
    }
    
    public boolean startServer(int port) {
        if (isRunning) {
            stopServer();
        }
        
        this.port = port;
        
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            
            serverThread = new Thread(() -> {
                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    try {
                        Socket socket = serverSocket.accept();
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            socket.close(); // Reject if already connected
                            continue;
                        }
                        
                        clientSocket = socket;
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();
                        
                        // Start client data reader
                        clientThread = new Thread(() -> {
                            byte[] buffer = new byte[1024];
                            while (isRunning && clientSocket != null && !clientSocket.isClosed()) {
                                try {
                                    int read = inputStream.read(buffer);
                                    if (read > 0 && dataCallback != null) {
                                        byte[] data = new byte[read];
                                        System.arraycopy(buffer, 0, data, 0, read);
                                        dataCallback.accept(data);
                                    } else if (read < 0) {
                                        break; // Connection closed
                                    }
                                } catch (IOException e) {
                                    break;
                                }
                            }
                        });
                        clientThread.setDaemon(true);
                        clientThread.start();
                        
                    } catch (IOException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void stopServer() {
        isRunning = false;
        
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        clientSocket = null;
        inputStream = null;
        outputStream = null;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocket = null;
    }
    
    public boolean sendData(byte[] data) {
        if (!isRunning || clientSocket == null || clientSocket.isClosed() || outputStream == null) {
            return false;
        }
        
        try {
            outputStream.write(data);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean isRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }
    
    public boolean isClientConnected() {
        return clientSocket != null && !clientSocket.isClosed();
    }
    
    public int getPort() {
        return port;
    }
    
    public String getServerAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}

