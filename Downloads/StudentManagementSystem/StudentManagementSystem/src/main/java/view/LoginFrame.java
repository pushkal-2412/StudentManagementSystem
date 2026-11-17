package view;

import controller.LoginController;
import model.Admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/** Secure login screen with BCrypt authentication and a database health indicator. */
public class LoginFrame extends JFrame {

    private final LoginController controller = new LoginController();
    private final JTextField usernameField = UITheme.field(20);
    private final JPasswordField passwordField = UITheme.passwordField(20);
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton loginButton = UITheme.primary("Sign in");
    private final JLabel dbStatus = new JLabel(" ");

    public LoginFrame() {
        setTitle("Student Management System  \u2022  Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(880, 540);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(loginButton);
        checkDatabase();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        root.setBackground(Color.WHITE);

        // ---------- left brand panel ----------
        JPanel brand = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, UITheme.SIDEBAR,
                        getWidth(), getHeight(), new Color(30, 64, 175));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-90, getHeight() - 200, 320, 320);
                g2.fillOval(getWidth() - 130, -80, 240, 240);
                g2.dispose();
            }
        };
        brand.setOpaque(true);

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.setBorder(new EmptyBorder(0, 44, 0, 44));

        JLabel mark = new JLabel("SMS");
        mark.setFont(UITheme.font(Font.BOLD, 13f));
        mark.setForeground(new Color(147, 197, 253));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel("<html>Student<br>Management<br>System</html>");
        name.setFont(UITheme.font(Font.BOLD, 30f));
        name.setForeground(Color.WHITE);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tag = new JLabel("<html>Java Swing \u2022 JDBC \u2022 MySQL<br>"
                + "MVC architecture for academic administration</html>");
        tag.setFont(UITheme.font(Font.PLAIN, 12.5f));
        tag.setForeground(new Color(203, 213, 225));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandText.add(mark);
        brandText.add(Box.createVerticalStrut(14));
        brandText.add(name);
        brandText.add(Box.createVerticalStrut(16));
        brandText.add(tag);
        brand.add(brandText);

        // ---------- right form panel ----------
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(0, 46, 0, 46));
        box.setMaximumSize(new Dimension(340, 420));

        JLabel hello = new JLabel("Welcome back");
        hello.setFont(UITheme.font(Font.BOLD, 22f));
        hello.setForeground(UITheme.TEXT);
        hello.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to manage students, courses and fees.");
        sub.setFont(UITheme.small());
        sub.setForeground(UITheme.MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ul = new JLabel("Username");
        ul.setFont(UITheme.font(Font.BOLD, 11.5f));
        ul.setForeground(UITheme.MUTED);
        ul.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pl = new JLabel("Password");
        pl.setFont(UITheme.font(Font.BOLD, 11.5f));
        pl.setForeground(UITheme.MUTED);
        pl.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginButton.addActionListener(this::doLogin);

        messageLabel.setFont(UITheme.small());
        messageLabel.setForeground(UITheme.DANGER);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("<html><b>Default login:</b> admin / admin123<br>"
                + "<span style='color:#64748b'>Change it from File \u2192 Change password "
                + "after signing in.</span></html>");
        hint.setFont(UITheme.tiny());
        hint.setForeground(UITheme.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        dbStatus.setFont(UITheme.tiny());
        dbStatus.setForeground(UITheme.MUTED);
        dbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        box.add(hello);
        box.add(Box.createVerticalStrut(4));
        box.add(sub);
        box.add(Box.createVerticalStrut(22));
        box.add(ul);
        box.add(Box.createVerticalStrut(5));
        box.add(usernameField);
        box.add(Box.createVerticalStrut(14));
        box.add(pl);
        box.add(Box.createVerticalStrut(5));
        box.add(passwordField);
        box.add(Box.createVerticalStrut(8));
        box.add(messageLabel);
        box.add(Box.createVerticalStrut(6));
        box.add(loginButton);
        box.add(Box.createVerticalStrut(18));
        box.add(hint);
        box.add(Box.createVerticalStrut(8));
        box.add(dbStatus);

        form.add(box);
        root.add(brand);
        root.add(form);
        return root;
    }

    private void checkDatabase() {
        String error = controller.checkDatabase();
        if (error == null) {
            dbStatus.setForeground(UITheme.SUCCESS);
            dbStatus.setText("\u25CF  Database connected");
        } else {
            dbStatus.setForeground(UITheme.DANGER);
            dbStatus.setText("\u25CF  Database unavailable - check MySQL and application.properties");
        }
    }

    private void doLogin(ActionEvent e) {
        messageLabel.setText(" ");
        loginButton.setEnabled(false);
        loginButton.setText("Signing in\u2026");

        String user = usernameField.getText();
        char[] pwd = passwordField.getPassword();

        SwingWorker<Admin, Void> worker = new SwingWorker<>() {
            @Override
            protected Admin doInBackground() throws Exception {
                return controller.login(user, pwd);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                loginButton.setText("Sign in");
                try {
                    Admin admin = get();
                    new MainFrame(admin).setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    String msg = cause.getMessage() == null
                            ? "Login failed." : cause.getMessage();
                    messageLabel.setText("<html>"
                            + msg.replace("\n", "<br>") + "</html>");
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                } finally {
                    java.util.Arrays.fill(pwd, '0');
                }
            }
        };
        worker.execute();
    }
}
