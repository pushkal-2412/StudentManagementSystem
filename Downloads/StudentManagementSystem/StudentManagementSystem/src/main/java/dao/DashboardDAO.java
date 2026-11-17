package dao;

import database.DBConnection;
import model.ChartRow;
import model.DashboardStats;

import java.sql.*;

/** Aggregated KPI queries for the dashboard. */
public class DashboardDAO {

    public DashboardStats load() throws SQLException {
        DashboardStats st = new DashboardStats();
        try (Connection cn = DBConnection.getConnection()) {
            st.setTotalStudents(scalar(cn, "SELECT COUNT(*) FROM students"));
            st.setTotalCourses(scalar(cn, "SELECT COUNT(*) FROM courses"));
            st.setTotalDepartments(scalar(cn, "SELECT COUNT(*) FROM departments"));
            st.setTotalEnrollments(scalar(cn, "SELECT COUNT(*) FROM enrollments"));
            st.setMaleStudents(scalar(cn, "SELECT COUNT(*) FROM students WHERE gender='Male'"));
            st.setFemaleStudents(scalar(cn, "SELECT COUNT(*) FROM students WHERE gender='Female'"));

            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT COALESCE(SUM(paid_amount),0) paid, "
                  + "       COALESCE(SUM(remaining_amount),0) pending FROM fees");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    st.setFeeCollected(rs.getDouble("paid"));
                    st.setFeePending(rs.getDouble("pending"));
                }
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT COUNT(*) t, COALESCE(SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END),0) p FROM attendance");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("t") > 0) {
                    st.setAverageAttendance(
                        Math.round(rs.getInt("p") * 10000.0 / rs.getInt("t")) / 100.0);
                }
            }

            st.setRecentStudents(new StudentDAO().findRecent(6));

            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT d.dept_code, COUNT(s.student_id) n FROM departments d "
                  + "LEFT JOIN students s ON s.dept_id = d.dept_id "
                  + "GROUP BY d.dept_id, d.dept_code ORDER BY n DESC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    st.getDepartmentWise().add(
                        new ChartRow(rs.getString(1), rs.getDouble(2)));
                }
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT c.course_code, COUNT(s.student_id) n FROM courses c "
                  + "LEFT JOIN students s ON s.course_id = c.course_id "
                  + "GROUP BY c.course_id, c.course_code ORDER BY n DESC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    st.getCourseWise().add(new ChartRow(rs.getString(1), rs.getDouble(2)));
                }
            }
        }
        return st;
    }

    private int scalar(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
