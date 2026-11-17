package view;

import controller.ExamController;
import model.Course;
import model.Examination;
import model.Mark;
import model.Student;
import model.StudentResult;
import model.TableData;
import utils.ExportUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Examination module: exams, marks entry with validation, results and grades. */
public class ExamPanel extends JPanel implements Refreshable {

    private final ExamController controller = new ExamController();

    private final DefaultTableModel examModel;
    private final JTable examTable;
    private final DefaultTableModel markModel;
    private final JTable markTable;
    private final DefaultTableModel resultModel;
    private final JTable resultTable;

    private final JComboBox<Course> resultCourse = new JComboBox<>();
    private List<Examination> exams = new ArrayList<>();
    private List<Mark> marks = new ArrayList<>();
    private List<StudentResult> results = new ArrayList<>();
    private Examination selectedExam;

    public ExamPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(14, 20, 14, 20));

        examModel = new DefaultTableModel(ExamController.EXAM_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        examTable = new JTable(examModel);
        UITheme.styleTable(examTable);
        examTable.getColumnModel().getColumn(0).setMaxWidth(70);
        examTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadMarks();
        });

        markModel = new DefaultTableModel(ExamController.MARK_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        markTable = new JTable(markModel);
        UITheme.styleTable(markTable);

        resultModel = new DefaultTableModel(ExamController.RESULT_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultTable = new JTable(resultModel);
        UITheme.styleTable(resultTable);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.font(Font.BOLD, 12.5f));
        tabs.addTab("Examinations & marks", buildExamTab());
        tabs.addTab("Result sheet", buildResultTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildExamTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(14, 4, 4, 4));

        JPanel bar = UITheme.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton add = UITheme.success("+  Add examination");
        add.addActionListener(e -> addExam());
        JButton edit = UITheme.primary("Edit exam");
        edit.addActionListener(e -> editExam());
        JButton del = UITheme.danger("Delete exam");
        del.addActionListener(e -> deleteExam());
        JButton enter = UITheme.primary("Enter / update marks");
        enter.addActionListener(e -> enterMarks());
        JButton csv = UITheme.neutral("Export marks CSV");
        csv.addActionListener(e -> exportMarks(true));
        JButton pdf = UITheme.neutral("Export marks PDF");
        pdf.addActionListener(e -> exportMarks(false));

        bar.add(add);
        bar.add(edit);
        bar.add(del);
        bar.add(enter);
        bar.add(csv);
        bar.add(pdf);

        JPanel topCard = UITheme.card();
        topCard.setLayout(new BorderLayout());
        topCard.add(UITheme.scroll(examTable), BorderLayout.CENTER);

        JPanel bottomCard = UITheme.card();
        bottomCard.setLayout(new BorderLayout(0, 8));
        JLabel t = new JLabel("Marks recorded for the selected examination");
        t.setFont(UITheme.h3());
        bottomCard.add(t, BorderLayout.NORTH);
        bottomCard.add(UITheme.scroll(markTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topCard, bottomCard);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(10);

        root.add(bar, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildResultTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(14, 4, 4, 4));

        JPanel bar = UITheme.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        resultCourse.setPreferredSize(new Dimension(240, 28));
        JButton gen = UITheme.primary("Calculate results");
        gen.addActionListener(e -> loadResults());
        JButton csv = UITheme.neutral("Export CSV");
        csv.addActionListener(e -> exportResults(true));
        JButton pdf = UITheme.neutral("Export PDF");
        pdf.addActionListener(e -> exportResults(false));

        bar.add(new JLabel("Course"));
        bar.add(resultCourse);
        bar.add(gen);
        bar.add(csv);
        bar.add(pdf);

        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());
        card.add(UITheme.scroll(resultTable), BorderLayout.CENTER);

        root.add(bar, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);
        return root;
    }

    @Override
    public void refresh() {
        try {
            Object keep = resultCourse.getSelectedItem();
            resultCourse.removeAllItems();
            resultCourse.addItem(null);
            for (Course c : controller.courses()) resultCourse.addItem(c);
            if (keep != null) resultCourse.setSelectedItem(keep);

            exams = controller.exams();
            examModel.setRowCount(0);
            for (Object[] r : controller.examRows(exams)) examModel.addRow(r);
            if (!exams.isEmpty() && examTable.getRowCount() > 0
                    && examTable.getSelectedRow() < 0) {
                examTable.setRowSelectionInterval(0, 0);
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load examinations.\n\n" + ex.getMessage());
        }
    }

    private Examination selectedExam() {
        int vr = examTable.getSelectedRow();
        if (vr < 0) return null;
        int mr = examTable.convertRowIndexToModel(vr);
        int id = Integer.parseInt(String.valueOf(examModel.getValueAt(mr, 0)));
        return exams.stream().filter(e -> e.getExamId() == id).findFirst().orElse(null);
    }

    private void loadMarks() {
        Examination e = selectedExam();
        selectedExam = e;
        if (e == null) {
            markModel.setRowCount(0);
            return;
        }
        try {
            marks = controller.marksOfExam(e.getExamId());
            markModel.setRowCount(0);
            for (Object[] r : controller.markRows(marks)) markModel.addRow(r);
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load marks.\n\n" + ex.getMessage());
        }
    }

    private void addExam() {
        JTextField name = UITheme.field(16);
        JComboBox<Course> course = new JComboBox<>();
        JSpinner date = UITheme.dateSpinner(LocalDate.now());
        JTextField max = UITheme.field(8);
        JComboBox<String> type = UITheme.combo("INTERNAL", "MIDTERM", "SEMESTER", "PRACTICAL");
        max.setText("100");
        try {
            for (Course c : controller.courses()) course.addItem(c);
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }
        JPanel p = form(name, course, date, max, type,
                "Examination name", "Course", "Date", "Maximum marks", "Type");
        int r = JOptionPane.showConfirmDialog(this, p, "Add examination",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            Examination e = new Examination();
            e.setExamName(name.getText());
            Course c = (Course) course.getSelectedItem();
            if (c == null) throw new IllegalArgumentException("Select a course.");
            e.setCourseId(c.getCourseId());
            e.setExamDate(UITheme.spinnerDate(date));
            e.setMaxMarks(Double.parseDouble(max.getText().trim()));
            e.setExamType((String) type.getSelectedItem());
            controller.createExam(e);
            Dialogs.success(this, "Examination created.");
            refresh();
        } catch (NumberFormatException ex) {
            Dialogs.warn(this, "Maximum marks must be a number.");
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private JPanel form(JTextField name, JComboBox<Course> course, JSpinner date,
                        JTextField max, JComboBox<String> type, String... labels) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(new JLabel(labels[0]));
        p.add(name);
        p.add(new JLabel(labels[1]));
        p.add(course);
        p.add(new JLabel(labels[2]));
        p.add(date);
        p.add(new JLabel(labels[3]));
        p.add(max);
        p.add(new JLabel(labels[4]));
        p.add(type);
        return p;
    }

    private void editExam() {
        Examination e = selectedExam();
        if (e == null) {
            Dialogs.warn(this, "Select an examination first.");
            return;
        }
        JTextField name = UITheme.field(16);
        name.setText(e.getExamName());
        JComboBox<Course> course = new JComboBox<>();
        JSpinner date = UITheme.dateSpinner(e.getExamDate());
        JTextField max = UITheme.field(8);
        max.setText(String.valueOf(e.getMaxMarks()));
        JComboBox<String> type = UITheme.combo("INTERNAL", "MIDTERM", "SEMESTER", "PRACTICAL");
        type.setSelectedItem(e.getExamType());
        try {
            for (Course c : controller.courses()) {
                course.addItem(c);
                if (c.getCourseId() == e.getCourseId()) course.setSelectedItem(c);
            }
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }
        JPanel p = form(name, course, date, max, type,
                "Examination name", "Course", "Date", "Maximum marks", "Type");
        int r = JOptionPane.showConfirmDialog(this, p, "Edit examination",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            e.setExamName(name.getText());
            Course c = (Course) course.getSelectedItem();
            if (c == null) throw new IllegalArgumentException("Select a course.");
            e.setCourseId(c.getCourseId());
            e.setExamDate(UITheme.spinnerDate(date));
            e.setMaxMarks(Double.parseDouble(max.getText().trim()));
            e.setExamType((String) type.getSelectedItem());
            controller.updateExam(e);
            Dialogs.success(this, "Examination updated.");
            refresh();
        } catch (NumberFormatException ex) {
            Dialogs.warn(this, "Maximum marks must be a number.");
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void deleteExam() {
        Examination e = selectedExam();
        if (e == null) {
            Dialogs.warn(this, "Select an examination first.");
            return;
        }
        if (!Dialogs.confirm(this, "Delete examination \"" + e.getExamName() + "\"?\n\n"
                + "All marks recorded for it are deleted as well.")) return;
        try {
            controller.deleteExam(e.getExamId());
            Dialogs.success(this, "Examination deleted.");
            refresh();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void enterMarks() {
        Examination e = selectedExam();
        if (e == null) {
            Dialogs.warn(this, "Select an examination first.");
            return;
        }
        try {
            List<Student> students = controller.studentsOfCourse(e.getCourseId());
            if (students.isEmpty()) {
                Dialogs.warn(this, "No students are enrolled in "
                        + e.getCourseName() + ".");
                return;
            }
            JComboBox<Student> studentCombo = new JComboBox<>();
            for (Student s : students) studentCombo.addItem(s);

            JSpinner marksSpinner = new JSpinner(new SpinnerNumberModel(
                    0.0, 0.0, e.getMaxMarks(), 0.5));
            marksSpinner.setEditor(new JSpinner.NumberEditor(marksSpinner, "0.00"));

            JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
            p.setBorder(new EmptyBorder(8, 8, 8, 8));
            p.add(new JLabel("Examination: " + e.getExamName()
                    + "  (max " + utils.ValidationUtil.fmt(e.getMaxMarks()) + ")"));
            p.add(new JLabel("Student"));
            p.add(studentCombo);
            p.add(new JLabel("Marks obtained (0 - "
                    + utils.ValidationUtil.fmt(e.getMaxMarks()) + ")"));
            p.add(marksSpinner);

            int r = JOptionPane.showConfirmDialog(this, p, "Enter marks",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return;

            Student s = (Student) studentCombo.getSelectedItem();
            if (s == null) return;
            double value = ((Number) marksSpinner.getValue()).doubleValue();
            controller.saveMark(e.getExamId(), s.getStudentId(), value);
            Dialogs.success(this, "Marks saved for " + s.getFullName()
                    + ": " + utils.ValidationUtil.fmt(value) + " / "
                    + utils.ValidationUtil.fmt(e.getMaxMarks())
                    + "  (grade " + controller.gradeFor(
                        value * 100 / e.getMaxMarks()) + ")");
            loadMarks();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void loadResults() {
        Course c = (Course) resultCourse.getSelectedItem();
        int courseId = c == null ? 0 : c.getCourseId();
        try {
            results = controller.results(courseId);
            resultModel.setRowCount(0);
            for (Object[] r : controller.resultRows(results)) resultModel.addRow(r);
            if (results.isEmpty()) {
                Dialogs.info(this, "No students found for this selection.");
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not build the result sheet.\n\n" + ex.getMessage());
        }
    }

    private void exportMarks(boolean csv) {
        if (marks.isEmpty()) {
            Dialogs.warn(this, "Select an examination with recorded marks first.");
            return;
        }
        try {
            TableData t = controller.markTable(marks);
            File f = csv
                ? ExportUtil.exportCsv(this, t, "marks_" + selectedExam.getExamId())
                : ExportUtil.exportPdf(this, t, "Marks - " + selectedExam.getExamName(),
                        selectedExam.getCourseName() + "  |  maximum "
                        + utils.ValidationUtil.fmt(selectedExam.getMaxMarks()) + " marks");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }

    private void exportResults(boolean csv) {
        if (results.isEmpty()) {
            Dialogs.warn(this, "Calculate the results first.");
            return;
        }
        try {
            TableData t = controller.resultTable(results);
            File f = csv
                ? ExportUtil.exportCsv(this, t, "results")
                : ExportUtil.exportPdf(this, t, "Examination Result Sheet",
                        "Aggregate marks with automatic percentage and grade");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
