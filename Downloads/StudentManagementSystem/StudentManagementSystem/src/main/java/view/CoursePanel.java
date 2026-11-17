package view;

import controller.CourseController;
import controller.StudentController;
import model.Course;
import model.Enrollment;
import model.Student;
import model.TableData;
import utils.ExportUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Course CRUD plus "assign student to course" and course-wise student list. */
public class CoursePanel extends JPanel implements Refreshable {

    private final CourseController controller = new CourseController();
    private final StudentController studentController = new StudentController();

    private final DefaultTableModel model;
    private final JTable table;
    private final DefaultTableModel enrollModel;
    private final JTable enrollTable;
    private final JLabel courseCount = UITheme.subtitle("0 courses");
    private List<Course> current = new ArrayList<>();
    private List<Enrollment> enrollments = new ArrayList<>();

    public CoursePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        model = new DefaultTableModel(CourseController.COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadEnrollments();
        });

        enrollModel = new DefaultTableModel(
                new Object[]{"Enrollment ID", "Student ID", "Student Name", "Course",
                             "Enrolled On", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        enrollTable = new JTable(enrollModel);
        UITheme.styleTable(enrollTable);

        add(buildTop(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
    }

    private JPanel buildTop() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton add = UITheme.success("+  Add course");
        add.addActionListener(e -> openForm(null));
        JButton edit = UITheme.primary("Edit");
        edit.addActionListener(e -> editSelected());
        JButton del = UITheme.danger("Delete");
        del.addActionListener(e -> deleteSelected());
        JButton assign = UITheme.neutral("Assign student");
        assign.addActionListener(e -> assignStudent());
        JButton remove = UITheme.neutral("Remove enrollment");
        remove.addActionListener(e -> removeEnrollment());
        JButton csv = UITheme.neutral("Export CSV");
        csv.addActionListener(e -> exportCsv());

        actions.add(add);
        actions.add(edit);
        actions.add(del);
        actions.add(assign);
        actions.add(remove);
        actions.add(csv);
        actions.add(courseCount);

        wrap.add(actions, BorderLayout.NORTH);
        wrap.add(UITheme.subtitle("Select a course to see its enrolled students below."),
                BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildSplit() {
        JPanel topCard = UITheme.card();
        topCard.setLayout(new BorderLayout());
        topCard.add(UITheme.scroll(table), BorderLayout.CENTER);

        JPanel bottomCard = UITheme.card();
        bottomCard.setLayout(new BorderLayout(0, 8));
        JLabel t = new JLabel("Students enrolled in the selected course");
        t.setFont(UITheme.h3());
        bottomCard.add(t, BorderLayout.NORTH);
        bottomCard.add(UITheme.scroll(enrollTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topCard, bottomCard);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(10);

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(split, BorderLayout.CENTER);
        return holder;
    }

    @Override
    public void refresh() {
        try {
            current = controller.findAll();
            model.setRowCount(0);
            for (Object[] r : controller.rows(current)) model.addRow(r);
            courseCount.setText(current.size() + " courses");
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load courses.\n\n" + ex.getMessage());
        }
    }

    private Course selectedCourse() {
        int vr = table.getSelectedRow();
        if (vr < 0) return null;
        int mr = table.convertRowIndexToModel(vr);
        int id = Integer.parseInt(String.valueOf(model.getValueAt(mr, 0)));
        return current.stream().filter(c -> c.getCourseId() == id).findFirst().orElse(null);
    }

    private void loadEnrollments() {
        Course c = selectedCourse();
        if (c == null) return;
        try {
            enrollments = controller.enrolledStudents(c.getCourseId());
            enrollModel.setRowCount(0);
            for (Enrollment e : enrollments) {
                enrollModel.addRow(new Object[]{e.getEnrollmentId(), e.getStudentCode(),
                        e.getStudentName(), e.getCourseName(),
                        e.getEnrollDate(), e.getStatus()});
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load enrollments.\n\n" + ex.getMessage());
        }
    }

    private void openForm(Course c) {
        JTextField code = UITheme.field(14);
        JTextField name = UITheme.field(14);
        JComboBox<model.Department> dept = new JComboBox<>();
        JSpinner duration = new JSpinner(new SpinnerNumberModel(4, 1, 6, 1));
        JTextField fee = UITheme.field(14);
        JTextArea desc = new JTextArea(3, 14);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);

        try {
            for (model.Department d : studentController.departments()) dept.addItem(d);
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }

        if (c != null) {
            code.setText(c.getCourseCode());
            name.setText(c.getCourseName());
            duration.setValue(c.getDurationYears());
            fee.setText(String.valueOf(c.getCourseFee()));
            desc.setText(c.getDescription() == null ? "" : c.getDescription());
            for (int i = 0; i < dept.getItemCount(); i++) {
                if (dept.getItemAt(i).getDeptId() == c.getDeptId()) {
                    dept.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            fee.setText("250000");
        }

        JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Course code (e.g. BTECH-CSE)"));
        p.add(code);
        p.add(new JLabel("Course name"));
        p.add(name);
        p.add(new JLabel("Department"));
        p.add(dept);
        p.add(new JLabel("Duration (years)"));
        p.add(duration);
        p.add(new JLabel("Total course fee"));
        p.add(fee);
        p.add(new JLabel("Description"));
        p.add(new JScrollPane(desc));

        int r = JOptionPane.showConfirmDialog(this, p,
                c == null ? "Add course" : "Edit course",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            Course target = c == null ? new Course() : c;
            target.setCourseCode(code.getText());
            target.setCourseName(name.getText());
            model.Department d = (model.Department) dept.getSelectedItem();
            if (d == null) throw new IllegalArgumentException("Select a department.");
            target.setDeptId(d.getDeptId());
            target.setDurationYears((Integer) duration.getValue());
            target.setCourseFee(Double.parseDouble(fee.getText().trim()));
            target.setDescription(desc.getText());

            if (c == null) {
                controller.create(target);
                Dialogs.success(this, "Course created.");
            } else {
                controller.update(target);
                Dialogs.success(this, "Course updated.");
            }
            refresh();
        } catch (NumberFormatException ex) {
            Dialogs.warn(this, "Course fee must be a number.");
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void editSelected() {
        Course c = selectedCourse();
        if (c == null) {
            Dialogs.warn(this, "Select a course first.");
            return;
        }
        openForm(c);
    }

    private void deleteSelected() {
        Course c = selectedCourse();
        if (c == null) {
            Dialogs.warn(this, "Select a course first.");
            return;
        }
        if (!Dialogs.confirm(this, "Delete course " + c.getCourseCode() + " - "
                + c.getCourseName() + "?\n\nOnly possible when no students are attached.")) return;
        try {
            controller.delete(c.getCourseId());
            Dialogs.success(this, "Course deleted.");
            refresh();
        } catch (SQLException ex) {
            Dialogs.warn(this, ex.getMessage());
        }
    }

    private void assignStudent() {
        Course c = selectedCourse();
        if (c == null) {
            Dialogs.warn(this, "Select a course first (this is the course the student joins).");
            return;
        }
        JComboBox<Student> students = new JComboBox<>();
        try {
            for (Student s : studentController.search(null, 0, 0)) students.addItem(s);
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Course: " + c.getCourseCode() + " - " + c.getCourseName()));
        p.add(new JLabel("Student"));
        p.add(students);

        int r = JOptionPane.showConfirmDialog(this, p, "Assign student to course",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        Student s = (Student) students.getSelectedItem();
        if (s == null) return;

        try {
            boolean ok = controller.assignStudent(s.getStudentId(), c.getCourseId());
            if (ok) {
                Dialogs.success(this, s.getFullName() + " was enrolled in " + c.getCourseCode() + ".");
            } else {
                Dialogs.warn(this, s.getFullName() + " is already enrolled in this course.");
            }
            loadEnrollments();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void removeEnrollment() {
        int vr = enrollTable.getSelectedRow();
        if (vr < 0) {
            Dialogs.warn(this, "Select an enrollment row first.");
            return;
        }
        int mr = enrollTable.convertRowIndexToModel(vr);
        int id = Integer.parseInt(String.valueOf(enrollModel.getValueAt(mr, 0)));
        String student = String.valueOf(enrollModel.getValueAt(mr, 2));
        if (!Dialogs.confirm(this, "Remove " + student + " from this course?\n"
                + "The student record itself is kept.")) return;
        try {
            controller.removeEnrollment(id);
            Dialogs.success(this, "Enrollment removed.");
            loadEnrollments();
            refresh();
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void exportCsv() {
        try {
            TableData t = controller.toTable(current);
            File f = ExportUtil.exportCsv(this, t, "courses");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
