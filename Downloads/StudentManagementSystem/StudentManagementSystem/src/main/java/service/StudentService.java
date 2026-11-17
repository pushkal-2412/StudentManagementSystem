package service;

import dao.CourseDAO;
import dao.DepartmentDAO;
import dao.StudentDAO;
import database.DBConnection;
import model.Course;
import model.Department;
import model.Student;
import utils.DateUtil;
import utils.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Business rules for students: validation, duplicate checks, transactional create. */
public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    public List<Student> search(String keyword, int deptId, int courseId) throws SQLException {
        return studentDAO.search(keyword, deptId, courseId);
    }

    public List<Student> findAll() throws SQLException { return studentDAO.findAll(); }

    public Optional<Student> findById(int id) throws SQLException { return studentDAO.findById(id); }

    public List<Department> departments() throws SQLException { return departmentDAO.findAll(); }

    public List<Course> courses() throws SQLException { return courseDAO.findAll(); }

    public String suggestStudentCode() throws SQLException {
        return studentDAO.nextStudentCode(LocalDate.now().getYear());
    }

    /** Full validation shared by create and update. */
    private void validate(Student s) {
        // The student code is generated automatically when the caller leaves it blank.
        if (s.getStudentCode() != null && !s.getStudentCode().trim().isEmpty()) {
            ValidationUtil.requireLength(s.getStudentCode(), "Student ID", 20);
            if (!ValidationUtil.isValidStudentCode(s.getStudentCode())) {
                throw new IllegalArgumentException(
                    "Student ID may only contain letters and digits (3-20 characters).");
            }
        }
        ValidationUtil.requireLength(s.getFullName(), "Full name", 100);
        if (s.getFullName().trim().length() < 3) {
            throw new IllegalArgumentException("Full name must be at least 3 characters.");
        }
        if (s.getDob() == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (!s.getDob().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must be in the past.");
        }
        if (s.getDob().isAfter(LocalDate.now().minusYears(14))) {
            throw new IllegalArgumentException("Student must be at least 14 years old.");
        }
        ValidationUtil.requireText(s.getGender(), "Gender");
        ValidationUtil.requireEmail(s.getEmail());
        ValidationUtil.requirePhone(s.getPhone());
        ValidationUtil.requireLength(s.getAddress(), "Address", 255);
        if (s.getDeptId() <= 0) {
            throw new IllegalArgumentException("Please select a department.");
        }
        if (s.getCourseId() <= 0) {
            throw new IllegalArgumentException("Please select a course.");
        }
        ValidationUtil.requireText(s.getAcademicYear(), "Academic year");
        if (!s.getAcademicYear().matches("\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException("Academic year must look like 2026-2027.");
        }
        if (s.getAdmissionDate() == null) {
            throw new IllegalArgumentException("Admission date is required.");
        }
        ValidationUtil.requireNotFuture(s.getAdmissionDate(), "Admission date");
        if (s.getAdmissionDate().isBefore(s.getDob())) {
            throw new IllegalArgumentException("Admission date cannot be before the date of birth.");
        }
        if (s.getSemester() < 1 || s.getSemester() > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
    }

    /**
     * Creates the student, the enrollment row and the fee account in ONE transaction
     * so a partial record can never exist.
     */
    public Student create(Student s) throws SQLException {
        // Auto-generate the student ID (e.g. STU2026007) when none was supplied.
        if (s.getStudentCode() == null || s.getStudentCode().trim().isEmpty()) {
            int admissionYear = s.getAdmissionDate() != null
                    ? s.getAdmissionDate().getYear() : LocalDate.now().getYear();
            String code = studentDAO.nextStudentCode(admissionYear);
            while (studentDAO.codeExists(code, 0)) {
                code = studentDAO.nextStudentCode(admissionYear);
            }
            s.setStudentCode(code);
        } else {
            s.setStudentCode(s.getStudentCode().trim().toUpperCase());
        }
        validate(s);
        if (studentDAO.codeExists(s.getStudentCode(), 0)) {
            throw new IllegalArgumentException(
                "Student ID " + s.getStudentCode() + " already exists.");
        }
        if (studentDAO.emailExists(s.getEmail(), 0)) {
            throw new IllegalArgumentException(
                "The email " + s.getEmail() + " is already registered to another student.");
        }

        Connection cn = null;
        try {
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);

            int id = studentDAO.insert(cn, s);
            if (id <= 0) throw new SQLException("Insert failed - no generated key returned.");

            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO enrollments (student_id, course_id, enroll_date, status) "
                  + "VALUES (?,?,?,'ACTIVE') "
                  + "ON DUPLICATE KEY UPDATE status = VALUES(status)")) {
                ps.setInt(1, id);
                ps.setInt(2, s.getCourseId());
                ps.setDate(3, java.sql.Date.valueOf(s.getAdmissionDate()));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO fees (student_id, total_fee) "
                  + "SELECT ?, course_fee FROM courses WHERE course_id = ? "
                  + "ON DUPLICATE KEY UPDATE total_fee = VALUES(total_fee)")) {
                ps.setInt(1, id);
                ps.setInt(2, s.getCourseId());
                ps.executeUpdate();
            }

            cn.commit();
            return studentDAO.findById(id).orElse(s);
        } catch (SQLException ex) {
            if (cn != null) { try { cn.rollback(); } catch (SQLException ignored) { } }
            throw ex;
        } finally {
            if (cn != null) {
                try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignored) { }
            }
        }
    }

    public void update(Student s) throws SQLException {
        validate(s);
        if (studentDAO.codeExists(s.getStudentCode(), s.getStudentId())) {
            throw new IllegalArgumentException(
                "Student ID " + s.getStudentCode() + " already belongs to another student.");
        }
        if (studentDAO.emailExists(s.getEmail(), s.getStudentId())) {
            throw new IllegalArgumentException(
                "The email " + s.getEmail() + " is already registered to another student.");
        }
        Student existing = studentDAO.findById(s.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student record not found."));

        if (!studentDAO.update(s)) {
            throw new IllegalArgumentException("Update failed - the record no longer exists.");
        }

        // keep enrollment in sync when the course changed
        if (existing.getCourseId() != s.getCourseId()) {
            try (Connection cn = DBConnection.getConnection();
                 PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO enrollments (student_id, course_id, enroll_date, status) "
                   + "VALUES (?,?,?,'ACTIVE') "
                   + "ON DUPLICATE KEY UPDATE status = VALUES(status)")) {
                ps.setInt(1, s.getStudentId());
                ps.setInt(2, s.getCourseId());
                ps.setDate(3, java.sql.Date.valueOf(s.getAdmissionDate()));
                ps.executeUpdate();
            }
        }
    }

    /** Deleting a student cascades to attendance, marks, fee and payments. */
    public void delete(int studentId) throws SQLException {
        Student s = studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student record not found."));
        if (!studentDAO.delete(studentId)) {
            throw new IllegalArgumentException("Delete failed for " + s.getStudentCode() + ".");
        }
    }

    public int count() throws SQLException { return studentDAO.count(); }

    /** Builds a formatted single-line summary used in confirmation dialogs. */
    public static String describe(Student s) {
        return s.getStudentCode() + " \u2013 " + s.getFullName()
             + "\nCourse: " + s.getCourseName()
             + "\nAdmitted: " + DateUtil.format(s.getAdmissionDate());
    }
}
