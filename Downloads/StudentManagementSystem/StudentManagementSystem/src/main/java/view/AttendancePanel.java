package view;

import controller.AttendanceController;
import controller.CourseController;
import model.Attendance;
import model.AttendanceSummary;
import model.Course;
import model.Student;
import model.TableData;
import utils.ExportUtil;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Attendance module: daily marking sheet + monthly percentage report. */
public class AttendancePanel extends JPanel implements Refreshable {

    private final AttendanceController controller = new AttendanceController();
    private final CourseController courseController = new CourseController();

    private final JComboBox<Course> markCourse = new JComboBox<>();
    private final JSpinner markDate = UITheme.dateSpinner(LocalDate.now());
    private final DefaultTableModel markModel;
    private final JTable markTable;
    private List<Student> markStudents = new ArrayList<>();

    private final JComboBox<Course> repCourse = new JComboBox<>();
    private final JComboBox<String> repMonth = UITheme.combo(
            "JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST",
            "SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER");
    private final JSpinner repYear = new JSpinner(new SpinnerNumberModel(
            LocalDate.now().getYear(), 2000, 2100, 1));
    private final DefaultTableModel repModel;
    private final JTable repTable;
    private List<AttendanceSummary> summary = new ArrayList<>();

    public AttendancePanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(14, 20, 14, 20));

        markModel = new DefaultTableModel(
                new Object[]{"Student ID", "Student Name", "Course", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        markTable = new JTable(markModel);
        UITheme.styleTable(markTable);
        JComboBox<String> statusEditor = new JComboBox<>(new String[]{"PRESENT", "ABSENT", "LATE"});
        markTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusEditor));

        repModel = new DefaultTableModel(AttendanceController.SUMMARY_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        repTable = new JTable(repModel);
        UITheme.styleTable(repTable);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.font(Font.BOLD, 12.5f));
        tabs.addTab("Mark attendance", buildMarkTab());
        tabs.addTab("Monthly report", buildReportTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildMarkTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(14, 4, 4, 4));

        JPanel bar = UITheme.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton load = UITheme.primary("Load students");
        load.addActionListener(e -> loadMarkSheet());
        JButton allPresent = UITheme.success("Mark all present");
        allPresent.addActionListener(e -> setAll("PRESENT"));
        JButton allAbsent = UITheme.neutral("Mark all absent");
        allAbsent.addActionListener(e -> setAll("ABSENT"));
        JButton save = UITheme.primary("Save attendance");
        save.addActionListener(e -> saveAttendance());

        markCourse.setPreferredSize(new Dimension(230, 28));
        bar.add(new JLabel("Course"));
        bar.add(markCourse);
        bar.add(new JLabel("Date"));
        bar.add(markDate);
        bar.add(load);
        bar.add(allPresent);
        bar.add(allAbsent);
        bar.add(save);

        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());
        card.add(UITheme.scroll(markTable), BorderLayout.CENTER);

        root.add(bar, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildReportTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(14, 4, 4, 4));

        JPanel bar = UITheme.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        repCourse.setPreferredSize(new Dimension(230, 28));
        JButton gen = UITheme.primary("Generate report");
        gen.addActionListener(e -> loadSummary());
        JButton csv = UITheme.neutral("Export CSV");
        csv.addActionListener(e -> export(true));
        JButton pdf = UITheme.neutral("Export PDF");
        pdf.addActionListener(e -> export(false));

        bar.add(new JLabel("Course"));
        bar.add(repCourse);
        bar.add(new JLabel("Month"));
        bar.add(repMonth);
        bar.add(new JLabel("Year"));
        bar.add(repYear);
        bar.add(gen);
        bar.add(csv);
        bar.add(pdf);

        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());
        card.add(UITheme.scroll(repTable), BorderLayout.CENTER);

        root.add(bar, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);
        return root;
    }

    @Override
    public void refresh() {
        try {
            Object a = markCourse.getSelectedItem();
            Object b = repCourse.getSelectedItem();
            markCourse.removeAllItems();
            repCourse.removeAllItems();
            repCourse.addItem(null);
            for (Course c : courseController.findAll()) {
                markCourse.addItem(c);
                repCourse.addItem(c);
            }
            if (a != null) markCourse.setSelectedItem(a);
            if (b != null) repCourse.setSelectedItem(b);
            if (markCourse.getItemCount() > 0 && markCourse.getSelectedItem() == null) {
                markCourse.setSelectedIndex(0);
            }
            loadMarkSheet();
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load courses.\n\n" + ex.getMessage());
        }
    }

    private void loadMarkSheet() {
        Course c = (Course) markCourse.getSelectedItem();
        if (c == null) return;
        try {
            markStudents = controller.studentsOfCourse(c.getCourseId());
            LocalDate date = UITheme.spinnerDate(markDate);
            List<Attendance> existing = controller.byDate(date, c.getCourseId());
            Map<Integer, String> byStudent = new HashMap<>();
            for (Attendance a : existing) byStudent.put(a.getStudentId(), a.getStatus());

            markModel.setRowCount(0);
            for (Student s : markStudents) {
                markModel.addRow(new Object[]{s.getStudentCode(), s.getFullName(),
                        s.getCourseName(), byStudent.getOrDefault(s.getStudentId(), "PRESENT")});
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load the marking sheet.\n\n" + ex.getMessage());
        }
    }

    private void setAll(String status) {
        for (int i = 0; i < markModel.getRowCount(); i++) {
            markModel.setValueAt(status, i, 3);
        }
    }

    private void saveAttendance() {
        Course c = (Course) markCourse.getSelectedItem();
        if (c == null) {
            Dialogs.warn(this, "Select a course first.");
            return;
        }
        if (markModel.getRowCount() == 0) {
            Dialogs.warn(this, "No students loaded. Click \"Load students\" first.");
            return;
        }
        LocalDate date = UITheme.spinnerDate(markDate);
        try {
            int saved = 0;
            for (int i = 0; i < markModel.getRowCount(); i++) {
                String code = String.valueOf(markModel.getValueAt(i, 0));
                String status = String.valueOf(markModel.getValueAt(i, 3));
                Student s = markStudents.stream()
                        .filter(x -> x.getStudentCode().equals(code)).findFirst().orElse(null);
                if (s == null) continue;
                controller.mark(s.getStudentId(), c.getCourseId(), date, status,
                        SessionManager.getInstance().getCurrentAdmin().getAdminId());
                saved++;
            }
            Dialogs.success(this, "Attendance saved for " + saved + " students on "
                    + utils.DateUtil.format(date) + ".");
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not save attendance.\n\n" + ex.getMessage());
        }
    }

    private void loadSummary() {
        YearMonth month = YearMonth.of((Integer) repYear.getValue(),
                repMonth.getSelectedIndex() + 1);
        Course c = (Course) repCourse.getSelectedItem();
        int courseId = c == null ? 0 : c.getCourseId();
        try {
            summary = controller.monthly(month, courseId);
            repModel.setRowCount(0);
            for (Object[] r : controller.summaryRows(summary)) repModel.addRow(r);
            if (summary.isEmpty()) {
                Dialogs.info(this, "No attendance recorded for "
                        + AttendanceController.monthLabel(month) + ".");
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Report failed.\n\n" + ex.getMessage());
        }
    }

    private void export(boolean csv) {
        if (summary.isEmpty()) {
            Dialogs.warn(this, "Generate the report first.");
            return;
        }
        try {
            YearMonth month = YearMonth.of((Integer) repYear.getValue(),
                    repMonth.getSelectedIndex() + 1);
            TableData t = controller.summaryTable(summary,
                    AttendanceController.monthLabel(month));
            File f = csv
                ? ExportUtil.exportCsv(this, t, "attendance_" + month)
                : ExportUtil.exportPdf(this, t, "Attendance Report - "
                        + AttendanceController.monthLabel(month),
                        "Monthly attendance with percentage and shortage flags");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
