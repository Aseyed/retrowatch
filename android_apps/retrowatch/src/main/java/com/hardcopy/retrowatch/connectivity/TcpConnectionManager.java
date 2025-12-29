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
        
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if (mState == STATE_CONNECTED) {
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
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
        
        mConnectedThread.start();
        
        // Send device name message
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS, mHost + ":" + mPort);
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME, "TCP Simulator");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        setState(STATE_CONNECTED);
        Logs.d(TAG, "Connection established and thread started");
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
                mConnectSocket = new Socket(mHost, mPort);
                Logs.d(TAG, "Socket created, calling connected()");
                connected(mConnectSocket);
            } catch (IOException e) {
                Logs.e(TAG, "Connection failed to " + mHost + ":" + mPort + ": " + e.getMessage());
                connectionFailed();
            }
        }
        
        public void cancel() {
            try {
                if (mConnectSocket != null && !mConnectSocket.isClosed()) {
                    mConnectSocket.close();
                    Logs.d(TAG, "ConnectThread socket closed");
                }
            } catch (IOException e) {
                Logs.e(TAG, "close() of connect socket failed: " + e.getMessage());
            }
        }
    }
    
    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        public ConnectedThread(Socket socket) {
            Logs.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
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
            
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (mState == STATE_CONNECTED && !Thread.currentThread().isInterrupted()) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                        // Create a copy of the buffer to avoid race conditions
                        byte[] readBuf = new byte[bytes];
                        System.arraycopy(buffer, 0, readBuf, 0, bytes);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, readBuf)
                                .sendToTarget();
                    } else if (bytes < 0) {
                        // End of stream
                        Logs.d(TAG, "End of stream detected");
                        break;
                    }
                } catch (IOException e) {
                    Logs.e(TAG, "Read error: " + e.getMessage());
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
            
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush(); // Ensure data is sent immediately
                Logs.d(TAG, "Wrote " + buffer.length + " bytes");
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Logs.e(TAG, "Exception during write: " + e.getMessage());
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

