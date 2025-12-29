package com.hardcopy.retrowatch.simulator.arduino;

import com.hardcopy.retrowatch.simulator.protocol.CommandTypes;
import com.hardcopy.retrowatch.simulator.protocol.LegacyProtocolParser;

import java.util.function.Consumer;

/**
 * Arduino logic simulator - matches Arduino behavior exactly
 */
public class ArduinoSimulator {
    
    // Message buffers
    private final MessageBuffer msgBuffer;      // Normal messages
    private final MessageBuffer emgBuffer;      // Emergency messages
    
    // Time state
    private final TimeState timeState;
    private long lastClockUpdate = 0;
    
    // Display state
    private DisplayMode displayMode = DisplayMode.START_UP;
    private long prevDisplayTime = 0;
    private long nextDisplayInterval = 0;
    private long modeChangeTimer = 0;
    
    // Clock settings
    private byte clockStyle = CommandTypes.CLOCK_STYLE_SIMPLE_MIX;
    private boolean updateIndicator = true;
    
    // Timing constants (matching Arduino)
    private static final long UPDATE_TIME_INTERVAL = 60000;      // 60 seconds
    private static final long CLOCK_DISP_INTERVAL = 60000;       // 60 seconds
    private static final long EMERGENCY_DISP_INTERVAL = 5000;    // 5 seconds
    private static final long MESSAGE_DISP_INTERVAL = 3000;      // 3 seconds
    private static final long CLOCK_DISPLAY_TIME = 300000;       // 5 minutes
    private static final long EMER_DISPLAY_TIME = 10000;         // 10 seconds
    private static final long MSG_DISPLAY_TIME = 5000;           // 5 seconds
    private static final long IDLE_DISP_INTERVAL = 60000;        // 60 seconds
    
    // Callbacks
    private Consumer<DisplayUpdate> displayUpdateCallback;
    
    public ArduinoSimulator() {
        msgBuffer = new MessageBuffer(CommandTypes.MSG_COUNT_MAX, CommandTypes.MSG_BUFFER_MAX);
        emgBuffer = new MessageBuffer(CommandTypes.EMG_COUNT_MAX, CommandTypes.EMG_BUFFER_MAX);
        timeState = new TimeState();
        lastClockUpdate = System.currentTimeMillis();
        prevDisplayTime = System.currentTimeMillis();
    }
    
    public void setDisplayUpdateCallback(Consumer<DisplayUpdate> callback) {
        this.displayUpdateCallback = callback;
    }
    
    /**
     * Process command from protocol parser
     */
    public void processCommand(LegacyProtocolParser.CommandData cmd) {
        switch (cmd.command) {
            case CommandTypes.CMD_TYPE_RESET_EMERGENCY_OBJ:
                emgBuffer.init();
                break;
                
            case CommandTypes.CMD_TYPE_RESET_NORMAL_OBJ:
                msgBuffer.init();
                break;
                
            case CommandTypes.CMD_TYPE_RESET_USER_MESSAGE:
                // Not available yet
                break;
                
            case CommandTypes.CMD_TYPE_ADD_NORMAL_OBJ:
                if (cmd.messageData != null && cmd.messageData.length > 0) {
                    // Reconstruct full buffer: [0x01][0x00][0xF0][ID][Icon][Text...][0x00]
                    byte[] fullData = new byte[CommandTypes.MSG_BUFFER_MAX];
                    fullData[0] = 0x01; // Enabled
                    fullData[1] = 0x00; // Reserved
                    fullData[2] = (byte)0xF0; // Management byte
                    fullData[3] = cmd.id;
                    fullData[4] = cmd.iconType;
                    int textLength = Math.min(cmd.messageData.length, CommandTypes.MSG_BUFFER_MAX - 5);
                    System.arraycopy(cmd.messageData, 0, fullData, 5, textLength);
                    fullData[CommandTypes.MSG_BUFFER_MAX - 1] = 0x00; // Null terminator
                    
                    msgBuffer.addMessage(fullData, CommandTypes.MSG_BUFFER_MAX);
                    setNextDisplayTime(System.currentTimeMillis(), 0); // Update immediately
                }
                break;
                
            case CommandTypes.CMD_TYPE_ADD_EMERGENCY_OBJ:
                if (cmd.messageData != null && cmd.messageData.length > 0) {
                    // Same format as normal messages
                    byte[] fullData = new byte[CommandTypes.EMG_BUFFER_MAX];
                    fullData[0] = 0x01; // Enabled
                    fullData[1] = 0x00; // Reserved
                    fullData[2] = (byte)0xF0; // Management byte
                    fullData[3] = cmd.id;
                    fullData[4] = cmd.iconType;
                    int textLength = Math.min(cmd.messageData.length, CommandTypes.EMG_BUFFER_MAX - 5);
                    System.arraycopy(cmd.messageData, 0, fullData, 5, textLength);
                    fullData[CommandTypes.EMG_BUFFER_MAX - 1] = 0x00; // Null terminator
                    
                    emgBuffer.addMessage(fullData, CommandTypes.EMG_BUFFER_MAX);
                    startEmergencyMode();
                    setNextDisplayTime(System.currentTimeMillis(), 2000);
                }
                break;
                
            case CommandTypes.CMD_TYPE_SET_TIME:
                if (cmd.timeData != null) {
                    timeState.setTime(cmd.timeData);
                    setNextDisplayTime(System.currentTimeMillis(), 0); // Update immediately
                }
                break;
                
            case CommandTypes.CMD_TYPE_SET_CLOCK_STYLE:
                clockStyle = cmd.id;
                setNextDisplayTime(System.currentTimeMillis(), 0);
                break;
                
            case CommandTypes.CMD_TYPE_SET_INDICATOR:
                updateIndicator = (cmd.id == CommandTypes.INDICATOR_ENABLE);
                setNextDisplayTime(System.currentTimeMillis(), 0);
                break;
                
            case CommandTypes.CMD_TYPE_PING:
            case CommandTypes.CMD_TYPE_AWAKE:
            case CommandTypes.CMD_TYPE_SLEEP:
            case CommandTypes.CMD_TYPE_REBOOT:
                // No-op for now
                break;
        }
    }
    
