package controller;

import model.Course;
import model.Enrollment;
import model.TableData;
import service.CourseService;
import utils.DateUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Mediates between the course views and the service layer. */
public class CourseController {

    private final CourseService service = new CourseService();

    public static final String[] COLUMNS = {
        "DB ID", "Code", "Course Name", "Department", "Duration", "Fee", "Students"
    };

    public List<Course> findAll() throws SQLException { return service.findAll(); }
    public List<Enrollment> enrolledStudents(int courseId) throws SQLException {
        return service.enrolledStudents(courseId);
    }
    public Course create(Course c) throws SQLException { return service.create(c); }
    public void update(Course c) throws SQLException { service.update(c); }
    public void delete(int id) throws SQLException { service.delete(id); }
    public boolean assignStudent(int studentId, int courseId) throws SQLException {
        return service.assignStudent(studentId, courseId);
    }
    public boolean removeEnrollment(int enrollmentId) throws SQLException {
        return service.removeEnrollment(enrollmentId);
    }

    public Object[][] rows(List<Course> courses) {
        Object[][] data = new Object[courses.size()][COLUMNS.length];
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            data[i][0] = c.getCourseId();
            data[i][1] = c.getCourseCode();
            data[i][2] = c.getCourseName();
            data[i][3] = c.getDeptName();
            data[i][4] = c.getDurationYears() + " years";
            data[i][5] = String.format("%.2f", c.getCourseFee());
            data[i][6] = c.getStudentCount();
        }
        return data;
    }

    public TableData toTable(List<Course> courses) {
        TableData t = new TableData();
        for (String c : COLUMNS) t.getHeaders().add(c);
        for (Course c : courses) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(c.getCourseId()));
            row.add(c.getCourseCode());
            row.add(c.getCourseName());
            row.add(c.getDeptName());
            row.add(c.getDurationYears() + " years");
            row.add(String.format("%.2f", c.getCourseFee()));
            row.add(String.valueOf(c.getStudentCount()));
            t.addRow(row);
        }
        return t;
    }

    public TableData enrollmentTable(List<Enrollment> list) {
        TableData t = new TableData();
        t.getHeaders().add("Enrollment ID");
        t.getHeaders().add("Student ID");
        t.getHeaders().add("Student Name");
        t.getHeaders().add("Course");
        t.getHeaders().add("Enrolled On");
        t.getHeaders().add("Status");
        for (Enrollment e : list) {
            t.addRow(String.valueOf(e.getEnrollmentId()), e.getStudentCode(), e.getStudentName(),
                     e.getCourseName(), DateUtil.iso(e.getEnrollDate()), e.getStatus());
        }
        return t;
    }
}
