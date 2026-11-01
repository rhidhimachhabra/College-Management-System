import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class DBConnection extends JFrame {

    static final String URL = "jdbc:mysql://localhost:3306/college_management_v2";
    static final String DB_USER = System.getenv("DB_USER");
    static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    Connection con;

    public DBConnection() {
        setTitle("College Management System");
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        connectDatabase();

        if (con == null) {
            return;
        }

        createTables();
        showHomeScreen();
    }

    void connectDatabase() {
        try {
            if (DB_USER == null || DB_USER.isBlank()
                    || DB_PASSWORD == null || DB_PASSWORD.isBlank()) {
                throw new SQLException(
                        "DB_USER or DB_PASSWORD environment variable is missing.");
            }

            con = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Database connection failed!\n" + e.getMessage());
        }
    }

    void createTables() {
        if (con == null) {
            return;
        }

        String[] queries = {
            """
            CREATE TABLE IF NOT EXISTS students (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                roll_no VARCHAR(20) UNIQUE NOT NULL,
                department VARCHAR(50),
                email VARCHAR(100),
                phone VARCHAR(15),
                username VARCHAR(50) UNIQUE,
                password_hash VARCHAR(64)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS admins (
                id INT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(64) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS faculty (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                department VARCHAR(50),
                email VARCHAR(100),
                phone VARCHAR(15)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS attendance (
                id INT PRIMARY KEY AUTO_INCREMENT,
                student_id INT,
                attendance_date DATE,
                status VARCHAR(10),
                FOREIGN KEY (student_id)
                    REFERENCES students(id)
                    ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS results (
                id INT PRIMARY KEY AUTO_INCREMENT,
                student_id INT,
                subject VARCHAR(100),
                marks DOUBLE,
                FOREIGN KEY (student_id)
                    REFERENCES students(id)
                    ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS fees (
                id INT PRIMARY KEY AUTO_INCREMENT,
                student_id INT,
                amount DOUBLE,
                status VARCHAR(20),
                FOREIGN KEY (student_id)
                    REFERENCES students(id)
                    ON DELETE CASCADE
            )
            """
        };

        try (Statement st = con.createStatement()) {
            for (String query : queries) {
                st.executeUpdate(query);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Table creation error:\n" + e.getMessage());
        }
    }

    void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    void executeUpdate(String sql, Object... values) {
        if (con == null) {
            message("Database is not connected.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            message("Database error:\n" + e.getMessage());
        }
    }

    void showTable(String title, String sql, Object... values) {
        if (con == null) {
            message("Database is not connected.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();

                DefaultTableModel model = new DefaultTableModel();

                for (int i = 1; i <= count; i++) {
                    model.addColumn(meta.getColumnName(i));
                }

                while (rs.next()) {
                    Object[] row = new Object[count];

                    for (int i = 1; i <= count; i++) {
                        row[i - 1] = rs.getObject(i);
                    }

                    model.addRow(row);
                }

                JTable table = new JTable(model);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

                JScrollPane scrollPane = new JScrollPane(table);
                scrollPane.setPreferredSize(new Dimension(750, 350));

                JOptionPane.showMessageDialog(
                        this,
                        scrollPane,
                        title,
                        JOptionPane.PLAIN_MESSAGE);
            }

        } catch (SQLException e) {
            message("Unable to display records:\n" + e.getMessage());
        }
    }

    void showHomeScreen() {
        getContentPane().removeAll();
        setLayout(new GridLayout(5, 1, 10, 10));

        JLabel heading = new JLabel(
                "COLLEGE MANAGEMENT SYSTEM",
                SwingConstants.CENTER);

        heading.setFont(new Font("Arial", Font.BOLD, 23));

        JButton adminLogin = new JButton("Admin Login");
        JButton studentLogin = new JButton("Student Login");
        JButton createAccount = new JButton("Create Student Account");
        JButton firstAdmin = new JButton("Set Up First Admin");

        add(heading);
        add(adminLogin);
        add(studentLogin);
        add(createAccount);
        add(firstAdmin);

        adminLogin.addActionListener(e -> adminLogin());
        studentLogin.addActionListener(e -> studentLogin());
        createAccount.addActionListener(e -> createStudentAccount());
        firstAdmin.addActionListener(e -> firstAdminSetup());

        revalidate();
        repaint();
        setVisible(true);
    }

    boolean adminExists() {
        if (con == null) {
            return true;
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM admins")) {

            rs.next();
            return rs.getInt(1) > 0;

        } catch (SQLException e) {
            message(e.getMessage());
            return true;
        }
    }

    void firstAdminSetup() {
        if (con == null) {
            message("Database is not connected.");
            return;
        }

        if (adminExists()) {
            message("An administrator already exists. Please use Admin Login.");
            return;
        }

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JPasswordField confirm = new JPasswordField();

        Object[] fields = {
            "Choose Admin Username:", username,
            "Choose Password:", password,
            "Confirm Password:", confirm
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "First Admin Setup",
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String confirmPass = new String(confirm.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            message("Username and password are required.");
            return;
        }

        if (!pass.equals(confirmPass)) {
            message("Passwords do not match.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO admins(username, password_hash) VALUES (?, ?)")) {

            ps.setString(1, user);
            ps.setString(2, pass);
            ps.executeUpdate();

            message("First administrator account created.");

        } catch (SQLException e) {
            message("Unable to create administrator:\n" + e.getMessage());
        }
    }

    void adminLogin() {
        if (con == null) {
            message("Database is not connected.");
            return;
        }

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        Object[] fields = {
            "Admin Username:", username,
            "Password:", password
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Admin Login",
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String user = username.getText().trim();
        String pass = new String(password.getPassword());

        String sql = "SELECT id FROM admins WHERE username = ? AND password_hash = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    adminDashboard();
                } else {
                    message("Invalid admin username or password.");
                }
            }

        } catch (SQLException e) {
            message(e.getMessage());
        }
    }

    void createAdmin() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JPasswordField confirm = new JPasswordField();

        Object[] fields = {
            "New Admin Username:", username,
            "Password:", password,
            "Confirm Password:", confirm
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Create Admin Account",
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String confirmPass = new String(confirm.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            message("All fields are required.");
            return;
        }

        if (!pass.equals(confirmPass)) {
            message("Passwords do not match.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO admins(username, password_hash) VALUES (?, ?)")) {

            ps.setString(1, user);
            ps.setString(2, pass);
            ps.executeUpdate();

            message("New administrator created successfully.");

        } catch (SQLException e) {
            message("Unable to create administrator.\n"
                    + "The username may already exist.\n" + e.getMessage());
        }
    }

    void adminDashboard() {
        getContentPane().removeAll();
        setLayout(new GridLayout(9, 1, 5, 5));

        JLabel heading = new JLabel("ADMIN DASHBOARD", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));

        JButton addStudent = new JButton("Add Student");
        JButton manageStudents = new JButton("Manage Students");
        JButton manageFaculty = new JButton("Manage Faculty");
        JButton manageAttendance = new JButton("Manage Attendance");
        JButton manageResults = new JButton("Manage Results");
        JButton manageFees = new JButton("Manage Fees");
        JButton manageAdmins = new JButton("Manage Admin Accounts");
        JButton logout = new JButton("Logout");

        add(heading);
        add(addStudent);
        add(manageStudents);
        add(manageFaculty);
        add(manageAttendance);
        add(manageResults);
        add(manageFees);
        add(manageAdmins);
        add(logout);

        addStudent.addActionListener(e -> addStudent());
        manageStudents.addActionListener(e -> manageStudents());
        manageFaculty.addActionListener(e -> manageFaculty());
        manageAttendance.addActionListener(e -> manageAttendance());
        manageResults.addActionListener(e -> manageResults());
        manageFees.addActionListener(e -> manageFees());

        manageAdmins.addActionListener(e -> {
            String[] options = {"Create Admin", "View Admins"};

            int choice = JOptionPane.showOptionDialog(
                    this, "Choose an operation",
                    "Manage Admin Accounts",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);

            if (choice == 0) {
                createAdmin();
            } else if (choice == 1) {
                showTable("Administrator Accounts",
                        "SELECT id, username FROM admins");
            }
        });

        logout.addActionListener(e -> showHomeScreen());

        revalidate();
        repaint();
    }

    void addStudent() {
        JTextField name = new JTextField();
        JTextField roll = new JTextField();
        JTextField department = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();

        Object[] fields = {
            "Student Name:", name,
            "Roll Number:", roll,
            "Department:", department,
            "Email:", email,
            "Phone:", phone
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Add Student",
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        if (name.getText().trim().isEmpty()
                || roll.getText().trim().isEmpty()) {
            message("Student name and roll number are required.");
            return;
        }

        String sql = "INSERT INTO students "
                + "(name, roll_no, department, email, phone) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name.getText().trim());
            ps.setString(2, roll.getText().trim());
            ps.setString(3, department.getText().trim());
            ps.setString(4, email.getText().trim());
            ps.setString(5, phone.getText().trim());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    message("Student added successfully!\nStudent ID: "
                            + keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            message("Unable to add student.\n"
                    + "The roll number may already exist.\n" + e.getMessage());
        }
    }

    void manageStudents() {
        String[] options = {"View Students", "Delete Student"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an operation", "Manage Students",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            showTable("Student Records",
                    "SELECT id, name, roll_no, department, email, phone FROM students");

        } else if (choice == 1) {
            String input = JOptionPane.showInputDialog(
                    this, "Enter Student ID to delete:");

            if (input == null) {
                return;
            }

            try {
                int id = Integer.parseInt(input.trim());

                int confirm = JOptionPane.showConfirmDialog(
                        this, "Delete student " + id + " and related records?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    executeUpdate("DELETE FROM students WHERE id = ?", id);
                    message("Deletion request completed. Check the student records.");
                }

            } catch (NumberFormatException e) {
                message("Enter a valid numeric Student ID.");
            }
        }
    }

    void manageFaculty() {
        String[] options = {"Add Faculty", "View Faculty", "Delete Faculty"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an operation", "Faculty Management",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            JTextField name = new JTextField();
            JTextField dept = new JTextField();
            JTextField email = new JTextField();
            JTextField phone = new JTextField();

            Object[] fields = {
                "Name:", name,
                "Department:", dept,
                "Email:", email,
                "Phone:", phone
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Add Faculty",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            if (name.getText().trim().isEmpty()) {
                message("Faculty name is required.");
                return;
            }

            executeUpdate(
                    "INSERT INTO faculty (name, department, email, phone) VALUES (?, ?, ?, ?)",
                    name.getText().trim(),
                    dept.getText().trim(),
                    email.getText().trim(),
                    phone.getText().trim());

            message("Faculty record added.");

        } else if (choice == 1) {
            showTable("Faculty Records", "SELECT * FROM faculty");

        } else if (choice == 2) {
            deleteById("faculty", "Faculty");
        }
    }

    void manageAttendance() {
        String[] options = {"Mark Attendance", "View Attendance"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an operation", "Attendance Management",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            JTextField studentId = new JTextField();
            JTextField date = new JTextField(LocalDate.now().toString());

            String[] statuses = {"Present", "Absent"};
            JComboBox<String> status = new JComboBox<>(statuses);

            Object[] fields = {
                "Student ID:", studentId,
                "Date (YYYY-MM-DD):", date,
                "Status:", status
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Mark Attendance",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                int id = Integer.parseInt(studentId.getText().trim());
                Date attendanceDate = Date.valueOf(date.getText().trim());

                executeUpdate(
                        "INSERT INTO attendance (student_id, attendance_date, status) VALUES (?, ?, ?)",
                        id, attendanceDate, status.getSelectedItem());

                message("Attendance entry processed.");

            } catch (IllegalArgumentException e) {
                message("Enter a valid Student ID and date.");
            }

        } else if (choice == 1) {
            showTable("Attendance Records",
                    """
                    SELECT a.id, a.student_id, s.name,
                           a.attendance_date, a.status
                    FROM attendance a
                    JOIN students s ON a.student_id = s.id
                    ORDER BY a.attendance_date DESC
                    """);
        }
    }

    void manageResults() {
        String[] options = {"Add Result", "View Results"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an operation", "Results Management",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            JTextField studentId = new JTextField();
            JTextField subject = new JTextField();
            JTextField marks = new JTextField();

            Object[] fields = {
                "Student ID:", studentId,
                "Subject:", subject,
                "Marks (0-100):", marks
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Add Result",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                int id = Integer.parseInt(studentId.getText().trim());
                double score = Double.parseDouble(marks.getText().trim());

                if (subject.getText().trim().isEmpty()
                        || score < 0 || score > 100) {
                    message("Enter a subject and marks between 0 and 100.");
                    return;
                }

                executeUpdate(
                        "INSERT INTO results (student_id, subject, marks) VALUES (?, ?, ?)",
                        id, subject.getText().trim(), score);

                message("Result entry processed.");

            } catch (NumberFormatException e) {
                message("Enter a valid Student ID and marks.");
            }

        } else if (choice == 1) {
            showTable("Examination Results",
                    """
                    SELECT r.id, r.student_id, s.name, r.subject, r.marks
                    FROM results r
                    JOIN students s ON r.student_id = s.id
                    """);
        }
    }

    void manageFees() {
        String[] options = {"Record Fees", "View Fees"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an operation", "Fee Management",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            JTextField studentId = new JTextField();
            JTextField amount = new JTextField();

            String[] statuses = {"Paid", "Pending"};
            JComboBox<String> status = new JComboBox<>(statuses);

            Object[] fields = {
                "Student ID:", studentId,
                "Fee Amount:", amount,
                "Payment Status:", status
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Record Fees",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                int id = Integer.parseInt(studentId.getText().trim());
                double feeAmount = Double.parseDouble(amount.getText().trim());

                if (feeAmount < 0) {
                    message("Fee amount cannot be negative.");
                    return;
                }

                executeUpdate(
                        "INSERT INTO fees (student_id, amount, status) VALUES (?, ?, ?)",
                        id, feeAmount, status.getSelectedItem());

                message("Fee entry processed.");

            } catch (NumberFormatException e) {
                message("Enter a valid Student ID and amount.");
            }

        } else if (choice == 1) {
            showTable("Fee Records",
                    """
                    SELECT f.id, f.student_id, s.name, f.amount, f.status
                    FROM fees f
                    JOIN students s ON f.student_id = s.id
                    """);
        }
    }

    void deleteById(String table, String label) {
        String input = JOptionPane.showInputDialog(
                this, "Enter " + label + " ID to delete:");

        if (input == null) {
            return;
        }

        try {
            int id = Integer.parseInt(input.trim());

            int confirm = JOptionPane.showConfirmDialog(
                    this, "Are you sure you want to delete this record?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                executeUpdate("DELETE FROM " + table + " WHERE id = ?", id);
                message("Deletion request completed.");
            }

        } catch (NumberFormatException e) {
            message("Enter a valid numeric ID.");
        }
    }

    void createStudentAccount() {
        JTextField studentId = new JTextField();
        JTextField email = new JTextField();
        JPasswordField password = new JPasswordField();
        JPasswordField confirm = new JPasswordField();

        Object[] fields = {
            "Student ID:", studentId,
            "Registered Email:", email,
            "Choose Password:", password,
            "Confirm Password:", confirm
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Create Student Account",
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String emailText = email.getText().trim();
        String pass = new String(password.getPassword());
        String confirmPass = new String(confirm.getPassword());

        if (pass.isEmpty()) {
            message("Password cannot be empty.");
            return;
        }

        if (!pass.equals(confirmPass)) {
            message("Passwords do not match.");
            return;
        }

        try {
            int id = Integer.parseInt(studentId.getText().trim());

            String sql = "UPDATE students SET password_hash = ? "
                    + "WHERE id = ? AND email = ? AND password_hash IS NULL";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, pass);
                ps.setInt(2, id);
                ps.setString(3, emailText);

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    message("Student account created successfully!");
                } else {
                    message("Account could not be created.\n"
                            + "Check your Student ID and registered email. "
                            + "An account may already exist.");
                }
            }

        } catch (NumberFormatException e) {
            message("Student ID must be a number.");
        } catch (SQLException e) {
            message(e.getMessage());
        }
    }

    void studentLogin() {
        JTextField studentId = new JTextField();
        JPasswordField password = new JPasswordField();

        Object[] fields = {
            "Student ID:", studentId,
            "Password:", password
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Student Login",
                JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int id = Integer.parseInt(studentId.getText().trim());
            String pass = new String(password.getPassword());

            String sql = "SELECT id, name FROM students "
                    + "WHERE id = ? AND password_hash = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setString(2, pass);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        studentDashboard(rs.getInt("id"), rs.getString("name"));
                    } else {
                        message("Invalid Student ID or password.");
                    }
                }
            }

        } catch (NumberFormatException e) {
            message("Student ID must be a number.");
        } catch (SQLException e) {
            message(e.getMessage());
        }
    }

    void studentDashboard(int id, String name) {
        getContentPane().removeAll();
        setLayout(new GridLayout(6, 1, 10, 10));

        JLabel heading = new JLabel(
                "Welcome, " + name + " (ID: " + id + ")",
                SwingConstants.CENTER);

        heading.setFont(new Font("Arial", Font.BOLD, 20));

        JButton profile = new JButton("View Profile");
        JButton attendance = new JButton("View Attendance");
        JButton results = new JButton("View Results");
        JButton fees = new JButton("View Fees");
        JButton logout = new JButton("Logout");

        add(heading);
        add(profile);
        add(attendance);
        add(results);
        add(fees);
        add(logout);

        profile.addActionListener(e -> showTable(
                "My Profile",
                """
                SELECT id, name, roll_no, department, email, phone
                FROM students WHERE id = ?
                """, id));

        attendance.addActionListener(e -> showTable(
                "My Attendance",
                """
                SELECT attendance_date, status
                FROM attendance
                WHERE student_id = ?
                ORDER BY attendance_date DESC
                """, id));

        results.addActionListener(e -> showTable(
                "My Results",
                "SELECT subject, marks FROM results WHERE student_id = ?",
                id));

        fees.addActionListener(e -> showTable(
                "My Fees",
                "SELECT amount, status FROM fees WHERE student_id = ?",
                id));

        logout.addActionListener(e -> showHomeScreen());

        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DBConnection app = new DBConnection();

            if (app.con != null) {
                app.setVisible(true);
            } else {
                app.dispose();
            }
        });
    }
}