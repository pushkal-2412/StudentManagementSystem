package view;

import controller.DepartmentController;
import model.Department;
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

/** Department management: list, add, edit, delete. */
public class DepartmentPanel extends JPanel implements Refreshable {

    private final DepartmentController controller = new DepartmentController();
    private final DefaultTableModel model;
    private final JTable table;
    private List<Department> current = new ArrayList<>();

    public DepartmentPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        model = new DefaultTableModel(DepartmentController.COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton add = UITheme.success("+  Add department");
        add.addActionListener(e -> openForm(null));
        JButton edit = UITheme.primary("Edit");
        edit.addActionListener(e -> editSelected());
        JButton del = UITheme.danger("Delete");
        del.addActionListener(e -> deleteSelected());
        JButton csv = UITheme.neutral("Export CSV");
        csv.addActionListener(e -> exportCsv());
        actions.add(add);
        actions.add(edit);
        actions.add(del);
        actions.add(csv);

        JPanel card = UITheme.card();
        card.setLayout(new BorderLayout());
        card.add(UITheme.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(actions, BorderLayout.NORTH);
        top.add(UITheme.subtitle("Departments group courses and students. A department in use cannot be deleted."),
                BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            current = controller.findAll();
            model.setRowCount(0);
            for (Object[] r : controller.rows(current)) model.addRow(r);
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load departments.\n\n" + ex.getMessage());
        }
    }

    private Department selected() {
        int vr = table.getSelectedRow();
        if (vr < 0) {
            Dialogs.warn(this, "Select a department first.");
            return null;
        }
        int mr = table.convertRowIndexToModel(vr);
        int id = Integer.parseInt(String.valueOf(model.getValueAt(mr, 0)));
        return current.stream().filter(d -> d.getDeptId() == id).findFirst().orElse(null);
    }

    private void openForm(Department d) {
        JTextField code = UITheme.field(14);
        JTextField name = UITheme.field(14);
        JTextField hod = UITheme.field(14);
        if (d != null) {
            code.setText(d.getDeptCode());
            name.setText(d.getDeptName());
            hod.setText(d.getHodName());
        }
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Department code (letters only, e.g. CSE)"));
        p.add(code);
        p.add(new JLabel("Department name"));
        p.add(name);
        p.add(new JLabel("Head of department"));
        p.add(hod);

        int r = JOptionPane.showConfirmDialog(this, p,
                d == null ? "Add department" : "Edit department",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            Department target = d == null ? new Department() : d;
            target.setDeptCode(code.getText());
            target.setDeptName(name.getText());
            target.setHodName(hod.getText());
            if (d == null) {
                controller.create(target);
                Dialogs.success(this, "Department created.");
            } else {
                controller.update(target);
                Dialogs.success(this, "Department updated.");
            }
            refresh();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void editSelected() {
        Department d = selected();
        if (d != null) openForm(d);
    }

    private void deleteSelected() {
        Department d = selected();
        if (d == null) return;
        if (!Dialogs.confirm(this, "Delete department " + d.getDeptCode() + " - "
                + d.getDeptName() + "?\n\nThis is only allowed when no students or courses "
                + "are attached.")) return;
        try {
            controller.delete(d.getDeptId());
            Dialogs.success(this, "Department deleted.");
            refresh();
        } catch (SQLException ex) {
            Dialogs.warn(this, ex.getMessage());
        }
    }

    private void exportCsv() {
        try {
            TableData t = controller.toTable(current);
            File f = ExportUtil.exportCsv(this, t, "departments");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
