package service;

import dao.CourseDAO;
import dao.EnrollmentDAO;
import model.Course;
import model.Enrollment;
import utils.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Business rules for courses and student assignment. */
public class CourseService {

    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public List<Course> findAll() throws SQLException { return courseDAO.findAll(); }

    public List<Course> findByDepartment(int deptId) throws SQLException {
        return courseDAO.findByDepartment(deptId);
    }

    public List<Enrollment> enrolledStudents(int courseId) throws SQLException {
        return enrollmentDAO.findByCourse(courseId);
    }

    private void validate(Course c) {
        ValidationUtil.requireText(c.getCourseCode(), "Course code");
        if (!ValidationUtil.isValidCourseCode(c.getCourseCode())) {
            throw new IllegalArgumentException(
                "Course code may only contain letters, digits and hyphens (2-15 characters).");
        }
        ValidationUtil.requireLength(c.getCourseName(), "Course name", 100);
        if (c.getDeptId() <= 0) {
            throw new IllegalArgumentException("Please select a department.");
        }
        if (c.getDurationYears() < 1 || c.getDurationYears() > 6) {
            throw new IllegalArgumentException("Duration must be between 1 and 6 years.");
        }
        if (c.getCourseFee() < 0) {
            throw new IllegalArgumentException("Course fee cannot be negative.");
        }
        if (c.getDescription() != null && c.getDescription().length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters.");
        }
    }

    public Course create(Course c) throws SQLException {
        validate(c);
        if (courseDAO.codeExists(c.getCourseCode(), 0)) {
            throw new IllegalArgumentException(
                "Course code " + c.getCourseCode() + " already exists.");
        }
        int id = courseDAO.insert(c);
        if (id <= 0) throw new SQLException("Could not create the course.");
        return c;
    }

    public void update(Course c) throws SQLException {
        validate(c);
        if (courseDAO.codeExists(c.getCourseCode(), c.getCourseId())) {
            throw new IllegalArgumentException(
                "Course code " + c.getCourseCode() + " belongs to another course.");
        }
        if (!courseDAO.update(c)) {
            throw new IllegalArgumentException("Update failed - the course no longer exists.");
        }
    }

    public void delete(int courseId) throws SQLException {
        courseDAO.delete(courseId); // DAO throws when students are attached
    }

    /** Assigns a student to a course; returns false when already assigned. */
    public boolean assignStudent(int studentId, int courseId) throws SQLException {
        if (studentId <= 0) throw new IllegalArgumentException("Please select a student.");
        if (courseId <= 0) throw new IllegalArgumentException("Please select a course.");
        return enrollmentDAO.enroll(studentId, courseId, LocalDate.now(), "ACTIVE");
    }

    public boolean removeEnrollment(int enrollmentId) throws SQLException {
        return enrollmentDAO.delete(enrollmentId);
    }
}
