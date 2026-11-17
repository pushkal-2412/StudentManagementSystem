package dao;

import database.DBConnection;
import model.Examination;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for the examinations table. */
public class ExaminationDAO {

    private static final String SELECT =
        "SELECT e.exam_id, e.exam_name, e.course_id, e.exam_date, e.max_marks, e.exam_type, "
      + "       c.course_name "
      + "FROM examinations e JOIN courses c ON c.course_id = e.course_id ";

    public List<Examination> findAll() throws SQLException {
        List<Examination> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "ORDER BY e.exam_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Examination> findByCourse(int courseId) throws SQLException {
        List<Examination> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "WHERE e.course_id = ? ORDER BY e.exam_date")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Examination> findById(int examId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "WHERE e.exam_id = ?")) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public int insert(Examination e) throws SQLException {
        String sql = "INSERT INTO examinations (exam_name, course_id, exam_date, max_marks, exam_type) "
                   + "VALUES (?,?,?,?,?)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    e.setExamId(k.getInt(1));
                    return e.getExamId();
                }
            }
        }
        return -1;
    }

    public boolean update(Examination e) throws SQLException {
        String sql = "UPDATE examinations SET exam_name=?, course_id=?, exam_date=?, "
                   + "max_marks=?, exam_type=? WHERE exam_id=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            bind(ps, e);
            ps.setInt(6, e.getExamId());
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Examination e) throws SQLException {
        ps.setString(1, e.getExamName().trim());
        ps.setInt(2, e.getCourseId());
        ps.setDate(3, Date.valueOf(e.getExamDate()));
        ps.setDouble(4, e.getMaxMarks());
        ps.setString(5, e.getExamType());
    }

    /** Exam deletion cascades to the marks entered for it. */
    public boolean delete(int examId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM examinations WHERE exam_id=?")) {
            ps.setInt(1, examId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean exists(String examName, int courseId, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM examinations WHERE exam_name=? AND course_id=? AND exam_id<>?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, examName.trim());
            ps.setInt(2, courseId);
            ps.setInt(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int count() throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM examinations");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Examination map(ResultSet rs) throws SQLException {
        Examination e = new Examination();
        e.setExamId(rs.getInt("exam_id"));
        e.setExamName(rs.getString("exam_name"));
        e.setCourseId(rs.getInt("course_id"));
        Date d = rs.getDate("exam_date");
        e.setExamDate(d == null ? null : d.toLocalDate());
        e.setMaxMarks(rs.getDouble("max_marks"));
        e.setExamType(rs.getString("exam_type"));
        e.setCourseName(rs.getString("course_name"));
        return e;
    }

    /** Convenience helper used by the marks screen default date. */
    public static LocalDate today() { return LocalDate.now(); }
}
