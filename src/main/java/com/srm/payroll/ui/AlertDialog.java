package com.srm.payroll.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Custom modal alert dialog implemented purely with Java AWT (replaces JOptionPane.showMessageDialog).
 */
public class AlertDialog extends Dialog {

    public AlertDialog(Frame owner, String title, String message) {
        super(owner != null ? owner : new Frame(), title, true);
        setLayout(new BorderLayout(10, 10));
        setSize(420, 200);
        setLocationRelativeTo(owner);
        setBackground(SystemColor.control);

        // Header/Icon/Message panel
        Panel messagePanel = new Panel(new BorderLayout(10, 10));
        messagePanel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        TextArea txtMessage = new TextArea(message, 4, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);
        txtMessage.setEditable(false);
        txtMessage.setBackground(SystemColor.control);
        messagePanel.add(txtMessage, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.CENTER);

        // Button panel
        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
        Button btnOk = new Button("   OK   ");
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(btnOk);
        add(buttonPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    public static void show(Frame owner, String title, String message) {
        if (EventQueue.isDispatchThread()) {
            new AlertDialog(owner, title, message).setVisible(true);
        } else {
            EventQueue.invokeLater(() -> new AlertDialog(owner, title, message).setVisible(true));
        }
    }
}
