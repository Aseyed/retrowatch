package com.hardcopy.retrowatch.simulator.protocol;

import java.util.function.Consumer;

/**
 * Parser for legacy RetroWatch protocol (0xFC/0xFD framing)
 * Matches Arduino parsing logic exactly
 */
public class LegacyProtocolParser {
    
    // Transaction states (matching Arduino)
    private static final byte TR_MODE_IDLE = 1;
    private static final byte TR_MODE_WAIT_CMD = 11;
    private static final byte TR_MODE_WAIT_MESSAGE = 101;
    private static final byte TR_MODE_WAIT_TIME = 111;
    private static final byte TR_MODE_WAIT_ID = 121;
    private static final int TR_MODE_WAIT_COMPLETE = 201;
    
    private int transactionPointer = TR_MODE_IDLE;
    private byte currentCommand = CommandTypes.CMD_TYPE_NONE;
    
    // Message parsing state
    private int msgParsingLine = 0;
    private int msgParsingChar = 0;
    private int emgParsingLine = 0;
    private int emgParsingChar = 0;
    private byte[] currentMessageBuffer = new byte[CommandTypes.MSG_BUFFER_MAX];
    
    // Time parsing state
    private int timeParsingIndex = 0;
    private byte[] timeBuffer = new byte[CommandTypes.TIME_BUFFER_MAX];
    
    // ID parsing state
    private byte idValue = 0;
    
    // Callbacks
    private Consumer<CommandData> commandCallback;
    
    public LegacyProtocolParser(Consumer<CommandData> commandCallback) {
        this.commandCallback = commandCallback;
        initTimeBuffer();
    }
    
    /**
     * Process incoming byte (matches Arduino receiveBluetoothData logic)
     */
    public boolean processByte(byte c) {
        // Special handling for 0xFF (matches Arduino code)
        if (c == 0xFF && transactionPointer != TR_MODE_WAIT_MESSAGE) {
            return false;
        }
        
        boolean transactionEnded = false;
        
        if (transactionPointer == TR_MODE_IDLE) {
            parseStartSignal(c);
        } else if (transactionPointer == TR_MODE_WAIT_CMD) {
            parseCommand(c);
        } else if (transactionPointer == TR_MODE_WAIT_MESSAGE) {
            parseMessage(c);
        } else if (transactionPointer == TR_MODE_WAIT_TIME) {
            parseTime(c);
        } else if (transactionPointer == TR_MODE_WAIT_ID) {
            parseId(c);
        } else if (transactionPointer == TR_MODE_WAIT_COMPLETE) {
            transactionEnded = parseEndSignal(c);
        }
        
        return transactionEnded;
    }
    
    private void parseStartSignal(byte c) {
        if (c == CommandTypes.TRANSACTION_START_BYTE) {
            transactionPointer = TR_MODE_WAIT_CMD;
            currentCommand = CommandTypes.CMD_TYPE_NONE;
        }
    }
    
    private void parseCommand(byte c) {
        if (c == CommandTypes.CMD_TYPE_RESET_EMERGENCY_OBJ || 
            c == CommandTypes.CMD_TYPE_RESET_NORMAL_OBJ || 
            c == CommandTypes.CMD_TYPE_RESET_USER_MESSAGE) {
            transactionPointer = TR_MODE_WAIT_COMPLETE;
            currentCommand = c;
            processTransaction();
        } else if (c == CommandTypes.CMD_TYPE_ADD_EMERGENCY_OBJ || 
                   c == CommandTypes.CMD_TYPE_ADD_NORMAL_OBJ || 
                   c == CommandTypes.CMD_TYPE_ADD_USER_MESSAGE) {
            transactionPointer = TR_MODE_WAIT_MESSAGE;
            currentCommand = c;
            // Clear message buffer
            for (int i = 0; i < currentMessageBuffer.length; i++) {
                currentMessageBuffer[i] = 0;
            }
            if (c == CommandTypes.CMD_TYPE_ADD_EMERGENCY_OBJ) {
                emgParsingChar = 0;
                if (emgParsingLine >= CommandTypes.EMG_COUNT_MAX || emgParsingLine < 0) {
                    emgParsingLine = 0;
                }
            } else if (c == CommandTypes.CMD_TYPE_ADD_NORMAL_OBJ) {
                msgParsingChar = 0;
                if (msgParsingLine >= CommandTypes.MSG_COUNT_MAX || msgParsingLine < 0) {
                    msgParsingLine = 0;
                }
            }
        } else if (c == CommandTypes.CMD_TYPE_DELETE_EMERGENCY_OBJ || 
                   c == CommandTypes.CMD_TYPE_DELETE_NORMAL_OBJ || 
                   c == CommandTypes.CMD_TYPE_DELETE_USER_MESSAGE) {
            transactionPointer = TR_MODE_WAIT_COMPLETE;
            currentCommand = c;
        } else if (c == CommandTypes.CMD_TYPE_SET_TIME) {
            transactionPointer = TR_MODE_WAIT_TIME;
            currentCommand = c;
            timeParsingIndex = 0;
            initTimeBuffer();
        } else if (c == CommandTypes.CMD_TYPE_SET_CLOCK_STYLE || 
                   c == CommandTypes.CMD_TYPE_SET_INDICATOR) {
            transactionPointer = TR_MODE_WAIT_ID;
            currentCommand = c;
        } else {
            transactionPointer = TR_MODE_IDLE;
            currentCommand = CommandTypes.CMD_TYPE_NONE;
        }
    }
    
