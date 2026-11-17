package dao;

import database.DBConnection;
import model.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access and business queries for the courses table. */
public class CourseDAO {

    private static final String SELECT =
        "SELECT c.course_id, c.course_code, c.course_name, c.dept_id, c.duration_years, "
      + "       c.course_fee, c.description, d.dept_name, "
      + " (SELECT COUNT(*) FROM students s WHERE s.course_id = c.course_id) AS student_count "
      + "FROM courses c JOIN departments d ON d.dept_id = c.dept_id ";

    public List<Course> findAll() throws SQLException {
        return query(SELECT + "ORDER BY c.course_code", null, 0);
    }

    public List<Course> findByDepartment(int deptId) throws SQLException {
        return query(SELECT + "WHERE c.dept_id = ? ORDER BY c.course_code", null, deptId);
    }

    public Optional<Course> findById(int courseId) throws SQLException {
        List<Course> list = query(SELECT + "WHERE c.course_id = ?", null, courseId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Loads a joined course row, used when building fee accounts and exams. */
    public Optional<Course> findByStudentId(int studentId) throws SQLException {
        String sql = SELECT + "JOIN students s2 ON s2.course_id = c.course_id WHERE s2.student_id = ?";
        List<Course> list = query(sql, null, studentId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private List<Course> query(String sql, String keyword, int param) throws SQLException {
        List<Course> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param > 0) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public int insert(Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_code, course_name, dept_id, duration_years, "
                   + "course_fee, description) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, course);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    course.setCourseId(k.getInt(1));
                    return course.getCourseId();
                }
            }
        }
        return -1;
    }

    public boolean update(Course course) throws SQLException {
        String sql = "UPDATE courses SET course_code=?, course_name=?, dept_id=?, duration_years=?, "
                   + "course_fee=?, description=? WHERE course_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, course);
            ps.setInt(7, course.getCourseId());
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Course course) throws SQLException {
        ps.setString(1, course.getCourseCode().trim().toUpperCase());
        ps.setString(2, course.getCourseName().trim());
        ps.setInt(3, course.getDeptId());
        ps.setInt(4, course.getDurationYears());
        ps.setDouble(5, course.getCourseFee());
        ps.setString(6, course.getDescription());
    }

    /** Refuses deletion while students are still attached to the course. */
    public void delete(int courseId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) AS n FROM students WHERE course_id = ?")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("n") > 0) {
                    throw new SQLException("Students are enrolled in this course. "
                        + "Move or remove them before deleting the course.");
                }
            }
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM courses WHERE course_id=?")) {
            ps.setInt(1, courseId);
            ps.executeUpdate();
        }
    }

    public boolean codeExists(String code, int excludeId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM courses WHERE course_code = ? AND course_id <> ?")) {
            ps.setString(1, code.trim().toUpperCase());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private Course map(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setCourseId(rs.getInt("course_id"));
        c.setCourseCode(rs.getString("course_code"));
        c.setCourseName(rs.getString("course_name"));
        c.setDeptId(rs.getInt("dept_id"));
        c.setDeptName(rs.getString("dept_name"));
        c.setDurationYears(rs.getInt("duration_years"));
        c.setCourseFee(rs.getDouble("course_fee"));
        c.setDescription(rs.getString("description"));
        c.setStudentCount(rs.getInt("student_count"));
        return c;
    }
}
