package controller;

import model.Course;
import model.Department;
import model.Student;
import model.TableData;
import service.StudentService;
import utils.DateUtil;
import utils.ValidationUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Mediates between the student views and the service layer. */
public class StudentController {

    private final StudentService service = new StudentService();

    public static final String[] COLUMNS = {
        "DB ID", "Student ID", "Full Name", "Email", "Phone",
        "Department", "Course", "Year", "Sem", "Admission"
    };

    public List<Student> search(String keyword, int deptId, int courseId) throws SQLException {
        return service.search(keyword, deptId, courseId);
    }

    public List<Department> departments() throws SQLException { return service.departments(); }
    public List<Course> courses() throws SQLException { return service.courses(); }
    public String suggestCode() throws SQLException { return service.suggestStudentCode(); }

    public Student create(Student s) throws SQLException { return service.create(s); }
    public void update(Student s) throws SQLException { service.update(s); }
    public void delete(int id) throws SQLException { service.delete(id); }

    /** Converts students into table rows (and into the export model). */
    public Object[][] rows(List<Student> students) {
        Object[][] data = new Object[students.size()][COLUMNS.length];
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            data[i][0] = s.getStudentId();
            data[i][1] = s.getStudentCode();
            data[i][2] = s.getFullName();
            data[i][3] = s.getEmail();
            data[i][4] = s.getPhone();
            data[i][5] = s.getDeptName();
            data[i][6] = s.getCourseName();
            data[i][7] = s.getAcademicYear();
            data[i][8] = s.getSemester();
            data[i][9] = DateUtil.format(s.getAdmissionDate());
        }
        return data;
    }

    public TableData toTable(List<Student> students) {
        TableData t = new TableData();
        for (String c : COLUMNS) t.getHeaders().add(c);
        for (Student s : students) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(s.getStudentId()));
            row.add(s.getStudentCode());
            row.add(s.getFullName());
            row.add(s.getEmail());
            row.add(s.getPhone());
            row.add(s.getDeptName());
            row.add(s.getCourseName());
            row.add(s.getAcademicYear());
            row.add(String.valueOf(s.getSemester()));
            row.add(DateUtil.iso(s.getAdmissionDate()));
            t.addRow(row);
        }
        return t;
    }

    public String describe(Student s) { return StudentService.describe(s); }

    public double round(double v) { return ValidationUtil.round2(v); }
}
