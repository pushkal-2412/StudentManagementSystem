package service;

import dao.CourseDAO;
import dao.ExaminationDAO;
import dao.MarkDAO;
import dao.StudentDAO;
import model.Examination;
import model.Mark;
import model.Student;
import model.StudentResult;
import utils.ValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Examination and marks rules with grade computation. */
public class ExamService {

    private final ExaminationDAO examDAO = new ExaminationDAO();
    private final MarkDAO markDAO = new MarkDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    public List<Examination> exams() throws SQLException { return examDAO.findAll(); }
    public List<Examination> examsOfCourse(int courseId) throws SQLException {
        return examDAO.findByCourse(courseId);
    }
    public Optional<Examination> exam(int examId) throws SQLException {
        return examDAO.findById(examId);
    }
    public List<Mark> marksOfExam(int examId) throws SQLException {
        return markDAO.findByExam(examId);
    }
    public List<Mark> marksOfStudent(int studentId) throws SQLException {
        return markDAO.findByStudent(studentId);
    }
    public List<StudentResult> results(int courseId) throws SQLException {
        return markDAO.resultSheet(courseId);
    }
    public List<Student> studentsOfCourse(int courseId) throws SQLException {
        return studentDAO.search(null, 0, courseId);
    }
    public List<model.Course> courses() throws SQLException { return courseDAO.findAll(); }

    public Examination createExam(Examination e) throws SQLException {
        ValidationUtil.requireLength(e.getExamName(), "Examination name", 100);
        if (e.getCourseId() <= 0) {
            throw new IllegalArgumentException("Please select a course.");
        }
        if (e.getExamDate() == null) {
            throw new IllegalArgumentException("Examination date is required.");
        }
        if (e.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than zero.");
        }
        if (e.getMaxMarks() > 1000) {
            throw new IllegalArgumentException("Maximum marks looks unrealistic (limit 1000).");
        }
        ValidationUtil.requireText(e.getExamType(), "Examination type");
        if (examDAO.exists(e.getExamName(), e.getCourseId(), 0)) {
            throw new IllegalArgumentException(
                "An examination with this name already exists for the selected course.");
        }
        int id = examDAO.insert(e);
        if (id <= 0) throw new SQLException("Could not create the examination.");
        return e;
    }

    public void updateExam(Examination e) throws SQLException {
        if (e.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than zero.");
        }
        if (examDAO.exists(e.getExamName(), e.getCourseId(), e.getExamId())) {
            throw new IllegalArgumentException("Another examination already uses that name.");
        }
        if (!examDAO.update(e)) {
            throw new IllegalArgumentException("Update failed - the examination was removed.");
        }
    }

    public void deleteExam(int examId) throws SQLException {
        if (!examDAO.delete(examId)) {
            throw new IllegalArgumentException("Delete failed - the examination was removed.");
        }
    }

    /**
     * Saves a mark after validating 0 <= marks <= maxMarks.
     * The same rule is enforced by the UI spinner and the database CHECK constraint.
     */
    public void saveMark(int examId, int studentId, double marks) throws SQLException {
        Examination exam = examDAO.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found."));
        if (studentId <= 0) {
            throw new IllegalArgumentException("Please select a student.");
        }
        ValidationUtil.validateMark(marks, exam.getMaxMarks());
        markDAO.save(examId, studentId, ValidationUtil.round2(marks));
    }

    /** Default starting value for the marks spinner of an exam. */
    public double maxMarksOf(int examId) throws SQLException {
        return examDAO.findById(examId).map(exam -> exam.getMaxMarks()).orElse(100.0);
    }

    public double classAverage(int courseId) throws SQLException {
        return markDAO.averagePercentage(courseId);
    }

    public static String gradeFor(double percentage) {
        return StudentResult.gradeFor(percentage);
    }
}
