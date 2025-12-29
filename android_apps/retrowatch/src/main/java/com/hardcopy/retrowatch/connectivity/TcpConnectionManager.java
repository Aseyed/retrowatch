package com.hardcopy.retrowatch.connectivity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hardcopy.retrowatch.utils.Constants;
import com.hardcopy.retrowatch.utils.Logs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TCP Connection Manager for testing with simulator
 * Can be used as alternative to Bluetooth connection
 */
public class TcpConnectionManager {
    
    private static final String TAG = "TcpConnectionManager";
    
    // Connection state (matching BluetoothManager)
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    
    // Message types
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    private Handler mHandler;
    private int mState = STATE_NONE;
    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    
    // Default TCP settings (can be changed)
    private String mHost = "192.168.1.100"; // Change to your PC's IP
    private int mPort = 8888;
    
    public TcpConnectionManager(Handler handler) {
        mHandler = handler;
        mState = STATE_NONE;
    }
    
    public void setTcpAddress(String host, int port) {
        mHost = host;
        mPort = port;
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    private synchronized void setState(int state) {
        Logs.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    public synchronized void connect() {
        Logs.d(TAG, "connect to " + mHost + ":" + mPort);
        
        // Validate host and port before attempting connection
        if (mHost == null || mHost.isEmpty()) {
            Logs.e(TAG, "Cannot connect - host is null or empty");
            connectionFailed();
            return;
        }
        if (mPort <= 0 || mPort > 65535) {
            Logs.e(TAG, "Cannot connect - invalid port: " + mPort);
            connectionFailed();
            return;
        }
        
        if (mState == STATE_CONNECTING) {
            Logs.d(TAG, "Already connecting - canceling previous attempt");
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if (mState == STATE_CONNECTED) {
            Logs.d(TAG, "Already connected - disconnecting first");
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
            setState(STATE_NONE);
        }
        
        setState(STATE_CONNECTING);
        mConnectThread = new ConnectThread();
        mConnectThread.start();
    }
    
    public synchronized void stop() {
        Logs.d(TAG, "stop");
        
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        setState(STATE_NONE);
    }
    
    public synchronized void write(byte[] out) {
        if (mState != STATE_CONNECTED) {
            Logs.e(TAG, "Cannot write - not connected (state: " + mState + ")");
            return;
        }
        
        if (mConnectedThread == null) {
            Logs.e(TAG, "Cannot write - ConnectedThread is null");
            return;
        }
        
        if (out == null || out.length == 0) {
            Logs.e(TAG, "Cannot write - buffer is null or empty");
            return;
        }
        
        Logs.d(TAG, "Writing " + out.length + " bytes");
        mConnectedThread.write(out);
    }
    
    private synchronized void connected(Socket socket) {
        Logs.d(TAG, "connected to " + mHost + ":" + mPort);
        
        // Store socket reference
        mSocket = socket;
        
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        // Create and start connected thread
        mConnectedThread = new ConnectedThread(socket);
        
        // Validate socket is still connected
        if (!socket.isConnected() || socket.isClosed()) {
            Logs.e(TAG, "Socket is not connected or is closed");
            connectionFailed();
            return;
        }
        
        // Set state to connected BEFORE starting the thread
        setState(STATE_CONNECTED);
        
        // Start the connected thread
        mConnectedThread.start();
        
        // Send device name message
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS, mHost + ":" + mPort);
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME, "TCP Simulator");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        Logs.d(TAG, "Connection established and thread started - state is already CONNECTED");
    }
    
    private void connectionFailed() {
        setState(STATE_NONE);
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST, "Unable to connect to " + mHost + ":" + mPort);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    private void connectionLost() {
        setState(STATE_NONE);
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    private class ConnectThread extends Thread {
        private Socket mConnectSocket = null;
        
        public ConnectThread() {
            setName("ConnectThread");
        }
        
        public void run() {
            Logs.i(TAG, "BEGIN mConnectThread - connecting to " + mHost + ":" + mPort);
            
            try {
                // Create unconnected socket first
                mConnectSocket = new Socket();
                
                // Set socket options before connecting
                mConnectSocket.setSoTimeout(10000); // 10 second read timeout
                mConnectSocket.setTcpNoDelay(true); // Disable Nagle's algorithm for faster response
                mConnectSocket.setKeepAlive(true); // Enable keep-alive
                
                // Connect with timeout to prevent hanging
                java.net.InetSocketAddress socketAddress = new java.net.InetSocketAddress(mHost, mPort);
                mConnectSocket.connect(socketAddress, 5000); // 5 second connection timeout
                
                Logs.d(TAG, "Socket connected successfully, calling connected()");
                // Pass socket to ConnectedThread - don't close it here
                Socket socketToPass = mConnectSocket;
                mConnectSocket = null; // Clear reference so finally block doesn't close it
                connected(socketToPass);
            } catch (java.net.SocketTimeoutException e) {
                Logs.e(TAG, "Connection timeout to " + mHost + ":" + mPort + " - server not reachable");
                connectionFailed();
            } catch (java.net.UnknownHostException e) {
                Logs.e(TAG, "Unknown host: " + mHost + " - check server address");
                connectionFailed();
            } catch (java.net.ConnectException e) {
                Logs.e(TAG, "Connection refused to " + mHost + ":" + mPort + " - server may not be running");
                connectionFailed();
            } catch (IOException e) {
                Logs.e(TAG, "Connection failed to " + mHost + ":" + mPort + ": " + e.getMessage());
                connectionFailed();
            } catch (Exception e) {
                Logs.e(TAG, "Unexpected error during connection: " + e.getMessage());
                connectionFailed();
            } finally {
                // Only clean up if connection failed (socket not passed to ConnectedThread)
                if (mConnectSocket != null) {
                    try {
                        Logs.d(TAG, "Cleaning up failed connection socket");
                        mConnectSocket.close();
                    } catch (IOException ignored) {}
                }
            }
        }
        
        public void cancel() {
            Logs.d(TAG, "ConnectThread cancel() called");
            try {
                if (mConnectSocket != null && !mConnectSocket.isClosed()) {
                    mConnectSocket.close();
                    Logs.d(TAG, "ConnectThread socket closed");
                }
            } catch (IOException e) {
                Logs.e(TAG, "close() of connect socket failed: " + e.getMessage());
            }
            // Interrupt the thread to stop blocking operations
            interrupt();
        }
    }
    
    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private int consecutiveTimeouts = 0;
        private static final int MAX_CONSECUTIVE_TIMEOUTS = 100; // ~16 minutes at 10s timeout
        
        public ConnectedThread(Socket socket) {
            Logs.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                // Set socket options to prevent hanging
                socket.setSoTimeout(10000); // 10 second read timeout
                socket.setTcpNoDelay(true); // Disable Nagle's algorithm
                socket.setKeepAlive(true); // Enable keep-alive
                
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Logs.d(TAG, "Streams created successfully");
            } catch (IOException e) {
                Logs.e(TAG, "Failed to get streams: " + e.getMessage());
                // Don't set streams to null - connection will fail
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
            // Validate streams
            if (mmInStream == null || mmOutStream == null) {
                Logs.e(TAG, "Streams are null - connection will fail");
            }
        }
        
        public void run() {
            Logs.i(TAG, "BEGIN mConnectedThread");
            
            // Check if streams are valid
            if (mmInStream == null) {
                Logs.e(TAG, "InputStream is null - cannot read");
                connectionLost();
                return;
            }
            
            if (mmOutStream == null) {
                Logs.e(TAG, "OutputStream is null - cannot write");
                connectionLost();
                return;
            }
            
            Logs.d(TAG, "ConnectedThread started - socket connected: " + mmSocket.isConnected() + ", closed: " + mmSocket.isClosed());
            
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (mState == STATE_CONNECTED && !Thread.currentThread().isInterrupted()) {
                try {
                    // Check if socket is still connected before reading
                    if (mmSocket.isClosed() || !mmSocket.isConnected()) {
                        Logs.d(TAG, "Socket is closed or disconnected - exiting read loop");
                        break;
                    }
                    
                    Logs.d(TAG, "Reading from socket (timeout: 10s)...");
                    bytes = mmInStream.read(buffer);
                    consecutiveTimeouts = 0; // Reset timeout counter on successful read
                    
                    if (bytes > 0) {
                        Logs.d(TAG, "Received " + bytes + " bytes from server");
                        // Create a copy of the buffer to avoid race conditions
                        byte[] readBuf = new byte[bytes];
                        System.arraycopy(buffer, 0, readBuf, 0, bytes);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, readBuf)
                                .sendToTarget();
                    } else if (bytes < 0) {
                        // End of stream - server closed connection
                        Logs.w(TAG, "End of stream detected (EOF) - server closed connection");
                        break;
                    } else {
                        // bytes == 0 - shouldn't happen with blocking read
                        Logs.w(TAG, "Read returned 0 bytes (unexpected)");
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout is normal - just continue reading
                    // This prevents the thread from hanging
                    consecutiveTimeouts++;
                    if (consecutiveTimeouts % 10 == 0) {
                        Logs.d(TAG, "Read timeout (normal) - " + consecutiveTimeouts + " consecutive timeouts");
                    }
                    
                    // Check if we should still be connected
                    if (mState != STATE_CONNECTED) {
                        Logs.d(TAG, "State changed during timeout - exiting read loop");
                        break;
                    }
                    
                    // Safety check: if too many timeouts, something might be wrong
                    if (consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
                        Logs.e(TAG, "Too many consecutive timeouts - closing connection");
                        connectionLost();
                        break;
                    }
                    
                    continue;
                } catch (java.net.SocketException e) {
                    Logs.e(TAG, "Socket error: " + e.getMessage());
                    connectionLost();
                    break;
                } catch (IOException e) {
                    Logs.e(TAG, "Read error: " + e.getMessage());
                    connectionLost();
                    break;
                } catch (Exception e) {
                    Logs.e(TAG, "Unexpected error in read loop: " + e.getMessage());
                    connectionLost();
                    break;
                }
            }
            Logs.d(TAG, "ConnectedThread exiting");
        }
        
        public void write(byte[] buffer) {
            if (mmOutStream == null) {
                Logs.e(TAG, "Cannot write - OutputStream is null");
                return;
            }
            
            if (buffer == null || buffer.length == 0) {
                Logs.e(TAG, "Cannot write - buffer is null or empty");
                return;
            }
            
            // Check socket state before writing
            if (mmSocket.isClosed() || !mmSocket.isConnected()) {
                Logs.e(TAG, "Cannot write - socket is closed or disconnected");
                connectionLost();
                return;
            }
            
            try {
                Logs.d(TAG, "Writing " + buffer.length + " bytes to server");
                mmOutStream.write(buffer);
                mmOutStream.flush(); // Ensure data is sent immediately
                Logs.d(TAG, "Successfully wrote and flushed " + buffer.length + " bytes");
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Logs.e(TAG, "Exception during write: " + e.getMessage(), e);
                connectionLost();
            } catch (NullPointerException e) {
                Logs.e(TAG, "NullPointerException during write - stream may be closed");
                connectionLost();
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Logs.e(TAG, "close() of connected socket failed: " + e.getMessage());
            }
        }
    }
}

