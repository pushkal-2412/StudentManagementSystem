package web;

import com.lowagie.text.DocumentException;
import controller.DashboardController;
import controller.ReportController;
import dao.AdminDAO;
import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import service.*;
import utils.ExportUtil;
import utils.PasswordUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api")
public class WebApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApiController.class);

    private final AdminDAO adminDAO = new AdminDAO();
    private final DashboardController dashboard = new DashboardController();
    private final ReportController reports = new ReportController();
    private final StudentService students = new StudentService();
    private final CourseService courses = new CourseService();
    private final DepartmentService departments = new DepartmentService();
    private final AttendanceService attendance = new AttendanceService();
    private final ExamService exams = new ExamService();
    private final FeeService fees = new FeeService();

    @GetMapping("/session")
    public Map<String, Object> session(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(WebSecurityInterceptor.CSRF_TOKEN);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(WebSecurityInterceptor.CSRF_TOKEN, token);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("csrfToken", token);
        Object id = session.getAttribute(WebSecurityInterceptor.ADMIN_ID);
        result.put("authenticated", id != null);
        if (id instanceof Integer adminId) {
            try {
                Admin admin = adminDAO.findById(adminId).orElse(null);
                if (admin == null) {
                    session.invalidate();
                    result.put("authenticated", false);
                } else {
                    result.put("user", publicUser(admin));
                }
            } catch (SQLException ex) {
                throw databaseError(ex);
            }
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest body,
                                     HttpServletRequest request) {
        if (body.username() == null || body.username().isBlank()
                || body.password() == null || body.password().isEmpty()) {
            throw new IllegalArgumentException("Enter your username and password.");
        }
        try {
            Admin admin = adminDAO.findByUsername(body.username().trim()).orElse(null);
            if (admin == null || !PasswordUtil.verify(body.password(), admin.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password.");
            }
            request.changeSessionId();
            HttpSession session = request.getSession(false);
            session.setAttribute(WebSecurityInterceptor.ADMIN_ID, admin.getAdminId());
            session.setAttribute(WebSecurityInterceptor.CSRF_TOKEN, UUID.randomUUID().toString());
            return Map.of("user", publicUser(admin),
                    "csrfToken", session.getAttribute(WebSecurityInterceptor.CSRF_TOKEN));
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Map.of("message", "Signed out.");
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestBody ChangePasswordRequest body,
                                               HttpServletRequest request) {
        try {
            int adminId = currentAdminId(request);
            Admin admin = adminDAO.findById(adminId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            if (!PasswordUtil.verify(body.currentPassword(), admin.getPasswordHash())) {
                throw new IllegalArgumentException("The current password is incorrect.");
            }
            if (body.newPassword() == null || body.newPassword().length() < 6) {
                throw new IllegalArgumentException("The new password must be at least 6 characters.");
            }
            if (!body.newPassword().equals(body.confirmPassword())) {
                throw new IllegalArgumentException("The new password confirmation does not match.");
            }
            if (PasswordUtil.verify(body.newPassword(), admin.getPasswordHash())) {
                throw new IllegalArgumentException("Choose a password different from the current one.");
            }
            if (!adminDAO.updatePasswordHash(admin.getUsername(),
                    PasswordUtil.hash(body.newPassword()))) {
                throw new IllegalArgumentException("The administrator account no longer exists.");
            }
            return Map.of("message", "Password updated.");
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    @GetMapping("/dashboard")
    public DashboardStats dashboard() throws SQLException {
        return dashboard.load();
    }

    @GetMapping("/options")
    public Map<String, Object> options() throws SQLException {
        return Map.of("departments", departments.findAll(), "courses", courses.findAll(),
                "students", students.findAll());
    }

    @GetMapping("/students")
    public List<Student> students(@RequestParam(defaultValue = "") String search,
                                 @RequestParam(defaultValue = "0") int deptId,
                                 @RequestParam(defaultValue = "0") int courseId) throws SQLException {
        return students.search(search, deptId, courseId);
    }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) throws SQLException {
        return students.create(student);
    }

    @PutMapping("/students/{id}")
    public Map<String, String> updateStudent(@PathVariable int id, @RequestBody Student student)
            throws SQLException {
        student.setStudentId(id);
        students.update(student);
        return Map.of("message", "Student updated.");
    }

    @DeleteMapping("/students/{id}")
    public Map<String, String> deleteStudent(@PathVariable int id) throws SQLException {
        students.delete(id);
        return Map.of("message", "Student deleted.");
    }

    @GetMapping("/courses")
    public List<Course> courses() throws SQLException {
        return courses.findAll();
    }

    @PostMapping("/courses")
    public Course createCourse(@RequestBody Course course) throws SQLException {
        return courses.create(course);
    }

    @PutMapping("/courses/{id}")
    public Map<String, String> updateCourse(@PathVariable int id, @RequestBody Course course)
            throws SQLException {
        course.setCourseId(id);
        courses.update(course);
        return Map.of("message", "Course updated.");
    }

    @DeleteMapping("/courses/{id}")
    public Map<String, String> deleteCourse(@PathVariable int id) throws SQLException {
        courses.delete(id);
        return Map.of("message", "Course deleted.");
    }

    @PostMapping("/courses/{courseId}/enrollments")
    public Map<String, Object> enrollStudent(@PathVariable int courseId,
                                              @RequestBody EnrollmentRequest body)
            throws SQLException {
        boolean created = courses.assignStudent(body.studentId(), courseId);
        return Map.of("created", created,
                "message", created ? "Student enrolled." : "Student is already enrolled.");
    }

    @GetMapping("/courses/{courseId}/enrollments")
    public List<Enrollment> courseEnrollments(@PathVariable int courseId) throws SQLException {
        return courses.enrolledStudents(courseId);
    }

    @DeleteMapping("/enrollments/{id}")
    public Map<String, String> deleteEnrollment(@PathVariable int id) throws SQLException {
        if (!courses.removeEnrollment(id)) {
            throw new IllegalArgumentException("Enrollment was not found.");
        }
        return Map.of("message", "Enrollment removed.");
    }

    @GetMapping("/departments")
    public List<Department> departments() throws SQLException {
        return departments.findAll();
    }

    @PostMapping("/departments")
    public Department createDepartment(@RequestBody Department department) throws SQLException {
        return departments.create(department);
    }

    @PutMapping("/departments/{id}")
    public Map<String, String> updateDepartment(@PathVariable int id,
                                                @RequestBody Department department)
            throws SQLException {
        department.setDeptId(id);
        departments.update(department);
        return Map.of("message", "Department updated.");
    }

    @DeleteMapping("/departments/{id}")
    public Map<String, String> deleteDepartment(@PathVariable int id) throws SQLException {
        departments.delete(id);
        return Map.of("message", "Department deleted.");
    }

    @GetMapping("/attendance")
    public List<Attendance> attendance(@RequestParam LocalDate date,
                                       @RequestParam(defaultValue = "0") int courseId)
            throws SQLException {
        return attendance.byDate(date, courseId);
    }

    @GetMapping("/attendance/monthly")
    public List<AttendanceSummary> monthlyAttendance(@RequestParam YearMonth month,
                                                      @RequestParam(defaultValue = "0") int courseId)
            throws SQLException {
        return attendance.monthly(month, courseId);
    }

    @PostMapping("/attendance")
    public Map<String, String> markAttendance(@RequestBody AttendanceRequest body,
                                               HttpServletRequest request) throws SQLException {
        attendance.mark(body.studentId(), body.courseId(), body.date(), body.status(),
                currentAdminId(request));
        return Map.of("message", "Attendance saved.");
    }

    @PostMapping("/attendance/bulk")
    public Map<String, Object> markAllAttendance(@RequestBody BulkAttendanceRequest body,
                                                  HttpServletRequest request) throws SQLException {
        List<Student> list = attendance.studentsOfCourse(body.courseId());
        int count = attendance.markAll(attendance.pairsFor(list, body.courseId()), body.date(),
                body.status(), currentAdminId(request));
        return Map.of("message", "Attendance saved.", "updated", count);
    }

    @GetMapping("/exams")
    public List<Examination> exams(@RequestParam(defaultValue = "0") int courseId)
            throws SQLException {
        return courseId > 0 ? exams.examsOfCourse(courseId) : exams.exams();
    }

    @PostMapping("/exams")
    public Examination createExam(@RequestBody Examination exam) throws SQLException {
        return exams.createExam(exam);
    }

    @PutMapping("/exams/{id}")
    public Map<String, String> updateExam(@PathVariable int id, @RequestBody Examination exam)
            throws SQLException {
        exam.setExamId(id);
        exams.updateExam(exam);
        return Map.of("message", "Examination updated.");
    }

    @DeleteMapping("/exams/{id}")
    public Map<String, String> deleteExam(@PathVariable int id) throws SQLException {
        exams.deleteExam(id);
        return Map.of("message", "Examination deleted.");
    }

    @GetMapping("/exams/{id}/marks")
    public List<Mark> marks(@PathVariable int id) throws SQLException {
        return exams.marksOfExam(id);
    }

    @PostMapping("/marks")
    public Map<String, String> saveMark(@RequestBody MarkRequest body) throws SQLException {
        exams.saveMark(body.examId(), body.studentId(), body.marks());
        return Map.of("message", "Mark saved.");
    }

    @GetMapping("/results")
    public List<StudentResult> results(@RequestParam(defaultValue = "0") int courseId)
            throws SQLException {
        return exams.results(courseId);
    }

    @GetMapping("/fees")
    public List<Fee> fees(@RequestParam(defaultValue = "") String search,
                          @RequestParam(defaultValue = "") String status) throws SQLException {
        return fees.search(search, status);
    }

    @GetMapping("/payments")
    public List<FeePayment> payments() throws SQLException {
        return fees.allPayments();
    }

    @GetMapping("/fees/{id}/payments")
    public List<FeePayment> paymentsForFee(@PathVariable int id) throws SQLException {
        return fees.payments(id);
    }

    @PostMapping("/payments")
    public Map<String, String> addPayment(@RequestBody PaymentRequest body,
                                           HttpServletRequest request) throws SQLException {
        fees.pay(body.feeId(), body.amount(), body.date(), body.mode(), body.remarks(),
                currentAdminId(request));
        return Map.of("message", "Payment recorded.");
    }

    @DeleteMapping("/payments/{id}")
    public Map<String, String> deletePayment(@PathVariable int id) throws SQLException {
        fees.deletePayment(id);
        return Map.of("message", "Payment deleted.");
    }

    @GetMapping("/reports")
    public TableData report(@RequestParam String type,
                            @RequestParam(defaultValue = "0") int courseId,
                            @RequestParam(defaultValue = "0") int deptId,
                            @RequestParam(defaultValue = "") String month,
                            @RequestParam(defaultValue = "") String status) throws SQLException {
        return loadReport(type, courseId, deptId, month, status);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(@RequestParam String type,
                                                @RequestParam(defaultValue = "0") int courseId,
                                                @RequestParam(defaultValue = "0") int deptId,
                                                @RequestParam(defaultValue = "") String month,
                                                @RequestParam(defaultValue = "") String status,
                                                @RequestParam(defaultValue = "csv") String format)
            throws SQLException, IOException {
        TableData data = loadReport(type, courseId, deptId, month, status);
        String name = safeFilename(type);
        HttpHeaders headers = new HttpHeaders();
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder("\uFEFF");
            appendCsvRow(csv, data.getHeaders());
            data.getRows().forEach(row -> appendCsvRow(csv, row));
            headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(name + ".csv").build());
            return new ResponseEntity<>(csv.toString().getBytes(StandardCharsets.UTF_8),
                    headers, HttpStatus.OK);
        }
        if ("pdf".equalsIgnoreCase(format)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                ExportUtil.writePdf(output, data, type.replace('-', ' '),
                        "Student Management System");
            } catch (DocumentException ex) {
                throw new IOException("Could not generate the PDF report.", ex);
            }
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(name + ".pdf").build());
            return new ResponseEntity<>(output.toByteArray(), headers, HttpStatus.OK);
        }
        throw new IllegalArgumentException("Export format must be csv or pdf.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", safeMessage(ex)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statusError(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(Map.of("message",
                ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, String>> databaseErrorHandler(SQLException ex) {
        LOGGER.error("Database request failed.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "The database operation failed. Check the server log."));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> exportError(IOException ex) {
        LOGGER.error("Report export failed.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "The report could not be generated."));
    }

    private TableData loadReport(String type, int courseId, int deptId, String month,
                                 String status) throws SQLException {
        return switch (type) {
            case "summary" -> reports.summary();
            case "department" -> reports.departmentReport(deptId, "");
            case "course" -> reports.courseReport(courseId);
            case "attendance" -> reports.attendanceReport(YearMonth.parse(month), courseId);
            case "examinations" -> reports.examReport(courseId);
            case "fees" -> reports.feeReport(status);
            case "payments" -> reports.paymentHistoryReport();
            default -> throw new IllegalArgumentException("Unknown report type.");
        };
    }

    private int currentAdminId(HttpServletRequest request) {
        Object id = request.getSession(false) == null ? null
                : request.getSession(false).getAttribute(WebSecurityInterceptor.ADMIN_ID);
        if (id instanceof Integer adminId) {
            return adminId;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
    }

    private Map<String, Object> publicUser(Admin admin) {
        return Map.of("username", admin.getUsername(), "fullName", admin.getFullName(),
                "role", admin.getRole());
    }

    private RuntimeException databaseError(SQLException ex) {
        LOGGER.error("Database request failed.", ex);
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "The database operation failed. Check the server log.");
    }

    private String safeMessage(IllegalArgumentException ex) {
        return ex.getMessage() == null ? "The submitted data is invalid." : ex.getMessage();
    }

    private void appendCsvRow(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) csv.append(',');
            String value = cells.get(i) == null ? "" : cells.get(i);
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private String safeFilename(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public record LoginRequest(String username, String password) { }
    public record ChangePasswordRequest(String currentPassword, String newPassword,
                                        String confirmPassword) { }
    public record EnrollmentRequest(int studentId) { }
    public record AttendanceRequest(int studentId, int courseId, LocalDate date, String status) { }
    public record BulkAttendanceRequest(int courseId, LocalDate date, String status) { }
    public record MarkRequest(int examId, int studentId, double marks) { }
    public record PaymentRequest(int feeId, double amount, LocalDate date, String mode,
                                 String remarks) { }
}
