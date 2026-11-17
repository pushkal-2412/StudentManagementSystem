package controller;

import model.Department;
import model.TableData;
import service.DepartmentService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Mediates between the department view and the service layer. */
public class DepartmentController {

    private final DepartmentService service = new DepartmentService();

    public static final String[] COLUMNS = {
        "DB ID", "Code", "Department Name", "Head of Department", "Courses", "Students"
    };

    public List<Department> findAll() throws SQLException { return service.findAll(); }
    public Department create(Department d) throws SQLException { return service.create(d); }
    public void update(Department d) throws SQLException { service.update(d); }
    public void delete(int id) throws SQLException { service.delete(id); }

    public Object[][] rows(List<Department> list) {
        Object[][] data = new Object[list.size()][COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            Department d = list.get(i);
            data[i][0] = d.getDeptId();
            data[i][1] = d.getDeptCode();
            data[i][2] = d.getDeptName();
            data[i][3] = d.getHodName();
            data[i][4] = d.getCourseCount();
            data[i][5] = d.getStudentCount();
        }
        return data;
    }

    public TableData toTable(List<Department> list) {
        TableData t = new TableData();
        for (String c : COLUMNS) t.getHeaders().add(c);
        for (Department d : list) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(d.getDeptId()));
            row.add(d.getDeptCode());
            row.add(d.getDeptName());
            row.add(d.getHodName());
            row.add(String.valueOf(d.getCourseCount()));
            row.add(String.valueOf(d.getStudentCount()));
            t.addRow(row);
        }
        return t;
    }
}
