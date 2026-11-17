package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** KPI tile used on the dashboard. */
public class StatCard extends JPanel {

    private final JLabel valueLabel;

    public StatCard(String caption, String value, String hint, Color accent, String glyph) {
        setLayout(new BorderLayout(0, 4));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel cap = new JLabel(caption);
        cap.setFont(UITheme.font(Font.BOLD, 11.5f));
        cap.setForeground(UITheme.MUTED);

        JLabel icon = new JLabel(glyph, SwingConstants.CENTER);
        icon.setFont(UITheme.font(Font.BOLD, 15f));
        icon.setForeground(accent);
        icon.setOpaque(true);
        icon.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
        icon.setPreferredSize(new Dimension(30, 30));

        top.add(cap, BorderLayout.WEST);
        top.add(icon, BorderLayout.EAST);

        valueLabel = new JLabel(value);
        valueLabel.setFont(UITheme.font(Font.BOLD, 25f));
        valueLabel.setForeground(UITheme.TEXT);

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(UITheme.tiny());
        hintLabel.setForeground(UITheme.MUTED);

        JPanel center = new JPanel(new BorderLayout(0, 2));
        center.setOpaque(false);
        center.add(valueLabel, BorderLayout.NORTH);
        center.add(hintLabel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    public void setValue(String v) { valueLabel.setText(v); }
}
