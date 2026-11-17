package dao;

import database.DBConnection;
import model.Student;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for the students table, including search and filters. */
public class StudentDAO {

    private static final String SELECT =
        "SELECT s.student_id, s.student_code, s.full_name, s.dob, s.gender, s.email, s.phone, "
      + "       s.address, s.dept_id, s.course_id, s.academic_year, s.admission_date, "
      + "       s.semester, s.created_at, d.dept_name, c.course_name "
      + "FROM students s "
      + "JOIN departments d ON d.dept_id = s.dept_id "
      + "JOIN courses c ON c.course_id = s.course_id ";

    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "ORDER BY s.student_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Search by name / code / email and optionally filter by department and course.
     * A null or non-positive filter means "all".
     */
    public List<Student> search(String keyword, int deptId, int courseId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (s.full_name LIKE ? OR s.student_code LIKE ? OR s.email LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (deptId > 0) {
            sql.append("AND s.dept_id = ? ");
            params.add(deptId);
        }
        if (courseId > 0) {
            sql.append("AND s.course_id = ? ");
            params.add(courseId);
        }
        sql.append("ORDER BY s.student_id");

        List<Student> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Student> findById(int studentId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "WHERE s.student_id = ?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Student> findByCode(String code) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "WHERE s.student_code = ?")) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /** Inserts inside the caller's transaction. */
    public int insert(Connection cn, Student s) throws SQLException {
        String sql = "INSERT INTO students (student_code, full_name, dob, gender, email, phone, "
                   + "address, dept_id, course_id, academic_year, admission_date, semester) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, s);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    s.setStudentId(k.getInt(1));
                    return s.getStudentId();
                }
            }
        }
        return -1;
    }

    public boolean update(Student s) throws SQLException {
        String sql = "UPDATE students SET student_code=?, full_name=?, dob=?, gender=?, email=?, "
                   + "phone=?, address=?, dept_id=?, course_id=?, academic_year=?, "
                   + "admission_date=?, semester=? WHERE student_id=?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            bind(ps, s);
            ps.setInt(13, s.getStudentId());
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Student s) throws SQLException {
        ps.setString(1, s.getStudentCode().trim().toUpperCase());
        ps.setString(2, s.getFullName().trim());
        ps.setDate(3, Date.valueOf(s.getDob()));
        ps.setString(4, s.getGender());
        ps.setString(5, s.getEmail().trim().toLowerCase());
        ps.setString(6, s.getPhone().trim());
        ps.setString(7, s.getAddress());
        ps.setInt(8, s.getDeptId());
        ps.setInt(9, s.getCourseId());
        ps.setString(10, s.getAcademicYear().trim());
        ps.setDate(11, Date.valueOf(s.getAdmissionDate()));
        ps.setInt(12, s.getSemester());
    }

    public boolean delete(int studentId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM students WHERE student_id=?")) {
            ps.setInt(1, studentId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean emailExists(String email, int excludeId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT 1 FROM students WHERE email = ? AND student_id <> ?")) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean codeExists(String code, int excludeId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT 1 FROM students WHERE student_code = ? AND student_id <> ?")) {
            ps.setString(1, code.trim().toUpperCase());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Generates the next sequential code for an admission year, e.g. STU2026007. */
    public String nextStudentCode(int year) throws SQLException {
        String prefix = "STU" + year;
        String sql = "SELECT student_code FROM students WHERE student_code LIKE ? "
                   + "ORDER BY student_code DESC LIMIT 1";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString(1);
                    String tail = last.substring(prefix.length());
                    try {
                        return prefix + String.format("%03d", Integer.parseInt(tail) + 1);
                    } catch (NumberFormatException ignored) {
                        // fall through to the default below
                    }
                }
            }
        }
        return prefix + "001";
    }

    public List<Student> findRecent(int limit) throws SQLException {
        List<Student> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 SELECT + "ORDER BY s.created_at DESC, s.student_id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public int count() throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM students");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setStudentCode(rs.getString("student_code"));
        s.setFullName(rs.getString("full_name"));
        Date dob = rs.getDate("dob");
        s.setDob(dob == null ? null : dob.toLocalDate());
        s.setGender(rs.getString("gender"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setDeptId(rs.getInt("dept_id"));
        s.setDeptName(rs.getString("dept_name"));
        s.setCourseId(rs.getInt("course_id"));
        s.setCourseName(rs.getString("course_name"));
        s.setAcademicYear(rs.getString("academic_year"));
        Date adm = rs.getDate("admission_date");
        s.setAdmissionDate(adm == null ? null : adm.toLocalDate());
        s.setSemester(rs.getInt("semester"));
        Timestamp ts = rs.getTimestamp("created_at");
        s.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return s;
    }

    /** Convenience for reports: safe LocalDate helper. */
    public static LocalDate toLocalDate(Date d) { return d == null ? null : d.toLocalDate(); }
}
