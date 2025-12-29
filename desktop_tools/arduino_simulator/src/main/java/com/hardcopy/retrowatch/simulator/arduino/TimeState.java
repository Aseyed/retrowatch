package com.hardcopy.retrowatch.simulator.arduino;

/**
 * Time state matching Arduino implementation
 */
public class TimeState {
    public byte month = 1;
    public byte day = 1;
    public byte week = 1;    // 1: SUN, MON, TUE, WED, THU, FRI, SAT
    public byte amPm = 0;    // 0:AM, 1:PM
    public byte hour = 0;
    public byte minutes = 0;
    public byte second = 0;
    
    private static final String[] WEEK_STRINGS = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] AMPM_STRINGS = {"AM", "PM"};
    
    public void setTime(byte month, byte day, byte week, byte amPm, byte hour, byte minutes) {
        this.month = month;
        this.day = day;
        this.week = week;
        this.amPm = amPm;
        this.hour = hour;
        this.minutes = minutes;
        this.second = 0;
    }
    
    public void setTime(byte[] timeData) {
        if (timeData != null && timeData.length >= 6) {
            setTime(timeData[0], timeData[1], timeData[2], timeData[3], timeData[4], timeData[5]);
        }
    }
    
    /**
     * Update time (increment minutes every 60 seconds)
     * Matches Arduino updateTime() logic
     */
    public void updateTime(long currentTimeMillis, long lastUpdateTime) {
        long elapsed = currentTimeMillis - lastUpdateTime;
        if (elapsed >= 60000) { // UPDATE_TIME_INTERVAL = 60000
            minutes++;
            if (minutes >= 60) {
                minutes = 0;
                hour++;
                if (hour > 12) {
                    hour = 1;
                    amPm = (byte)(amPm == 0 ? 1 : 0);
                    if (amPm == 0) {
                        week++;
                        if (week > 7) {
                            week = 1;
                        }
                        day++;
                        if (day > 30) { // Yes, day is not exact (Arduino comment)
                            day = 1;
                        }
                    }
                }
            }
        }
    }
    
    public String getWeekString() {
        if (week >= 1 && week < WEEK_STRINGS.length) {
            return WEEK_STRINGS[week];
        }
        return "";
    }
    
    public String getAmPmString() {
        if (amPm >= 0 && amPm < AMPM_STRINGS.length) {
            return AMPM_STRINGS[amPm];
        }
        return "AM";
    }
    
    public String getTimeString() {
        return String.format("%02d:%02d", hour, minutes);
    }
    
    public String getDateString() {
        return String.format("%02d/%02d", month, day);
    }
}

