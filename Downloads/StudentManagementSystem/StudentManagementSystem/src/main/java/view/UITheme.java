package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Central design system: colours, fonts, spacing and reusable styled components. */
public final class UITheme {

    public static final Color PRIMARY       = new Color(31, 111, 235);
    public static final Color PRIMARY_DARK  = new Color(23, 88, 190);
    public static final Color SIDEBAR       = new Color(15, 23, 42);
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);
    public static final Color BG            = new Color(244, 246, 250);
    public static final Color CARD          = Color.WHITE;
    public static final Color BORDER        = new Color(226, 232, 240);
    public static final Color TEXT          = new Color(15, 23, 42);
    public static final Color MUTED         = new Color(100, 116, 139);
    public static final Color SUCCESS       = new Color(22, 163, 74);
    public static final Color DANGER        = new Color(220, 38, 38);
    public static final Color WARN          = new Color(217, 119, 6);
    public static final Color ACCENT_PURPLE = new Color(124, 58, 237);
    public static final Color ACCENT_TEAL   = new Color(13, 148, 136);

    public static final String FONT_FAMILY = pickFont();

    private UITheme() { }

    private static String pickFont() {
        String[] preferred = {"Segoe UI", "Inter", "SF Pro Text", "Ubuntu", "DejaVu Sans",
                              "SansSerif"};
        java.util.List<String> available = java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames());
        for (String f : preferred) {
            if (available.contains(f)) return f;
        }
        return "SansSerif";
    }

    /** Applies FlatLaf (bundled) so the whole app looks modern on every OS. */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("Table.showHorizontalLines", false);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Panel.background", BG);
            UIManager.put("OptionPane.messageFont", body());
            UIManager.put("OptionPane.buttonFont", small());
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default Metal look and feel
            }
        }
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size));
    }

    public static Font h1() { return font(Font.BOLD, 24f); }
    public static Font h2() { return font(Font.BOLD, 17f); }
    public static Font h3() { return font(Font.BOLD, 14f); }
    public static Font body() { return font(Font.PLAIN, 13f); }
    public static Font small() { return font(Font.PLAIN, 12f); }
    public static Font tiny() { return font(Font.PLAIN, 11f); }

    /** Flat primary action button. */
    public static JButton primary(String text) {
        return styled(text, PRIMARY, Color.WHITE);
    }

    public static JButton success(String text) {
        return styled(text, SUCCESS, Color.WHITE);
    }

    public static JButton danger(String text) {
        return styled(text, DANGER, Color.WHITE);
    }

    public static JButton neutral(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 12.5f));
        b.setForeground(TEXT);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(7, 14, 7, 14)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton styled(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 12.5f));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        return b;
    }

    /** White rounded card with a soft border. */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        return p;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(h1());
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel subtitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(small());
        l.setForeground(MUTED);
        return l;
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(body());
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(6, 9, 6, 9)));
        return f;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField f = new JPasswordField(columns);
        f.setFont(body());
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(6, 9, 6, 9)));
        return f;
    }

    public static JComboBox<String> combo(String... values) {
        JComboBox<String> c = new JComboBox<>(values);
        c.setFont(body());
        c.setBackground(Color.WHITE);
        return c;
    }

    public static JSpinner dateSpinner(LocalDate initial) {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner sp = new JSpinner(model);
        JSpinner.DateEditor ed = new JSpinner.DateEditor(sp, "yyyy-MM-dd");
        ed.getTextField().setFont(body());
        sp.setEditor(ed);
        sp.setValue(toDate(initial == null ? LocalDate.now() : initial));
        return sp;
    }

    public static LocalDate spinnerDate(JSpinner spinner) {
        Object v = spinner.getValue();
        if (v instanceof Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.now();
    }

    public static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Consistent table look: row height, header styling, zebra rows. */
    public static void styleTable(JTable t) {
        t.setFont(body());
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);

        JTableHeader h = t.getTableHeader();
        h.setFont(font(Font.BOLD, 12f));
        h.setBackground(new Color(248, 250, 252));
        h.setForeground(MUTED);
        h.setPreferredSize(new Dimension(10, 34));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        h.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        table, value, selected, focus, row, col);
                if (!selected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 251, 253));
                    c.setForeground(TEXT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, renderer);
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static void pad(JComponent c, int top, int left, int bottom, int right) {
        c.setBorder(new EmptyBorder(top, left, bottom, right));
    }

    /** Small coloured pill used for statuses inside tables. */
    public static JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font(Font.BOLD, 11f));
        l.setForeground(Color.WHITE);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        return l;
    }

    public static Color statusColor(String status) {
        if (status == null) return MUTED;
        return switch (status.toUpperCase()) {
            case "PAID", "PRESENT", "ACTIVE", "PASS", "GOOD" -> SUCCESS;
            case "PENDING", "ABSENT", "SHORTAGE", "FAIL" -> DANGER;
            case "PARTIALLY_PAID", "LATE", "AVERAGE", "DROPPED" -> WARN;
            default -> MUTED;
        };
    }
}
