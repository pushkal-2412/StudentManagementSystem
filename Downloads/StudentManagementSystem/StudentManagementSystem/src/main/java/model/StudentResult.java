package model;

/** Aggregated exam result of a student with automatic grade calculation. */
public class StudentResult {
    private int studentId;
    private String studentCode;
    private String studentName;
    private String courseName;
    private int examCount;
    private double totalMax;
    private double totalObtained;

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getExamCount() { return examCount; }
    public void setExamCount(int examCount) { this.examCount = examCount; }

    public double getTotalMax() { return totalMax; }
    public void setTotalMax(double totalMax) { this.totalMax = totalMax; }

    public double getTotalObtained() { return totalObtained; }
    public void setTotalObtained(double totalObtained) { this.totalObtained = totalObtained; }

    /** Percentage of aggregate marks, rounded to 2 decimals. */
    public double getPercentage() {
        if (totalMax <= 0) return 0.0;
        return Math.round((totalObtained * 10000.0) / totalMax) / 100.0;
    }

    /** Automatic grade from percentage (university 10-point style bands). */
    public String getGrade() { return gradeFor(getPercentage()); }

    public String getResultStatus() { return getPercentage() >= 40 ? "PASS" : "FAIL"; }

    /** Central grade band logic used by the whole application. */
    public static String gradeFor(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        if (percentage >= 40) return "E";
        return "F";
    }
}
