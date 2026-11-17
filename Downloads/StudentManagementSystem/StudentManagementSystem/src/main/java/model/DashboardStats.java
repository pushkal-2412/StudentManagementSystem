package model;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of numbers shown on the dashboard. */
public class DashboardStats {
    private int totalStudents;
    private int totalCourses;
    private int totalDepartments;
    private int totalEnrollments;
    private int maleStudents;
    private int femaleStudents;
    private double feeCollected;
    private double feePending;
    private double averageAttendance;
    private List<Student> recentStudents = new ArrayList<>();
    private List<ChartRow> departmentWise = new ArrayList<>();
    private List<ChartRow> courseWise = new ArrayList<>();

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int v) { this.totalStudents = v; }

    public int getTotalCourses() { return totalCourses; }
    public void setTotalCourses(int v) { this.totalCourses = v; }

    public int getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(int v) { this.totalDepartments = v; }

    public int getTotalEnrollments() { return totalEnrollments; }
    public void setTotalEnrollments(int v) { this.totalEnrollments = v; }

    public int getMaleStudents() { return maleStudents; }
    public void setMaleStudents(int v) { this.maleStudents = v; }

    public int getFemaleStudents() { return femaleStudents; }
    public void setFemaleStudents(int v) { this.femaleStudents = v; }

    public double getFeeCollected() { return feeCollected; }
    public void setFeeCollected(double v) { this.feeCollected = v; }

    public double getFeePending() { return feePending; }
    public void setFeePending(double v) { this.feePending = v; }

    public double getAverageAttendance() { return averageAttendance; }
    public void setAverageAttendance(double v) { this.averageAttendance = v; }

    public List<Student> getRecentStudents() { return recentStudents; }
    public void setRecentStudents(List<Student> v) { this.recentStudents = v; }

    public List<ChartRow> getDepartmentWise() { return departmentWise; }
    public void setDepartmentWise(List<ChartRow> v) { this.departmentWise = v; }

    public List<ChartRow> getCourseWise() { return courseWise; }
    public void setCourseWise(List<ChartRow> v) { this.courseWise = v; }
}
