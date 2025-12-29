package com.hardcopy.retrowatch.simulator.arduino;

/**
 * Message buffer matching Arduino implementation
 */
public class MessageBuffer {
    private final byte[][] buffer;
    private final int maxCount;
    private final int bufferSize;
    private int parsingLine = 0;
    private int currentDisplay = 0;
    
    public MessageBuffer(int maxCount, int bufferSize) {
        this.maxCount = maxCount;
        this.bufferSize = bufferSize;
        this.buffer = new byte[maxCount][bufferSize];
        init();
    }
    
    public void init() {
        for (int i = 0; i < maxCount; i++) {
            for (int j = 0; j < bufferSize; j++) {
                buffer[i][j] = 0x00;
            }
        }
        parsingLine = 0;
        currentDisplay = 0;
    }
    
    public void addMessage(byte[] data, int dataLength) {
        if (parsingLine >= maxCount || parsingLine < 0) {
            parsingLine = 0;
        }
        
        // Arduino sets buffer[0] = 0x01 (enabled) and buffer[size-1] = 0x00 (null terminator)
        buffer[parsingLine][0] = 0x01; // Enabled flag
        
        // Copy data starting at index 2 (after management bytes)
        int copyLength = Math.min(dataLength, bufferSize - 2);
        System.arraycopy(data, 0, buffer[parsingLine], 2, copyLength);
        
        // Set null terminator
        buffer[parsingLine][bufferSize - 1] = 0x00;
        
        parsingLine++;
        if (parsingLine >= maxCount) {
            parsingLine = 0;
        }
    }
    
    public boolean findNextMessage() {
        if (currentDisplay < 0 || currentDisplay >= maxCount) {
            currentDisplay = 0;
        }
        
        int startIndex = currentDisplay;
        do {
            if (buffer[currentDisplay][0] != 0x00) { // 0x00 means disabled
                return true;
            }
            currentDisplay++;
            if (currentDisplay >= maxCount) {
                currentDisplay = 0;
            }
        } while (currentDisplay != startIndex);
        
        return false;
    }
    
    public byte[] getCurrentMessage() {
        if (currentDisplay >= 0 && currentDisplay < maxCount) {
            return buffer[currentDisplay];
        }
        return null;
    }
    
    public void advanceDisplay() {
        currentDisplay++;
        if (currentDisplay >= maxCount) {
            currentDisplay = 0;
        }
    }
    
    public void resetDisplay() {
        currentDisplay = 0;
    }
    
    public int countMessages() {
        int count = 0;
        for (int i = 0; i < maxCount; i++) {
            if (buffer[i][0] != 0x00) {
                count++;
            }
        }
        return count;
    }
    
    public byte getCurrentId() {
        byte[] msg = getCurrentMessage();
        if (msg != null && msg.length > 3) {
            return msg[3]; // ID is at index 3 (after 0x01, 0x00, 0xF0)
        }
        return 0;
    }
    
    public byte getCurrentIcon() {
        byte[] msg = getCurrentMessage();
        if (msg != null && msg.length > 4) {
            return msg[4]; // Icon is at index 4
        }
        return 0;
    }
    
    public String getCurrentText() {
        byte[] msg = getCurrentMessage();
        if (msg != null && msg.length > 5) {
            // Text starts at index 5, find null terminator
            int textStart = 5;
            int textEnd = bufferSize - 1; // Last byte is null terminator
            for (int i = textStart; i < textEnd; i++) {
                if (msg[i] == 0x00 || msg[i] >= (byte)0xF0) {
                    textEnd = i;
                    break;
                }
            }
            if (textEnd > textStart) {
                return new String(msg, textStart, textEnd - textStart);
            }
        }
        return "";
    }
}

