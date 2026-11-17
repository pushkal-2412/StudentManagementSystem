package dao;

import database.DBConnection;
import model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for the departments table. */
public class DepartmentDAO {

    private static final String BASE =
        "SELECT d.dept_id, d.dept_code, d.dept_name, d.hod_name, "
      + " (SELECT COUNT(*) FROM students s WHERE s.dept_id = d.dept_id) AS student_count, "
      + " (SELECT COUNT(*) FROM courses c WHERE c.dept_id = d.dept_id) AS course_count "
      + "FROM departments d ";

    public List<Department> findAll() throws SQLException {
        List<Department> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "ORDER BY d.dept_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Optional<Department> findById(int deptId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE d.dept_id = ?")) {
            ps.setInt(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public int insert(Department d) throws SQLException {
        String sql = "INSERT INTO departments (dept_code, dept_name, hod_name) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getDeptCode().trim().toUpperCase());
            ps.setString(2, d.getDeptName().trim());
            ps.setString(3, d.getHodName().trim());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    int id = k.getInt(1);
                    d.setDeptId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public boolean update(Department d) throws SQLException {
        String sql = "UPDATE departments SET dept_code=?, dept_name=?, hod_name=? WHERE dept_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getDeptCode().trim().toUpperCase());
            ps.setString(2, d.getDeptName().trim());
            ps.setString(3, d.getHodName().trim());
            ps.setInt(4, d.getDeptId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Refuses to delete a department that still has students or courses. */
    public void delete(int deptId) throws SQLException {
        String check = "SELECT (SELECT COUNT(*) FROM students WHERE dept_id=?) + "
                     + "(SELECT COUNT(*) FROM courses WHERE dept_id=?) AS used";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(check)) {
            ps.setInt(1, deptId);
            ps.setInt(2, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("used") > 0) {
                    throw new SQLException(
                        "This department is still linked to students or courses and cannot be deleted.");
                }
            }
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM departments WHERE dept_id=?")) {
            ps.setInt(1, deptId);
            ps.executeUpdate();
        }
    }

    public boolean codeExists(String code, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM departments WHERE dept_code = ? AND dept_id <> ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean nameExists(String name, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM departments WHERE dept_name = ? AND dept_id <> ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private Department map(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDeptId(rs.getInt("dept_id"));
        d.setDeptCode(rs.getString("dept_code"));
        d.setDeptName(rs.getString("dept_name"));
        d.setHodName(rs.getString("hod_name"));
        d.setStudentCount(rs.getInt("student_count"));
        d.setCourseCount(rs.getInt("course_count"));
        return d;
    }
}
