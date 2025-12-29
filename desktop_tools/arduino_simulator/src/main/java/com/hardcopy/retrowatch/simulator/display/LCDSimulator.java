package com.hardcopy.retrowatch.simulator.display;

import com.hardcopy.retrowatch.simulator.arduino.ArduinoSimulator.DisplayUpdate;
import com.hardcopy.retrowatch.simulator.arduino.TimeState;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * LCD Display Simulator - 128x64 OLED display
 * Matches Adafruit SSD1306 behavior
 */
public class LCDSimulator {
    
    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;
    
    private BufferedImage displayBuffer;
    private Graphics2D graphics;
    
    private int centerX = WIDTH / 2;
    private int centerY = HEIGHT / 2;
    private int radius = centerY - 2;
    
    public LCDSimulator() {
        displayBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        graphics = displayBuffer.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        clearDisplay();
    }
    
    public void clearDisplay() {
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
    }
    
    public BufferedImage render(DisplayUpdate update) {
        clearDisplay();
        
        if (update.showStartup) {
            drawStartup();
        } else if (update.showClock) {
            drawClock(update);
        } else if (update.showEmergency) {
            drawEmergency(update);
        } else if (update.showMessage) {
            drawMessage(update);
        } else if (update.showIdleClock) {
            drawIdleClock(update);
        }
        
        return displayBuffer;
    }
    
    private void drawStartup() {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        
        // Draw RetroWatch logo (simplified)
        graphics.drawString("Retro", 45, 28);
        graphics.drawString("Watch", 45, 44);
        
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        graphics.drawString("Arduino v1.0", 45, 55);
    }
    
    private void drawClock(DisplayUpdate update) {
        if (update.updateIndicator) {
            drawIndicator(update);
        }
        
        TimeState time = update.timeState;
        
        switch (update.clockStyle) {
            case 0x01: // CLOCK_STYLE_SIMPLE_ANALOG
                drawAnalogClock(time);
                break;
            case 0x02: // CLOCK_STYLE_SIMPLE_DIGIT
                drawDigitalClock(time);
                break;
            case 0x03: // CLOCK_STYLE_SIMPLE_MIX
            default:
                drawMixedClock(time);
                break;
        }
    }
    
    private void drawAnalogClock(TimeState time) {
        graphics.setColor(Color.WHITE);
        graphics.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // Draw hour hand
        double hourAngle = Math.toRadians((time.hour * 30 + time.minutes * 0.5) - 90);
        int hourX = centerX + (int)(radius * 0.5 * Math.cos(hourAngle));
        int hourY = centerY + (int)(radius * 0.5 * Math.sin(hourAngle));
        graphics.drawLine(centerX, centerY, hourX, hourY);
        
        // Draw minute hand
        double minuteAngle = Math.toRadians(time.minutes * 6 - 90);
        int minuteX = centerX + (int)(radius * 0.78 * Math.cos(minuteAngle));
        int minuteY = centerY + (int)(radius * 0.78 * Math.sin(minuteAngle));
        graphics.drawLine(centerX, centerY, minuteX, minuteY);
    }
    
    private void drawDigitalClock(TimeState time) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        
        // Week and AM/PM
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        graphics.drawString(time.getWeekString(), centerX - 34, centerY - 17);
        graphics.drawString(time.getAmPmString(), centerX + 11, centerY - 17);
        
        // Time
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        graphics.drawString(time.getTimeString(), centerX - 29, centerY + 6);
    }
    
    private void drawMixedClock(TimeState time) {
        graphics.setColor(Color.WHITE);
        
        // Analog clock on left
        int leftCenterX = centerY;
        int leftCenterY = centerY;
        int leftRadius = radius - 6;
        graphics.drawOval(leftCenterX - leftRadius, leftCenterY - leftRadius, 
                         leftRadius * 2, leftRadius * 2);
        
        // Hour hand
        double hourAngle = Math.toRadians((time.hour * 5 + (int)(time.minutes * 5.0 / 60)) * 6 - 90);
        int hourX = leftCenterX + (int)(leftRadius * 0.4 * Math.cos(hourAngle));
        int hourY = leftCenterY + (int)(leftRadius * 0.4 * Math.sin(hourAngle));
        graphics.drawLine(leftCenterX, leftCenterY, hourX, hourY);
        
        // Minute hand
        double minuteAngle = Math.toRadians(time.minutes * 6 - 90);
        int minuteX = leftCenterX + (int)(leftRadius * 0.70 * Math.cos(minuteAngle));
        int minuteY = leftCenterY + (int)(leftRadius * 0.70 * Math.sin(minuteAngle));
        graphics.drawLine(leftCenterX, leftCenterY, minuteX, minuteY);
        
        // Digital info on right
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        graphics.drawString(time.getWeekString(), leftCenterX * 2 + 3, 23);
        graphics.drawString(time.getAmPmString(), leftCenterX * 2 + 28, 23);
        
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        graphics.drawString(time.getTimeString(), leftCenterX * 2, 37);
    }
    
    private void drawEmergency(DisplayUpdate update) {
        if (update.updateIndicator) {
            drawIndicator(update);
        }
        
        graphics.setColor(Color.WHITE);
        
        // Draw icon (simplified - just a box for now)
        int iconX = centerX - 8;
        int iconY = centerY - 20;
        graphics.fillRect(iconX, iconY, 16, 16);
        
        // Draw text
        if (update.messageText != null && !update.messageText.isEmpty()) {
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            int textX = getCenterAlignedX(update.messageText);
            graphics.drawString(update.messageText, textX, centerY + 10);
        }
    }
    
    private void drawMessage(DisplayUpdate update) {
        if (update.updateIndicator) {
            drawIndicator(update);
        }
        
        graphics.setColor(Color.WHITE);
        
        // Draw icon (simplified - just a box for now)
        int iconX = centerX - 8;
        int iconY = centerY - 20;
        graphics.drawRect(iconX, iconY, 16, 16);
        
        // Draw text
        if (update.messageText != null && !update.messageText.isEmpty()) {
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            int textX = getCenterAlignedX(update.messageText);
            graphics.drawString(update.messageText, textX, centerY + 10);
        }
    }
    
    private void drawIdleClock(DisplayUpdate update) {
        if (update.updateIndicator) {
            drawIndicator(update);
        }
        
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        graphics.drawString(update.timeState.getTimeString(), centerX - 29, centerY - 4);
    }
    
    private void drawIndicator(DisplayUpdate update) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
        
        int x = WIDTH - 8;
        int y = 1;
        
        if (update.msgCount > 0) {
            // Draw message indicator
            graphics.fillRect(x - 8, y, 8, 8);
            graphics.setColor(Color.BLACK);
            graphics.drawString(String.valueOf(update.msgCount), x - 15, y + 7);
            graphics.setColor(Color.WHITE);
            x -= 15;
        }
        
        if (update.emgCount > 0) {
            // Draw emergency indicator
            graphics.fillOval(x - 8, y, 8, 8);
            graphics.setColor(Color.BLACK);
            graphics.drawString(String.valueOf(update.emgCount), x - 15, y + 7);
        }
    }
    
    private int getCenterAlignedX(String text) {
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = centerX - textWidth / 2;
        return Math.max(0, x);
    }
    
    public BufferedImage getDisplayBuffer() {
        return displayBuffer;
    }
}

