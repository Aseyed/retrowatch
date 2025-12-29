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
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }
    
    private synchronized void connected(Socket socket) {
        Logs.d(TAG, "connected");
        
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS, mHost + ":" + mPort);
        bundle.putString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME, "TCP Simulator");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        setState(STATE_CONNECTED);
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
        public ConnectThread() {
            setName("ConnectThread");
        }
        
        public void run() {
            Logs.i(TAG, "BEGIN mConnectThread");
            
            try {
                Socket socket = new Socket(mHost, mPort);
                connected(socket);
            } catch (IOException e) {
                Logs.e(TAG, "Connection failed: " + e.getMessage());
                connectionFailed();
            }
        }
        
        public void cancel() {
            try {
                if (mSocket != null) {
                    mSocket.close();
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
            } catch (IOException e) {
                Logs.e(TAG, "temp sockets not created: " + e.getMessage());
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        
        public void run() {
            Logs.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    Logs.e(TAG, "disconnected: " + e.getMessage());
                    connectionLost();
                    break;
                }
            }
        }
        
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Logs.e(TAG, "Exception during write: " + e.getMessage());
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

