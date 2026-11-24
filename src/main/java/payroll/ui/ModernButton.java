package payroll.ui;

import javax.swing.JButton;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom modern rounded button built with pure Java Swing and Graphics2D.
 * Features rounded corners, smooth hover highlights, hand cursor, and color categorization.
 */
public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY(UITheme.BTN_PRIMARY, Color.WHITE),
        POSITIVE(UITheme.BTN_POSITIVE, Color.WHITE),
        DESTRUCTIVE(UITheme.BTN_DESTRUCTIVE, Color.WHITE),
        UTILITY(UITheme.BTN_UTILITY, UITheme.BTN_UTILITY_TEXT);

        final Color baseColor;
        final Color textColor;

        ButtonStyle(Color baseColor, Color textColor) {
            this.baseColor = baseColor;
            this.textColor = textColor;
        }
    }

    private final ButtonStyle style;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 9;

    public ModernButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        init();
    }

    private void init() {
        setFont(UITheme.FONT_BUTTON);
        setForeground(style.textColor);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(7, 14, 7, 14));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        Color bgColor;
        if (!isEnabled()) {
            bgColor = new Color(0xCB, 0xD5, 0xE1); // muted slate
        } else if (isPressed) {
            bgColor = darken(style.baseColor, 0.15f);
        } else if (isHovered) {
            bgColor = lighten(style.baseColor, 0.12f);
        } else {
            bgColor = style.baseColor;
        }

        // Draw soft rounded background
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Subtle border for utility buttons
        if (style == ButtonStyle.UTILITY && isEnabled()) {
            g2.setColor(new Color(0xCB, 0xD5, 0xE1));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, cornerRadius, cornerRadius));
        }

        // Draw text
        Color txtColor = isEnabled() ? style.textColor : new Color(0x64, 0x74, 0x8B);
        g2.setColor(txtColor);
        g2.setFont(getFont());

        FontMetrics fm = g2.getFontMetrics();
        int stringWidth = fm.stringWidth(getText());
        int stringHeight = fm.getAscent();
        int x = (width - stringWidth) / 2;
        int y = (height + stringHeight) / 2 - 2;

        g2.drawString(getText(), x, y);
        g2.dispose();
    }

    private Color lighten(Color c, float amount) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * amount));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * amount));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * amount));
        return new Color(r, g, b, c.getAlpha());
    }

    private Color darken(Color c, float amount) {
        int r = Math.max(0, (int) (c.getRed() * (1 - amount)));
        int g = Math.max(0, (int) (c.getGreen() * (1 - amount)));
        int b = Math.max(0, (int) (c.getBlue() * (1 - amount)));
        return new Color(r, g, b, c.getAlpha());
    }
}
