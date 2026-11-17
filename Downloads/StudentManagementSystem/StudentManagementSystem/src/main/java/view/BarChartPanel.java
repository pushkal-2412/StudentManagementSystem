package view;

import model.ChartRow;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Lightweight horizontal bar chart drawn with Java2D (no external library). */
public class BarChartPanel extends JPanel {

    private List<ChartRow> data;
    private Color barColor = UITheme.PRIMARY;

    public BarChartPanel(List<ChartRow> data) {
        this.data = data;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(320, 200));
    }

    public void setData(List<ChartRow> data) {
        this.data = data;
        repaint();
    }

    public void setBarColor(Color c) { this.barColor = c; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (data == null || data.isEmpty()) {
            g2.setColor(UITheme.MUTED);
            g2.setFont(UITheme.small());
            g2.drawString("No data available", 16, getHeight() / 2);
            g2.dispose();
            return;
        }

        double max = 1;
        for (ChartRow r : data) max = Math.max(max, r.getValue());

        int left = 78;
        int right = 44;
        int top = 8;
        int rowH = Math.max(20, (getHeight() - top * 2) / data.size());
        int barMax = Math.max(30, getWidth() - left - right);

        g2.setFont(UITheme.tiny());
        for (int i = 0; i < data.size(); i++) {
            ChartRow r = data.get(i);
            int y = top + i * rowH;

            g2.setColor(UITheme.MUTED);
            String label = r.getLabel();
            if (label.length() > 12) label = label.substring(0, 11) + "\u2026";
            g2.drawString(label, 4, y + rowH / 2 + 4);

            int w = (int) Math.round(r.getValue() / max * barMax);
            g2.setColor(new Color(UITheme.BORDER.getRed(), UITheme.BORDER.getGreen(),
                    UITheme.BORDER.getBlue(), 90));
            g2.fillRoundRect(left, y + 4, barMax, rowH - 10, 8, 8);

            g2.setColor(barColor);
            g2.fillRoundRect(left, y + 4, Math.max(w, 3), rowH - 10, 8, 8);

            g2.setColor(UITheme.TEXT);
            g2.setFont(UITheme.font(Font.BOLD, 11f));
            g2.drawString(String.valueOf((int) r.getValue()), left + barMax + 8,
                    y + rowH / 2 + 4);
            g2.setFont(UITheme.tiny());
        }
        g2.dispose();
    }
}
