package model;

/** Aggregated attendance for one student over a period. */
public class AttendanceSummary {
    private int studentId;
    private String studentCode;
    private String studentName;
    private String courseName;
    private int presentCount;
    private int absentCount;
    private int lateCount;
    private int totalCount;

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getPresentCount() { return presentCount; }
    public void setPresentCount(int presentCount) { this.presentCount = presentCount; }

    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }

    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) { this.lateCount = lateCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    /** Attendance percentage = present / total * 100 (rounded to 2 decimals). */
    public double getPercentage() {
        if (totalCount <= 0) return 0.0;
        return Math.round((presentCount * 10000.0) / totalCount) / 100.0;
    }

    public String getStatusLabel() {
        double p = getPercentage();
        if (totalCount == 0) return "NO DATA";
        if (p >= 75) return "GOOD";
        if (p >= 60) return "AVERAGE";
        return "SHORTAGE";
    }
}
