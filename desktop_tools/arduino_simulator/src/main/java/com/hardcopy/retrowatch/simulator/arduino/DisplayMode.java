package com.hardcopy.retrowatch.simulator.arduino;

/**
 * Display modes matching Arduino constants
 */
public enum DisplayMode {
    START_UP(0),
    CLOCK(1),
    EMERGENCY_MSG(2),
    NORMAL_MSG(3),
    IDLE(11);
    
    private final int value;
    
    DisplayMode(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static DisplayMode fromValue(int value) {
        for (DisplayMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return CLOCK; // Default
    }
}

