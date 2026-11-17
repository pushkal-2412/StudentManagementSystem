package controller;

import model.Fee;
import model.FeePayment;
import model.TableData;
import service.FeeService;
import utils.DateUtil;
import utils.MoneyUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Mediates between the fee views and the service layer. */
public class FeeController {

    private final FeeService service = new FeeService();

    public static final String[] FEE_COLUMNS = {
        "Fee ID", "Student ID", "Student Name", "Course", "Total Fee",
        "Paid", "Balance", "Status"
    };
    public static final String[] PAYMENT_COLUMNS = {
        "Receipt", "Student ID", "Student Name", "Amount", "Date", "Mode", "Remarks"
    };

    public List<Fee> search(String keyword, String status) throws SQLException {
        return service.search(keyword, status);
    }
    public List<FeePayment> payments(int feeId) throws SQLException {
        return service.payments(feeId);
    }
    public List<FeePayment> allPayments() throws SQLException { return service.allPayments(); }
    public void pay(int feeId, double amount, LocalDate date, String mode, String remarks,
                    int adminId) throws SQLException {
        service.pay(feeId, amount, date, mode, remarks, adminId);
    }
    public void deletePayment(int paymentId) throws SQLException {
        service.deletePayment(paymentId);
    }

    public double collected() throws SQLException { return service.collected(); }
    public double pending() throws SQLException { return service.pending(); }

    public Object[][] rows(List<Fee> list) {
        Object[][] data = new Object[list.size()][FEE_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            Fee f = list.get(i);
            data[i][0] = f.getFeeId();
            data[i][1] = f.getStudentCode();
            data[i][2] = f.getStudentName();
            data[i][3] = f.getCourseName();
            data[i][4] = MoneyUtil.format(f.getTotalFee());
            data[i][5] = MoneyUtil.format(f.getPaidAmount());
            data[i][6] = MoneyUtil.format(f.getRemainingAmount());
            data[i][7] = f.getPaymentStatus().replace('_', ' ');
        }
        return data;
    }

    public Object[][] paymentRows(List<FeePayment> list) {
        Object[][] data = new Object[list.size()][PAYMENT_COLUMNS.length];
        for (int i = 0; i < list.size(); i++) {
            FeePayment p = list.get(i);
            data[i][0] = p.getPaymentId();
            data[i][1] = p.getStudentCode();
            data[i][2] = p.getStudentName();
            data[i][3] = MoneyUtil.format(p.getAmount());
            data[i][4] = DateUtil.format(p.getPaymentDate());
            data[i][5] = p.getMode();
            data[i][6] = p.getRemarks();
        }
        return data;
    }

    public TableData feeTable(List<Fee> list) {
        TableData t = new TableData();
        for (String c : FEE_COLUMNS) t.getHeaders().add(c);
        for (Fee f : list) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(f.getFeeId()));
            row.add(f.getStudentCode());
            row.add(f.getStudentName());
            row.add(f.getCourseName());
            row.add(MoneyUtil.plain(f.getTotalFee()));
            row.add(MoneyUtil.plain(f.getPaidAmount()));
            row.add(MoneyUtil.plain(f.getRemainingAmount()));
            row.add(f.getPaymentStatus().replace('_', ' '));
            t.addRow(row);
        }
        return t;
    }

    public TableData paymentTable(List<FeePayment> list) {
        TableData t = new TableData();
        for (String c : PAYMENT_COLUMNS) t.getHeaders().add(c);
        for (FeePayment p : list) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(p.getPaymentId()));
            row.add(p.getStudentCode());
            row.add(p.getStudentName());
            row.add(MoneyUtil.plain(p.getAmount()));
            row.add(DateUtil.iso(p.getPaymentDate()));
            row.add(p.getMode());
            row.add(p.getRemarks());
            t.addRow(row);
        }
        return t;
    }
}
