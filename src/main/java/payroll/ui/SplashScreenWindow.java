package payroll.ui;

import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

/**
 * Startup Splash Screen (pure Java Swing + Graphics2D).
 * Renders a custom vector university crest, typography, and progress animation.
 */
public class SplashScreenWindow extends JWindow {

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private String statusMessage = "Initializing Database Connection...";

    public SplashScreenWindow() {
        setSize(540, 350);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0)); // Transparent window background for rounded corners

        setLayout(new BorderLayout());

        // Custom canvas for painting logo, typography, and frame
        SplashPanel panel = new SplashPanel();
        panel.setLayout(null);

        // Position progress bar inside the panel
        progressBar.setBounds(50, 280, 440, 8);
        progressBar.setValue(0);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(0x1E, 0x3A, 0x5F));
        progressBar.setForeground(UITheme.GOLD_ACCENT);
        panel.add(progressBar);

        add(panel, BorderLayout.CENTER);
    }

    /**
     * Displays the splash screen for 2.5 seconds with smooth progress, then runs onFinish.
     */
    public static void showSplash(Runnable onFinish) {
        SplashScreenWindow splash = new SplashScreenWindow();
        splash.setVisible(true);

        // Run 2500ms animation across 100 steps (25ms per step)
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i++) {
                    final int step = i;
                    SwingUtilities.invokeLater(() -> {
                        splash.progressBar.setValue(step);
                        if (step < 25) {
                            splash.statusMessage = "Initializing Database Connection...";
                        } else if (step < 50) {
                            splash.statusMessage = "Loading Enterprise Security Credentials...";
                        } else if (step < 80) {
                            splash.statusMessage = "Configuring Multithreaded Payroll Engine...";
                        } else {
                            splash.statusMessage = "Launching Application Dashboard...";
                        }
                        splash.repaint();
                    });
                    Thread.sleep(25);
                }
                // Brief pause at 100%
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            SwingUtilities.invokeLater(() -> {
                splash.dispose();
                if (onFinish != null) {
                    onFinish.run();
                }
            });
        }, "splash-worker").start();
    }

    private class SplashPanel extends javax.swing.JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background card with rounded corners & gradient
            RoundRectangle2D.Float card = new RoundRectangle2D.Float(0, 0, w, h, 20, 20);
            GradientPaint gradient = new GradientPaint(
                    0, 0, UITheme.NAVY_PRIMARY,
                    0, h, UITheme.NAVY_DARK
            );
            g2.setPaint(gradient);
            g2.fill(card);

            // Subtle Gold Border
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 20, 20));

            // Draw Vector University Emblem (Centered at x=270, y=90)
            drawUniversityCrest(g2, w / 2, 90);

            // Typography
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            FontMetrics fmTitle = g2.getFontMetrics();
            String title1 = "SRM UNIVERSITY AP";
            g2.drawString(title1, (w - fmTitle.stringWidth(title1)) / 2, 175);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fmSub = g2.getFontMetrics();
            String title2 = "Employee Payroll Management System";
            g2.drawString(title2, (w - fmSub.stringWidth(title2)) / 2, 202);

            g2.setColor(new Color(0x94, 0xA3, 0xB8)); // muted slate
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics fmVer = g2.getFontMetrics();
            String version = "Enterprise Edition • Version 2.4.0";
            g2.drawString(version, (w - fmVer.stringWidth(version)) / 2, 222);

            // Status message above progress bar
            g2.setColor(new Color(0xE2, 0xE8, 0xF0));
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.drawString(statusMessage, 50, 270);

            g2.dispose();
        }

        private void drawUniversityCrest(Graphics2D g2, int cx, int cy) {
            // Outer Gold Roundel
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval(cx - 38, cy - 38, 76, 76);

            // Inner Ring
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval(cx - 33, cy - 33, 66, 66);

            // Heraldic Shield Shape
            GeneralPath shield = new GeneralPath();
            shield.moveTo(cx - 22, cy - 18);
            shield.lineTo(cx + 22, cy - 18);
            shield.lineTo(cx + 22, cy + 6);
            shield.quadTo(cx + 18, cy + 24, cx, cy + 30);
            shield.quadTo(cx - 18, cy + 24, cx - 22, cy + 6);
            shield.closePath();

            // Fill Shield
            g2.setColor(new Color(0x16, 0x37, 0x61));
            g2.fill(shield);
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(shield);

            // Inside the shield: Open Book / Torch Symbol
            // Left page
            GeneralPath leftPage = new GeneralPath();
            leftPage.moveTo(cx, cy + 10);
            leftPage.quadTo(cx - 8, cy + 6, cx - 14, cy + 4);
            leftPage.lineTo(cx - 14, cy - 8);
            leftPage.quadTo(cx - 8, cy - 6, cx, cy - 2);
            leftPage.closePath();

            // Right page
            GeneralPath rightPage = new GeneralPath();
            rightPage.moveTo(cx, cy + 10);
            rightPage.quadTo(cx + 8, cy + 6, cx + 14, cy + 4);
            rightPage.lineTo(cx + 14, cy - 8);
            rightPage.quadTo(cx + 8, cy - 6, cx, cy - 2);
            rightPage.closePath();

            g2.setColor(Color.WHITE);
            g2.fill(leftPage);
            g2.fill(rightPage);

            // Torch / Star at top of shield
            g2.setColor(UITheme.GOLD_ACCENT);
            g2.fillOval(cx - 3, cy - 14, 6, 6);

            // Academic Laurels / Star Accents
            g2.fillOval(cx - 28, cy - 2, 4, 4);
            g2.fillOval(cx + 24, cy - 2, 4, 4);
            g2.fillOval(cx, cy - 30, 4, 4);
        }
    }
}
