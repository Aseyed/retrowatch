package com.hardcopy.smartglasses.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TCP Connection helper for testing with simulator
 */
public class TcpConnectionHelper {
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
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            return true;
        } catch (IOException e) {
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

