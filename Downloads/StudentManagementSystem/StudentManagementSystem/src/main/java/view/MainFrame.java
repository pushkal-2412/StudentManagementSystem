package view;

import model.Admin;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Application shell: sidebar navigation + card layout content area. */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel pageSubtitle = new JLabel(" ");
    private final Admin admin;

    public MainFrame(Admin admin) {
        this.admin = admin;
        setTitle("Student Management System  \u2022  " + admin.getFullName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1360, 820);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        setJMenuBar(buildMenu());

        registerPage("Dashboard", new DashboardPanel());
        registerPage("Students", new StudentPanel());
        registerPage("Courses", new CoursePanel());
        registerPage("Departments", new DepartmentPanel());
        registerPage("Attendance", new AttendancePanel());
        registerPage("Examinations", new ExamPanel());
        registerPage("Fees", new FeePanel());
        registerPage("Reports", new ReportPanel());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(), BorderLayout.CENTER);
        setContentPane(root);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { confirmExit(); }
        });

        show("Dashboard");
    }

    private void registerPage(String name, JPanel panel) {
        pages.put(name, panel);
        content.add(panel, name);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(Color.WHITE);

        JMenu file = new JMenu("File");
        file.setFont(UITheme.body());
        JMenuItem changePwd = new JMenuItem("Change password\u2026");
        changePwd.addActionListener(e -> new ChangePasswordDialog(this).setVisible(true));
        JMenuItem logout = new JMenuItem("Logout");
        logout.addActionListener(e -> doLogout());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> confirmExit());
        file.add(changePwd);
        file.addSeparator();
        file.add(logout);
        file.add(exit);

        JMenu help = new JMenu("Help");
        help.setFont(UITheme.body());
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> Dialogs.info(this,
            "Student Management System 1.0.0\n\n"
          + "Java Swing + JDBC + MySQL, MVC architecture.\n"
          + "BCrypt password hashing, PreparedStatement everywhere.\n\n"
          + "Signed in as: " + admin.getUsername() + " (" + admin.getRole() + ")"));
        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(UITheme.SIDEBAR);
        side.setPreferredSize(new Dimension(232, 0));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(22, 22, 18, 22));

        JLabel logo = new JLabel("STUDENT MS");
        logo.setFont(UITheme.font(Font.BOLD, 15f));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tag = new JLabel("Academic administration");
        tag.setFont(UITheme.tiny());
        tag.setForeground(new Color(148, 163, 184));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalStrut(3));
        brand.add(tag);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(4, 12, 12, 12));

        addNav(nav, "Dashboard", "\u25A6");
        addNav(nav, "Students", "\u263A");
        addNav(nav, "Courses", "\u2261");
        addNav(nav, "Departments", "\u25A4");
        addNav(nav, "Attendance", "\u2713");
        addNav(nav, "Examinations", "\u270E");
        addNav(nav, "Fees", "\u20B9");
        addNav(nav, "Reports", "\u25A4");

        JPanel user = new JPanel();
        user.setOpaque(false);
        user.setLayout(new BoxLayout(user, BoxLayout.Y_AXIS));
        user.setBorder(new EmptyBorder(14, 22, 20, 22));

        JLabel name = new JLabel(admin.getFullName());
        name.setFont(UITheme.font(Font.BOLD, 12f));
        name.setForeground(Color.WHITE);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel role = new JLabel(admin.getRole() + "  \u2022  " + admin.getUsername());
        role.setFont(UITheme.tiny());
        role.setForeground(new Color(148, 163, 184));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton logout = new JButton("Logout");
        logout.setFont(UITheme.font(Font.BOLD, 11.5f));
        logout.setForeground(new Color(226, 232, 240));
        logout.setBackground(UITheme.SIDEBAR_HOVER);
        logout.setOpaque(true);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        logout.addActionListener(e -> doLogout());

        user.add(name);
        user.add(Box.createVerticalStrut(2));
        user.add(role);
        user.add(Box.createVerticalStrut(10));
        user.add(logout);

        side.add(brand, BorderLayout.NORTH);
        side.add(nav, BorderLayout.CENTER);
        side.add(user, BorderLayout.SOUTH);
        return side;
    }

    private void addNav(JPanel nav, String name, String glyph) {
        JButton b = new JButton(glyph + "   " + name);
        b.setFont(UITheme.font(Font.BOLD, 12.5f));
        b.setForeground(new Color(203, 213, 225));
        b.setBackground(UITheme.SIDEBAR);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(9, 14, 9, 14));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.addActionListener(e -> show(name));
        navButtons.put(name, b);
        nav.add(b);
        nav.add(Box.createVerticalStrut(2));
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UITheme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(14, 24, 14, 24)));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        pageTitle.setFont(UITheme.h2());
        pageTitle.setForeground(UITheme.TEXT);
        pageSubtitle.setFont(UITheme.tiny());
        pageSubtitle.setForeground(UITheme.MUTED);
        titles.add(pageTitle);
        titles.add(Box.createVerticalStrut(2));
        titles.add(pageSubtitle);

        JLabel stamp = new JLabel("Session started "
                + (SessionManager.getInstance().getLoginTime() == null ? "-"
                   : SessionManager.getInstance().getLoginTime()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))));
        stamp.setFont(UITheme.tiny());
        stamp.setForeground(UITheme.MUTED);

        header.add(titles, BorderLayout.WEST);
        header.add(stamp, BorderLayout.EAST);

        content.setBackground(UITheme.BG);
        main.add(header, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        main.add(buildStatusBar(), BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
                new EmptyBorder(6, 24, 6, 24)));

        JLabel db = new JLabel("\u25CF  " + database.DBConnection.describeTarget());
        db.setFont(UITheme.tiny());
        db.setForeground(UITheme.MUTED);

        JLabel roles = new JLabel("Role: " + SessionManager.getInstance().getRole()
                + "   |   Session secured with BCrypt");
        roles.setFont(UITheme.tiny());
        roles.setForeground(UITheme.MUTED);

        bar.add(db, BorderLayout.WEST);
        bar.add(roles, BorderLayout.EAST);
        return bar;
    }

    private void show(String name) {
        cardLayout.show(content, name);
        pageTitle.setText(name);
        pageSubtitle.setText(subtitleFor(name));
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean on = e.getKey().equals(name);
            JButton b = e.getValue();
            b.setBackground(on ? UITheme.PRIMARY : UITheme.SIDEBAR);
            b.setForeground(on ? Color.WHITE : new Color(203, 213, 225));
        }
        JPanel p = pages.get(name);
        if (p instanceof Refreshable r) {
            r.refresh();
        }
    }

    private String subtitleFor(String name) {
        return switch (name) {
            case "Dashboard" -> "Institution overview, KPIs and recent registrations";
            case "Students" -> "Add, edit, delete and search student records";
            case "Courses" -> "Course catalogue and student assignment";
            case "Departments" -> "Academic departments and heads of department";
            case "Attendance" -> "Daily marking, percentages and monthly reports";
            case "Examinations" -> "Exams, marks entry, results and grades";
            case "Fees" -> "Fee accounts, payments and receipts";
            case "Reports" -> "Export department, course, attendance, exam and fee reports";
            default -> " ";
        };
    }

    private void doLogout() {
        if (!Dialogs.confirm(this, "End this session and return to the login screen?")) return;
        SessionManager.getInstance().logout();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private void confirmExit() {
        if (Dialogs.confirm(this, "Close the application?")) {
            SessionManager.getInstance().logout();
            dispose();
            System.exit(0);
        }
    }
}
