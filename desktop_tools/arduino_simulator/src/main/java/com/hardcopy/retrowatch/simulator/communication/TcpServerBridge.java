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
    private Runnable connectionCallback; // Called when client connects/disconnects
    private boolean isRunning = false;
    private int port = 8888;
    
    public TcpServerBridge(Consumer<byte[]> dataCallback) {
        this.dataCallback = dataCallback;
    }
    
    public void setConnectionCallback(Runnable callback) {
        this.connectionCallback = callback;
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
                        
                        // Close previous connection if any
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            // Stop old read thread
                            if (clientThread != null && clientThread.isAlive()) {
                                clientThread.interrupt();
                                try {
                                    clientThread.join(500);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                        
                        clientSocket = socket;
                        // Set socket timeouts to prevent hanging
                        clientSocket.setSoTimeout(30000); // 30 second read timeout
                        clientSocket.setTcpNoDelay(true); // Disable Nagle's algorithm
                        clientSocket.setKeepAlive(true); // Enable keep-alive
                        
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();
                        
                        // Notify that a new client connected (for parser reset)
                        if (connectionCallback != null) {
                            connectionCallback.run();
                        }
                        
                        // Start client data reader
                        clientThread = new Thread(() -> {
                            byte[] buffer = new byte[1024];
                            System.out.println("[TcpServerBridge] Read thread started - waiting for data...");
                            
                            while (isRunning && clientSocket != null && !clientSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
                                try {
                                    int read = inputStream.read(buffer);
                                    if (read > 0 && dataCallback != null) {
                                        // Log raw bytes received for debugging
                                        System.out.println("[TcpServerBridge] Received " + read + " bytes");
                                        
                                        byte[] data = new byte[read];
                                        System.arraycopy(buffer, 0, data, 0, read);
                                        dataCallback.accept(data);
                                    } else if (read < 0) {
                                        // Connection closed
                                        System.out.println("[TcpServerBridge] Client disconnected (EOF)");
                                        break;
                                    } else if (read == 0) {
                                        // Shouldn't happen with blocking read, but handle it
                                        System.out.println("[TcpServerBridge] Read returned 0 bytes");
                                        Thread.sleep(100); // Small delay to avoid tight loop
                                    }
                                } catch (java.net.SocketTimeoutException e) {
                                    // Timeout is normal - just continue reading
                                    // This prevents the thread from hanging
                                    continue;
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    System.out.println("[TcpServerBridge] Read thread interrupted");
                                    break;
                                } catch (IOException e) {
                                    if (isRunning && !Thread.currentThread().isInterrupted()) {
                                        System.err.println("[TcpServerBridge] Read error: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                    break;
                                }
                            }
                            System.out.println("[TcpServerBridge] Read thread exiting");
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

