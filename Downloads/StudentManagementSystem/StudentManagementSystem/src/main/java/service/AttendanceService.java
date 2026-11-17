package service;

import dao.AttendanceDAO;
import dao.StudentDAO;
import model.Attendance;
import model.AttendanceSummary;
import model.Student;
import utils.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Attendance rules: marking, percentages and monthly reports. */
public class AttendanceService {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    /** Marks one student for the given date and course. */
    public void mark(int studentId, int courseId, LocalDate date, String status, int adminId)
            throws SQLException {
        markInternal(studentId, courseId, date, status, adminId);
    }

    private void markInternal(int studentId, int courseId, LocalDate date, String status,
                              int adminId) throws SQLException {
        if (studentId <= 0) throw new IllegalArgumentException("Please select a student.");
        if (courseId <= 0) throw new IllegalArgumentException("Please select a course.");
        if (date == null) throw new IllegalArgumentException("Please choose a date.");
        ValidationUtil.requireNotFuture(date, "Attendance date");
        if (!"PRESENT".equals(status) && !"ABSENT".equals(status) && !"LATE".equals(status)) {
            throw new IllegalArgumentException("Status must be PRESENT, ABSENT or LATE.");
        }
        attendanceDAO.mark(studentId, courseId, date, status, adminId);
    }

    /** Marks every supplied student at once, in a single transaction. */
    public int markAll(List<int[]> pairs, LocalDate date, String status, int adminId)
            throws SQLException {
        if (pairs == null || pairs.isEmpty()) {
            throw new IllegalArgumentException("There are no students to mark.");
        }
        ValidationUtil.requireNotFuture(date, "Attendance date");
        return attendanceDAO.markBatch(pairs, date, status, adminId);
    }

    public List<Attendance> byDate(LocalDate date, int courseId) throws SQLException {
        return attendanceDAO.findByDate(date, courseId);
    }

    public List<Attendance> byStudent(int studentId) throws SQLException {
        return attendanceDAO.findByStudent(studentId);
    }

    public List<AttendanceSummary> monthly(YearMonth month, int courseId) throws SQLException {
        if (month == null) throw new IllegalArgumentException("Please choose a month.");
        return attendanceDAO.monthlySummary(month, courseId);
    }

    public double percentageForStudent(int studentId) throws SQLException {
        return attendanceDAO.percentageForStudent(studentId);
    }

    /** Students of a course, used to build the marking sheet. */
    public List<Student> studentsOfCourse(int courseId) throws SQLException {
        return studentDAO.search(null, 0, courseId);
    }

    public Optional<Student> student(int id) throws SQLException { return studentDAO.findById(id); }

    /** Pairs of [studentId, courseId] for the "mark all" action. */
    public List<int[]> pairsFor(List<Student> students, int courseId) {
        List<int[]> pairs = new ArrayList<>();
        for (Student s : students) {
            int cid = courseId > 0 ? courseId : s.getCourseId();
            pairs.add(new int[]{s.getStudentId(), cid});
        }
        return pairs;
    }
}