    private void parseMessage(byte c) {
        if (c == CommandTypes.TRANSACTION_END_BYTE) {
            processTransaction();
            transactionPointer = TR_MODE_IDLE;
            return;
        }
        
        // Android sends: [0xFC][CMD][0xF0][ID][Icon][Text...][0xFD]
        // Arduino receives bytes after CMD: [0xF0][ID][Icon][Text...][0xFD]
        // Arduino stores in buffer: buffer[0]=0x01, buffer[1]=0x00, buffer[2]=0xF0, buffer[3]=ID, buffer[4]=Icon, buffer[5+]=Text
        // msgParsingChar counts from 0, but we skip storing bytes 0-1 (management), so:
        // - msgParsingChar=0: skip (will be set to 0x01 by processTransaction)
        // - msgParsingChar=1: skip (will be set to 0x00 by processTransaction)
        // - msgParsingChar=2: store at buffer[2] (0xF0)
        // - msgParsingChar=3: store at buffer[3] (ID)
        // - msgParsingChar=4: store at buffer[4] (Icon)
        // - msgParsingChar=5+: store at buffer[5+] (Text)
        
        if (currentCommand == CommandTypes.CMD_TYPE_ADD_EMERGENCY_OBJ) {
            if (emgParsingChar < CommandTypes.EMG_BUFFER_MAX - 1) {
                if (emgParsingChar > 1) {
                    // Store in buffer starting at index 2
                    int bufferIndex = emgParsingChar;
                    if (bufferIndex < currentMessageBuffer.length) {
                        currentMessageBuffer[bufferIndex] = c;
                    }
                }
                emgParsingChar++;
            } else {
                transactionPointer = TR_MODE_IDLE;
                processTransaction();
            }
        } else if (currentCommand == CommandTypes.CMD_TYPE_ADD_NORMAL_OBJ) {
            if (msgParsingChar < CommandTypes.MSG_BUFFER_MAX - 1) {
                if (msgParsingChar > 1) {
                    // Store in buffer starting at index 2
                    int bufferIndex = msgParsingChar;
                    if (bufferIndex < currentMessageBuffer.length) {
                        currentMessageBuffer[bufferIndex] = c;
                    }
                }
                msgParsingChar++;
            } else {
                transactionPointer = TR_MODE_IDLE;
                processTransaction();
            }
        } else if (currentCommand == CommandTypes.CMD_TYPE_ADD_USER_MESSAGE) {
            transactionPointer = TR_MODE_WAIT_COMPLETE;
        }
    }
    
    private void parseTime(byte c) {
        if (currentCommand == CommandTypes.CMD_TYPE_SET_TIME) {
            if (timeParsingIndex >= 0 && timeParsingIndex < CommandTypes.TIME_BUFFER_MAX) {
                timeBuffer[timeParsingIndex] = c;
                timeParsingIndex++;
            } else {
                processTransaction();
                transactionPointer = TR_MODE_WAIT_COMPLETE;
            }
        }
    }
    
    private void parseId(byte c) {
        idValue = c;
        if (currentCommand == CommandTypes.CMD_TYPE_SET_CLOCK_STYLE) {
            processTransaction();
        } else if (currentCommand == CommandTypes.CMD_TYPE_SET_INDICATOR) {
            processTransaction();
        }
        transactionPointer = TR_MODE_WAIT_COMPLETE;
    }
    
