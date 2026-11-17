package dao;

import database.DBConnection;
import model.Fee;
import model.FeePayment;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for fees and fee_payments, including transactional payments. */
public class FeeDAO {

    private static final String SELECT =
        "SELECT f.fee_id, f.student_id, f.total_fee, f.paid_amount, f.remaining_amount, "
      + "       f.payment_status, s.student_code, s.full_name, c.course_name "
      + "FROM fees f "
      + "JOIN students s ON s.student_id = f.student_id "
      + "JOIN courses  c ON c.course_id  = s.course_id ";

    public List<Fee> findAll() throws SQLException {
        List<Fee> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "ORDER BY s.student_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Fee> search(String keyword, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (s.full_name LIKE ? OR s.student_code LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND f.payment_status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY f.remaining_amount DESC, s.student_code");
        List<Fee> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Fee> findByStudent(int studentId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT + "WHERE f.student_id = ?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /** Creates the fee account for a student if it does not exist yet. */
    public void ensureFeeAccount(Connection cn, int studentId) throws SQLException {
        String sql = "INSERT INTO fees (student_id, total_fee) "
                   + "SELECT ?, c.course_fee FROM students s "
                   + "JOIN courses c ON c.course_id = s.course_id "
                   + "WHERE s.student_id = ? "
                   + "ON DUPLICATE KEY UPDATE total_fee = VALUES(total_fee)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        }
    }

    public void ensureFeeAccount(int studentId) throws SQLException {
        try (Connection cn = DBConnection.getConnection()) {
            ensureFeeAccount(cn, studentId);
        }
    }

    public boolean updateTotalFee(int feeId, double totalFee) throws SQLException {
        String sql = "UPDATE fees SET total_fee = ? WHERE fee_id = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDouble(1, totalFee);
            ps.setInt(2, feeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Records a payment inside a transaction.
     * The database trigger recalculates paid_amount and payment_status.
     * Payment is rejected when it would exceed the total course fee.
     */
    public void addPayment(int feeId, double amount, LocalDate date, String mode, String remarks,
                           int adminId) throws SQLException {
        if (amount <= 0) {
            throw new SQLException("Payment amount must be greater than zero.");
        }
        Connection cn = null;
        try {
            cn = DBConnection.getConnection();
            cn.setAutoCommit(false);

            double total;
            double paid;
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT total_fee, paid_amount FROM fees WHERE fee_id = ? FOR UPDATE")) {
                ps.setInt(1, feeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Fee account not found.");
                    }
                    total = rs.getDouble("total_fee");
                    paid = rs.getDouble("paid_amount");
                }
            }
            double newPaid = paid + amount;
            if (newPaid > total + 0.001) {
                throw new SQLException(String.format(
                    "Payment rejected: %s would exceed the total fee of %s (already paid %s).",
                    fmt(amount), fmt(total), fmt(paid)));
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO fee_payments (fee_id, amount, payment_date, mode, remarks) "
                  + "VALUES (?,?,?,?,?)")) {
                ps.setInt(1, feeId);
                ps.setDouble(2, amount);
                ps.setDate(3, Date.valueOf(date));
                ps.setString(4, mode);
                ps.setString(5, remarks);
                ps.executeUpdate();
            }
            cn.commit();
        } catch (SQLException ex) {
            if (cn != null) { try { cn.rollback(); } catch (SQLException ignored) { } }
            throw ex;
        } finally {
            if (cn != null) {
                try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignored) { }
            }
        }
    }

    public List<FeePayment> findPayments(int feeId) throws SQLException {
        String sql = "SELECT p.payment_id, p.fee_id, p.amount, p.payment_date, p.mode, p.remarks, "
                   + "       s.student_code, s.full_name "
                   + "FROM fee_payments p "
                   + "JOIN fees f ON f.fee_id = p.fee_id "
                   + "JOIN students s ON s.student_id = f.student_id "
                   + "WHERE p.fee_id = ? ORDER BY p.payment_date DESC, p.payment_id DESC";
        List<FeePayment> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, feeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FeePayment p = new FeePayment();
                    p.setPaymentId(rs.getInt("payment_id"));
                    p.setFeeId(rs.getInt("fee_id"));
                    p.setAmount(rs.getDouble("amount"));
                    Date d = rs.getDate("payment_date");
                    p.setPaymentDate(d == null ? null : d.toLocalDate());
                    p.setMode(rs.getString("mode"));
                    p.setRemarks(rs.getString("remarks"));
                    p.setStudentCode(rs.getString("student_code"));
                    p.setStudentName(rs.getString("full_name"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    public List<FeePayment> findAllPayments() throws SQLException {
        String sql = "SELECT p.payment_id, p.fee_id, p.amount, p.payment_date, p.mode, p.remarks, "
                   + "       s.student_code, s.full_name "
                   + "FROM fee_payments p "
                   + "JOIN fees f ON f.fee_id = p.fee_id "
                   + "JOIN students s ON s.student_id = f.student_id "
                   + "ORDER BY p.payment_date DESC, p.payment_id DESC";
        List<FeePayment> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FeePayment p = new FeePayment();
                p.setPaymentId(rs.getInt("payment_id"));
                p.setFeeId(rs.getInt("fee_id"));
                p.setAmount(rs.getDouble("amount"));
                Date d = rs.getDate("payment_date");
                p.setPaymentDate(d == null ? null : d.toLocalDate());
                p.setMode(rs.getString("mode"));
                p.setRemarks(rs.getString("remarks"));
                p.setStudentCode(rs.getString("student_code"));
                p.setStudentName(rs.getString("full_name"));
                list.add(p);
            }
        }
        return list;
    }

    public boolean deletePayment(int paymentId) throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "DELETE FROM fee_payments WHERE payment_id=?")) {
            ps.setInt(1, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    public double totalCollected() throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT COALESCE(SUM(paid_amount),0) FROM fees");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    public double totalPending() throws SQLException {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT COALESCE(SUM(remaining_amount),0) FROM fees");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private static String fmt(double d) { return String.format("%.2f", d); }

    private Fee map(ResultSet rs) throws SQLException {
        Fee f = new Fee();
        f.setFeeId(rs.getInt("fee_id"));
        f.setStudentId(rs.getInt("student_id"));
        f.setTotalFee(rs.getDouble("total_fee"));
        f.setPaidAmount(rs.getDouble("paid_amount"));
        f.setRemainingAmount(rs.getDouble("remaining_amount"));
        f.setPaymentStatus(rs.getString("payment_status"));
        f.setStudentCode(rs.getString("student_code"));
        f.setStudentName(rs.getString("full_name"));
        f.setCourseName(rs.getString("course_name"));
        return f;
    }
}
