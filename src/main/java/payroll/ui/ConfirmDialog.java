package payroll.ui;

import java.awt.*;
import java.awt.event.*;

/** Custom modal confirm dialog (pure AWT, replaces JOptionPane). */
public class ConfirmDialog extends Dialog {

    private boolean confirmed = false;

    public ConfirmDialog(Frame owner, String title, String message) {
        super(owner != null ? owner : new Frame(), title, true);
        setLayout(new BorderLayout(10, 10));
        setSize(420, 200);
        setLocationRelativeTo(owner);
        setBackground(SystemColor.control);

        TextArea txtMessage = new TextArea(message, 4, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);
        txtMessage.setEditable(false);
        txtMessage.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtMessage.setBackground(SystemColor.control);
        add(txtMessage, BorderLayout.CENTER);

        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        Button btnYes = new Button("   Yes   ");
        Button btnNo  = new Button("   No    ");
        btnYes.addActionListener(e -> { confirmed = true; dispose(); });
        btnNo.addActionListener(e -> { confirmed = false; dispose(); });
        buttonPanel.add(btnYes);
        buttonPanel.add(btnNo);
        add(buttonPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmed = false; dispose(); }
        });
    }

    public static boolean ask(Frame owner, String title, String message) {
        ConfirmDialog dialog = new ConfirmDialog(owner, title, message);
        dialog.setVisible(true);
        return dialog.confirmed;
    }
}
