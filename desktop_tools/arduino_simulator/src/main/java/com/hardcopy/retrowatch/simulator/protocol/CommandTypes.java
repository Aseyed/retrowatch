package com.hardcopy.retrowatch.simulator.protocol;

/**
 * Command type constants matching Arduino code
 */
public final class CommandTypes {
    private CommandTypes() {}
    
    // Command types
    public static final byte CMD_TYPE_NONE = 0x00;
    public static final byte CMD_TYPE_RESET_EMERGENCY_OBJ = 0x05;
    public static final byte CMD_TYPE_RESET_NORMAL_OBJ = 0x02;
    public static final byte CMD_TYPE_RESET_USER_MESSAGE = 0x03;
    
    public static final byte CMD_TYPE_ADD_EMERGENCY_OBJ = 0x11;
    public static final byte CMD_TYPE_ADD_NORMAL_OBJ = 0x12;
    public static final byte CMD_TYPE_ADD_USER_MESSAGE = 0x13;
    
    public static final byte CMD_TYPE_DELETE_EMERGENCY_OBJ = 0x21;
    public static final byte CMD_TYPE_DELETE_NORMAL_OBJ = 0x22;
    public static final byte CMD_TYPE_DELETE_USER_MESSAGE = 0x23;
    
    public static final byte CMD_TYPE_SET_TIME = 0x31;
    public static final byte CMD_TYPE_REQUEST_MOVEMENT_HISTORY = 0x32;
    public static final byte CMD_TYPE_SET_CLOCK_STYLE = 0x33;
    public static final byte CMD_TYPE_SET_INDICATOR = 0x34;
    
    public static final byte CMD_TYPE_PING = 0x51;
    public static final byte CMD_TYPE_AWAKE = 0x52;
    public static final byte CMD_TYPE_SLEEP = 0x53;
    public static final byte CMD_TYPE_REBOOT = 0x54;
    
    // Transaction framing
    public static final byte TRANSACTION_START_BYTE = (byte)0xFC;
    public static final byte TRANSACTION_END_BYTE = (byte)0xFD;
    
    // Clock styles
    public static final byte CLOCK_STYLE_SIMPLE_ANALOG = 0x01;
    public static final byte CLOCK_STYLE_SIMPLE_DIGIT = 0x02;
    public static final byte CLOCK_STYLE_SIMPLE_MIX = 0x03;
    
    // Indicator
    public static final byte INDICATOR_ENABLE = 0x01;
    
    // Buffer sizes (matching Arduino)
    public static final int MSG_COUNT_MAX = 7;
    public static final int MSG_BUFFER_MAX = 19;
    public static final int EMG_COUNT_MAX = 3;
    public static final int EMG_BUFFER_MAX = 19;
    public static final int TIME_BUFFER_MAX = 6;
    
    public static String getCommandName(byte cmd) {
        switch (cmd) {
            case CMD_TYPE_NONE: return "NONE";
            case CMD_TYPE_RESET_EMERGENCY_OBJ: return "RESET_EMERGENCY_OBJ";
            case CMD_TYPE_RESET_NORMAL_OBJ: return "RESET_NORMAL_OBJ";
            case CMD_TYPE_RESET_USER_MESSAGE: return "RESET_USER_MESSAGE";
            case CMD_TYPE_ADD_EMERGENCY_OBJ: return "ADD_EMERGENCY_OBJ";
            case CMD_TYPE_ADD_NORMAL_OBJ: return "ADD_NORMAL_OBJ";
            case CMD_TYPE_ADD_USER_MESSAGE: return "ADD_USER_MESSAGE";
            case CMD_TYPE_DELETE_EMERGENCY_OBJ: return "DELETE_EMERGENCY_OBJ";
            case CMD_TYPE_DELETE_NORMAL_OBJ: return "DELETE_NORMAL_OBJ";
            case CMD_TYPE_DELETE_USER_MESSAGE: return "DELETE_USER_MESSAGE";
            case CMD_TYPE_SET_TIME: return "SET_TIME";
            case CMD_TYPE_REQUEST_MOVEMENT_HISTORY: return "REQUEST_MOVEMENT_HISTORY";
            case CMD_TYPE_SET_CLOCK_STYLE: return "SET_CLOCK_STYLE";
            case CMD_TYPE_SET_INDICATOR: return "SET_INDICATOR";
            case CMD_TYPE_PING: return "PING";
            case CMD_TYPE_AWAKE: return "AWAKE";
            case CMD_TYPE_SLEEP: return "SLEEP";
            case CMD_TYPE_REBOOT: return "REBOOT";
            default: return "UNKNOWN(0x" + String.format("%02X", cmd) + ")";
        }
    }
}

