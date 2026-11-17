package controller;

import model.Attendance;
import model.AttendanceSummary;
import model.Student;
import model.TableData;
import service.AttendanceService;
import utils.DateUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Mediates between the attendance views and the service layer. */
public class AttendanceController {

    private final AttendanceService service = new AttendanceService();

    public static final String[] RECORD_COLUMNS = {
        "Attendance ID", "Student ID", "Student Name", "Course",
        "Date", "Status", "Percentage"
    };

    public static final String[] SUMMARY_COLUMNS = {
        "Student ID", "Student Name", "Course", "Present", "Absent", "Late",
        "Total", "Percentage", "Remark"
    };

    public List<Student> studentsOfCourse(int courseId) throws SQLException {
        return service.studentsOfCourse(courseId);
    }

    public List<Attendance> byDate(LocalDate date, int courseId) throws SQLException {
        return service.byDate(date, courseId);
    }

    public List<Attendance> byStudent(int studentId) throws SQLException {
        return service.byStudent(studentId);
    }

    public void mark(int studentId, int courseId, LocalDate date, String status, int adminId)
            throws SQLException {
        service.mark(studentId, courseId, date, status, adminId);
    }

    public int markAll(List<int[]> pairs, LocalDate date, String status, int adminId)
            throws SQLException {
        return service.markAll(pairs, date, status, adminId);
    }

    public List<AttendanceSummary> monthly(YearMonth month, int courseId) throws SQLException {
        return service.monthly(month, courseId);
    }

    public double percentage(int studentId) throws SQLException {
        return service.percentageForStudent(studentId);
    }

    public Object[][] rows(List<Attendance> list) {
        Object[][] data = new Object[list.size()][RECORD_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            Attendance a = list.get(i);
            data[i][0] = a.getAttendanceId();
            data[i][1] = a.getStudentCode();
            data[i][2] = a.getStudentName();
            data[i][3] = a.getCourseName();
            data[i][4] = DateUtil.format(a.getAttDate());
            data[i][5] = a.getStatus();
            try {
                data[i][6] = String.format("%.2f%%", percentage(a.getStudentId()));
            } catch (SQLException e) {
                data[i][6] = "-";
            }
        }
        return data;
    }

    public Object[][] summaryRows(List<AttendanceSummary> list) {
        Object[][] data = new Object[list.size()][SUMMARY_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            AttendanceSummary s = list.get(i);
            data[i][0] = s.getStudentCode();
            data[i][1] = s.getStudentName();
            data[i][2] = s.getCourseName();
            data[i][3] = s.getPresentCount();
            data[i][4] = s.getAbsentCount();
            data[i][5] = s.getLateCount();
            data[i][6] = s.getTotalCount();
            data[i][7] = String.format("%.2f%%", s.getPercentage());
            data[i][8] = s.getStatusLabel();
        }
        return data;
    }

    public TableData summaryTable(List<AttendanceSummary> list, String period) {
        TableData t = new TableData();
        for (String c : SUMMARY_COLUMNS) t.getHeaders().add(c);
        for (AttendanceSummary s : list) {
            List<String> row = new ArrayList<>();
            row.add(s.getStudentCode());
            row.add(s.getStudentName());
            row.add(s.getCourseName());
            row.add(String.valueOf(s.getPresentCount()));
            row.add(String.valueOf(s.getAbsentCount()));
            row.add(String.valueOf(s.getLateCount()));
            row.add(String.valueOf(s.getTotalCount()));
            row.add(String.format("%.2f%%", s.getPercentage()));
            row.add(s.getStatusLabel());
            t.addRow(row);
        }
        return t;
    }

    public TableData recordTable(List<Attendance> list) {
        TableData t = new TableData();
        for (String c : RECORD_COLUMNS) t.getHeaders().add(c);
        for (Attendance a : list) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(a.getAttendanceId()));
            row.add(a.getStudentCode());
            row.add(a.getStudentName());
            row.add(a.getCourseName());
            row.add(DateUtil.iso(a.getAttDate()));
            row.add(a.getStatus());
            row.add("-");
            t.addRow(row);
        }
        return t;
    }

    public static String monthLabel(YearMonth m) {
        return m.getMonth().toString() + " " + m.getYear();
    }
}
