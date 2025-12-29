package com.hardcopy.retrowatch.simulator.communication;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * COM Port bridge for virtual serial port communication
 */
public class ComPortBridge {
    
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Consumer<byte[]> dataCallback;
    private boolean isConnected = false;
    private Thread readThread;
    
    public ComPortBridge(Consumer<byte[]> dataCallback) {
        this.dataCallback = dataCallback;
    }
    
    public String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }
        return portNames;
    }
    
    public boolean connect(String portName, int baudRate) {
        if (isConnected) {
            disconnect();
        }
        
        SerialPort[] ports = SerialPort.getCommPorts();
        SerialPort port = null;
        for (SerialPort p : ports) {
            if (p.getSystemPortName().equals(portName)) {
                port = p;
                break;
            }
        }
        
        if (port == null) {
            return false;
        }
        
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        
        if (!port.openPort()) {
            return false;
        }
        
        serialPort = port;
        inputStream = port.getInputStream();
        outputStream = port.getOutputStream();
        
        // Start read thread
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isConnected && !Thread.currentThread().isInterrupted()) {
                try {
                    int available = inputStream.available();
                    if (available > 0) {
                        int read = inputStream.read(buffer, 0, Math.min(available, buffer.length));
                        if (read > 0 && dataCallback != null) {
                            byte[] data = new byte[read];
                            System.arraycopy(buffer, 0, data, 0, read);
                            dataCallback.accept(data);
                        }
                    } else {
                        Thread.sleep(10); // Small delay to avoid busy waiting
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        e.printStackTrace();
                    }
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();
        
        isConnected = true;
        return true;
    }
    
    public void disconnect() {
        isConnected = false;
        if (readThread != null) {
            readThread.interrupt();
            try {
                readThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            readThread = null;
        }
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        serialPort = null;
        inputStream = null;
        outputStream = null;
    }
    
    public boolean sendData(byte[] data) {
        if (!isConnected || outputStream == null) {
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
    
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }
    
    public String getPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : null;
    }
}

