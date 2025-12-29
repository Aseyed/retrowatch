package com.hardcopy.retrowatch.simulator.ui;

import com.hardcopy.retrowatch.simulator.display.LCDSimulator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Panel for displaying the simulated LCD screen
 */
public class LCDPanel extends JPanel {
    
    private LCDSimulator lcdSimulator;
    private BufferedImage currentDisplay;
    private static final int SCALE = 4; // Scale factor for display (128x64 -> 512x256)
    
    public LCDPanel() {
        lcdSimulator = new LCDSimulator();
        setPreferredSize(new Dimension(LCDSimulator.WIDTH * SCALE, LCDSimulator.HEIGHT * SCALE));
        setBackground(Color.BLACK);
        setBorder(BorderFactory.createTitledBorder("LCD Display (128x64)"));
    }
    
    public LCDSimulator getLCDSimulator() {
        return lcdSimulator;
    }
    
    public void updateDisplay(BufferedImage display) {
        this.currentDisplay = display;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        
        if (currentDisplay != null) {
            int x = (getWidth() - LCDSimulator.WIDTH * SCALE) / 2;
            int y = (getHeight() - LCDSimulator.HEIGHT * SCALE) / 2;
            g2d.drawImage(currentDisplay, x, y, LCDSimulator.WIDTH * SCALE, LCDSimulator.HEIGHT * SCALE, null);
        } else {
            // Show placeholder
            g2d.setColor(Color.GRAY);
            g2d.drawString("No display data", getWidth() / 2 - 50, getHeight() / 2);
        }
    }
}

