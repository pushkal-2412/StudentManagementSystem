package controller;

import dao.DashboardDAO;
import model.AttendanceSummary;
import model.Student;
import model.StudentResult;
import model.TableData;
import service.AttendanceService;
import service.FeeService;
import service.StudentService;
import service.ExamService;
import utils.DateUtil;
import utils.MoneyUtil;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Builds every report as a TableData ready for CSV / PDF export. */
public class ReportController {

    private final StudentService studentService = new StudentService();
    private final AttendanceService attendanceService = new AttendanceService();
    private final ExamService examService = new ExamService();
    private final FeeService feeService = new FeeService();

    public List<Student> students() throws SQLException { return studentService.findAll(); }

    public TableData departmentReport(int deptId, String deptName) throws SQLException {
        List<Student> students = studentService.search(null, deptId, 0);
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Student ID", "Full Name", "Gender", "Email", "Phone",
                "Department", "Course", "Year", "Semester", "Admission Date"));
        for (Student s : students) {
            t.addRow(s.getStudentCode(), s.getFullName(), s.getGender(), s.getEmail(),
                    s.getPhone(), s.getDeptName(), s.getCourseName(), s.getAcademicYear(),
                    String.valueOf(s.getSemester()), DateUtil.iso(s.getAdmissionDate()));
        }
        return t;
    }

    public TableData courseReport(int courseId) throws SQLException {
        List<Student> students = studentService.search(null, 0, courseId);
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Student ID", "Full Name", "Department", "Course",
                "Year", "Semester", "Email", "Phone"));
        for (Student s : students) {
            t.addRow(s.getStudentCode(), s.getFullName(), s.getDeptName(), s.getCourseName(),
                    s.getAcademicYear(), String.valueOf(s.getSemester()), s.getEmail(),
                    s.getPhone());
        }
        return t;
    }

    public TableData attendanceReport(YearMonth month, int courseId) throws SQLException {
        List<AttendanceSummary> list = attendanceService.monthly(month, courseId);
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Student ID", "Student Name", "Course", "Present",
                "Absent", "Late", "Total Days", "Attendance %", "Remark"));
        for (AttendanceSummary s : list) {
            t.addRow(s.getStudentCode(), s.getStudentName(), s.getCourseName(),
                    String.valueOf(s.getPresentCount()), String.valueOf(s.getAbsentCount()),
                    String.valueOf(s.getLateCount()), String.valueOf(s.getTotalCount()),
                    String.format("%.2f%%", s.getPercentage()), s.getStatusLabel());
        }
        return t;
    }

    public TableData examReport(int courseId) throws SQLException {
        List<StudentResult> list = examService.results(courseId);
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Student ID", "Student Name", "Course", "Exams Taken",
                "Total Obtained", "Total Maximum", "Percentage", "Grade", "Result"));
        for (StudentResult r : list) {
            t.addRow(r.getStudentCode(), r.getStudentName(), r.getCourseName(),
                    String.valueOf(r.getExamCount()),
                    String.format("%.2f", r.getTotalObtained()),
                    String.format("%.2f", r.getTotalMax()),
                    String.format("%.2f%%", r.getPercentage()),
                    r.getGrade(), r.getResultStatus());
        }
        return t;
    }

    public TableData feeReport(String status) throws SQLException {
        var fees = feeService.search(null, status);
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Student ID", "Student Name", "Course", "Total Fee",
                "Paid", "Balance", "Status"));
        for (var f : fees) {
            t.addRow(f.getStudentCode(), f.getStudentName(), f.getCourseName(),
                    MoneyUtil.plain(f.getTotalFee()), MoneyUtil.plain(f.getPaidAmount()),
                    MoneyUtil.plain(f.getRemainingAmount()),
                    f.getPaymentStatus().replace('_', ' '));
        }
        return t;
    }

    public TableData paymentHistoryReport() throws SQLException {
        var list = feeService.allPayments();
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Receipt No", "Student ID", "Student Name", "Amount",
                "Payment Date", "Mode", "Remarks"));
        for (var p : list) {
            t.addRow(String.valueOf(p.getPaymentId()), p.getStudentCode(), p.getStudentName(),
                    MoneyUtil.plain(p.getAmount()), DateUtil.iso(p.getPaymentDate()),
                    p.getMode(), p.getRemarks());
        }
        return t;
    }

    /** Institute summary table used on the reports screen. */
    public TableData summary() throws SQLException {
        var stats = new DashboardDAO().load();
        TableData t = new TableData();
        t.getHeaders().addAll(List.of("Metric", "Value"));
        t.addRow("Total students", String.valueOf(stats.getTotalStudents()));
        t.addRow("Total courses", String.valueOf(stats.getTotalCourses()));
        t.addRow("Total departments", String.valueOf(stats.getTotalDepartments()));
        t.addRow("Total enrollments", String.valueOf(stats.getTotalEnrollments()));
        t.addRow("Male students", String.valueOf(stats.getMaleStudents()));
        t.addRow("Female students", String.valueOf(stats.getFemaleStudents()));
        t.addRow("Fee collected", MoneyUtil.plain(stats.getFeeCollected()));
        t.addRow("Fee pending", MoneyUtil.plain(stats.getFeePending()));
        t.addRow("Average attendance %",
                String.format("%.2f", stats.getAverageAttendance()));
        List<String> names = new ArrayList<>();
        for (var row : stats.getDepartmentWise()) {
            names.add(row.getLabel() + "=" + (int) row.getValue());
        }
        t.addRow("Students per department", String.join(", ", names));
        return t;
    }

    public String describeCourse(int courseId) {
        return courseId > 0 ? "Course filter applied" : "All courses";
    }
}
