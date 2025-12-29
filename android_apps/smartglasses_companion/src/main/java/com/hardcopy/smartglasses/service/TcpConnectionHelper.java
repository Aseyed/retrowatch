package com.hardcopy.smartglasses.service;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TCP Connection helper for testing with simulator
 */
public class TcpConnectionHelper {
    private static final String TAG = "TcpConnectionHelper";
    
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private String host;
    private int port;
    
    public TcpConnectionHelper(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public boolean connect() {
        try {
            Log.d(TAG, "Connecting to " + host + ":" + port);
            
            // Create socket with timeout
            socket = new Socket();
            socket.setSoTimeout(30000); // 30 second read timeout
            socket.setTcpNoDelay(true); // Disable Nagle's algorithm for faster response
            socket.setKeepAlive(true); // Enable keep-alive
            
            // Connect with timeout
            java.net.InetSocketAddress socketAddress = new java.net.InetSocketAddress(host, port);
            socket.connect(socketAddress, 5000); // 5 second connection timeout
            
            in = socket.getInputStream();
            out = socket.getOutputStream();
            
            Log.d(TAG, "Connected successfully to " + host + ":" + port);
            Log.d(TAG, "Socket configured: SoTimeout=30s, TCP_NODELAY=true, KeepAlive=true");
            return true;
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Connection timeout to " + host + ":" + port);
            return false;
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Unknown host: " + host);
            return false;
        } catch (java.net.ConnectException e) {
            Log.e(TAG, "Connection refused to " + host + ":" + port + " - server may not be running");
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Connection failed to " + host + ":" + port + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        socket = null;
        in = null;
        out = null;
    }
    
    public InputStream getInputStream() {
        return in;
    }
    
    public OutputStream getOutputStream() {
        return out;
    }
    
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}

