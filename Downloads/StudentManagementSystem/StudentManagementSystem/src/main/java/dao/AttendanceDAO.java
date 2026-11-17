package dao;

import database.DBConnection;
import model.Attendance;
import model.AttendanceSummary;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Data access and aggregation for the attendance table. */
public class AttendanceDAO {

    private static final String SELECT =
        "SELECT a.attendance_id, a.student_id, a.course_id, a.att_date, a.status, "
      + "       s.student_code, s.full_name, c.course_name "
      + "FROM attendance a "
      + "JOIN students s ON s.student_id = a.student_id "
      + "JOIN courses  c ON c.course_id  = a.course_id ";

    /**
     * Marks (or updates) attendance. Uses INSERT ... ON DUPLICATE KEY UPDATE so the
     * unique key (student, course, date) makes re-marking safe.
     */
    public void mark(int studentId, int courseId, LocalDate date, String status, int markedBy)
            throws SQLException {
        String sql = "INSERT INTO attendance (student_id, course_id, att_date, status, marked_by) "
                   + "VALUES (?,?,?,?,?) "
                   + "ON DUPLICATE KEY UPDATE status = VALUES(status), marked_by = VALUES(marked_by)";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, status);
            if (markedBy > 0) ps.setInt(5, markedBy); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    /** Batch mark using one transaction - used by the "Mark all present" action. */
    public int markBatch(List<int[]> studentCoursePairs, LocalDate date, String status,
                         int markedBy) throws SQLException {
        String sql = "INSERT INTO attendance (student_id, course_id, att_date, status, marked_by) "
                   + "VALUES (?,?,?,?,?) "
                   + "ON DUPLICATE KEY UPDATE status = VALUES(status)";
        Connection cn = null;
        try {
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);
            int rows = 0;
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                for (int[] pair : studentCoursePairs) {
                    ps.setInt(1, pair[0]);
                    ps.setInt(2, pair[1]);
                    ps.setDate(3, Date.valueOf(date));
                    ps.setString(4, status);
                    if (markedBy > 0) ps.setInt(5, markedBy); else ps.setNull(5, Types.INTEGER);
                    ps.addBatch();
                }
                for (int r : ps.executeBatch()) rows += Math.max(r, 0);
            }
            cn.commit();
            return rows;
        } catch (SQLException ex) {
            if (cn != null) {
                try { cn.rollback(); } catch (SQLException ignored) { }
            }
            throw ex;
        } finally {
            if (cn != null) {
                try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignored) { }
            }
        }
    }

    public List<Attendance> findByDate(LocalDate date, int courseId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE a.att_date = ? ");
        if (courseId > 0) sql.append("AND a.course_id = ? ");
        sql.append("ORDER BY s.student_code");
        List<Attendance> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            ps.setDate(1, Date.valueOf(date));
            if (courseId > 0) ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Attendance> findByStudent(int studentId) throws SQLException {
        List<Attendance> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "WHERE a.student_id = ? ORDER BY a.att_date DESC")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Aggregated percentage per student for a month, optionally per course. */
    public List<AttendanceSummary> monthlySummary(YearMonth month, int courseId)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT a.student_id, s.student_code, s.full_name, c.course_name, "
          + " SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count, "
          + " SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count, "
          + " SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_count, "
          + " COUNT(*) AS total_count "
          + "FROM attendance a "
          + "JOIN students s ON s.student_id = a.student_id "
          + "JOIN courses  c ON c.course_id  = a.course_id "
          + "WHERE a.att_date BETWEEN ? AND ? ");
        if (courseId > 0) sql.append("AND a.course_id = ? ");
        sql.append("GROUP BY a.student_id, s.student_code, s.full_name, c.course_name "
                 + "ORDER BY s.student_code");

        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<AttendanceSummary> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            if (courseId > 0) ps.setInt(3, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceSummary s = new AttendanceSummary();
                    s.setStudentId(rs.getInt("student_id"));
                    s.setStudentCode(rs.getString("student_code"));
                    s.setStudentName(rs.getString("full_name"));
                    s.setCourseName(rs.getString("course_name"));
                    s.setPresentCount(rs.getInt("present_count"));
                    s.setAbsentCount(rs.getInt("absent_count"));
                    s.setLateCount(rs.getInt("late_count"));
                    s.setTotalCount(rs.getInt("total_count"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    /** Percentage for one student across all recorded days. */
    public double percentageForStudent(int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) total, SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present "
                   + "FROM attendance WHERE student_id = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int present = rs.getInt("present");
                    if (total == 0) return 0.0;
                    return Math.round(present * 10000.0 / total) / 100.0;
                }
            }
        }
        return 0.0;
    }

    /** Average attendance across the whole institution (dashboard KPI). */
    public double overallPercentage() throws SQLException {
        String sql = "SELECT COUNT(*) total, SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present FROM attendance";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int total = rs.getInt("total");
                if (total == 0) return 0.0;
                return Math.round(rs.getInt("present") * 10000.0 / total) / 100.0;
            }
        }
        return 0.0;
    }

    public boolean delete(int attendanceId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "DELETE FROM attendance WHERE attendance_id=?")) {
            ps.setInt(1, attendanceId);
            return ps.executeUpdate() > 0;
        }
    }

    private Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setAttendanceId(rs.getInt("attendance_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setCourseId(rs.getInt("course_id"));
        Date d = rs.getDate("att_date");
        a.setAttDate(d == null ? null : d.toLocalDate());
        a.setStatus(rs.getString("status"));
        a.setStudentCode(rs.getString("student_code"));
        a.setStudentName(rs.getString("full_name"));
        a.setCourseName(rs.getString("course_name"));
        return a;
    }
}
