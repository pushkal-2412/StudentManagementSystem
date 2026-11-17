package controller;

import model.ChartRow;
import model.Course;
import model.Examination;
import model.Mark;
import model.Student;
import model.StudentResult;
import model.TableData;
import service.ExamService;
import utils.DateUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Mediates between the examination views and the service layer. */
public class ExamController {

    private final ExamService service = new ExamService();

    public static final String[] EXAM_COLUMNS = {
        "Exam ID", "Examination", "Course", "Date", "Max Marks", "Type"
    };
    public static final String[] MARK_COLUMNS = {
        "Mark ID", "Student ID", "Student Name", "Marks", "Max", "Percentage", "Grade"
    };
    public static final String[] RESULT_COLUMNS = {
        "Student ID", "Student Name", "Course", "Exams", "Total", "Max", "%", "Grade", "Result"
    };

    public List<Examination> exams() throws SQLException { return service.exams(); }
    public List<Course> courses() throws SQLException { return service.courses(); }
    public List<Student> studentsOfCourse(int courseId) throws SQLException {
        return service.studentsOfCourse(courseId);
    }
    public List<Mark> marksOfExam(int examId) throws SQLException {
        return service.marksOfExam(examId);
    }
    public List<Mark> marksOfStudent(int studentId) throws SQLException {
        return service.marksOfStudent(studentId);
    }
    public List<StudentResult> results(int courseId) throws SQLException {
        return service.results(courseId);
    }
    public Examination createExam(Examination e) throws SQLException {
        return service.createExam(e);
    }
    public void updateExam(Examination e) throws SQLException { service.updateExam(e); }
    public void deleteExam(int id) throws SQLException { service.deleteExam(id); }
    public void saveMark(int examId, int studentId, double marks) throws SQLException {
        service.saveMark(examId, studentId, marks);
    }
    public double maxMarks(int examId) throws SQLException { return service.maxMarksOf(examId); }
    public double classAverage(int courseId) throws SQLException {
        return service.classAverage(courseId);
    }
    public String gradeFor(double percentage) { return ExamService.gradeFor(percentage); }

    public Object[][] examRows(List<Examination> list) {
        Object[][] data = new Object[list.size()][EXAM_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            Examination e = list.get(i);
            data[i][0] = e.getExamId();
            data[i][1] = e.getExamName();
            data[i][2] = e.getCourseName();
            data[i][3] = DateUtil.format(e.getExamDate());
            data[i][4] = e.getMaxMarks();
            data[i][5] = e.getExamType();
        }
        return data;
    }

    public Object[][] markRows(List<Mark> list) {
        Object[][] data = new Object[list.size()][MARK_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            Mark m = list.get(i);
            double pct = m.getMaxMarks() > 0 ? m.getMarksObtained() * 100 / m.getMaxMarks() : 0;
            data[i][0] = m.getMarkId();
            data[i][1] = m.getStudentCode();
            data[i][2] = m.getStudentName();
            data[i][3] = m.getMarksObtained();
            data[i][4] = m.getMaxMarks();
            data[i][5] = String.format("%.2f%%", pct);
            data[i][6] = gradeFor(pct);
        }
        return data;
    }

    public Object[][] resultRows(List<StudentResult> list) {
        Object[][] data = new Object[list.size()][RESULT_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            StudentResult r = list.get(i);
            data[i][0] = r.getStudentCode();
            data[i][1] = r.getStudentName();
            data[i][2] = r.getCourseName();
            data[i][3] = r.getExamCount();
            data[i][4] = r.getTotalObtained();
            data[i][5] = r.getTotalMax();
            data[i][6] = String.format("%.2f%%", r.getPercentage());
            data[i][7] = r.getGrade();
            data[i][8] = r.getResultStatus();
        }
        return data;
    }

    public TableData resultTable(List<StudentResult> list) {
        TableData t = new TableData();
        for (String c : RESULT_COLUMNS) t.getHeaders().add(c);
        for (StudentResult r : list) {
            List<String> row = new ArrayList<>();
            row.add(r.getStudentCode());
            row.add(r.getStudentName());
            row.add(r.getCourseName());
            row.add(String.valueOf(r.getExamCount()));
            row.add(String.format("%.2f", r.getTotalObtained()));
            row.add(String.format("%.2f", r.getTotalMax()));
            row.add(String.format("%.2f%%", r.getPercentage()));
            row.add(r.getGrade());
            row.add(r.getResultStatus());
            t.addRow(row);
        }
        return t;
    }

    public TableData markTable(List<Mark> list) {
        TableData t = new TableData();
        for (String c : MARK_COLUMNS) t.getHeaders().add(c);
        for (Mark m : list) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(m.getMarkId()));
            row.add(m.getStudentCode());
            row.add(m.getStudentName());
            row.add(String.format("%.2f", m.getMarksObtained()));
            row.add(String.format("%.2f", m.getMaxMarks()));
            double pct = m.getMaxMarks() > 0 ? m.getMarksObtained() * 100 / m.getMaxMarks() : 0;
            row.add(String.format("%.2f%%", pct));
            row.add(gradeFor(pct));
            t.addRow(row);
        }
        return t;
    }

    public List<ChartRow> gradeDistribution(int courseId) throws SQLException {
        List<ChartRow> rows = new ArrayList<>();
        String[] grades = {"A+", "A", "B", "C", "D", "E", "F"};
        int[] counts = new int[grades.length];
        for (StudentResult r : results(courseId)) {
            for (int i = 0; i < grades.length; i++) {
                if (grades[i].equals(r.getGrade())) { counts[i]++; break; }
            }
        }
        for (int i = 0; i < grades.length; i++) {
            rows.add(new ChartRow(grades[i], counts[i]));
        }
        return rows;
    }
}
