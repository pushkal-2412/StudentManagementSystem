package view;

import controller.LoginController;
import service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Lets the logged-in administrator rotate their password (BCrypt re-hash). */
public class ChangePasswordDialog extends JDialog {

    private final LoginController controller = new LoginController();
    private final JPasswordField oldPwd = UITheme.passwordField(18);
    private final JPasswordField newPwd = UITheme.passwordField(18);
    private final JPasswordField confirmPwd = UITheme.passwordField(18);

    public ChangePasswordDialog(Window owner) {
        super(owner, "Change password", ModalityType.APPLICATION_MODAL);
        setSize(430, 330);
        setLocationRelativeTo(owner);
        setContentPane(build());
    }

    private JPanel build() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 6, 0);

        JLabel title = UITheme.title("Change password");
        title.setFont(UITheme.h2());
        root.add(title, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 16, 0);
        root.add(UITheme.subtitle("Passwords are stored as BCrypt hashes, never as plain text."), gc);

        addRow(root, gc, "Current password", oldPwd);
        addRow(root, gc, "New password (min 6 characters)", newPwd);
        addRow(root, gc, "Confirm new password", confirmPwd);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = UITheme.neutral("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = UITheme.primary("Update password");
        save.addActionListener(e -> save());
        buttons.add(cancel);
        buttons.add(save);

        gc.gridy++;
        gc.insets = new Insets(10, 0, 0, 0);
        root.add(buttons, gc);
        getRootPane().setDefaultButton(save);
        return root;
    }

    private void addRow(JPanel root, GridBagConstraints gc, String label, JComponent field) {
        gc.gridy++;
        gc.insets = new Insets(0, 0, 4, 0);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.font(Font.BOLD, 11.5f));
        l.setForeground(UITheme.MUTED);
        root.add(l, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 12, 0);
        root.add(field, gc);
    }

    private void save() {
        try {
            controller.changePassword(new String(oldPwd.getPassword()),
                    new String(newPwd.getPassword()),
                    new String(confirmPwd.getPassword()));
            Dialogs.success(this, "Password updated successfully.\n"
                    + "Use the new password the next time you sign in.");
            dispose();
        } catch (AuthService.AuthException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
