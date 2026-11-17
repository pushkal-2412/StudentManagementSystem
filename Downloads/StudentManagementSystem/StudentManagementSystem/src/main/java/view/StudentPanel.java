package view;

import controller.StudentController;
import model.Course;
import model.Department;
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
import java.util.ArrayList;
import java.util.List;

/** Student CRUD screen with search, filters and CSV/PDF export. */
public class StudentPanel extends JPanel implements Refreshable {

    private final StudentController controller = new StudentController();

    private final JTextField searchField = UITheme.field(18);
    private final JComboBox<Object> deptFilter = new JComboBox<>();
    private final JComboBox<Object> courseFilter = new JComboBox<>();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel countLabel = UITheme.subtitle("0 students");
    private List<Student> current = new ArrayList<>();

    public StudentPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        model = new DefaultTableModel(StudentController.COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);

        model.addTableModelListener(e -> countLabel.setText(model.getRowCount() + " students"));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);

        JPanel filters = UITheme.card();
        filters.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));

        searchField.setToolTipText("Search by name, student ID or email");
        JButton searchBtn = UITheme.primary("Search");
        searchBtn.addActionListener(e -> applyFilter());
        searchField.addActionListener(e -> applyFilter());

        JButton reset = UITheme.neutral("Reset");
        reset.addActionListener(e -> {
            searchField.setText("");
            if (deptFilter.getItemCount() > 0) deptFilter.setSelectedIndex(0);
            if (courseFilter.getItemCount() > 0) courseFilter.setSelectedIndex(0);
            refresh();
        });

        deptFilter.addActionListener(e -> {
            if (deptFilter.isShowing()) applyFilter();
        });

        filters.add(new JLabel("Search"));
        ((JLabel) filters.getComponent(0)).setFont(UITheme.font(Font.BOLD, 11.5f));
        filters.add(searchField);
        filters.add(new JLabel("Department"));
        filters.add(deptFilter);
        filters.add(new JLabel("Course"));
        filters.add(courseFilter);
        filters.add(searchBtn);
        filters.add(reset);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton add = UITheme.success("+  Add student");
        add.addActionListener(e -> openForm(null));
        JButton edit = UITheme.primary("Edit");
        edit.addActionListener(e -> editSelected());
        JButton delete = UITheme.danger("Delete");
        delete.addActionListener(e -> deleteSelected());
        JButton csv = UITheme.neutral("Export CSV");
        csv.addActionListener(e -> exportCsv());
        JButton pdf = UITheme.neutral("Export PDF");
        pdf.addActionListener(e -> exportPdf());

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(csv);
        actions.add(pdf);
        actions.add(countLabel);

        wrap.add(filters, BorderLayout.NORTH);
        wrap.add(actions, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildBody() {
        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        loadFilters();
        applyFilter();
    }

    private void loadFilters() {
        try {
            Object dep = deptFilter.getSelectedItem();
            Object cou = courseFilter.getSelectedItem();
            deptFilter.removeAllItems();
            deptFilter.addItem("All departments");
            for (Department d : controller.departments()) deptFilter.addItem(d);
            courseFilter.removeAllItems();
            courseFilter.addItem("All courses");
            for (Course c : controller.courses()) courseFilter.addItem(c);
            if (dep != null) deptFilter.setSelectedItem(dep);
            if (cou != null) courseFilter.setSelectedItem(cou);
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load filters.\n\n" + ex.getMessage());
        }
    }

    private void applyFilter() {
        int deptId = 0;
        if (deptFilter.getSelectedItem() instanceof Department d) deptId = d.getDeptId();
        int courseId = 0;
        if (courseFilter.getSelectedItem() instanceof Course c) courseId = c.getCourseId();
        try {
            current = controller.search(searchField.getText(), deptId, courseId);
            Object[][] rows = controller.rows(current);
            model.setRowCount(0);
            for (Object[] r : rows) model.addRow(r);
        } catch (SQLException ex) {
            Dialogs.error(this, "Search failed.\n\n" + ex.getMessage());
        }
    }

    private Student selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Dialogs.warn(this, "Please select a student in the table first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object idObj = model.getValueAt(modelRow, 0);
        int id = Integer.parseInt(String.valueOf(idObj));
        return current.stream().filter(s -> s.getStudentId() == id).findFirst().orElse(null);
    }

    private void openForm(Student s) {
        StudentFormDialog dlg = new StudentFormDialog(
                SwingUtilities.getWindowAncestor(this), controller, s);
        dlg.setVisible(true);
        if (dlg.isSaved()) applyFilter();
    }

    private void editSelected() {
        Student s = selected();
        if (s != null) openForm(s);
    }

    private void deleteSelected() {
        Student s = selected();
        if (s == null) return;
        String msg = "Delete this student permanently?\n\n" + controller.describe(s)
                + "\n\nThis also removes their attendance, marks, fee account and payments.";
        if (!Dialogs.confirm(this, msg)) return;
        try {
            controller.delete(s.getStudentId());
            Dialogs.success(this, "Student " + s.getStudentCode() + " was deleted.");
            applyFilter();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Delete failed.\n\n" + ex.getMessage());
        }
    }

    private void exportCsv() {
        try {
            TableData data = controller.toTable(current);
            File f = ExportUtil.exportCsv(this, data, "students");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }

    private void exportPdf() {
        try {
            TableData data = controller.toTable(current);
            File f = ExportUtil.exportPdf(this, data, "Student Report",
                    "All students - generated by " + SessionManager.getInstance().getUsername());
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
