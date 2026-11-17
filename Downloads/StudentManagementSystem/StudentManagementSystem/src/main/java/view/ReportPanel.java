package view;

import controller.CourseController;
import controller.DepartmentController;
import controller.ReportController;
import model.Course;
import model.Department;
import model.TableData;
import utils.ExportUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Central reporting screen; every report can be exported as CSV or PDF. */
public class ReportPanel extends JPanel implements Refreshable {

    private final ReportController controller = new ReportController();
    private final CourseController courseController = new CourseController();
    private final DepartmentController departmentController = new DepartmentController();

    private final JComboBox<String> reportType = UITheme.combo(
            "Institute summary",
            "Department-wise students",
            "Course-wise students",
            "Monthly attendance",
            "Examination results",
            "Fee status",
            "Payment history");

    private final JComboBox<Department> deptCombo = new JComboBox<>();
    private final JComboBox<Course> courseCombo = new JComboBox<>();
    private final JComboBox<String> monthCombo = UITheme.combo(
            "JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST",
            "SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER");
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
            LocalDate.now().getYear(), 2000, 2100, 1));
    private final JComboBox<String> feeStatus =
            UITheme.combo("ALL", "PAID", "PENDING", "PARTIALLY_PAID");

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel caption = UITheme.subtitle(" ");
    private TableData lastResult;

    public ReportPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        model = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UITheme.styleTable(table);

        add(buildTop(), BorderLayout.NORTH);
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    private JPanel buildTop() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        JPanel bar = UITheme.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        reportType.setPreferredSize(new Dimension(215, 28));
        reportType.addActionListener(e -> updateControls());
        courseCombo.setPreferredSize(new Dimension(210, 28));
        deptCombo.setPreferredSize(new Dimension(190, 28));

        JButton run = UITheme.primary("Generate");
        run.addActionListener(e -> generate());
        JButton csv = UITheme.neutral("Export CSV");
        csv.addActionListener(e -> export(true));
        JButton pdf = UITheme.neutral("Export PDF");
        pdf.addActionListener(e -> export(false));

        bar.add(new JLabel("Report"));
        bar.add(reportType);
        bar.add(deptCombo);
        bar.add(courseCombo);
        bar.add(monthCombo);
        bar.add(yearSpinner);
        bar.add(feeStatus);
        bar.add(run);
        bar.add(csv);
        bar.add(pdf);

        wrap.add(bar, BorderLayout.NORTH);
        wrap.add(caption, BorderLayout.SOUTH);
        return wrap;
    }

    @Override
    public void refresh() {
        try {
            Object d = deptCombo.getSelectedItem();
            Object c = courseCombo.getSelectedItem();
            deptCombo.removeAllItems();
            courseCombo.removeAllItems();
            courseCombo.addItem(null);
            for (Department dep : departmentController.findAll()) deptCombo.addItem(dep);
            for (Course co : courseController.findAll()) courseCombo.addItem(co);
            if (d != null) deptCombo.setSelectedItem(d);
            if (c != null) courseCombo.setSelectedItem(c);
            updateControls();
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load filters.\n\n" + ex.getMessage());
        }
    }

    private void updateControls() {
        String type = (String) reportType.getSelectedItem();
        boolean dept = "Department-wise students".equals(type);
        boolean course = "Course-wise students".equals(type)
                || "Examination results".equals(type);
        boolean monthly = "Monthly attendance".equals(type);
        boolean fees = "Fee status".equals(type);

        deptCombo.setVisible(dept);
        courseCombo.setVisible(course || monthly);
        monthCombo.setVisible(monthly);
        yearSpinner.setVisible(monthly);
        feeStatus.setVisible(fees);
        revalidate();
        repaint();
    }

    private void generate() {
        String type = (String) reportType.getSelectedItem();
        try {
            TableData data;
            String title;
            switch (type) {
                case "Department-wise students" -> {
                    Department d = (Department) deptCombo.getSelectedItem();
                    if (d == null) {
                        Dialogs.warn(this, "Select a department.");
                        return;
                    }
                    data = controller.departmentReport(d.getDeptId(), d.getDeptName());
                    title = "Department report - " + d.getDeptCode();
                }
                case "Course-wise students" -> {
                    Course c = (Course) courseCombo.getSelectedItem();
                    if (c == null) {
                        Dialogs.warn(this, "Select a course.");
                        return;
                    }
                    data = controller.courseReport(c.getCourseId());
                    title = "Course report - " + c.getCourseCode();
                }
                case "Monthly attendance" -> {
                    Course c = (Course) courseCombo.getSelectedItem();
                    YearMonth ym = YearMonth.of((Integer) yearSpinner.getValue(),
                            monthCombo.getSelectedIndex() + 1);
                    data = controller.attendanceReport(ym,
                            c == null ? 0 : c.getCourseId());
                    title = "Attendance report - " + ym;
                }
                case "Examination results" -> {
                    Course c = (Course) courseCombo.getSelectedItem();
                    data = controller.examReport(c == null ? 0 : c.getCourseId());
                    title = "Examination results"
                            + (c == null ? "" : " - " + c.getCourseCode());
                }
                case "Fee status" -> {
                    data = controller.feeReport((String) feeStatus.getSelectedItem());
                    title = "Fee status report";
                }
                case "Payment history" -> {
                    data = controller.paymentHistoryReport();
                    title = "Fee payment history";
                }
                default -> {
                    data = controller.summary();
                    title = "Institute summary";
                }
            }
            lastResult = data;
            render(data);
            caption.setText(title + "   |   " + data.size() + " rows");
        } catch (SQLException ex) {
            Dialogs.error(this, "Report failed.\n\n" + ex.getMessage());
        }
    }

    private void render(TableData data) {
        model.setRowCount(0);
        model.setColumnCount(0);
        for (String h : data.getHeaders()) model.addColumn(h);
        for (var row : data.getRows()) model.addRow(row.toArray());
    }

    private void export(boolean csv) {
        if (lastResult == null || lastResult.size() == 0) {
            Dialogs.warn(this, "Generate a report with data first.");
            return;
        }
        try {
            String type = (String) reportType.getSelectedItem();
            File f = csv
                ? ExportUtil.exportCsv(this, lastResult,
                        type.toLowerCase().replace(' ', '_'))
                : ExportUtil.exportPdf(this, lastResult, type,
                        "Student Management System report");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
