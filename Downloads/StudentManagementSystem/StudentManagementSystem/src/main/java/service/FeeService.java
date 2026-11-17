package service;

import dao.CourseDAO;
import dao.FeeDAO;
import dao.StudentDAO;
import model.Course;
import model.Fee;
import model.FeePayment;
import model.Student;
import utils.MoneyUtil;
import utils.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Fee account rules: balances, payments and history. */
public class FeeService {

    private final FeeDAO feeDAO = new FeeDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    public List<Fee> search(String keyword, String status) throws SQLException {
        return feeDAO.search(keyword, status);
    }

    public List<Fee> findAll() throws SQLException { return feeDAO.findAll(); }

    public List<FeePayment> payments(int feeId) throws SQLException {
        return feeDAO.findPayments(feeId);
    }

    public List<FeePayment> allPayments() throws SQLException {
        return feeDAO.findAllPayments();
    }

    public double collected() throws SQLException { return feeDAO.totalCollected(); }
    public double pending() throws SQLException { return feeDAO.totalPending(); }

    /**
     * Records a payment. Rejects zero / negative amounts, future dates and any
     * amount that would push the student above the total course fee.
     */
    public void pay(int feeId, double amount, LocalDate date, String mode, String remarks,
                    int adminId) throws SQLException {
        Fee fee = feeDAO.findAll().stream()
                .filter(f -> f.getFeeId() == feeId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fee account not found."));

        if (fee.getRemainingAmount() <= 0) {
            throw new IllegalArgumentException(
                "This student has already paid the full fee of "
              + MoneyUtil.format(fee.getTotalFee()) + ".");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (amount > fee.getRemainingAmount() + 0.001) {
            throw new IllegalArgumentException("Amount exceeds the remaining balance of "
                + MoneyUtil.format(fee.getRemainingAmount()) + ".");
        }
        if (date == null) {
            throw new IllegalArgumentException("Payment date is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future.");
        }
        if (date.isBefore(fee.getPaidAmount() > 0 ? LocalDate.of(2000, 1, 1) : LocalDate.of(2000, 1, 1))) {
            throw new IllegalArgumentException("Payment date is not valid.");
        }
        ValidationUtil.requireText(mode, "Payment mode");
        if (remarks != null && remarks.length() > 200) {
            throw new IllegalArgumentException("Remarks cannot exceed 200 characters.");
        }
        feeDAO.addPayment(feeId, ValidationUtil.round2(amount), date, mode, remarks, adminId);
    }

    /** Re-syncs the fee account total with the current course fee. */
    public void syncWithCourseFee(int feeId, int studentId) throws SQLException {
        Course course = courseDAO.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student has no course."));
        feeDAO.updateTotalFee(feeId, course.getCourseFee());
    }

    public Optional<Fee> ofStudent(int studentId) throws SQLException {
        return feeDAO.findByStudent(studentId);
    }

    public List<Student> students() throws SQLException { return studentDAO.findAll(); }

    public void deletePayment(int paymentId) throws SQLException {
        if (!feeDAO.deletePayment(paymentId)) {
            throw new IllegalArgumentException("Payment not found.");
        }
    }

    /** Status label with the balance, used in tables and dialogs. */
    public static String statusText(Fee f) {
        return f.getPaymentStatus().replace('_', ' ') + "  \u2013  due "
             + MoneyUtil.format(f.getRemainingAmount());
    }
}
