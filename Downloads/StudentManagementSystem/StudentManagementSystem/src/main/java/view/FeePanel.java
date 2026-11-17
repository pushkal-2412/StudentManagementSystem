package view;

import controller.FeeController;
import model.Fee;
import model.FeePayment;
import model.TableData;
import utils.ExportUtil;
import utils.MoneyUtil;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Fee module: accounts, balances, payments and receipt history. */
public class FeePanel extends JPanel implements Refreshable {

    private final FeeController controller = new FeeController();

    private final JTextField searchField = UITheme.field(16);
    private final JComboBox<String> statusFilter =
            UITheme.combo("ALL", "PAID", "PENDING", "PARTIALLY_PAID");
    private final DefaultTableModel feeModel;
    private final JTable feeTable;
    private final DefaultTableModel payModel;
    private final JTable payTable;
    private final JLabel totals = UITheme.subtitle(" ");

    private List<Fee> current = new ArrayList<>();
    private List<FeePayment> payments = new ArrayList<>();

    public FeePanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        feeModel = new DefaultTableModel(FeeController.FEE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        feeTable = new JTable(feeModel);
        UITheme.styleTable(feeTable);
        feeTable.getColumnModel().getColumn(0).setMaxWidth(70);
        feeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadPayments();
        });

        payModel = new DefaultTableModel(FeeController.PAYMENT_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payTable = new JTable(payModel);
        UITheme.styleTable(payTable);

        add(buildTop(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(totals, BorderLayout.SOUTH);
    }

    private JPanel buildTop() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        JPanel filters = UITheme.card();
        filters.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton search = UITheme.primary("Search");
        search.addActionListener(e -> loadFees());
        searchField.addActionListener(e -> loadFees());
        JButton reset = UITheme.neutral("Reset");
        reset.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            loadFees();
        });
        filters.add(new JLabel("Search"));
        filters.add(searchField);
        filters.add(new JLabel("Status"));
        filters.add(statusFilter);
        filters.add(search);
        filters.add(reset);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton pay = UITheme.success("Record payment");
        pay.addActionListener(e -> recordPayment());
        JButton del = UITheme.danger("Delete payment");
        del.addActionListener(e -> deletePayment());
        JButton csv = UITheme.neutral("Export fees CSV");
        csv.addActionListener(e -> exportFees(true));
        JButton pdf = UITheme.neutral("Export fees PDF");
        pdf.addActionListener(e -> exportFees(false));

        actions.add(pay);
        actions.add(del);
        actions.add(csv);
        actions.add(pdf);

        wrap.add(filters, BorderLayout.NORTH);
        wrap.add(actions, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSplit() {
        JPanel topCard = UITheme.card();
        topCard.setLayout(new BorderLayout());
        topCard.add(UITheme.scroll(feeTable), BorderLayout.CENTER);

        JPanel bottomCard = UITheme.card();
        bottomCard.setLayout(new BorderLayout(0, 8));
        JLabel t = new JLabel("Payment history for the selected student");
        t.setFont(UITheme.h3());
        bottomCard.add(t, BorderLayout.NORTH);
        bottomCard.add(UITheme.scroll(payTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topCard, bottomCard);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(10);

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(split, BorderLayout.CENTER);
        return holder;
    }

    @Override
    public void refresh() { loadFees(); }

    private void loadFees() {
        try {
            current = controller.search(searchField.getText(),
                    (String) statusFilter.getSelectedItem());
            feeModel.setRowCount(0);
            for (Object[] r : controller.rows(current)) feeModel.addRow(r);
            double collected = 0;
            double pending = 0;
            for (Fee f : current) {
                collected += f.getPaidAmount();
                pending += f.getRemainingAmount();
            }
            totals.setText("Accounts: " + current.size()
                    + "     Collected: " + MoneyUtil.format(collected)
                    + "     Outstanding: " + MoneyUtil.format(pending));
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load fee accounts.\n\n" + ex.getMessage());
        }
    }

    private Fee selectedFee() {
        int vr = feeTable.getSelectedRow();
        if (vr < 0) return null;
        int mr = feeTable.convertRowIndexToModel(vr);
        int id = Integer.parseInt(String.valueOf(feeModel.getValueAt(mr, 0)));
        return current.stream().filter(f -> f.getFeeId() == id).findFirst().orElse(null);
    }

    private void loadPayments() {
        Fee f = selectedFee();
        if (f == null) return;
        try {
            payments = controller.payments(f.getFeeId());
            payModel.setRowCount(0);
            for (Object[] r : controller.paymentRows(payments)) payModel.addRow(r);
        } catch (SQLException ex) {
            Dialogs.error(this, "Could not load payments.\n\n" + ex.getMessage());
        }
    }

    private void recordPayment() {
        Fee f = selectedFee();
        if (f == null) {
            Dialogs.warn(this, "Select a student's fee row first.");
            return;
        }
        if (f.getRemainingAmount() <= 0) {
            Dialogs.info(this, f.getStudentName() + " has already paid the full fee of "
                    + MoneyUtil.format(f.getTotalFee()) + ".");
            return;
        }
        JSpinner amount = new JSpinner(new SpinnerNumberModel(
                f.getRemainingAmount(), 0.0, f.getRemainingAmount(), 100.0));
        amount.setEditor(new JSpinner.NumberEditor(amount, "0.00"));
        JSpinner date = UITheme.dateSpinner(LocalDate.now());
        JComboBox<String> mode = UITheme.combo("CASH", "CARD", "UPI", "NET_BANKING", "CHEQUE");
        JTextField remarks = UITheme.field(18);

        JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Student: " + f.getStudentCode() + " - " + f.getStudentName()));
        p.add(new JLabel("Total fee " + MoneyUtil.format(f.getTotalFee())
                + "   |   paid " + MoneyUtil.format(f.getPaidAmount())
                + "   |   balance " + MoneyUtil.format(f.getRemainingAmount())));
        p.add(new JLabel("Amount to pay"));
        p.add(amount);
        p.add(new JLabel("Payment date"));
        p.add(date);
        p.add(new JLabel("Mode"));
        p.add(mode);
        p.add(new JLabel("Remarks"));
        p.add(remarks);

        int r = JOptionPane.showConfirmDialog(this, p, "Record fee payment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            double value = ((Number) amount.getValue()).doubleValue();
            controller.pay(f.getFeeId(), value, UITheme.spinnerDate(date),
                    (String) mode.getSelectedItem(), remarks.getText(),
                    SessionManager.getInstance().getCurrentAdmin().getAdminId());
            Dialogs.success(this, "Payment of " + MoneyUtil.format(value)
                    + " recorded for " + f.getStudentName() + ".");
            loadFees();
            loadPayments();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Payment rejected.\n\n" + ex.getMessage());
        }
    }

    private void deletePayment() {
        int vr = payTable.getSelectedRow();
        if (vr < 0) {
            Dialogs.warn(this, "Select a payment receipt first.");
            return;
        }
        int mr = payTable.convertRowIndexToModel(vr);
        int id = Integer.parseInt(String.valueOf(payModel.getValueAt(mr, 0)));
        if (!Dialogs.confirm(this, "Delete receipt #" + id + "?\n\n"
                + "The fee balance is recalculated automatically.")) return;
        try {
            controller.deletePayment(id);
            Dialogs.success(this, "Payment deleted and balance recalculated.");
            loadFees();
            loadPayments();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error.\n\n" + ex.getMessage());
        }
    }

    private void exportFees(boolean csv) {
        if (current.isEmpty()) {
            Dialogs.warn(this, "Nothing to export.");
            return;
        }
        try {
            TableData t = controller.feeTable(current);
            File f = csv
                ? ExportUtil.exportCsv(this, t, "fees")
                : ExportUtil.exportPdf(this, t, "Fee Payment Report",
                        "Total fee, paid amount, balance and payment status");
            if (f != null) ExportUtil.confirmSaved(this, f);
        } catch (Exception ex) {
            Dialogs.error(this, "Export failed.\n\n" + ex.getMessage());
        }
    }
}
