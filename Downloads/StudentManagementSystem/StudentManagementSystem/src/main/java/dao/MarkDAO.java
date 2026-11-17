package dao;

import database.DBConnection;
import model.Mark;
import model.StudentResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Data access for the marks table plus result aggregation. */
public class MarkDAO {

    private static final String SELECT =
        "SELECT m.mark_id, m.exam_id, m.student_id, m.marks_obtained, "
      + "       e.exam_name, e.max_marks, s.student_code, s.full_name "
      + "FROM marks m "
      + "JOIN examinations e ON e.exam_id = m.exam_id "
      + "JOIN students     s ON s.student_id = m.student_id ";

    public List<Mark> findByExam(int examId) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "WHERE m.exam_id = ? ORDER BY s.student_code")) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Mark> findByStudent(int studentId) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "WHERE m.student_id = ? ORDER BY e.exam_date")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Inserts or updates a mark (unique key exam_id + student_id). */
    public void save(int examId, int studentId, double marks) throws SQLException {
        String sql = "INSERT INTO marks (exam_id, student_id, marks_obtained) VALUES (?,?,?) "
                   + "ON DUPLICATE KEY UPDATE marks_obtained = VALUES(marks_obtained)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.setInt(2, studentId);
            ps.setDouble(3, marks);
            ps.executeUpdate();
        }
    }

    public boolean delete(int markId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM marks WHERE mark_id=?")) {
            ps.setInt(1, markId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Aggregate result per student (total max, total obtained -> % and grade). */
    public List<StudentResult> resultSheet(int courseId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT s.student_id, s.student_code, s.full_name, c.course_name, "
          + " COUNT(m.mark_id) AS exam_count, "
          + " COALESCE(SUM(e.max_marks),0)    AS total_max, "
          + " COALESCE(SUM(m.marks_obtained),0) AS total_obtained "
          + "FROM students s "
          + "JOIN courses c ON c.course_id = s.course_id "
          + "LEFT JOIN marks m ON m.student_id = s.student_id "
          + "LEFT JOIN examinations e ON e.exam_id = m.exam_id ");
        if (courseId > 0) sql.append("WHERE s.course_id = ? ");
        sql.append("GROUP BY s.student_id, s.student_code, s.full_name, c.course_name "
                 + "ORDER BY total_obtained DESC");

        List<StudentResult> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            if (courseId > 0) ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudentResult r = new StudentResult();
                    r.setStudentId(rs.getInt("student_id"));
                    r.setStudentCode(rs.getString("student_code"));
                    r.setStudentName(rs.getString("full_name"));
                    r.setCourseName(rs.getString("course_name"));
                    r.setExamCount(rs.getInt("exam_count"));
                    r.setTotalMax(rs.getDouble("total_max"));
                    r.setTotalObtained(rs.getDouble("total_obtained"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    /** Overall class average percentage for a course (0 means all courses). */
    public double averagePercentage(int courseId) throws SQLException {
        List<StudentResult> results = resultSheet(courseId);
        double sum = 0;
        int n = 0;
        for (StudentResult r : results) {
            if (r.getTotalMax() > 0) {
                sum += r.getPercentage();
                n++;
            }
        }
        return n == 0 ? 0.0 : Math.round(sum / n * 100.0) / 100.0;
    }

    private Mark map(ResultSet rs) throws SQLException {
        Mark m = new Mark();
        m.setMarkId(rs.getInt("mark_id"));
        m.setExamId(rs.getInt("exam_id"));
        m.setStudentId(rs.getInt("student_id"));
        m.setMarksObtained(rs.getDouble("marks_obtained"));
        m.setExamName(rs.getString("exam_name"));
        m.setMaxMarks(rs.getDouble("max_marks"));
        m.setStudentCode(rs.getString("student_code"));
        m.setStudentName(rs.getString("full_name"));
        return m;
    }
}