    private boolean parseEndSignal(byte c) {
        if (c == CommandTypes.TRANSACTION_END_BYTE) {
            transactionPointer = TR_MODE_IDLE;
            return true;
        }
        return false;
    }
    
    private void processTransaction() {
        CommandData cmd = new CommandData();
        cmd.command = currentCommand;
        
        if (currentCommand == CommandTypes.CMD_TYPE_SET_TIME) {
            cmd.timeData = new byte[CommandTypes.TIME_BUFFER_MAX];
            System.arraycopy(timeBuffer, 0, cmd.timeData, 0, CommandTypes.TIME_BUFFER_MAX);
        } else if (currentCommand == CommandTypes.CMD_TYPE_ADD_EMERGENCY_OBJ || 
                   currentCommand == CommandTypes.CMD_TYPE_ADD_NORMAL_OBJ) {
            // Extract message data: Android sends [0xF0][ID][Icon][Text...]
            // Arduino stores: buffer[0]=0x01, buffer[1]=0x00, buffer[2]=0xF0, buffer[3]=ID, buffer[4]=Icon, buffer[5+]=Text
            // msgParsingChar counts bytes received (including skipped 0-1), so:
            // - msgParsingChar=2: received 0xF0, stored at buffer[2]
            // - msgParsingChar=3: received ID, stored at buffer[3]
            // - msgParsingChar=4: received Icon, stored at buffer[4]
            // - msgParsingChar=5+: received Text bytes, stored at buffer[5+]
            if (msgParsingChar >= 5) {
                // Extract ID and Icon
                if (currentMessageBuffer.length > 3) {
                    cmd.id = currentMessageBuffer[3];
                }
                if (currentMessageBuffer.length > 4) {
                    cmd.iconType = currentMessageBuffer[4];
                }
                // Extract text (starts at buffer index 5, ends at msgParsingChar-1 or null terminator)
                int textStart = 5;
                int textEnd = Math.min(msgParsingChar, currentMessageBuffer.length - 1); // Leave room for null terminator
                // Find actual end (null terminator or 0xF0+ bytes)
                for (int i = textStart; i < textEnd; i++) {
                    if (currentMessageBuffer[i] == 0x00 || currentMessageBuffer[i] >= (byte)0xF0) {
                        textEnd = i;
                        break;
                    }
                }
                int textLength = Math.max(0, textEnd - textStart);
                if (textLength > 0) {
                    cmd.messageData = new byte[textLength];
                    System.arraycopy(currentMessageBuffer, textStart, cmd.messageData, 0, textLength);
                }
            }
        } else if (currentCommand == CommandTypes.CMD_TYPE_SET_CLOCK_STYLE || 
                   currentCommand == CommandTypes.CMD_TYPE_SET_INDICATOR) {
            cmd.id = idValue;
        }
        
        if (commandCallback != null) {
            commandCallback.accept(cmd);
        }
        
        // Reset message buffer for next transaction
        if (currentCommand == CommandTypes.CMD_TYPE_ADD_EMERGENCY_OBJ) {
            emgParsingChar = 0;
            if (emgParsingLine >= CommandTypes.EMG_COUNT_MAX || emgParsingLine < 0) {
                emgParsingLine = 0;
            }
        } else if (currentCommand == CommandTypes.CMD_TYPE_ADD_NORMAL_OBJ) {
            msgParsingChar = 0;
            if (msgParsingLine >= CommandTypes.MSG_COUNT_MAX || msgParsingLine < 0) {
                msgParsingLine = 0;
            }
        }
        
        // Clear message buffer
        for (int i = 0; i < currentMessageBuffer.length; i++) {
            currentMessageBuffer[i] = 0;
        }
    }
    
    private void initTimeBuffer() {
        for (int i = 0; i < CommandTypes.TIME_BUFFER_MAX; i++) {
            timeBuffer[i] = -1;
        }
    }
    
    public void reset() {
        transactionPointer = TR_MODE_IDLE;
        currentCommand = CommandTypes.CMD_TYPE_NONE;
        msgParsingLine = 0;
        msgParsingChar = 0;
        emgParsingLine = 0;
        emgParsingChar = 0;
        timeParsingIndex = 0;
        idValue = 0;
        initTimeBuffer();
        for (int i = 0; i < currentMessageBuffer.length; i++) {
            currentMessageBuffer[i] = 0;
        }
    }
    
    /**
     * Command data structure
     */
    public static class CommandData {
        public byte command;
        public byte[] timeData;
        public byte[] messageData;
        public byte id;
        public byte iconType;
    }
}

