package com.hardcopy.retrowatch.simulator.ui;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log panel for displaying messages and debug information
 */
public class LogPanel extends JPanel {
    
    private JTextArea logArea;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    public LogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Message Log"));
        
        logArea = new JTextArea(15, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
        });
    }
    
    public void log(String level, String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] [" + level + "] " + message + "\n");
        });
    }
    
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            logArea.setText("");
        });
    }
}