    /**
     * Update simulator state (call periodically, e.g., every 100ms)
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        
        // Update clock time
        timeState.updateTime(currentTime, lastClockUpdate);
        if (currentTime - lastClockUpdate >= UPDATE_TIME_INTERVAL) {
            lastClockUpdate = currentTime;
        }
        
        // Update display
        onDraw(currentTime);
    }
    
    private void onDraw(long currentTime) {
        if (!isDisplayTime(currentTime)) {
            return;
        }
        
        DisplayUpdate update = new DisplayUpdate();
        update.mode = displayMode;
        update.timeState = timeState;
        update.clockStyle = clockStyle;
        update.updateIndicator = updateIndicator;
        update.msgCount = msgBuffer.countMessages();
        update.emgCount = emgBuffer.countMessages();
        
        switch (displayMode) {
            case START_UP:
                // Show startup screen for 2 seconds, then go to clock
                if (currentTime - prevDisplayTime > 2000) {
                    startClockMode();
                    setNextDisplayTime(currentTime, 0);
                } else {
                    update.showStartup = true;
                    notifyDisplayUpdate(update);
                }
                break;
                
            case CLOCK:
                update.showClock = true;
                notifyDisplayUpdate(update);
                
                if (isPageChangeTime(currentTime)) {
                    startIdleMode();
                    setPageChangeTime(currentTime);
                }
                setNextDisplayTime(currentTime, CLOCK_DISP_INTERVAL);
                break;
                
            case EMERGENCY_MSG:
                if (emgBuffer.findNextMessage()) {
                    update.showEmergency = true;
                    update.messageId = emgBuffer.getCurrentId();
                    update.messageIcon = emgBuffer.getCurrentIcon();
                    update.messageText = emgBuffer.getCurrentText();
                    notifyDisplayUpdate(update);
                    
                    emgBuffer.advanceDisplay();
                    if (emgBuffer.getCurrentMessage() == null || 
                        emgBuffer.getCurrentMessage()[0] == 0x00) {
                        emgBuffer.resetDisplay();
                        startMessageMode();
                    }
                    setNextDisplayTime(currentTime, EMERGENCY_DISP_INTERVAL);
                } else {
                    startMessageMode();
                    setNextDisplayTime(currentTime, 0);
                }
                break;
                
            case NORMAL_MSG:
                if (msgBuffer.findNextMessage()) {
                    update.showMessage = true;
                    update.messageId = msgBuffer.getCurrentId();
                    update.messageIcon = msgBuffer.getCurrentIcon();
                    update.messageText = msgBuffer.getCurrentText();
                    notifyDisplayUpdate(update);
                    
                    msgBuffer.advanceDisplay();
                    if (msgBuffer.getCurrentMessage() == null || 
                        msgBuffer.getCurrentMessage()[0] == 0x00) {
                        msgBuffer.resetDisplay();
                        startClockMode();
                    }
                    setNextDisplayTime(currentTime, MESSAGE_DISP_INTERVAL);
                } else {
                    startClockMode();
                    setPageChangeTime(currentTime);
                    setNextDisplayTime(currentTime, 0);
                }
                break;
                
            case IDLE:
                update.showIdleClock = true;
                notifyDisplayUpdate(update);
                setNextDisplayTime(currentTime, IDLE_DISP_INTERVAL);
                break;
        }
    }
    
    private boolean isDisplayTime(long currentTime) {
        if (currentTime - prevDisplayTime > nextDisplayInterval) {
            return true;
        }
        return false;
    }
    
    private void setNextDisplayTime(long currentTime, long nextUpdateTime) {
        nextDisplayInterval = nextUpdateTime;
        prevDisplayTime = currentTime;
    }
    
    private boolean isPageChangeTime(long currentTime) {
        if (displayMode == DisplayMode.CLOCK) {
            if (currentTime - modeChangeTimer > CLOCK_DISPLAY_TIME) {
                return true;
            }
        }
        return false;
    }
    
    private void setPageChangeTime(long currentTime) {
        modeChangeTimer = currentTime;
    }
    
    private void startClockMode() {
        displayMode = DisplayMode.CLOCK;
    }
    
    private void startEmergencyMode() {
        displayMode = DisplayMode.EMERGENCY_MSG;
        emgBuffer.resetDisplay();
    }
    
    private void startMessageMode() {
        displayMode = DisplayMode.NORMAL_MSG;
        msgBuffer.resetDisplay();
    }
    
    private void startIdleMode() {
        displayMode = DisplayMode.IDLE;
    }
    
    private void notifyDisplayUpdate(DisplayUpdate update) {
        if (displayUpdateCallback != null) {
            displayUpdateCallback.accept(update);
        }
    }
    
    /**
     * Display update data structure
     */
    public static class DisplayUpdate {
        public DisplayMode mode;
        public TimeState timeState;
        public byte clockStyle;
        public boolean updateIndicator;
        public int msgCount;
        public int emgCount;
        
        // Display flags
        public boolean showStartup;
        public boolean showClock;
        public boolean showEmergency;
        public boolean showMessage;
        public boolean showIdleClock;
        
        // Message data
        public byte messageId;
        public byte messageIcon;
        public String messageText;
    }
}

