package model;

import java.time.LocalDate;

/** An examination conducted for a course. */
public class Examination {
    private int examId;
    private String examName;
    private int courseId;
    private String courseName;
    private LocalDate examDate;
    private double maxMarks;
    private String examType;

    public Examination() { }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    @Override
    public String toString() { return examName + " (max " + (int) maxMarks + ")"; }
}
