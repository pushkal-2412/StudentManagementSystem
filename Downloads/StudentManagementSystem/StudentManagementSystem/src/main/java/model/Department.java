package model;

/** Academic department, e.g. CSE. */
public class Department {
    private int deptId;
    private String deptCode;
    private String deptName;
    private String hodName;
    private int studentCount;
    private int courseCount;

    public Department() { }

    public Department(int deptId, String deptCode, String deptName, String hodName) {
        this.deptId = deptId;
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.hodName = hodName;
    }

    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getHodName() { return hodName; }
    public void setHodName(String hodName) { this.hodName = hodName; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    public int getCourseCount() { return courseCount; }
    public void setCourseCount(int courseCount) { this.courseCount = courseCount; }

    /** Used by JComboBox renderers. */
    @Override
    public String toString() { return deptCode + " - " + deptName; }
}
