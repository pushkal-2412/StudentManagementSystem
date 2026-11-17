package dao;

import database.DBConnection;
import model.Enrollment;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data access for the enrollments table (student <-> course). */
public class EnrollmentDAO {

    private static final String SELECT =
        "SELECT e.enrollment_id, e.student_id, e.course_id, e.enroll_date, e.status, "
      + "       s.student_code, s.full_name, c.course_name "
      + "FROM enrollments e "
      + "JOIN students s ON s.student_id = e.student_id "
      + "JOIN courses  c ON c.course_id  = e.course_id ";

    public List<Enrollment> findAll() throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "ORDER BY e.enrollment_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Enrollment> findByCourse(int courseId) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "WHERE e.course_id = ? ORDER BY s.full_name")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Enrollment> findByStudent(int studentId) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "WHERE e.student_id = ? ORDER BY e.enrollment_id")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** @return false when the student is already enrolled in that course. */
    public boolean enroll(int studentId, int courseId, LocalDate date, String status)
            throws SQLException {
        if (exists(studentId, courseId)) {
            return false;
        }
        String sql = "INSERT INTO enrollments (student_id, course_id, enroll_date, status) "
                   + "VALUES (?,?,?,?)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, status);
            ps.executeUpdate();
            return true;
        }
    }

    public boolean exists(int studentId, int courseId) throws SQLException {
        String sql = "SELECT 1 FROM enrollments WHERE student_id=? AND course_id=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean updateStatus(int enrollmentId, String status) throws SQLException {
        String sql = "UPDATE enrollments SET status=? WHERE enrollment_id=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int enrollmentId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "DELETE FROM enrollments WHERE enrollment_id=?")) {
            ps.setInt(1, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM enrollments");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setEnrollmentId(rs.getInt("enrollment_id"));
        e.setStudentId(rs.getInt("student_id"));
        e.setCourseId(rs.getInt("course_id"));
        Date d = rs.getDate("enroll_date");
        e.setEnrollDate(d == null ? null : d.toLocalDate());
        e.setStatus(rs.getString("status"));
        e.setStudentCode(rs.getString("student_code"));
        e.setStudentName(rs.getString("full_name"));
        e.setCourseName(rs.getString("course_name"));
        return e;
    }
}
