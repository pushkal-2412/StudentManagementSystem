package model;

import java.time.LocalDate;

/** One attendance entry for a student on a given date for a given course. */
public class Attendance {
    private int attendanceId;
    private int studentId;
    private String studentCode;
    private String studentName;
    private int courseId;
    private String courseName;
    private LocalDate attDate;
    private String status;

    public Attendance() { }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public LocalDate getAttDate() { return attDate; }
    public void setAttDate(LocalDate attDate) { this.attDate = attDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
