package view;

import controller.DashboardController;
import model.DashboardStats;
import model.Student;
import utils.DateUtil;
import utils.MoneyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

/** Dashboard: KPI cards, recent registrations and two distribution charts. */
public class DashboardPanel extends JPanel implements Refreshable {

    private final DashboardController controller = new DashboardController();

    private final StatCard studentsCard =
            new StatCard("TOTAL STUDENTS", "0", "registered students", UITheme.PRIMARY, "\u263A");
    private final StatCard coursesCard =
            new StatCard("COURSES", "0", "active programmes", UITheme.ACCENT_PURPLE, "\u2261");
    private final StatCard deptCard =
            new StatCard("DEPARTMENTS", "0", "academic departments", UITheme.ACCENT_TEAL, "\u25A4");
    private final StatCard enrollCard =
            new StatCard("ENROLLMENTS", "0", "student-course links", UITheme.WARN, "\u2713");

    private final JLabel collectedLabel = new JLabel("\u20B9 0");
    private final JLabel pendingLabel = new JLabel("\u20B9 0");
    private final JLabel attendanceLabel = new JLabel("0%");
    private final JLabel genderLabel = new JLabel("-");

    private final DefaultTableModel recentModel;
    private final BarChartPanel deptChart = new BarChartPanel(java.util.List.of());
    private final BarChartPanel courseChart = new BarChartPanel(java.util.List.of());

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        recentModel = new DefaultTableModel(
                new Object[]{"Student ID", "Name", "Course", "Year", "Semester", "Registered"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        add(buildScroll(), BorderLayout.CENTER);
    }

    private JScrollPane buildScroll() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(0, 0, 0, 6));

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(studentsCard);
        cards.add(coursesCard);
        cards.add(deptCard);
        cards.add(enrollCard);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        cards.setPreferredSize(new Dimension(900, 96));

        wrap.add(cards);
        wrap.add(Box.createVerticalStrut(16));
        wrap.add(buildFinanceRow());
        wrap.add(Box.createVerticalStrut(16));
        wrap.add(buildChartsRow());
        wrap.add(Box.createVerticalStrut(16));
        wrap.add(buildRecentCard());

        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(null);
        sp.getViewport().setBackground(UITheme.BG);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        return sp;
    }

    private JPanel buildFinanceRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        row.setPreferredSize(new Dimension(900, 92));

        row.add(smallStat("FEE COLLECTED", collectedLabel, UITheme.SUCCESS));
        row.add(smallStat("FEE PENDING", pendingLabel, UITheme.DANGER));
        row.add(smallStat("AVERAGE ATTENDANCE  \u2022  " + genderLabel, attendanceLabel,
                UITheme.PRIMARY_DARK));
        return row;
    }

    private JPanel smallStat(String caption, JLabel value, Color color) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(14, 18, 14, 18)));

        JLabel cap = new JLabel(caption);
        cap.setFont(UITheme.font(Font.BOLD, 11f));
        cap.setForeground(UITheme.MUTED);

        value.setFont(UITheme.font(Font.BOLD, 20f));
        value.setForeground(color);

        p.add(cap, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        row.setPreferredSize(new Dimension(900, 250));
        row.add(chartCard("Students per department", deptChart, UITheme.PRIMARY));
        row.add(chartCard("Students per course", courseChart, UITheme.ACCENT_PURPLE));
        return row;
    }

    private JPanel chartCard(String title, BarChartPanel chart, Color color) {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout(0, 10));

        JLabel t = new JLabel(title);
        t.setFont(UITheme.h3());
        t.setForeground(UITheme.TEXT);

        chart.setBarColor(color);
        card.add(t, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecentCard() {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        card.setPreferredSize(new Dimension(900, 300));

        JLabel t = new JLabel("Recent student registrations");
        t.setFont(UITheme.h3());
        t.setForeground(UITheme.TEXT);

        JTable table = new JTable(recentModel);
        UITheme.styleTable(table);
        table.setAutoCreateRowSorter(false);

        card.add(t, BorderLayout.NORTH);
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        try {
            DashboardStats st = controller.load();
            studentsCard.setValue(String.valueOf(st.getTotalStudents()));
            coursesCard.setValue(String.valueOf(st.getTotalCourses()));
            deptCard.setValue(String.valueOf(st.getTotalDepartments()));
            enrollCard.setValue(String.valueOf(st.getTotalEnrollments()));
            collectedLabel.setText(MoneyUtil.format(st.getFeeCollected()));
            pendingLabel.setText(MoneyUtil.format(st.getFeePending()));
            attendanceLabel.setText(String.format("%.2f%%", st.getAverageAttendance()));
            genderLabel.setText("M " + st.getMaleStudents() + " / F " + st.getFemaleStudents());

            recentModel.setRowCount(0);
            for (Student s : st.getRecentStudents()) {
                recentModel.addRow(new Object[]{
                    s.getStudentCode(), s.getFullName(), s.getCourseName(),
                    s.getAcademicYear(), s.getSemester(),
                    DateUtil.format(s.getCreatedAt())
                });
            }

            deptChart.setData(st.getDepartmentWise());
            courseChart.setData(st.getCourseWise());
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load dashboard data.\n\n" + ex.getMessage());
        }
    }
}
