package service;

import dao.DepartmentDAO;
import model.Department;
import utils.ValidationUtil;

import java.sql.SQLException;
import java.util.List;

/** Business rules for departments. */
public class DepartmentService {

    private final DepartmentDAO dao = new DepartmentDAO();

    public List<Department> findAll() throws SQLException { return dao.findAll(); }

    private void validate(Department d) {
        ValidationUtil.requireText(d.getDeptCode(), "Department code");
        if (!d.getDeptCode().matches("^[A-Za-z]{2,10}$")) {
            throw new IllegalArgumentException("Department code must be 2-10 letters, e.g. CSE.");
        }
        ValidationUtil.requireLength(d.getDeptName(), "Department name", 100);
        ValidationUtil.requireLength(d.getHodName(), "Head of department", 100);
    }

    public Department create(Department d) throws SQLException {
        validate(d);
        if (dao.codeExists(d.getDeptCode(), 0)) {
            throw new IllegalArgumentException(
                "Department code " + d.getDeptCode().toUpperCase() + " already exists.");
        }
        if (dao.nameExists(d.getDeptName(), 0)) {
            throw new IllegalArgumentException("That department name already exists.");
        }
        int id = dao.insert(d);
        if (id <= 0) throw new SQLException("Could not create the department.");
        return d;
    }

    public void update(Department d) throws SQLException {
        validate(d);
        if (dao.codeExists(d.getDeptCode(), d.getDeptId())) {
            throw new IllegalArgumentException("That department code belongs to another department.");
        }
        if (dao.nameExists(d.getDeptName(), d.getDeptId())) {
            throw new IllegalArgumentException("That department name belongs to another department.");
        }
        if (!dao.update(d)) {
            throw new IllegalArgumentException("Update failed - the department no longer exists.");
        }
    }

    public void delete(int deptId) throws SQLException { dao.delete(deptId); }
}
