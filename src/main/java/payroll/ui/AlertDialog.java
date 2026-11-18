package payroll.ui;

import java.awt.*;
import java.awt.event.*;

/** Custom modal alert dialog (pure AWT, replaces JOptionPane). */
public class AlertDialog extends Dialog {

    public AlertDialog(Frame owner, String title, String message) {
        super(owner != null ? owner : new Frame(), title, true);
        setLayout(new BorderLayout(10, 10));
        setSize(420, 200);
        setLocationRelativeTo(owner);
        setBackground(SystemColor.control);

        TextArea txtMessage = new TextArea(message, 4, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);
        txtMessage.setEditable(false);
        txtMessage.setBackground(SystemColor.control);
        add(txtMessage, BorderLayout.CENTER);

        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
        Button btnOk = new Button("   OK   ");
        btnOk.addActionListener(e -> dispose());
        buttonPanel.add(btnOk);
        add(buttonPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
    }

    public static void show(Frame owner, String title, String message) {
        if (EventQueue.isDispatchThread()) new AlertDialog(owner, title, message).setVisible(true);
        else EventQueue.invokeLater(() -> new AlertDialog(owner, title, message).setVisible(true));
    }
}
