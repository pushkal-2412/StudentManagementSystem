package view;

import controller.StudentController;
import model.Course;
import model.Department;
import model.Student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;

/** Add / edit student form with full client-side validation. */
public class StudentFormDialog extends JDialog {

    private final StudentController controller;
    private final Student existing;
    private boolean saved;

    private final JTextField codeField = UITheme.field(16);
    private final JTextField nameField = UITheme.field(16);
    private final JSpinner dobSpinner = UITheme.dateSpinner(LocalDate.now().minusYears(20));
    private final JComboBox<String> genderCombo =
            UITheme.combo("Male", "Female", "Other");
    private final JTextField emailField = UITheme.field(16);
    private final JTextField phoneField = UITheme.field(16);
    private final JTextArea addressField = new JTextArea(3, 16);
    private final JComboBox<Department> deptCombo = new JComboBox<>();
    private final JComboBox<Course> courseCombo = new JComboBox<>();
    private final JTextField yearField = UITheme.field(16);
    private final JSpinner admissionSpinner = UITheme.dateSpinner(LocalDate.now());
    private final JSpinner semesterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 8, 1));

    public StudentFormDialog(Window owner, StudentController controller, Student existing) {
        super(owner, existing == null ? "Add new student" : "Edit student", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.existing = existing;
        setSize(720, 620);
        setLocationRelativeTo(owner);
        setContentPane(build());
        loadLookups();
        if (existing != null) populate(existing);
        else prefillNew();
    }

    public boolean isSaved() { return saved; }

    private JPanel build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(18, 24, 10, 24));
        JLabel t = UITheme.title(existing == null ? "Register a new student" : "Update student record");
        t.setFont(UITheme.h2());
        header.add(t, BorderLayout.NORTH);
        header.add(UITheme.subtitle("Fields marked * are required. Student ID is generated automatically."),
                BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(6, 24, 10, 24));

        addressField.setFont(UITheme.body());
        addressField.setLineWrap(true);
        addressField.setWrapStyleWord(true);
        addressField.setBorder(new EmptyBorder(6, 9, 6, 9));

        int row = 0;
        row = addField(form, row, 0, "Student ID *", codeField);
        row = addField(form, row, 1, "Full name *", nameField);
        row = addField(form, row, 0, "Date of birth *", dobSpinner);
        row = addField(form, row, 1, "Gender *", genderCombo);
        row = addField(form, row, 0, "Email address *", emailField);
        row = addField(form, row, 1, "Phone number *", phoneField);
        row = addField(form, row, 0, "Department *", deptCombo);
        row = addField(form, row, 1, "Course *", courseCombo);
        row = addField(form, row, 0, "Academic year *", yearField);
        row = addField(form, row, 1, "Admission date *", admissionSpinner);
        row = addField(form, row, 0, "Semester *", semesterSpinner);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(0, 0, 4, 12);
        JLabel al = new JLabel("Address *");
        al.setFont(UITheme.font(Font.BOLD, 11.5f));
        al.setForeground(UITheme.MUTED);
        form.add(al, gc);
        gc.gridy = row + 1;
        gc.insets = new Insets(0, 0, 8, 12);
        form.add(new JScrollPane(addressField), gc);

        deptCombo.addActionListener(e -> filterCourses());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(new EmptyBorder(4, 24, 16, 24));
        JButton cancel = UITheme.neutral("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = UITheme.primary(existing == null ? "Register student" : "Save changes");
        save.addActionListener(e -> save());
        buttons.add(cancel);
        buttons.add(save);

        root.add(header, BorderLayout.NORTH);
        root.add(new JScrollPane(form), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        return root;
    }

    private int addField(JPanel form, int row, int col, String label, JComponent field) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = col; gc.gridy = row * 2;
        gc.weightx = 0.5; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, col == 0 ? 0 : 12, 3, 0);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.font(Font.BOLD, 11.5f));
        l.setForeground(UITheme.MUTED);
        form.add(l, gc);

        gc.gridy = row * 2 + 1;
        gc.insets = new Insets(0, col == 0 ? 0 : 12, 6, 0);
        field.setFont(UITheme.body());
        form.add(field, gc);

        return col == 1 ? row + 1 : row;
    }

    private void loadLookups() {
        try {
            for (Department d : controller.departments()) deptCombo.addItem(d);
            for (Course c : controller.courses()) courseCombo.addItem(c);
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load departments/courses.\n\n" + ex.getMessage());
        }
    }

    private void filterCourses() {
        Object sel = deptCombo.getSelectedItem();
        if (!(sel instanceof Department d)) return;
        Object current = courseCombo.getSelectedItem();
        courseCombo.removeAllItems();
        try {
            for (Course c : controller.courses()) {
                if (c.getDeptId() == d.getDeptId()) courseCombo.addItem(c);
            }
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
        }
        if (current instanceof Course cc) {
            for (int i = 0; i < courseCombo.getItemCount(); i++) {
                if (courseCombo.getItemAt(i).getCourseId() == cc.getCourseId()) {
                    courseCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void prefillNew() {
        try {
            codeField.setText(controller.suggestCode());
        } catch (SQLException ignored) {
            codeField.setText("STU" + LocalDate.now().getYear() + "001");
        }
        yearField.setText(LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1));
    }

    private void populate(Student s) {
        codeField.setText(s.getStudentCode());
        nameField.setText(s.getFullName());
        dobSpinner.setValue(UITheme.toDate(s.getDob()));
        genderCombo.setSelectedItem(s.getGender());
        emailField.setText(s.getEmail());
        phoneField.setText(s.getPhone());
        addressField.setText(s.getAddress() == null ? "" : s.getAddress());
        for (int i = 0; i < deptCombo.getItemCount(); i++) {
            if (deptCombo.getItemAt(i).getDeptId() == s.getDeptId()) {
                deptCombo.setSelectedIndex(i);
                break;
            }
        }
        filterCourses();
        for (int i = 0; i < courseCombo.getItemCount(); i++) {
            if (courseCombo.getItemAt(i).getCourseId() == s.getCourseId()) {
                courseCombo.setSelectedIndex(i);
                break;
            }
        }
        yearField.setText(s.getAcademicYear());
        admissionSpinner.setValue(UITheme.toDate(s.getAdmissionDate()));
        semesterSpinner.setValue(s.getSemester());
    }

    private void save() {
        try {
            Student s = existing == null ? new Student() : existing;
            s.setStudentCode(codeField.getText());
            s.setFullName(nameField.getText());
            s.setDob(UITheme.spinnerDate(dobSpinner));
            s.setGender((String) genderCombo.getSelectedItem());
            s.setEmail(emailField.getText());
            s.setPhone(phoneField.getText());
            s.setAddress(addressField.getText());
            Department d = (Department) deptCombo.getSelectedItem();
            if (d == null) throw new IllegalArgumentException("Please select a department.");
            s.setDeptId(d.getDeptId());
            Course c = (Course) courseCombo.getSelectedItem();
            if (c == null) throw new IllegalArgumentException("Please select a course.");
            s.setCourseId(c.getCourseId());
            s.setAcademicYear(yearField.getText());
            s.setAdmissionDate(UITheme.spinnerDate(admissionSpinner));
            s.setSemester((Integer) semesterSpinner.getValue());

            if (existing == null) {
                Student created = controller.create(s);
                Dialogs.success(this, "Student registered successfully.\n\n"
                        + "Student ID: " + created.getStudentCode()
                        + "\nName: " + created.getFullName()
                        + "\nA fee account and course enrollment were created automatically.");
            } else {
                controller.update(s);
                Dialogs.success(this, "Student record updated.");
            }
            saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error while saving.\n\n" + ex.getMessage());
        }
    }
}
